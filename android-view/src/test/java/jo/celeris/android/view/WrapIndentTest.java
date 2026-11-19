package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jo.celeris.core.text.Document;

import org.junit.Test;

/**
 * L'indentation de continuation du retour à la ligne souple (B-05,
 * l'analyse croisée) : les rangées de suite partaient toutes de la
 * colonne zéro — le dépôt de départ rétrécit ses rangées de suite de
 * l'indentation de tête de la ligne. Une ligne indentée qui coupe
 * garde son retrait visuel : la suite démarre à l'indentation, le
 * budget effectif de la ligne s'en réduit.
 */
public class WrapIndentTest {

    /** Document : une ligne indentée de 4, longue, sous budget 10. */
    private static final Document DOC = Document.of(
            "    alpha beta gamma delta\n");

    /** Le comptage des rangées : la ligne de 26 sous budget 10 sans
     *  indent occupe 3 rangées ; avec l'indent de 4, le budget
     *  effectif est 6 et elle en occupe 4 — la projection suit. */
    @Test
    public void leBudgetEffectifRetranchelIndent() {
        VisualProjection projection = new VisualProjection(DOC,
                new FoldModel(), 10);
        assertEquals(4, projection.indentAtRow(0));
        assertEquals(4, projection.indentAtRow(1));
        assertEquals(4, projection.indentAtRow(2));
        // 26 caractères, indent 4, budget effectif 6 : la première
        // rangée porte 10, puis ceil(16/6) = 3 rangées de suite →
        // 4 rangées pour la ligne 0 (la ligne vide finale à part).
        assertEquals(4, projection.rowOfLine(1) - projection.rowOfLine(0));
    }

    /** Le début de tranche suit le budget effectif : la première
     *  rangée porte [0,10), la suivante [10,16), puis [16,22),
     *  [22,26) — les débuts avancent du budget restant, pas du
     *  budget brut. */
    @Test
    public void lesTranchesSuiventLeBudgetEffectif() {
        VisualProjection projection = new VisualProjection(DOC,
                new FoldModel(), 10);
        assertEquals(0, projection.spanStartAtRow(0));
        assertEquals(10, projection.spanStartAtRow(1));
        assertEquals(16, projection.spanStartAtRow(2));
        assertEquals(22, projection.spanStartAtRow(3));
    }

}
