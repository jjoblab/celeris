package jo.celeris.core.syntax;

import jo.celeris.core.syntax.json.JsonSyntax;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Le lexeur JSON (ADR-004) : chaînes échappées, nombres signés à virgule
 * et exposant, mots réservés true/false/null, accolades et crochets
 * appariés, deux-points et virgules en ponctuation. Aucun état transporté
 * — une chaîne ne traverse jamais les lignes ; une chaîne non fermée est
 * un jeton ERROR, jamais une exception.
 */
class JsonLexeurTest {

    private final LanguageLexer lexeur = JsonSyntax.INSTANCE.lexer();

    private Token[] lex(String ligne) {
        return lexeur.lex(ligne, lexeur.initialState()).tokens();
    }

    /** Les chaînes se fermement, échappements compris. */
    @Test
    void chainesEchappees() {
        Token[] jetons = lex("\"clé\": \"a\\\"b\\\\c\"");
        assertEquals(TokenKind.STRING, jetons[0].kind());
        assertEquals(5, jetons[0].length());
        assertEquals(TokenKind.PUNCTUATION, jetons[1].kind());
        assertEquals(TokenKind.STRING, jetons[2].kind());
        assertEquals(9, jetons[2].length(), "les échappements restent dans la chaîne");
    }

    /** Les nombres signés, à virgule et à exposant forment un jeton chacun. */
    @Test
    void nombres() {
        assertEquals(TokenKind.NUMBER, lex("42")[0].kind());
        assertEquals(TokenKind.NUMBER, lex("-17")[0].kind());
        assertEquals(TokenKind.NUMBER, lex("0.5")[0].kind());
        assertEquals(TokenKind.NUMBER, lex("1e-9")[0].kind());
        assertEquals(TokenKind.NUMBER, lex("6.02e23")[0].kind());
        assertEquals(4, lex("1e-9")[0].length());
    }

    /** true, false et null sont des mots réservés. */
    @Test
    void motsReserves() {
        assertEquals(TokenKind.KEYWORD, lex("true")[0].kind());
        assertEquals(TokenKind.KEYWORD, lex("false")[0].kind());
        assertEquals(TokenKind.KEYWORD, lex("null")[0].kind());
        assertEquals(TokenKind.IDENTIFIER, lex("vrai")[0].kind(),
                "les autres mots restent des identificateurs tolérés");
    }

    /** Accolades et crochets appariés, ponctuation deux-points et virgule. */
    @Test
    void crochetsEtPonctuation() {
        Token[] jetons = lex("{ \"a\": [1, 2] }");
        assertEquals(TokenKind.OPEN_BRACKET, jetons[0].kind());
        assertEquals(TokenKind.PUNCTUATION, jetons[2].kind(), "le deux-points");
        assertEquals(TokenKind.PUNCTUATION, jetons[5].kind(), "la virgule");
        assertEquals(TokenKind.CLOSE_BRACKET, jetons[7].kind(), "le crochet fermant");
        assertEquals(TokenKind.CLOSE_BRACKET, jetons[8].kind(), "l'accolade fermante");
    }

    /** Une chaîne non fermée donne un jeton ERROR, pas d'exception. */
    @Test
    void chaineNonFermee() {
        assertEquals(TokenKind.ERROR, lex("\"sans fin")[0].kind());
    }

    /** Le lexique complet du corpus tient sans incident. */
    @Test
    void lexiqueComplet() {
        String source = CorpusChargeur.exempleJson();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult resultat = moteur.lexFull(Document.of(source), lexeur);
        assertEquals(Document.of(source).lineCount(), resultat.lex().lineCount());
        assertEquals(lexeur.initialState(),
                resultat.lex().line(resultat.lex().lineCount() - 1).exitState());
    }
}
