package jo.celeris.core.syntax;

/**
 * Le langage Kotlin (ADR-004, priorité P0) : lexeur ligne à ligne à états
 * transportés — chaînes brutes, commentaires de bloc nichés — et parseur
 * tolérant ancré par chunks. L'instance est unique et sans état ; le
 * lexeur, lui, est à usage de fil unique (ADR-007).
 */
public final class KotlinSyntax implements LanguageSyntax {

    /** L'instance unique du langage Kotlin. */
    public static final KotlinSyntax INSTANCE = new KotlinSyntax();

    private KotlinSyntax() {
    }

    @Override
    public String id() {
        return "kotlin";
    }

    @Override
    public LanguageLexer lexer() {
        return new KotlinLexeur();
    }

    @Override
    public LanguageParser parser() {
        return new KotlinParseur();
    }
}
