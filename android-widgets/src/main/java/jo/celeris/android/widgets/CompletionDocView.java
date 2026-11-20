package jo.celeris.android.widgets;

import android.content.Context;
import jo.celeris.android.widgets.internal.TextRowsPopup;

import java.util.ArrayList;
import java.util.List;

/**
 * La documentation de complétion (portage du design du dépôt de départ
 * — B-09, D-10) : les TROIS BLOCS du panneau intégré — la bande
 * SIGNATURE teintée (le titre, jamais fondu dans le corps, PIEGE-16),
 * le RÉSUMÉ en corps, les lignes PARAM indentées à l'encre des
 * mots-clés et RETOUR en gras atténué. Chaque section typée porte son
 * libellé de genre en micro-typographie, un filet l'ouvre. Le survol
 * de symbole s'y sert aussi (son contenu est le même modèle).
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
        List<Row> rows = new ArrayList<>();
        // La bande signature : teintée, à l'encre d'accent.
        rows.add(Row.teintee(model.titre(), palette.accent(),
                Tier.TITRE));
        for (CompletionDocModel.Section section : model.sections()) {
            rows.add(Row.afterRule(Row.tier(section.kind(),
                    palette.muted(), Tier.MICRO)));
            setBody(rows, section.text());
        }
        if (!model.freeText().isEmpty()) {
            List<Row> bloc = new ArrayList<>();
            setBody(bloc, model.freeText());
            if (!bloc.isEmpty()) {
                bloc.set(0, Row.afterRule(bloc.get(0)));
                rows.addAll(bloc);
            }
        }
        setRows(rows);
    }

    /**
     * Le corps d'un bloc — section typée ou texte libre : les lignes
     * PARAM s'indentent à l'encre des mots-clés, RETOUR passe au
     * gras atténué, les listes à tirets des serveurs LSP (« - nom —
     * description ») s'indentent de la même encre — la doc rapide
     * d'Android Studio y rend ses paramètres de la même façon, le
     * reste vit en corps.
     */
    private void setBody(List<Row> rows, String body) {
        for (String line : body.split("\n", -1)) {
            if (line.startsWith("@param")) {
                rows.add(Row.indentee(line, palette.keyword(), Tier.META));
            } else if (line.startsWith("@return")) {
                rows.add(Row.indentee(line, palette.muted(), Tier.META));
            } else if (estTiret(line)) {
                rows.add(Row.indentee(line, palette.keyword(), Tier.META));
            } else {
                rows.add(Row.of(line));
            }
        }
    }

    /** La ligne de liste — « - » ou « * » ouvre l'item du serveur. */
    private static boolean estTiret(String line) {
        String epuree = line.strip();
        return epuree.startsWith("- ") || epuree.startsWith("* ");
    }
}
