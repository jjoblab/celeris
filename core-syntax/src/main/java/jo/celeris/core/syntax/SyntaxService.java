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
     * Analyse par fenêtre (G-01) : lexicographie complète — les états se
     * cascadent de la première ligne, la fenêtre seule ne suffit pas —
     * puis construction des seuls chunks coupés par la fenêtre visible ;
     * les autres attendent leur complétion.
     *
     * @param doc           le document à analyser
     * @param premiereLigne la première ligne visible
     * @param derniereLigne la dernière ligne visible
     * @param jeton         le jeton d'annulation de l'ouverture
     * @return l'analyse versionnée, partielle hors fenêtre
     * @throws AnalyseAnnulee si l'ouverture est annulée avant la fenêtre
     */
    public Analysis analyseFenetre(Document doc, int premiereLigne, int derniereLigne,
                                   JetonAnnulation jeton) {
        LexResult lex = lexer.lexFull(doc, language.lexer(), jeton);
        SyntaxTree tree = parser.parseFenetre(doc, lex.lex(), language.parser(),
                doc.revision(), premiereLigne, derniereLigne, jeton);
        return new Analysis(doc.revision(), lex.lex(), tree);
    }

    /**
     * Complétion d'une analyse partielle : construit au plus
     * {@code maxChunks} chunks en attente, dans l'ordre du document —
     * les sous-arbres déjà construits gardent leur identité.
     *
     * @param doc       le document de l'analyse partielle
     * @param partielle l'analyse partielle à compléter
     * @param jeton     le jeton d'annulation de la complétion
     * @param maxChunks le nombre maximal de chunks à construire
     * @return l'analyse complétée d'autant de chunks, ou la même si rien à construire
     * @throws AnalyseAnnulee si la complétion est annulée
     */
    public Analysis completer(Document doc, Analysis partielle, JetonAnnulation jeton,
                              int maxChunks) {
        if (partielle.estComplet() || maxChunks <= 0) {
            return partielle;
        }
        SyntaxTree tree = parser.completer(doc, partielle.lex(), language.parser(),
                doc.revision(), partielle.tree(), jeton, maxChunks);
        return new Analysis(doc.revision(), partielle.lex(), tree);
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
