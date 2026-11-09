package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Les jetons sémantiques du protocole (B-02) : le delta encodé du
 * serveur — chaque jeton relatif au précédent — décodé en positions
 * absolues. Le décodage est une fonction pure de la charge « data » :
 * aucun autre flux n'y entre (PIEGE-18 — les diagnostics et les jetons
 * sont deux sources indépendantes, jamais chaînées).
 */
public final class JetonsSemantiques {

    private final @Nullable String identifiantResultat;
    private final List<JetonSemantique> jetons;

    private JetonsSemantiques(@Nullable String identifiantResultat,
                              List<JetonSemantique> jetons) {
        this.identifiantResultat = identifiantResultat;
        this.jetons = List.copyOf(jetons);
    }

    /**
     * Décode la réponse du serveur.
     *
     * @param json la valeur « result » de la requête de jetons
     * @return les jetons en positions absolues, vide si la réponse est vide
     * @throws IllegalArgumentException si les données ne sont pas une
     *         suite de quintuples
     */
    public static JetonsSemantiques de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return de(null, List.of());
        }
        String identifiant = objet.membreTexte("resultId");
        List<Integer> donnees = new ArrayList<>();
        if (objet.membre("data") instanceof JsonTableau tableau) {
            for (int i = 0; i < tableau.taille(); i++) {
                donnees.add(entier(tableau, i));
            }
        }
        return de(identifiant, donnees);
    }

    /**
     * Décode les jetons de leur charge encodée — la voie typée de la
     * frontière protocolaire (ADR-015) : la bibliothèque rend la liste
     * d'entiers telle quelle, le décodage relatif-à-absolu reste au
     * domaine.
     *
     * @param identifiant l'identifiant de résultat du serveur, ou null
     * @param donnees      la charge « data » du protocole, quintuples
     *                    relatifs
     * @return les jetons en positions absolues, vide sans données
     * @throws IllegalArgumentException si les données ne sont pas une
     *         suite de quintuples
     */
    public static JetonsSemantiques de(@Nullable String identifiant,
            List<Integer> donnees) {
        if (donnees.size() % 5 != 0) {
            throw new IllegalArgumentException(
                    "semantic token data must be a list of quintuples");
        }
        List<JetonSemantique> jetons = new ArrayList<>();
        int ligne = 0;
        int colonne = 0;
        for (int i = 0; i < donnees.size(); i += 5) {
            ligne += donnees.get(i);
            colonne = donnees.get(i) != 0 ? donnees.get(i + 1)
                    : colonne + donnees.get(i + 1);
            jetons.add(new JetonSemantique(ligne, colonne, donnees.get(i + 2),
                    donnees.get(i + 3), donnees.get(i + 4)));
        }
        return new JetonsSemantiques(identifiant, jetons);
    }

    private static int entier(JsonTableau donnees, int indice) {
        return (int) ((JsonNombre) donnees.element(indice)).decimal();
    }

    /** L'identifiant de résultat du serveur, pour un décodage incrémental — ou null. */
    public @Nullable String identifiantResultat() {
        return identifiantResultat;
    }

    /** Les jetons en positions absolues, dans l'ordre du flux. */
    public List<JetonSemantique> jetons() {
        return jetons;
    }
}
