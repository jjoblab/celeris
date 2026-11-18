package jo.celeris.core.syntax.json;

import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.LanguageSyntax;
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

    /** Les littéraux réservés du JSON — la complétion de base les
     *  sert ({@code reservedWords()} de {@link LanguageSyntax}). */
    @Override
    public java.util.List<String> reservedWords() {
        return java.util.List.of("true", "false", "null");
    }
}
