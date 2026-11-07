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

    private static final int CAPACITE_INITIALE = 64;
    private static final int QUART = 4;

    private static final class Entree {
        private final RowKey cle;
        private RowLayout valeur;

        Entree(RowKey cle, RowLayout valeur) {
            this.cle = cle;
            this.valeur = valeur;
        }
    }

    private Entree[] table = new Entree[CAPACITE_INITIALE];
    private int nombre;

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
        Entree[] cases = table;
        int indice = empreinte(revision, row, themeId, font, columns) & (cases.length - 1);
        while (cases[indice] != null) {
            Entree caseCourante = cases[indice];
            if (correspond(caseCourante, revision, row, themeId, font, columns)) {
                return caseCourante.valeur;
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
            if (table[indice].cle.equals(key)) {
                table[indice].valeur = layout;
                return;
            }
            indice = (indice + 1) & (table.length - 1);
        }
        table[indice] = new Entree(key, layout);
        nombre++;
    }

    /**
     * Retire toutes les mises en page de la révision donnée.
     *
     * @param revision la révision périmée
     */
    public void invalidate(long revision) {
        Entree[] restantes = new Entree[nombre];
        int gardees = 0;
        for (Entree caseCourante : table) {
            if (caseCourante != null && caseCourante.cle.revision() != revision) {
                restantes[gardees++] = caseCourante;
            }
        }
        table = new Entree[CAPACITE_INITIALE];
        nombre = 0;
        for (int i = 0; i < gardees; i++) {
            store(restantes[i].cle, restantes[i].valeur);
        }
    }

    /**
     * Retire TOUTES les mises en page, toutes révisions comprises — le
     * changement de source qui ne signe aucune composante de clé
     * (changement de langage, changement de session) ne peut pas servir
     * une mise en page périmée (PIEGE-02 par construction).
     */
    public void clear() {
        table = new Entree[CAPACITE_INITIALE];
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
        for (Entree caseCourante : table) {
            if (caseCourante != null && caseCourante.cle.revision() == revision) {
                caseCourante.valeur = aJour.apply(caseCourante.valeur);
            }
        }
    }

    /** Le nombre de mises en page en cache. */
    public int size() {
        return nombre;
    }

    private void agrandir() {
        Entree[] anciennes = table;
        table = new Entree[anciennes.length * 2];
        nombre = 0;
        for (Entree caseCourante : anciennes) {
            if (caseCourante != null) {
                store(caseCourante.cle, caseCourante.valeur);
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

    private static boolean correspond(Entree caseCourante, long revision, int row,
            int themeId, FontSpec font, int columns) {
        RowKey cle = caseCourante.cle;
        return cle.revision() == revision && cle.row() == row
                && cle.themeId() == themeId && cle.font().equals(font)
                && cle.columns() == columns;
    }
}
