package jo.celeris.core.syntax.grammars;

import java.util.List;

/**
 * Référence à une liste de règles du référentiel (format TextMate,
 * ADR-004) : l'inclusion {@code #cle} devient une référence à la liste
 * de la clé — créée avant sa propre résolution, les inclusions
 * cycliques (un tableau de valeurs qui contient des tableaux) se
 * référencent sans fin d'aplatissement. La lexicographie parcourt la
 * référence au fil de l'énumération, chaque liste distincte d'un
 * contexte ne se considère qu'une fois.
 */
final class RegleListe implements Regle {

    private List<Regle> regles = List.of();

    /** Les règles référencées — vide pendant la construction du chargement. */
    List<Regle> regles() {
        return regles;
    }

    /** Fixe les règles référencées — appelé une fois, au chargement. */
    void fixer(List<Regle> regles) {
        this.regles = regles;
    }
}
