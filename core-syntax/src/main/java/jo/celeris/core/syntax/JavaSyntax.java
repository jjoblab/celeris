package jo.celeris.core.syntax;

/**
 * Le langage Java (ADR-004, priorité P0) : lexeur ligne à ligne à états
 * transportés — commentaires de bloc et de documentation, textes en bloc —
 * et parseur tolérant ancré par chunks. L'instance est unique et sans état ;
 * le lexeur, lui, est à usage de fil unique (ADR-007) : le moteur en crée
 * un par passe.
 */
public final class JavaSyntax implements LanguageSyntax {

    /** L'instance unique du langage Java. */
    public static final JavaSyntax INSTANCE = new JavaSyntax();

    private JavaSyntax() {
    }

    @Override
    public String id() {
        return "java";
    }

    @Override
    public LanguageLexer lexer() {
        return new JavaLexeur();
    }

    @Override
    public LanguageParser parser() {
        return new JavaParseur();
    }
}
