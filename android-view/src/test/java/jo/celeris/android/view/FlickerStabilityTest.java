package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le scintillement de coloration constaté dans la démonstration
 * (M7.5, §6) : à chaque frappe, la ligne touchée perd TOUTE sa couleur
 * jusqu'à la publication suivante — deux états visibles par frappe,
 * les autres mots de la ligne clignotent (PIEGE-26).
 *
 * <p>Écrits ROUGES sur le code courant : entre la frappe et la
 * publication, les caractères non touchés gardent leur couleur ; la
 * publication ne jette pas les mesures des rangées ; deux cents
 * frappes avec analyse retenue ne produisent AUCUNE image sans
 * couleur d'un caractère qui en avait une.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class FlickerStabilityTest {

    private static EditorTestRig bancColore(String contenu) {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), contenu);
        banc.vue().setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        banc.image();
        curseurEnFin(banc);
        return banc;
    }

    private static void curseurEnFin(EditorTestRig banc) {
        banc.vue().session().setSelection(MultiSelection.of(
                SelRange.caretAt(banc.vue().session().document().length())));
    }

    /** Le curseur juste après le nombre de la première ligne (offset 9). */
    private static void curseurApresNombre(EditorTestRig banc) {
        banc.vue().session().setSelection(MultiSelection.of(SelRange.caretAt(9)));
    }

    private static int encreMotCle(EditorTestRig banc) {
        EditorTheme theme = banc.vue().etatImageCourante().theme();
        return theme.syntaxe().couleur(TokenKind.KEYWORD);
    }

    private static int encreBase(EditorTestRig banc) {
        return banc.vue().etatImageCourante().theme().baseTextColor();
    }

    private static RowLayout rangeeCachee(EditorTestRig banc, int rangee) {
        EditorView vue = banc.vue();
        FrameState etat = vue.etatImageCourante();
        return vue.cache.find(etat.revision(), rangee, etat.theme().id(),
                etat.metrics().font(), etat.projection().columns());
    }

    /**
     * PIEGE-26 — l'image entre la frappe et la publication : les mots
     * non touchés de la ligne gardent leur couleur, la frappe suivie
     * de la publication n'en produit aucune sans couleur.
     */
    @Test
    public void noFrameBetweenKeystrokeAndPublicationPaintsUncoloredText() {
        EditorTestRig banc = bancColore("int x = 1;");
        curseurApresNombre(banc);
        int encreMotCle = encreMotCle(banc);
        EditorTestRig.RecordingCanvas imageAvant = banc.image();
        int encreNombre = imageAvant.couleurDe("1");
        assertEquals("avant frappe, le mot-clé est coloré",
                encreMotCle, imageAvant.couleurDe("int"));
        assertNotEquals("avant frappe, le nombre est coloré",
                encreBase(banc), encreNombre);
        // La frappe : « int x = 12; » — la publication ne part PAS.
        banc.validerTexte("2");
        EditorTestRig.RecordingCanvas avantPublication = banc.image();
        assertEquals("entre frappe et publication, int garde sa couleur",
                encreMotCle, avantPublication.couleurDe("int"));
        assertEquals("entre frappe et publication, 1 garde sa couleur",
                encreNombre, avantPublication.couleurDe("1"));
        // La publication arrive : les couleurs restent, rien ne clignote —
        // la sorte d'un jeton peut mûrir (1 → 12), jamais vers l'encre
        // de base : le mot reste coloré d'un bout à l'autre.
        banc.publierAnalyse();
        assertEquals("après publication, int garde sa couleur",
                encreMotCle, banc.image().couleurDe("int"));
        assertNotEquals("après publication, 12 reste coloré",
                encreBase(banc), banc.image().couleurDe("12"));
    }

    /** La composition IME suit la même loi : les mots non touchés tiennent. */
    @Test
    public void compositionKeepsColorsDuringIme() {
        EditorTestRig banc = bancColore("int x = 1;");
        curseurApresNombre(banc);
        int encreMotCle = encreMotCle(banc);
        int encreNombre = banc.image().couleurDe("1");
        banc.composerTexte("2");
        assertEquals("en composition, int garde sa couleur",
                encreMotCle, banc.image().couleurDe("int"));
        assertEquals("en composition, 1 garde sa couleur",
                encreNombre, banc.image().couleurDe("1"));
    }

    /**
     * La publication ne jette pas les mesures : la rangée d'une ligne
     * NON touchée garde la MÊME instance de mise en page à travers la
     * publication qui suit une frappe ailleurs.
     */
    @Test
    public void publicationKeepsRowMeasurementsOfUntouchedLines() {
        EditorTestRig banc = bancColore("int x = 1;\nint y = 2;");
        curseurApresNombre(banc);
        // Frappe sur la fin de la première ligne, image intermédiaire :
        // la rangée 1 se mesure et se dépose à la nouvelle révision.
        banc.validerTexte("2");
        banc.image();
        RowLayout rangee1Avant = rangeeCachee(banc, 1);
        assertNotEquals("la rangée est servie par le cache",
                null, rangee1Avant);
        // La publication arrive : la rangée non touchée garde SON
        // instance — la mesure survit, seule la couleur pouvait bouger.
        banc.publierAnalyse();
        banc.image();
        assertSame("la rangée non touchée garde sa mise en page mesurée",
                rangee1Avant, rangeeCachee(banc, 1));
    }

    /**
     * Deux cents frappes avec analyse ralentie : aucune image ne trace
     * sans couleur un caractère qui en avait une (l'invariant de la
     * section 6, au clavier matériel).
     */
    @Test
    public void twoHundredKeystrokesNeverLoseColor() {
        EditorTestRig banc = bancColore("int valeur = 1000;");
        EditorView vue = banc.vue();
        int encreBase = encreBase(banc);
        int encreNombre = banc.image().couleurDe("1000");
        assertNotEquals("le nombre initial est coloré", encreBase, encreNombre);
        for (int frappe = 0; frappe < 200; frappe++) {
            banc.validerTexte("0");
            EditorTestRig.RecordingCanvas image = banc.image();
            assertNotEquals("frappe " + frappe + " : int reste coloré",
                    encreBase, image.couleurDe("int"));
            assertEquals("frappe " + frappe + " : le nombre garde son encre",
                    encreNombre, image.couleurDe("1000"));
            if (frappe % 10 == 9) {
                banc.publierAnalyse();
                assertNotEquals("publication " + frappe + " : int reste coloré",
                        encreBase, banc.image().couleurDe("int"));
            }
        }
    }
}
