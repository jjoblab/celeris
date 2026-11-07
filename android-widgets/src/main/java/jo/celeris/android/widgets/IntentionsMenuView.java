package jo.celeris.android.widgets;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Le menu d'intentions (M7.5, §7, reprise — F-03, D-10) : les actions
 * disponibles au curseur en rangées cliquables — intentions
 * classiques (lint, aperçu, revue) et actions IA au même titre, même
 * barre, même gestuelle. L'appui exécute : l'hôte ferme le menu et
 * déroule l'action choisie.
 */
public final class IntentionsMenuView extends TextRowsPopup {

    /** Une action du menu : son libellé et ce qu'elle fait. */
    public record Item(String label, Runnable action) {
    }

    private List<Item> actions = List.of();

    public IntentionsMenuView(Context context, PopupPalette palette) {
        super(context, palette);
        setOnPick(this::run);
    }

    /**
     * Pose les actions du menu.
     *
     * @param items les actions, dans l'ordre d'affichage
     */
    public void setActions(List<Item> items) {
        this.actions = List.copyOf(items);
        List<Row> rangees = new ArrayList<>(items.size());
        for (Item item : items) {
            rangees.add(Row.of(item.label()));
        }
        setRows(rangees);
    }

    private void run(int index) {
        if (index >= 0 && index < actions.size()) {
            actions.get(index).action().run();
        }
    }

    /** L'action d'index donné, null s'il n'existe pas — lisibilité des hôtes. */
    public @Nullable Item itemAt(int index) {
        return index >= 0 && index < actions.size() ? actions.get(index)
                : null;
    }
}
