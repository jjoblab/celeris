package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La ligne du caret s'illumine et la portée active se met en évidence
 * (le retour terrain du 2026-11-19 : « la ligne où se trouve le curseur
 * s'illumine avec une couleur de fond, de même pour le fil vertical
 * devant la méthode, après le { et avant le } ») : la BANDE de la
 * rangée du caret se trace au curseur réduit (jamais pendant une
 * sélection), et le guide du bloc qui contient le caret prend l'encre
 * du CURSEUR, plus ÉPAIS que les guides ombres.
 */
@RunWith(RobolectricTestRunner.class)
public class BandeEtPorteeActiveTest {

    private Context contexte;

    private static final String TEXTE = "class A {\n"
            + "    void m() {\n"
            + "        int x = 1;\n"
            + "    }\n"
            + "}\n";

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** Le banc prêt : langage branché, analyse publiée, image dessinée. */
    private EditorTestRig bancPublie(String contenu) {
        EditorTestRig banc = EditorTestRig.sur(contexte, contenu);
        banc.vue().setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        return banc;
    }

    /** La bande de la rangée du caret : un rectangle plein qui couvre
     *  la ligne entière, de la gouttière au bord de la fenêtre. */
    @Test
    public void laBandeIlllumineLaLigneDuCaret() {
        EditorTestRig banc = bancPublie(TEXTE);
        EditorView vue = banc.vue();
        // le caret au début de la ligne 2 (« int x = 1; »)
        int caret = vue.session().document().lineStart(2);
        vue.session().setSelection(MultiSelection.of(new SelRange(caret, caret)));
        EditorTestRig.RecordingCanvas image = banc.image();
        float haut = vue.metrics.rowTop(2);
        EditorTestRig.RecordingCanvas.RectDraw bande = null;
        for (EditorTestRig.RecordingCanvas.RectDraw rect : image.rectangles()) {
            if (Math.abs(rect.top() - haut) <= 1f
                    && rect.right() >= rect.left() + 100f) {
                bande = rect;
                break;
            }
        }
        assertNotNull("la bande de la ligne du caret se trace", bande);
        assertTrue("la bande couvre la hauteur de la rangée",
                bande.bottom() - bande.top() >= vue.metrics.lineHeight() - 1f);
    }

    /** Une sélection ne porte PAS de bande : la sélection parle déjà. */
    @Test
    public void laSelectionCoupeLaBande() {
        EditorTestRig banc = bancPublie(TEXTE);
        EditorView vue = banc.vue();
        int debut = vue.session().document().lineStart(2);
        vue.session().setSelection(
                MultiSelection.of(new SelRange(debut, debut + 3)));
        EditorTestRig.RecordingCanvas image = banc.image();
        float haut = vue.metrics.rowTop(2);
        for (EditorTestRig.RecordingCanvas.RectDraw rect : image.rectangles()) {
            if (Math.abs(rect.top() - haut) <= 1f
                    && rect.right() >= rect.left() + 100f) {
                // la bande pleine largeur ne vit pas pendant une sélection
                assertTrue("pas de bande pleine largeur en sélection",
                        rect.right() < rect.left() + 50f
                        || rect.color() != couleurBande(banc));
            }
        }
    }

    /** Le guide du bloc qui CONTIENT le caret prend l'encre du curseur
     *  et un trait PLUS ÉPAIS que les guides ombres. */
    @Test
    public void leGuideDeLaPorteeActiveSAccente() {
        EditorTestRig banc = bancPublie(TEXTE);
        EditorView vue = banc.vue();
        // le caret dans le corps de la méthode (ligne 2, colonne 8) :
        // le guide de la MÉTHODE s'illumine (colonne 4), celui de la
        // CLASSE reste ombre (colonne 0)
        int caret = vue.session().document().lineStart(2) + 8;
        vue.session().setSelection(MultiSelection.of(new SelRange(caret, caret)));
        EditorTestRig.RecordingCanvas image = banc.image();
        float xMethode = vue.gutterWidth() + 4 * vue.metrics.charWidth();
        float xClasse = vue.gutterWidth();
        EditorTestRig.RecordingCanvas.LineDraw guideMethode = null;
        EditorTestRig.RecordingCanvas.LineDraw guideClasse = null;
        for (EditorTestRig.RecordingCanvas.LineDraw ligne : image.lignes()) {
            if (Math.abs(ligne.x1() - xMethode) <= 1f
                    && Math.abs(ligne.x2() - xMethode) <= 1f
                    && ligne.y2() > ligne.y1()) {
                if (guideMethode == null
                        || ligne.epaisseur() > guideMethode.epaisseur()) {
                    guideMethode = ligne;
                }
            }
            if (Math.abs(ligne.x1() - xClasse) <= 1f
                    && Math.abs(ligne.x2() - xClasse) <= 1f
                    && ligne.y2() > ligne.y1()) {
                if (guideClasse == null
                        || ligne.epaisseur() > guideClasse.epaisseur()) {
                    guideClasse = ligne;
                }
            }
        }
        assertNotNull("le guide de la méthode vit", guideMethode);
        assertNotNull("le guide de la classe vit", guideClasse);
        assertEquals("le guide actif porte l'encre du curseur",
                (vue.theme.cursorColor() & 0x00FFFFFF) | 0xCC000000,
                guideMethode.color());
        assertNotEquals("le guide ombre reste une ombre",
                guideMethode.color(), guideClasse.color());
        assertTrue("le guide actif est plus épais",
                guideMethode.epaisseur() > guideClasse.epaisseur());
    }

    /** Le caret sur l'ACCOLADE fermante illumine aussi le guide du
     *  bloc qu'elle ferme — « avant le } ». */
    @Test
    public void leCaretSurLaFermanteIllumineLeGuide() {
        EditorTestRig banc = bancPublie(TEXTE);
        EditorView vue = banc.vue();
        // la fermante de la méthode : ligne 3, colonne 4
        int caret = vue.session().document().lineStart(3) + 4;
        vue.session().setSelection(MultiSelection.of(new SelRange(caret, caret)));
        EditorTestRig.RecordingCanvas image = banc.image();
        float xMethode = vue.gutterWidth() + 4 * vue.metrics.charWidth();
        EditorTestRig.RecordingCanvas.LineDraw guide = null;
        for (EditorTestRig.RecordingCanvas.LineDraw ligne : image.lignes()) {
            if (Math.abs(ligne.x1() - xMethode) <= 1f
                    && Math.abs(ligne.x2() - xMethode) <= 1f
                    && ligne.y2() > ligne.y1()
                    && ligne.color() == ((vue.theme.cursorColor() & 0x00FFFFFF)
                            | 0xCC000000)) {
                guide = ligne;
                break;
            }
        }
        assertNotNull("le guide de la méthode fermée s'illumine", guide);
    }

    /** La couleur attendue de la bande (voile de l'encre de base). */
    private static int couleurBande(EditorTestRig banc) {
        return (banc.vue().theme.baseTextColor() & 0x00FFFFFF) | 0x18000000;
    }
}
