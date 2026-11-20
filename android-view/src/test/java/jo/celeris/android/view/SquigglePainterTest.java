package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.SyntaxIssue;

/**
 * D-05/D-06 (l'analyse croisée) : le squiggle sous les plages en
 * diagnostic — la dentelle se trace après le texte, sa TEINTE suit
 * la sévérité (rouge l'erreur, ambre l'avertissement, gris
 * l'information).
 *
 * <p>Le compteur du banc : les segments tracés à la teinte de
 * sévérité, lus dans les lignes du canvas enregistreur — chaque
 * ondulation est un drawLine coloré.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class SquigglePainterTest {

    /** Les teintes de sévérité (D-06) — miroir de SquigglePainter. */
    private static final int ROUGE = 0xFFF44336;
    private static final int AMBRE = 0xFFFFB300;
    private static final int GRIS = 0xFF9E9E9E;

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** Le nombre de segments tracés à la teinte donnée. */
    private int segments(EditorTestRig.RecordingCanvas image, int teinte) {
        int n = 0;
        for (EditorTestRig.RecordingCanvas.LineDraw ligne : image.lignes()) {
            if (ligne.color() == teinte) {
                n++;
            }
        }
        return n;
    }

    /** Le banc posé : java branché, l'analyse publiée (les issues de
     *  l'arbre y vivent), une image dessinée. */
    private EditorTestRig.RecordingCanvas banc(String contenu) {
        EditorTestRig banc = EditorTestRig.sur(contexte, contenu);
        banc.vue().setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        banc.vue().invalidate();
        return banc.image();
    }

    /** Le XML mal fermé porte ses squiggles rouges : la structure
     *  cassée se voit sous le texte, pas seulement en gouttière. */
    @Test
    public void laMalformationSeVoitSousLeTexte() {
        Context contexte = RuntimeEnvironment.getApplication();
        EditorTestRig banc = EditorTestRig.sur(contexte,
                "<a>\n  <b>\n</a>\n");
        banc.vue().setLanguage(LanguageSupport.of(
                jo.celeris.core.syntax.xml.XmlSyntax.INSTANCE));
        banc.publierAnalyse();
        banc.vue().invalidate();
        EditorTestRig.RecordingCanvas image = banc.image();
        assertTrue("des squiggles rouges se tracent",
                segments(image, ROUGE) > 0);
    }

    /** Le document sain ne trace rien : pas de dentelle sans diagnostic. */
    @Test
    public void leDocumentSainNeTraceRien() {
        Context contexte = RuntimeEnvironment.getApplication();
        EditorTestRig banc = EditorTestRig.sur(contexte, "<a>\n  <b/>\n</a>\n");
        banc.vue().setLanguage(LanguageSupport.of(
                jo.celeris.core.syntax.xml.XmlSyntax.INSTANCE));
        banc.publierAnalyse();
        banc.vue().invalidate();
        EditorTestRig.RecordingCanvas image = banc.image();
        assertEquals("aucun squiggle sans erreur", 0, segments(image, ROUGE));
    }

    /** La teinte suit la sévérité (D-06) : les issues importées
     *  WARNING se tracent en ambre, pas en rouge. */
    @Test
    public void laTeinteSuitLaSeverite() {
        EditorTestRig banc = EditorTestRig.sur(contexte,
                "class A {\n    int x = 1;\n}\n");
        banc.vue().setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        // les diagnostics de l'hôte : un avertissement importé sur la
        // ligne de la déclaration (la voie C-12 d'avecErreurs)
        // la sévérité se lit par les issues de la syntaxe : le banc
        // y pose une issue WARNING (la voie des producteurs externes
        // — lint du build, LSP — qui posent LEUR sévérité, D-06)
        banc.vue().syntaxe.poserIssuesDeBanc(java.util.List.of(
                new SyntaxIssue(13, 5, "variable inutilisee",
                        SyntaxIssue.Severity.WARNING)));
        banc.vue().invalidate();
        EditorTestRig.RecordingCanvas image = banc.image();
        assertTrue("le squiggle ambre de l'avertissement se trace",
                segments(image, AMBRE) > 0);
        assertEquals("et pas de rouge pour un simple avertissement",
                0, segments(image, ROUGE));
    }
}
