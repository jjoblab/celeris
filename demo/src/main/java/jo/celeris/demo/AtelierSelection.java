package jo.celeris.demo;

import java.util.List;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.SelectionBarView;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier de sélection de la démonstration (M7.5, §7, reprise) : les
 * actions de la barre de sélection en transactions de session, un
 * presse-papiers LOCAL qui ne touche pas au système (les gestes de la
 * recette restent déterministes, l'atelier n'emporte rien au-dehors).
 * Les PRINCIPALES tiennent la barre — couper, copier, coller —, le
 * DÉBORDEMENT vit derrière la puce du même nom — tout sélectionner —
 * le motif de la barre de la plateforme.
 */
final class AtelierSelection {

    private final EditorView editeur;
    private final TextSession session;
    /** Le presse-papiers local — jamais le système. */
    private String pressePapiers;

    /**
     * Construit l'atelier de sélection.
     *
     * @param editeur la vue d'éditeur ouverte
     * @param session la session d'édition courante
     */
    AtelierSelection(EditorView editeur, TextSession session) {
        this.editeur = editeur;
        this.session = session;
    }

    /**
     * Les actions principales de la barre — celles qui tiennent en
     * pastilles, libellés compris.
     *
     * @param couper le libellé de coupe
     * @param copier le libellé de copie
     * @param coller le libellé de collage
     * @return les items principaux de la barre de sélection
     */
    List<SelectionBarView.Item> principales(String couper, String copier,
            String coller) {
        return List.of(
                new SelectionBarView.Item(couper, this::couper),
                new SelectionBarView.Item(copier, this::copier),
                new SelectionBarView.Item(coller, this::coller));
    }

    /**
     * Les actions de débordement — servies par le menu de la puce.
     *
     * @param tout le libellé de tout-sélectionner
     * @return les items repliés derrière la puce de débordement
     */
    List<SelectionBarView.Item> debordement(String tout) {
        return List.of(new SelectionBarView.Item(tout,
                this::toutSelectionner));
    }

    private void couper() {
        copier();
        remplacer("");
    }

    private void copier() {
        SelRange plage = plagePrimaire();
        if (plage != null && !plage.isEmpty()) {
            pressePapiers = session.document().subText(
                    plage.start(), plage.end());
        }
    }

    private void coller() {
        if (pressePapiers != null) {
            remplacer(pressePapiers);
        }
    }

    private void toutSelectionner() {
        session.setSelection(MultiSelection.of(
                new SelRange(0, session.document().length())));
    }

    private void remplacer(String texte) {
        SelRange plage = plagePrimaire();
        if (plage == null) {
            return;
        }
        session.replaceRange(plage.start(), plage.end(), texte);
        editeur.invalidate();
    }

    private SelRange plagePrimaire() {
        List<SelRange> plages = session.selection().ranges();
        return plages.size() == 1 ? plages.get(0) : null;
    }
}
