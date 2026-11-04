package jo.celeris.core.syntax;

/**
 * Le langage JSON (ADR-004, priorité P0) : lexeur sans état transporté —
 * une chaîne ne traverse jamais les lignes — et parseur de valeurs
 * tolérant. L'instance est unique et sans état.
 */
public final class JsonSyntax implements LanguageSyntax {

    /** L'instance unique du langage JSON. */
    public static final JsonSyntax INSTANCE = new JsonSyntax();

    private JsonSyntax() {
    }

    @Override
    public String id() {
        return "json";
    }

    @Override
    public LanguageLexer lexer() {
        return new JsonLexeur();
    }

    @Override
    public LanguageParser parser() {
        return new JsonParseur();
    }
}
