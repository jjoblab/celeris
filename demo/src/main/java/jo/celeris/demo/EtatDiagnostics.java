package jo.celeris.demo;

import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import jo.celeris.android.host.popups.PopupsEditeur;
import jo.celeris.android.host.spi.CheminDoc;
import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.host.spi.GraviteDiagnostic;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.DiagnosticTooltipView;
import jo.celeris.core.lsp.types.DiagnosticLsp;


/**
 * L'état des diagnostics de la démonstration (M8.6) : les deux voies —
 * l'analyse lint de l'hôte, les diagnostics POUSSÉS par le serveur
 * de langage — s'y convertissent en marques de gouttière, bande d'erreurs et
 * étiquette de barre d'état. Les publications du serveur arrivent sur
 * le fil du client : elles repassent par le fil d'interface avant de
 * toucher la scène.
 */
final class EtatDiagnostics {

    private final AppCompatActivity activite;
    private final EditorView editeur;
    private final PopupsEditeur popups;
    private final TextView etiquette;
    private final CheminDoc chemin;

    EtatDiagnostics(AppCompatActivity activite, EditorView editeur,
            PopupsEditeur popups, TextView etiquette,
            CheminDoc chemin) {
        this.activite = activite;
        this.editeur = editeur;
        this.popups = popups;
        this.etiquette = etiquette;
        this.chemin = chemin;
    }

    /**
     * L'état lint de l'hôte : les lignes marquées par l'atelier, la
     * bande et l'étiquette suivent.
     *
     * @param diagnostics     les diagnostics de l'hôte
     * @param lignesMarquees  les lignes une à une, pour la gouttière
     */
    void surLint(List<DiagnosticHote> diagnostics, int[] lignesMarquees) {
        editeur.marquesGouttiere().setErreursImportees(lignesMarquees);
        popups.setDiagnostics(diagnostics);
        publier(diagnostics);
    }

    /**
     * Les diagnostics publiés par le serveur de langage : la réponse
     * arrive sur le fil du client — elle repasse par le fil
     * d'interface avant de toucher la scène.
     *
     * @param uri         l'uri du document diagnostiqué, ignorée — un
     *                    seul document vit dans l'atelier
     * @param diagnostics les diagnostics du protocole, dernier état
     */
    void surServeur(String uri, List<DiagnosticLsp> diagnostics) {
        activite.runOnUiThread(() -> appliquer(diagnostics));
    }

    /** La sévérité de l'info-bulle d'une gravité de l'hôte — l'erreur à
     *  l'encre d'erreur, le reste à l'encre d'avertissement (la
     *  conversion du domaine : l'hôte parle gravité, la carte parle
     *  sévérité).
     *
     * @param gravite la gravité du diagnostic
     * @return la sévérité de la carte
     */
    static DiagnosticTooltipView.Severity severite(
            GraviteDiagnostic gravite) {
        return gravite == GraviteDiagnostic.ERREUR
                ? DiagnosticTooltipView.Severity.ERROR
                : DiagnosticTooltipView.Severity.WARNING;
    }

    /** L'application des diagnostics du serveur : conversion et scène. */
    private void appliquer(List<DiagnosticLsp> diagnostics) {
        List<DiagnosticHote> convertis = new ArrayList<>(diagnostics.size());
        int[] lignes = new int[diagnostics.size()];
        for (int i = 0; i < diagnostics.size(); i++) {
            DiagnosticLsp diagnostic = diagnostics.get(i);
            lignes[i] = diagnostic.plage().debut().ligne() + 1;
            convertis.add(new DiagnosticHote(chemin, lignes[i],
                    diagnostic.plage().debut().colonne() + 1,
                    diagnostic.gravite() == DiagnosticLsp.ERREUR
                            ? GraviteDiagnostic.ERREUR
                            : GraviteDiagnostic.AVERTISSEMENT,
                    diagnostic.message()));
        }
        editeur.marquesGouttiere().setErreursImportees(lignes);
        popups.setDiagnostics(convertis);
        publier(convertis);
    }

    /** L'étiquette publie le compte — propre sinon. */
    private void publier(List<DiagnosticHote> diagnostics) {
        if (!diagnostics.isEmpty()) {
            etiquette.setText(activite.getResources().getQuantityString(
                    R.plurals.etat_lint, diagnostics.size(),
                    diagnostics.size()));
        } else {
            etiquette.setText(R.string.lint_propre);
        }
        etiquette.setVisibility(View.VISIBLE);
        editeur.invalidate();
    }
}
