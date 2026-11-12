package jo.celeris.core.syntax.clike;

import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.LanguageSyntax;
/**
 * Le langage Kotlin (ADR-004, priorité P0) : lexeur ligne à ligne à états
 * transportés — chaînes brutes, commentaires de bloc nichés — et parseur
 * tolérant ancré par chunks, l'un et l'autre portés par le socle C-like
 * (M7.6, H-10) au dialecte Kotlin. L'instance est unique et sans état ;
 * le lexeur, lui, est à usage de fil unique (ADR-007).
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
        return new CLikeLexer(KotlinDialect.INSTANCE);
    }

    @Override
    public LanguageParser parser() {
        return new CLikeParser(KotlinDialect.INSTANCE);
    }
}
