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
            poserCorps(rangees, section.texte());
        }
        if (!model.texteLibre().isEmpty()) {
            List<Row> bloc = new ArrayList<>();
            poserCorps(bloc, model.texteLibre());
            if (!bloc.isEmpty()) {
                bloc.set(0, Row.apresFilet(bloc.get(0)));
                rangees.addAll(bloc);
            }
        }
        setRows(rangees);
    }

    /**
     * Le corps d'un bloc — section typée ou texte libre : les lignes
     * PARAM s'indentent à l'encre des mots-clés, RETOUR passe au
     * gras atténué, les listes à tirets des serveurs LSP (« - nom —
     * description ») s'indentent de la même encre — la doc rapide
     * d'Android Studio y rend ses paramètres de la même façon, le
     * reste vit en corps.
     */
    private void poserCorps(List<Row> rangees, String corps) {
        for (String ligne : corps.split("\n", -1)) {
            if (ligne.startsWith("@param")) {
                rangees.add(Row.indentee(ligne, palette.keyword(), Tier.META));
            } else if (ligne.startsWith("@return")) {
                rangees.add(Row.indentee(ligne, palette.muted(), Tier.META));
            } else if (estTiret(ligne)) {
                rangees.add(Row.indentee(ligne, palette.keyword(), Tier.META));
            } else {
                rangees.add(Row.of(ligne));
            }
        }
    }

    /** La ligne de liste — « - » ou « * » ouvre l'item du serveur. */
    private static boolean estTiret(String ligne) {
        String epuree = ligne.strip();
        return epuree.startsWith("- ") || epuree.startsWith("* ");
    }
}
