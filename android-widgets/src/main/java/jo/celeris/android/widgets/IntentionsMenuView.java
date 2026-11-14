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
 * le menu et déroule l'action choisie. PIEGE-27 : jamais de focus —
 * la navigation clavier s'y reçoit DÉLÉGUÉE ({@link PopupClavier}).
 *
 * <p>Le menu de DÉBORDEMENT de la barre de sélection y prend le motif
 * NavMenu du dépôt de départ : une SECTION en petites capitales muettes
 * (le « MORE » du dépôt de départ) annonce les rangées — elle ne se
 * prend jamais, le choix d'une action la referme avant de l'exécuter.
 * {@link #setSection(String)} la pose, null la retire.</p>
 */
public final class IntentionsMenuView extends TextRowsPopup implements PopupClavier {

    /** Une action du menu : son libellé, son pictogramme propre
     *  (null : l'action sans glyphe) et ce qu'elle fait — l'audit du
     *  propriétaire du 2026-11-14 : chaque action porte SON icône,
     *  jamais la même pour toutes. */
    public record Item(String label, IconeAction picto, Runnable action) {

        /** L'action sans glyphe — les hôtes qui ne posent pas
         *  d'icône gardent leur rangée telle quelle. */
        public Item(String label, Runnable action) {
            this(label, null, action);
        }
    }

    private List<Item> actions = List.of();
    /** L'en-tête de section du débordement — null : pas d'en-tête. */
    private @Nullable String section;
    /** L'action surlignée par le clavier délégué — —1 si aucune. */
    private int surligne = -1;

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
        // le contenu neuf efface la surbrillance clavier — jamais
        // d'action retenue d'un contenu d'avant
        this.surligne = -1;
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

    /** Rebâtit les rangées : la section d'abord, les actions
     *  ensuite — chacune son pictogramme quand l'hôte l'a posé. */
    private void rebatirRangees() {
        List<Row> rangees = new ArrayList<>(actions.size() + 1);
        if (section != null && !section.isEmpty()) {
            rangees.add(Row.section(section));
        }
        for (Item item : actions) {
            rangees.add(Row.action(item.label(), -1, item.picto()));
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

    /** L'action surlignée par le clavier délégué, —1 si aucune — les
     *  bancs y vérifient la navigation. */
    public int surbrillance() {
        return surligne;
    }

    @Override
    public boolean surFleche(int pas) {
        if (actions.isEmpty()) {
            return false;
        }
        // la flèche part de la tête quand la surbrillance n'existe pas —
        // jamais de saut hors des bornes ni d'arrêt sur l'en-tête
        int cible = surligne < 0 ? 0
                : Math.min(actions.size() - 1, Math.max(0, surligne + pas));
        surligne = cible;
        setHighlighted(rangDe(cible));
        return true;
    }

    @Override
    public boolean surPage(int pas) {
        // le menu décline les pages : ses actions tiennent en peu de
        // rangées — la touche retourne à l'éditeur, le document y défile
        return false;
    }

    @Override
    public boolean surValidation() {
        if (surligne < 0 || surligne >= actions.size()) {
            return false;
        }
        // la validation clavier prend le MÊME chemin que le doigt : le
        // runnable de l'action seule décide ce que choisir veut dire
        actions.get(surligne).action().run();
        performClick();
        return true;
    }

    /** Le rang d'affichage de l'action d'index donné — l'en-tête de
     *  section éventuel compte une rangée, jamais plus il ne se prend. */
    private int rangDe(int index) {
        return index + (section != null && !section.isEmpty() ? 1 : 0);
    }
}
