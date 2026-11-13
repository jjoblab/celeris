package jo.celeris.android.widgets;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Le menu d'intentions (M7.5, §7, reprise — F-03, D-10) : les actions
 * disponibles au curseur en rangées cliquables au palier titre —
 * intentions classiques (lint, aperçu, revue) et actions IA au même
 * titre, même barre, même gestuelle. L'appui exécute : l'hôte ferme
 * le menu et déroule l'action choisie.
 *
 * <p>Le menu de DÉBORDEMENT de la barre de sélection y prend le motif
 * NavMenu du dépôt de départ : une SECTION en petites capitales muettes
 * (le « MORE » du dépôt de départ) annonce les rangées — elle ne se
 * prend jamais, le choix d'une action la referme avant de l'exécuter.
 * {@link #setSection(String)} la pose, null la retire.</p>
 */
public final class IntentionsMenuView extends TextRowsPopup {

    /** Une action du menu : son libellé et ce qu'elle fait. */
    public record Item(String label, Runnable action) {
    }

    private List<Item> actions = List.of();
    /** L'en-tête de section du débordement — null : pas d'en-tête. */
    private @Nullable String section;

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
        rebatirRangees();
    }

    /**
     * Pose l'en-tête de section — le motif NavMenu du dépôt de
     * départ : les petites capitales muettes qui annoncent les rangées
     * du débordement de la barre de sélection. Null la retire.
     *
     * @param titre le titre de la section, null pour aucune
     */
    public void setSection(@Nullable String titre) {
        this.section = titre;
        rebatirRangees();
    }

    /** Rebâtit les rangées : la section d'abord, les actions ensuite. */
    private void rebatirRangees() {
        List<Row> rangees = new ArrayList<>(actions.size() + 1);
        if (section != null && !section.isEmpty()) {
            rangees.add(Row.section(section));
        }
        for (Item item : actions) {
            rangees.add(Row.tier(item.label(), -1, Tier.TITRE));
        }
        setRows(rangees);
    }

    /** L'appui d'une rangée : le rang compte la section éventuelle. */
    private void run(int rang) {
        int decalage = section != null && !section.isEmpty() ? 1 : 0;
        int index = rang - decalage;
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
