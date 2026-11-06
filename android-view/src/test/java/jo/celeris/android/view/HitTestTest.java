package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests de la conversion position ← pixel (visée de test).
 *
 * <p>PIEGE-06 : dans le dépôt de départ, la conversion position ← pixel
 * des tests de vue arrondissait au plus proche — une visée au centre d'un
 * glyphe, équidistante de deux frontières, basculait sur la colonne
 * suivante au moindre bruit. La discipline : viser le quart gauche d'un
 * caractère, jamais son centre ; la conversion elle-même coupe au plancher.</p>
 */
class HitTestTest {

    /** Règle fixe : chaque glyphe mesure exactement 10 px de large. */
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

    private static HitTest viseurFixe() {
        return new HitTest(EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe()));
    }

    /**
     * PIEGE-06 — les tests de vue visent le quart gauche du glyphe.
     *
     * <p>Pour chaque colonne, la visée au quart gauche est à distance
     * sûre de toute frontière : la conversion ne peut pas basculer sur la
     * colonne voisine. Une visée centrée (0,5 × largeur) serait pile sur
     * l'équidistance des frontières — le piège historique.</p>
     */
    @Test
    void viewTestHitTestAimsAtLeftQuarterOfGlyph() {
        HitTest hit = viseurFixe();

        for (int colonne = 0; colonne < 12; colonne++) {
            float quartGauche = colonne * 10f + 10f / 4f;
            assertEquals(colonne, hit.columnAt(quartGauche),
                    "la visée au quart gauche de la colonne " + colonne
                            + " doit donner cette colonne");
        }

        // La frontière exacte entre deux colonnes appartient à la colonne
        // suivante (coupe au plancher, jamais arrondi au plus proche) :
        // la visée au quart gauche ne peut jamais s'y retrouver.
        assertEquals(3, hit.columnAt(30f));
        assertEquals(3, hit.columnAt(30.0001f));
        assertEquals(2, hit.columnAt(29.9999f));
    }

    /** La conversion est un plancher : x dans [0, largeur) donne la colonne 0, et ainsi de suite. */
    @Test
    void conversionAuPlancher() {
        HitTest hit = viseurFixe();
        assertEquals(0, hit.columnAt(0f));
        assertEquals(0, hit.columnAt(9.9999f));
        assertEquals(1, hit.columnAt(10f));
        assertEquals(4, hit.columnAt(49.5f));
    }

    /** Hors rangée, la conversion borne : jamais de colonne négative ou illimitée. */
    @Test
    void horsRangeeBorneAuxColonnes() {
        HitTest hit = viseurFixe();
        assertEquals(0, hit.columnAt(-5f));
        assertEquals(0, hit.columnAt(-0.1f, 10));
        assertEquals(0, hit.columnAt(0f, 10));
        assertEquals(7, hit.columnAt(75f, 10));
        assertEquals(10, hit.columnAt(999f, 10), "au-delà de la fin, la colonne de fin");
    }

    /**
     * L'offset visé par un point : la rangée suit l'ordonnée
     * (défilement compris), la colonne suit l'abscisse, bornée à la fin
     * de la tranche visible.
     */
    @Test
    void offsetViseParPointDeVue() {
        HitTest hit = viseurFixe();
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        // deux lignes de 3 caractères, gouttière de 20 px, sans pli
        jo.celeris.core.text.Document document =
                jo.celeris.core.text.Document.of("abc\ndef");
        VisualProjection projection = new VisualProjection(2, new FoldModel());

        // premier caractère de la première ligne : y = 1 (quart bas du
        // rang 0), x = gouttière + 2 (quart gauche du glyphe 0)
        assertEquals(0, hit.offsetAt(document, projection, metrics,
                20f, 0f, 0f, 22f, 1f));
        // troisième caractère de la deuxième ligne
        assertEquals(6, hit.offsetAt(document, projection, metrics,
                20f, 0f, 0f, 42f, 21f), "y = 21 px : deuxième rangée");
        // sans défilement, le même quart de glyphe sur la première
        // rangée vise la colonne 2 de la première ligne
        assertEquals(2, hit.offsetAt(document, projection, metrics,
                20f, 0f, 0f, 42f, 1f), "y = 1 px : première rangée, colonne 2");
        // défilement vertical d'une rangée : le point du haut vise
        // désormais la deuxième rangée
        assertEquals(6, hit.offsetAt(document, projection, metrics,
                20f, 0f, 20f, 42f, 1f), "scrollY = 20 : deuxième rangée");
        // au-delà de la fin de ligne, la colonne borne à la fin
        assertEquals(3, hit.offsetAt(document, projection, metrics,
                20f, 0f, 0f, 999f, 1f), "x démesuré : fin de la première ligne");
        // au-delà du contenu, la rangée borne à la dernière
        assertEquals(4, hit.offsetAt(document, projection, metrics,
                20f, 0f, 0f, 22f, 999f), "y démesuré : dernière rangée");
    }
}
