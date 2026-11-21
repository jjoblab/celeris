package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.intelligence.internal.scan.IdentifierScan;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.IncrementalLexer;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;

/**
 * La complétion sur un lexique PÉRIMÉ (l'audit de la 2.2.13) : la
 * publication date d'avant la frappe quand la première analyse part
 * direct — la fusion qui raccourcit le document exposait des jetons
 * au-delà de la fin, la complétion crashait en
 * IndexOutOfBoundsException. Les jetons morts se sautent, les
 * survivants servent : jamais la complétion ne crashe sur une
 * publication plus ancienne que le document.
 */
class CompletionLexiquePerimeTest {

    private static final String AVANT = """
            class Avant {
                void corps() {
                    int alpha = 1;
                }
            }
            """;

    /** Le scan survit à un document RACCOURCI : le lexique périmé ne
     *  dépasse plus la fin, les noms survivants se servent. */
    @Test
    void leScanSurvitAuDocumentRaccourci() {
        Document avant = Document.of(AVANT);
        var lex = new IncrementalLexer()
                .lexFull(avant, JavaSyntax.INSTANCE.lexer()).lex();
        Document apres = Document.of("class Apres { int beta; }\n");
        List<IdentifierScan.NomOffset> noms = IdentifierScan.scanner(apres, lex);
        assertFalse(noms.isEmpty(),
                "les noms survivants du lexique périmé se servent");
    }

    /** La complétion entière survit : l'analyse périmée d'un document
     *  raccourci ne fait plus trembler la frappe. */
    @Test
    void laCompletionSurvitALanalysePerimee() {
        Document avant = Document.of(AVANT);
        Analysis perimee = jo.celeris.core.syntax.SyntaxService
                .of(JavaSyntax.INSTANCE).analyseFull(avant);
        Document apres = Document.of("class Apres { int beta; }\n");
        // Caret au début (préfixe vide) : le jeton survivant du
        // lexique périmé — position tenue, texte courant lu — sert
        // « Apres » ; l'essentiel : la complétion ne crashe plus sur
        // le document raccourci.
        List<String> mots = CompletionMots.completer(apres, perimee, 0,
                List.of("class"));
        assertTrue(mots.contains("Apres"),
                "le jeton survivant sert le texte courant : " + mots);
    }
}
