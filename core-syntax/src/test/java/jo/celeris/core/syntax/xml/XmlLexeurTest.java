package jo.celeris.core.syntax.xml;

import jo.celeris.core.syntax.CorpusChargeur;

import jo.celeris.core.syntax.IncrementalLexer;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LexResult;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private Token[] lex(String ligne) {
        return lexeur.lex(ligne, XmlLexeur.Etat.TEXTE).tokens();
    }

    /** Le texte du jeton relu dans sa ligne (le jeton ne porte que ses bornes). */
    private String texte(String ligne, Token jeton) {
        return ligne.substring(jeton.start(), jeton.end());
    }

    /** Le prologue «&#x2f;?xml … ?&#x2f;» est un jeton de documentation. */
    @Test
    void prologue() {
        Token[] jetons = lex("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        assertEquals(1, jetons.length);
        assertEquals(TokenKind.DOC, jetons[0].kind());
    }

    /** La balise ouvrante porte son nom, ses attributs, son chevron final. */
    @Test
    void baliseOuvranteEtAttributs() {
        String ligne = "<produit id=\"a1\" prix=\"12,50\">";
        Token[] jetons = lex(ligne);
        assertEquals(8, jetons.length);
        assertEquals(TokenKind.OPEN_BRACKET, jetons[0].kind());
        assertEquals("<produit", texte(ligne, jetons[0]));
        assertEquals(TokenKind.IDENTIFIER, jetons[1].kind());
        assertEquals("id", texte(ligne, jetons[1]));
        assertEquals(TokenKind.OPERATOR, jetons[2].kind());
        assertEquals(TokenKind.STRING, jetons[3].kind());
        assertEquals("\"a1\"", texte(ligne, jetons[3]));
        assertEquals(TokenKind.STRING, jetons[6].kind());
        assertEquals("\"12,50\"", texte(ligne, jetons[6]));
        assertEquals(TokenKind.PUNCTUATION, jetons[7].kind());
        assertEquals(">", texte(ligne, jetons[7]));
    }

    /** La fermeture porte le nom ; l'auto-fermeture est un crochet fermant. */
    @Test
    void fermetureEtAutoFermeture() {
        assertEquals(TokenKind.CLOSE_BRACKET, lex("</produit>")[0].kind());
        assertEquals("</produit>", texte("</produit>", lex("</produit>")[0]));
        String ligneBrute = "<rupture/>";
        Token[] self = lex(ligneBrute);
        assertEquals(TokenKind.OPEN_BRACKET, self[0].kind());
        assertEquals(TokenKind.CLOSE_BRACKET, self[1].kind());
        assertEquals("/>", texte(ligneBrute, self[1]));
    }

    /** Le texte entre balises est un jeton, entités comprises. */
    @Test
    void texteEtEntites() {
        String ligne = "Auteurs &amp; contributeurs — « Celeris »";
        Token[] jetons = lex(ligne);
        assertEquals(1, jetons.length);
        assertEquals(TokenKind.STRING, jetons[0].kind());
        assertEquals(ligne, texte(ligne, jetons[0]));
    }

    /** Le texte s'arrête au «&#x2f;» qui ouvre la balise suivante. */
    @Test
    void textePuisBalise() {
        String ligne = "<nom>Livre « Unicode »</nom>";
        Token[] jetons = lex(ligne);
        assertEquals(4, jetons.length);
        assertEquals(TokenKind.OPEN_BRACKET, jetons[0].kind());
        assertEquals(TokenKind.PUNCTUATION, jetons[1].kind());
        assertEquals(TokenKind.STRING, jetons[2].kind());
        assertEquals("Livre « Unicode »", texte("<nom>Livre « Unicode »</nom>", jetons[2]));
        assertEquals(TokenKind.CLOSE_BRACKET, jetons[3].kind());
    }

    /** Le chemin Windows en valeur d'attribut se ferme : l'antislash
     * n'échappe rien en XML — le guillemet après les antislashs ferme
     * la valeur, la cascade d'état ne dérive plus. */
    @Test
    void leCheminWindowsEnAttributSeFerme() {
        String ligne = "<projet chemin=\"C:\\Users\\celeris\">";
        Token[] jetons = lex(ligne);
        assertEquals(TokenKind.STRING, jetons[3].kind());
        assertEquals("\"C:\\Users\\celeris\"", texte(ligne, jetons[3]));
        assertEquals(">", texte(ligne, jetons[jetons.length - 1]));

        String enTraine = "<a racine=\"C:\\\">";
        Token[] traine = lex(enTraine);
        assertEquals(TokenKind.STRING, traine[3].kind(),
                "l'antislash final ne saute pas le guillemet fermant");
        assertEquals("\"C:\\\"", texte(enTraine, traine[3]));
    }

    /** Un commentaire ouvert traverse les lignes par l'état, puis reprend. */
    @Test
    void commentaireMultiLignes() {
        LexedLine premiere = lexeur.lex("<!-- ouverture", XmlLexeur.Etat.TEXTE);
        assertSame(XmlLexeur.Etat.COMMENTAIRE, premiere.exitState());
        LexedLine fermeture = lexeur.lex("fin --> <suite/>", premiere.exitState());
        assertEquals(TokenKind.COMMENT, fermeture.tokens()[0].kind());
        assertEquals(TokenKind.OPEN_BRACKET, fermeture.tokens()[1].kind());
        assertSame(XmlLexeur.Etat.TEXTE, fermeture.exitState());
    }

    /** La section CDATA ouvre un crochet apparié à sa fermeture. */
    @Test
    void cdataMultiLignes() {
        LexedLine ouverture = lexeur.lex("<d><![CDATA[brut <balise> &",
                XmlLexeur.Etat.TEXTE);
        assertSame(XmlLexeur.Etat.CDATA, ouverture.exitState());
        assertEquals(TokenKind.OPEN_BRACKET, ouverture.tokens()[2].kind());
        LexedLine milieu = lexeur.lex("encore du brut", ouverture.exitState());
        assertEquals(1, milieu.tokens().length);
        assertEquals(TokenKind.STRING, milieu.tokens()[0].kind());
        LexedLine fermeture = lexeur.lex("fin]]> </d>", ouverture.exitState());
        assertEquals(TokenKind.STRING, fermeture.tokens()[0].kind());
        assertEquals(TokenKind.CLOSE_BRACKET, fermeture.tokens()[1].kind());
        assertEquals("]]>", texte("fin]]> </d>", fermeture.tokens()[1]));
        assertEquals(TokenKind.CLOSE_BRACKET, fermeture.tokens()[2].kind());
        assertSame(XmlLexeur.Etat.TEXTE, fermeture.exitState());
    }

    /** Une balise ouverte traverse les lignes : les attributs continuent. */
    @Test
    void baliseMultiLignes() {
        LexedLine premiere = lexeur.lex("<produit id=\"a1\"",
                XmlLexeur.Etat.TEXTE);
        assertSame(XmlLexeur.Etat.BALISE, premiere.exitState());
        LexedLine suite = lexeur.lex("prix=\"8,90\">", premiere.exitState());
        assertEquals(TokenKind.IDENTIFIER, suite.tokens()[0].kind());
        assertEquals(TokenKind.OPERATOR, suite.tokens()[1].kind());
        assertEquals(TokenKind.STRING, suite.tokens()[2].kind());
        assertEquals(TokenKind.PUNCTUATION, suite.tokens()[3].kind());
        assertSame(XmlLexeur.Etat.TEXTE, suite.exitState());
    }

    /** Une valeur d'attribut non fermée donne un jeton ERROR, pas d'exception. */
    @Test
    void attributNonFerme() {
        Token[] jetons = lex("<a v=\"sans fin>");
        assertEquals(TokenKind.ERROR, jetons[3].kind());
    }

    /** Les noms de balises accueillent les lettres non ASCII. */
    @Test
    void nomsNonAscii() {
        String ligne = "<méta>français</méta>";
        Token[] jetons = lex(ligne);
        assertEquals(TokenKind.OPEN_BRACKET, jetons[0].kind());
        assertEquals("<méta", texte(ligne, jetons[0]));
        assertEquals(TokenKind.PUNCTUATION, jetons[1].kind());
        assertEquals(TokenKind.STRING, jetons[2].kind());
        assertEquals(TokenKind.CLOSE_BRACKET, jetons[3].kind());
        assertEquals("</méta>", texte(ligne, jetons[3]));
    }

    /** Le lexique complet du corpus tient sans incident. */
    @Test
    void lexiqueComplet() {
        String source = CorpusChargeur.exempleXml();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult resultat = moteur.lexFull(Document.of(source), lexeur);
        assertEquals(Document.of(source).lineCount(), resultat.lex().lineCount());
        assertSame(XmlLexeur.Etat.TEXTE,
                resultat.lex().line(resultat.lex().lineCount() - 1).exitState());
    }
}
