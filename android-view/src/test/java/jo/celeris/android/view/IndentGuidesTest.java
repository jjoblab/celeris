package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * Les guides d'indentation structurels (A-17 relue, H-09 — la
 * structure sert le trait) : un guide par bloc — l'accolade ouvrante
 * donne sa colonne au trait, le trait part de la ligne APRÈS
 * l'ouvrante et meurt à la ligne AVANT la fermante, les lignes vides
 * du corps se joignent à lui. Test de capture : les lignes tracées se
 * capent image par image, bornes comprises.
 */
@RunWith(RobolectricTestRunner.class)
public class IndentGuidesTest {

    private Context contexte;

    private static final String TEXTE = "class A {\n"
            + "    void m() {\n"
            + "        int x = 1;\n"
            + "\n"
            + "        int y = 2;\n"
            + "    }\n"
            + "}\n";

    /** L'accolade dans une chaîne ou un commentaire n'ouvre rien. */
    private static final String PIEGES = "class A {\n"
            + "    String s = \"{\";\n"
            + "    // } dans un commentaire\n"
            + "}\n";

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** L'abscisse du guide d'une colonne, de la vue mesurée. */
    private static float xDeColonne(EditorView vue, int colonne) {
        return vue.gutterWidth() + colonne * vue.metrics.charWidth();
    }

    /** Le segment vertical tracé à l'abscisse donnée, null si aucun. */
    private static EditorTestRig.RecordingCanvas.LineDraw segmentA(
            EditorTestRig.RecordingCanvas image, float x) {
        for (EditorTestRig.RecordingCanvas.LineDraw ligne : image.lignes()) {
            if (Math.abs(ligne.x1() - x) <= 1f
                    && Math.abs(ligne.x2() - x) <= 1f
                    && ligne.y2() > ligne.y1()) {
                return ligne;
            }
        }
        return null;
    }

    /** Le banc prêt : langage branché, analyse publiée, image dessinée. */
    private EditorTestRig bancPublie(String contenu) {
        EditorTestRig banc = EditorTestRig.sur(contexte, contenu);
        banc.vue().setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        return banc;
    }

    /** Un guide par bloc, à la colonne de la ligne qui l'ouvre — le
     *  cran de blancs seul n'ouvre plus rien. */
    @Test
    public void leGuideSuitLeBloc() {
        EditorTestRig banc = bancPublie(TEXTE);
        EditorView vue = banc.vue();
        EditorTestRig.RecordingCanvas image = banc.image();
        assertEquals("un guide pour le bloc de la classe, à sa colonne", 1,
                image.lignesVerticalesA(xDeColonne(vue, 0)));
        assertEquals("un guide pour le bloc de la méthode, à sa colonne", 1,
                image.lignesVerticalesA(xDeColonne(vue, 4)));
        assertEquals("pas de guide au cran sans bloc", 0,
                image.lignesVerticalesA(xDeColonne(vue, 8)));
    }

    /** Le trait part du BAS de la rangée ouvrante et meurt au HAUT de
     *  la rangée fermante — jamais sur l'une ni l'autre. */
    @Test
    public void leTraitPartSousLOuvranteEtMeurtAvantLaFermante() {
        EditorTestRig banc = bancPublie(TEXTE);
        EditorView vue = banc.vue();
        EditorTestRig.RecordingCanvas image = banc.image();
        EditorTestRig.RecordingCanvas.LineDraw classe =
                segmentA(image, xDeColonne(vue, 0));
        assertNotNull("le guide de la classe se trace", classe);
        assertEquals("le trait part sous la rangée ouvrante",
                vue.metrics.rowTop(0) + vue.metrics.lineHeight(),
                classe.y1(), 0.5f);
        assertEquals("le trait meurt au-dessus de la rangée fermante",
                vue.metrics.rowTop(6), classe.y2(), 0.5f);
        EditorTestRig.RecordingCanvas.LineDraw methode =
                segmentA(image, xDeColonne(vue, 4));
        assertNotNull("le guide de la méthode se trace", methode);
        assertEquals(vue.metrics.rowTop(1) + vue.metrics.lineHeight(),
                methode.y1(), 0.5f);
        assertEquals(vue.metrics.rowTop(5), methode.y2(), 0.5f);
    }

    /** La ligne vide du corps se joint au trait : le guide la traverse,
     *  un seul segment continu par bloc. */
    @Test
    public void leGuideTraverseLesLignesVides() {
        EditorTestRig banc = bancPublie("{\n\n}\n");
        EditorView vue = banc.vue();
        // le curseur vit dans la ligne vide : le miroir des accolades
        // ne se mêle pas du trait du guide
        vue.session.setSelection(MultiSelection.of(SelRange.caretAt(2)));
        EditorTestRig.RecordingCanvas image = banc.image();
        EditorTestRig.RecordingCanvas.LineDraw guide =
                segmentA(image, xDeColonne(vue, 0));
        assertNotNull("le guide traverse la ligne vide", guide);
        assertEquals(vue.metrics.rowTop(0) + vue.metrics.lineHeight(),
                guide.y1(), 0.5f);
        assertEquals("la ligne vide seule se couvre d'un trait entier",
                vue.metrics.rowTop(2), guide.y2(), 0.5f);
    }

    /** Le pli replié ne casse pas le guide : la rangée du pli se
     *  traverse — le bloc continue de se lire d'une ligne. */
    @Test
    public void leGuideTraverseLePli() {
        EditorTestRig banc = bancPublie(TEXTE);
        EditorView vue = banc.vue();
        vue.plis.fold(2, 4);
        EditorTestRig.RecordingCanvas image = banc.image();
        assertEquals("le guide de la classe traverse le pli", 1,
                image.lignesVerticalesA(xDeColonne(vue, 0)));
        assertEquals("le guide de la méthode vit dans le pli", 1,
                image.lignesVerticalesA(xDeColonne(vue, 4)));
    }

    /** L'accolade dans une chaîne ou un commentaire n'ouvre rien : la
     *  structure décide, pas le caractère. */
    @Test
    public void laccoladeDansUneChaineOuUnCommentaireNouvreRien() {
        EditorTestRig banc = bancPublie(PIEGES);
        EditorView vue = banc.vue();
        EditorTestRig.RecordingCanvas image = banc.image();
        assertEquals("le bloc de la classe seul se guide", 1,
                image.lignesVerticalesA(xDeColonne(vue, 0)));
        assertEquals("l'accolade dans la chaîne n'ouvre aucun guide", 0,
                image.lignesVerticalesA(xDeColonne(vue, 4)));
    }

    /** Sans langage, pas de structure — pas de guide. */
    @Test
    public void sansLangueAucunGuide() {
        EditorTestRig banc = EditorTestRig.sur(contexte, TEXTE);
        EditorView vue = banc.vue();
        EditorTestRig.RecordingCanvas image = banc.image();
        assertEquals("sans structure, rien à aligner", 0,
                image.lignesVerticalesA(xDeColonne(vue, 0)));
        assertEquals(0, image.lignesVerticalesA(xDeColonne(vue, 4)));
    }

    /** Les accolades d'une même ligne n'ouvrent aucun guide : rien à
     *  couvrir entre elles. */
    @Test
    public void lesAccoladesDeLaMemeLigneNouvrentRien() {
        EditorTestRig banc = bancPublie("{ int a; }\n");
        EditorView vue = banc.vue();
        // le curseur vit loin des bornes : le miroir des accolades ne
        // se mêle pas du compte du guide
        vue.session.setSelection(MultiSelection.of(SelRange.caretAt(7)));
        EditorTestRig.RecordingCanvas image = banc.image();
        assertEquals("rien à couvrir sur une seule ligne", 0,
                image.lignesVerticalesA(xDeColonne(vue, 0)));
    }

    /** La tabulation compte à son cran : la ligne ouvrante à la
     *  tabulation porte son guide au taquet. */
    @Test
    public void laTabulationCompteASonCran() {
        EditorTestRig banc = bancPublie("class A {\n"
                + "\tvoid m() {\n"
                + "\t\tint x = 1;\n"
                + "\t}\n"
                + "}\n");
        EditorView vue = banc.vue();
        EditorTestRig.RecordingCanvas image = banc.image();
        assertEquals("la tabulation porte le guide de la méthode au taquet", 1,
                image.lignesVerticalesA(xDeColonne(vue, 4)));
        assertEquals("la classe reste à la colonne zéro", 1,
                image.lignesVerticalesA(xDeColonne(vue, 0)));
    }

    /** L'édition remappe la table : le guide survit au vol de
     *  publication, sa borne basse suit la fermante décalée — puis la
     *  publication reprend la vérité sans que le trait ne bouge. */
    @Test
    public void leGuideSurvitALeditionSansPublication() {
        EditorTestRig banc = bancPublie(TEXTE);
        EditorView vue = banc.vue();
        assertEquals("le guide de la méthode se trace avant l'édition", 1,
                banc.image().lignesVerticalesA(xDeColonne(vue, 4)));
        Document document = vue.session.document();
        int point = document.lineEnd(2);
        vue.session.replaceRange(point, point, "\n");
        EditorTestRig.RecordingCanvas image = banc.image();
        assertEquals("le guide survit au vol de publication", 1,
                image.lignesVerticalesA(xDeColonne(vue, 4)));
        EditorTestRig.RecordingCanvas.LineDraw guide =
                segmentA(image, xDeColonne(vue, 4));
        assertNotNull(guide);
        assertEquals("la borne basse a suivi la fermante décalée",
                vue.metrics.rowTop(6), guide.y2(), 0.5f);
        banc.publierAnalyse();
        assertEquals("la publication reprend la même vérité", 1,
                banc.image().lignesVerticalesA(xDeColonne(vue, 4)));
    }
}
