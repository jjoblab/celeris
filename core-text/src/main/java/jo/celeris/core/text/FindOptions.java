package jo.celeris.core.text;

/**
 * Options des opérations de recherche/remplacement — portage du dépôt de
 * départ (registre de portage), en enregistrement immuable : les fabriques
 * {@code with...} dérivent une nouvelle options d'une existante.
 *
 * @param caseSensitive vrai pour respecter la casse (faux par défaut)
 * @param wholeWord     vrai pour exiger les frontières de mot
 * @param regex         vrai pour interpréter la requête comme une expression
 *                      régulière
 */
public record FindOptions(boolean caseSensitive, boolean wholeWord, boolean regex) {

    /** Options par défaut : casse ignorée, mot quelconque, requête littérale. */
    public FindOptions() {
        this(false, false, false);
    }

    /**
     * Dérive les options avec la sensibilité à la casse donnée.
     *
     * @param cs vrai pour respecter la casse
     * @return les options dérivées
     */
    public FindOptions withCaseSensitive(boolean cs) {
        return new FindOptions(cs, wholeWord, regex);
    }

    /**
     * Dérive les options avec l'exigence de mot entier donnée.
     *
     * @param ww vrai pour exiger les frontières de mot
     * @return les options dérivées
     */
    public FindOptions withWholeWord(boolean ww) {
        return new FindOptions(caseSensitive, ww, regex);
    }

    /**
     * Dérive les options avec le mode regex donné.
     *
     * @param rx vrai pour interpréter la requête comme une expression régulière
     * @return les options dérivées
     */
    public FindOptions withRegex(boolean rx) {
        return new FindOptions(caseSensitive, wholeWord, rx);
    }
}
