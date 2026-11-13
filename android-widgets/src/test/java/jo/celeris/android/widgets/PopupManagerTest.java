package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le coordinateur des popups (chantier d'unification) : l'unique
 * écrivain de l'état ET des vues. Le banc prouve les invariants —
 * l'état et la couche ne divergent JAMAIS (les exclus et les cascades
 * ferment d'un seul tenant), la carte partagée entre genres se refuse
 * dès le câblage, le clavier délégué sert la carte interactive, le tap
 * hors cartes referme les genres qui cèdent, la pose se borne aux murs
 * de la couche.
 */
@RunWith(RobolectricTestRunner.class)
public class PopupManagerTest {

    private Context contexte;

    /** Le panneau d'essai : un contenu carré de 100 × 50. */
    private static final class PanneauEssai extends PopupPanel {

        static final int LARGEUR = 100;
        static final int HAUTEUR = 50;

        PanneauEssai(Context context, PopupPalette palette) {
            super(context, palette);
        }

        @Override
        void drawContent(Canvas canvas) {
            // rien à tracer — la possession seule intéresse le banc
        }

        @Override
        int contentWidth() {
            return LARGEUR;
        }

        @Override
        int contentHeight() {
            return HAUTEUR;
        }
    }

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    private PanneauEssai panneau() {
        return new PanneauEssai(contexte, PopupPalette.dark());
    }

    /** La couche d'essai, mesurée 300 × 200 — les murs y vivent. */
    private PopupLayer couche() {
        PopupLayer couche = new PopupLayer(contexte);
        couche.layout(0, 0, 300, 200);
        return couche;
    }

    private PopupManager manager() {
        return new PopupManager(new PopupController(), couche());
    }

    /** La carte partagée entre deux genres se REFUSE — le partage
     *  d'instances désynchronisait l'état (l'audit P1) : le lien
     *  l'interdit dès le câblage. */
    @Test
    public void laCartePartageeSeRefuse() {
        PopupManager popups = manager();
        PanneauEssai carte = panneau();
        popups.lier(PopupController.PopupKind.COMPLETION_LIST, carte);
        try {
            popups.lier(PopupController.PopupKind.HOVER, carte);
            fail("une carte ne sert jamais deux genres");
        } catch (IllegalArgumentException attendu) {
            assertTrue("le refus nomme le genre fautif",
                    attendu.getMessage().contains("HOVER"));
        }
        assertSame("le lien initial survit au refus", carte,
                popups.vue(PopupController.PopupKind.COMPLETION_LIST));
    }

    /** Fermer la liste emporte SA documentation — l'état et la couche
     *  d'un seul tenant, jamais la cascade codée deux fois. */
    @Test
    public void fermerLaListeEmporteSaDocumentation() {
        PopupManager popups = manager();
        popups.lier(PopupController.PopupKind.COMPLETION_LIST, panneau());
        popups.lier(PopupController.PopupKind.COMPLETION_DOC, panneau());
        assertTrue(popups.montrer(
                PopupController.PopupKind.COMPLETION_LIST, 10, 10));
        assertTrue(popups.montrer(
                PopupController.PopupKind.COMPLETION_DOC, 120, 10));
        assertNotNull(popups.couche().popupOf(
                PopupController.PopupKind.COMPLETION_DOC));
        assertTrue(popups.fermer(PopupController.PopupKind.COMPLETION_LIST));
        assertFalse("l'état ferme la documentation",
                popups.estOuvert(PopupController.PopupKind.COMPLETION_DOC));
        assertNull("la couche retire la documentation — jamais de zombie",
                popups.couche().popupOf(
                        PopupController.PopupKind.COMPLETION_DOC));
        assertNull("la couche retire la liste",
                popups.couche().popupOf(
                        PopupController.PopupKind.COMPLETION_LIST));
    }

    /** L'ouverture qui EXCLUT retire la carte de l'exclu AVANT la
     *  pose — l'état et la couche ne diverge jamais (deux menus ne tiennent jamais). */
    @Test
    public void louvertureQuiExclutRetireLaCarteDeLexclu() {
        PopupManager popups = manager();
        popups.lier(PopupController.PopupKind.COMPLETION_LIST, panneau());
        popups.lier(PopupController.PopupKind.TOOLBAR_OVERFLOW, panneau());
        assertTrue(popups.montrer(
                PopupController.PopupKind.COMPLETION_LIST, 10, 10));
        assertNotNull(popups.couche().popupOf(
                PopupController.PopupKind.COMPLETION_LIST));
        assertTrue(popups.montrer(
                PopupController.PopupKind.TOOLBAR_OVERFLOW, 150, 0));
        assertFalse("l'état ferme la complétion",
                popups.estOuvert(PopupController.PopupKind.COMPLETION_LIST));
        assertNull("la couche retire la carte de l'exclu",
                popups.couche().popupOf(
                        PopupController.PopupKind.COMPLETION_LIST));
        assertNotNull("la carte de la toolbar se montre",
                popups.couche().popupOf(
                        PopupController.PopupKind.TOOLBAR_OVERFLOW));
    }

    /** Le genre REFUSÉ par la machine ne se montre pas — la
     *  documentation sans sa liste ne pose aucune vue. */
    @Test
    public void leGenreRefuseNeSeMontrePas() {
        PopupManager popups = manager();
        popups.lier(PopupController.PopupKind.COMPLETION_DOC, panneau());
        assertFalse(popups.montrer(
                PopupController.PopupKind.COMPLETION_DOC, 10, 10));
        assertFalse(popups.estOuvert(PopupController.PopupKind.COMPLETION_DOC));
        assertNull(popups.couche().popupOf(
                PopupController.PopupKind.COMPLETION_DOC));
    }

    /** La frappe déléguée ferme l'état ET retire les cartes — le
     *  zombie ne s'y rencontre jamais, le chrome survit. */
    @Test
    public void laFrappeRetireLesCartes() {
        PopupManager popups = manager();
        popups.lier(PopupController.PopupKind.HOVER, panneau());
        popups.lier(PopupController.PopupKind.COLOR_PICKER, panneau());
        popups.lier(PopupController.PopupKind.MINIMAP, panneau());
        popups.montrer(PopupController.PopupKind.HOVER, 10, 40);
        popups.montrer(PopupController.PopupKind.COLOR_PICKER, 150, 40);
        popups.montrer(PopupController.PopupKind.MINIMAP, 180, 40);
        assertFalse(popups.surFrappe().isEmpty());
        assertNull("le survol cède à la frappe",
                popups.couche().popupOf(PopupController.PopupKind.HOVER));
        assertNull("le sélecteur cède à la frappe",
                popups.couche().popupOf(
                        PopupController.PopupKind.COLOR_PICKER));
        assertNotNull("le chrome survit à la frappe",
                popups.couche().popupOf(
                        PopupController.PopupKind.MINIMAP));
    }

    /** Le tap HORS CARTES referme les genres qui cèdent — le doigt
     *  tombe hors de toute carte portée, l'avis part, la carte
     *  d'actions quitte la couche ; le doigt SUR une carte ne
     *  referme rien — le geste sert la carte. */
    @Test
    public void leTapHorsCartesRefermeLesGenresQuiCedent() {
        PopupManager popups = manager();
        popups.lier(PopupController.PopupKind.INTENTIONS_MENU, panneau());
        popups.lier(PopupController.PopupKind.COMPLETION_LIST, panneau());
        popups.montrer(PopupController.PopupKind.INTENTIONS_MENU, 0, 0);
        popups.montrer(PopupController.PopupKind.COMPLETION_LIST, 200, 150);
        MotionEvent horsCartes = MotionEvent.obtain(0L, 0L,
                MotionEvent.ACTION_DOWN, 5f, 5f, 0);
        popups.couche().dispatchTouchEvent(horsCartes);
        horsCartes.recycle();
        assertFalse("l'état ferme le menu au geste extérieur",
                popups.estOuvert(PopupController.PopupKind.INTENTIONS_MENU));
        assertNull("la couche retire la carte du menu",
                popups.couche().popupOf(
                        PopupController.PopupKind.INTENTIONS_MENU));
        assertTrue("la complétion suit le curseur, pas le doigt",
                popups.estOuvert(PopupController.PopupKind.COMPLETION_LIST));
        popups.montrer(PopupController.PopupKind.INTENTIONS_MENU, 0, 0);
        MotionEvent surCarte = MotionEvent.obtain(0L, 0L,
                MotionEvent.ACTION_DOWN, 50f, 25f, 0);
        popups.couche().dispatchTouchEvent(surCarte);
        surCarte.recycle();
        assertTrue("le doigt sur la carte ne la referme pas",
                popups.estOuvert(PopupController.PopupKind.INTENTIONS_MENU));
    }

    /** La pose se BORNE aux murs de la couche — la défense en
     *  profondeur : la carte qui déborde se recolle au mur, la carte
     *  ENTIÈRE (respirations comprises) tient dans la couche. */
    @Test
    public void laPoseSeBorneAuxMurs() {
        PopupManager popups = manager();
        popups.lier(PopupController.PopupKind.HOVER, panneau());
        popups.montrer(PopupController.PopupKind.HOVER, 500, 500);
        View carte = popups.vue(PopupController.PopupKind.HOVER);
        assertEquals("la carte se recolle au mur droit",
                300 - carte.getMeasuredWidth(), carte.getLeft());
        assertEquals("la carte se recolle au mur bas",
                200 - carte.getMeasuredHeight(), carte.getTop());
    }

    /** Le clavier DÉLÉGUÉ sert la complétion : flèches, validation
     *  (Enter, Tab), échappement — la complétion emporte sa
     *  documentation, la touche inconnue retourne à l'éditeur. */
    @Test
    public void leClavierDelegueSertLaCompletion() {
        PopupLayer couche = couche();
        PopupManager popups = new PopupManager(new PopupController(), couche);
        CompletionListView liste = new CompletionListView(
                couche.getContext(), PopupPalette.dark());
        popups.lier(PopupController.PopupKind.COMPLETION_LIST, liste);
        popups.lier(PopupController.PopupKind.COMPLETION_DOC, panneau());
        liste.setItems(List.of(
                new CompletionListView.Candidat("alpha", "", BadgeGenre.Genre.MOT_CLE),
                new CompletionListView.Candidat("beta", "", BadgeGenre.Genre.CHAMP),
                new CompletionListView.Candidat("gamma", "", BadgeGenre.Genre.CLASSE)));
        popups.montrer(PopupController.PopupKind.COMPLETION_LIST, 10, 10);
        popups.montrer(PopupController.PopupKind.COMPLETION_DOC, 120, 10);
        assertEquals("la surbrillance naît à la tête", 0, liste.surbrillance());
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_DPAD_DOWN));
        assertEquals("la flèche descend la surbrillance", 1,
                liste.surbrillance());
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_DPAD_DOWN));
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_DPAD_DOWN));
        assertEquals("la flèche au bas s'arrête", 2, liste.surbrillance());
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_DPAD_UP));
        assertEquals("la flèche remonte", 1, liste.surbrillance());
        assertFalse("la touche inconnue retourne à l'éditeur",
                popups.surTouche(KeyEvent.KEYCODE_DPAD_LEFT));
        int[] retenu = { -1 };
        liste.setOnPick(index -> retenu[0] = index);
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_ENTER));
        assertEquals("Enter valide le candidat courant", 1, retenu[0]);
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_TAB));
        assertEquals("Tab valide aussi le candidat courant", 1, retenu[0]);
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_ESCAPE));
        assertFalse(popups.estOuvert(PopupController.PopupKind.COMPLETION_LIST));
        assertNull("la documentation suit sa liste à l'échappement",
                popups.couche().popupOf(
                        PopupController.PopupKind.COMPLETION_DOC));
    }

    /** Le clavier DÉLÉGUÉ sert le menu d'intentions : flèches et
     *  validation exécutent l'action surlignée, l'échappement ferme. */
    @Test
    public void leClavierDelegueSertLeMenu() {
        PopupLayer couche = couche();
        PopupManager popups = new PopupManager(new PopupController(), couche);
        IntentionsMenuView menu = new IntentionsMenuView(
                couche.getContext(), PopupPalette.dark());
        popups.lier(PopupController.PopupKind.INTENTIONS_MENU, menu);
        int[] execute = { 0 };
        menu.setActions(List.of(
                new IntentionsMenuView.Item("premier", () -> execute[0] = 1),
                new IntentionsMenuView.Item("second", () -> execute[0] = 2)));
        popups.montrer(PopupController.PopupKind.INTENTIONS_MENU, 10, 10);
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_DPAD_DOWN));
        assertEquals("la flèche surligne la première action", 0,
                menu.surbrillance());
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_DPAD_DOWN));
        assertEquals("la flèche descend", 1, menu.surbrillance());
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_ENTER));
        assertEquals("Enter exécute l'action surlignée", 2, execute[0]);
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_ESCAPE));
        assertFalse(popups.estOuvert(PopupController.PopupKind.INTENTIONS_MENU));
    }

    /** La page DÉLÉGUÉE sert la complétion (le pas étendu, premier
     *  reste du signal du propriétaire du 2026-11-13) : PAGE_DOWN
     *  saute la surbrillance d'une PAGE de candidats visibles — huit,
     *  le plafond de la liste —, PAGE_UP remonte d'autant, le saut se
     *  borne aux murs de la liste ; la carte sans pages DÉCLINE, la
     *  touche retourne à la vie normale de l'éditeur. */
    @Test
    public void laPageEtendueSertLaCompletion() {
        PopupLayer couche = couche();
        PopupManager popups = new PopupManager(new PopupController(), couche);
        CompletionListView liste = new CompletionListView(
                couche.getContext(), PopupPalette.dark());
        popups.lier(PopupController.PopupKind.COMPLETION_LIST, liste);
        List<CompletionListView.Candidat> candidats = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            candidats.add(new CompletionListView.Candidat("c" + i, "",
                    BadgeGenre.Genre.MOT_CLE));
        }
        liste.setItems(candidats);
        popups.montrer(PopupController.PopupKind.COMPLETION_LIST, 10, 10);
        assertEquals("la surbrillance naît à la tête", 0, liste.surbrillance());
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_PAGE_DOWN));
        assertEquals("la page descend de huit rangées", 8,
                liste.surbrillance());
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_PAGE_DOWN));
        assertEquals("la page se borne au bas de la liste", 11,
                liste.surbrillance());
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_PAGE_UP));
        assertEquals("la page remonte de huit", 3, liste.surbrillance());
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_PAGE_UP));
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_PAGE_UP));
        assertEquals("la page se borne à la tête", 0, liste.surbrillance());
        popups.fermer(PopupController.PopupKind.COMPLETION_LIST);
        IntentionsMenuView menu = new IntentionsMenuView(
                couche.getContext(), PopupPalette.dark());
        popups.lier(PopupController.PopupKind.INTENTIONS_MENU, menu);
        int[] execute = { 0 };
        menu.setActions(List.of(
                new IntentionsMenuView.Item("premier", () -> execute[0] = 1),
                new IntentionsMenuView.Item("second", () -> execute[0] = 2)));
        popups.montrer(PopupController.PopupKind.INTENTIONS_MENU, 10, 10);
        assertFalse("le menu décline les pages — la touche retourne à "
                + "l'éditeur", popups.surTouche(KeyEvent.KEYCODE_PAGE_DOWN));
        assertTrue("le menu vit, la page déclinée ne l'a pas servie",
                popups.estOuvert(PopupController.PopupKind.INTENTIONS_MENU));
    }

    /** Le clavier DÉLÉGUÉ sert la carte des usages (le signal du
     *  propriétaire du 2026-11-13) : l'ouverture chasse le menu du
     *  caret (deux surfaces d'actions ne tiennent jamais — sa carte
     *  quitte la couche), les flèches déplacent la surbrillance
     *  d'occurrence, Enter exécute le saut, la page décline — la
     *  touche retourne à l'éditeur, l'échappement referme. */
    @Test
    public void leClavierDelegueSertLesUsages() {
        PopupLayer couche = couche();
        PopupManager popups = new PopupManager(new PopupController(), couche);
        ReferencesView carte = new ReferencesView(couche.getContext(),
                PopupPalette.dark());
        popups.lier(PopupController.PopupKind.REFERENCES, carte);
        popups.lier(PopupController.PopupKind.INTENTIONS_MENU, panneau());
        popups.montrer(PopupController.PopupKind.INTENTIONS_MENU, 10, 10);
        assertNotNull("la carte du menu se montre d'abord",
                popups.couche().popupOf(PopupController.PopupKind.INTENTIONS_MENU));
        int[] saute = { -1 };
        carte.setOccurrences(List.of(
                new ReferencesView.Occurrence("L3 · int a", () -> saute[0] = 3),
                new ReferencesView.Occurrence("L7 · int a", () -> saute[0] = 7)));
        popups.montrer(PopupController.PopupKind.REFERENCES, 10, 80);
        assertFalse("l'ouverture chasse le menu du caret",
                popups.estOuvert(PopupController.PopupKind.INTENTIONS_MENU));
        assertNull("la carte du menu quitte la couche",
                popups.couche().popupOf(PopupController.PopupKind.INTENTIONS_MENU));
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_DPAD_DOWN));
        assertEquals("la flèche surligne la première occurrence", 0,
                carte.surbrillance());
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_DPAD_DOWN));
        assertEquals("la flèche descend", 1, carte.surbrillance());
        assertFalse("la page décline — la touche retourne à l'éditeur",
                popups.surTouche(KeyEvent.KEYCODE_PAGE_DOWN));
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_ENTER));
        assertEquals("Enter exécute le saut de l'occurrence surlignée", 7,
                saute[0]);
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_ESCAPE));
        assertFalse("l'échappement referme la carte des usages",
                popups.estOuvert(PopupController.PopupKind.REFERENCES));
    }

    /** L'échappement sans carte interactive referme les bulles
     *  éphémères — le chrome demeure. */
    @Test
    public void lechappementRefermeLesBulles() {
        PopupManager popups = manager();
        popups.lier(PopupController.PopupKind.HOVER, panneau());
        popups.lier(PopupController.PopupKind.MINIMAP, panneau());
        popups.montrer(PopupController.PopupKind.HOVER, 10, 10);
        popups.montrer(PopupController.PopupKind.MINIMAP, 180, 40);
        assertTrue(popups.surTouche(KeyEvent.KEYCODE_ESCAPE));
        assertFalse(popups.estOuvert(PopupController.PopupKind.HOVER));
        assertTrue("le chrome ne cède pas à l'échappement",
                popups.estOuvert(PopupController.PopupKind.MINIMAP));
    }

    /** La feuille modale coupe l'intelligence de la couche — le
     *  chrome demeure, jamais une complétion sous le voile. */
    @Test
    public void laFeuilleCoupeLintelligencePasLeChrome() {
        PopupManager popups = manager();
        popups.lier(PopupController.PopupKind.COMPLETION_LIST, panneau());
        popups.lier(PopupController.PopupKind.MINIMAP, panneau());
        popups.montrer(PopupController.PopupKind.COMPLETION_LIST, 10, 10);
        popups.montrer(PopupController.PopupKind.MINIMAP, 180, 40);
        assertFalse(popups.fermerSousVoile().isEmpty());
        assertFalse("la complétion ne survit pas au voile",
                popups.estOuvert(PopupController.PopupKind.COMPLETION_LIST));
        assertTrue("le chrome survit au voile",
                popups.estOuvert(PopupController.PopupKind.MINIMAP));
    }

    /** La mesure du genre — LA mesure unique : la taille rendue sert
     *  les ancres de l'hôte. Elle rend la carte ENTIÈRE, respirations
     *  comprises — ce que la couche pose, ce que l'ancre anticipe. */
    @Test
    public void laMesureDuGenreSeRend() {
        PopupManager popups = manager();
        PanneauEssai carte = panneau();
        popups.lier(PopupController.PopupKind.HOVER, carte);
        PopupPlacer.Cadre taille = popups.taille(
                PopupController.PopupKind.HOVER);
        assertEquals("la mesure rend le contenu et sa respiration",
                PanneauEssai.LARGEUR + 2 * carte.paddingPx,
                taille.largeur());
        assertEquals("la mesure rend le contenu et sa respiration",
                PanneauEssai.HAUTEUR + 2 * carte.paddingYPx,
                taille.hauteur());
    }

    /** Le détachement de fin de vie : tout se ferme, l'avis du tap
     *  hors cartes se coupe — le doigt ne referme plus rien. */
    @Test
    public void leDetachementCoupeTout() {
        PopupManager popups = manager();
        popups.lier(PopupController.PopupKind.INTENTIONS_MENU, panneau());
        popups.montrer(PopupController.PopupKind.INTENTIONS_MENU, 10, 10);
        popups.detacher();
        assertFalse(popups.estOuvert(PopupController.PopupKind.INTENTIONS_MENU));
        assertNull(popups.vue(PopupController.PopupKind.INTENTIONS_MENU));
        MotionEvent appui = MotionEvent.obtain(0L, 0L,
                MotionEvent.ACTION_DOWN, 5f, 5f, 0);
        popups.couche().dispatchTouchEvent(appui);
        appui.recycle();
        assertFalse("l'avis coupé ne referme plus rien",
                popups.estOuvert(PopupController.PopupKind.INTENTIONS_MENU));
    }
}
