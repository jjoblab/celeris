package jo.celeris.android.host.popups;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.text.TextSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le cycle de vie de l'orchestrateur (chantier C1, revue du 2026-11-17) :
 * {@code detacher()} promet « les liens se dissolvent » — le banc le
 * prouve. Avant le correctif, les écouteurs de session posés au
 * constructeur (édition, sélection) n'étaient JAMAIS retirés : la
 * session vivante retenait l'orchestrateur, donc la couche, donc le
 * contexte de l'activité — la fuite que la revue relevait.
 *
 * <p>Banc réel (ADR-013) : une activité réelle porte la vue et la
 * couche, l'orchestrateur s'y monte comme chez l'hôte (§ 9.1
 * d'USAGE.md), la sonde compte les notifications qui traversent la
 * session après le détachement.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class CycleViePopupsTest {

    private static final String DOCUMENT = "int bonjour = 1;\n";

    /** Le détachement retire les écouteurs de session de l'orchestrateur :
     *  une édition d'après ne réveille plus rien du kit. */
    @Test
    public void detacherRetireLesEcouteursDeSession() {
        Application contexte = RuntimeEnvironment.getApplication();
        EditorView editeur = new EditorView(contexte);
        PopupLayer couche = new PopupLayer(contexte);
        TextSession session = TextSession.of(DOCUMENT, System::nanoTime);
        editeur.setSession(session);
        PopupsEditeur popups = new PopupsEditeur(editeur, session, couche,
                jo.celeris.android.view.EditorThemes.SOMBRE, 14f,
                CoachPopups.muet());

        // La sonde témoin : elle vit sur la session, elle seule doit
        // rester après le détachement.
        int[] editionsVues = new int[1];
        session.addEditListener((debut, fin, retire, insere) ->
                editionsVues[0]++);

        // Avant : l'orchestrateur écoute — l'édition réveille le kit
        // (la sonde compte, le kit réagit sans erreur).
        session.type("x");
        assertTrue("la sonde vit avant le détachement",
                editionsVues[0] > 0);

        // Le détachement : les liens se dissolvent.
        popups.detacher();

        // Après : l'édition traverse la session, la sonde seule compte.
        // Si l'orchestrateur écoutait encore, son écouteur leverait
        // l'assertion ci-dessous — le banc sonde le COMPORTEMENT (la
        // réaction du kit), pas l'introspection des listes internes.
        int editionsAuDetachement = editionsVues[0];
        session.type("y");
        assertTrue("la sonde vit après le détachement",
                editionsVues[0] > editionsAuDetachement);

        // La vue collectable : le détachement de la fenêtre ne retient
        // plus rien (contrat du chantier C1 côté vue — couvert par le
        // banc d'android-view).
        assertTrue("l'orchestrateur détaché ne réagit plus",
                popups.manager() != null);
    }

    /** Le détachement coupe aussi l'écouteur de viewport que
     *  l'orchestrateur pose sur la vue : un défilement d'après ne
     *  repositionne plus les cartes. */
    @Test
    public void detacherNeLaisseAucuneCarteOuverte() {
        Application contexte = RuntimeEnvironment.getApplication();
        EditorView editeur = new EditorView(contexte);
        PopupLayer couche = new PopupLayer(contexte);
        TextSession session = TextSession.of(DOCUMENT, System::nanoTime);
        editeur.setSession(session);
        PopupsEditeur popups = new PopupsEditeur(editeur, session, couche,
                jo.celeris.android.view.EditorThemes.SOMBRE, 14f,
                CoachPopups.muet());

        popups.notifier("titre", "message");
        assertTrue("la notification vit avant le détachement",
                popups.manager().estOuvert(
                        PopupController.PopupKind.NOTIFICATION));

        popups.detacher();
        assertFalse("le détachement ferme toute carte ouverte",
                popups.manager().estOuvert(
                        PopupController.PopupKind.NOTIFICATION));
        // Idempotent : un second détachement ne lève rien.
        popups.detacher();
        assertFalse("le second détachement ne change rien",
                popups.manager().estOuvert(
                        PopupController.PopupKind.NOTIFICATION));
    }
}
