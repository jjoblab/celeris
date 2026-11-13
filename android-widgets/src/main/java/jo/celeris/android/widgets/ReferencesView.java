package jo.celeris.android.widgets;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * La carte des usages du symbole (catégorie C de l'inventaire — le
 * signal du propriétaire du 2026-11-13, « lancé 90 % du chantier ») :
 * les références au curseur en rangées cliquables au palier corps,
 * l'en-tête de section compte — « N usages de « symbole » » —, le
 * choix d'une occurrence exécute son saut (le Runnable de l'hôte
 * seul décide ce que sauter veut dire : porter le curseur au lieu,
 * refermer la carte). PIEGE-27 : jamais de focus — la navigation
 * clavier s'y reçoit DÉLÉGUÉE ({@link PopupClavier} : flèches,
 * validation ; les pages déclinent — les rangées vivent toutes à
 * découvert).
 *
 * <p>Le patron NavMenu du menu d'intentions y revient : l'en-tête de
 * section en petites capitales muettes annonce les occurrences — il ne
 * se prend jamais, la flèche n'y atterrit jamais.</p>
 */
public final class ReferencesView extends TextRowsPopup implements PopupClavier {

    /** Une occurrence d'usage : son libellé et son saut. */
    public record Occurrence(String libelle, Runnable saut) {
    }

    private List<Occurrence> occurrences = List.of();
    /** L'en-tête qui compte les usages — null : pas d'en-tête. */
    private @Nullable String titre;
    /** L'occurrence surlignée par le clavier délégué — —1 si aucune. */
    private int surligne = -1;

    public ReferencesView(Context context, PopupPalette palette) {
        super(context, palette);
        setOnPick(this::prendre);
    }

    /**
     * Pose l'en-tête de section — le compte des usages du symbole, la
     * plupart du temps. Null le retire.
     *
     * @param titre le titre de la section, null pour aucun
     */
    public void setTitre(@Nullable String titre) {
        this.titre = titre;
        rebatirRangees();
    }

    /**
     * Pose les occurrences — les rangées se rebâtissent, la
     * surbrillance s'efface : jamais d'occurrence retenue d'un
     * contenu d'avant.
     *
     * @param usages les occurrences, dans l'ordre du serveur
     */
    public void setOccurrences(List<Occurrence> usages) {
        this.occurrences = List.copyOf(usages);
        this.surligne = -1;
        rebatirRangees();
    }

    /** L'occurrence d'index donné, null s'il n'existe pas. */
    public @Nullable Occurrence occurrenceAt(int index) {
        return index >= 0 && index < occurrences.size()
                ? occurrences.get(index) : null;
    }

    /** Le nombre d'occurrences posées — l'hôte y compte ses usages. */
    public int nombre() {
        return occurrences.size();
    }

    /** L'occurrence surlignée par le clavier délégué, —1 si aucune —
     *  les bancs y vérifient la navigation. */
    public int surbrillance() {
        return surligne;
    }

    /** Rebâtit les rangées : l'en-tête d'abord, les occurrences ensuite. */
    private void rebatirRangees() {
        List<Row> rangees = new ArrayList<>(occurrences.size() + 1);
        if (titre != null && !titre.isEmpty()) {
            rangees.add(Row.section(titre));
        }
        for (Occurrence usage : occurrences) {
            rangees.add(Row.tier(usage.libelle(), -1, Tier.CORPS));
        }
        setRows(rangees);
    }

    /** L'appui d'une rangée : le rang compte l'en-tête éventuel. */
    private void prendre(int rang) {
        sauter(rang - decalage());
    }

    /** Le saut de l'occurrence d'index donné — le Runnable de l'hôte
     *  seul décide (refermer la carte y vit, l'atelier l'exécute). */
    private void sauter(int index) {
        if (index >= 0 && index < occurrences.size()) {
            occurrences.get(index).saut().run();
        }
    }

    @Override
    public boolean surFleche(int pas) {
        if (occurrences.isEmpty()) {
            return false;
        }
        // la flèche part de la tête quand la surbrillance n'existe pas —
        // jamais de saut hors des bornes ni d'arrêt sur l'en-tête
        int cible = surligne < 0 ? 0
                : Math.min(occurrences.size() - 1, Math.max(0, surligne + pas));
        surligne = cible;
        setHighlighted(rangDe(cible));
        return true;
    }

    @Override
    public boolean surPage(int pas) {
        // la carte décline les pages : ses rangées vivent toutes à
        // découvert — la touche retourne à l'éditeur, le document y défile
        return false;
    }

    @Override
    public boolean surValidation() {
        if (surligne < 0 || surligne >= occurrences.size()) {
            return false;
        }
        // la validation clavier prend le MÊME chemin que le doigt : le
        // Runnable de l'occurrence seul décide ce que sauter veut dire
        occurrences.get(surligne).saut().run();
        performClick();
        return true;
    }

    /** Le rang d'affichage de l'occurrence d'index donné — l'en-tête
     *  éventuel compte une rangée, jamais plus il ne se prend. */
    private int rangDe(int index) {
        return index + decalage();
    }

    /** Le décalage des rangées d'occurrence — l'en-tête éventuel
     *  compte une rangée. */
    private int decalage() {
        return titre != null && !titre.isEmpty() ? 1 : 0;
    }
}
