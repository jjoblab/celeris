package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * Tests du bornage de la fenêtre d'image au contenu : les rangées
 * au-delà de la dernière ne se tracent jamais — sans ce bornage, la
 * dernière ligne se répétait sous la fenêtre sans coupure, et
 * débordait sous coupure (le plantage du retour à la ligne souple).
 */
class FenetreBorneeTest {

    /** Règle de police régulière : 8 pixels par caractère, 16 par rangée. */
    private static final class RegleDroite implements FontRuler {
        @Override
        public float charWidth(FontSpec spec) {
            return spec.size() / 2f;
        }

        @Override
        public float lineHeight(FontSpec spec) {
            return spec.size();
        }

        @Override
        public float ascent(FontSpec spec) {
            return spec.size() * 3f / 4f;
        }

        @Override
        public float descent(FontSpec spec) {
            return spec.size() / 4f;
        }
    }

    /**
     * Le bornage nu : la fenêtre ne descend jamais sous la dernière
     * rangée, quel que excès qu'on lui donne.
     */
    @Test
    void borneesAuContenu_fenetreRognee() {
        assertEquals(19, ScrollGeometry.borneesAuContenu(0, 40, 19));
        assertEquals(4, ScrollGeometry.borneesAuContenu(15, 30, 19));
        assertEquals(0, ScrollGeometry.borneesAuContenu(19, 10, 19));
        assertEquals(0, ScrollGeometry.borneesAuContenu(0, 10, 0));
        assertEquals(19, ScrollGeometry.borneesAuContenu(0, 19, 19));
    }

    /**
     * L'état d'image borne sa fenêtre : une image qui demande plus de
     * rangées qu'il n'en existe n'en annonce que le contenu.
     */
    @Test
    void beginFrame_fenetreBorneeAuContenu() {
        Document document = Document.of("aaaa\nbbbb\ncccc");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.of(
                jo.celeris.core.text.SelRange.caretAt(0)), projection,
                document.revision(),
                EditorMetrics.of(new FontSpec("mono", 16f), new RegleDroite()),
                EditorThemes.CLAIR, 0, 40, 40f, 0f, 0f);
        assertEquals(3, projection.rowCount());
        assertEquals(0, etat.firstRow());
        assertEquals(3, etat.visibleRows());
    }

    /**
     * Sous retour à la ligne, le même bornage tient : la fenêtre
     * demandée dépasse le contenu réel, l'image n'annonce que lui.
     */
    @Test
    void beginFrame_fenetreBorneeSousCoupures() {
        Document document = Document.of("aaaaaaaaaa\nbb\ncc");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.of(
                jo.celeris.core.text.SelRange.caretAt(0)), projection,
                document.revision(),
                EditorMetrics.of(new FontSpec("mono", 16f), new RegleDroite()),
                EditorThemes.CLAIR, 0, 40, 40f, 0f, 0f);
        // Ligne 0 coupée en 3 rangées, lignes 1 et 2 entières : 5 rangées.
        assertEquals(5, projection.rowCount());
        assertEquals(5, etat.visibleRows());
    }

    /**
     * La couche de texte, servie par une fenêtre bornée, ne consulte
     * jamais une rangée au-delà du contenu — la rangée fantôme qui
     * faisait fermer l'application n'existe plus.
     */
    @Test
    void textPainter_rangeesFantomesImpossibles() {
        TextSession session = TextSession.of("abcdefghij", System::nanoTime);
        Document document = session.document();
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 16f),
                new RegleDroite());
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);
        DocumentRowSource source = new DocumentRowSource(document, projection,
                metrics, 4);
        RowLayoutCache cache = new RowLayoutCache();
        List<String> textes = new ArrayList<>();
        DrawSurface surface = new DrawSurface() {
            @Override
            public void drawText(String text, float x, float y,
                    TextBrush brush) {
                textes.add(text);
            }

            @Override
            public void drawText(CharSequence text, int start, int end,
                    float x, float y, TextBrush brush) {
                textes.add(text.subSequence(start, end).toString());
            }

            @Override
            public void drawRect(float left, float top, float right,
                    float bottom, TextBrush brush) {
                // la couche de texte ne trace aucun rectangle
            }
        };
        FrameState etat = new FrameState();
        // La fenêtre demandée dépasse largement le contenu.
        etat.beginFrame(document, session.selection(), projection,
                document.revision(), metrics, EditorThemes.CLAIR, 0, 100,
                40f, 0f, 0f);
        TextPainter peintre = new TextPainter(new BrushFactory() {
            @Override
            public TextBrush newBrush() {
                return new TextBrush() {
                    private int couleur;

                    @Override
                    public void setColor(int argb) {
                        couleur = argb;
                    }

                    @Override
                    public int color() {
                        return couleur;
                    }
                };
            }
        }, cache, source);
        peintre.paint(surface, etat);
        // Le contenu coupé en 4 colonnes : 3 rangées, 3 textes — pas un
        // de plus, jamais l'exception de la rangée fantôme.
        assertEquals(3, textes.size());
        assertEquals("abcd", textes.get(0));
        assertEquals("efgh", textes.get(1));
        assertEquals("ij", textes.get(2));
    }

    /**
     * La couche repose sa police à chaque image (PIEGE-22) : le pinceau
     * reçoit la police des métriques avant tout tracé, une couche au
     * pinceau fraîchement créé ne trace jamais à la police par défaut.
     */
    @Test
    void painterLayer_policeReposeeAChaqueImage() {
        List<FontSpec> policesRecues = new ArrayList<>();
        TextBrush pinceauEspion = new TextBrush() {
            private int couleur;

            @Override
            public void setColor(int argb) {
                couleur = argb;
            }

            @Override
            public int color() {
                return couleur;
            }

            @Override
            public void setFont(FontSpec police) {
                policesRecues.add(police);
            }
        };
        PainterLayer couche = new PainterLayer(new BrushFactory() {
            @Override
            public TextBrush newBrush() {
                return pinceauEspion;
            }
        }) {
            @Override
            protected int baseColor(FrameState frame) {
                return 0xFF000000;
            }

            @Override
            protected void onPaint(DrawSurface surface, FrameState frame) {
                // le tracé concret n'importe pas ici
            }
        };
        Document document = Document.of("abc");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        FrameState etat = new FrameState();
        FontSpec police = new FontSpec("mono", 22f);
        etat.beginFrame(document, MultiSelection.of(
                jo.celeris.core.text.SelRange.caretAt(0)), projection,
                document.revision(),
                EditorMetrics.of(police, new RegleDroite()),
                EditorThemes.CLAIR, 0, 3, 40f, 0f, 0f);
        couche.paint(new DrawSurface() {
            @Override
            public void drawText(String text, float x, float y,
                    TextBrush brush) {
                // muet
            }

            @Override
            public void drawRect(float left, float top, float right,
                    float bottom, TextBrush brush) {
                // muet
            }
        }, etat);
        assertEquals(1, policesRecues.size());
        assertSame(police, policesRecues.get(0));
    }

    /**
     * Le pinceau concret de la plateforme implante la police (PIEGE-22)
     * — sans implantation, le repli muet de l'interface laisserait le
     * tracé à la police par défaut, texte éparpillé garanti.
     */
    @Test
    void paintBrush_implanteLaPolice() throws NoSuchMethodException {
        assertEquals(PaintBrush.class,
                PaintBrush.class.getMethod("setFont", FontSpec.class)
                        .getDeclaringClass());
        assertTrue(TextBrush.class.isAssignableFrom(PaintBrush.class));
    }
}
