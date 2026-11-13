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
 * se prend jamais, la flèche n'y atterrit jamais. L'audit Android
 * Studio du 2026-11-14 y pose les usages ÉTRANGERS : l'IDE de
 * référence montre les usages de tous les fichiers (groupés par
 * fichier) — l'éditeur embarqué n'ouvre qu'un document, l'occurrence
 * au saut nul vit MUETTE (le nom du fichier du serveur se lit, le
 * lieu ne se visite pas, jamais il ne se devine) et ni le doigt ni
 * la flèche ne la prennent.</p>
 */
public final class ReferencesView extends TextRowsPopup implements PopupClavier {

    /** Une occurrence d'usage : son libellé et son saut — null pour
     *  l'étrangère (hors du document ouvert : muette, jamais prise). */
    public record Occurrence(String libelle, @Nullable Runnable saut) {
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

    /** Rebâtit les rangées : l'en-tête d'abord, les occurrences
     *  ensuite — l'étrangère muette, la locale à l'encre du texte. */
    private void rebatirRangees() {
        List<Row> rangees = new ArrayList<>(occurrences.size() + 1);
        if (titre != null && !titre.isEmpty()) {
            rangees.add(Row.section(titre));
        }
        for (Occurrence usage : occurrences) {
            rangees.add(Row.tier(usage.libelle(),
                    usage.saut() == null ? palette.muted() : -1,
                    Tier.CORPS));
        }
        setRows(rangees);
    }

    /** L'appui d'une rangée : le rang compte l'en-tête éventuel,
     *  l'étrangère muette ne se prend jamais. */
    private void prendre(int rang) {
        sauter(rang - decalage());
    }

    /** Le saut de l'occurrence d'index donné — le Runnable de l'hôte
     *  seul décide (refermer la carte y vit, l'atelier l'exécute) ;
     *  l'étrangère au saut nul ne saute pas. */
    private void sauter(int index) {
        if (index >= 0 && index < occurrences.size()) {
            Runnable saut = occurrences.get(index).saut();
            if (saut != null) {
                saut.run();
            }
        }
    }

    @Override
    public boolean surFleche(int pas) {
        if (occurrences.isEmpty()) {
            return false;
        }
        // la flèche part de la tête PRENABLE quand la surbrillance
        // n'existe pas — jamais de saut hors des bornes, jamais d'arrêt
        // sur l'en-tête ni sur l'étrangère muette
        int cible = surligne < 0 ? premierPrenable(pas)
                : bornePrenable(surligne, pas);
        if (cible < 0) {
            return false;
        }
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
        Runnable saut = occurrences.get(surligne).saut();
        if (saut == null) {
            return false;
        }
        // la validation clavier prend le MÊME chemin que le doigt : le
        // Runnable de l'occurrence seul décide ce que sauter veut dire
        saut.run();
        performClick();
        return true;
    }

    /** Le premier prenable depuis le départ, dans la direction —
     *  l'étrangère muette se saute, jamais elle ne s'arrête. */
    private int premierPrenable(int pas) {
        int taille = occurrences.size();
        int cible = pas >= 0 ? 0 : taille - 1;
        while (cible >= 0 && cible < taille) {
            if (estPrenable(cible)) {
                return cible;
            }
            cible += pas >= 0 ? 1 : -1;
        }
        return -1;
    }

    /** Le prenable depuis l'index donné, borné aux extrêmes — l'arrêt
     *  sur l'étrangère muette ne s'invente pas. */
    private int bornePrenable(int courant, int pas) {
        int taille = occurrences.size();
        int cible = Math.min(taille - 1, Math.max(0, courant + pas));
        while (cible >= 0 && cible < taille && !estPrenable(cible)) {
            cible += pas >= 0 ? 1 : -1;
        }
        if (cible < 0 || cible >= taille) {
            return courant;
        }
        return cible;
    }

    /** Vrai quand l'occurrence d'index donné se prend — l'étrangère
     *  muette ne se prend jamais. */
    private boolean estPrenable(int index) {
        return index >= 0 && index < occurrences.size()
                && occurrences.get(index).saut() != null;
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
