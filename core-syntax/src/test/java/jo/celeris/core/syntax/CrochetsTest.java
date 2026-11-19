package jo.celeris.core.syntax;

import jo.celeris.core.syntax.structure.BracketMatcher;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * L'appariement des crochets (socle commun, ADR-004) : la visée part d'un
 * offset, trouve le crochet qui s'y trouve (ou le plus proche à droite) et
 * désigne son partenaire par un balayage local — jamais une passe sur le
 * document entier. Les crochets dans les commentaires ou les chaînes
 * n'existent pas pour le lexique, donc jamais pour la visée.
 */
class CrochetsTest {

    private final IncrementalLexer moteur = new IncrementalLexer();
    private final LanguageLexer lexeur = MiniLangage.langage().lexer();

    /** Le crochet à l'offset désigne son partenaire, dans les deux sens. */
    @Test
    void crochetDesigneSonPartenaire() {
        String text = "a ( b { c } d ) e";
        Document doc = Document.of(text);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();

        TokenMatch ouvrant = attendMatch(doc, lex, text.indexOf('('));
        assertEquals(text.indexOf('('), ouvrant.start());
        assertEquals(text.indexOf(')'), ouvrant.otherStart());

        TokenMatch fermant = attendMatch(doc, lex, text.indexOf(')'));
        assertEquals(text.indexOf(')'), fermant.start());
        assertEquals(text.indexOf('('), fermant.otherStart());

        TokenMatch interne = attendMatch(doc, lex, text.indexOf('{'));
        assertEquals(text.indexOf('}'), interne.otherStart());
    }

    /** L'imbriquement rend le partenaire du niveau, pas le premier venu. */
    @Test
    void imbricationRendLePartenaireDuNiveau() {
        String text = "( a ( b ) c )";
        Document doc = Document.of(text);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();
        TokenMatch interne = attendMatch(doc, lex, text.indexOf("( b") + 1);
        assertEquals(text.indexOf(")"), interne.otherStart(),
                "le partenaire du niveau, pas le premier fermant venu");
    }

    /** Un crochet sans partenaire ne désigne personne, sans lever. */
    @Test
    void crochetOrphelinNeDesignePersonne() {
        String text = "a ( b c";
        Document doc = Document.of(text);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();
        assertNull(BracketMatcher.matchAt(doc, lex, text.indexOf('(')));
    }

    /** Un offset hors crochet ne désigne personne. */
    @Test
    void offsetSansCrochetNeDesignePersonne() {
        String text = "abc def";
        Document doc = Document.of(text);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();
        assertNull(BracketMatcher.matchAt(doc, lex, 0));
        assertNull(BracketMatcher.matchAt(doc, lex, 7));
    }

    /** Les crochets des commentaires et chaînes sont invisibles pour la visée. */
    @Test
    void crochetsDeCommentaireEtChaineInvisibles() {
        String text = "/* ( { */ x \"y)\" z (w)";
        Document doc = Document.of(text);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();
        assertNull(BracketMatcher.matchAt(doc, lex, text.indexOf("/* (") + 3));
        assertNull(BracketMatcher.matchAt(doc, lex, text.indexOf("( {") + 2));
        assertNull(BracketMatcher.matchAt(doc, lex, text.indexOf("y)") ));
        TokenMatch reel = attendMatch(doc, lex, text.indexOf("(w)"));
        assertEquals(text.indexOf("(w)"), reel.start());
        assertEquals(text.length() - 1, reel.otherStart());
    }

    /** Un offset collé au crochet le vise ; loin d'un crochet, personne. */
    @Test
    void offsetColleAuCrochetLeVise() {
        String text = "ab ( c ) de { f }";
        Document doc = Document.of(text);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();
        TokenMatch colle = attendMatch(doc, lex, text.indexOf('('));
        assertEquals(text.indexOf('('), colle.start());
        TokenMatch apres = attendMatch(doc, lex, text.indexOf('(') + 1);
        assertEquals(text.indexOf('('), apres.start(),
                "collé juste après l'ouvrant, c'est encore lui");
        assertNull(BracketMatcher.matchAt(doc, lex, 0));
        assertNull(BracketMatcher.matchAt(doc, lex, text.indexOf("de")));
    }

    /** Le partenaire se trouve au-delà des lignes, par balayage local. */
    @Test
    void partenaireMultiLignes() {
        String text = "debut (\nmilieu\n) fin";
        Document doc = Document.of(text);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();
        TokenMatch vise = attendMatch(doc, lex, text.indexOf('('));
        assertEquals(doc.lineStart(2), vise.otherStart());
    }

    private static TokenMatch attendMatch(Document doc, LexDocument lex, int offset) {
        TokenMatch pair = BracketMatcher.matchAt(doc, lex, offset);
        assertNotNull(pair);
        return pair;
    }
}
