package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.junit.jupiter.api.Test;

/**
 * Tests de la couche de gouttière : numéros absolus par rangée, alignés
 * à droite, insensibles au défilement horizontal (D-01), servis par la
 * projection (PIEGE-04) et mémoïsés (PIEGE-15).
 */
class GutterPainterTest {

    private static final EditorTheme THEME =
            new EditorTheme(1, 0xFFFFFFFF, 0xFF202124, 0xFF1A73E8,
                    0xFF000000, 0xFF5F6368);

    /** Un thème à voile : la gouttière teintée, translucide (M7.5). */
    private static final EditorTheme THEME_VOILE =
            new EditorTheme(4, 0xFFFFFFFF, 0xFF202124, 0xFF1A73E8,
                    0xFF000000, 0xFF5F6368, 0x14202124,
                    SyntaxPalette.monochrome(0xFF202124));

    /** Pinceau mutable minimal : la couleur courante, l'épaisseur de
     *  trait et la graisse posées, rien d'autre. */
    private static final class PinceauMutable implements TextBrush {
        private int couleur;
        private float epaisseur;
        private boolean gras;

        @Override
        public void setColor(int argb) {
            couleur = argb;
        }

        @Override
        public int color() {
            return couleur;
        }

        /** La dernière épaisseur de trait posée — le chevron s'y éprouve. */
        float epaisseur() {
            return epaisseur;
        }

        /** La graisse courante — le numéro courant s'y éprouve. */
        boolean gras() {
            return gras;
        }

        @Override
        public void setStrokeWidth(float pixels) {
            epaisseur = pixels;
        }

        @Override
        public void setStyle(boolean bold, boolean italic, boolean underline) {
            gras = bold;
        }
    }

    /** Un texte tracé : son contenu, son abscisse, sa couleur et sa graisse. */
    private record TexteTrace(String texte, float x, int couleur, boolean gras) {
    }

    /** Surface enregistreuse de textes. */
    private static final class SurfaceTextes implements DrawSurface {
        private final List<TexteTrace> textes = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            textes.add(new TexteTrace(text, x, brush.color(), grasDe(brush)));
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            // la gouttière ne trace aucun rectangle
        }

        List<TexteTrace> textes() {
            return textes;
        }
    }

    /** La graisse du pinceau au tracé — faux sur un pinceau muet. */
    private static boolean grasDe(TextBrush brush) {
        return brush instanceof PinceauMutable pinceau && pinceau.gras();
    }

    /** Un trait tracé : ses deux extrémités et sa couleur. */
    private record TraitTrace(float x1, float y1, float x2, float y2, int couleur) {
    }

    /** Un rectangle tracé : ses bornes et sa couleur. */
    private record RectTrace(float left, float top, float right, float bottom, int couleur) {
    }

    /** Un cercle tracé : son centre, son rayon, son remplissage, sa couleur. */
    private record CercleTrace(float cx, float cy, float rayon, boolean plein,
            int couleur) {
    }

    /** Un triangle plein tracé : son centre, sa pointe, sa couleur. */
    private record TriangleTrace(float cx, float cy, float rayon,
            boolean pointeEnBas, int couleur) {
    }

    /** Surface enregistreuse complète : textes, traits, rectangles,
     *  cercles et triangles. */
    private static final class SurfaceMarques implements DrawSurface {
        private final List<TexteTrace> textes = new ArrayList<>();
        private final List<TraitTrace> traits = new ArrayList<>();
        private final List<RectTrace> rectangles = new ArrayList<>();
        private final List<CercleTrace> cercles = new ArrayList<>();
        private final List<TriangleTrace> triangles = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            textes.add(new TexteTrace(text, x, brush.color(), grasDe(brush)));
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            rectangles.add(new RectTrace(left, top, right, bottom, brush.color()));
        }

        @Override
        public void drawLine(float x1, float y1, float x2, float y2, TextBrush brush) {
            traits.add(new TraitTrace(x1, y1, x2, y2, brush.color()));
        }

        @Override
        public void drawCircle(float cx, float cy, float rayon, boolean plein,
                TextBrush brush) {
            cercles.add(new CercleTrace(cx, cy, rayon, plein, brush.color()));
        }

        @Override
        public void drawTriangle(float centreX, float centreY, float rayon,
                boolean pointeEnBas, TextBrush brush) {
            triangles.add(new TriangleTrace(centreX, centreY, rayon,
                    pointeEnBas, brush.color()));
        }

        List<TexteTrace> textes() {
            return textes;
        }

        List<TraitTrace> traits() {
            return traits;
        }

        List<RectTrace> rectangles() {
            return rectangles;
        }

        List<CercleTrace> cercles() {
            return cercles;
        }

        List<TriangleTrace> triangles() {
            return triangles;
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
     * D-01 — les numéros absolus, un par rangée visible, alignés à
     * droite dans la gouttière.
     */
    @Test
    void numerosAbsolusAlignesADroite() {
        SurfaceTextes surface = new SurfaceTextes();
        ScrollFrame image = new ScrollFrame();
        GutterPainter gouttiere = new GutterPainter(() -> new PinceauMutable(), 4f);
        image.addLayer(gouttiere);
        Document document = Document.of("un\ndeux\ntrois");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        // gouttière de 18 px pour « 3 » (un chiffre) : 1 × 10 + 2 × 4.
        float largeur = GutterGeometry.width(3, 10f, 4f);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(3, new FoldModel()), 1L, metrics, THEME,
                0, 3, largeur, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(3, surface.textes().size());
        assertEquals("1", surface.textes().get(0).texte());
        assertEquals("2", surface.textes().get(1).texte());
        assertEquals("3", surface.textes().get(2).texte());
        // alignés à droite : « 1 » et « 3 » finissent à 18 - 4, abscisse 4.
        assertEquals(4f, surface.textes().get(0).x(), 1e-6f);
        assertEquals(4f, surface.textes().get(2).x(), 1e-6f);
        assertEquals(THEME.gutterTextColor(), surface.textes().get(0).couleur());
    }

    /**
     * La gouttière ne défile jamais horizontalement : le défilement
     * horizontal du contenu ne déplace pas les numéros.
     */
    @Test
    void gouttiereInsensibleAuDefilementHorizontal() {
        SurfaceTextes surface = new SurfaceTextes();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f));
        Document document = Document.of("un\ndeux\ntrois");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        float largeur = GutterGeometry.width(3, 10f, 4f);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(3, new FoldModel()), 1L, metrics, THEME,
                0, 3, largeur, 120f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(4f, surface.textes().get(0).x(), 1e-6f,
                "l'abscisse des numéros ne dépend pas du défilement horizontal");
    }

    /**
     * La rangée d'un pli porte le numéro de son en-tête (PIEGE-04) : la
     * conversion passe par la projection.
     */
    @Test
    void rangeeDePliPorteLeNumeroDeSonEnTete() {
        SurfaceTextes surface = new SurfaceTextes();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f));
        Document document = Document.of("a\nb\nc\nd\ne");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FoldModel plis = new FoldModel();
        plis.fold(1, 3); // l'en-tête 1 reste, les lignes 2 à 4 sont cachées
        VisualProjection projection = new VisualProjection(5, plis);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 1L, metrics, THEME,
                0, projection.rowCount(), 46f, 0f, 0f);
        image.drawFrame(surface, etat);

        // 5 lignes, pli (1, 3) : les lignes 2 et 3 sont cachées —
        // rangées : 0 → ligne 0, 1 → lignes 1 à 3 (pli), 2 → ligne 4.
        assertEquals(3, surface.textes().size());
        assertEquals("1", surface.textes().get(0).texte());
        assertEquals("2", surface.textes().get(1).texte(), "la rangée du pli porte l'en-tête");
        assertEquals("5", surface.textes().get(2).texte());
    }

    /** Les étiquettes sont mémoïsées : deux images, mêmes instances (PIEGE-15). */
    @Test
    void etiquettesMemoiseesEntreImages() {
        SurfaceTextes surface = new SurfaceTextes();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f));
        Document document = Document.of("a\nb");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(2, new FoldModel()), 1L, metrics, THEME,
                0, 2, 28f, 0f, 0f);
        image.drawFrame(surface, etat);
        image.drawFrame(surface, etat);

        assertTrue(surface.textes().get(0).texte() == surface.textes().get(2).texte(),
                "la même instance d'étiquette sert les deux images");
        assertTrue(surface.textes().get(1).texte() == surface.textes().get(3).texte());
    }

    /**
     * A-18 — le numéro ne se pose que sur la première rangée d'une
     * ligne coupée : les rangées de continuation restent muettes.
     */
    @Test
    void numeroSeulementSurPremiereRangeeCoupee() {
        SurfaceTextes surface = new SurfaceTextes();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f));
        // Ligne unique de 8 caractères, budget 4 : deux rangées.
        Document document = Document.of("abcdefgh");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 1L,
                metrics, THEME, 0, projection.rowCount(), 18f, 0f, 0f);
        image.drawFrame(surface, etat);

        // Seule la première tranche porte le numéro ; la continuation
        // n'écrit rien en gouttière.
        assertEquals(1, surface.textes().size());
        assertEquals("1", surface.textes().get(0).texte());
    }

    /**
     * D-07 — l'en-tête replié porte son chevron : TRIANGLE PLEIN à
     * pointe à droite, dans la BANDE DE PLIS au bord droit de la
     * gouttière, contre le code qu'il replie.
     */
    @Test
    void enTeteRepliePorteSonChevron() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        FoldModel replis = new FoldModel();
        replis.fold(1, 3);
        GutterMarks marques = GutterMarks.of(
                List.of(new FoldSpan(1, 3)), replis, new int[0]);
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f,
                () -> marques, () -> 12f, () -> 8f, ligne -> 0,
                ligne -> false));
        Document document = Document.of("a\nb\nc\nd\ne");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        VisualProjection projection = new VisualProjection(5, replis);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 1L, metrics,
                THEME, 0, projection.rowCount(), 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(1, surface.triangles().size(),
                "le chevron replié vit en un triangle plein");
        TriangleTrace chevron = surface.triangles().get(0);
        // la bande de plis vit au bord droit : huit pixels, centre à 26
        assertEquals(26f, chevron.cx(), 0.5f,
                "le chevron vit dans la bande de plis, contre le code");
        assertFalse(chevron.pointeEnBas(),
                "la pointe du chevron replié regarde à droite");
        assertEquals(attenuer(THEME.gutterTextColor(), 0.8f), chevron.couleur());
        // les numéros restent : lignes 1, 2 et 5 visibles
        assertEquals(3, surface.textes().size());
    }

    /**
     * D-07 — l'en-tête pliable non replié porte un chevron TRIANGLE
     * PLEIN pointant vers le bas : le contenu se déplie dessous.
     */
    @Test
    void enTetePliablePointeVersLeBas() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        GutterMarks marques = GutterMarks.of(
                List.of(new FoldSpan(1, 3)), new FoldModel(), new int[0]);
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f,
                () -> marques, () -> 12f, () -> 8f, ligne -> 0,
                ligne -> false));
        Document document = Document.of("a\nb\nc\nd\ne");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(5, new FoldModel()), 1L, metrics, THEME,
                0, 5, 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(1, surface.triangles().size());
        TriangleTrace chevron = surface.triangles().get(0);
        assertTrue(chevron.pointeEnBas(),
                "la pointe du chevron déplié regarde vers le bas");
        assertEquals(26f, chevron.cx(), 0.5f,
                "le chevron vit dans la bande de plis");
    }

    /**
     * D-02 — la ligne en erreur porte sa marque dans la coulisse à
     * gauche : un POINT PLEIN à l'encre d'erreur du thème.
     */
    @Test
    void ligneEnErreurPorteSaMarque() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        GutterMarks marques = GutterMarks.of(
                List.of(), new FoldModel(), new int[] {0});
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f,
                () -> marques, () -> 12f));
        Document document = Document.of("a\nb");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(2, new FoldModel()), 1L, metrics, THEME,
                0, 2, 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(1, surface.cercles().size(),
                "une marque d'erreur, un point plein");
        CercleTrace marque = surface.cercles().get(0);
        assertTrue(marque.cx() <= 12f,
                "la marque vit dans la coulisse des marqueurs");
        assertTrue(marque.plein(), "la marque d'erreur se rend PLEINE");
        assertEquals(THEME.syntaxe().couleur(TokenKind.ERROR), marque.couleur());
    }

    /**
     * L'ampoule d'intentions (l'audit Android Studio du 2026-11-14) :
     * le petit icône JAUNE de l'IDE de référence — le disque du verre
     * et son culot — PRIME sur le point d'erreur de la même ligne (au
     * caret qui porte un diagnostic, l'ampoule remplace le marqueur).
     */
    @Test
    void lampouleDIntentionsPrimeSurLePointDerreur() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        GutterMarks marques = GutterMarks.of(List.of(), new FoldModel(),
                new int[] {0});
        GutterPainter couche = new GutterPainter(() -> new PinceauMutable(),
                4f, () -> marques, () -> 12f);
        couche.brancherAmpoules(ligne -> ligne == 0);
        image.addLayer(couche);
        Document document = Document.of("a\nb");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f),
                new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(2, new FoldModel()), 1L, metrics, THEME,
                0, 2, 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(1, surface.cercles().size(),
                "l'ampoule remplace le point d'erreur de sa ligne");
        assertEquals(0xFFFFCB6B, surface.cercles().get(0).couleur(),
                "le jaune de l'ampoule de l'IDE de référence");
        assertEquals(1, surface.rectangles().size(),
                "l'ampoule trace son culot sous le verre");
        assertEquals(0xFFFFCB6B, surface.rectangles().get(0).couleur());
    }

    /** Sans marques, la gouttière ne trace ni trait ni figure : le
     *  chemin reste muet. */
    @Test
    void sansMarquesLaGouttiereResteMuet() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f));
        Document document = Document.of("a\nb");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(2, new FoldModel()), 1L, metrics, THEME,
                0, 2, 28f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(0, surface.traits().size());
        assertEquals(0, surface.rectangles().size());
        assertEquals(0, surface.cercles().size());
        assertEquals(0, surface.triangles().size());
        assertEquals(2, surface.textes().size(), "les numéros restent seuls");
    }

    /** La coulisse de marqueurs élargit la gouttière d'autant (D-01/D-07). */
    @Test
    void laCoulisseDeMarqueursElargitLaGouttiere() {
        assertEquals(30f, GutterGeometry.width(3, 10f, 4f, 12f), 1e-6f,
                "18 px de numéros + 12 px de coulisse");
    }

    /**
     * Numéros masqués : aucune étiquette tracée, la coulisse des
     * marqueurs et la bande de plis restent entières — la bascule des
     * numéros (D-01) ne sacrifie ni les chevrons de pli (D-07) ni les
     * marques d'erreur (D-02).
     */
    @Test
    void numerosMasquesLaCoulisseResteEntiere() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        FoldModel replis = new FoldModel();
        replis.fold(1, 3);
        GutterMarks marques = GutterMarks.of(
                List.of(new FoldSpan(1, 3)), replis, new int[] {0});
        GutterPainter gouttiere = new GutterPainter(() -> new PinceauMutable(), 4f,
                () -> marques, () -> 12f, () -> 8f, ligne -> 0,
                ligne -> false);
        gouttiere.setNumerosVisibles(false);
        image.addLayer(gouttiere);
        Document document = Document.of("a\nb\nc\nd\ne");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        VisualProjection projection = new VisualProjection(5, replis);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 1L, metrics,
                THEME, 0, projection.rowCount(), 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(0, surface.textes().size(), "aucun numéro tracé");
        assertEquals(1, surface.triangles().size(), "le chevron de pli reste");
        assertEquals(1, surface.cercles().size(), "la marque d'erreur reste");
    }

    /**
     * La largeur sans numéros se réduit à la coulisse et à sa
     * respiration : le champ de numéros ne réserve rien (D-01).
     */
    @Test
    void largeurSansNumerosReduiteALaCoulisse() {
        assertEquals(20f, GutterGeometry.laneOnly(4f, 12f), 1e-6f,
                "12 px de coulisse + 2 × 4 px de respiration");
        assertEquals(8f, GutterGeometry.laneOnly(4f, 0f), 1e-6f,
                "sans coulisse, la respiration seule reste");
    }

    /**
     * C-05 — la pastille de couleur se rend depuis la valeur résolue :
     * un carré à droite du champ de numéros, à la couleur du littéral,
     * une seule par ligne porteuse.
     */
    @Test
    void pastilleRendueDepuisLaValeurResolue() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        GutterPainter gouttiere = new GutterPainter(() -> new PinceauMutable(), 8f,
                () -> GutterMarks.VIDE, () -> 0f,
                ligne -> ligne == 1 ? 0xFFFF5722 : 0, ligne -> false);
        image.addLayer(gouttiere);
        Document document = Document.of("<a>\n<color>#FF5722</color>\n</a>");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        float largeur = GutterGeometry.width(3, 10f, 8f);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(3, new FoldModel()), 1L, metrics, THEME,
                0, 3, largeur, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(1, surface.rectangles().size(),
                "une seule pastille, la ligne porteuse seule");
        RectTrace pastille = surface.rectangles().get(0);
        assertEquals(0xFFFF5722, pastille.couleur(),
                "la pastille porte la valeur résolue, pas une encre de thème");
        // Le carré vit dans la respiration à droite du champ : bord de
        // 2 px, côté de 4 px — entre largeur - 6 et largeur - 2.
        assertEquals(largeur - 6f, pastille.left(), 1e-6f);
        assertEquals(largeur - 2f, pastille.right(), 1e-6f);
        // Posé au milieu vertical de la rangée 1 (0-based) : la rangée
        // couvre 20 à 40, le carré 24 à 28.
        assertEquals(24f, pastille.top(), 1e-6f);
        assertEquals(28f, pastille.bottom(), 1e-6f);
    }

    /**
     * C-11 — le repère {@code @Preview} se trace dans la coulisse, à
     * sa ligne ; le rendu du composable reste la part de l'hôte.
     */
    @Test
    void reperePreviewTraceDansLaCoulisseASaLigne() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        GutterPainter gouttiere = new GutterPainter(() -> new PinceauMutable(), 4f,
                () -> GutterMarks.VIDE, () -> 12f,
                ligne -> 0, ligne -> ligne == 1);
        image.addLayer(gouttiere);
        Document document = Document.of("package\n@Preview\nfun Apercu()");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(3, new FoldModel()), 1L, metrics, THEME,
                0, 3, 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        // Le losange du repère : quatre traits, tous dans la coulisse,
        // leur enceinte centrée sur l'axe de la coulisse (6 px).
        assertEquals(4, surface.traits().size());
        assertEquals(THEME.gutterTextColor(), surface.traits().get(0).couleur());
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (TraitTrace trait : surface.traits()) {
            min = Math.min(min, Math.min(trait.x1(), trait.x2()));
            max = Math.max(max, Math.max(trait.x1(), trait.x2()));
        }
        assertEquals(6f, (min + max) / 2f, 1e-6f,
                "le repère vit au centre de la coulisse de 12 px");
        assertTrue(min >= 0f && max <= 12f, "le repère reste dans la coulisse");
    }

    /**
     * M7.5, redressement — le VOILE translucide du fond : un thème à
     * voile le trace sur toute la largeur de la gouttière et la hauteur
     * visible, à sa couleur ; un thème sans voile n'en trace aucun.
     */
    @Test
    void voileTranslucideSurTouteLaGouttiere() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f));
        Document document = Document.of("a\nb");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(2, new FoldModel()), 1L, metrics,
                THEME_VOILE, 0, 2, 28f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(1, surface.rectangles().size(),
                "le voile du fond est l'unique rectangle");
        RectTrace voile = surface.rectangles().get(0);
        assertEquals(0f, voile.left(), 1e-6f);
        assertEquals(28f, voile.right(), 1e-6f,
                "le voile couvre la largeur de la gouttière");
        assertTrue(voile.bottom() >= 40f,
                "le voile couvre la hauteur visible (deux rangées de 20 px)");
        assertEquals(THEME_VOILE.gutterBackground(), voile.couleur());
        assertEquals(2, surface.textes().size(), "les numéros restent");
    }

    /** M7.5, redressement — sans voile (alpha nul), aucun fond tracé. */
    @Test
    void themeSansVoileNeTraceAucunFond() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f));
        Document document = Document.of("a\nb");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(2, new FoldModel()), 1L, metrics, THEME,
                0, 2, 28f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(0, surface.rectangles().size(),
                "un thème sans voile ne trace aucun fond de gouttière");
    }

    /**
     * M7.5, redressement — le chevron vit dans sa CELLULE : bras au
     * tiers du côté (bande 8 × hauteur 20 → cellule 8), centre au
     * MILIEU de la rangée et au centre de la bande — un triangle qui
     * se voit et se vise.
     */
    @Test
    void chevronVitDansSaCelluleEpaisEtCentre() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        GutterMarks marques = GutterMarks.of(
                List.of(new FoldSpan(0, 1)), new FoldModel(), new int[0]);
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f,
                () -> marques, () -> 12f, () -> 8f, ligne -> 0,
                ligne -> false));
        Document document = Document.of("a\nb");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(2, new FoldModel()), 1L, metrics, THEME,
                0, 2, 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(1, surface.triangles().size(),
                "le chevron pliable vit en un triangle plein");
        TriangleTrace chevron = surface.triangles().get(0);
        // la cellule est min(8, 20) = 8 — le triangle en vaut deux cinquièmes
        assertEquals(8f * 0.4f, chevron.rayon(), 1e-6f,
                "le triangle prend sa cellule");
        // le centre : au MILIEU de la rangée — la rangée 0 couvre 0 à 20,
        // le centre vit à 10 — et au centre de la bande (26)
        assertEquals(10f, chevron.cy(), 1e-6f,
                "le chevron vit au milieu de la rangée");
        assertEquals(26f, chevron.cx(), 1e-6f,
                "le chevron vit au centre de la bande de plis");
    }

    /**
     * La reprise du design — le numéro de la ligne COURANTE se graisse
     * à l'encre du texte de base ; les autres restent à l'encre de
     * gouttière, maigres.
     */
    @Test
    void leNumeroCourantSeGraisse() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f));
        Document document = Document.of("a\nb\nc");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        // le curseur vit sur la ligne 1 — son numéro se graisse
        etat.beginFrame(document, MultiSelection.of(SelRange.caretAt(2)),
                new VisualProjection(3, new FoldModel()), 1L, metrics, THEME,
                0, 3, 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(3, surface.textes().size());
        TexteTrace courant = null;
        for (TexteTrace texte : surface.textes()) {
            if ("2".equals(texte.texte())) {
                courant = texte;
            }
        }
        assertTrue(courant != null, "le numéro de la ligne courante se trace");
        assertTrue(courant.gras(), "le numéro courant se graisse");
        assertEquals(THEME.baseTextColor(), courant.couleur(),
                "le numéro courant prend l'encre du texte de base");
        for (TexteTrace texte : surface.textes()) {
            if (!"2".equals(texte.texte())) {
                assertFalse(texte.gras(), "les autres numéros restent maigres");
                assertEquals(THEME.gutterTextColor(), texte.couleur(),
                        "les autres numéros gardent l'encre de gouttière");
            }
        }
    }

    /**
     * D-04, la reprise du design — le point d'arrêt se rend CERCLE :
     * PLEIN à l'encre d'erreur posé, CREUX à l'encre de gouttière
     * désactivé, au centre de la coulisse.
     */
    @Test
    void lePointDArretSeRendCercle() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        GutterMarks marques = GutterMarks.of(List.of(), new FoldModel(),
                new int[0]).avec(MarquesIntegration.of(Map.of(),
                        Map.of(0, EtatArret.POSE, 1, EtatArret.DESACTIVE),
                        null, Set.of()));
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f,
                () -> marques, () -> 12f));
        Document document = Document.of("a\nb");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(2, new FoldModel()), 1L, metrics, THEME,
                0, 2, 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(2, surface.cercles().size(), "deux arrêts, deux cercles");
        CercleTrace pose = surface.cercles().get(0);
        CercleTrace desactive = surface.cercles().get(1);
        assertTrue(pose.plein(), "l'arrêt posé se rend PLEIN");
        assertEquals(THEME.syntaxe().couleur(TokenKind.ERROR), pose.couleur(),
                "l'arrêt posé prend l'encre d'erreur");
        assertFalse(desactive.plein(), "l'arrêt désactivé se rend CREUX");
        assertEquals(THEME.gutterTextColor(), desactive.couleur(),
                "l'arrêt désactivé garde l'encre de gouttière");
        assertEquals(6f, pose.cx(), 0.5f,
                "les deux cercles vivent au centre de la coulisse");
    }

    /**
     * D-05, la reprise du design — le repère d'exécution se rend FLÈCHE
     * PLEINE à l'encre du curseur, par-dessus l'arrêt éventuel.
     */
    @Test
    void laFlecheDExecutionSeRendTriangle() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        GutterMarks marques = GutterMarks.of(List.of(), new FoldModel(),
                new int[0]).avec(MarquesIntegration.of(Map.of(),
                        Map.of(0, EtatArret.POSE), 0, Set.of()));
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f,
                () -> marques, () -> 12f));
        Document document = Document.of("a\nb");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(2, new FoldModel()), 1L, metrics, THEME,
                0, 2, 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(1, surface.triangles().size(),
                "la flèche d'exécution vit en un triangle plein");
        TriangleTrace fleche = surface.triangles().get(0);
        assertFalse(fleche.pointeEnBas(), "la flèche pointe à droite");
        assertEquals(THEME.cursorColor(), fleche.couleur(),
                "la flèche prend l'encre du curseur");
        // la flèche vit PAR-DESSUS l'arrêt : le cercle se trace aussi
        assertEquals(1, surface.cercles().size(),
                "l'arrêt sous la flèche reste tracé");
    }

    /** L'atténuation d'une couleur ARGB — comme la couche. */
    private static int attenuer(int couleur, float facteur) {
        int a = (int) (((couleur >>> 24) & 0xFF)
                * Math.max(0f, Math.min(1f, facteur)));
        return (a << 24) | (couleur & 0x00FFFFFF);
    }
}
