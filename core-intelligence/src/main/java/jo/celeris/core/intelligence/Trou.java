package jo.celeris.core.intelligence;

import java.util.List;

/**
 * Un trou de gabarit (M5, A-22) : l'occurrence d'une variable dans le
 * rendu, ses bornes et son nom. Les occurrences d'une même variable
 * sont liées : l'édition de l'une se propage aux autres (les miroirs,
 * {@link Gabarits#miroirs}).
 *
 * <p>Le trou riche (chantier C7) porte ses choix — la liste fermée
 * d'un trou à choix ({@code ${fruit|pomme,poire,banane|}}), vide pour
 * un trou libre. Le choix courant se lit dans le rendu, le suivant se
 * cycle ({@link #choixSuivant(String)}) — l'hôte présente la liste ou
 * le cycle, le moteur reste pur.</p>
 *
 * @param debut    le début du trou dans le rendu, inclusif
 * @param fin      la fin du trou dans le rendu, exclusive
 * @param variable le nom de la variable
 * @param choix    les choix du trou à choix, vides pour un trou libre
 */
public record Trou(int debut, int fin, String variable, List<String> choix) {

    /**
     * Constructeur de commodité d'un trou libre — sans choix.
     *
     * @param debut    le début du trou dans le rendu, inclusif
     * @param fin      la fin du trou dans le rendu, exclusive
     * @param variable le nom de la variable
     */
    public Trou(int debut, int fin, String variable) {
        this(debut, fin, variable, List.of());
    }

    /**
     * Constructeur canonique : bornes ordonnées, nom jamais vide,
     * choix figés — jamais nuls.
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
        if (choix == null) {
            throw new IllegalArgumentException("choix must not be null");
        }
        choix = List.copyOf(choix);
    }

    /**
     * La longueur du trou.
     *
     * @return {@code fin - debut}
     */
    public int longueur() {
        return fin - debut;
    }

    /**
     * Vrai quand le trou est un trou à choix — sa liste est fermée.
     *
     * @return vrai quand le trou porte des choix
     */
    public boolean aDesChoix() {
        return !choix.isEmpty();
    }

    /**
     * Le choix suivant d'un trou à choix : celui qui suit le courant
     * dans la liste, le premier revient après le dernier — le cycle
     * est tactile, jamais bloqué. Le courant inconnu rend le premier.
     * Un trou libre rend le courant tel quel.
     *
     * @param courant le choix courant — ce que le rendu montre
     * @return le choix suivant, ou le courant pour un trou libre
     */
    public String choixSuivant(String courant) {
        if (choix.isEmpty()) {
            return courant;
        }
        int index = choix.indexOf(courant);
        return choix.get((index + 1) % choix.size());
    }
}
