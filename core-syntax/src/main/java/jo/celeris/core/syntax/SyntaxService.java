package jo.celeris.core.syntax;

import jo.celeris.core.text.Document;

/**
 * Service d'analyse d'un langage (ADR-004) : la voie d'entrée unique du
 * socle. La première analyse parcourt le document ; les suivantes ne
 * rejouent que l'édition — lexicographie bornée par la reconvergence des
 * états (PIEGE-19), ré-analyse des seuls chunks propriétaires des lignes
 * touchées.
 */
public final class SyntaxService {

    private final LanguageSyntax language;
    private final IncrementalLexer lexer = new IncrementalLexer();
    private final IncrementalParser parser = new IncrementalParser();

    private SyntaxService(LanguageSyntax language) {
        this.language = language;
    }

    /**
     * Le service d'un langage.
     *
     * @param language le langage (lexeur et parseur)
     * @return le service prêt à analyser
     */
    public static SyntaxService of(LanguageSyntax language) {
        return new SyntaxService(language);
    }

    /**
     * Analyse complète d'un instantané.
     *
     * @param doc le document à analyser
     * @return l'analyse versionnée par la révision du document
     */
    public Analysis analyseFull(Document doc) {
        LexResult lex = lexer.lexFull(doc, language.lexer());
        SyntaxTree tree = parser.parseFull(doc, lex.lex(), language.parser(),
                doc.revision());
        return new Analysis(doc.revision(), lex.lex(), tree);
    }

    /**
     * Analyse incrémentale après une édition.
     *
     * @param doc      le document après l'édition
     * @param previous l'analyse du document avant l'édition
     * @param site     le site de l'édition en lignes
     * @return l'analyse versionnée, sous-arbres non touchés conservés
     */
    public Analysis analyseIncremental(Document doc, Analysis previous, EditSite site) {
        LexResult diff = lexer.lexIncremental(doc, language.lexer(),
                previous.lex(), site);
        SyntaxTree tree = parser.parseIncremental(doc, diff.lex(), language.parser(),
                doc.revision(), previous.tree(), diff, site);
        return new Analysis(doc.revision(), diff.lex(), tree);
    }
}
