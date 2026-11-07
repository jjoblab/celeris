package jo.celeris.android.widgets;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * La documentation de complétion (M7.5, §7, reprise — B-09, D-10) :
 * le modèle déjà livré ({@link CompletionDocModel}) devient des
 * rangées — le titre à l'encre d'accent, jamais fondu dans le corps
 * (PIEGE-16), chaque section typée son libellé de genre à part, le
 * texte libre à la suite. Le survol de symbole s'y sert aussi (son
 * contenu est le même modèle).
 */
public final class CompletionDocView extends TextRowsPopup {

    public CompletionDocView(Context context, PopupPalette palette) {
        super(context, palette);
    }

    /**
     * Pose la documentation à montrer.
     *
     * @param model la documentation structurée du serveur, ou null
     *              pour vider le panneau
     */
    public void setModel(CompletionDocModel model) {
        if (model == null) {
            setRows(List.of());
            return;
        }
        List<Row> rangees = new ArrayList<>();
        rangees.add(Row.of(model.titre(), palette.accent()));
        for (CompletionDocModel.Section section : model.sections()) {
            rangees.add(Row.of("[" + section.genre() + "]",
                    palette.accent()));
            for (String ligne : section.texte().split("\n", -1)) {
                rangees.add(Row.of(ligne));
            }
        }
        if (!model.texteLibre().isEmpty()) {
            for (String ligne : model.texteLibre().split("\n", -1)) {
                rangees.add(Row.of(ligne));
            }
        }
        setRows(rangees);
    }
}
