package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.EditSite;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;

/**
 * Le lexeur des grammaires (ADR-004) : jetons pilotés par la grammaire,
 * états transportés par la pile des blocs ouverts — la chaîne triple
 * ouvre un crochet apparié, le contenu traverse les lignes en encre de
 * chaîne, la fermeture referme le crochet. PIEGE-21 : la pile est à
 * égalité de valeur, la cascade de re-lexicographie s'arrête exactement
 * à la reconvergence — les paniers au-delà gardent leur identité.
 */
class GrammaireLexeurTest {

    private static final String PYTHON = """
            x = 1
            doc = \"\"\"
            ligne un
            ligne deux
            \"\"\"
            y = 2
            """;

    /** La pile transportée est à égalité de valeur, jamais réifiée par identité. */
    @Test
    void pileEtatsReconvergeParValeur() {
        SyntaxService service = SyntaxService.of(syntaxeDePaquet("python"));
        Analysis analyse = service.analyseFull(Document.of(PYTHON));
        GrammarState output = (GrammarState) java.util.Objects.requireNonNull(analyse.lex().line(1).exitState());
        assertTrue(output.dansBloc(), "la chaîne triple est ouverte en fin de ligne 1");
        OpenBlock bloc = output.bloc();
        GrammarState ouvertA = new GrammarState(bloc, GrammarState.EMPTY);
        GrammarState ouvertB = new GrammarState(bloc, GrammarState.EMPTY);
        assertNotSame(ouvertA, ouvertB, "deux piles distinctes");
        assertEquals(ouvertA, ouvertB, "mais égales par valeur (PIEGE-21)");
        assertEquals(new GrammarState(bloc, ouvertA), new GrammarState(bloc, ouvertB),
                "les piles imbriquées aussi");
        assertEquals(GrammarState.EMPTY, new GrammarState(null, null),
                "le fond se compare au vide");
    }

    /**
     * La cascade après édition s'arrête à la reconvergence : l'édition
     * au cœur d'une chaîne ouverte ne relit QUE sa ligne — l'état de
     * sortie (chaîne toujours ouverte) n'a pas changé, la ligne suivante
     * garde son panier.
     */
    @Test
    void cascadeBorneeParLaReconvergence() {
        SyntaxService service = SyntaxService.of(syntaxeDePaquet("python"));
        Document doc = Document.of(PYTHON);
        Analysis complet = service.analyseFull(doc);

        int target = PYTHON.indexOf("ligne un");
        Document edite = doc.replace(target, target + 1, "L");
        EditSite site = EditSite.of(doc, edite, target, target + 1, "L");
        Analysis diff = service.analyseIncremental(edite, complet, site);

        for (int line = 0; line < 6; line++) {
            LexedLine avant = complet.lex().line(line);
            LexedLine apres = diff.lex().line(line);
            if (line == 2) {
                assertNotSame(avant, apres, "seule la ligne éditée se relit");
            } else {
                assertSame(avant, apres,
                        "la ligne " + line + " garde son panier (PIEGE-21)");
            }
        }
    }

    /** La chaîne triple ouvre un crochet apparié, son contenu traverse les lignes. */
    @Test
    void chaineTripleEnCrochetsApparies() {
        SyntaxService service = SyntaxService.of(syntaxeDePaquet("python"));
        Analysis analyse = service.analyseFull(Document.of(PYTHON));

        assertTrue(contient(analyse, 1, TokenKind.OPEN_BRACKET),
                "l'ouverture triple guillemet est un crochet ouvrant");
        assertTrue(contient(analyse, 4, TokenKind.CLOSE_BRACKET),
                "la fermeture est un crochet fermant apparié");
        assertTrue(contient(analyse, 2, TokenKind.STRING),
                "le contenu de la première ligne de chaîne est coloré");
        assertTrue(contient(analyse, 3, TokenKind.STRING),
                "le contenu de la deuxième ligne de chaîne est coloré");
    }

    /** Les mots réservés, nombres et commentaires de la grammaire python. */
    @Test
    void motsNombresCommentaires() {
        Grammar grammaire = Grammars.read(texteDePaquet("python"));
        LanguageLexer lexeur = new GrammarLexer(grammaire);
        LexedLine line = lexeur.lex("if x >= 2:  # borne",
                GrammarState.EMPTY);
        Token[] tokens = line.tokens();
        assertEquals(6, tokens.length, "six jetons, blancs exclus");
        // la pile de portées vit dans les jetons (H-07) : les bornes
        // attendues se comparent champ à champ, la pile a son banc propre
        assertEquals(TokenKind.KEYWORD, tokens[0].kind(), "if est réservé");
        assertEquals(0, tokens[0].start());
        assertEquals(2, tokens[0].end());
        assertEquals(TokenKind.IDENTIFIER, tokens[1].kind(), "x est un écart");
        assertEquals(3, tokens[1].start());
        assertEquals(4, tokens[1].end());
        assertEquals(TokenKind.OPERATOR, tokens[2].kind(), ">= est un opérateur");
        assertEquals(5, tokens[2].start());
        assertEquals(7, tokens[2].end());
        assertEquals(TokenKind.NUMBER, tokens[3].kind(), "2 est un nombre");
        assertEquals(8, tokens[3].start());
        assertEquals(9, tokens[3].end());
        assertEquals(TokenKind.PUNCTUATION, tokens[4].kind(), "le séparateur");
        assertEquals(9, tokens[4].start());
        assertEquals(10, tokens[4].end());
        assertEquals(TokenKind.COMMENT, tokens[5].kind(),
                "le commentaire de fin de ligne");
        assertEquals(12, tokens[5].start());
        assertEquals(19, tokens[5].end());
        assertEquals(GrammarState.EMPTY, line.exitState(), "rien d'ouvert en sortie");
    }

    /**
     * L'ouverture qui ne ferme plus cascade jusqu'à la dernière ligne
     * et termine (PIEGE-19) : chaque état de sortie diverge, chaque
     * ligne se relit — mais la ligne d'avant garde son panier.
     */
    @Test
    void editionDeLOuvertureCascadeJusquauBout() {
        SyntaxService service = SyntaxService.of(syntaxeDePaquet("python"));
        Document doc = Document.of(PYTHON);
        Analysis complet = service.analyseFull(doc);

        int target = PYTHON.indexOf("\"\"\"");
        Document edite = doc.replace(target, target + 3, "'''");
        EditSite site = EditSite.of(doc, edite, target, target + 3, "'''");
        Analysis diff = service.analyseIncremental(edite, complet, site);

        for (int line = 0; line < 6; line++) {
            LexedLine avant = complet.lex().line(line);
            LexedLine apres = diff.lex().line(line);
            if (line == 0) {
                assertSame(avant, apres,
                        "la ligne d'avant l'ouverture garde son panier");
            } else {
                assertNotSame(avant, apres,
                        "la ligne " + line + " se relit : l'état de sortie diverge");
            }
        }
    }

    private static boolean contient(Analysis analyse, int line, TokenKind sorte) {
        for (Token token : analyse.lex().line(line).tokens()) {
            if (token.kind() == sorte) {
                return true;
            }
        }
        return false;
    }

    /** Le texte JSON de la grammaire d'un paquet intégré (M7.6). */
    private static String texteDePaquet(String id) {
        var flux = GrammaireLexeurTest.class.getResourceAsStream(
                "/langages/" + id + "/grammar.json");
        assertSame(true, flux != null, "grammaire du paquet " + id);
        try (flux) {
            return new String(flux.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** La source syntaxique d'un paquet intégré, par la vraie fabrique. */
    private static LanguageSyntax syntaxeDePaquet(String id) {
        return PackGrammars.INSTANCE.compile(id, texteDePaquet(id));
    }
}
