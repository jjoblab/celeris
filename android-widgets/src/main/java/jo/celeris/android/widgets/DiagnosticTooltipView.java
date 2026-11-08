package jo.celeris.android.widgets;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * L'info-bulle de diagnostic (portage du design du dépôt de départ —
 * D-02) : le marqueur de sévérité — « × » à l'encre d'erreur, « ! » à
 * l'encre d'avertissement, au palier titre — précède le MESSAGE à
 * l'encre du texte : la sévérité se lit au marqueur, le message reste
 * lisible, comme la fiche du dépôt de départ. Fermée à la frappe et au
 * défilement par le contrôleur ; l'ancre vit chez
 * {@link PopupAnchor#atPoint}.
 */
public final class DiagnosticTooltipView extends TextRowsPopup {

    /** La sévérité d'un diagnostic — son marqueur et son encre suivent. */
    public enum Severity {
        /** Une erreur — le marqueur « × » à l'encre d'erreur. */
        ERROR,
        /** Un avertissement — le marqueur « ! » à l'encre d'avertissement. */
        WARNING
    }

    public DiagnosticTooltipView(Context context, PopupPalette palette) {
        super(context, palette);
    }

    /**
     * Pose le message à montrer.
     *
     * @param message  le message du diagnostic, ou null pour vider
     * @param severity la sévérité, qui décide le marqueur et son encre
     */
    public void setMessage(String message, Severity severity) {
        if (message == null) {
            setRows(List.of());
            return;
        }
        int encre = severity == Severity.ERROR
                ? palette.error() : palette.warning();
        String marqueur = severity == Severity.ERROR ? "×" : "!";
        List<Row> rangees = new ArrayList<>();
        rangees.add(Row.tier(marqueur, encre, Tier.TITRE));
        for (String ligne : message.split("\n", -1)) {
            rangees.add(Row.of(ligne));
        }
        setRows(rangees);
    }
}
