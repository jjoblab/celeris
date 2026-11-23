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

    /** Le langage servi — le moteur l'expose, la vue y reconnaît
     *  le moteur de la langue courante (parquage par document). */
    public LanguageSyntax language() {
        return language;
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
     * Analyse par fenêtre (G-01, la suite — le retard résiduel du
     * prompt du 2026-11-23) : lexicographie BORNÉE à la fenêtre et sa
     * marge ({@link IncrementalLexer#lexFenetre}) — les états se
     * cascadent depuis la première ligne lue de la fenêtre, les lignes
     * au-delà vivent VIDES au lexique — puis construction des seuls
     * chunks coupés par la fenêtre visible ; les autres attendent leur
     * complétion (qui RELEXE le reste par lots, puis bâtit leurs
     * chunks : jamais un chunk ne se bâtit sur des jetons fantômes).
     *
     * @param doc           le document à analyser
     * @param premiereLigne la première ligne visible
     * @param derniereLigne la dernière ligne visible
     * @param token         le jeton d'annulation de l'ouverture
     * @return l'analyse versionnée, partielle hors fenêtre
     * @throws AnalysisCancelled si l'ouverture est annulée avant la fenêtre
     */
    public Analysis analyzeWindow(Document doc, int premiereLigne, int derniereLigne,
                                   TokenCancellation token) {
        LexResult lex = lexer.lexFenetre(doc, language.lexer(),
                premiereLigne, derniereLigne, MARGE_FENETRE, token);
        SyntaxTree tree = parser.parseFenetre(doc, lex.lex(), language.parser(),
                doc.revision(), premiereLigne, derniereLigne, token);
        return new Analysis(doc.revision(), lex.lex(), tree);
    }

    /**
     * Complétion d'une analyse partielle : relit d'abord par lots le
     * lexique des lignes encore vides (le préfixe depuis zéro, le
     * suffixe depuis la frontière d'état de la fenêtre), puis
     * construit au plus {@code maxChunks} chunks en attente, dans
     * l'ordre du document — les sous-arbres déjà construits gardent
     * leur identité.
     *
     * @param doc       le document de l'analyse partielle
     * @param partielle l'analyse partielle à compléter
     * @param token     le jeton d'annulation de la complétion
     * @param maxChunks le nombre maximal de chunks à construire
     * @return l'analyse complétée d'autant de chunks, ou la même si rien à construire
     * @throws AnalysisCancelled si la complétion est annulée
     */
    public Analysis completer(Document doc, Analysis partielle, TokenCancellation token,
                              int maxChunks) {
        if (partielle.isComplete() || maxChunks <= 0) {
            return partielle;
        }
        LexDocument lexique = partielle.lex();
        LexDocument complet = lexer.completerLexique(doc, language.lexer(),
                lexique, token, LOTS_LEXIQUE);
        if (lexique.porteLignesVides()) {
            // L'analyse née d'une FENÊTRE : sa géométrie de chunks ne
            // se prédit pas (les bornes hors fenêtre venaient des
            // jetons du visible seul) — les chunks attendent que le
            // lexique soit complet, puis l'arbre se REBÂTIT entier :
            // l'équivalence au complet l'exige. La couleur, elle,
            // avance par le LEXIQUE pendant les lots (la vue colore
            // ligne à ligne, jamais l'arbre).
            if (complet.porteLignesVides()) {
                return new Analysis(doc.revision(), complet,
                        partielle.tree());
            }
            SyntaxTree tree = parser.parseFull(doc, complet,
                    language.parser(), doc.revision());
            return new Analysis(doc.revision(), complet, tree);
        }
        SyntaxTree tree = parser.completer(doc, complet, language.parser(),
                doc.revision(), partielle.tree(), token, maxChunks);
        return new Analysis(doc.revision(), complet, tree);
    }

    /** La marge du lexeur au-delà du visible (le défilement ne
     *  re-paie pas la lecture) — deux écrans et demi de marge. */
    private static final int MARGE_FENETRE = 64;

    /** Les lignes relues par lot de complétion du lexique — la
     *  publication suit à ce pas, la couleur avance par vagues. */
    private static final int LOTS_LEXIQUE = 256;

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
