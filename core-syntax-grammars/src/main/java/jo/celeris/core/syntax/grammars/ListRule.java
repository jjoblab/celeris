package jo.celeris.core.syntax.grammars;

import java.util.List;

/**
 * Une référence d'inclusion TextMate : la liste visée — une clé du
 * référentiel, la racine ({@code $self}, {@code $base}) ou une portée
 * externe liée au chargement. La lexicographie suit la référence au fil
 * de l'énumération, chaque liste distincte d'un contexte ne se
 * considère qu'une fois : les inclusions cycliques (une clé qui
 * s'inclut, directement ou par ricochet) se parcourent sans fin
 * d'aplatissement.
 */
final class ListRule implements Rule {

    private List<Rule> regles = List.of();

    /** Les règles référencées — vide pendant la construction du chargement. */
    List<Rule> regles() {
        return regles;
    }

    /** Fixe les règles référencées — appelé une fois, au chargement. */
    void fixer(List<Rule> regles) {
        this.regles = regles;
    }
}
