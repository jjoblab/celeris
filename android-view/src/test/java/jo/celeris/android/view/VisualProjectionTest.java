package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests de la projection lignes de document ↔ rangées visuelles à travers
 * le modèle de plis.
 *
 * <p>PIEGE-04 : dans le dépôt de départ, les indices de première/dernière
 * ligne visible étaient traités comme des lignes du document alors que ce
 * sont des rangées visuelles — toute plage calculée sous des plis était
 * fausse. Toute conversion entre rangées et lignes passe par le modèle de
 * plis, jamais par l'identité.</p>
 */
class VisualProjectionTest {

    /**
     * PIEGE-04 — les fenêtres de rangées visibles se convertissent en
     * lignes de document PAR le modèle de plis.
     */
    @Test
    void renderer_visibleRowsConvertThroughFoldModel() {
        FoldModel folds = new FoldModel();
        folds.fold(2, 4); // l'en-tête 2 reste visible, les lignes 3 et 4 sont cachées
        VisualProjection projection = new VisualProjection(10, folds);

        // 8 rangées visibles pour 10 lignes : le pli en cache deux.
        assertEquals(8, projection.rowCount());

        // La conversion rangée ↔ ligne passe par le modèle : une ligne
        // cachée se projette sur la rangée de son en-tête, et la ligne
        // après le pli remonte d'autant.
        assertEquals(2, projection.rowOfLine(2));
        assertEquals(2, projection.rowOfLine(3));
        assertEquals(2, projection.rowOfLine(4));
        assertEquals(3, projection.rowOfLine(5));
        assertEquals(7, projection.rowOfLine(9));

        // La rangée du pli couvre toutes ses lignes cachées.
        assertEquals(2, projection.firstLineOfRow(2));
        assertEquals(4, projection.lastLineOfRow(2));

        // Fenêtre visible de rangées 1 à 3 : elle couvre les lignes 1 à 5.
        // L'identité naïve « rangée = ligne » dirait 1 à 3 — faux sous pli.
        assertEquals(1, projection.firstLineOfRow(1));
        assertEquals(5, projection.lastLineOfRow(3));
    }

    /** Sans pli, rangée et ligne se correspondent un pour un. */
    @Test
    void sansPlisRangeeEtLigneSeCorrespondent() {
        VisualProjection projection = new VisualProjection(5, new FoldModel());
        assertEquals(5, projection.rowCount());
        for (int line = 0; line < 5; line++) {
            assertEquals(line, projection.rowOfLine(line));
            assertEquals(line, projection.firstLineOfRow(line));
            assertEquals(line, projection.lastLineOfRow(line));
        }
    }

    /** Déplier redonne toutes les rangées : la projection lit le modèle en direct. */
    @Test
    void deplierRedonneToutesLesRangees() {
        FoldModel folds = new FoldModel();
        folds.fold(2, 4);
        VisualProjection projection = new VisualProjection(10, folds);
        assertEquals(8, projection.rowCount());

        folds.unfold(2);

        assertEquals(10, projection.rowCount());
        assertEquals(5, projection.rowOfLine(5));
        assertEquals(5, projection.lastLineOfRow(5));
    }

    /** Deux plis successifs se cumulent proprement. */
    @Test
    void deuxPlisSeCumulent() {
        FoldModel folds = new FoldModel();
        folds.fold(1, 2); // l'en-tête 1 reste visible, la ligne 2 est cachée
        folds.fold(6, 8); // l'en-tête 6 reste visible, les lignes 7 et 8 sont cachées
        VisualProjection projection = new VisualProjection(12, folds);

        // 12 lignes - 3 cachées = 9 rangées visibles.
        assertEquals(9, projection.rowCount());
        assertEquals(0, projection.rowOfLine(0));
        assertEquals(1, projection.rowOfLine(2));
        assertEquals(2, projection.rowOfLine(3));
        assertEquals(5, projection.rowOfLine(6));
        assertEquals(5, projection.rowOfLine(7));
        assertEquals(5, projection.rowOfLine(8));
        assertEquals(6, projection.rowOfLine(9));
        assertEquals(2, projection.lastLineOfRow(1));
        assertEquals(8, projection.lastLineOfRow(5));
        assertEquals(9, projection.firstLineOfRow(6));
    }

    /** Un pli au-delà de la dernière ligne ne cache rien : robustesse aux bornes. */
    @Test
    void pliHorsBornesNeCacheRien() {
        FoldModel folds = new FoldModel();
        folds.fold(20, 24);
        VisualProjection projection = new VisualProjection(10, folds);

        assertEquals(10, projection.rowCount());
        assertEquals(9, projection.rowOfLine(9));
    }

    /**
     * Le redressement du 2026-11-13 : le parent replié par-dessus son
     * enfant déjà replié — la ligne cachée par les DEUX ne se compte
     * qu'une fois (la fonte du modèle sert l'union), le dépliage du
     * parent rend l'enfant replié.
     */
    @Test
    void pliParentImbriqueSurEnfantReplie() {
        FoldModel folds = new FoldModel();
        folds.fold(3, 5);
        // l'enfant seul : 10 lignes - 2 cachées (4 et 5) = 8 rangées
        VisualProjection projection = new VisualProjection(10, folds);
        assertEquals(8, projection.rowCount());

        // le parent par-dessus : 1..8 cache 2..8 tout entières —
        // l'enfant est DANS cette plage, rien ne se compte double
        folds.fold(1, 8);
        assertEquals(3, projection.rowCount(),
                "le parent absorbe la plage entière sans double compte");
        assertEquals(1, projection.rowOfLine(2));
        assertEquals(1, projection.rowOfLine(8));
        assertEquals(1, projection.firstLineOfRow(1));
        assertEquals(8, projection.lastLineOfRow(1));
        assertEquals(9, projection.firstLineOfRow(2),
                "la rangée 2 porte la ligne 9, dernière visible");
        assertEquals(9, projection.lastLineOfRow(2));
        assertEquals(2, projection.rowOfLine(9));

        // le dépliage du parent rend l'enfant replié : 4 et 5 cachées
        folds.unfold(1);
        assertEquals(8, projection.rowCount(),
                "l'enfant reparaît replié après le dépliage du parent");
        assertEquals(2, projection.rowOfLine(2));
        assertEquals(3, projection.rowOfLine(4));
        assertEquals(3, projection.rowOfLine(5));
        assertEquals(4, projection.rowOfLine(6));
    }
}
