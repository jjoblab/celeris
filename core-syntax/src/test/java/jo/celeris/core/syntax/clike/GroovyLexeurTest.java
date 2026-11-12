package jo.celeris.core.syntax.clike;


import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Le lexeur Groovy (ADR-004, dialecte Gradle — C-07) : commentaires de
 * bloc NICHÉS par état à profondeur, documentation distinguée, chaînes
 * simples à apostrophe, gabarits — les guillemets internes aux
 * accolades {@code ${…}} ne clôturent pas —, chaînes triples
 * multi-lignes par état transporté (crochet apparié à l'ouverture comme
 * à la fermeture), annotations, signes propres ({@code ?., *., ..,
 * ..<, <=>, ==~}). Une chaîne non fermée est un jeton ERROR, jamais
 * une exception.
 */
class GroovyLexeurTest {

    private final CLikeLexer lexeur = new CLikeLexer(GroovyDialect.INSTANCE);

    private LexedLine ligne(String texte, LexState entree) {
        return lexeur.lex(texte, entree);
    }

    /** Le commentaire de ligne couvre la fin de ligne. */
    @Test
    void commentaireDeLigne() {
        LexedLine lue = ligne("x = 1 // note", CLikeLexer.SimpleState.NORMAL);
        assertTrue(lue.tokens().length >= 2);
        Token dernier = lue.tokens()[lue.tokens().length - 1];
        assertEquals(TokenKind.COMMENT, dernier.kind());
        assertEquals(6, dernier.start());
    }

    /** Le commentaire de bloc niche : la profondeur traverse les lignes par état. */
    @Test
    void commentaireDeBlocNicheParEtat() {
        LexedLine premiere = ligne("/* ouvre /* niche", CLikeLexer.SimpleState.NORMAL);
        assertEquals(1, premiere.tokens().length);
        assertEquals(TokenKind.COMMENT, premiere.tokens()[0].kind());
        assertTrue(premiere.exitState() instanceof CLikeLexer.InBlockComment);
        CLikeLexer.InBlockComment dansBloc = (CLikeLexer.InBlockComment) premiere.exitState();
        assertEquals(2, dansBloc.depth());
        LexedLine seconde = ligne(" ferme */ */ code", dansBloc);
        assertEquals(TokenKind.COMMENT, seconde.tokens()[0].kind());
        assertEquals(CLikeLexer.SimpleState.NORMAL, seconde.exitState());
        Token[] suite = seconde.tokens();
        assertEquals(TokenKind.IDENTIFIER, suite[1].kind());
    }

    /** La documentation se distingue du commentaire ordinaire. */
    @Test
    void documentationDistinguee() {
        LexedLine lue = ligne("/** doc du script */", CLikeLexer.SimpleState.NORMAL);
        assertEquals(1, lue.tokens().length);
        assertEquals(TokenKind.DOC, lue.tokens()[0].kind());
    }

    /** La chaîne à apostrophe est un jeton soudé. */
    @Test
    void chaineSimpleApostrophe() {
        LexedLine lue = ligne("id 'java-library'", CLikeLexer.SimpleState.NORMAL);
        Token[] jetons = lue.tokens();
        assertEquals(TokenKind.STRING, jetons[1].kind());
        assertEquals(3, jetons[1].start());
        assertEquals(17, jetons[1].end());
    }

    /** Les guillemets internes aux accolades d'un gabarit ne clôturent pas. */
    @Test
    void gabaritGuillemetsInternesAuxAccolades() {
        LexedLine lue = ligne("println \"cle ${map[\"k\"]}\"", CLikeLexer.SimpleState.NORMAL);
        Token[] jetons = lue.tokens();
        assertEquals(TokenKind.STRING, jetons[1].kind());
        assertEquals(8, jetons[1].start());
        assertEquals(25, jetons[1].end());
    }

    /** Un gabarit non fermé est un jeton ERROR. */
    @Test
    void gabaritNonFermeEstErreur() {
        LexedLine lue = ligne("println \"ouvert", CLikeLexer.SimpleState.NORMAL);
        Token[] jetons = lue.tokens();
        assertEquals(TokenKind.ERROR, jetons[jetons.length - 1].kind());
    }

    /** La chaîne triple ouvre un crochet apparié et traverse les lignes par état. */
    @Test
    void tripleSimpleMultiLignes() {
        LexedLine ouverture = ligne("def doc = '''texte", CLikeLexer.SimpleState.NORMAL);
        Token[] jetons = ouverture.tokens();
        assertEquals(TokenKind.OPEN_BRACKET, jetons[3].kind());
        assertTrue(ouverture.exitState() instanceof CLikeLexer.InTripleString);
        CLikeLexer.InTripleString dansTriple = (CLikeLexer.InTripleString) ouverture.exitState();
        assertEquals('\'', dansTriple.delimiter());
        LexedLine milieu = ligne("suite", dansTriple);
        assertEquals(1, milieu.tokens().length);
        assertEquals(TokenKind.STRING, milieu.tokens()[0].kind());
        assertEquals(dansTriple, milieu.exitState());
        LexedLine fermeture = ligne("fin''' + x", dansTriple);
        assertEquals(TokenKind.STRING, fermeture.tokens()[0].kind());
        assertEquals(TokenKind.CLOSE_BRACKET, fermeture.tokens()[1].kind());
        assertEquals(CLikeLexer.SimpleState.NORMAL, fermeture.exitState());
    }

    /** La chaîne triple à guillemets suit la même voie. */
    @Test
    void tripleDoublePareil() {
        LexedLine ouverture = ligne("println \"\"\"gabarit ${x}", CLikeLexer.SimpleState.NORMAL);
        Token[] jetons = ouverture.tokens();
        assertEquals(TokenKind.OPEN_BRACKET, jetons[1].kind());
        assertTrue(ouverture.exitState() instanceof CLikeLexer.InTripleString);
    }

    /** Annotations, mots réservés propres et convention de casse. */
    @Test
    void annotationsMotsReservesEtCasse() {
        LexedLine lue = ligne("@Task def it = Nouveau.MAX", CLikeLexer.SimpleState.NORMAL);
        Token[] jetons = lue.tokens();
        assertEquals(TokenKind.ANNOTATION, jetons[0].kind());
        assertEquals(TokenKind.KEYWORD, jetons[1].kind());
        assertEquals(TokenKind.KEYWORD, jetons[2].kind());
        assertEquals(TokenKind.IDENTIFIER, jetons[4].kind(),
                "le segment qualifié reste un identificateur");
        assertEquals(TokenKind.CONSTANT, jetons[6].kind());
    }

    /** Les signes propres de Groovy se lexe au plus long d'abord. */
    @Test
    void signesPropres() {
        LexedLine lue = ligne("a?.b *.c d..<e f<=>g h==~i", CLikeLexer.SimpleState.NORMAL);
        Token[] jetons = lue.tokens();
        int operateurs = 0;
        for (Token jeton : jetons) {
            if (jeton.kind() == TokenKind.OPERATOR) {
                operateurs++;
            }
        }
        assertEquals(5, operateurs);
    }
}
