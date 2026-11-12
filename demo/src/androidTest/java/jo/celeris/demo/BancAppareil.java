package jo.celeris.demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;

/**
 * Le socle commun des bancs d'appareil du second étage (C11,
 * M7.5) : ouvre l'écran d'édition de la démonstration, porte la
 * frappe par la connexion d'entrée (la voie exacte de l'IME), pose
 * la retenue d'analyse (le crochet de débogage), distribue les
 * pincements à la vue (la voie exacte du banc JVM — l'injection au
 * système, sur l'image ATD, coalesçait le geste : la vitre garde le
 * dépôt) et capture LE RENDU RÉEL DE LA VUE par son propre dessin
 * (le vrai Skia de l'appareil — le compositeur de l'image ATD
 * demeure muet, son GPU v37 rejette les options d'AGP).
 *
 * <p>Le curseur se porte en fin de document par la sélection de la
 * connexion d'entrée — la connexion borne d'elle-même les valeurs
 * hors bornes à la longueur du document : la frappe vit toujours en
 * fin, les premières lignes restent jamais touchées.</p>
 */
final class BancAppareil {

    /** Le fichier ouvert par les bancs : Java coloré, avec identifiants. */
    static final String FICHIER = "ServiceUtilisateur.java";

    /** La retenue d'analyse des bancs à captures : trois secondes. */
    static final long RETENUE_MS = 2500L;

    private BancAppareil() {
    }

    /** Ouvre l'écran d'édition sur le fichier d'exemple du banc. */
    static ActivityScenario<EditorActivity> ouvrirScenario() {
        Intent intention = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EditorActivity.class);
        intention.putExtra(EditorActivity.EXTRA_FICHIER, FICHIER);
        return ActivityScenario.launch(intention);
    }

    /** L'éditeur de l'écran — le banc le dit si la vitre ne le porte pas. */
    static EditorView editeur(Activity ecran) {
        EditorView vue = ecran.findViewById(R.id.editeur);
        if (vue == null) {
            throw new AssertionError("l'éditeur ne vit pas à l'écran");
        }
        return vue;
    }

    /** La couche des popups de l'écran — le banc le dit si elle manque. */
    static PopupLayer couchePopups(Activity ecran) {
        PopupLayer couche = ecran.findViewById(R.id.couche_popups);
        if (couche == null) {
            throw new AssertionError("la couche des popups ne vit pas à l'écran");
        }
        return couche;
    }

    /**
     * Attend la mise en page de l'éditeur : le premier passage de
     * mesures. Sans cette attente, les captures mesureraient une vue
     * encore vide. L'écran se réveille au passage — la politique
     * d'entrée jette les gestes d'une vitre endormie.
     */
    static EditorView attendreVue(ActivityScenario<EditorActivity> scenario)
            throws Exception {
        appareil().wakeUp();
        appareil().executeShellCommand("svc power stayon true");
        appareil().executeShellCommand("wm dismiss-keyguard");
        Thread.sleep(400L);
        EditorView[] vue = new EditorView[1];
        for (int essai = 0; essai < 50; essai++) {
            scenario.onActivity(ecran -> {
                EditorView candidat = editeur(ecran);
                if (candidat.getWidth() > 0 && candidat.getHeight() > 0) {
                    vue[0] = candidat;
                }
            });
            if (vue[0] != null) {
                return vue[0];
            }
            Thread.sleep(100L);
        }
        throw new AssertionError("l'éditeur n'a jamais été mesuré à l'écran");
    }

    /**
     * Pose la retenue d'analyse — le crochet de débogage du banc : la
     * publication attend le calme du doigt, les captures prennent la
     * frappe AVANT l'analyse.
     */
    static void retenirAnalyse(ActivityScenario<EditorActivity> scenario,
            long delaiMs) {
        scenario.onActivity(ecran -> editeur(ecran).setRetenueAnalyseMs(delaiMs));
    }

    /**
     * Focalise l'éditeur : le focus d'entrée existe AVANT le popup —
     * le banc attend l'installation de l'IME pour des mesures stables.
     */
    static void focaliser(ActivityScenario<EditorActivity> scenario)
            throws InterruptedException {
        scenario.onActivity(ecran -> editeur(ecran).requestFocus());
        Thread.sleep(800L);
    }

    /** La connexion d'entrée de l'éditeur : la voie exacte de l'IME. */
    static InputConnection connexion(EditorView vue) {
        InputConnection connexion = vue.onCreateInputConnection(new EditorInfo());
        if (connexion == null) {
            throw new AssertionError("l'éditeur ne sert aucune connexion d'entrée");
        }
        return connexion;
    }

    /**
     * Frappe un texte en fin de document par la voie de l'IME : le
     * curseur se porte en fin (la sélection borne), le texte se
     * commet, l'analyse retenue suit son cours.
     */
    static void frapper(ActivityScenario<EditorActivity> scenario, String texte) {
        scenario.onActivity(ecran -> {
            InputConnection connexion = connexion(editeur(ecran));
            connexion.setSelection(Integer.MAX_VALUE, Integer.MAX_VALUE);
            connexion.commitText(texte, 1);
        });
    }

    /**
     * Pince l'éditeur : deux pointeurs RÉELS — les mêmes
     * {@code MotionEvent} que le banc JVM, doigts VERTICAUX autour du
     * centre — distribués à la vue à la cadence du doigt : un
     * événement tous les seize millisecondes, horodaté au moment
     * réel. Le détecteur d'échelle de l'appareil ré-ancre les sauts
     * d'envergure (une rafale d'événements en une milliseconde
     * ressemble à un saut de digitaliseur : l'engagement se
     * reprend, le facteur retombe à l'unité).
     *
     * @return le compte d'événements distribués à la vue
     */
    static int pincer(ActivityScenario<EditorActivity> scenario, float ecartDepart,
            float ecartArrivee) throws InterruptedException {
        Rect bornes = bornesEditeur(scenario);
        int[] distribues = new int[1];
        distribuerEtape(scenario, bornes, distribues,
                MotionEvent.ACTION_DOWN, 1, ecartDepart);
        Thread.sleep(16L);
        distribuerEtape(scenario, bornes, distribues,
                MotionEvent.ACTION_POINTER_DOWN
                | 1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT, 2, ecartDepart);
        for (int etape = 1; etape <= 24; etape++) {
            Thread.sleep(16L);
            float ecart = ecartDepart + (ecartArrivee - ecartDepart) * etape / 24f;
            distribuerEtape(scenario, bornes, distribues,
                    MotionEvent.ACTION_MOVE, 2, ecart);
        }
        Thread.sleep(16L);
        distribuerEtape(scenario, bornes, distribues,
                MotionEvent.ACTION_POINTER_UP
                | 1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT, 2, ecartArrivee);
        Thread.sleep(16L);
        distribuerEtape(scenario, bornes, distribues,
                MotionEvent.ACTION_UP, 1, ecartArrivee);
        appareil().waitForIdle();
        return distribues[0];
    }

    /** Distribue une étape du geste à la vue, à l'instant réel. */
    private static void distribuerEtape(
            ActivityScenario<EditorActivity> scenario, Rect bornes,
            int[] distribues, int action, int compte, float ecart) {
        scenario.onActivity(ecran -> {
            long maintenant = SystemClock.uptimeMillis();
            distribues[0] += distribuer(editeur(ecran), evenement(maintenant,
                    maintenant, action, compte, bornes, ecart));
        });
    }

    /**
     * L'événement de geste : doigts VERTICAUX symétriques autour du
     * centre des bornes — le doigt zéro au nord, le doigt un au sud
     * (la géométrie du banc JVM) ; le compte porte le premier doigt
     * seul au premier et dernier contact.
     */
    private static MotionEvent evenement(long ancre, long instant, int action,
            int compte, Rect bornes, float ecart) {
        int demi = Math.max(8, Math.round(ecart / 2f));
        MotionEvent.PointerProperties[] proprietes =
                new MotionEvent.PointerProperties[compte];
        MotionEvent.PointerCoords[] coordonnees = new MotionEvent.PointerCoords[compte];
        for (int doigt = 0; doigt < compte; doigt++) {
            proprietes[doigt] = new MotionEvent.PointerProperties();
            proprietes[doigt].id = doigt;
            proprietes[doigt].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coordonnees[doigt] = new MotionEvent.PointerCoords();
            coordonnees[doigt].x = bornes.centerX();
            coordonnees[doigt].y = bornes.centerY() + (doigt == 0 ? -demi : demi);
            coordonnees[doigt].pressure = 1f;
            coordonnees[doigt].size = 1f;
        }
        return Objects.requireNonNull(MotionEvent.obtain(ancre, instant,
                action, compte, proprietes, coordonnees, 0, 0, 1f, 1f, 0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0));
    }

    /** Distribue l'événement à la vue, sur le fil principal. */
    private static int distribuer(EditorView vue, MotionEvent evenement) {
        boolean servi = vue.dispatchTouchEvent(evenement);
        evenement.recycle();
        return servi ? 1 : 0;
    }

    /**
     * L'état du système vu du banc — la veille de l'écran et la
     * fenêtre focalisée : les messages d'échec le portent quand la
     * vitre reste muette. La sortie brute de dumpsys se filtre en
     * Java : la voie de commandes n'interprète pas les tubes.
     */
    static String etatSysteme() {
        try {
            return " veille: " + ligne(appareil().executeShellCommand("dumpsys power"),
                    "mWakefulness")
                    + ", focus: " + ligne(appareil().executeShellCommand(
                    "dumpsys window"), "mCurrentFocus")
                    + ", couche: " + (appareil().executeShellCommand(
                    "dumpsys SurfaceFlinger").contains("jo.celeris.demo")
                    ? "présente" : "absente de SurfaceFlinger");
        } catch (IOException echec) {
            return " état illisible : " + echec.getMessage();
        }
    }

    /** La ligne qui porte la marque, tirée d'une sortie brute. */
    private static String ligne(String sortie, String marque) {
        int debut = sortie.indexOf(marque);
        if (debut < 0) {
            return marque + " introuvable";
        }
        int fin = sortie.indexOf('\n', debut);
        return sortie.substring(debut, fin < 0 ? sortie.length() : fin).trim();
    }

    /** L'appareil UiAutomator du banc. */
    private static UiDevice appareil() {
        return UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    /**
     * Capture LE RENDU RÉEL DE LA VUE : la vue se dessine entière
     * dans une bitmap — le vrai Skia de l'appareil, la vraie mise en
     * page, le vrai pinceau (le compositeur de l'image ATD demeure
     * muet : les défauts du jalon vivent dans le rendu de l'éditeur,
     * pas dans la composition de la vitre). Les coordonnées de la
     * capture sont celles de la vue.
     */
    static Bitmap capturerVue(ActivityScenario<EditorActivity> scenario) {
        Bitmap[] image = new Bitmap[1];
        scenario.onActivity(ecran -> {
            EditorView vue = editeur(ecran);
            Bitmap cible = Bitmap.createBitmap(vue.getWidth(), vue.getHeight(),
                    Bitmap.Config.ARGB_8888);
            vue.draw(new Canvas(cible));
            image[0] = cible;
        });
        return Objects.requireNonNull(image[0]);
    }

    /**
     * La bande des premières lignes : le haut de l'éditeur, en
     * coordonnées de vue, jamais touchée par les frappes (elles
     * vivent en fin de document).
     */
    static Rect bandePremieresLignes(ActivityScenario<EditorActivity> scenario) {
        Rect[] bande = new Rect[1];
        scenario.onActivity(ecran -> {
            EditorView vue = editeur(ecran);
            bande[0] = new Rect(0, 0, vue.getWidth(), Math.max(1, vue.getHeight() / 7));
        });
        return Objects.requireNonNull(bande[0]);
    }

    /**
     * L'aperçu ASCII d'une bande de capture — les bancs à captures le
     * portent à leurs messages d'échec : la CI n'a pas d'œil sur la
     * vitre, le journal en tient lieu. Soixante colonnes, vingt
     * rangées, la gamme de luminance en dix marches, les dimensions de
     * la bitmap et son compte de couleurs distinctes en tête.
     */
    static String apercu(Bitmap image, Rect bande) {
        StringBuilder journal = new StringBuilder();
        journal.append("bitmap ").append(image.getWidth()).append('x')
                .append(image.getHeight())
                .append(", couleurs distinctes ~").append(compteCouleurs(image))
                .append(", coin haut-gauche 0x").append(Integer.toHexString(
                        image.getPixel(3, 3)))
                .append(", centre 0x").append(Integer.toHexString(
                        image.getPixel(image.getWidth() / 2, image.getHeight() / 2)))
                .append(", bande ").append(bande).append('\n');
        for (int rang = 0; rang < 20; rang++) {
            int y = bande.top + (bande.height() * rang / 20);
            for (int colonne = 0; colonne < 60; colonne++) {
                int x = bande.left + (bande.width() * colonne / 60);
                journal.append(marche(luminance(image.getPixel(x, y))));
            }
            journal.append('\n');
        }
        return journal.toString();
    }

    /** La luminance d'un pixel, en cinquantièmes (0 à 50). */
    private static int luminance(int pixel) {
        int somme = ((pixel >> 16) & 0xFF) * 3 + ((pixel >> 8) & 0xFF) * 6
                + (pixel & 0xFF);
        return (somme + 25) / 51;
    }

    /** La marche ASCII d'une luminance — du plus clair au plus foncé. */
    private static char marche(int luminance) {
        final String marches = " .:-=+*#%@";
        return marches.charAt(Math.min(9, luminance / 5));
    }

    /** Le compte de couleurs distinctes, échantillonné au treizième. */
    private static int compteCouleurs(Bitmap image) {
        Set<Integer> vues = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y += 13) {
            for (int x = 0; x < image.getWidth(); x += 13) {
                vues.add(image.getPixel(x, y));
            }
        }
        return vues.size();
    }
}
