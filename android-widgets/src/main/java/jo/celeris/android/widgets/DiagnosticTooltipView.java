package jo.celeris.android.widgets;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * L'info-bulle de diagnostic (portage du design du dépôt de départ —
 * D-02, l'audit Android Studio du 2026-11-14 y empile) : le marqueur
 * de sévérité — « × » à l'encre d'erreur, « ! » à l'encre
 * d'avertissement, au palier titre — précède le MESSAGE à l'encre du
 * texte : la sévérité se lit au marqueur, le message reste lisible,
 * comme la fiche du dépôt de départ. L'IDE de référence EMPILE tous
 * les problèmes du lieu (aucune bulle par diagnostic — la fiche les
 * porte toutes, un filet sépare chaque problème) et l'INDICE
 * D'ACTIONS clôt la fiche : le lien du bas (« More actions… ») qui
 * mène aux correctifs — le libellé arrive de l'hôte, la carte reste
 * pure ; null ne pose rien. Fermée à la frappe et au défilement par
 * le contrôleur ; l'ancre vit chez {@link PopupAnchor#atPoint}.
 */
public final class DiagnosticTooltipView extends TextRowsPopup {

    /** La sévérité d'un diagnostic — son marqueur et son encre suivent. */
    public enum Severity {
        /** Une erreur — le marqueur « × » à l'encre d'erreur. */
        ERROR,
        /** Un avertissement — le marqueur « ! » à l'encre d'avertissement. */
        WARNING
    }

    /** Un problème de la fiche : son message et sa sévérité — le
     *  marqueur et l'encre suivent la sévérité. */
    public record Probleme(String message, Severity severity) {
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
        setProblemes(message == null ? List.of()
                : List.of(new Probleme(message, severity)), null);
    }

    /**
     * Pose les problèmes du lieu — l'audit Android Studio du
     * 2026-11-14 : l'IDE de référence empile TOUTES les signatures de
     * la position dans sa fiche, un filet sépare les problèmes, et
     * l'indice d'actions clôt (« More actions… ») quand des correctifs
     * existent.
     *
     * @param problemes   les problèmes du lieu, chacun son message et
     *                    sa sévérité
     * @param plusActions le libellé de l'indice d'actions (le lien du
     *                    bas de l'IDE), null ou vide pour aucun
     */
    public void setProblemes(List<Probleme> problemes,
            @Nullable String plusActions) {
        List<Row> rangees = new ArrayList<>();
        boolean premier = true;
        for (Probleme probleme : problemes) {
            Row marqueur = Row.tier(marqueurDe(probleme),
                    encreDe(probleme), Tier.TITRE);
            rangees.add(premier ? marqueur : Row.apresFilet(marqueur));
            premier = false;
            for (String ligne : probleme.message().split("\n", -1)) {
                rangees.add(Row.of(ligne));
            }
        }
        if (plusActions != null && !plusActions.isEmpty()) {
            rangees.add(Row.apresFilet(
                    Row.tier(plusActions, palette.muted(), Tier.META)));
        }
        setRows(rangees);
    }

    /** Le marqueur du problème — « × » à l'erreur, « ! » sinon. */
    private static String marqueurDe(Probleme probleme) {
        return probleme.severity() == Severity.ERROR ? "×" : "!";
    }

    /** L'encre du marqueur — l'erreur ou l'avertissement suit la
     *  sévérité (l'encre explicite se pose enfin, l'entier négatif
     *  n'est plus confondu avec la sentinelle). */
    private int encreDe(Probleme probleme) {
        return probleme.severity() == Severity.ERROR
                ? palette.error() : palette.warning();
    }
}
