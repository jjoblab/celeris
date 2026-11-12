package jo.celeris.core.language.model;

/**
 * Une incorporation de langage (M7.6) : le code d'un langage dans un
 * autre (script dans HTML, bloc de code en Markdown, SQL dans une
 * chaîne). Le sélecteur nomme la portée TextMate qui porte
 * l'incorporation ; l'identifiant désigne le langage injecté. Le
 * moteur de grammaires empilera les deux à l'exécution.
 *
 * @param scopeSelector     le sélecteur de portée (par exemple
 *                          {@code text.html string})
 * @param injectedLanguageId l'identifiant du langage incorporé
 */
public record LanguageInjection(String scopeSelector, String injectedLanguageId) {

    /** Constructeur canonique : aucun champ vide. */
    public LanguageInjection {
        if (scopeSelector == null || scopeSelector.isEmpty()) {
            throw new IllegalArgumentException("scopeSelector must not be empty");
        }
        if (injectedLanguageId == null || injectedLanguageId.isEmpty()) {
            throw new IllegalArgumentException("injectedLanguageId must not be empty");
        }
    }
}
