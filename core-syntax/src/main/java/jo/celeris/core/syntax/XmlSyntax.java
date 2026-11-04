package jo.celeris.core.syntax;

/**
 * Le langage XML (ADR-004, priorité P0) : lexeur ligne à ligne à états
 * transportés — commentaires, sections CDATA et balises à cheval sur les
 * lignes — et parseur d'éléments tolérant. L'instance est unique et sans
 * état ; le lexeur, lui, est à usage de fil unique (ADR-007).
 */
public final class XmlSyntax implements LanguageSyntax {

    /** L'instance unique du langage XML. */
    public static final XmlSyntax INSTANCE = new XmlSyntax();

    private XmlSyntax() {
    }

    @Override
    public String id() {
        return "xml";
    }

    @Override
    public LanguageLexer lexer() {
        return new XmlLexeur();
    }

    @Override
    public LanguageParser parser() {
        return new XmlParseur();
    }
}
