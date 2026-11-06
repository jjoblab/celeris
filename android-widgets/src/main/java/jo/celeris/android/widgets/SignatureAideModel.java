package jo.celeris.android.widgets;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Le modèle d'aide aux paramètres (B-07) : la signature active montre
 * le paramètre courant SOULIGNÉ — la plage de caractères du paramètre
 * dans le libellé, cherchée DANS LA ZONE DES PARAMÈTRES (entre les
 * parenthèses), dans l'ordre : les doublons tombent sur le bon, le nom
 * de la méthode jamais confondu avec un paramètre. L'index reçu est
 * BORNÉ à l'arité — le serveur généreux au-delà ne casse pas le
 * surlignage. Pur et testable sans SDK : la popup le lit, le peintre
 * souligne ; le placement au-dessus du curseur vit chez PopupPlacer
 * (D-10, livré M5).
 */
public final class SignatureAideModel {

    /** Une plage de caractères dans le libellé : début inclus, fin exclue. */
    public record Plage(int debut, int fin) {
    }

    private static final int AUCUN = -1;

    private final String libelle;
    private final List<String> libellesParametres;
    private final int parametreCourant;

    private SignatureAideModel(String libelle, List<String> libellesParametres,
            int parametreCourant) {
        this.libelle = libelle;
        this.libellesParametres = libellesParametres;
        this.parametreCourant = parametreCourant;
    }

    /**
     * Construit le modèle d'une signature.
     *
     * @param libelle            le libellé de la signature — « ajouter(int a, int b) »
     * @param libellesParametres les libellés des paramètres, dans l'ordre
     * @param parametreCourant   l'index du paramètre en cours de frappe,
     *                           -1 pour aucun ; l'index est BORNÉ à
     *                           l'arité (au-delà : le dernier paramètre)
     * @return le modèle
     */
    public static SignatureAideModel of(String libelle, List<String> libellesParametres,
            int parametreCourant) {
        int borne = parametreCourant;
        if (borne >= libellesParametres.size()) {
            borne = libellesParametres.size() - 1;
        }
        if (borne < 0) {
            borne = AUCUN;
        }
        return new SignatureAideModel(libelle, List.copyOf(libellesParametres), borne);
    }

    /** Le libellé de la signature. */
    public String libelle() {
        return libelle;
    }

    /** L'index du paramètre courant, borné à l'arité — -1 s'il n'y en a pas. */
    public int parametreCourant() {
        return parametreCourant;
    }

    /**
     * La plage du paramètre courant dans le libellé — ce que le peintre
     * souligne. La recherche vit ENTRE LES PARENTHÈSES (le nom de la
     * méthode n'est jamais un paramètre), séquentielle : chaque
     * paramètre se cherche après la fin du précédent, les doublons
     * tombent sur le bon. Le paramètre introuvable ne donne aucune
     * plage — jamais inventée.
     *
     * @return la plage du paramètre courant, ou null s'il n'y en a pas
     */
    public @Nullable Plage plageParametreCourant() {
        if (parametreCourant == AUCUN) {
            return null;
        }
        int ouverture = libelle.indexOf('(');
        int fermeture = libelle.lastIndexOf(')');
        int depuis = ouverture < 0 ? 0 : ouverture + 1;
        int borne = fermeture < 0 ? libelle.length() : fermeture;
        for (int i = 0; i < libellesParametres.size(); i++) {
            String nom = libellesParametres.get(i);
            int trouve = libelle.indexOf(nom, depuis);
            if (trouve < 0 || trouve + nom.length() > borne) {
                continue;
            }
            int fin = trouve + nom.length();
            if (i == parametreCourant) {
                return new Plage(trouve, fin);
            }
            depuis = fin;
        }
        return null;
    }
}
