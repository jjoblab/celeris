package jo.celeris.core.syntax.clike;

import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.LanguageSyntax;
/**
 * Le dialecte Gradle Groovy (ADR-004, priorité P0 — C-07) : les scripts
 * de construction {@code build.gradle} — le socle C-like et le dialecte
 * Groovy (M7.6, H-10), l'identifiant propre au dialecte. Le registre des
 * langages distingue {@code build.gradle} de {@code build.gradle.kts}
 * par l'extension composée la plus longue.
 */
public final class GroovySyntax implements LanguageSyntax {

    /** L'instance unique du dialecte Gradle Groovy. */
    public static final GroovySyntax INSTANCE = new GroovySyntax();

    private GroovySyntax() {
    }

    @Override
    public String id() {
        return "groovy";
    }

    @Override
    public LanguageLexer lexer() {
        return new CLikeLexer(GroovyDialect.INSTANCE);
    }

    @Override
    public LanguageParser parser() {
        return new CLikeParser(GroovyDialect.INSTANCE);
    }
}
