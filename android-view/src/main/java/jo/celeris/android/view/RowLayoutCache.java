package jo.celeris.android.view;

import org.jspecify.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * Cache de mises en page par rangée, indexé sur des valeurs immuables
 * (PIEGE-02, ADR-006).
 *
 * <p>La consultation passe par les valeurs brutes — révision, rangée,
 * thème, police — et n'alloue rien (PIEGE-15) : l'empreinte est calculée
 * sur les valeurs, la sonde compare champ à champ. Le dépôt et
 * l'invalidation, hors du chemin de dessin, allouent librement.</p>
 *
 * <p>Adressage ouvert à sonde linéaire, capacité puissance de deux,
 * agrandissement aux trois quarts de charge.</p>
 */
public final class RowLayoutCache {

    private static final int INITIAL_CAPACITY = 64;
    private static final int QUART = 4;
    /** Le plafond d'entrées (B-02, l'analyse croisée) : comme le
     *  LineRenderCache du dépôt de départ — 512 rangées mesurées
     *  suffisent à un viewport et à son préchargement ; au-delà, les
     *  moins récemment déposées partent (le défilement d'un document
     *  monstre n'emplit plus la table à vie). */
    public static final int CEILING = 512;

    private static final class Entry {
        private final RowKey key;
        private RowLayout value;
        /** L'âge LRU : le numéro de dépôt — les plus vieux partent
         *  au plafond. */
        long age;

        Entry(RowKey key, RowLayout value, long age) {
            this.key = key;
            this.value = value;
            this.age = age;
        }
    }

    private Entry[] table = new Entry[INITIAL_CAPACITY];
    private int nombre;
    private long compteurAge;

    /**
     * Consulte la mise en page d'une rangée, sans allouer.
     *
     * @param revision la révision de l'instantané
     * @param row      le numéro de rangée visuelle
     * @param themeId  l'identifiant du thème
     * @param font     la spécification de police
     * @param columns  le budget de colonnes du retour à la ligne (PIEGE-02)
     * @return la mise en page en cache, null si absente
     */
    public @Nullable RowLayout find(long revision, int row, int themeId, FontSpec font,
            int columns) {
        Entry[] cases = table;
        int indice = empreinte(revision, row, themeId, font, columns) & (cases.length - 1);
        while (cases[indice] != null) {
            Entry caseCourante = cases[indice];
            if (correspond(caseCourante, revision, row, themeId, font, columns)) {
                // Servie = vivante (LRU) : l'âge se rafraîchit hors
                // du chemin d'éviction.
                caseCourante.age = ++compteurAge;
                return caseCourante.value;
            }
            indice = (indice + 1) & (cases.length - 1);
        }
        return null;
    }

    /**
     * Consulte la mise en page d'une rangée sans retour à la ligne
     * (budget nul) — forme héritée du socle de rendu.
     *
     * @param revision la révision de l'instantané
     * @param row      le numéro de rangée visuelle
     * @param themeId  l'identifiant du thème
     * @param font     la spécification de police
     * @return la mise en page en cache, null si absente
     */
    public @Nullable RowLayout find(long revision, int row, int themeId, FontSpec font) {
        return find(revision, row, themeId, font, 0);
    }

    /**
     * Dépose (ou remplace) la mise en page d'une rangée.
     *
     * @param key    la clé immuable de la rangée
     * @param layout la mise en page à déposer
     */
    public void store(RowKey key, RowLayout layout) {
        if (nombre + 1 > table.length * 3 / QUART) {
            agrandir();
        }
        int indice = empreinte(key.revision(), key.row(), key.themeId(), key.font(),
                key.columns()) & (table.length - 1);
        while (table[indice] != null) {
            if (table[indice].key.equals(key)) {
                table[indice].value = layout;
                table[indice].age = ++compteurAge;
                return;
            }
            indice = (indice + 1) & (table.length - 1);
        }
        table[indice] = new Entry(key, layout, ++compteurAge);
        nombre++;
        if (nombre > CEILING) {
            evincerLesPlusVieilles();
        }
    }

    /** Évince les entrées les moins récemment servies : la table
     *  revient sous le plafond (B-02). Hors du chemin de dessin —
     *  l'éviction ne vit que lors des dépôts. */
    private void evincerLesPlusVieilles() {
        // Le seuil d'âge : les PLAFOND entrées les plus récentes
        // survivent — un tri partiel par sélection des ages max.
        long[] ages = new long[nombre];
        int guard = 0;
        for (Entry caseCourante : table) {
            if (caseCourante != null) {
                ages[guard++] = caseCourante.age;
            }
        }
        java.util.Arrays.sort(ages);
        long seuil = ages[ages.length - CEILING];
        Entry[] anciennes = table;
        table = new Entry[INITIAL_CAPACITY];
        int anciens = nombre;
        nombre = 0;
        for (int i = 0; i < anciennes.length; i++) {
            Entry caseCourante = anciennes[i];
            if (caseCourante != null && caseCourante.age >= seuil) {
                store(caseCourante.key, caseCourante.value);
            }
        }
        // Le plafond tient par construction : seules les PLAFOND
        // entrées les plus récentes ont survécu au seuil.
    }

    /**
     * Épisse le cache à une édition (B-01, l'analyse croisée) : les
     * rangées AVANT la zone touchée survivent par IDENTITÉ,
     * re-timbrées à la nouvelle révision ; celles de la zone touchée
     * meurent (elles se re-mesurent au prochain dessin) ; celles
     * d'APRÈS survivent re-timbrées et DÉCALÉES du nombre de rangées
     * et de lignes qu'amène l'édition. Le dépôt de départ documente
     * le principe à propos de son LineRenderCache : « un compteur de
     * révision global invaliderait toutes les lignes en cache à
     * chaque édition, ruinant le cache ».
     *
     * @param ancienneRevision la révision avant l'édition
     * @param nouvelleRevision la révision après l'édition
     * @param premiereRangee   la première rangée visuelle touchée
     * @param rangeesMortes    le nombre de rangées de la zone morte
     *                         (au moins une : la portée de l'édité)
     * @param deltaRangees     le décalage de rangées visuelles
     *                         (nouvelle rowCount − ancienne)
     * @param deltaLignes      le décalage de numéros de ligne
     */
    public void splice(long ancienneRevision, long nouvelleRevision,
            int premiereRangee, int rangeesMortes, int deltaRangees,
            int deltaLignes) {
        Entry[] restantes = new Entry[nombre];
        int gardees = 0;
        int finZone = premiereRangee + Math.max(1, rangeesMortes);
        for (Entry caseCourante : table) {
            if (caseCourante == null
                    || caseCourante.key.revision() != ancienneRevision) {
                continue;
            }
            RowKey key = caseCourante.key;
            RowLayout value = caseCourante.value;
            if (key.row() < premiereRangee) {
                // Avant la zone : survie par identité, re-timbrée.
                restantes[gardees++] = new Entry(
                        new RowKey(nouvelleRevision, key.row(),
                                key.themeId(), key.font(), key.columns()),
                        value, caseCourante.age);
            } else if (key.row() >= finZone) {
                // Après la zone : décalage de rangée et de ligne.
                restantes[gardees++] = new Entry(
                        new RowKey(nouvelleRevision, key.row() + deltaRangees,
                                key.themeId(), key.font(), key.columns()),
                        new RowLayout(value.line() + deltaLignes,
                                value.start(), value.end(),
                                value.text(), value.width(),
                                value.runs(), value.spans()),
                        caseCourante.age);
            }
            // La zone touchée : la mise en page meurt — re-mesure au
            // prochain dessin.
        }
        Entry[] anciennes = table;
        table = new Entry[INITIAL_CAPACITY];
        nombre = 0;
        for (Entry caseCourante : anciennes) {
            if (caseCourante != null
                    && caseCourante.key.revision() != ancienneRevision) {
                store(caseCourante.key, caseCourante.value);
            }
        }
        for (int i = 0; i < gardees; i++) {
            store(restantes[i].key, restantes[i].value);
        }
    }

    /**
     * Retire toutes les mises en page de la révision donnée.
     *
     * @param revision la révision périmée
     */
    public void invalidate(long revision) {
        Entry[] restantes = new Entry[nombre];
        int gardees = 0;
        for (Entry caseCourante : table) {
            if (caseCourante != null && caseCourante.key.revision() != revision) {
                restantes[gardees++] = caseCourante;
            }
        }
        table = new Entry[INITIAL_CAPACITY];
        nombre = 0;
        for (int i = 0; i < gardees; i++) {
            store(restantes[i].key, restantes[i].value);
        }
    }

    /**
     * Retire TOUTES les mises en page, toutes révisions comprises — le
     * changement de source qui ne signe aucune composante de clé
     * (changement de langage, changement de session) ne peut pas servir
     * une mise en page périmée (PIEGE-02 par construction).
     */
    public void clear() {
        table = new Entry[INITIAL_CAPACITY];
        nombre = 0;
    }

    /**
     * Re-spanne les mises en page de la révision donnée : l'opérateur
     * rend la même instance quand les plages n'ont pas changé — la
     * MESURE survit à la publication, seules les rangées dont les
     * couleurs ont bougé se remplacent (PIEGE-26).
     *
     * @param revision la révision des mises en page à re-spanner
     * @param aJour     l'opérateur de re-span, rendant la même instance
     *                  sans changement
     */
    public void respan(long revision, UnaryOperator<RowLayout> aJour) {
        for (Entry caseCourante : table) {
            if (caseCourante != null && caseCourante.key.revision() == revision) {
                caseCourante.value = aJour.apply(caseCourante.value);
            }
        }
    }

    /** Le nombre de mises en page en cache. */
    public int size() {
        return nombre;
    }

    /** Le plafond d'entrées du cache (B-02). */
    public int capacity() {
        return CEILING;
    }

    private void agrandir() {
        Entry[] anciennes = table;
        table = new Entry[anciennes.length * 2];
        nombre = 0;
        for (Entry caseCourante : anciennes) {
            if (caseCourante != null) {
                store(caseCourante.key, caseCourante.value);
            }
        }
    }

    private static int empreinte(long revision, int row, int themeId, FontSpec font,
            int columns) {
        int empreinte = Long.hashCode(revision);
        empreinte = empreinte * 31 + row;
        empreinte = empreinte * 31 + themeId;
        empreinte = empreinte * 31 + font.hashCode();
        empreinte = empreinte * 31 + columns;
        return empreinte;
    }

    private static boolean correspond(Entry caseCourante, long revision, int row,
            int themeId, FontSpec font, int columns) {
        RowKey key = caseCourante.key;
        return key.revision() == revision && key.row() == row
                && key.themeId() == themeId && key.font().equals(font)
                && key.columns() == columns;
    }

    /**
     * Le stockage interne du cache (feuille des améliorations, C1) :
     * la table et ses compteurs, DÉTACHABLES de l'instance. Le
     * pipeline porte l'INSTANCE (les couches la capturent à leur
     * montage) ; le parc de mises en page parque le STOCKAGE — la
     * bascule de session change le contenu, jamais la référence que
     * les couches portent.
     */
    public static final class Stockage {

        private Entry[] table = new Entry[INITIAL_CAPACITY];
        private int nombre;
        private long compteurAge;

        /** Le stockage vide (C1) — le parc y naît ses caches neufs. */
        public Stockage() {
        }
    }

    /** Détache le stockage interne (C1) : l'instance repart VIDE,
     *  l'état rendu vit sa vie ailleurs (le parc). */
    Stockage detach() {
        Stockage partant = new Stockage();
        partant.table = table;
        partant.nombre = nombre;
        partant.compteurAge = compteurAge;
        table = new Entry[INITIAL_CAPACITY];
        nombre = 0;
        compteurAge = 0;
        return partant;
    }

    /** Adopte un stockage parqué (C1) : l'état de l'instance courante
     *  meurt, remplacé par celui du document qui revient.
     *
     * @param partant le stockage adopté, jamais null */
    void adopter(Stockage partant) {
        table = partant.table;
        nombre = partant.nombre;
        compteurAge = partant.compteurAge;
    }
}
