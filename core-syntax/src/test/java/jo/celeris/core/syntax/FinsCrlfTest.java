package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;

/**
 * Le banc de la convention CRLF (ADR-0026, feuille de route A3) : le
 * {@code \r} d'une fin CRLF reste dans la ligne rendue — l'index du
 * modèle ne compte que {@code \n}. Le constat d'origine : la
 * lexicographie est EXACTE (le {@code \r} est un blanc, aucun jeton
 * ne le couvre) ; la convention s'assume ici pour de bon — jamais un
 * document CRLF ne produit de jeton divergent, jamais une édition
 * avant une fin CRLF ne décale les couleurs.
 */
final class FinsCrlfTest {

    private static final LanguageLexer LEXEUR = JavaSyntax.INSTANCE.lexer();

    /** La convention du modèle : le \r reste dans la ligne rendue. */
    @Test
    void leCrResteDansLaLigneRendue() {
        Document doc = Document.of("int a;\r\nint b;\r\n");
        assertEquals(3, doc.lineCount());
        assertTrue(doc.lineText(0).endsWith("\r"),
                "le \\r est le dernier caractère du contenu (ADR-0026)");
        assertEquals(8, doc.lineStart(1), "l'entrée suivante suit le \\n");
        assertEquals(7, doc.lineEnd(0),
                "lineEnd rend l'offset du \\n, le \\r est contenu");
    }

    /** La lexicographie est exacte : aucun jeton ne couvre le \r. */
    @Test
    void laLexicographieIgnoreLeCr() {
        Document doc = Document.of("int a;\r\nint b;\r\n");
        LexDocument lex = new IncrementalLexer().lexFull(doc, LEXEUR).lex();
        Token[] ligne0 = lex.line(0).tokens();
        for (Token token : ligne0) {
            assertTrue(token.end() <= 7,
                    "aucun jeton ne couvre le \\r : " + token.kind()
                            + "[" + token.start() + ".." + token.end() + "]");
        }
    }

    /** L'édition avant une fin CRLF : l'incrémental égale le complet. */
    @Test
    void leditionAvantUneFinCrlfRecolle() {
        String text = "int alpha;\r\nint beta;\r\n";
        Document avant = Document.of(text);
        LexDocument lexAvant = new IncrementalLexer().lexFull(avant, LEXEUR).lex();
        int pos = text.indexOf("alpha") + 5;
        String apresTexte = text.substring(0, pos) + "bet" + text.substring(pos);
        Document apres = Document.of(apresTexte);
        LexDocument increm = new IncrementalLexer().lexIncremental(apres, LEXEUR,
                lexAvant, EditSite.of(avant, apres, pos, pos, "bet")).lex();
        LexDocument reference = new IncrementalLexer().lexFull(apres, LEXEUR).lex();
        assertEquals(decrire(reference).toString(), decrire(increm).toString(),
                "l'incrémental égale le complet sur un document CRLF");
    }

    private static StringBuilder decrire(LexDocument lex) {
        StringBuilder sb = new StringBuilder();
        for (int l = 0; l < lex.lineCount(); l++) {
            sb.append('L').append(l).append('[').append(lex.line(l).exitState()).append(']');
            for (Token t : lex.line(l).tokens()) {
                sb.append(t.kind()).append(t.start()).append(':').append(t.end()).append(' ');
            }
            sb.append('\n');
        }
        return sb;
    }
}
