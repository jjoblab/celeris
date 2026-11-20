package jo.celeris.core.syntax;

/**
 * Un problème de syntaxe localisé (D-02) : plage source, message et
 * SÉVÉRITÉ (D-06, l'analyse croisée) — l'erreur ne se distingue plus
 * de l'avertissement par la seule prose du message.
 *
 * <p>La sévérité par défaut est {@link Severity#ERROR} : le
 * constructeur compact à trois champs (plage, message) fabrique une
 * erreur — le comportement historique de la gouttière (D-02) et de
 * tous les producteurs existants. Les diagnostics importés (lint du
 * build, LSP) posent LEUR sévérité : la vue colore le squiggle et
 * le marqueur de gouttière en conséquence (D-05).</p>
 *
 * @param startOffset début absolu de la plage en erreur
 * @param length      longueur de la plage
 * @param message     la description du problème
 * @param severity    la sévérité du problème
 */
public record SyntaxIssue(int startOffset, int length, String message,
        Severity severity) {

    /**
     * Le constructeur compact : une ERREUR (la sévérité historique —
     * la gouttière et les bancs existants ne changent pas).
     *
     * @param startOffset début absolu de la plage en erreur
     * @param length      longueur de la plage
     * @param message     la description du problème
     */
    public SyntaxIssue(int startOffset, int length, String message) {
        this(startOffset, length, message, Severity.ERROR);
    }

    /** La sévérité d'un diagnostic (D-06) : l'erreur bloque, l'avis
     *  avertit, l'info informe — la teinte suit. */
    public enum Severity {

        /** Une erreur : la structure est cassée, la compilation
         *  échouera. */
        ERROR,

        /** Un avertissement : le code vit, mais il sent le souci. */
        WARNING,

        /** Une information : style, suggestion, convention. */
        INFO
    }
}
