package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests du modèle de plis : plages de lignes repliées sous une ligne
 * d'en-tête, imbriquables sans croisement.
 */
class FoldModelTest {

    /** Un pli cache les lignes qui suivent son en-tête. */
    @Test
    void pliEtDeploiement() {
        FoldModel folds = new FoldModel();
        assertFalse(folds.isFolded(2));

        folds.fold(2, 4);
        assertTrue(folds.isFolded(2));
        assertFalse(folds.isFolded(3), "seul l'en-tête porte le pli");

        folds.unfold(2);
        assertFalse(folds.isFolded(2));
    }

    /** Les plis CROISÉS sont rejetés : un pli, une plage de lignes —
     *  l'imbriqué passe, le traversé se refuse. */
    @Test
    void plisCroisesRejetes() {
        FoldModel folds = new FoldModel();
        folds.fold(2, 4);

        assertThrows(IllegalStateException.class, () -> folds.fold(3, 6),
                "un pli qui CROISE un pli existant est rejeté");
        assertThrows(IllegalStateException.class, () -> folds.fold(0, 2),
                "un pli qui recouvre l'en-tête d'un pli existant est rejeté");
        assertThrows(IllegalArgumentException.class, () -> folds.fold(5, 3),
                "un pli inversé (fin avant début) est rejeté");
        assertTrue(folds.isFolded(2), "le pli d'origine est intact après les rejets");
    }

    /** Déplier un pli inexistant est une erreur de programmation, pas un silence. */
    @Test
    void deplierInexistantRejete() {
        FoldModel folds = new FoldModel();
        folds.fold(2, 4);

        assertThrows(IllegalStateException.class, () -> folds.unfold(9));
        assertThrows(IllegalStateException.class, () -> folds.unfold(3),
                "seule la ligne d'en-tête déplie");
        assertTrue(folds.isFolded(2));
    }

    /**
     * Le redressement du 2026-11-13 (« si je plie le {} imbriqué, le {}
     * parent ne veut pas plier ») : le parent d'un bloc déjà replié se
     * replie PAR-DESSUS lui — l'enfant survit replié sous le parent, il
     * reparaît replié au dépliage du parent (la sémantique du dépôt de
     * départ : les régions repliées s'unissent dans la projection).
     */
    @Test
    void pliParentApresEnfant() {
        FoldModel folds = new FoldModel();
        folds.fold(3, 5);

        folds.fold(1, 8);
        assertTrue(folds.isFolded(1), "le parent se replie par-dessus l'enfant");
        assertTrue(folds.isFolded(3), "l'enfant survit replié sous le parent");

        folds.unfold(1);
        assertFalse(folds.isFolded(1), "le parent se déplie");
        assertTrue(folds.isFolded(3), "l'enfant reparaît replié après le dépliage");
    }

    /** Les plis EN CONTENUS s'imbriquent aussi : le parent d'abord,
     *  l'enfant ensuite — les deux ordres mènent au même état. */
    @Test
    void pliEnfantApresParent() {
        FoldModel folds = new FoldModel();
        folds.fold(1, 8);

        folds.fold(3, 5);
        assertTrue(folds.isFolded(1));
        assertTrue(folds.isFolded(3), "l'enfant se pose dans le parent replié");
        assertEquals(5, folds.finDe(3));
        assertEquals(8, folds.finDe(1));
    }

    /**
     * La fonte des plis imbriqués : le parent et son enfant ne fondent
     * qu'UNE plage — aucune ligne cachée ne se compte double (la
     * projection s'y sert, le rowCount du banc VisualProjection en
     * dérive).
     */
    @Test
    void fonteDesImbriques() {
        FoldModel folds = new FoldModel();
        folds.fold(1, 8);
        folds.fold(3, 5);
        int[] fondus = folds.plisFondus();
        assertEquals(2, fondus.length, "parent et enfant ne fondent qu'une plage");
        assertEquals(1, fondus[0]);
        assertEquals(8, fondus[1]);

        // deux plis disjoints qui se touchent : l'en-tête du second
        // reste VISIBLE — la fonte ne fusionne pas les plages adjacentes
        folds.unfold(1);
        folds.unfold(3);
        folds.fold(1, 5);
        folds.fold(6, 8);
        int[] adjacents = folds.plisFondus();
        assertEquals(4, adjacents.length, "les plages qui se touchent ne fondent pas");
        assertEquals(5, adjacents[1]);
        assertEquals(6, adjacents[2]);
    }

    /** La fonte se mémoïse par révision : plier/déplier paie, la
     *  lecture relit le tableau sans recompter. */
    @Test
    void fonteMemoiseeParRevision() {
        FoldModel folds = new FoldModel();
        folds.fold(1, 8);
        int[] premiere = folds.plisFondus();
        assertSame(premiere, folds.plisFondus(), "la fonte relit le tableau mémoïsé");

        folds.fold(3, 5);
        assertNotSame(premiere, folds.plisFondus(), "une mutation recalcule la fonte");
        assertEquals(2, folds.plisFondus().length, "l'imbriqué n'ajoute pas de plage");

        int revision = folds.revision();
        folds.unfold(3);
        assertEquals(revision + 1, folds.revision(), "le dépliage incrémente la révision");
    }
}
