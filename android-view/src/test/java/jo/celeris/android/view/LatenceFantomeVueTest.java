package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.ai.SuggestionFantome;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;

/**
 * Le budget de latence d'affichage du texte fantôme (F-01, ADR-012) :
 * le tracé éphémère se consigne avec le rendu — la mesure au minimum
 * de trois tours (PIEGE-13), l'assertion sur le budget d'une image à
 * soixante hertz (16 ms par tracé).
 */
class LatenceFantomeVueTest {

    /** Surface muette : le tracé mesure, l'enregistrement ne sert pas. */
    private static final class SurfaceMuette implements DrawSurface {

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            // muette — la mesure veut le coût du tracé, pas le journal
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            // muette
        }
    }

    /** Pinceau muet : la couleur se pose, rien d'autre. */
    private static final class PinceauMuet implements TextBrush {
        @Override
        public void setColor(int argb) {
            // muet
        }

        @Override
        public int color() {
            return 0;
        }
    }

    /** Fabrique : un pinceau neuf par demande. */
    private static final class FabriqueMuette implements BrushFactory {
        @Override
        public TextBrush newBrush() {
            return new PinceauMuet();
        }
    }

    /** Règle fixe : largeur 10, hauteur de ligne 20. */
    private static final class RegleFixe implements FontRuler {
        @Override
        public float charWidth(FontSpec spec) {
            return 10f;
        }

        @Override
        public float lineHeight(FontSpec spec) {
            return 20f;
        }

        @Override
        public float ascent(FontSpec spec) {
            return 15f;
        }

        @Override
        public float descent(FontSpec spec) {
            return 5f;
        }
    }

    /**
     * F-01 — le budget d'affichage : mille tracés du fantôme, minimum
     * de trois tours, chaque tracé sous le budget d'une image à
     * soixante hertz (16 ms) — le chemin est fait d'appels purs sur
     * l'état d'image, la protection est structurelle.
     */
    @Test
    void milleTracesSousLeBudgetDUneImage() {
        FantomePainter peintre = new FantomePainter(new FabriqueMuette());
        peintre.setSuggestion(new SuggestionFantome(
                "lireAuClavier()\n    .premier()\n    .second()", 14, 0));
        SurfaceMuette surface = new SurfaceMuette();
        Document document = Document.of("val reponse = \n");
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 0L,
                metrics, EditorThemes.CLAIR, 0, 4, 40f, 0f, 0f);

        long minimum = minDeTroisTours(() -> {
            for (int i = 0; i < 1_000; i++) {
                peintre.paint(surface, etat);
            }
        });
        System.out.printf("[benchmark] fantome : 1000 tracés au chemin des"
                        + " indices (min de 3 tours) : %.3f ms par tracé%n",
                minimum / 1000.0 / 1e6);
        assertTrue(minimum < 16_000L * 1_000_000L,
                "mille tracés dépassent le budget d'une image chacun : "
                        + (minimum / 1000.0 / 1e6) + " ms par tracé (min de 3 tours)");
    }

    /**
     * Le protocole PIEGE-13 : trois tours de la phase, minimum
     * conservé — le bruit du runner ne fait qu'ajouter du temps, le
     * minimum l'écarte.
     */
    private static long minDeTroisTours(Runnable phase) {
        long meilleur = Long.MAX_VALUE;
        for (int tour = 0; tour < 3; tour++) {
            long debut = System.nanoTime();
            phase.run();
            long duree = System.nanoTime() - debut;
            if (duree < meilleur) {
                meilleur = duree;
            }
        }
        return meilleur;
    }
}
