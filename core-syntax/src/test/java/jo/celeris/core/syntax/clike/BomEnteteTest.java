package jo.celeris.core.syntax.clike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.IncrementalLexer;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;

/**
 * Le banc de la BOM (feuille de route A2) : la marque d'ordre U+FEFF
 * d'un fichier UTF-8 était lexicographiée comme un OPÉRATEUR — elle
 * recevait l'encre des opérateurs, une couleur parasite en tête de
 * fichier (constat de l'audit : L0 OPERATOR[0..1] KEYWORD[1..4]…).
 * Le caractère est un blanc de FORMAT : la première ligne commence à
 * l'offset 1, aucun jeton ne le couvre.
 *
 * <p>Le banc verrouille les DEUX voies de reprise du lexeur : la passe
 * complète ET la reprise incrémentale — la BOM d'une ligne 0 touchée
 * par une édition ne doit pas redevenir un opérateur.</p>
 */
final class BomEnteteTest {

    private static final LanguageLexer LEXEUR = JavaSyntax.INSTANCE.lexer();

    /** La BOM en tête : aucun jeton ne la couvre, le mot-clé commence
     *  à l'offset 1. */
    @Test
    void laBomDeTeteNEstPasUnOperateur() {
        Document doc = Document.of("\uFEFFint a;\nint b;\n");
        LexDocument lex = new IncrementalLexer().lexFull(doc, LEXEUR).lex();
        Token[] tokens = lex.line(0).tokens();
        assertEquals(TokenKind.KEYWORD, tokens[0].kind(),
                "le premier jeton est le mot-clé, pas un opérateur");
        assertEquals(1, tokens[0].start(),
                "le mot-clé commence après la BOM (offset 1)");
        for (Token token : tokens) {
            assertTrue(token.start() >= 1 && token.end() >= 1,
                    "aucun jeton ne couvre la BOM");
        }
        // la ligne 1 reste ordinaire
        assertEquals(TokenKind.KEYWORD, lex.line(1).tokens()[0].kind());
        assertEquals(0, lex.line(1).tokens()[0].start(),
                "la BOM ne concerne que la première ligne");
    }

    /** Une BOM AU MILIEU d'une ligne reste ce qu'elle est (un
     *  opérateur visuel) : seule la BOM DE TÊTE DE PREMIÈRE EMPTY_LINE est
     *  un blanc de format — le banc fige la frontière. */
    @Test
    void laBomAuMilieuResteUnJeton() {
        Document doc = Document.of("int a\uFEFFb;\n");
        LexDocument lex = new IncrementalLexer().lexFull(doc, LEXEUR).lex();
        Token[] tokens = lex.line(0).tokens();
        boolean bomColoree = false;
        for (Token token : tokens) {
            if (token.start() <= 4 && token.end() > 4) {
                bomColoree = token.kind() == TokenKind.OPERATOR
                        || token.kind() == TokenKind.IDENTIFIER;
            }
        }
        assertTrue(bomColoree,
                "la BOM au milieu d'un identifiant fait partie du jeton "
                        + "(identificateur ou opérateur), jamais ignorée");
    }

    /** La reprise incrémentale de la ligne 0 : éditer après la BOM ne
     *  fait pas revenir l'opérateur parasite. */
    @Test
    void laRepriseIncrementaleGardeLaBomIgnoree() {
        String text = "\uFEFFint alpha;\nint beta;\n";
        Document avant = Document.of(text);
        LexDocument lexAvant = new IncrementalLexer().lexFull(avant, LEXEUR).lex();
        int pos = text.indexOf("alpha") + 5;
        String apresTexte = text.substring(0, pos) + "bet" + text.substring(pos);
        Document apres = Document.of(apresTexte);
        LexDocument lexApres = new IncrementalLexer().lexIncremental(apres,
                LEXEUR, lexAvant,
                jo.celeris.core.syntax.EditSite.of(avant, apres, pos, pos, "bet")).lex();
        Token[] tokens = lexApres.line(0).tokens();
        assertEquals(TokenKind.KEYWORD, tokens[0].kind());
        assertEquals(1, tokens[0].start(),
                "la reprise garde la BOM ignorée — le mot-clé à l'offset 1");
    }
}
