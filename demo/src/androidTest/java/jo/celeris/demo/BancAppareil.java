package jo.celeris.demo;

import android.app.Activity;
import android.app.UiAutomation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;

/**
 * Le socle commun des bancs d'appareil du second étage (C11,
 * M7.5) : ouvre l'écran d'édition de la démonstration, porte la
 * frappe par la connexion d'entrée (la voie exacte de l'IME, sans
 * clavier visible — la vitre reste stable pour les captures), pose
 * la retenue d'analyse (le crochet de débogage), pince par
 * l'injection de MotionEvents à deux pointeurs et capture la vitre
 * en PNG.
 *
 * <p>L'arbre d'accessibilité de l'image ATD ne porte pas la vitre —
 * les recherches d'UiAutomator reviennent vides : les gestes vivent
 * donc par la voie d'entrée brute (injectInputEvent, celle
 * qu'UiAutomator emprunte lui-même), la géométrie venant des bornes
 * mesurées de la vue.</p>
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
     * mesures. L'image ATD s'éveille muette — la vitre se réveille et
     * se maintient allumée AVANT les mesures : sans cela, les captures
     * reviennent unies et les gestes injectés se perdent (l'écran
     * dort, la politique d'entrée jette les touchers).
     */
    static EditorView attendreVue(ActivityScenario<EditorActivity> scenario)
            throws Exception {
        appareil().wakeUp();
        appareil().executeShellCommand("svc power stayon true");
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
     * Les bornes de l'éditeur à l'écran — la géométrie des gestes
     * injectés s'y cale.
     */
    static Rect bornesEditeur(ActivityScenario<EditorActivity> scenario) {
        Rect[] bornes = new Rect[1];
        scenario.onActivity(ecran -> {
            EditorView vue = editeur(ecran);
            int[] origine = new int[2];
            vue.getLocationOnScreen(origine);
            bornes[0] = new Rect(origine[0], origine[1],
                    origine[0] + vue.getWidth(), origine[1] + vue.getHeight());
        });
        return Objects.requireNonNull(bornes[0]);
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
     * Pince l'éditeur à l'écran : deux pointeurs RÉELS, posés,
     * écartés (ou resserrés) puis levés — les MotionEvents à deux
     * doigts passent par la voie d'entrée du système
     * (injectInputEvent), celle qu'UiAutomator emprunte lui-même ;
     * l'écartement suit la géométrie EXACTE du banc JVM (120 à 520
     * pour l'ample, 400 à 160 pour le resserré), les doigts
     * symétriques autour du centre — le point focal ne bouge pas.
     */
    static void pincer(ActivityScenario<EditorActivity> scenario, float ecartDepart,
            float ecartArrivee) throws InterruptedException {
        Rect bornes = bornesEditeur(scenario);
        UiAutomation automatisation = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation();
        long depart = SystemClock.uptimeMillis();
        injecter(automatisation, evenement(depart, depart,
                MotionEvent.ACTION_DOWN, bornes, ecartDepart));
        injecter(automatisation, evenement(depart, depart + 20L,
                MotionEvent.ACTION_POINTER_DOWN | 1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT,
                bornes, ecartDepart));
        for (int pas = 1; pas <= 20; pas++) {
            int ecart = Math.round(ecartDepart
                    + (ecartArrivee - ecartDepart) * pas / 20f);
            injecter(automatisation, evenement(depart, depart + 20L + pas * 40L,
                    MotionEvent.ACTION_MOVE, bornes, ecart));
            Thread.sleep(20L);
        }
        injecter(automatisation, evenement(depart, depart + 840L,
                MotionEvent.ACTION_POINTER_UP | 1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT,
                bornes, ecartArrivee));
        injecter(automatisation, evenement(depart, depart + 860L,
                MotionEvent.ACTION_UP, bornes, ecartArrivee));
        appareil().waitForIdle();
    }

    /**
     * L'événement à deux pointeurs, symétriques autour du centre des
     * bornes — le doigt zéro à l'ouest, le doigt un à l'est.
     */
    private static MotionEvent evenement(long instantPose, long instant,
            int action, Rect bornes, float ecart) {
        int demi = Math.max(8, Math.round(ecart / 2f));
        MotionEvent.PointerProperties[] proprietes =
                new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coordonnees = new MotionEvent.PointerCoords[2];
        for (int doigt = 0; doigt < 2; doigt++) {
            proprietes[doigt] = new MotionEvent.PointerProperties();
            proprietes[doigt].id = doigt;
            proprietes[doigt].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coordonnees[doigt] = new MotionEvent.PointerCoords();
            coordonnees[doigt].x = bornes.centerX() + (doigt == 0 ? -demi : demi);
            coordonnees[doigt].y = bornes.centerY();
            coordonnees[doigt].pressure = 1f;
            coordonnees[doigt].size = 1f;
        }
        return Objects.requireNonNull(MotionEvent.obtain(instantPose, instant,
                action, 2, proprietes, coordonnees, 0, 0, 1f, 1f, 0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0));
    }

    /** Porte l'événement à la voie d'entrée du système. */
    private static void injecter(UiAutomation automatisation, MotionEvent evenement) {
        automatisation.injectInputEvent(evenement, false);
        evenement.recycle();
    }

    /** L'appareil UiAutomator du banc. */
    private static UiDevice appareil() {
        return UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    /**
     * Capture la vitre entière en PNG, relue en bitmap — le banc le
     * dit si la capture ou la relecture manque.
     */
    static Bitmap capturer() throws IOException {
        File cible = File.createTempFile("celeris-capture", ".png",
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext().getCacheDir());
        if (!appareil().takeScreenshot(cible)) {
            throw new AssertionError("la capture de la vitre a échoué");
        }
        Bitmap image = BitmapFactory.decodeFile(cible.getAbsolutePath());
        if (image == null) {
            throw new AssertionError("la capture ne se relit pas en bitmap");
        }
        return image;
    }

    /**
     * La bande des premières lignes : le haut de l'éditeur à l'écran,
     * jamais touchée par les frappes (elles vivent en fin de document).
     */
    static Rect bandePremieresLignes(ActivityScenario<EditorActivity> scenario) {
        Rect[] bande = new Rect[1];
        scenario.onActivity(ecran -> {
            EditorView vue = editeur(ecran);
            int[] origine = new int[2];
            vue.getLocationOnScreen(origine);
            int hauteur = Math.max(1, Math.round(vue.getHeight() * 0.15f));
            bande[0] = new Rect(origine[0], origine[1],
                    origine[0] + vue.getWidth(), origine[1] + hauteur);
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
