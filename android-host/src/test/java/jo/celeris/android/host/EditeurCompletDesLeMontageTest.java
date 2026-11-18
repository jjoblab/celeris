package jo.celeris.android.host;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.language.LanguageKit;
import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

/**
 * L'éditeur complet dès le montage (revue du propriétaire, 2026-11-19 :
 * « les popups de base ne sont pas affichés, l'éditeur doit les fournir
 * de base, l'application hôte n'a pas à les activer ») : la façade pose
 * le coach de la LANGUE — les mots réservés du langage servent la
 * complétion dès le premier caractère, SANS table de l'hôte — et le
 * CHROME vit dès la pose (minimap, pouce, bande, fil d'Ariane ne
 * patientent plus la première frappe).
 *
 * <p>Banc réel (ADR-013) : la scène se monte comme le sample (vue sous
 * la couche, façade attachée), le comportement se sonde par les voies
 * publiques : le coach servi, les cartes ouvertes.</p>
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class EditeurCompletDesLeMontageTest {

    /** La langue de banc : java posé directement, registre immédiat. */
    private static final LanguageKit KIT = LanguageKit
            .builder(Runnable::run)
            .addSupport(LanguageSupport.builder(
                            LanguageDescriptor.of("java", "Java",
                                    java.util.List.of("java")))
                    .syntax(JavaSyntax.INSTANCE)
                    .build())
            .build();

    private static final String SOURCE = """
            class Premiere {
                void methode() {
                }
            }
            """;

    /** La scène du banc : la vue sous la couche, mesurée, la façade
     *  attachée et le document ouvert — comme le sample. */
    private static CelerisEditor monter(FrameLayout scene, PopupLayer couche) {
        Application contexte = RuntimeEnvironment.getApplication();
        EditorView vue = new EditorView(contexte);
        vue.setId(android.R.id.text1);
        couche.setId(android.R.id.text2);
        scene.addView(vue, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        scene.addView(couche, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        // La scène vit à une fenêtre — comme l'écran réel : la posted
        // pose de langue (open() ne bloque plus le fil) s'y exécute.
        android.view.WindowManager fenetre = (android.view.WindowManager)
                contexte.getSystemService(android.content.Context.WINDOW_SERVICE);
        fenetre.addView(scene, new android.view.WindowManager.LayoutParams(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.MATCH_PARENT));
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", SOURCE);
        attendreLaPoseDeLangue();
        return editeur;
    }

    /** Laisse la pose de langue arriver : le fil démon résout, le
     *  posted se joue au fil de l'interface (Robolectric le vide). */
    private static void attendreLaPoseDeLangue() {
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        org.robolectric.shadows.ShadowLooper.runUiThreadTasks();
    }

    /** Le coach de la langue sert les mots réservés du Java sans
     *  qu'aucune table ne soit posée : la complétion de base vit
     *  dès le montage — class, void, public y passent. */
    @Test
    public void leCoachDeLaLangueSertLesMotsReserves() {
        Application contexte = RuntimeEnvironment.getApplication();
        FrameLayout scene = new FrameLayout(contexte);
        PopupLayer couche = new PopupLayer(contexte);
        CelerisEditor editeur = monter(scene, couche);

        assertNotNull("l'orchestrateur vit", editeur.popups());
        // La voie publique de la complétion : les mots réservés du
        // langage ouvert. Le banc échouait AVANT (le coach muet rendait
        // une table vide).
        assertTrue("la complétion connaît class",
                editeur.popups().completionMotsReserves().contains("class"));
        assertTrue("la complétion connaît void",
                editeur.popups().completionMotsReserves().contains("void"));
        assertTrue("la table est riche (Java : dizaines de mots)",
                editeur.popups().completionMotsReserves().size() >= 20);
        editeur.close();
    }

    /** Le chrome vit dès la pose : la minimap et le pouce de
     *  défilement rapide ne patientent pas la première frappe —
     *  la scène mesurée, le rafraîchissement du montage les a
     *  posés. */
    @Test
    public void leChromeVitDesLaPose() {
        Application contexte = RuntimeEnvironment.getApplication();
        FrameLayout scene = new FrameLayout(contexte);
        PopupLayer couche = new PopupLayer(contexte);
        CelerisEditor editeur = monter(scene, couche);

        // La scène se mesure et se drape (monter() l'a déjà posée à
        // une fenêtre) : le posted rafraîchissement du montage
        // s'exécute — la file du fil d'interface se vide.
        scene.measure(android.view.View.MeasureSpec.makeMeasureSpec(
                1080, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(
                        1920, android.view.View.MeasureSpec.EXACTLY));
        scene.layout(0, 0, 1080, 1920);
        org.robolectric.shadows.ShadowLooper.runUiThreadTasks();

        PopupController controleur = editeur.popups().manager().controleur();
        assertTrue("la minimap vit dès la pose",
                controleur.isOpen(PopupController.PopupKind.MINIMAP));
        assertTrue("le pouce de défilement rapide vit dès la pose",
                controleur.isOpen(PopupController.PopupKind.FAST_SCROLLER));
        editeur.close();
    }

    /** Le coach suit la langue du document ouvert : le .java sert les
     *  mots du Java ; sans langage, la table se vide proprement
     *  (l'édition reste entière, les mots du document seuls). */
    @Test
    public void leCoachSuitLaLangueDuDocument() {
        Application contexte = RuntimeEnvironment.getApplication();
        FrameLayout scene = new FrameLayout(contexte);
        PopupLayer couche = new PopupLayer(contexte);
        CelerisEditor editeur = monter(scene, couche);
        assertTrue("le .java sert les mots du Java",
                editeur.popups().completionMotsReserves().contains("class"));

        // Le document sans langage : la table se vide, sans erreur.
        editeur.open("notes.txt", "du texte libre\n");
        attendreLaPoseDeLangue();
        assertTrue("sans langage, la table se vide",
                editeur.popups().completionMotsReserves().isEmpty());
        editeur.close();
    }
}
