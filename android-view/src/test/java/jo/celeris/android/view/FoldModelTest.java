package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests du modèle de plis : plages de lignes repliées sous une ligne
 * d'en-tête, non chevauchantes.
 */
class FoldModelTest {

    /** Un pli cache les lignes qui suivent son en-tête. */
    @Test
    void pliEtDeploiement() {
        FoldModel plis = new FoldModel();
        assertFalse(plis.isFolded(2));

        plis.fold(2, 4);
        assertTrue(plis.isFolded(2));
        assertFalse(plis.isFolded(3), "seul l'en-tête porte le pli");

        plis.unfold(2);
        assertFalse(plis.isFolded(2));
    }

    /** Les plis imbriqués ou chevauchants sont rejetés : un pli, une plage. */
    @Test
    void plisChevauchantsRejetes() {
        FoldModel plis = new FoldModel();
        plis.fold(2, 4);

        assertThrows(IllegalStateException.class, () -> plis.fold(3, 6),
                "un pli qui chevauche un pli existant est rejeté");
        assertThrows(IllegalStateException.class, () -> plis.fold(0, 2),
                "un pli qui recouvre l'en-tête d'un pli existant est rejeté");
        assertThrows(IllegalArgumentException.class, () -> plis.fold(5, 3),
                "un pli inversé (fin avant début) est rejeté");
        assertTrue(plis.isFolded(2), "le pli d'origine est intact après les rejets");
    }

    /** Déplier un pli inexistant est une erreur de programmation, pas un silence. */
    @Test
    void deplierInexistantRejete() {
        FoldModel plis = new FoldModel();
        plis.fold(2, 4);

        assertThrows(IllegalStateException.class, () -> plis.unfold(9));
        assertThrows(IllegalStateException.class, () -> plis.unfold(3),
                "seule la ligne d'en-tête déplie");
        assertTrue(plis.isFolded(2));
    }
}
