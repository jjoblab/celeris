package jo.celeris.core.syntax.xml;

import jo.celeris.core.syntax.CorpusChargeur;

import jo.celeris.core.syntax.IncrementalLexer;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LexResult;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Le lexeur XML (ADR-004) : balises ouvrantes, fermantes et auto-fermées
 * appariées, attributs nommés-valeur, texte, sections CDATA et commentaires
 * multi-lignes par état transporté, prologue et entités. Une chaîne
 * d'attribut non fermée est un jeton ERROR, jamais une exception.
 */
class XmlLexeurTest {

    private final LanguageLexer lexeur = XmlSyntax.INSTANCE.lexer();

    private Token[] lex(String line) {
        return lexeur.lex(line, XmlLexeur.State.TEXTE).tokens();
    }

    /** Le texte du jeton relu dans sa ligne (le jeton ne porte que ses bornes). */
    private String text(String line, Token token) {
        return line.substring(token.start(), token.end());
    }

    /** Le prologue «&#x2f;?xml … ?&#x2f;» est un jeton de documentation. */
    @Test
    void prologue() {
        Token[] tokens = lex("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        assertEquals(1, tokens.length);
        assertEquals(TokenKind.DOC, tokens[0].kind());
    }

    /** La balise ouvrante porte son nom, ses attributs, son chevron final. */
    @Test
    void baliseOuvranteEtAttributs() {
        String line = "<produit id=\"a1\" prix=\"12,50\">";
        Token[] tokens = lex(line);
        assertEquals(8, tokens.length);
        assertEquals(TokenKind.OPEN_BRACKET, tokens[0].kind());
        assertEquals("<produit", text(line, tokens[0]));
        assertEquals(TokenKind.IDENTIFIER, tokens[1].kind());
        assertEquals("id", text(line, tokens[1]));
        assertEquals(TokenKind.OPERATOR, tokens[2].kind());
        assertEquals(TokenKind.STRING, tokens[3].kind());
        assertEquals("\"a1\"", text(line, tokens[3]));
        assertEquals(TokenKind.STRING, tokens[6].kind());
        assertEquals("\"12,50\"", text(line, tokens[6]));
        assertEquals(TokenKind.PUNCTUATION, tokens[7].kind());
        assertEquals(">", text(line, tokens[7]));
    }

    /** La fermeture porte le nom ; l'auto-fermeture est un crochet fermant. */
    @Test
    void fermetureEtAutoFermeture() {
        assertEquals(TokenKind.CLOSE_BRACKET, lex("</produit>")[0].kind());
        assertEquals("</produit>", text("</produit>", lex("</produit>")[0]));
        String ligneBrute = "<rupture/>";
        Token[] self = lex(ligneBrute);
        assertEquals(TokenKind.OPEN_BRACKET, self[0].kind());
        assertEquals(TokenKind.CLOSE_BRACKET, self[1].kind());
        assertEquals("/>", text(ligneBrute, self[1]));
    }

    /** Le texte entre balises est un jeton, entités comprises. */
    @Test
    void texteEtEntites() {
        String line = "Auteurs &amp; contributeurs — « Celeris »";
        Token[] tokens = lex(line);
        assertEquals(1, tokens.length);
        assertEquals(TokenKind.STRING, tokens[0].kind());
        assertEquals(line, text(line, tokens[0]));
    }

    /** Le texte s'arrête au «&#x2f;» qui ouvre la balise suivante. */
    @Test
    void textePuisBalise() {
        String line = "<nom>Livre « Unicode »</nom>";
        Token[] tokens = lex(line);
        assertEquals(4, tokens.length);
        assertEquals(TokenKind.OPEN_BRACKET, tokens[0].kind());
        assertEquals(TokenKind.PUNCTUATION, tokens[1].kind());
        assertEquals(TokenKind.STRING, tokens[2].kind());
        assertEquals("Livre « Unicode »", text("<nom>Livre « Unicode »</nom>", tokens[2]));
        assertEquals(TokenKind.CLOSE_BRACKET, tokens[3].kind());
    }

    /** Le chemin Windows en valeur d'attribut se ferme : l'antislash
     * n'échappe rien en XML — le guillemet après les antislashs ferme
     * la valeur, la cascade d'état ne dérive plus. */
    @Test
    void leCheminWindowsEnAttributSeFerme() {
        String line = "<projet chemin=\"C:\\Users\\celeris\">";
        Token[] tokens = lex(line);
        assertEquals(TokenKind.STRING, tokens[3].kind());
        assertEquals("\"C:\\Users\\celeris\"", text(line, tokens[3]));
        assertEquals(">", text(line, tokens[tokens.length - 1]));

        String enTraine = "<a racine=\"C:\\\">";
        Token[] traine = lex(enTraine);
        assertEquals(TokenKind.STRING, traine[3].kind(),
                "l'antislash final ne saute pas le guillemet fermant");
        assertEquals("\"C:\\\"", text(enTraine, traine[3]));
    }

    /** Un commentaire ouvert traverse les lignes par l'état, puis reprend. */
    @Test
    void commentaireMultiLignes() {
        LexedLine premiere = lexeur.lex("<!-- ouverture", XmlLexeur.State.TEXTE);
        assertNotNull(premiere.exitState());
        assertSame(XmlLexeur.State.COMMENTAIRE, premiere.exitState());
        LexedLine fermeture = lexeur.lex("fin --> <suite/>",
                premiere.exitState());
        assertEquals(TokenKind.COMMENT, fermeture.tokens()[0].kind());
        assertEquals(TokenKind.OPEN_BRACKET, fermeture.tokens()[1].kind());
        assertNotNull(fermeture.exitState());
        assertSame(XmlLexeur.State.TEXTE, fermeture.exitState());
    }

    /** La section CDATA ouvre un crochet apparié à sa fermeture. */
    @Test
    void cdataMultiLignes() {
        LexedLine ouverture = lexeur.lex("<d><![CDATA[brut <balise> &",
                XmlLexeur.State.TEXTE);
        assertNotNull(ouverture.exitState());
        assertSame(XmlLexeur.State.CDATA, ouverture.exitState());
        assertEquals(TokenKind.OPEN_BRACKET, ouverture.tokens()[2].kind());
        assertNotNull(ouverture.exitState());
        LexedLine milieu = lexeur.lex("encore du brut", ouverture.exitState());
        assertEquals(1, milieu.tokens().length);
        assertEquals(TokenKind.STRING, milieu.tokens()[0].kind());
        assertNotNull(ouverture.exitState());
        LexedLine fermeture = lexeur.lex("fin]]> </d>", ouverture.exitState());
        assertEquals(TokenKind.STRING, fermeture.tokens()[0].kind());
        assertEquals(TokenKind.CLOSE_BRACKET, fermeture.tokens()[1].kind());
        assertEquals("]]>", text("fin]]> </d>", fermeture.tokens()[1]));
        assertEquals(TokenKind.CLOSE_BRACKET, fermeture.tokens()[2].kind());
        assertNotNull(fermeture.exitState());
        assertSame(XmlLexeur.State.TEXTE, fermeture.exitState());
    }

    /** Une balise ouverte traverse les lignes : les attributs continuent. */
    @Test
    void baliseMultiLignes() {
        LexedLine premiere = lexeur.lex("<produit id=\"a1\"",
                XmlLexeur.State.TEXTE);
        assertSame(XmlLexeur.State.BALISE, premiere.exitState());
        assertNotNull(premiere.exitState());
        LexedLine suite = lexeur.lex("prix=\"8,90\">", premiere.exitState());
        assertEquals(TokenKind.IDENTIFIER, suite.tokens()[0].kind());
        assertEquals(TokenKind.OPERATOR, suite.tokens()[1].kind());
        assertEquals(TokenKind.STRING, suite.tokens()[2].kind());
        assertEquals(TokenKind.PUNCTUATION, suite.tokens()[3].kind());
        assertSame(XmlLexeur.State.TEXTE, suite.exitState());
    }

    /** Une valeur d'attribut non fermée donne un jeton ERROR, pas d'exception. */
    @Test
    void attributNonFerme() {
        Token[] tokens = lex("<a v=\"sans fin>");
        assertEquals(TokenKind.ERROR, tokens[3].kind());
    }

    /** Les noms de balises accueillent les lettres non ASCII. */
    @Test
    void nomsNonAscii() {
        String line = "<méta>français</méta>";
        Token[] tokens = lex(line);
        assertEquals(TokenKind.OPEN_BRACKET, tokens[0].kind());
        assertEquals("<méta", text(line, tokens[0]));
        assertEquals(TokenKind.PUNCTUATION, tokens[1].kind());
        assertEquals(TokenKind.STRING, tokens[2].kind());
        assertEquals(TokenKind.CLOSE_BRACKET, tokens[3].kind());
        assertEquals("</méta>", text(line, tokens[3]));
    }

    /** Le lexique complet du corpus tient sans incident. */
    @Test
    void lexiqueComplet() {
        String source = CorpusChargeur.exempleXml();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult resultat = moteur.lexFull(Document.of(source), lexeur);
        assertEquals(Document.of(source).lineCount(), resultat.lex().lineCount());
        assertSame(XmlLexeur.State.TEXTE,
                resultat.lex().line(resultat.lex().lineCount() - 1).exitState());
    }
}
