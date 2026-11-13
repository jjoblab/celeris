package jo.celeris.demo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import jo.celeris.android.widgets.DiffBlocs;
import jo.celeris.android.widgets.FeuilleHaut;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.RevueDiff;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier de revue du diff de la démonstration (D-12) : le diff du
 * texte courant face au contenu d'origine se relit bloc par bloc —
 * accepter applique les lignes du bloc, refuser les garde anciennes,
 * le réalignement suit. La revue vit en feuille du haut (ADR-018,
 * chantier C12) : chaque bloc se décide sur la même feuille, la
 * décision finale s'applique en une transaction de remplacement
 * intégral, annulable d'un geste.
 */
final class AtelierDiff {

    private final AppCompatActivity activite;
    private final String reference;
    private final TextSession session;
    private final Runnable apresApplication;
    private final Supplier<PopupPalette> palettes;

    private RevueDiff revue;
    private @Nullable FeuilleHaut feuille;
    private @Nullable TextView lignesVue;

    /**
     * Construit l'atelier sur la session ouverte.
     *
     * @param activite         l'activité hôte de la feuille
     * @param reference        le contenu d'origine du fichier
     * @param session          la session d'édition courante
     * @param palettes         la palette courante, dérivée à chaque ouverture
     * @param apresApplication invoqué après une fusion appliquée
     */
    AtelierDiff(AppCompatActivity activite, String reference,
            TextSession session, Supplier<PopupPalette> palettes,
            Runnable apresApplication) {
        this.activite = activite;
        this.reference = reference;
        this.session = session;
        this.palettes = palettes;
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

    /** Le bloc courant : sa feuille, ou la fusion appliquée qui referme. */
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
            fermer();
            return;
        }
        FeuilleHaut feuilleCourante = feuille;
        if (feuilleCourante == null) {
            feuilleCourante = new FeuilleHaut(activite, palettes.get());
            feuilleCourante.libelleFermeture(
                    activite.getString(R.string.action_fermer));
            View contenu = activite.getLayoutInflater()
                    .inflate(R.layout.feuille_diff, null);
            feuilleCourante.contenu(contenu);
            lignesVue = contenu.findViewById(R.id.lignes_diff);
            feuilleCourante.findViewById(R.id.bouton_accepter_diff)
                    .setOnClickListener(vue ->
                            decider(RevueDiff.Decision.ACCEPTE));
            feuilleCourante.findViewById(R.id.bouton_refuser_diff)
                    .setOnClickListener(vue ->
                            decider(RevueDiff.Decision.REFUSE));
            feuille = feuilleCourante;
        }
        feuilleCourante.titre(activite.getString(
                R.string.titre_feuille_diff, courant + 1));
        TextView vueLignes = lignesVue;
        if (vueLignes != null) {
            vueLignes.setText(
                    String.join("\n", lignesDuBloc(courant)));
        }
        feuilleCourante.setPalette(palettes.get());
        feuilleCourante.montrer(hote());
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

    private List<String> lignesDuBloc(int bloc) {
        DiffBlocs diffs = DiffBlocs.de(reference, session.document().text());
        List<String> rendues = new ArrayList<>();
        for (DiffBlocs.LineDiff ligne : diffs.blocs().get(bloc).lignes()) {
            rendues.add((ligne.type() == DiffBlocs.TypeLigne.ENLEVEE ? "- " : "+ ")
                    + ligne.texte());
        }
        return rendues;
    }

    /** Le cadre de contenu de la fenêtre — toujours un FrameLayout. */
    private ViewGroup hote() {
        return (ViewGroup) activite.findViewById(android.R.id.content);
    }
}
