package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.syntax.FoldSpan;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;

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

    /** Pinceau mutable minimal : la couleur courante et l'épaisseur de
     *  trait posée, rien d'autre. */
    private static final class PinceauMutable implements TextBrush {
        private int couleur;
        private float epaisseur;

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

        @Override
        public void setStrokeWidth(float pixels) {
            epaisseur = pixels;
        }
    }

    /** Un texte tracé : son contenu, son abscisse et sa couleur. */
    private record TexteTrace(String texte, float x, int couleur) {
    }

    /** Surface enregistreuse de textes. */
    private static final class SurfaceTextes implements DrawSurface {
        private final List<TexteTrace> textes = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            textes.add(new TexteTrace(text, x, brush.color()));
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

    /** Un trait tracé : ses deux extrémités et sa couleur. */
    private record TraitTrace(float x1, float y1, float x2, float y2, int couleur) {
    }

    /** Un rectangle tracé : ses bornes et sa couleur. */
    private record RectTrace(float left, float top, float right, float bottom, int couleur) {
    }

    /** Surface enregistreuse complète : textes, traits et rectangles. */
    private static final class SurfaceMarques implements DrawSurface {
        private final List<TexteTrace> textes = new ArrayList<>();
        private final List<TraitTrace> traits = new ArrayList<>();
        private final List<RectTrace> rectangles = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            textes.add(new TexteTrace(text, x, brush.color()));
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

        List<TexteTrace> textes() {
            return textes;
        }

        List<TraitTrace> traits() {
            return traits;
        }

        List<RectTrace> rectangles() {
            return rectangles;
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
     * D-07 — l'en-tête replié porte son chevron : deux traits, pointe
     * à droite, dans la coulisse des marqueurs.
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
                () -> marques, () -> 12f));
        Document document = Document.of("a\nb\nc\nd\ne");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        VisualProjection projection = new VisualProjection(5, replis);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 1L, metrics,
                THEME, 0, projection.rowCount(), 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(2, surface.traits().size(), "le chevron replié vit en deux traits");
        TraitTrace premier = surface.traits().get(0);
        TraitTrace second = surface.traits().get(1);
        // pointe à droite : le premier trait finit où le second commence
        assertEquals(premier.x2(), second.x1(), 1e-6f,
                "les deux traits se rejoignent à la pointe");
        assertTrue(premier.y1() < second.y1(),
                "le premier trait part plus haut que le second");
        assertTrue(premier.x1() < premier.x2(),
                "la pointe du chevron replié regarde vers la droite");
        assertTrue(premier.x2() <= 12f,
                "le chevron vit dans la coulisse des marqueurs");
        assertEquals(THEME.gutterTextColor(), premier.couleur());
        // les numéros restent : lignes 1, 2 et 5 visibles
        assertEquals(3, surface.textes().size());
    }

    /**
     * D-07 — l'en-tête pliable non replié porte un chevron pointant
     * vers le bas : le contenu se déplie dessous.
     */
    @Test
    void enTetePliablePointeVersLeBas() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        GutterMarks marques = GutterMarks.of(
                List.of(new FoldSpan(1, 3)), new FoldModel(), new int[0]);
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f,
                () -> marques, () -> 12f));
        Document document = Document.of("a\nb\nc\nd\ne");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(5, new FoldModel()), 1L, metrics, THEME,
                0, 5, 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(2, surface.traits().size());
        TraitTrace premier = surface.traits().get(0);
        TraitTrace second = surface.traits().get(1);
        // pointe en bas : le premier trait finit où le second commence
        assertEquals(premier.y2(), second.y1(), 1e-6f,
                "les deux traits se rejoignent à la pointe basse");
        assertTrue(premier.y2() > premier.y1(),
                "la pointe du chevron déplié regarde vers le bas");
        assertTrue(premier.x1() < second.x2(), "le chevron s'ouvre vers le haut");
    }

    /**
     * D-02 — la ligne en erreur porte sa marque dans la coulisse, à
     * l'encre d'erreur du thème.
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

        assertEquals(1, surface.rectangles().size(), "une marque d'erreur, un rectangle");
        RectTrace marque = surface.rectangles().get(0);
        assertTrue(marque.right() <= 12f,
                "la marque vit dans la coulisse des marqueurs");
        assertEquals(THEME.syntaxe().couleur(TokenKind.ERROR), marque.couleur());
    }

    /** Sans marques, la gouttière ne trace ni trait ni rectangle : le chemin reste muet. */
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
     * marqueurs reste entière — la bascule des numéros (D-01) ne
     * sacrifie ni les chevrons de pli (D-07) ni les marques d'erreur
     * (D-02).
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
                () -> marques, () -> 12f);
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
        assertEquals(2, surface.traits().size(), "le chevron de pli reste");
        assertEquals(1, surface.rectangles().size(), "la marque d'erreur reste");
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
     * tiers du côté (coulisse 12 × hauteur 20 → cellule 12, bras 4),
     * trait ÉPAIS (un huitième de côté, deux pixels au moins), centre
     * au MILIEU de la rangée — plus jamais un chevron minuscule au
     * quart supérieur.
     */
    @Test
    void chevronVitDansSaCelluleEpaisEtCentre() {
        SurfaceMarques surface = new SurfaceMarques();
        ScrollFrame image = new ScrollFrame();
        PinceauMutable pinceau = new PinceauMutable();
        GutterMarks marques = GutterMarks.of(
                List.of(new FoldSpan(0, 1)), new FoldModel(), new int[0]);
        image.addLayer(new GutterPainter(() -> pinceau, 4f,
                () -> marques, () -> 12f));
        Document document = Document.of("a\nb");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(2, new FoldModel()), 1L, metrics, THEME,
                0, 2, 30f, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(2, surface.traits().size(), "le chevron pliable vit en deux traits");
        // les bras : la cellule est min(12, 20) = 12, le bras en vaut le
        // tiers — le chevron s'ouvre sur 8 px de large
        TraitTrace premier = surface.traits().get(0);
        TraitTrace second = surface.traits().get(1);
        assertEquals(8f, Math.abs(premier.x1() - second.x2()), 1e-6f,
                "les bras prennent le tiers de la cellule");
        // le centre : au MILIEU de la rangée — la rangée 0 couvre 0 à 20,
        // le centre vit à 10, le chevron l'encadre de bras/2
        float centreY = (premier.y1() + second.y1()) / 2f;
        assertEquals(10f, centreY, 1e-6f,
                "le chevron vit au milieu de la rangée, plus au quart");
        // le trait : épais — deux pixels au moins, un huitième de la cellule
        assertTrue(pinceau.epaisseur() >= 2f,
                "le trait du chevron se pose épais (au moins deux pixels)");
        assertEquals(Math.max(2f, 12f / 8f), pinceau.epaisseur(), 1e-6f);
    }
}
