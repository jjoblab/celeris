package jo.celeris.core.language;

import jo.celeris.core.syntax.LanguageSyntax;

/**
 * La fabrique de sources syntaxiques par grammaire (M7.6) : c'est le
 * pont vers la voie de grammaires — le module du bas (core-language)
 * définit la couture, la voie (core-syntax-grammars) l'implémente.
 * Le chargeur de paquets compile ainsi les grammaires au chargement :
 * un paquet dont la grammaire ne compile pas se refuse avec le
 * message précis de la fabrique, jamais à moitié chargé.
 */
@FunctionalInterface
public interface GrammarFactory {

    /**
     * Compile la grammaire JSON d'un langage en source syntaxique.
     *
     * @param languageId  l'identifiant du langage du paquet
     * @param grammarJson le texte JSON de la grammaire
     * @return la source syntaxique compilée
     * @throws IllegalArgumentException si la grammaire est mal formée,
     *                                  avec le message précis du refus
     */
    LanguageSyntax compile(String languageId, String grammarJson);
}
