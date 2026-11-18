package jo.celeris.core.syntax;

import java.util.List;

/**
 * Un langage analysable par le socle (ADR-004) : son identifiant, son
 * lexeur et son parseur. Les implémentations vivent dans ce module (P0 :
 * Java, Kotlin, KTS, XML, JSON, Groovy) ; la voie de grammaires couvre la
 * longue traîne sans arbre.
 */
public interface LanguageSyntax {

    /** L'identifiant du langage (par exemple {@code "java"}). */
    String id();

    /** Le lexeur ligne à ligne du langage. */
    LanguageLexer lexer();

    /** Le parseur de chunks du langage. */
    LanguageParser parser();

    /**
     * Les mots réservés du langage — la complétion de base les sert
     * dès le premier caractère, sans serveur de langage ni table de
     * l'hôte (le coach de la façade y lit). La liste vide par défaut
     * garde la voie additive : chaque langage P0 y pose les siens,
     * la voie de grammaires y extrait les siens des motifs fixes.
     *
     * @return les mots réservés, distincts, jamais null
     */
    default List<String> reservedWords() {
        return List.of();
    }
}
