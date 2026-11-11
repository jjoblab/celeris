package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.JsonSyntax;
import jo.celeris.core.syntax.KotlinSyntax;
import jo.celeris.core.syntax.KtsSyntax;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * Reproduction du plantage signalé : l'activation du retour à la ligne
 * souple fait fermer l'application. Le chemin exact de la vue est
 * rejoué sur les vrais contenus de la démonstration — analyse, plis,
 * gouttière, sélection, visée — image après image, à travers les
 * couches du pipeline.
 */
class RetourSoupleCrashTest {

    /** Règle de police régulière : 8 pixels par caractère, 16 par rangée. */
    private static final class RegleDroite implements FontRuler {
        @Override
        public float charWidth(FontSpec spec) {
            return spec.size() / 2f;
        }

        @Override
        public float lineHeight(FontSpec spec) {
            return spec.size() * 8f / 7f;
        }

        @Override
        public float ascent(FontSpec spec) {
            return spec.size() * 6f / 7f;
        }

        @Override
        public float descent(FontSpec spec) {
            return spec.size() / 7f;
        }
    }

    /** Pinceau mutable minimal. */
    private static final class PinceauSimple implements TextBrush {
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

    /** Fabrique de pinceaux simples. */
    private static final class FabriqueSimple implements BrushFactory {
        @Override
        public TextBrush newBrush() {
            return new PinceauSimple();
        }
    }

    /** Surface enregistreuse : tout tracé se note, rien ne casse. */
    private static final class SurfaceEnregistreuse implements DrawSurface {
        private final List<String> textes = new ArrayList<>();
        private int rectangles;
        private int traits;

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            textes.add(text);
        }

        @Override
        public void drawText(CharSequence text, int start, int end, float x,
                float y, TextBrush brush) {
            textes.add(text.subSequence(start, end).toString());
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            rectangles++;
        }

        @Override
        public void drawLine(float x1, float y1, float x2, float y2,
                TextBrush brush) {
            traits++;
        }
    }

    private static final String KOTLIN = """
            package com.exemple.outils

            /**
             * Conversions entre les unités d'interface.
             */
            object Conversions {
                private const val SEUIL = 0.5f

                fun dpVersPx(dp: Float, densite: Float): Int {
                    return (dp * densite + SEUIL).toInt()
                }
            }
            """;

    private static final String JAVA = """
            package com.exemple.app;

            import java.util.ArrayList;
            import java.util.List;

            public final class ServiceUtilisateur {
                private final List<String> noms = new ArrayList<>();

                public boolean inscrire(String nom) {
                    if (nom == null || nom.isBlank()) {
                        throw new IllegalArgumentException("nom requis");
                    }
                    return noms.add(nom);
                }
            }
            """;

    private static final String JSON = """
            {
              "catalogue": {
                "nom": "exemples",
                "entrees": [
                  { "id": "A-13", "libelle": "annulation groupee" },
                  { "id": "B-03", "libelle": "completion de base" }
                ]
              }
            }
            """;

    private static final String KTS = """
            plugins {
                alias(libs.plugins.android.application)
            }

            android {
                namespace = "com.exemple.app"
                compileSdk = 36
            }
            """;

    private static final EditorTheme THEME = EditorThemes.CLAIR;

    /**
     * Rejoue une image complète de la vue : projection, habillage,
     * gouttière, sélection, texte — le chemin exact de
     * {@code EditorView.preparerEtatImage} et {@code onDraw}.
     */
    private SurfaceEnregistreuse image(TextSession session, FoldModel plis,
            EditorSyntax syntaxe, int colonnes, int hauteur, int largeur,
            int premiereRangeeClic) {
        Document document = session.document();
        EditorMetrics metrics = EditorMetrics.of(
                new FontSpec("mono", 14f), new RegleDroite());
        VisualProjection projection =
                new VisualProjection(document, plis, colonnes);
        RowLayoutCache cache = new RowLayoutCache();
        RebindingRowSource source = new RebindingRowSource(
                new DocumentRowSource(document, projection, metrics, 0));
        source.set(syntaxe.habiller(
                new DocumentRowSource(document, projection, metrics, colonnes),
                document));

        MarquesGouttiere marquesGouttiere = new MarquesGouttiere();
        BrushFactory fabrique = new FabriqueSimple();
        ScrollFrame trace = new ScrollFrame();
        GutterLanes coulisses = marquesGouttiere.coulisses(syntaxe.active());
        trace.addLayer(new GutterPainter(fabrique, 8f,
                () -> marquesGouttiere.marques(syntaxe.marques()),
                () -> coulisses.laneWidth(metrics.charWidth(), 8f)));
        trace.addLayer(new SelectionPainter(fabrique));
        trace.addLayer(new TextPainter(fabrique, cache, source));

        float coulisse = coulisses.laneWidth(metrics.charWidth(), 8f);
        float largeurGouttiere = GutterLayout.width(
                GutterLayout.digitsFor(document.lineCount()),
                metrics.charWidth(), 1f, 8f, coulisses);
        int premiere = 0;
        int visibles = ScrollGeometry.visibleRows(hauteur, metrics.lineHeight());
        syntaxe.fenetre(projection, premiere, visibles);

        FrameState etat = new FrameState();
        etat.beginFrame(document, session.selection(), projection,
                document.revision(), metrics, THEME, premiere, visibles,
                largeurGouttiere, 0f, 0f);
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        trace.drawFrame(surface, etat);
        viserEtDevoilerChaqueColonne(document, projection, metrics,
                largeurGouttiere, largeur, hauteur);
        return surface;
    }

    /**
     * La visée et le dévoilement sous coupures, comme au toucher :
     * chaque colonne visible de chaque rangée se vise puis se dévoile.
     */
    private static void viserEtDevoilerChaqueColonne(Document document,
            VisualProjection projection, EditorMetrics metrics,
            float largeurGouttiere, int largeur, int hauteur) {
        HitTest viseur = new HitTest(metrics);
        for (int rangee = 0; rangee < projection.rowCount(); rangee++) {
            int ligne = projection.firstLineOfRow(rangee);
            int debut = projection.spanStartAtRow(rangee);
            int fin = Math.min(projection.spanEndAtRow(rangee),
                    document.lineEnd(ligne) - document.lineStart(ligne));
            for (int colonne = 0; colonne <= fin - debut; colonne++) {
                int vise = viseur.offsetAt(document, projection, metrics,
                        largeurGouttiere, 0f, 0f,
                        largeurGouttiere + colonne * metrics.charWidth() + 1f,
                        rangee * metrics.lineHeight() + 1f);
                CaretReveal.reveal(vise, document, projection, metrics,
                        largeurGouttiere, largeur, hauteur, 0f, 0f, 8f);
            }
        }
    }

    private void rejouer(String contenu, LanguageSyntax langue, int colonnes) {
        TextSession session = TextSession.of(contenu, System::nanoTime);
        FoldModel plis = new FoldModel();
        EditorSyntax syntaxe = new EditorSyntax(plis);
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(langue));
        SurfaceEnregistreuse surface =
                image(session, plis, syntaxe, colonnes, 640, 1080, 0);
        assertTrue(!surface.textes.isEmpty());
    }

    /**
     * Le retour souple s'active sur chaque langage de la démonstration
     * sans lever d'exception — le plantage signalé.
     */
    @Test
    void retourSouple_chaqueLangageSansCrash() {
        rejouer(KOTLIN, KotlinSyntax.INSTANCE, 40);
        rejouer(JAVA, JavaSyntax.INSTANCE, 40);
        rejouer(JSON, JsonSyntax.INSTANCE, 40);
        rejouer(KTS, KtsSyntax.INSTANCE, 40);
    }

    /**
     * Le retour souple survit aux plis : plier une région puis tracer
     * sous coupures — numéros, chevrons, tranches colorées.
     */
    @Test
    void retourSouple_avecPliSansCrash() {
        TextSession session = TextSession.of(KOTLIN, System::nanoTime);
        FoldModel plis = new FoldModel();
        EditorSyntax syntaxe = new EditorSyntax(plis);
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(KotlinSyntax.INSTANCE));
        // Première image sans pli : les régions pliables se publient.
        image(session, plis, syntaxe, 40, 640, 1080, 0);
        // Plier la première région pliable publiée.
        boolean plie = false;
        for (int ligne = 0; ligne < session.document().lineCount()
                && !plie; ligne++) {
            plie = syntaxe.basculerPli(ligne);
        }
        assertTrue(plie, "une région pliable devait exister");
        SurfaceEnregistreuse surface =
                image(session, plis, syntaxe, 40, 640, 1080, 0);
        assertTrue(!surface.textes.isEmpty());
    }

    /**
     * Le retour souple avec un budget d'une colonne : la coupure par
     * caractère, le pire cas de PIEGE-08.
     */
    @Test
    void retourSouple_uneColonneSansCrash() {
        rejouer(KOTLIN, KotlinSyntax.INSTANCE, 1);
        rejouer(JAVA, JavaSyntax.INSTANCE, 1);
    }

    /**
     * La sélection s'étend sous coupures puis se trace : le glissement
     * du doigt rejoué par offsets visés.
     */
    @Test
    void retourSouple_selectionSousCoupuresSansCrash() {
        TextSession session = TextSession.of(KOTLIN, System::nanoTime);
        FoldModel plis = new FoldModel();
        EditorSyntax syntaxe = new EditorSyntax(plis);
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(KotlinSyntax.INSTANCE));
        Document document = session.document();
        session.setSelection(MultiSelection.of(new SelRange(
                document.lineStart(5),
                Math.min(document.lineStart(5) + 80, document.length()))));
        SurfaceEnregistreuse surface =
                image(session, plis, syntaxe, 40, 640, 1080, 0);
        assertTrue(surface.rectangles > 0, "la sélection devait se tracer");
    }
}
