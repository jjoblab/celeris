package jo.celeris.core.syntax;

/**
 * Le dialecte Gradle KTS (ADR-004, priorité P0) : les scripts de
 * construction Kotlin — le même lexeur et le même parseur que
 * {@link KotlinSyntax}, seul l'identifiant change. Le registre des
 * langages distingue {@code build.gradle.kts} de {@code build.gradle}
 * par l'extension composée la plus longue.
 */
public final class KtsSyntax implements LanguageSyntax {

    /** L'instance unique du dialecte Gradle KTS. */
    public static final KtsSyntax INSTANCE = new KtsSyntax();

    private KtsSyntax() {
    }

    @Override
    public String id() {
        return "kts";
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
