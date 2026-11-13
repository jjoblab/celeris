package jo.celeris.demo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import jo.celeris.android.widgets.PatchPreview;
import jo.celeris.android.widgets.FeuilleHaut;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.core.lsp.EditsEspaceLsp;
import jo.celeris.core.lsp.PositionsLsp;
import jo.celeris.core.lsp.types.CorrectifLsp;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

/**
 * L'atelier de l'aperçu d'un correctif (B-18) : le correctif proposé
 * sur la sélection se prévise AVANT l'acceptation — les lignes en
 * moins et en plus se lisent dans la feuille du kit, préfixées « - »
 * et « + », le contexte encadrant (ADR-018 : tout en popups) ;
 * l'acceptation applique l'édition par la voie de transaction,
 * annulable d'un geste (B-17), le refus referme sans rien toucher —
 * l'aperçu n'engage à rien.
 *
 * <p>L'édition se joue sur une copie pour se prévoir : jamais sur la
 * session ouverte — l'aperçu se construit en pur, l'application
 * appartient à la session. La sélection vide n'engage à rien ; le
 * serveur qui ne propose rien referme sans bruit.</p>
 */
final class AtelierCorrectif {

    private final AppCompatActivity activite;
    private final TextSession session;
    private final Supplier<PopupPalette> palettes;
    private final Runnable apresApplication;
    private final Supplier<@Nullable LanguageSession> serveur;
    private final Supplier<String> uri;

    private @Nullable FeuilleHaut feuille;
    private @Nullable TextView lignesVue;
    private @Nullable Transaction edition;

    /**
     * Construit l'atelier sur la session ouverte.
     *
     * @param activite         l'activité hôte de la feuille
     * @param session          la session d'édition courante
     * @param palettes         la palette courante, dérivée à chaque ouverture
     * @param apresApplication invoqué après un correctif accepté
     * @param serveur          la session de langage, lue à l'ouverture
     * @param uri              l'uri du document ouvert, lue à l'ouverture
     */
    AtelierCorrectif(AppCompatActivity activite, TextSession session,
            Supplier<PopupPalette> palettes, Runnable apresApplication,
            Supplier<@Nullable LanguageSession> serveur, Supplier<String> uri) {
        this.activite = activite;
        this.session = session;
        this.palettes = palettes;
        this.apresApplication = apresApplication;
        this.serveur = serveur;
        this.uri = uri;
    }

    /** Ouvre (ou rouvre) l'aperçu du correctif proposé sur la sélection. */
    void ouvrir() {
        LanguageSession courant = serveur.get();
        if (courant == null) {
            return;
        }
        MultiSelection selection = session.selection();
        if (selection.isEmpty()) {
            return;
        }
        SelRange principale = selection.ranges().get(0);
        if (principale.start() >= principale.end()) {
            return;
        }
        // Le moment juste : la plage proposée se sert par le fil du
        // protocole, l'aperçu s'en suit sur le fil de l'interface.
        courant.correctifs(uri.get(),
                PositionsLsp.plage(session.document(), principale.start(),
                        principale.end()))
                .whenComplete((correctifs, echec) ->
                        activite.runOnUiThread(() -> servir(correctifs)));
    }

    /** Le correctif servi : son édition se prévoit sur une copie, puis se montre. */
    private void servir(@Nullable List<CorrectifLsp> correctifs) {
        if (correctifs == null || correctifs.isEmpty()) {
            return;
        }
        CorrectifLsp correctif = correctifs.get(0);
        if (correctif.edition().documents().isEmpty()) {
            return;
        }
        // La copie porte l'épreuve : la session ouverte reste intacte
        // tant que l'acceptation n'a pas parlé.
        String avant = session.document().text();
        Transaction proposee = EditsEspaceLsp.transaction(session.document(),
                correctif.edition().documents().get(0));
        TextSession epreuve = TextSession.of(avant, () -> 0L);
        if (!epreuve.apply(proposee)) {
            return;
        }
        PatchPreview apercu = PatchPreview.de(avant, epreuve.text());
        if (!apercu.porteChangement()) {
            return;
        }
        edition = proposee;
        montrer(correctif.titre(), apercu);
    }

    /** L'acceptation applique l'édition proposée — un geste défait tout. */
    private void accepter() {
        Transaction proposee = edition;
        edition = null;
        if (proposee != null && session.apply(proposee)) {
            apresApplication.run();
        }
        fermer();
    }

    /** La feuille de l'aperçu : titre, lignes préfixées, les deux décisions. */
    private void montrer(String titre, PatchPreview apercu) {
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
                    .setOnClickListener(vue -> accepter());
            feuilleCourante.findViewById(R.id.bouton_refuser_diff)
                    .setOnClickListener(vue -> fermer());
            feuille = feuilleCourante;
        }
        feuilleCourante.titre(activite.getString(
                R.string.titre_feuille_correctif, titre));
        TextView vueLignes = lignesVue;
        if (vueLignes != null) {
            vueLignes.setText(String.join("\n", apercu.rendues()));
        }
        feuilleCourante.setPalette(palettes.get());
        feuilleCourante.montrer(hote());
    }

    /** Referme la feuille d'aperçu, ouverte ou non — l'engagement se perd. */
    private void fermer() {
        edition = null;
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
