package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * La géométrie des curseurs (A-18) : la conversion offset → position
 * suit les rangées visuelles — sous coupures, la position vit sur SA
 * tranche, le miroir exact de la visée {@code HitTest}.
 */
class CaretGeometryTest {

    /** Règle fixe : 10 px par caractère, 20 px par rangée. */
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
            return 16f;
        }

        @Override
        public float descent(FontSpec spec) {
            return 4f;
        }
    }

    private static final EditorMetrics METRICS =
            EditorMetrics.of(new FontSpec("mono", 16f), new RegleFixe());

    /** Sans coupure, la rangée de l'offset est la rangée de sa ligne. */
    @Test
    void rangeeDeLOffsetSansCoupure() {
        Document document = Document.of("alpha\nbeta");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        assertEquals(0, CaretGeometry.rowOfOffset(document, projection, 2));
        assertEquals(1, CaretGeometry.rowOfOffset(document, projection, 7));
    }

    /**
     * Sous coupures (A-18) : la rangée d'un offset au milieu d'une ligne
     * coupée est la rangée de sa ligne plus l'indice de SA tranche — la
     * position vit sur la tranche qui la porte.
     */
    @Test
    void rangeeDeLOffsetSurSaTranche() {
        // 10 colonnes par rangée : "abcdef" tient sur une rangée,
        // l'offset 12 de la ligne de 20 vit sur la deuxième tranche.
        Document document = Document.of("01234567890123456789");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 10);
        assertEquals(0, CaretGeometry.rowOfOffset(document, projection, 5));
        assertEquals(1, CaretGeometry.rowOfOffset(document, projection, 12));
        assertEquals(1, CaretGeometry.rowOfOffset(document, projection, 19));
    }

    /**
     * L'abscisse du curseur : la colonne DANS la tranche, depuis le bord
     * gauche du texte, défilement déduit.
     */
    @Test
    void abscisseDuCurseurSurSaTranche() {
        Document document = Document.of("01234567890123456789");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 10);
        // Gouttière 30 px : le caractère 12 vit colonne 2 de la tranche 1.
        assertEquals(30f + 2 * 10f, CaretGeometry.xOfOffset(document,
                projection, METRICS, 30f, 0f, 12), 1e-6f);
        // Le défilement horizontal se déduit.
        assertEquals(30f + 2 * 10f - 15f, CaretGeometry.xOfOffset(document,
                projection, METRICS, 30f, 15f, 12), 1e-6f);
    }

    /**
     * L'ordonnée du curseur : le haut de SA rangée — sous coupures, la
     * deuxième tranche vit une rangée plus bas.
     */
    @Test
    void ordonneeDuCurseurSurSaRangee() {
        Document document = Document.of("01234567890123456789");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 10);
        assertEquals(0f, CaretGeometry.yOfOffset(document, projection,
                METRICS, 0f, 5), 1e-6f);
        assertEquals(20f, CaretGeometry.yOfOffset(document, projection,
                METRICS, 0f, 12), 1e-6f);
        // Le défilement vertical se déduit.
        assertEquals(20f - 8f, CaretGeometry.yOfOffset(document, projection,
                METRICS, 8f, 12), 1e-6f);
    }

    /**
     * Le miroir de la visée : la position d'un offset, lue en retour par
     * la visée du quart gauche de sa colonne (PIEGE-06), retombe sur SA
     * colonne — jamais sur une colonne d'une autre tranche.
     */
    @Test
    void laPositionResteSurSaTranche() {
        Document document = Document.of("01234567890123456789");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 10);
        HitTest viseur = new HitTest(METRICS);
        // Jusqu'à la colonne 19 incluse : la borne 20 (fin de ligne)
        // exclue — le caret de fin de ligne vit sur une rangée que la
        // projection ne rend pas (la rangée fantôme), la visée y
        // retourne la première colonne de la dernière tranche rendue.
        for (int offset = 0; offset < 20; offset++) {
            // Gouttière nulle des deux côtés : la position et la visée
            // partagent la même règle.
            float x = CaretGeometry.xOfOffset(document, projection, METRICS,
                    0f, 0f, offset);
            int rangee = CaretGeometry.rowOfOffset(document, projection, offset);
            int vise = viseur.offsetAt(document, projection,
                    METRICS, 0f, 0f, 0f, x + 1f, rangee * 20f + 1f);
            assertEquals(offset, vise,
                    "offset " + offset + " : la visée retombe sur sa colonne");
        }
    }

    /** Le document vide ne lève jamais : la rangée 0, l'abscisse du bord. */
    @Test
    void documentVideNeLeveJamais() {
        Document document = Document.of("");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 10);
        assertEquals(0, CaretGeometry.rowOfOffset(document, projection, 0));
        assertEquals(0f, CaretGeometry.xOfOffset(document, projection,
                METRICS, 0f, 0f, 0), 1e-6f);
    }
}
