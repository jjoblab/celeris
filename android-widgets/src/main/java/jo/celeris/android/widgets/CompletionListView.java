package jo.celeris.android.widgets;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;


/**
 * La liste de complétion (M7.5, §7, reprise — B-03, D-10) : les
 * candidats en rangées, le candidat courant surligné, l'appui choisit
 * — le FILTRAGE vit chez l'hôte (la complétion filtre sans se fermer,
 * M7.5 §7) : la liste ne montre que ce qu'on lui pose, la requête ne
 * repart jamais d'ici. PIEGE-27 : jamais de focus, l'éditeur garde le
 * clavier.
 */
public final class CompletionListView extends TextRowsPopup {

    /** Le nombre maximal de candidats montrés — l'hôte tronque sa fenêtre. */
    public static final int MAX_MONTRES = 8;

    private List<String> candidats = List.of();

    public CompletionListView(Context context, PopupPalette palette) {
        super(context, palette);
    }

    /**
     * Pose les candidats — le premier est le candidat courant,
     * surligné.
     *
     * @param items les candidats, déjà filtrés par l'hôte
     */
    public void setItems(List<String> items) {
        this.candidats = List.copyOf(items);
        List<Row> rangees = new ArrayList<>(items.size());
        for (String item : items) {
            rangees.add(Row.of(item));
        }
        setRows(rangees);
        setHighlighted(items.isEmpty() ? -1 : 0);
    }

    /** Le candidat d'index donné, null s'il n'existe pas. */
    public String itemAt(int index) {
        return index >= 0 && index < candidats.size()
                ? candidats.get(index) : null;
    }
}
