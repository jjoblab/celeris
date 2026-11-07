package jo.celeris.demo;

import org.jspecify.annotations.Nullable;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import jo.celeris.core.ai.RevueAgent;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier de revue des modifications d'agent de la démonstration
 * (F-04) : l'agent factice propose, la revue découpe en blocs — la
 * boîte de dialogue montre l'intention déclarée et les lignes du bloc
 * en lecture en ligne, accepter applique les éditions du bloc, refuser
 * les garde pour l'origine, le point de contrôle d'ouverture revient
 * d'un geste. L'application se prend en une transaction annulable,
 * comme tout geste de l'atelier.
 */
final class AtelierAgent {

    private final AppCompatActivity activite;
    private final TextSession session;
    private final Runnable apresApplication;
    private final FauxAgent agent = new FauxAgent();

    private @Nullable RevueAgent revue;
    private @Nullable RevueAgent pointDeControle;

    /**
     * Construit l'atelier sur la session ouverte.
     *
     * @param activite         l'activité hôte des boîtes de dialogue
     * @param session          la session d'édition courante
     * @param apresApplication invoqué après une fusion appliquée
     */
    AtelierAgent(AppCompatActivity activite, TextSession session,
            Runnable apresApplication) {
        this.activite = activite;
        this.session = session;
        this.apresApplication = apresApplication;
    }

    /**
     * Ouvre (ou rouvre) la revue de la proposition de l'agent sur le
     * document courant — le point de contrôle d'ouverture se prend
     * aussitôt.
     */
    void ouvrir() {
        revue = RevueAgent.de(session.document(),
                agent.proposer(session.document()));
        pointDeControle = revue.pointDeControle();
        montrerBloc();
    }

    /** Décide le bloc courant et avance au suivant. */
    private void decider(RevueAgent.Decision decision) {
        RevueAgent courante = revue;
        if (courante == null) {
            return;
        }
        for (int i = 0; i < courante.nombreDeBlocs(); i++) {
            if (courante.decision(i) == RevueAgent.Decision.EN_ATTENTE) {
                revue = courante.decider(i, decision);
                break;
            }
        }
        montrerBloc();
    }

    /** Revient au point de contrôle d'ouverture : tout recommence. */
    private void revenirAuPointDeControle() {
        RevueAgent controle = pointDeControle;
        if (controle == null || revue == null) {
            return;
        }
        revue = revue.retourAuPointDeControle(controle);
        montrerBloc();
    }

    /**
     * Le bloc courant : sa boîte de dialogue quand il attend, la fusion
     * appliquée quand tout est décidé.
     */
    private void montrerBloc() {
        RevueAgent courante = revue;
        if (courante == null) {
            return;
        }
        int courant = -1;
        for (int i = 0; i < courante.nombreDeBlocs(); i++) {
            if (courante.decision(i) == RevueAgent.Decision.EN_ATTENTE) {
                courant = i;
                break;
            }
        }
        if (courant < 0) {
            if (courante.appliquer(session) == RevueAgent.Sortie.APPLIQUEE) {
                apresApplication.run();
            }
            return;
        }
        List<String> lignes = courante.lignesDuBloc(courant);
        new MaterialAlertDialogBuilder(activite)
                .setTitle(activite.getString(R.string.titre_agent, courant + 1))
                .setMessage(courante.etiquette().intention()
                        + "\n\n" + String.join("\n", lignes))
                .setPositiveButton(R.string.bloc_accepter,
                        (dialog, lequel) -> decider(RevueAgent.Decision.ACCEPTE))
                .setNegativeButton(R.string.bloc_refuser,
                        (dialog, lequel) -> decider(RevueAgent.Decision.REFUSE))
                .setNeutralButton(R.string.agent_revenir,
                        (dialog, lequel) -> revenirAuPointDeControle())
                .show();
    }
}
