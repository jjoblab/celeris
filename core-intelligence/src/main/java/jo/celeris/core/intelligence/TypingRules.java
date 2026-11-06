package jo.celeris.core.intelligence;

import java.util.Map;
import java.util.Set;

/**
 * Les règles de frappe par langage (M5, A-05) : les paires appariées qui
 * se ferment seules et les citations qui se dédoublent. Les règles sont
 * immuables et vivent par identifiant de langage — le même que
 * {@code LanguageSyntax#id()}.
 */
public final class TypingRules {

    /** Les paires communes aux langages impératifs et de données. */
    private static final Map<Character, Character> PAIRES_COMMUNES =
            Map.of('(', ')', '[', ']', '{', '}');

    /** Les citations des langages de la famille C. */
    private static final Set<Character> CITATIONS_C = Set.of('"', '\'');

    /** La citation unique du JSON. */
    private static final Set<Character> CITATIONS_JSON = Set.of('"');

    private final Map<Character, Character> paires;

    private final Set<Character> citations;

    private TypingRules(Map<Character, Character> paires, Set<Character> citations) {
        this.paires = paires;
        this.citations = citations;
    }

    /**
     * Les règles d'un langage par identifiant. Les identifiants inconnus
     * retombent sur les règles par défaut — la longue traîne (C-16) frappe
     * aussi bien.
     *
     * @param id l'identifiant du langage (par exemple {@code "json"})
     * @return ses règles
     */
    public static TypingRules langage(String id) {
        if ("json".equals(id)) {
            return new TypingRules(PAIRES_COMMUNES, CITATIONS_JSON);
        }
        return new TypingRules(PAIRES_COMMUNES, CITATIONS_C);
    }

    /**
     * Les règles par défaut : paires communes, citations de la famille C.
     *
     * @return les règles
     */
    public static TypingRules parDefaut() {
        return new TypingRules(PAIRES_COMMUNES, CITATIONS_C);
    }

    /**
     * Vrai si le caractère ouvre une paire appariée.
     *
     * @param c le caractère frappé
     * @return son statut d'ouvrant
     */
    public boolean estOuvrant(char c) {
        return paires.containsKey(c);
    }

    /**
     * Vrai si le caractère ferme une paire appariée.
     *
     * @param c le caractère frappé
     * @return son statut de fermant
     */
    public boolean estFermant(char c) {
        return paires.containsValue(c);
    }

    /**
     * Le fermant apparié d'un ouvrant.
     *
     * @param ouvrant l'ouvrant
     * @return son fermant
     * @throws IllegalArgumentException si {@code ouvrant} n'ouvre rien
     */
    public char fermantDe(char ouvrant) {
        Character fermant = paires.get(ouvrant);
        if (fermant == null) {
            throw new IllegalArgumentException("not an opener: " + ouvrant);
        }
        return fermant;
    }

    /**
     * Vrai si le caractère est une citation qui se dédouble.
     *
     * @param c le caractère frappé
     * @return son statut de citation
     */
    public boolean estCitation(char c) {
        return citations.contains(c);
    }
}
