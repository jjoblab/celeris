package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Un gabarit dynamique (M5, A-22) : le rendu — le corps où chaque
 * {@code ${variable}} a laissé sa valeur par défaut, vide sans
 * deux-points — et ses trous, dans l'ordre du rendu : l'ordre de
 * tabulation. Les occurrences d'une même variable sont liées.
 *
 * @param rendu le corps rendu, défauts en place
 * @param trous les occurrences, dans l'ordre du rendu
 */
public record Gabarit(String rendu, List<Trou> trous) {

    /**
     * Constructeur canonique : composantes figées, jamais nulles.
     */
    public Gabarit {
        java.util.Objects.requireNonNull(rendu, "rendu");
        java.util.Objects.requireNonNull(trous, "trous");
        trous = List.copyOf(trous);
    }

    /**
     * Analyse le corps d'un gabarit : chaque {@code ${nom}} ou
     * {@code ${nom:defaut}} devient un trou portant le défaut, les
     * autres segments restent littéraux.
     *
     * @param corps le corps du gabarit
     * @return le gabarit rendu et analysé
     * @throws IllegalArgumentException si une balise n'est pas fermée
     */
    public static Gabarit of(String corps) {
        StringBuilder rendu = new StringBuilder();
        List<Trou> trous = new ArrayList<>();
        int i = 0;
        while (i < corps.length()) {
            int ouverture = corps.indexOf("${", i);
            if (ouverture < 0) {
                rendu.append(corps, i, corps.length());
                break;
            }
            int fermeture = corps.indexOf('}', ouverture + 2);
            if (fermeture < 0) {
                throw new IllegalArgumentException("unclosed placeholder at " + ouverture);
            }
            rendu.append(corps, i, ouverture);
            String nom = corps.substring(ouverture + 2, fermeture);
            int deuxPoints = nom.indexOf(':');
            String variable = deuxPoints >= 0 ? nom.substring(0, deuxPoints) : nom;
            String defaut = deuxPoints >= 0 ? nom.substring(deuxPoints + 1) : "";
            if (variable.isBlank()) {
                throw new IllegalArgumentException("blank variable at " + ouverture);
            }
            int debut = rendu.length();
            rendu.append(defaut);
            trous.add(new Trou(debut, debut + defaut.length(), variable));
            i = fermeture + 1;
        }
        return new Gabarit(rendu.toString(), trous);
    }

    /**
     * Les variables distinctes, dans l'ordre de leur première occurrence
     * — l'ordre de tabulation des variables.
     *
     * @return les noms distincts
     */
    public List<String> variables() {
        Set<String> noms = new LinkedHashSet<>();
        for (Trou trou : trous) {
            noms.add(trou.variable());
        }
        return List.copyOf(noms);
    }

    /**
     * Les occurrences d'une variable, liées entre elles.
     *
     * @param variable le nom de la variable
     * @return ses occurrences, dans l'ordre du rendu
     */
    public List<Trou> occurrencesDe(String variable) {
        List<Trou> occurrences = new ArrayList<>();
        for (Trou trou : trous) {
            if (trou.variable().equals(variable)) {
                occurrences.add(trou);
            }
        }
        return occurrences;
    }
}
