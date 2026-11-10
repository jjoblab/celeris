package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.junit.jupiter.api.Test;

/**
 * Tests de la couche de texte du pipeline de rendu (ADR-006 : une seule
 * vue Canvas, couches à tracé par image, chaque couche possédant ses
 * pinceaux).
 *
 * <p>PIEGE-01 : dans le dépôt de départ, un pinceau de texte était partagé
 * par tout le pipeline et muté en cours d'image — une couche qui ne
 * reposait pas explicitement la couleur de base héritait de la couleur
 * résiduelle de la couche précédente. Double parade, testée ici : chaque
 * couche possède son pinceau, et chaque couche repose explicitement sa
 * couleur de base à chaque image.</p>
 */
class TextPainterTest {

    private static final EditorTheme THEME =
            new EditorTheme(1, 0xFFFFFFFF, 0xFF202124, 0xFF1A73E8,
            0xFF000000, 0xFF5F6368);

    /** Pinceau mutable minimal : la couleur courante, rien d'autre. */
    private static final class PinceauMutable implements TextBrush {
        private int couleur;

        @Override
        public void setColor(int argb) {
            couleur = argb;
        }

        @Override
        public int color() {
            return couleur;
        }
    }

    /** Un dessin croisé : le texte (ou le rectangle), le pinceau et la couleur au moment de l'appel. */
    private record Dessin(String texte, TextBrush pinceau, int couleur) {
    }

    /** Surface enregistreuse : capture chaque appel avec la couleur du pinceau à l'instant précis. */
    private static final class SurfaceEnregistreuse implements DrawSurface {
        private final List<Dessin> dessins = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            dessins.add(new Dessin(text, brush, brush.color()));
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            dessins.add(new Dessin("<rect>", brush, brush.color()));
        }

        List<Dessin> dessins() {
            return dessins;
        }
    }

    /** Fabrique comptant ses pinceaux : chaque appel crée une instance neuve. */
    private static final class FabriqueSaine implements BrushFactory {
        private int creations;

        @Override
        public TextBrush newBrush() {
            creations++;
            return new PinceauMutable();
        }
    }

    /** Fabrique hostile : LE MÊME pinceau mutable pour tout le pipeline (le monde du dépôt de départ). */
    private static final class FabriquePartagee implements BrushFactory {
        private final TextBrush unique = new PinceauMutable();

        @Override
        public TextBrush newBrush() {
            return unique;
        }
    }

    /** Source de rangées de test : sans pli ni retour à la ligne, la rangée r porte la ligne r. */
    private static final class SourceParLigne implements RowSource {
        private final Document document;
        private final EditorMetrics metrics;

        SourceParLigne(Document document, EditorMetrics metrics) {
            this.document = document;
            this.metrics = metrics;
        }

        @Override
        public RowLayout layoutOf(RowKey key) {
            String texte = document.lineText(key.row());
            return new RowLayout(key.row(), 0, texte.length(), texte,
                    texte.length() * metrics.charWidth());
        }
    }

    /** Règle fixe : largeur 10 px, hauteur 20 px, ascendance 16, descendance 4. */
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

    /**
     * PIEGE-01 — la couche de texte possède son pinceau et repose
     * explicitement sa couleur de base à chaque image.
     *
     * <p>Premier volet : avec une fabrique saine, le pinceau de la couche
     * de sélection et celui de la couche de texte sont deux instances
     * distinctes. Second volet : même avec la pire fabrique — le même
     * pinceau mutable pour tout le pipeline — le texte part toujours de la
     * couleur de base du thème, jamais de la couleur résiduelle laissée
     * par la couche précédente.</p>
     */
    @Test
    void textPainter_layerOwnsItsPaint_perFrame() {
        Document document = Document.of("alpha\nbeta");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        MultiSelection selection = MultiSelection.of(new SelRange(0, 5));
        VisualProjection projection = new VisualProjection(2, new FoldModel());
        RowLayoutCache cache = new RowLayoutCache();

        // Premier volet : fabrique saine, pinceaux distincts par couche.
        FabriqueSaine saine = new FabriqueSaine();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new SelectionPainter(saine));
        image.addLayer(new TextPainter(saine, cache, new SourceParLigne(document, metrics)));
        FrameState etat = new FrameState();
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        etat.beginFrame(document, selection, projection, 1L, metrics, THEME, 0, 2, 40f, 0f, 0f);
        image.drawFrame(surface, etat);

        Dessin rect = surface.dessins().get(0);
        Dessin texte = surface.dessins().get(1);
        assertEquals("<rect>", rect.texte());
        assertEquals("alpha", texte.texte());
        assertNotSame(rect.pinceau(), texte.pinceau(),
                "chaque couche doit posséder son pinceau (fabrique saine)");
        // 1 pinceau de sélection, 1 de base, 14 de sortes de jetons
        // (B-01) et 1 de portées (M7.6, H-07) : chacun possède le
        // sien, aucun n'est partagé.
        assertEquals(17, saine.creations);

        // Second volet : fabrique hostile — le même pinceau partagé pour
        // tout le pipeline. La couleur de base est reposée à chaque image
        // par chaque couche : le texte hérite de la couleur résiduelle de
        // personne.
        FabriquePartagee hostile = new FabriquePartagee();
        ScrollFrame imageHostile = new ScrollFrame();
        imageHostile.addLayer(new SelectionPainter(hostile));
        imageHostile.addLayer(new TextPainter(hostile, cache,
                new SourceParLigne(document, metrics)));
        FrameState etatHostile = new FrameState();
        SurfaceEnregistreuse surfaceHostile = new SurfaceEnregistreuse();
        etatHostile.beginFrame(document, selection, projection, 2L, metrics, THEME, 0, 2, 40f, 0f, 0f);
        imageHostile.drawFrame(surfaceHostile, etatHostile);
        imageHostile.drawFrame(surfaceHostile, etatHostile);

        List<Dessin> dessins = surfaceHostile.dessins();
        assertEquals(6, dessins.size(),
                "deux images : chacune peint un rectangle de sélection et deux textes");
        for (Dessin dessin : dessins) {
            if ("<rect>".equals(dessin.texte())) {
                assertEquals(THEME.selectionColor(), dessin.couleur(),
                        "la sélection peint dans sa couleur propre");
            } else {
                assertEquals(THEME.baseTextColor(), dessin.couleur(),
                        "le texte repose sa couleur de base à chaque image (PIEGE-01)");
            }
        }
        assertTrue(dessins.get(0).texte().equals("<rect>")
                && dessins.get(dessins.size() - 1).texte().equals("beta"),
                "dans chaque image, la sélection peint avant le texte");
    }

    /** Sans sélection, la couche de sélection ne peint rien ; le texte reste. */
    @Test
    void sansSelectionSeulLeTextePeint() {
        Document document = Document.of("alpha\nbeta");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        VisualProjection projection = new VisualProjection(2, new FoldModel());
        RowLayoutCache cache = new RowLayoutCache();
        FabriqueSaine saine = new FabriqueSaine();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new SelectionPainter(saine));
        image.addLayer(new TextPainter(saine, cache, new SourceParLigne(document, metrics)));
        FrameState etat = new FrameState();
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();

        etat.beginFrame(document, MultiSelection.empty(), projection, 1L, metrics, THEME,
                0, 2, 40f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(2, surface.dessins().size());
        assertEquals("alpha", surface.dessins().get(0).texte());
        assertEquals("beta", surface.dessins().get(1).texte());
    }

    /**
     * La couche de texte consulte la source COURANTE, jamais une capture
     * périmée : la vue réassigne la source à chaque reconstruction de
     * projection (changement de session, de révision, de budget de
     * coupure) — le peintre, construit une fois au pipeline, doit
     * suivre, sinon il servirait le contenu d'avant sous la révision
     * d'après.
     */
    @Test
    void textPainter_followsReboundSource() {
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        RowLayoutCache cache = new RowLayoutCache();
        RebindingRowSource sourceLiee = new RebindingRowSource(
                new SourceParLigne(Document.of("alpha"), metrics));
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new TextPainter(new FabriqueSaine(), cache, sourceLiee));
        FrameState etat = new FrameState();
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();

        // Première image : la source d'origine sert la révision 1.
        etat.beginFrame(Document.of("alpha"), MultiSelection.empty(),
                new VisualProjection(1, new FoldModel()), 1L, metrics, THEME,
                0, 1, 40f, 0f, 0f);
        image.drawFrame(surface, etat);
        assertEquals("alpha", surface.dessins().get(0).texte());

        // La session change : autre source, autre révision — le peintre
        // construit une fois doit servir le NOUVEAU contenu.
        sourceLiee.set(new SourceParLigne(Document.of("nouveau"), metrics));
        cache.invalidate(1L);
        etat.beginFrame(Document.of("nouveau"), MultiSelection.empty(),
                new VisualProjection(1, new FoldModel()), 2L, metrics, THEME,
                0, 1, 40f, 0f, 0f);
        image.drawFrame(surface, etat);
        assertEquals("nouveau", surface.dessins().get(1).texte(),
                "le peintre doit consulter la source réassignée, pas sa capture d'origine");
    }
}
