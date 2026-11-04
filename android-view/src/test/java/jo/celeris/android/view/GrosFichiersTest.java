package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Tests de charge de la vue sur très gros fichiers (A-25, G-01).
 *
 * <p>G-01 : l'ouverture est instantanée et progressive — 100 000 lignes
 * donnent une première image en moins de 500 ms, 480 000 lignes restent
 * ouvrables. La première image côté vue, c'est la projection (plis et
 * retour à la ligne confondus) puis les mises en page des seules rangées
 * visibles : jamais le document entier ne se met en page d'un coup, le
 * défilement sert les rangées à la demande.</p>
 *
 * <p>PIEGE-13 : la mesure retient le minimum de trois tours — le bruit
 * du runner ne fait qu'ajouter du temps, le minimum l'écarte.</p>
 */
class GrosFichiersTest {

    /** Le budget de première image du mandat G-01, en millisecondes. */
    private static final long BUDGET_PREMIERE_IMAGE_MS = 500L;

    /** Les tours de mesure (PIEGE-13). */
    private static final int TOURS = 3;

    /** Règle fixe : caractère de 10 px, hauteur de ligne 20 px. */
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

    /** Un texte de {@code lignes} lignes de {@code colonnes} caractères. */
    private static String texte(int lignes, int colonnes) {
        StringBuilder gabarit = new StringBuilder(colonnes);
        for (int i = 0; i < colonnes; i++) {
            gabarit.append((char) ('a' + (i % 26)));
        }
        String ligne = gabarit.toString();
        StringBuilder texte = new StringBuilder(lignes * (colonnes + 1));
        for (int i = 0; i < lignes; i++) {
            if (i > 0) {
                texte.append('\n');
            }
            texte.append(ligne);
        }
        return texte.toString();
    }

    /**
     * G-01 — 100 000 lignes : ouverture et première image sous le budget
     * de 500 ms (minimum de trois tours, PIEGE-13).
     */
    @Test
    void largeFile_firstFrameUnderBudget() {
        String contenu = texte(100_000, 80);
        FontSpec police = new FontSpec("mono", 14f);
        EditorMetrics metrics = EditorMetrics.of(police, new RegleFixe());
        Document[] documents = new Document[1];
        VisualProjection[] projections = new VisualProjection[1];
        DocumentRowSource[] sources = new DocumentRowSource[1];

        long meilleur = Long.MAX_VALUE;
        for (int tour = 0; tour < TOURS; tour++) {
            long debut = System.nanoTime();
            documents[0] = Document.of(contenu);
            // Budget de 40 colonnes : chaque ligne de 80 occupe 2 rangées.
            projections[0] = new VisualProjection(documents[0], new FoldModel(), 40);
            sources[0] = new DocumentRowSource(documents[0], projections[0],
                    metrics, 40);
            // La première image ne met en page que les rangées visibles.
            for (int rangee = 0; rangee < 60; rangee++) {
                sources[0].layoutOf(new RowKey(1L, rangee, 1, police, 40));
            }
            meilleur = Math.min(meilleur, System.nanoTime() - debut);
        }

        // Les comptes exacts prouvent la projection complète sans mise en
        // page globale : 200 000 rangées pour 100 000 lignes, et la
        // continuation (rangée 1) porte la seconde moitié de la première
        // ligne — 40 caractères.
        assertEquals(100_000, documents[0].lineCount());
        assertEquals(200_000, projections[0].rowCount());
        assertEquals(40, sources[0]
                .layoutOf(new RowKey(1L, 1, 1, police, 40)).text().length());

        assertTrue(meilleur / 1_000_000 < BUDGET_PREMIERE_IMAGE_MS,
                "premiere image " + meilleur / 1_000_000 + " ms, budget "
                        + BUDGET_PREMIERE_IMAGE_MS + " ms");
    }

    /**
     * G-01 — 480 000 lignes ouvrables : la projection se construit, les
     * comptes sont exacts, les premières rangées se servent.
     */
    @Test
    void largeFile_halfMillionLinesOpenable() {
        FontSpec police = new FontSpec("mono", 14f);
        EditorMetrics metrics = EditorMetrics.of(police, new RegleFixe());
        Document document = Document.of(texte(480_000, 40));
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        DocumentRowSource source = new DocumentRowSource(document, projection,
                metrics, 0);

        assertEquals(480_000, document.lineCount());
        assertEquals(480_000, projection.rowCount());
        assertEquals(40, projection.longestLineLength());
        assertEquals(40, source.layoutOf(new RowKey(1L, 479_999, 1, police, 0))
                .text().length());
    }
}
