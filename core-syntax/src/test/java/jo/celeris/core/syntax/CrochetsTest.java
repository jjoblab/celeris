package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        String texte = "a ( b { c } d ) e";
        Document doc = Document.of(texte);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();

        TokenMatch ouvrant = BracketMatcher.matchAt(doc, lex, texte.indexOf('('));
        assertEquals(texte.indexOf('('), ouvrant.start());
        assertEquals(texte.indexOf(')'), ouvrant.otherStart());

        TokenMatch fermant = BracketMatcher.matchAt(doc, lex, texte.indexOf(')'));
        assertEquals(texte.indexOf(')'), fermant.start());
        assertEquals(texte.indexOf('('), fermant.otherStart());

        TokenMatch interne = BracketMatcher.matchAt(doc, lex, texte.indexOf('{'));
        assertEquals(texte.indexOf('}'), interne.otherStart());
    }

    /** L'imbriquement rend le partenaire du niveau, pas le premier venu. */
    @Test
    void imbricationRendLePartenaireDuNiveau() {
        String texte = "( a ( b ) c )";
        Document doc = Document.of(texte);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();
        TokenMatch interne = BracketMatcher.matchAt(doc, lex, texte.indexOf("( b") + 1);
        assertEquals(texte.indexOf("b )"), interne.otherStart());
    }

    /** Un crochet sans partenaire ne désigne personne, sans lever. */
    @Test
    void crochetOrphelinNeDesignePersonne() {
        String texte = "a ( b c";
        Document doc = Document.of(texte);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();
        assertNull(BracketMatcher.matchAt(doc, lex, texte.indexOf('(')));
    }

    /** Un offset hors crochet ne désigne personne. */
    @Test
    void offsetSansCrochetNeDesignePersonne() {
        String texte = "abc def";
        Document doc = Document.of(texte);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();
        assertNull(BracketMatcher.matchAt(doc, lex, 0));
        assertNull(BracketMatcher.matchAt(doc, lex, 7));
    }

    /** Les crochets des commentaires et chaînes sont invisibles pour la visée. */
    @Test
    void crochetsDeCommentaireEtChaineInvisibles() {
        String texte = "/* ( { */ x \"y)\" z (w)";
        Document doc = Document.of(texte);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();
        assertNull(BracketMatcher.matchAt(doc, lex, texte.indexOf("/* (") + 3));
        assertNull(BracketMatcher.matchAt(doc, lex, texte.indexOf("( {") + 2));
        assertNull(BracketMatcher.matchAt(doc, lex, texte.indexOf("y)") ));
        TokenMatch reel = BracketMatcher.matchAt(doc, lex, texte.indexOf("(w)"));
        assertEquals(texte.indexOf("(w)"), reel.start());
        assertEquals(texte.length() - 1, reel.otherStart());
    }

    /** Un offset entre deux crochets vise le suivant. */
    @Test
    void offsetEntreDeuxCrochetsViseLeSuivant() {
        String texte = "ab ( c ) de { f }";
        Document doc = Document.of(texte);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();
        TokenMatch vise = BracketMatcher.matchAt(doc, lex, 0);
        assertEquals(texte.indexOf('('), vise.start());
        TokenMatch second = BracketMatcher.matchAt(doc, lex, texte.indexOf("de"));
        assertEquals(texte.indexOf('{'), second.start());
    }

    /** Le partenaire se trouve au-delà des lignes, par balayage local. */
    @Test
    void partenaireMultiLignes() {
        String texte = "debut (\n" + "milieu\n" + ") fin";
        Document doc = Document.of(texte);
        LexDocument lex = moteur.lexFull(doc, lexeur).lex();
        TokenMatch vise = BracketMatcher.matchAt(doc, lex, texte.indexOf('('));
        assertEquals(doc.lineStart(2), vise.otherStart());
    }
}
