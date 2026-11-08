package jo.celeris.android.widgets;

import android.content.Context;

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
        List<Row> rangees = new ArrayList<>();
        // La bande signature : teintée, à l'encre d'accent.
        rangees.add(Row.teintee(model.titre(), palette.accent(),
                Tier.TITRE));
        for (CompletionDocModel.Section section : model.sections()) {
            rangees.add(Row.apresFilet(Row.tier(section.genre(),
                    palette.muted(), Tier.MICRO)));
            for (String ligne : section.texte().split("\n", -1)) {
                rangees.add(Row.of(ligne));
            }
        }
        if (!model.texteLibre().isEmpty()) {
            poserTexteLibre(rangees, model.texteLibre());
        }
        setRows(rangees);
    }

    /**
     * Le texte libre du serveur : les lignes PARAM s'indentent à
     * l'encre des mots-clés, RETOUR passe au gras atténué, le reste
     * vit en corps — un filet ouvre le bloc.
     */
    private void poserTexteLibre(List<Row> rangees, String texteLibre) {
        List<Row> bloc = new ArrayList<>();
        for (String ligne : texteLibre.split("\n", -1)) {
            if (ligne.startsWith("@param")) {
                bloc.add(Row.indentee(ligne, palette.keyword(), Tier.META));
            } else if (ligne.startsWith("@return")) {
                bloc.add(Row.indentee(ligne, palette.muted(), Tier.META));
            } else {
                bloc.add(Row.of(ligne));
            }
        }
        if (!bloc.isEmpty()) {
            bloc.set(0, Row.apresFilet(bloc.get(0)));
            rangees.addAll(bloc);
        }
    }
}
