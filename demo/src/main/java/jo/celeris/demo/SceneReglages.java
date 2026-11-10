package jo.celeris.demo;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import jo.celeris.android.view.EditorView;

/**
 * Les réglages de scène de l'atelier (M7.5, §9 — livrés) : la lenteur
 * d'analyse et la surimpression de diagnostic s'y appliquent depuis
 * les préférences persistées — l'activité délègue, la mécanique vit
 * ici. La boucle de la surimpression se repose à la pause de l'activité.
 */
final class SceneReglages {

    private final EditorView editeur;
    private final TextView ligne;
    private final DiagnosticOverlay surimpression;

    /**
     * Construit les réglages de scène — la surimpression se pose sur
     * son étiquette, cachée tant que la préférence ne la montre pas.
     *
     * @param editeur la vue d'éditeur ouverte
     * @param ligne   l'étiquette qui porte la ligne de vérité
     */
    SceneReglages(EditorView editeur, TextView ligne) {
        this.editeur = editeur;
        this.ligne = ligne;
        this.surimpression = new DiagnosticOverlay(editeur, ligne);
    }

    /**
     * Applique les préférences persistées : la retenue d'analyse à chaud
     * (M7.5, §9), la surimpression montrée ou cachée — sa boucle de
     * rafraîchissement ne vit que montrée.
     *
     * @param prefs les préférences de la démonstration
     */
    void appliquer(SharedPreferences prefs) {
        editeur.setRetenueAnalyseMs(prefs.getLong(
                FeuilleReglages.CLE_RETENU,
                EditorView.DELAI_RETENU_PAR_DEFAUT_MS));
        boolean montrer = prefs.getBoolean(
                FeuilleReglages.CLE_SURIMPRESSION, false);
        ligne.setVisibility(montrer ? View.VISIBLE : View.GONE);
        if (montrer) {
            surimpression.demarrer();
        } else {
            surimpression.arreter();
        }
    }

    /** La boucle de la surimpression se repose — l'activité se pause. */
    void reposer() {
        surimpression.arreter();
    }
}
