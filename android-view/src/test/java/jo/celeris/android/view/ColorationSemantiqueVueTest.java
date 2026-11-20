package jo.celeris.android.view;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.clike.JavaSyntax;

/**
 * Le banc d'intégration de la coloration sémantique (ADR-0027, B1) :
 * les jetons du serveur, posés par la voie publique, RAFFINENT les
 * rangées servies — la preuve de bout en bout : pose des jetons,
 * frame, lecture du cache (ADR-013), plages raffinées aux
 * intersections. Le périmé retombe à la syntaxe (PIEGE-20), l'effacé
 * aussi.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ColorationSemantiqueVueTest {

    /** Le montage : une vue JAVA mesurée — la syntaxe colore les
     *  identificateurs, la surcouche s'y posera. */
    private EditorTestRig banc(String text) {
        EditorTestRig rig = EditorTestRig.on(
                RuntimeEnvironment.getApplication(), text);
        rig.vue().setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        rig.vue().layout(0, 0, 1080, 1920);
        rig.publierAnalyse();
        rig.image();
        return rig;
    }

    private RowLayout rangee(EditorView vue, int rangee) {
        long revision = vue.session().document().revision();
        RowLayout servie = vue.cache.find(revision, rangee, vue.theme.id(),
                vue.metrics.font(), vue.projection.columns());
        assertNotNull("la frame a rempli le cache", servie);
        return servie;
    }

    /** La plage d'une rangée qui couvre l'offset absolu donné. */
    private ColoredSpan plageCouvrant(RowLayout servie, int offsetAbsolu) {
        for (ColoredSpan plage : servie.spans()) {
            if (servie.start() + plage.start() <= offsetAbsolu
                    && offsetAbsolu < servie.start() + plage.end()) {
                return plage;
            }
        }
        return null;
    }

    @Test
    public void leJetonPoseRaffineLaRangeeServie() {
        // « valeur » en colonne 4 de la ligne 0 : la syntaxe la voit
        // IDENTIFIER ; le serveur la voit « variable » — le raffinement
        // sémantique prend la part couverte, en KEYWORD… non : variable
        // raffine en IDENTIFIER. Prenons un type : « String » en 0-6,
        // le serveur le dit « class » — TYPE, déjà la sorte attendue.
        // La preuve nette : un PARAMÈTRE que la syntaxe voit KEYWORD
        // n'existe pas — la voici : « int » (mot réservé) que le
        // serveur dit « variable » : la part couverte devient
        // IDENTIFIER, la preuve du raffinement.
        EditorTestRig rig = banc("int valeur = 3;\n");
        EditorView vue = rig.vue();
        RowLayout avant = rangee(vue, 0);
        ColoredSpan plageInt = plageCouvrant(avant, 1);
        assertNotNull("la syntaxe a coloré « int »", plageInt);
        assertEquals(TokenKind.KEYWORD, plageInt.kind(),
                "la syntaxe voit « int » en mot réservé");

        // le serveur pose « int » (offsets 0-3) comme VARIABLE
        long revision = vue.session().document().revision();
        vue.setSemanticTokens(new int[] {0, 3}, new int[] {0},
                List.of("variable"), revision);
        rig.image();

        RowLayout apres = rangee(vue, 0);
        ColoredSpan raffinee = plageCouvrant(apres, 1);
        assertEquals(TokenKind.IDENTIFIER, raffinee.kind(),
                "le jeton sémantique raffine la part couverte");
    }

    @Test
    public void leJetonPerimeNeColorePasLaRevisionPlusRecente() {
        EditorTestRig rig = banc("int valeur = 3;\n");
        EditorView vue = rig.vue();
        RowLayout avant = rangee(vue, 0);

        // le serveur a vu la révision courante, PUIS le document édite
        long revisionServie = vue.session().document().revision();
        vue.setSemanticTokens(new int[] {0, 3}, new int[] {0},
                List.of("variable"), revisionServie);
        assertTrue(vue.session().replaceRange(0, 0, "x"));
        rig.image();

        RowLayout apres = rangee(vue, 0);
        ColoredSpan plage = plageCouvrant(apres, 1);
        assertEquals(TokenKind.KEYWORD, plage.kind(),
                "la publication périmée ne colore pas la révision neuve");
    }

    @Test
    public void laPoseVideEffaceLaSurcouche() {
        EditorTestRig rig = banc("int valeur = 3;\n");
        EditorView vue = rig.vue();
        long revision = vue.session().document().revision();
        vue.setSemanticTokens(new int[] {0, 3}, new int[] {0},
                List.of("variable"), revision);
        rig.image();
        RowLayout raffinee = rangee(vue, 0);
        assertEquals(TokenKind.IDENTIFIER,
                plageCouvrant(raffinee, 1).kind());

        // la pose vide (débranchement du serveur) : la syntaxe seule
        vue.setSemanticTokens(new int[0], new int[0], List.of(), -1L);
        rig.image();
        RowLayout effacee = rangee(vue, 0);
        assertEquals(TokenKind.KEYWORD, plageCouvrant(effacee, 1).kind(),
                "la pose vide rend la coloration syntaxique seule");
    }
}
