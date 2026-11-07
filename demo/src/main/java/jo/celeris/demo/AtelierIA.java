package jo.celeris.demo;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import jo.celeris.android.view.EditorView;
import jo.celeris.core.ai.AppliquerPrediction;
import jo.celeris.core.ai.FauxModeleLocal;
import jo.celeris.core.ai.PredictionEdition;
import jo.celeris.core.ai.RequeteModele;
import jo.celeris.core.ai.SessionTexteFantome;
import jo.celeris.core.ai.SuggestionFantome;
import jo.celeris.core.syntax.JetonAnnulation;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier IA de la démonstration (F-01 interface, F-02, F-03) : les
 * actions IA vivent au même titre que les intentions classiques — le
 * menu de l'atelier, rien d'ailleurs. Le modèle est le faux local,
 * déterministe et sans réseau : la complétion se sert synchrone, la
 * prédiction demande son application explicite.
 */
final class AtelierIA {

    private final AppCompatActivity activite;
    private final EditorView editeur;
    private final TextSession session;
    private final FauxModeleLocal modele = new FauxModeleLocal();
    private final PiloteFantome pilote;

    /**
     * Construit l'atelier : le pilote du fantôme s'attache à la vue —
     * Tab et l'appui sur le fantôme acceptent le premier mot.
     *
     * @param activite l'activité hôte
     * @param editeur  la vue d'éditeur ouverte
     * @param session  la session d'édition courante
     */
    AtelierIA(AppCompatActivity activite, EditorView editeur,
            TextSession session) {
        this.activite = activite;
        this.editeur = editeur;
        this.session = session;
        this.pilote = new PiloteFantome(session,
                new SessionTexteFantome(modele, Runnable::run));
        editeur.setSurGesteFantome(geste -> {
            pilote.surGeste(geste);
            rafraichir();
        });
    }

    /**
     * F-01 — demande une complétion au modèle local : la suggestion se
     * dessine fantôme à l'ancre, l'acceptation se prend par mot (Tab,
     * appui), par ligne ou en bloc (menu).
     */
    void demanderFantome() {
        Document document = session.document();
        int curseur = curseur();
        modele.fantome(new SuggestionFantome(
                fragmentSuggere(document, curseur), curseur,
                document.revision()));
        pilote.demander(document, curseur);
        rafraichir();
    }

    /**
     * F-02 — demande une prédiction d'édition : le modèle prédit une
     * opération, l'application reste explicite — la boîte de dialogue
     * montre la description, l'hôte applique en transaction annulable,
     * le refus est un résultat qui se lit.
     */
    void demanderPrediction() {
        Document document = session.document();
        int fin = document.length();
        modele.prediction(new PredictionEdition(
                activite.getString(R.string.prediction_description),
                List.of(new TextEdit(fin, "", "\n// ligne prédite par le modèle local")),
                document.revision()));
        modele.predire(new RequeteModele(document, curseur(), new JetonAnnulation()),
                new jo.celeris.core.ai.ModelePrediction.Recepteur() {
                    @Override
                    public void prediction(PredictionEdition predite) {
                        montrerPrediction(predite);
                    }

                    @Override
                    public void aucune() {
                        activite.runOnUiThread(() -> new MaterialAlertDialogBuilder(
                                activite)
                                .setMessage(R.string.prediction_aucune)
                                .setPositiveButton(android.R.string.ok, null)
                                .show());
                    }
                });
    }

    /** F-01 — le fantôme visible suit le pilote, ré-ancré. */
    void rafraichir() {
        editeur.setFantome(pilote.suggestionVue());
    }

    /** La frappe suivante refuse le fantôme et annule le vol (F-09). */
    void surEdition() {
        pilote.surEdition();
        rafraichir();
    }

    private void montrerPrediction(PredictionEdition predite) {
        activite.runOnUiThread(() -> new MaterialAlertDialogBuilder(activite)
                .setTitle(R.string.prediction_titre)
                .setMessage(predite.description() + "\n\n"
                        + activite.getString(R.string.prediction_question))
                .setPositiveButton(R.string.prediction_appliquer,
                        (dialog, lequel) -> appliquerPrediction(predite))
                .setNegativeButton(android.R.string.cancel, null)
                .show());
    }

    private void appliquerPrediction(PredictionEdition predite) {
        AppliquerPrediction.Resultat resultat =
                AppliquerPrediction.appliquer(session, predite);
        int message = resultat == AppliquerPrediction.Resultat.APPLIQUEE
                ? R.string.prediction_appliquee
                : R.string.prediction_refusee;
        new MaterialAlertDialogBuilder(activite)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        editeur.invalidate();
    }

    /** Le fragment suggéré par le faux modèle, déterministe au contenu. */
    private static String fragmentSuggere(Document document, int curseur) {
        String texte = document.text();
        char avant = curseur > 0 ? texte.charAt(curseur - 1) : ' ';
        if (avant == '.') {
            return "taille()";
        }
        if (avant == '(') {
            return "\")";
        }
        return "suite()";
    }

    private int curseur() {
        var plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).caret();
    }
}
