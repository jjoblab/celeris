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
        EtatGrammaire sortie = (EtatGrammaire) analyse.lex().line(1).exitState();
        assertTrue(sortie.dansBloc(), "la chaîne triple est ouverte en fin de ligne 1");
        BlocOuvert bloc = sortie.bloc();
        EtatGrammaire ouvertA = new EtatGrammaire(bloc, EtatGrammaire.VIDE);
        EtatGrammaire ouvertB = new EtatGrammaire(bloc, EtatGrammaire.VIDE);
        assertNotSame(ouvertA, ouvertB, "deux piles distinctes");
        assertEquals(ouvertA, ouvertB, "mais égales par valeur (PIEGE-21)");
        assertEquals(new EtatGrammaire(bloc, ouvertA), new EtatGrammaire(bloc, ouvertB),
                "les piles imbriquées aussi");
        assertEquals(EtatGrammaire.VIDE, new EtatGrammaire(null, null),
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

        int cible = PYTHON.indexOf("ligne un");
        Document edite = doc.replace(cible, cible + 1, "L");
        EditSite site = EditSite.of(doc, edite, cible, cible + 1, "L");
        Analysis diff = service.analyseIncremental(edite, complet, site);

        for (int ligne = 0; ligne < 6; ligne++) {
            LexedLine avant = complet.lex().line(ligne);
            LexedLine apres = diff.lex().line(ligne);
            if (ligne == 2) {
                assertNotSame(avant, apres, "seule la ligne éditée se relit");
            } else {
                assertSame(avant, apres,
                        "la ligne " + ligne + " garde son panier (PIEGE-21)");
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
        Grammaire grammaire = Grammaires.lire(texteDePaquet("python"));
        LanguageLexer lexeur = new GrammaireLexeur(grammaire);
        LexedLine ligne = lexeur.lex("if x >= 2:  # borne",
                EtatGrammaire.VIDE);
        Token[] jetons = ligne.tokens();
        assertEquals(6, jetons.length, "six jetons, blancs exclus");
        assertEquals(new Token(TokenKind.KEYWORD, 0, 2), jetons[0], "if est réservé");
        assertEquals(new Token(TokenKind.IDENTIFIER, 3, 4), jetons[1], "x est un écart");
        assertEquals(new Token(TokenKind.OPERATOR, 5, 7), jetons[2], ">= est un opérateur");
        assertEquals(new Token(TokenKind.NUMBER, 8, 9), jetons[3], "2 est un nombre");
        assertEquals(new Token(TokenKind.PUNCTUATION, 9, 10), jetons[4], "le séparateur");
        assertEquals(new Token(TokenKind.COMMENT, 12, 19), jetons[5],
                "le commentaire de fin de ligne");
        assertEquals(EtatGrammaire.VIDE, ligne.exitState(), "rien d'ouvert en sortie");
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

        int cible = PYTHON.indexOf("\"\"\"");
        Document edite = doc.replace(cible, cible + 3, "'''");
        EditSite site = EditSite.of(doc, edite, cible, cible + 3, "'''");
        Analysis diff = service.analyseIncremental(edite, complet, site);

        for (int ligne = 0; ligne < 6; ligne++) {
            LexedLine avant = complet.lex().line(ligne);
            LexedLine apres = diff.lex().line(ligne);
            if (ligne == 0) {
                assertSame(avant, apres,
                        "la ligne d'avant l'ouverture garde son panier");
            } else {
                assertNotSame(avant, apres,
                        "la ligne " + ligne + " se relit : l'état de sortie diverge");
            }
        }
    }

    private static boolean contient(Analysis analyse, int ligne, TokenKind sorte) {
        for (Token jeton : analyse.lex().line(ligne).tokens()) {
            if (jeton.kind() == sorte) {
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
