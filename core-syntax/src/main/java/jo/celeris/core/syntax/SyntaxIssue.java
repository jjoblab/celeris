package jo.celeris.core.syntax;

/**
 * Un problème de syntaxe localisé (D-02) : plage source et message.
 *
 * @param startOffset début absolu de la plage en erreur
 * @param length      longueur de la plage
 * @param message     la description du problème
 */
public record SyntaxIssue(int startOffset, int length, String message) {
}
