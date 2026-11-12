package jo.celeris.core.syntax.clike;

import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.LanguageSyntax;
/**
 * Le dialecte Gradle KTS (ADR-004, priorité P0) : les scripts de
 * construction Kotlin — le même socle C-like et le même dialecte
 * Kotlin que {@link KotlinSyntax}, seul l'identifiant change. Le
 * registre des langages distingue {@code build.gradle.kts} de
 * {@code build.gradle} par l'extension composée la plus longue.
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
        return new CLikeLexer(KotlinDialect.INSTANCE);
    }

    @Override
    public LanguageParser parser() {
        return new CLikeParser(KotlinDialect.INSTANCE);
    }
}
