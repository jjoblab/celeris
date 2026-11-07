package jo.celeris.android.widgets;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * L'info-bulle de diagnostic (M7.5, §7, reprise — D-02) : le message
 * du diagnostic touché, son encre suit la sévérité — erreur à l'encre
 * d'erreur, avertissement à l'encre d'accent. Fermée à la frappe et
 * au défilement par le contrôleur ; l'ancre vit chez
 * {@link PopupAnchor#atPoint}.
 */
public final class DiagnosticTooltipView extends TextRowsPopup {

    /** La sévérité d'un diagnostic — son encre suit. */
    public enum Severity {
        /** Une erreur — l'encre d'erreur de la palette. */
        ERROR,

        /** Un avertissement — l'encre d'accent de la palette. */
        WARNING
    }

    public DiagnosticTooltipView(Context context, PopupPalette palette) {
        super(context, palette);
    }

    /**
     * Pose le message à montrer.
     *
     * @param message  le message du diagnostic, ou null pour vider
     * @param severity la sévérité, qui décide l'encre
     */
    public void setMessage(String message, Severity severity) {
        if (message == null) {
            setRows(List.of());
            return;
        }
        int encre = severity == Severity.ERROR
                ? palette.error() : palette.accent();
        List<Row> rangees = new ArrayList<>();
        for (String ligne : message.split("\n", -1)) {
            rangees.add(Row.of(ligne, encre));
        }
        setRows(rangees);
    }
}
