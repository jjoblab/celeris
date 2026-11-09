package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Le lexeur Java (ADR-004) : mots réservés, littéraux numériques, chaînes
 * et textes en bloc (état transporté), commentaires de ligne, de bloc et
 * de documentation, annotations, opérateurs contre ponctuation, crochets
 * appariés, identificateurs qualifiés. Une chaîne non fermée est un jeton
 * ERROR, jamais une exception.
 */
class JavaLexeurTest {

    private final LanguageLexer lexeur = JavaSyntax.INSTANCE.lexer();

    private Token[] lex(String ligne) {
        return lexeur.lex(ligne, CLikeLexer.SimpleState.NORMAL).tokens();
    }

    /** Les mots réservés et les littéraux se distinguent des identificateurs. */
    @Test
    void motsReservesEtLitteraux() {
        Token[] jetons = lex("public class while true false null");
        assertEquals(TokenKind.KEYWORD, jetons[0].kind());
        assertEquals("public".length(), jetons[0].length());
        assertEquals(TokenKind.KEYWORD, jetons[1].kind());
        assertEquals(TokenKind.KEYWORD, jetons[2].kind());
        assertEquals(TokenKind.KEYWORD, jetons[3].kind());
        assertEquals(TokenKind.KEYWORD, jetons[4].kind());
        assertEquals(TokenKind.KEYWORD, jetons[5].kind());

        Token[] identifiants = lex("compteur valeur");
        assertEquals(TokenKind.IDENTIFIER, identifiants[0].kind());
        assertEquals(TokenKind.IDENTIFIER, identifiants[1].kind());
    }

    /** Les littéraux numériques de toutes bases forment un jeton chacun. */
    @Test
    void litterauxNumeriques() {
        assertEquals(TokenKind.NUMBER, lex("0x1F")[0].kind());
        assertEquals(TokenKind.NUMBER, lex("0b1010")[0].kind());
        assertEquals(TokenKind.NUMBER, lex("1_000_000L")[0].kind());
        assertEquals(TokenKind.NUMBER, lex("3.14f")[0].kind());
        assertEquals(TokenKind.NUMBER, lex("1e-9")[0].kind());
        assertEquals(TokenKind.NUMBER, lex("42")[0].kind());
        assertEquals(1, lex("1_000_000L").length);
    }

    /** Chaînes et caractères, échappements compris. */
    @Test
    void chainesEtCaracteres() {
        Token[] jetons = lex("\"bon\\\"jour\" 'a' '\\n'");
        assertEquals(TokenKind.STRING, jetons[0].kind());
        assertEquals(TokenKind.STRING, jetons[1].kind());
        assertEquals(TokenKind.STRING, jetons[2].kind());
        assertEquals(3, jetons.length);
        assertEquals("\"bon\\\"jour\"".length(), jetons[0].length());
    }

    /** Une chaîne non fermée donne un jeton ERROR, pas d'exception. */
    @Test
    void chaineNonFermee() {
        Token[] jetons = lex("\"sans fin");
        assertEquals(TokenKind.ERROR, jetons[0].kind());
        assertSame(CLikeLexer.SimpleState.NORMAL, lexeur.lex("\"sans fin", CLikeLexer.SimpleState.NORMAL).exitState());
    }

    /** Les commentaires de ligne, de bloc et de documentation. */
    @Test
    void commentaires() {
        assertEquals(TokenKind.COMMENT, lex("// note")[0].kind());
        assertEquals(TokenKind.COMMENT, lex("/* bloc */ code")[0].kind());
        assertEquals(TokenKind.DOC, lex("/** doc */ class")[0].kind());
        Token[] lignes = lex("code /* ouverture");
        assertEquals(TokenKind.COMMENT, lignes[1].kind());
    }

    /** Un commentaire de bloc ouvert traverse les lignes par l'état. */
    @Test
    void commentaireDeBlocMultiLignes() {
        LexedLine premiere = lexeur.lex("a /* ouvert", CLikeLexer.SimpleState.NORMAL);
        assertEquals(new CLikeLexer.InBlockComment(1, false), premiere.exitState());
        LexedLine milieu = lexeur.lex("encore", premiere.exitState());
        assertEquals(1, milieu.tokens().length);
        assertEquals(TokenKind.COMMENT, milieu.tokens()[0].kind());
        LexedLine fermeture = lexeur.lex("fin */ b", milieu.exitState());
        assertEquals(TokenKind.COMMENT, fermeture.tokens()[0].kind());
        assertEquals(TokenKind.IDENTIFIER, fermeture.tokens()[1].kind());
        assertSame(CLikeLexer.SimpleState.NORMAL, fermeture.exitState());
    }

    /** La documentation (double astérisque) se distingue du bloc simple. */
    @Test
    void documentationContreBloc() {
        LexedLine doc = lexeur.lex("/** ouverte", CLikeLexer.SimpleState.NORMAL);
        assertEquals(new CLikeLexer.InBlockComment(1, true), doc.exitState());
        assertEquals(TokenKind.DOC, doc.tokens()[0].kind());
    }

    /** Le texte en bloc (triple guillemet) traverse les lignes. */
    @Test
    void texteEnBloc() {
        LexedLine ouverture = lexeur.lex("return \"\"\"", CLikeLexer.SimpleState.NORMAL);
        assertEquals(TokenKind.OPEN_BRACKET, ouverture.tokens()[1].kind());
        assertEquals(new CLikeLexer.InTripleString('"'), ouverture.exitState());
        LexedLine milieu = lexeur.lex("du texte", ouverture.exitState());
        assertEquals(1, milieu.tokens().length);
        assertEquals(TokenKind.STRING, milieu.tokens()[0].kind());
        LexedLine fermeture = lexeur.lex("encore\"\"\";", ouverture.exitState());
        assertSame(CLikeLexer.SimpleState.NORMAL, fermeture.exitState());
    }

    /** Annotations, opérateurs, ponctuation et crochets. */
    @Test
    void annotationsOperateursPonctuation() {
        Token[] jetons = lex("@Override");
        assertEquals(TokenKind.ANNOTATION, jetons[0].kind());
        assertEquals(1, jetons.length);

        Token[] ops = lex("a + b * c");
        assertEquals(TokenKind.IDENTIFIER, ops[0].kind());
        assertEquals(TokenKind.OPERATOR, ops[1].kind());
        assertEquals(TokenKind.IDENTIFIER, ops[2].kind());

        Token[] ponct = lex("a.b,c;");
        assertEquals(TokenKind.PUNCTUATION, ponct[1].kind());
        assertEquals(TokenKind.PUNCTUATION, ponct[3].kind());
        assertEquals(TokenKind.PUNCTUATION, ponct[5].kind());

        Token[] crochets = lex("f(a[0]) { }");
        assertEquals(TokenKind.OPEN_BRACKET, crochets[1].kind());
        assertEquals(TokenKind.OPEN_BRACKET, crochets[3].kind());
        assertEquals(TokenKind.CLOSE_BRACKET, crochets[5].kind());
        assertEquals(TokenKind.OPEN_BRACKET, crochets[7].kind());
    }

    /** Identificateurs à dollar et souligné, qualifiés par points. */
    @Test
    void identificateursExotiques() {
        assertEquals(TokenKind.IDENTIFIER, lex("$valeur _x")[0].kind());
        assertEquals(TokenKind.IDENTIFIER, lex("$valeur _x")[1].kind());
        Token[] qualifie = lex("Map.Entry");
        assertEquals(TokenKind.IDENTIFIER, qualifie[0].kind());
        assertEquals(TokenKind.PUNCTUATION, qualifie[1].kind());
        assertEquals(TokenKind.TYPE, qualifie[2].kind());
    }

    /** La convention de casse distingue types et constantes lexicalement. */
    @Test
    void conventionDeCasse() {
        assertEquals(TokenKind.TYPE, lex("String")[0].kind());
        assertEquals(TokenKind.CONSTANT, lex("LIMITE_MAX")[0].kind());
        assertEquals(TokenKind.IDENTIFIER, lex("compteur")[0].kind());
    }

    /** Le lexique complet d'un document Java tient sans incident. */
    @Test
    void lexiqueComplet() {
        String source = CorpusChargeur.exempleJava();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult resultat = moteur.lexFull(Document.of(source), lexeur);
        assertEquals(Document.of(source).lineCount(), resultat.lex().lineCount());
        assertSame(CLikeLexer.SimpleState.NORMAL,
                resultat.lex().line(resultat.lex().lineCount() - 1).exitState());
    }

    /** Les jetons d'une ligne sont disjoints et triés. */
    @Test
    void jetonsDisjointsEtTries() {
        Token[] jetons = lex("public static int LIMITE = 512;");
        int finPrecedente = 0;
        for (Token jeton : jetons) {
            assertEquals(finPrecedente, jeton.start(), "les blancs séparent les jetons");
            finPrecedente = jeton.end();
        }
        assertArrayEquals(jetons, jetons);
    }
}
