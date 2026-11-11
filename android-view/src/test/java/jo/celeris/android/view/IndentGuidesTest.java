package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Les guides d'indentation (chantier C13, A-17) : les traits verticaux
 * alignés aux niveaux — un guide par cran de quatre colonnes, de la
 * première à la dernière rangée de chaque niveau, les rangées
 * contiguës d'un même niveau se joignant en une ligne continue. Le pli
 * replié compte : le guide traverse le pli au niveau le plus faible de
 * son bouquet. Test de capture : les lignes tracées se captent image
 * par image.
 */
@RunWith(RobolectricTestRunner.class)
public class IndentGuidesTest {

    private Context contexte;

    private static final String TEXTE = "class A {\n"
            + "    void m() {\n"
            + "        int x = 1;\n"
            + "        int y = 2;\n"
            + "    }\n"
            + "}\n";

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** L'abscisse du guide d'un niveau, de la vue mesurée. */
    private static float xDuNiveau(EditorView vue, int niveau) {
        return vue.gutterWidth()
                + (niveau * IndentGuides.UNIT - 0.5f) * vue.metrics.charWidth();
    }

    /** Les guides s'alignent aux niveaux : un trait vertical par cran,
     *  de la première à la dernière rangée du niveau. */
    @Test
    public void guidesAlignesAuxNiveaux() {
        EditorTestRig banc = EditorTestRig.sur(contexte, TEXTE);
        EditorView vue = banc.vue();
        EditorTestRig.RecordingCanvas image = banc.image();
        // niveau 1 (4 colonnes) : les lignes 1 à 4 → quatre segments
        assertEquals("le guide du niveau 1 traverse ses quatre lignes", 4,
                image.lignesVerticalesA(xDuNiveau(vue, 1)));
        // niveau 2 (8 colonnes) : les lignes 2 et 3 → deux segments
        assertEquals("le guide du niveau 2 traverse ses deux lignes", 2,
                image.lignesVerticalesA(xDuNiveau(vue, 2)));
        // niveau 3 : aucun — la ligne la plus creuse n'y va que de deux
        assertEquals("pas de guide au niveau vide", 0,
                image.lignesVerticalesA(xDuNiveau(vue, 3)));
    }

    /** Le pli replié compte : le guide traverse le pli au niveau le plus
     *  faible de son bouquet. */
    @Test
    public void leGuideTraverseLePli() {
        EditorTestRig banc = EditorTestRig.sur(contexte, TEXTE);
        EditorView vue = banc.vue();
        // la ligne 2 replie les lignes 2 et 3 : l'en-tête reste, la
        // rangée couvre le bouquet
        vue.plis.fold(2, 3);
        EditorTestRig.RecordingCanvas image = banc.image();
        // les rangées visibles : 0, 1, 2 (le pli), 4, 5 — le niveau 1
        // vit sur les lignes 1, 2 (le pli) et 4 → trois segments
        assertEquals("le guide du niveau 1 traverse le pli", 3,
                image.lignesVerticalesA(xDuNiveau(vue, 1)));
        // le niveau 2 vit sur le pli seul → un segment
        assertEquals("le guide du niveau 2 vit dans le pli", 1,
                image.lignesVerticalesA(xDuNiveau(vue, 2)));
    }

    /** La tabulation compte à son cran : une tabulation vaut un niveau. */
    @Test
    public void laTabulationCompteASonCran() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "\tint x;\n");
        EditorView vue = banc.vue();
        EditorTestRig.RecordingCanvas image = banc.image();
        assertEquals("la tabulation ouvre le niveau 1", 1,
                image.lignesVerticalesA(xDuNiveau(vue, 1)));
    }

    /** Un document sans indentation ne trace aucun guide. */
    @Test
    public void sansIndentationAucunGuide() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "a\nb\nc\n");
        EditorView vue = banc.vue();
        EditorTestRig.RecordingCanvas image = banc.image();
        assertEquals("rien à aligner, rien à tracer", 0,
                image.lignesVerticalesA(xDuNiveau(vue, 1)));
    }
}
