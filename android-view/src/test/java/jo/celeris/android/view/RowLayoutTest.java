package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * La mise en page d'une rangée visuelle : les invariants de la
 * tranche (bornes croissantes, ligne positive, listes jamais nulles)
 * et les chemins rapides du peintre — segments et plages vidés
 * signalent les tracés simples.
 */
class RowLayoutTest {

    /** La rangée saine porte sa tranche, son texte et sa largeur. */
    @Test
    void laRangeeSainePorteSaTranche() {
        RowLayout rangee = new RowLayout(3, 0, 5, "alpha", 50f);
        assertEquals(3, rangee.line());
        assertEquals(0, rangee.start());
        assertEquals(5, rangee.end());
        assertEquals("alpha", rangee.text());
        assertEquals(50f, rangee.width(), 1e-6f);
    }

    /** Une ligne négative refuse : les lignes se comptent depuis zéro. */
    @Test
    void laLigneNegativeRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> new RowLayout(-1, 0, 5, "alpha", 50f),
                "negative line");
    }

    /** Un début négatif refuse : la tranche vit dans la ligne. */
    @Test
    void leDebutNegatifRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> new RowLayout(0, -1, 5, "alpha", 50f),
                "negative start");
    }

    /**
     * Une fin avant le début refuse (la fin est exclusive) ; une fin
     * ÉGALE au début reste légale — la tranche vide existe, la ligne
     * vide reste une rangée.
     */
    @Test
    void laFinAvantLeDebutRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> new RowLayout(0, 5, 4, "alpha", 50f),
                "inverted span");
        // La tranche vide (fin égale au début) est légale : elle porte
        // le début de la ligne vide, jamais une exception.
        assertEquals(0, new RowLayout(0, 0, 0, "", 0f).end());
    }

    /** Une tranche vide au début de ligne (début 0, fin 0) est légale. */
    @Test
    void laTrancheVideAuDebutEstLegale() {
        RowLayout vide = new RowLayout(0, 0, 0, "", 0f);
        assertEquals(0, vide.end());
        assertEquals("", vide.text());
    }

    /**
     * Le constructeur court vide les segments et les plages : les
     * chemins rapides du peintre — un seul tracé, l'encre de base.
     */
    @Test
    void leConstructeurCourtVideSegmentsEtPlages() {
        RowLayout rangee = new RowLayout(0, 0, 5, "alpha", 50f);
        assertEquals(List.of(), rangee.runs(),
                "segments vidés : rangée purement gauche-à-droite");
        assertEquals(List.of(), rangee.spans(),
                "plages vidées : rangée à l'encre de base");
    }

    /**
     * Le constructeur à segments vide les plages : la directionnalité
     * sans coloration reste un chemin rapide d'encre.
     */
    @Test
    void leConstructeurASegmentsVideLesPlages() {
        List<VisualRuns.Run> runs = List.of(
                new VisualRuns.Run(0, 3, false), new VisualRuns.Run(3, 5, true));
        RowLayout rangee = new RowLayout(0, 0, 5, "alpha", 50f, runs);
        assertEquals(2, rangee.runs().size());
        assertEquals(List.of(), rangee.spans(),
                "plages vidées : l'encre de base seule");
    }

    /** Les listes nulles refusent : les chemins rapides sont des vides, pas des nuls. */
    @Test
    void lesListesNullesRefusent() {
        assertThrows(IllegalArgumentException.class,
                () -> new RowLayout(0, 0, 5, "alpha", 50f, null, List.of()),
                "null runs");
        assertThrows(IllegalArgumentException.class,
                () -> new RowLayout(0, 0, 5, "alpha", 50f, List.of(), null),
                "null spans");
    }
}
