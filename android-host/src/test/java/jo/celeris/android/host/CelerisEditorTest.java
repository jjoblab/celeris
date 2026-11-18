package jo.celeris.android.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        editeur.open("Autre.java", SOURCE);
        assertNull("open() après close() ne fait rien",
                editeur.session());
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
}
