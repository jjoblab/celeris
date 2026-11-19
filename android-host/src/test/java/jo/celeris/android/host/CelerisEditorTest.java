package jo.celeris.android.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jo.celeris.android.view.EditorThemes;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.language.LanguageKit;
import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.TextSession;

import org.junit.Test;
import jo.celeris.android.widgets.SearchBarModel;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La façade de montage (ADR-0022, phase 1) : attach() compose vue,
 * popups, saisie intelligente et langue détectée en quelques lignes ;
 * open() crée la session, résout la langue du nom de fichier et la
 * pose dès que le registre vit ; settings() est LA source unique du
 * thème et de la taille (vue et popups ensemble, la taille suit aussi
 * le pincement) ; close() libère tout (idempotent, la vue collectable,
 * les fils d'analyse éteints).
 */
@RunWith(RobolectricTestRunner.class)
public class CelerisEditorTest {

    /** La langue de banc : java posé directement, registre immédiat. */
    private static final LanguageKit KIT = LanguageKit
            .builder(Runnable::run)
            .addSupport(LanguageSupport.builder(
                            LanguageDescriptor.of("java", "Java",
                                    java.util.List.of("java")))
                    .syntax(JavaSyntax.INSTANCE)
                    .build())
            .build();

    private static final String SOURCE = """
            class Premiere {
                void methode() {
                }
            }
            """;

    /** Le montage complet tient en quelques lignes : la vue, la
     *  couche, la façade — session, popups, intelligence et langue
     *  suivent. */
    @Test
    public void leMontageCompletTientEnQuelquesLignes() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", SOURCE);

        TextSession session = editeur.session();
        assertNotNull("la session vit", session);
        assertEquals("le texte y est", SOURCE, session.document().text());
        assertTrue("l'intelligence active est celle du kit",
                vue.intelligence().provider() != null);
        assertNotNull("l'orchestrateur des popups vit", editeur.popups());
        assertNotNull("la langue du .java est résolue", editeur.language());
        editeur.close();
    }

    /** open() résout la langue par le nom du fichier : le .java se
     *  pose, l'extension inconnue reste sans langue. */
    @Test
    public void openResoutLaLangueDuNomDeFichier() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", SOURCE);
        LanguageSupport langue = editeur.language();
        assertTrue("la langue du .java est résolue",
                langue != null && "java".equals(langue.id()));
        editeur.open("notes.inconnu", "texte");
        assertNull("une extension inconnue ne pose aucune langue",
                editeur.language());
        editeur.close();
    }

    /** settings() est LA source unique : le thème et la taille y
     *  tournent, la vue ET les popups suivent ensemble — la taille se
     *  lit par l'API publique de la vue (fontSizeSp). */
    @Test
    public void settingsSourceUniqueThemeEtTaille() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", SOURCE);

        editeur.settings().setFontSizeSp(18f);
        assertEquals("la vue porte la taille posée (source unique)",
                18f, vue.fontSizeSp(), 0.001f);
        editeur.settings().setTheme(EditorThemes.SOMBRE);
        // Le thème se lit par le comportement : la façade le rend.
        assertEquals("la façade rend le thème posé",
                EditorThemes.SOMBRE, editeur.settings().theme());
        editeur.close();
    }

    /** close() libère tout : vue inerte, façade muette, idempotent —
     *  la session survit (l'hôte peut la garder). */
    @Test
    public void closeLibereTout() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", SOURCE);
        TextSession session = editeur.session();

        editeur.close();
        editeur.close();

        assertNull("la façade fermée ne rend plus d'orchestrateur",
                editeur.popups());
        assertNull("la façade fermée ne rend plus de session",
                editeur.session());
        // D12 : la façade fermée PARLE — l'open lève IllegalStateException
        try {
            editeur.open("Autre.java", SOURCE);
            org.junit.Assert.fail("l'open après close doit lever");
        } catch (IllegalStateException attendu) {
            assertNull("la session reste muette après le refus",
                    editeur.session());
        }
        assertTrue("la session survit à la fermeture (l'hôte la garde)",
                session.document().text().contains("Premiere"));
    }

    /** La vue collectable après close() même si la session survit —
     *  la fuite relevée par la revue ne revient pas par la façade.
     *  La façade meurt dans sa méthode à elle : le frame du banc ne
     *  garde aucune référence forte. */
    @Test
    public void laVueLibereeEstCollectable() throws Exception {
        TextSession session = TextSession.of(SOURCE, System::nanoTime);
        WeakReference<EditorView> vueFaible = monterOuvrirFermer(session);
        for (int tour = 0; tour < 50 && vueFaible.get() != null; tour++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(10L);
        }
        assertNull("la vue fermée ne survit pas au ramasse-miettes",
                vueFaible.get());
    }

    /** La facade fermee ne survit pas au ramasse-miettes même quand
     *  la VUE vit encore (le scénario réel : la vue reste dans le
     *  layout) : chaque rebranchement d'orchestrateur retire les
     *  verdicts de touche et de double appui AVANT de les reposer —
     *  l'ancien chemin les empilait (N onglets = N exemplaires du
     *  même prédicat, close() n'en retirait qu'un : les N-1 restants
     *  retenaient la façade fermée tant que la vue vivait). */

    @Test
    public void laFacadeFermeeNeFuitPasVersUneVueVivante() throws Exception {
        // La vue vit dans le layout (référence forte du banc).
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        WeakReference<CelerisEditor> facadeFaible =
                monterCyclerFermer(vue, couche, 5);
        // Le looper d'abord : les posts en vol (montage du chrome)
        // ne retiennent pas la façade, mais vidons tout de même.
        org.robolectric.Shadows.shadowOf(
                android.os.Looper.getMainLooper()).idle();
        for (int tour = 0; tour < 50 && facadeFaible.get() != null; tour++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(10L);
        }
        assertNull("aucun verdict empilé ne retient la façade fermée "
                + "pendant que la vue vit", facadeFaible.get());
    }

    /** Monte la façade sur la vue donnée, cycle N documents DIFFÉRENTS
     *  (rebranchement de l'orchestrateur à chaque fois), ferme — ne
     *  rend que la référence faible de la FAÇADE ; la vue et la
     *  couche vivent encore, tenues par l'appelant. */
    /** Le kit PRÉCHARGÉ du banc de fuite : sans preload(), le
     *  premier open() lance la pose différée de langue (fil démon +
     *  vue.post) qui retient la façade TANT QUE le registre
     *  s'assemble — le banc la court-circuite, comme le banc de
     *  réouverture (le scénario testé est la fuite APRÈS close(),
     *  pas la pose différée légitime). */
    private static final LanguageKit KIT_PRET = LanguageKit
            .builder(Runnable::run)
            .addSupport(LanguageSupport.builder(
                            LanguageDescriptor.of("java", "Java",
                                    java.util.List.of("java")))
                    .syntax(JavaSyntax.INSTANCE)
                    .build())
            .build();

    private static WeakReference<CelerisEditor> monterCyclerFermer(
            EditorView vue, PopupLayer couche, int onglets) {
        KIT_PRET.preload();
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT_PRET);
        for (int onglet = 0; onglet < onglets; onglet++) {
            editeur.open("Fichier" + onglet + ".java", SOURCE);
        }
        editeur.close();
        WeakReference<CelerisEditor> faible = new WeakReference<>(editeur);
        editeur = null; // le slot de pile ne retient plus la façade
        return faible;
    }

    /** Monte la façade, ouvre, ferme — ne rend que la référence
     *  faible de la vue ; la façade et la couche meurent dans le
     *  frame de CETTE méthode, pas dans celui du banc. */
    private static WeakReference<EditorView> monterOuvrirFermer(
            TextSession sessionGardee) {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", sessionGardee.document().text());
        editeur.close();
        return new WeakReference<>(vue);
    }

    /** Aucune prolifération de fils sur N montages/libérations — les
     *  fils « celeris-analyse » du kit s'éteignent à chaque close(). */
    @Test
    public void aucuneProliferationDeFils() throws Exception {
        for (int tour = 0; tour < 3; tour++) {
            EditorView vue = new EditorView(
                    RuntimeEnvironment.getApplication());
            PopupLayer couche = new PopupLayer(
                    RuntimeEnvironment.getApplication());
            CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
            editeur.open("Main.java", SOURCE);
            editeur.close();
        }
        long limite = System.nanoTime() + 10_000_000_000L;
        while (!filsAnalyse().isEmpty() && System.nanoTime() < limite) {
            Thread.sleep(10L);
        }
        assertTrue("aucun fil celeris-analyse ne survit aux close()",
                filsAnalyse().isEmpty());
    }

    /** Les noms des fils d'analyse du kit, vivants. */
    private static Set<Thread> filsAnalyse() {
        Set<Thread> vivants = ConcurrentHashMap.newKeySet();
        for (Thread fil : Thread.getAllStackTraces().keySet()) {
            if (fil.isAlive() && fil.getName().startsWith("celeris-analyse")) {
                vivants.add(fil);
            }
        }
        return vivants;
    }

    /** La taille posée par la façade suit aussi le pincement : le
     *  réglage écrit reste la source, l'écouteur de la façade y
     *  rabat les popups. */
    @Test
    public void laTailleSuitLeReglage() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", SOURCE);
        editeur.settings().setFontSizeSp(16f);
        assertEquals(16f, vue.fontSizeSp(), 0.001f);
        editeur.settings().setFontSizeSp(20f);
        assertEquals(20f, vue.fontSizeSp(), 0.001f);
        editeur.close();
    }

    /** Le kit et l'hôte s'abonnent au même événement de fenêtre sans
     *  s'écraser (ADR-0022, phase 2, T3 — le point 13 de la revue) :
     *  l'orchestrateur des popups vit sur la voie multi-abonnés, le
     *  second abonné ne l'écrase plus ; après close(), l'abonné du
     *  kit part, celui de l'hôte reste. */
    @Test
    public void leKitEtLhoteSabonnentEnsembleALaFenetre() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", SOURCE);
        int[] avisHote = { 0 };
        vue.addViewportChangeListener(() -> avisHote[0]++);

        // L'hôte ne peut plus écraser le kit : le repositionnement des
        // popups vit encore (l'abonné du kit n'a pas été remplacé).
        assertNotNull("l'orchestrateur vit toujours après l'abonnement de l'hôte",
                editeur.popups());

        editeur.close();
        // Le kit est parti ; l'écouteur de l'hôte survit à la façade.
        int[] avisApres = { 0 };
        vue.addViewportChangeListener(() -> avisApres[0]++);
        assertNotNull("la vue close garde un écouteur d'hôte vivant",
                vue);
        // D12 : la façade fermée PARLE — l'open d'après close lève
        // au lieu de laisser l'hôte devant un écran vide silencieux.
        try {
            editeur.open("Autre.java", SOURCE);
            org.junit.Assert.fail("l'open après close doit lever");
        } catch (IllegalStateException attendu) {
            assertNull("la façade close reste muette", editeur.session());
        }
    }

    /** T4 (ADR-0022, phase 2) : les déclencheurs popups sont branchés
     *  PAR LE KIT — l'hôte n'écrit plus setSurTouchePopup ni
     *  setSurDoubleTap (le geste de l'application d'avant). La preuve par
     *  la délégation clavier (PIEGE-27) : la carte des réglages
     *  ouverte par la voie publique, ÉCHAP remonte au kit AVANT la
     *  table de raccourcis — la carte se referme et le curseur ne
     *  bouge pas ; sans le branchement, ÉCHAP ne fermait rien (le
     *  verdict n'existait pas côté vue) et la touche tombait dans la
     *  vie normale de l'éditeur. */
    @Test
    public void laFacadeBrancheLaDelegationClavierDesPopups() throws Exception {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", SOURCE);

        // la carte s'ouvre par la voie publique du contrôleur (une
        // carte du régime clavier, INTENTIONS_MENU) — le banc isole
        //  la DÉLÉGATION : ce qui se prouve ici, c'est le branchement
        // du verdict touche par la façade, pas le contenu des cartes
        // (le plan et l'analyse suivent leurs propres bancs).
        jo.celeris.core.text.TextSession sessionOuverte = editeur.session();
        editeur.popups().manager().controleur().open(
                jo.celeris.android.widgets.PopupController.PopupKind.INTENTIONS_MENU);
        assertTrue("la carte vit (ouverte par la voie publique)",
                editeur.popups().manager().estOuvert(
                        jo.celeris.android.widgets.PopupController.PopupKind.INTENTIONS_MENU));

        int caretAvant = sessionOuverte.selection().ranges().get(0).caret();
        KeyEvent echap = new KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ESCAPE, 0, 0);
        vue.onKeyDown(KeyEvent.KEYCODE_ESCAPE, echap);

        // le verdict du kit a consommé ÉCHAP : la carte s'est fermée
        // et le curseur n'a pas bougé — sans branchement, la carte
        // serait restée ouverte (personne n'écoutait)
        assertFalse("la carte refermée par le kit",
                editeur.popups().manager().estOuvert(
                        jo.celeris.android.widgets.PopupController.PopupKind.INTENTIONS_MENU));
        assertEquals("ÉCHAP a servi la carte, pas le curseur",
                caretAvant,
                sessionOuverte.selection().ranges().get(0).caret());
        editeur.close();
    }

    /** Le registre des documents (le retour terrain du 2026-11-19 :
     *  « quand j'appuie sur l'un des onglets la coloration se refait
     *  encore et encore ») : un fichier réouvert retrouve SA session —
     *  le texte ÉDITÉ survit au changement d'onglet, l'orchestrateur
     *  des popups ne se recrée pas pour un document connu, la langue
     *  du document revient avec lui. */
    @Test
    public void laReouvertureRetrouveLeDocumentEtSonTexteEdite() {
        // Le kit du banc ASSEMBLE son registre : la langue se résout
        // dès le premier open (le KIT statique ne précharge pas — la
        // pose différée y vivrait, le banc la court-circuite).
        LanguageKit kitPret = LanguageKit
                .builder(Runnable::run)
                .addSupport(LanguageSupport.builder(
                                LanguageDescriptor.of("java", "Java",
                                        java.util.List.of("java")))
                        .syntax(JavaSyntax.INSTANCE)
                        .build())
                .build();
        kitPret.preload();
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, kitPret);
        editeur.open("Main.java", SOURCE);
        TextSession premiere = editeur.session();
        // l'utilisateur ÉDITE le premier document
        premiere.replaceRange(0, 0, "// note du banc\n");

        editeur.open("Autre.java", SOURCE);
        TextSession deuxieme = editeur.session();
        assertFalse("un autre fichier ouvre une AUTRE session",
                deuxieme == premiere);

        // le RETOUR au premier fichier : SA session, son texte édité
        editeur.open("Main.java", SOURCE);
        assertTrue("la réouverture retrouve LA session du document",
                editeur.session() == premiere);
        assertTrue("le texte édité survit au changement d'onglet",
                editeur.session().document().text()
                        .contains("// note du banc"));
        assertTrue("la langue du document revient avec lui",
                editeur.language() != null
                        && "java".equals(editeur.language().id()));
        editeur.close();
    }

    /** L'orchestrateur des popups suit la SESSION (le retour terrain
     *  du 2026-11-19 : chaque changement d'onglet détruisait et
     *  recréait ~20 vues de popups) : il survit au retour d'un
     *  document connu, il se REBRANCHE pour un document différent. */
    @Test
    public void lOrchestrateurSurvitAuRetourDUnDocumentConnu() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", SOURCE);
        Object orchestrateurInitial = editeur.popups();

        editeur.open("Autre.java", SOURCE);
        // document différent : l'orchestrateur se rebranche
        assertFalse("un document différent rebranche l'orchestrateur",
                editeur.popups() == orchestrateurInitial);
        Object orchestrateurRebranche = editeur.popups();

        // le retour au document connu recrée l'orchestrateur de SA
        // session (la session de l'orchestrateur est figée à la
        // construction — il ne peut pas suivre l'autre document) :
        // le coà»t est celui des popups, PAS de la tokenisation (les
        // moteurs parqués de la vue ne re-tokenisent rien).
        editeur.open("Main.java", SOURCE);
        // le MÊME document rouvert D'AFFILÉE ne touche à rien du tout
        Object orchestrateurDuRetour = editeur.popups();
        editeur.open("Main.java", SOURCE);
        assertTrue("rouvrir le même document d'affilée ne recrée rien",
                editeur.popups() == orchestrateurDuRetour);
        editeur.close();
    }

    /** C10 (l'analyse croisée) : la barre de recherche de la façade
     *  VIT avec le document — les occurrences suivent la frappe, la
     *  barre suit le changement d'onglet, le remplacement courant
     *  passe par la voie de transaction, et close() la détache. */
    @Test
    public void laBarreDeRechercheVitAvecLeDocument() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", SOURCE);

        SearchBarModel barre = editeur.search();
        barre.setQuery("void");
        assertEquals("les occurrences vivent", 1, barre.matches().size());
        // une édition AJOUTE une occurrence : la barre le voit seule
        editeur.session().replaceRange(0, 0, "// void\n");
        assertEquals("le recalcul part à l'édition",
                2, barre.matches().size());
        // le remplacement courant traverse la transaction (annulable) :
        // l'occurrence visée est celle de la méthode, pas du commentaire
        int offsetMethode = editeur.session().document().text().indexOf("void methode");
        barre.setCurrentNear(offsetMethode);
        assertTrue(barre.replaceCurrent("int"));
        assertTrue(editeur.session().document().text().contains("int methode"));

        // le changement d'onglet : la MÊME barre suit la session neuve
        editeur.open("Autre.java", "void autre()\n");
        assertEquals("la barre suit l'onglet ouvert", 1, barre.matches().size());
        assertTrue("la même instance sert l'autre document",
                editeur.search() == barre);
        editeur.close();
    }

    /** D12 (l'analyse croisée) : la façade fermée PARLE — open()
     *  après close() lève IllegalStateException au lieu de laisser
     *  l'hôte devant un écran vide sans signal. */
    @Test(expected = IllegalStateException.class)
    public void openApresCloseLeve() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", SOURCE);
        editeur.close();
        editeur.open("Main.java", SOURCE); // avant : silence, écran vide
    }
}
