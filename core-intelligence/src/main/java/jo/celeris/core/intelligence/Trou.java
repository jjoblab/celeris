package jo.celeris.core.intelligence;

/**
 * Un trou de gabarit (M5, A-22) : l'occurrence d'une variable dans le
 * rendu, ses bornes et son nom. Les occurrences d'une même variable
 * sont liées : l'édition de l'une se propage aux autres.
 *
 * @param debut   le début du trou dans le rendu, inclusif
 * @param fin     la fin du trou dans le rendu, exclusive
 * @param variable le nom de la variable
 */
public record Trou(int debut, int fin, String variable) {

    /**
     * Constructeur canonique : bornes ordonnées, nom jamais vide.
     */
    public Trou {
        if (debut < 0) {
            throw new IllegalArgumentException("debut must be >= 0: " + debut);
        }
        if (fin < debut) {
            throw new IllegalArgumentException("fin must be >= debut: " + fin);
        }
        if (variable == null || variable.isBlank()) {
            throw new IllegalArgumentException("variable must not be blank");
        }
    }

    /**
     * La longueur du trou.
     *
     * @return {@code fin - debut}
     */
    public int longueur() {
        return fin - debut;
    }
}
