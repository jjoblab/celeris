package jo.celeris.android.host.popups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.SyntaxCoordinator;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.TextSession;

/**
 * Le plan de génération chez l'orchestrateur (le déménagement du
 * 2026-11-14, la cinquième surface portée — reprise de
 * {@code AtelierGabaritsTest} de la démonstration) : le plan
 * d'insertion du code généré se pose avant l'accolade fermante de la
 * classe, le texte gagne son cran de membre, le caret se porte à sa
 * suite — la classe inconnue ne propose rien. Pur, sans appareil.
 */
final class GabaritsPlanTest {

    private static final String CLASSE = """
            class Exemple {
                int total;
            }
            """;

    @Test
    void lePlanGenereSinsereAvantLaFermante() {
        TextSession session = TextSession.of(CLASSE, () -> 0L);
        EditPlan plan = surAnalyse(session, analyse -> Gabarits.planGenere(
                session.document(), analyse, "Exemple",
                "public Exemple() {\n}"));
        assertNotNull(plan, "la classe connue a son plan");
        assertTrue(session.apply(plan.transaction(), plan.selection()),
                "la transaction s'applique");
        assertEquals("""
                class Exemple {
                    int total;

                    public Exemple() {
                    }
                }
                """, session.document().text(),
                "le généré se pose avant la fermante, à son cran de membre");
        session.undo();
        assertEquals(CLASSE, session.document().text(),
                "un geste défait la génération");
    }

    @Test
    void laClasseInconnueNeGenereRien() {
        TextSession session = TextSession.of(CLASSE, () -> 0L);
        surAnalyse(session, analyse -> {
            assertNull(Gabarits.planGenere(session.document(), analyse,
                            "Inconnue", "public Inconnue() {\n}"),
                    "le généré ne se devine jamais sur une classe absente");
            assertNull(Gabarits.planGenere(session.document(), analyse,
                            null, "rien"),
                    "sans classe englobante, rien ne se génère");
            return null;
        });
        assertEquals(CLASSE, session.document().text(),
                "le document ne bouge pas");
    }

    /** Le calcul mené sur une analyse fraîche — coordinateur ponctuel,
     *  fermé d'entrée. */
    private <T> T surAnalyse(TextSession session,
            Function<Analysis, T> calcul) {
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                JavaSyntax.INSTANCE);
        try {
            return calcul.apply(coordinateur.analysis());
        } finally {
            coordinateur.close();
        }
    }
}
