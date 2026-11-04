package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Tests du tracé d'image en régime de défilement (ADR-006 : zéro
 * allocation par image en régime établi).
 *
 * <p>PIEGE-15 : dans le dépôt de départ, les rectangles et chemins de
 * travail étaient alloués dans la boucle de dessin à chaque image. La
 * discipline : les coordonnées passent en primitives, les chaînes et
 * pinceaux sont réutilisés, l'état d'image est muté sur place — la
 * preuve par identité : aucun objet nouveau ne croise l'interface de
 * dessin d'une image à l'autre en régime établi.</p>
 */
class ScrollFrameTest {

    private static final EditorTheme THEME =
            new EditorTheme(1, 0xFF202124, 0xFF1A73E8, 0xFF000000, 0xFF5F6368);

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

    /** Fabrique saine : chaque appel crée une instance neuve. */
    private static final class FabriqueSaine implements BrushFactory {
        @Override
        public TextBrush newBrush() {
            return new PinceauMutable();
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

    /**
     * Surface traqueuse d'identités : mémorise l'identité de tout objet
     * croisant l'interface de dessin (chaînes de texte, pinceaux). Après
     * le basculement en régime établi, toute identité jamais vue est une
     * allocation par image — la violation du piège.
     */
    private static final class SurfaceIdentites implements DrawSurface {
        private final Set<Integer> vues = new HashSet<>();
        private final Set<Integer> etabli = new HashSet<>();
        private final Set<Integer> nouvelles = new HashSet<>();
        private boolean enRegime;
        private int dessins;

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            dessins++;
            viser(text, brush);
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            dessins++;
            viser(null, brush);
        }

        private void viser(@Nullable String texte, TextBrush pinceau) {
            if (texte != null) {
                Integer identite = System.identityHashCode(texte);
                if (enRegime && !etabli.contains(identite)) {
                    nouvelles.add(identite);
                }
                vues.add(identite);
            }
            Integer identitePinceau = System.identityHashCode(pinceau);
            if (enRegime && !etabli.contains(identitePinceau)) {
                nouvelles.add(identitePinceau);
            }
            vues.add(identitePinceau);
        }

        /** Bascule en régime établi : l'identité de tout objet déjà vu devient la référence. */
        void basculerEnRegimeEtabli() {
            etabli.addAll(vues);
            enRegime = true;
        }

        boolean aucuneIdentiteNouvelle() {
            return nouvelles.isEmpty();
        }

        int dessins() {
            return dessins;
        }
    }

    /**
     * PIEGE-15 — aucune allocation par image en régime de défilement
     * établi.
     *
     * <p>Cent une images de défilement pixel : après l'image
     * d'échauffement (remplissage du cache), aucun objet nouveau — ni
     * chaîne, ni pinceau — ne croise l'interface de dessin.</p>
     */
    @Test
    void scrollFrame_allocatesNothing() {
        Document document = Document.of("un\ndeux\ntrois\nquatre\ncinq");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        MultiSelection selection = MultiSelection.of(new SelRange(3, 7));
        VisualProjection projection = new VisualProjection(5, new FoldModel());
        RowLayoutCache cache = new RowLayoutCache();
        FabriqueSaine fabrique = new FabriqueSaine();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new SelectionPainter(fabrique));
        image.addLayer(new TextPainter(fabrique, cache,
                new SourceParLigne(document, metrics)));
        FrameState etat = new FrameState();
        SurfaceIdentites surface = new SurfaceIdentites();

        // Image 1 : échauffement — le cache se remplit, les instances
        // croisent l'interface pour la première fois (légitime).
        etat.beginFrame(document, selection, projection, 1L, metrics, THEME, 0, 5, 0f, 0f);
        image.drawFrame(surface, etat);
        assertEquals(6, surface.dessins(), "un rectangle de sélection et cinq textes");
        assertEquals(5, cache.size());
        surface.basculerEnRegimeEtabli();

        // Images 2 à 101 : défilement pixel en régime établi — aucun
        // objet nouveau ne doit croiser l'interface (PIEGE-15).
        for (int numero = 2; numero <= 101; numero++) {
            float dy = (numero - 2) * 0.5f;
            etat.beginFrame(document, selection, projection, 1L, metrics, THEME, 0, 5, 0f, dy);
            image.drawFrame(surface, etat);
        }

        assertEquals(606, surface.dessins(), "cent une images, six dessins chacune");
        assertEquals(5, cache.size(), "aucun redépôt après l'échauffement");
        assertTrue(surface.aucuneIdentiteNouvelle(),
                "PIEGE-15 : aucun objet nouveau ne croise l'interface en régime établi");
    }

    /**
     * Revenir sur des rangées déjà vues ne ralloue rien : la première vue
     * d'une rangée est légitime, sa re-vue ne l'est pas.
     */
    @Test
    void retourSurRangéesDéjàVuesSansRienAllouer() {
        Document document = Document.of("un\ndeux\ntrois\nquatre\ncinq\nsix");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        MultiSelection selection = MultiSelection.of(new SelRange(0, 2));
        VisualProjection projection = new VisualProjection(6, new FoldModel());
        RowLayoutCache cache = new RowLayoutCache();
        FabriqueSaine fabrique = new FabriqueSaine();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new SelectionPainter(fabrique));
        image.addLayer(new TextPainter(fabrique, cache,
                new SourceParLigne(document, metrics)));
        FrameState etat = new FrameState();
        SurfaceIdentites surface = new SurfaceIdentites();

        // Fenêtre sur les rangées 0 à 4, puis 1 à 5 (la rangée 5 se voit
        // pour la première fois — légitime), puis retour sur 0 à 4 : tout
        // a déjà été vu, rien ne doit être alloué.
        etat.beginFrame(document, selection, projection, 1L, metrics, THEME, 0, 5, 0f, 0f);
        image.drawFrame(surface, etat);
        surface.basculerEnRegimeEtabli();

        etat.beginFrame(document, selection, projection, 1L, metrics, THEME, 1, 5, 0f, 0f);
        image.drawFrame(surface, etat);
        surface.basculerEnRegimeEtabli();

        etat.beginFrame(document, selection, projection, 1L, metrics, THEME, 0, 5, 0f, 0f);
        image.drawFrame(surface, etat);

        assertTrue(surface.aucuneIdentiteNouvelle(),
                "re-voir des rangées déjà vues n'alloue rien");
        assertEquals(6, cache.size());
    }
}
