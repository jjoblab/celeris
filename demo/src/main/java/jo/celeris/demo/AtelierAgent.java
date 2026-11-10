package jo.celeris.demo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import jo.celeris.android.widgets.FeuilleHaut;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.core.ai.RevueAgent;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier de revue des modifications d'agent de la démonstration
 * (F-04) : l'agent factice propose, la revue découpe en blocs — la
 * feuille du haut montre l'intention déclarée et les lignes du bloc en
 * lecture, accepter applique les éditions du bloc, refuser les garde
 * pour l'origine, le point de contrôle d'ouverture revient d'un geste
 * (ADR-018, chantier C12 : la maquette porte la revue d'agent en
 * feuille du haut). L'application se prend en une transaction
 * annulable, comme tout geste de l'atelier.
 */
final class AtelierAgent {

    private final AppCompatActivity activite;
    private final TextSession session;
    private final Supplier<PopupPalette> palettes;
    private final Runnable apresApplication;
    private final FauxAgent agent = new FauxAgent();

    private @Nullable RevueAgent revue;
    private @Nullable RevueAgent pointDeControle;
    private @Nullable FeuilleHaut feuille;
    private @Nullable TextView intentionVue;
    private @Nullable TextView lignesVue;

    /**
     * Construit l'atelier sur la session ouverte.
     *
     * @param activite         l'activité hôte de la feuille
     * @param session          la session d'édition courante
     * @param palettes         la palette courante, dérivée à chaque ouverture
     * @param apresApplication invoqué après une fusion appliquée
     */
    AtelierAgent(AppCompatActivity activite, TextSession session,
            Supplier<PopupPalette> palettes, Runnable apresApplication) {
        this.activite = activite;
        this.session = session;
        this.palettes = palettes;
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
     * Le bloc courant : sa feuille quand il attend, la fusion appliquée
     * — et la feuille refermée — quand tout est décidé.
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
            fermer();
            return;
        }
        poserTextes(courante.etiquette().intention(),
                String.join("\n", courante.lignesDuBloc(courant)), courant);
    }

    /** Le bloc posé sur la feuille : titre, intention, lignes, ouverture. */
    private void poserTextes(String intention, String lignes, int courant) {
        FeuilleHaut feuilleCourante = poserFeuille();
        feuilleCourante.titre(activite.getString(
                R.string.titre_agent, courant + 1));
        TextView vueIntention = intentionVue;
        if (vueIntention != null) {
            vueIntention.setText(intention);
        }
        TextView vueLignes = lignesVue;
        if (vueLignes != null) {
            vueLignes.setText(lignes);
        }
        feuilleCourante.setPalette(palettes.get());
        feuilleCourante.montrer(hote());
    }

    /** La feuille de revue — née à son premier bloc, gardée ensuite. */
    private FeuilleHaut poserFeuille() {
        FeuilleHaut feuilleCourante = feuille;
        if (feuilleCourante != null) {
            return feuilleCourante;
        }
        FeuilleHaut neuve = new FeuilleHaut(activite, palettes.get());
        neuve.libelleFermeture(activite.getString(R.string.action_fermer));
        View contenu = activite.getLayoutInflater()
                .inflate(R.layout.feuille_agent, null);
        neuve.contenu(contenu);
        intentionVue = contenu.findViewById(R.id.intention_agent);
        lignesVue = contenu.findViewById(R.id.lignes_agent);
        neuve.findViewById(R.id.bouton_accepter_agent)
                .setOnClickListener(vue ->
                        decider(RevueAgent.Decision.ACCEPTE));
        neuve.findViewById(R.id.bouton_refuser_agent)
                .setOnClickListener(vue ->
                        decider(RevueAgent.Decision.REFUSE));
        neuve.findViewById(R.id.bouton_revenir_agent)
                .setOnClickListener(vue -> revenirAuPointDeControle());
        feuille = neuve;
        return neuve;
    }

    /** Referme la feuille de revue, ouverte ou non. */
    private void fermer() {
        FeuilleHaut feuilleCourante = feuille;
        if (feuilleCourante != null) {
            feuilleCourante.cacher();
        }
    }

    /** La touche retour rencontre la feuille — ouverte, elle se referme. */
    boolean surRetour() {
        FeuilleHaut feuilleCourante = feuille;
        return feuilleCourante != null && feuilleCourante.surRetour();
    }

    /** Le cadre de contenu de la fenêtre — toujours un FrameLayout. */
    private ViewGroup hote() {
        return (ViewGroup) activite.findViewById(android.R.id.content);
    }
}
