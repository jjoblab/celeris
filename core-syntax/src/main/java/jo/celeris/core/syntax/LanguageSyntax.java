package jo.celeris.core.syntax;

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
}
