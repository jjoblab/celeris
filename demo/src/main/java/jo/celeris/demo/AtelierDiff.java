package jo.celeris.demo;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import jo.celeris.android.widgets.DiffBlocs;
import jo.celeris.android.widgets.RevueDiff;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier de revue du diff de la démonstration (D-12) : le diff du
 * texte courant face au contenu d'origine se relit bloc par bloc —
 * accepter applique les lignes du bloc, refuser les garde anciennes,
 * le réalignement suit. La décision finale s'applique en une
 * transaction de remplacement intégral, annulable d'un geste.
 */
final class AtelierDiff {

    private final AppCompatActivity activite;
    private final String reference;
    private final TextSession session;
    private final Runnable apresApplication;

    private RevueDiff revue;

    /**
     * Construit l'atelier sur la session ouverte.
     *
     * @param activite         l'activité hôte des boîtes de dialogue
     * @param reference        le contenu d'origine du fichier
     * @param session          la session d'édition courante
     * @param apresApplication invoqué après une fusion appliquée
     */
    AtelierDiff(AppCompatActivity activite, String reference,
            TextSession session, Runnable apresApplication) {
        this.activite = activite;
        this.reference = reference;
        this.session = session;
        this.apresApplication = apresApplication;
    }

    /**
     * Ouvre (ou rouvre) la revue du texte courant face à l'origine.
     */
    void ouvrir() {
        revue = RevueDiff.de(reference, session.document().text());
        montrerBloc();
    }

    /** Décide le bloc courant et avance au suivant. */
    private void decider(RevueDiff.Decision decision) {
        if (revue == null) {
            return;
        }
        for (int i = 0; i < revue.nombreDeBlocs(); i++) {
            if (revue.decision(i) == RevueDiff.Decision.EN_ATTENTE) {
                revue = revue.decider(i, decision);
                break;
            }
        }
        montrerBloc();
    }

    /**
     * Applique le texte fusionné des décisions courantes : une
     * transaction de remplacement intégral — annulable.
     *
     * @return vrai si le texte a changé
     */
    boolean appliquer() {
        if (revue == null) {
            return false;
        }
        String fusion = revue.texte();
        if (fusion.equals(session.document().text())) {
            return false;
        }
        return session.replaceRange(0, session.document().text().length(),
                fusion);
    }

    /** Le bloc courant : sa boîte de dialogue, ou la fusion appliquée. */
    private void montrerBloc() {
        if (revue == null) {
            return;
        }
        int courant = -1;
        for (int i = 0; i < revue.nombreDeBlocs(); i++) {
            if (revue.decision(i) == RevueDiff.Decision.EN_ATTENTE) {
                courant = i;
                break;
            }
        }
        if (courant < 0) {
            if (appliquer()) {
                apresApplication.run();
            }
            return;
        }
        new MaterialAlertDialogBuilder(activite)
                .setTitle(activite.getString(R.string.titre_diff, courant + 1))
                .setMessage(String.join("\n", lignesDuBloc(courant)))
                .setPositiveButton(R.string.bloc_accepter,
                        (dialog, lequel) -> decider(RevueDiff.Decision.ACCEPTE))
                .setNegativeButton(R.string.bloc_refuser,
                        (dialog, lequel) -> decider(RevueDiff.Decision.REFUSE))
                .show();
    }

    private List<String> lignesDuBloc(int bloc) {
        DiffBlocs diffs = DiffBlocs.de(reference, session.document().text());
        List<String> rendues = new ArrayList<>();
        for (DiffBlocs.LigneDiff ligne : diffs.blocs().get(bloc).lignes()) {
            rendues.add((ligne.type() == DiffBlocs.TypeLigne.ENLEVEE ? "- " : "+ ")
                    + ligne.texte());
        }
        return rendues;
    }
}
