package jo.celeris.core.intelligence;

import java.util.LinkedHashMap;
import java.util.Map;

import jo.celeris.core.language.LanguageConfiguration;

/**
 * Les règles de frappe d'un langage (M5, A-05 ; M7.6 : pilotées par la
 * configuration) : les paires appariées qui se ferment seules et les
 * citations qui se dédoublent — les paires déclarées du paquet, celles
 * des crochets par convention de VS Code, une paire ouvrante égale à sa
 * fermante étant une citation. Plus aucun aiguillage par identifiant :
 * la longue traîne frappe aussi bien que les langages dédiés.
 */
public final class TypingRules {

    private final Map<Character, Character> paires;

    private final Map<Character, Character> citations;

    private TypingRules(Map<Character, Character> paires,
            Map<Character, Character> citations) {
        this.paires = paires;
        this.citations = citations;
    }

    /**
     * Les règles d'une configuration de langage : ses paires déclarées
     * (ou celles dérivées de ses crochets), la citation dans les
     * paires ouvrant sur elles-mêmes.
     *
     * @param configuration la configuration du langage
     * @return ses règles
     */
    public static TypingRules of(LanguageConfiguration configuration) {
        Map<Character, Character> paires = new LinkedHashMap<>();
        Map<Character, Character> citations = new LinkedHashMap<>();
        for (LanguageConfiguration.AutoClosePair paire
                : configuration.effectiveAutoClosingPairs()) {
            if (paire.open() == paire.close()) {
                citations.put(paire.open(), paire.close());
            } else {
                paires.put(paire.open(), paire.close());
            }
        }
        return new TypingRules(Map.copyOf(paires), Map.copyOf(citations));
    }

    /**
     * Les règles génériques des langages sans configuration : paires
     * communes, citations de la famille C — la configuration générique
     * de la plateforme, exprimée ici pour les hôtes qui n'ont pas de
     * langage.
     *
     * @return les règles génériques
     */
    public static TypingRules generic() {
        return of(LanguageConfiguration.generic());
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
        return citations.containsKey(c);
    }
}
