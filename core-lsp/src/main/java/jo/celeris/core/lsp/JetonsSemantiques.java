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
            return new JetonsSemantiques(null, List.of());
        }
        String identifiant = objet.membreTexte("resultId");
        List<JetonSemantique> jetons = new ArrayList<>();
        if (objet.membre("data") instanceof JsonTableau donnees) {
            if (donnees.taille() % 5 != 0) {
                throw new IllegalArgumentException(
                        "semantic token data must be a list of quintuples");
            }
            int ligne = 0;
            int colonne = 0;
            for (int i = 0; i < donnees.taille(); i += 5) {
                ligne += entier(donnees, i);
                colonne = ligneVientDeBouger(donnees, i)
                        ? entier(donnees, i + 1) : colonne + entier(donnees, i + 1);
                jetons.add(new JetonSemantique(ligne, colonne,
                        entier(donnees, i + 2), entier(donnees, i + 3),
                        entier(donnees, i + 4)));
            }
        }
        return new JetonsSemantiques(identifiant, jetons);
    }

    private static boolean ligneVientDeBouger(JsonTableau donnees, int indice) {
        return entier(donnees, indice) != 0;
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
