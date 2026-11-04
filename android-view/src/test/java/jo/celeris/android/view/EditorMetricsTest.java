package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests des métriques d'éditeur dérivées d'une règle de police injectée
 * (ADR-008 : la mesure de police est injectée, jamais lue directement —
 * les tests restent déterministes).
 *
 * <p>PIEGE-05 : dans le dépôt de départ, les métriques de police étaient
 * recalculées à chaque changement de taille, invalidant les valeurs
 * injectées dans les tests. La discipline : les valeurs dérivées se
 * reconsultent auprès de la règle pour la nouvelle taille après tout
 * changement, et les tests réinjectent leurs valeurs après ce changement
 * — jamais un réemploi silencieux de l'état d'avant.</p>
 */
class EditorMetricsTest {

    /**
     * Règle proportionnelle déterministe : largeur = 0,6 × taille, hauteur
     * de ligne = 1,25 × taille, ascendance = taille, descendance = quart.
     * Elle enregistre chaque spécification consultée.
     */
    private static final class RegleProportionnelle implements FontRuler {
        private final List<FontSpec> consultations = new ArrayList<>();

        @Override
        public float charWidth(FontSpec spec) {
            consultations.add(spec);
            return spec.size() * 0.6f;
        }

        @Override
        public float lineHeight(FontSpec spec) {
            return spec.size() * 1.25f;
        }

        @Override
        public float ascent(FontSpec spec) {
            return spec.size();
        }

        @Override
        public float descent(FontSpec spec) {
            return spec.size() * 0.25f;
        }

        boolean aConsulte(FontSpec spec) {
            return consultations.contains(spec);
        }
    }

    /**
     * PIEGE-05 — un changement de taille de police invalide les métriques
     * dérivées : elles se recalculent par consultation de la règle pour la
     * nouvelle taille.
     *
     * <p>Les métriques d'avant restent exactes pour leur propre taille
     * (aucune instance n'est mutée en place) : la voie sûr après un
     * changement de taille est de réinjecter la règle et de redériver.</p>
     */
    @Test
    void editorMetrics_fontSizeChangeInvalidatesDerivedMetrics() {
        RegleProportionnelle regle = new RegleProportionnelle();
        EditorMetrics petites = EditorMetrics.of(new FontSpec("mono", 14f), regle);
        assertEquals(8.4f, petites.charWidth(), 1e-6f);
        assertEquals(17.5f, petites.lineHeight(), 1e-6f);

        // Le changement de taille invalide les valeurs dérivées d'avant :
        // la règle est reconsultée pour la nouvelle taille, jamais contournée.
        EditorMetrics grandes = EditorMetrics.of(new FontSpec("mono", 16f), regle);
        assertEquals(9.6f, grandes.charWidth(), 1e-6f);
        assertNotEquals(petites.charWidth(), grandes.charWidth());
        assertTrue(regle.aConsulte(new FontSpec("mono", 16f)),
                "la règle doit être reconsultée pour la nouvelle taille");

        // Les valeurs d'avant ne survivent pas au changement par mutation :
        // l'instance d'avant reste liée à sa propre taille.
        assertEquals(14f, petites.font().size());
        assertEquals(8.4f, petites.charWidth(), 1e-6f);
    }

    /** Les valeurs dérivées proviennent toutes de la règle injectée. */
    @Test
    void valeursDeriveesDeLaRegle() {
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleProportionnelle());
        assertEquals(14f, metrics.ascent(), 1e-6f);
        assertEquals(3.5f, metrics.descent(), 1e-6f);
        assertEquals(17.5f, metrics.lineHeight(), 1e-6f);
        assertEquals(8.4f, metrics.charWidth(), 1e-6f);
    }

    /** Les rangées s'empilent à la hauteur de ligne, ligne de base comprise. */
    @Test
    void rangeursAlignesSurLaHauteurDeLigne() {
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleProportionnelle());
        assertEquals(0f, metrics.rowTop(0), 1e-6f);
        assertEquals(17.5f, metrics.rowTop(1), 1e-6f);
        assertEquals(35f, metrics.rowTop(2), 1e-6f);
        assertEquals(14f, metrics.rowBaseline(0), 1e-6f);
        assertEquals(31.5f, metrics.rowBaseline(1), 1e-6f);
        assertEquals(49f, metrics.rowBaseline(2), 1e-6f);
    }
}
