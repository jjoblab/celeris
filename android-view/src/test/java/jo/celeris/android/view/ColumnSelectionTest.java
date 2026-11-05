package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * La sélection en colonne (A-02, M5) : le glissement rectangulaire
 * produit une plage par rangée, aux bornes visuelles exactes — la
 * colonne de départ à la colonne d'arrivée, bornées à la longueur de
 * chaque ligne. La rangée courte s'arrête à sa fin ; l'ordre du
 * glissement n'y change rien.
 */
class ColumnSelectionTest {

    /** Dix lignes, une colonne : une plage par rangée, bornes exactes (A-02). */
    @Test
    void colonneSurDixLignes() {
        StringBuilder texte = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            texte.append("ligne").append(i).append("abcdef\n");
        }
        Document doc = Document.of(texte.toString());
        VisualProjection projection = new VisualProjection(doc, new FoldModel(), 0);
        MultiSelection colonne = ColumnSelection.ranges(doc, projection,
                0, 2, 9, 5);
        List<SelRange> plages = colonne.ranges();
        assertEquals(10, plages.size());
        for (int i = 0; i < 10; i++) {
            int debutLigne = doc.lineStart(i);
            assertEquals(debutLigne + 2, plages.get(i).start(),
                    "début de la rangée " + i);
            assertEquals(debutLigne + 5, plages.get(i).end(),
                    "fin de la rangée " + i);
        }
    }

    /** La ligne courte borne sa fin à sa longueur, la plage reste valide. */
    @Test
    void laLigneCourteBorneSaFin() {
        Document doc = Document.of("abc\na\nabcdef\n");
        VisualProjection projection = new VisualProjection(doc, new FoldModel(), 0);
        MultiSelection colonne = ColumnSelection.ranges(doc, projection,
                0, 1, 2, 4);
        List<SelRange> plages = colonne.ranges();
        assertEquals(3, plages.size());
        // « abc » : [1, 3)
        assertEquals(1, plages.get(0).start());
        assertEquals(3, plages.get(0).end());
        // « a » : [5, 5) — la colonne 4 n'existe pas, la ligne s'arrête en 1
        assertEquals(5, plages.get(1).start());
        assertEquals(5, plages.get(1).end());
        // « abcdef » : [7, 10)
        assertEquals(7, plages.get(2).start());
        assertEquals(10, plages.get(2).end());
    }

    /** Le glissement inversé (du bas vers le haut) produit les mêmes plages. */
    @Test
    void glissementInverseMemesPlages() {
        Document doc = Document.of("abcd\nefgh\n");
        VisualProjection projection = new VisualProjection(doc, new FoldModel(), 0);
        MultiSelection aller = ColumnSelection.ranges(doc, projection, 0, 1, 1, 3);
        MultiSelection retour = ColumnSelection.ranges(doc, projection, 1, 3, 0, 1);
        assertEquals(aller.ranges(), retour.ranges());
    }

    /** La colonne au-delà de toutes les lignes : des plages vides en fin de ligne. */
    @Test
    void colonneAuDelaDesLignes() {
        Document doc = Document.of("ab\ncd\n");
        VisualProjection projection = new VisualProjection(doc, new FoldModel(), 0);
        MultiSelection colonne = ColumnSelection.ranges(doc, projection,
                0, 5, 1, 6);
        List<SelRange> plages = colonne.ranges();
        assertEquals(2, plages.size());
        assertEquals(2, plages.get(0).start());
        assertEquals(2, plages.get(0).end());
        assertEquals(5, plages.get(1).start());
        assertEquals(5, plages.get(1).end());
    }

    /** Le retour à la ligne souple : la colonne vit dans la tranche de la rangée. */
    @Test
    void trancheDuRetourALaLigneSouple() {
        // deux rangées par ligne avec une largeur de quatre colonnes
        Document doc = Document.of("abcdef\nghijkl\n");
        VisualProjection projection = new VisualProjection(doc, new FoldModel(), 4);
        // deux rangées par ligne, plus la rangée de la ligne vide finale
        assertEquals(5, projection.rowCount());
        MultiSelection colonne = ColumnSelection.ranges(doc, projection,
                0, 1, 1, 3);
        List<SelRange> plages = colonne.ranges();
        assertEquals(2, plages.size());
        // rangée 0 = « abcd » : [1, 3)
        assertEquals(1, plages.get(0).start());
        assertEquals(3, plages.get(0).end());
        // rangée 1 = « ef » (la tranche est courte) : [5, 6)
        assertEquals(5, plages.get(1).start());
        assertEquals(6, plages.get(1).end());
    }
}
