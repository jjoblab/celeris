package jo.celeris.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * Le harnais de test de vue (ADR-013) : une {@link EditorView} réelle,
 * mesurée et posée, dont le travail d'analyse ne part que sur ordre
 * (exécuteur manuel), dessinée sur un canvas enregistreur qui capte
 * chaque tracé de texte avec sa couleur.
 *
 * <p>Le banc ne simule rien : les {@link MotionEvent} sont de vrais
 * événements multi-pointeurs, la connexion d'entrée est celle de la vue,
 * l'image passe par {@code draw}. Les tests de vue de M7.5 (pincement,
 * gouttière, coloration, popups) s'y servent tous — un seul harnais, une
 * seule discipline (ADR-010).</p>
 */
final class EditorTestRig {

    /** La fenêtre du banc : largeur d'un téléphone moyen. */
    static final int LARGEUR = 1080;
    /** La fenêtre du banc : hauteur d'un téléphone moyen. */
    static final int HAUTEUR = 1920;

    private final EditorView vue;
    private final ManualAnalysisExecutor executeur = new ManualAnalysisExecutor();

    private EditorTestRig(Context context) {
        vue = new EditorView(context, null, executeur);
        vue.measure(LARGEUR, HAUTEUR);
        vue.layout(0, 0, LARGEUR, HAUTEUR);
    }

    /**
     * Le banc sur le contenu donné : une session fraîche, jamais éditée
     * — la révision part de zéro.
     *
     * @param context  le contexte Robolectric
     * @param contenu  le texte initial du document
     * @return le banc posé et mesuré
     */
    static EditorTestRig sur(Context context, String contenu) {
        EditorTestRig banc = new EditorTestRig(context);
        banc.vue.setSession(TextSession.of(contenu, System::nanoTime));
        return banc;
    }

    /** La vue sous test. */
    EditorView vue() {
        return vue;
    }

    /** L'exécuteur manuel du travail d'analyse. */
    ManualAnalysisExecutor executeur() {
        return executeur;
    }

    /**
     * Jette le travail d'analyse en attente : la publication ne partira
     * que sur le prochain {@link ManualAnalysisExecutor#drain()}.
     */
    void retenirAnalyse() {
        executeur.oublier();
    }

    /**
     * Publie l'analyse en attente : les tâches retenues s'exécutent sur
     * le fil du test, la publication arrive avant le retour.
     */
    void publierAnalyse() {
        executeur.drain();
    }

    /**
     * Dessine une image et rend le canvas enregistreur : chaque tracé
     * de texte y vit avec sa couleur.
     *
     * @return les tracés de l'image
     */
    RecordingCanvas image() {
        RecordingCanvas canvas = new RecordingCanvas();
        vue.draw(canvas);
        return canvas;
    }

    /**
     * Le pincement à deux doigts autour du point donné : l'écartement
     * passe de l'initial au final en autant d'étapes, quinze événements
     * de déplacement par défaut — le geste réaliste du zoom (D-15).
     *
     * @param cx            l'abscisse du centre du pincement
     * @param cy            l'ordonnée du centre du pincement
     * @param ecartInitial  l'écartement des doigts au départ, en pixels
     * @param ecartFinal    l'écartement des doigts à l'arrivée, en pixels
     * @param etapes        le nombre d'événements de déplacement
     */
    void pincer(float cx, float cy, float ecartInitial, float ecartFinal,
            int etapes) {
        long depart = SystemClock.uptimeMillis();
        long t = depart;
        vue.onTouchEvent(obtenir(depart, t, MotionEvent.ACTION_DOWN,
                cx, cy - ecartInitial / 2f));
        t += 16L;
        vue.onTouchEvent(deuxDoigts(t, MotionEvent.ACTION_POINTER_DOWN
                | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                cx, cy - ecartInitial / 2f, cx, cy + ecartInitial / 2f));
        for (int etape = 1; etape <= etapes; etape++) {
            t += 16L;
            float ecart = ecartInitial
                    + (ecartFinal - ecartInitial) * etape / etapes;
            vue.onTouchEvent(deuxDoigts(t, MotionEvent.ACTION_MOVE,
                    cx, cy - ecart / 2f, cx, cy + ecart / 2f));
        }
        t += 16L;
        vue.onTouchEvent(deuxDoigts(t, MotionEvent.ACTION_POINTER_UP
                | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                cx, cy - ecartFinal / 2f, cx, cy + ecartFinal / 2f));
        t += 16L;
        vue.onTouchEvent(obtenir(depart, t, MotionEvent.ACTION_UP,
                cx, cy - ecartFinal / 2f));
    }

    /** Valide du texte par la vraie connexion d'entrée de la vue. */
    void validerTexte(CharSequence texte) {
        connexion().commitText(texte, 1);
    }

    /** Pose du texte en composition par la vraie connexion d'entrée. */
    void composerTexte(CharSequence texte) {
        connexion().setComposingText(texte, 1);
    }

    private android.view.inputmethod.InputConnection connexion() {
        return vue.onCreateInputConnection(
                new android.view.inputmethod.EditorInfo());
    }

    static MotionEvent obtenir(long depart, long event, int action,
            float x, float y) {
        return MotionEvent.obtain(depart, event, action, x, y, 0);
    }

    static MotionEvent deuxDoigts(long event, int action,
            float x0, float y0, float x1, float y1) {
        MotionEvent.PointerProperties[] doigts =
                new MotionEvent.PointerProperties[2];
        for (int i = 0; i < 2; i++) {
            doigts[i] = new MotionEvent.PointerProperties();
            doigts[i].id = i;
            doigts[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
        }
        MotionEvent.PointerCoords[] positions =
                new MotionEvent.PointerCoords[2];
        for (int i = 0; i < 2; i++) {
            positions[i] = new MotionEvent.PointerCoords();
            positions[i].orientation = 0f;
            positions[i].pressure = 1f;
            positions[i].size = 1f;
        }
        positions[0].x = x0;
        positions[0].y = y0;
        positions[1].x = x1;
        positions[1].y = y1;
        return MotionEvent.obtain(event, event, action, 2, doigts, positions,
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
    }

    /**
     * L'exécuteur manuel du travail d'analyse : chaque tâche soumise
     * reste en attente — la publication ne part que sur
     * {@link #drain()}, sur le fil du test.
     */
    static final class ManualAnalysisExecutor implements Executor {

        private final List<Runnable> taches = new ArrayList<>();

        @Override
        public void execute(Runnable commande) {
            taches.add(commande);
        }

        /** Le nombre de tâches en attente. */
        int enAttente() {
            return taches.size();
        }

        /** Oublie les tâches en attente. */
        void oublier() {
            taches.clear();
        }

        /**
         * Exécute les tâches en attente, en tours bornés : une tâche
         * peut en soumettre une autre (relance d'analyse), jamais de
         * boucle infinie au banc.
         */
        void drain() {
            for (int tour = 0; tour < 64 && !taches.isEmpty(); tour++) {
                List<Runnable> enAttente = new ArrayList<>(taches);
                taches.clear();
                for (Runnable tache : enAttente) {
                    tache.run();
                }
            }
        }
    }

    /**
     * Le canvas enregistreur : chaque tracé de texte se capte avec sa
     * couleur et sa position — le banc juge la couleur des mots image
     * par image (PIEGE-26).
     */
    static final class RecordingCanvas extends Canvas {

        /** Un tracé de texte : la tranche, sa couleur, sa position. */
        record TextDraw(CharSequence text, int color, float x, float y) {
        }

        private final List<TextDraw> traces = new ArrayList<>();

        /** Les tracés de l'image, dans l'ordre du dessin. */
        List<TextDraw> traces() {
            return List.copyOf(traces);
        }

        /**
         * La couleur du premier tracé qui contient le fragment donné.
         *
         * @param fragment le fragment cherché (un mot, un signe)
         * @return la couleur ARGB du tracé, 0 si le fragment n'est pas
         *         tracé
         */
        int couleurDe(String fragment) {
            for (TextDraw trace : traces) {
                if (trace.text().toString().contains(fragment)) {
                    return trace.color();
                }
            }
            return 0;
        }

        @Override
        public void drawText(String text, float x, float y, Paint paint) {
            traces.add(new TextDraw(text, paint.getColor(), x, y));
        }

        @Override
        public void drawText(CharSequence text, int start, int end,
                float x, float y, Paint paint) {
            traces.add(new TextDraw(text.subSequence(start, end),
                    paint.getColor(), x, y));
        }

        @Override
        public void drawText(String text, int start, int end,
                float x, float y, Paint paint) {
            traces.add(new TextDraw(text.substring(start, end),
                    paint.getColor(), x, y));
        }

        @Override
        public void drawText(char[] text, int index, int count,
                float x, float y, Paint paint) {
            traces.add(new TextDraw(new String(text, index, count),
                    paint.getColor(), x, y));
        }
    }
}
