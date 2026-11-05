package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.TokenKind;

/**
 * Le mapping portées → sortes de jetons (voie héritée du dépôt de
 * départ, ADR-004) : les portées pointées du format TextMate se
 * rabattent sur les quatorze sortes communes — la coloration d'une
 * grammaire vit dans les mêmes paniers que celle des analyseurs
 * dédiés, la palette n'en connaît qu'une.
 */
class PorteesTest {

    /** Les commentaires se distinguent de leur documentation. */
    @Test
    void commentaires() {
        assertEquals(TokenKind.COMMENT, Portees.de("comment.line.number-sign.toml"));
        assertEquals(TokenKind.COMMENT, Portees.de("comment.block.python"));
        assertEquals(TokenKind.DOC, Portees.de("comment.block.documentation.java"));
    }

    /** Chaînes, nombres et constantes. */
    @Test
    void scalaires() {
        assertEquals(TokenKind.STRING, Portees.de("string.quoted.double.toml"));
        assertEquals(TokenKind.STRING, Portees.de("string.unquoted.value.properties"));
        assertEquals(TokenKind.NUMBER, Portees.de("constant.numeric.integer.toml"));
        assertEquals(TokenKind.NUMBER, Portees.de("constant.numeric.hex.python"));
        assertEquals(TokenKind.CONSTANT, Portees.de("constant.language.python"));
        assertEquals(TokenKind.CONSTANT, Portees.de("constant.character.escape.toml"));
    }

    /** Mots réservés contre opérateurs. */
    @Test
    void mots() {
        assertEquals(TokenKind.KEYWORD, Portees.de("keyword.control.python"));
        assertEquals(TokenKind.KEYWORD, Portees.de("storage.modifier.block-scalar.yaml"));
        assertEquals(TokenKind.OPERATOR, Portees.de("keyword.operator.assignment.python"));
        assertEquals(TokenKind.ANNOTATION, Portees.de("meta.annotation.decorator.python"));
    }

    /** Entités nommées : types, sections, balises se distinguent. */
    @Test
    void entites() {
        assertEquals(TokenKind.TYPE, Portees.de("entity.name.type.python"));
        assertEquals(TokenKind.TYPE, Portees.de("entity.name.section.table.toml"));
        assertEquals(TokenKind.TYPE, Portees.de("entity.name.tag.xml"));
        assertEquals(TokenKind.IDENTIFIER, Portees.de("entity.name.function.python"));
        assertEquals(TokenKind.IDENTIFIER, Portees.de("entity.name.key.yaml"));
        assertEquals(TokenKind.IDENTIFIER, Portees.de("variable.other.key.toml"));
    }

    /** La ponctuation : les sections début/fin portent les crochets appariés. */
    @Test
    void ponctuation() {
        assertEquals(TokenKind.OPEN_BRACKET,
                Portees.de("punctuation.section.array.begin.toml"));
        assertEquals(TokenKind.CLOSE_BRACKET,
                Portees.de("punctuation.section.array.end.toml"));
        assertEquals(TokenKind.OPEN_BRACKET,
                Portees.de("punctuation.section.fenced.begin.markdown"));
        assertEquals(TokenKind.PUNCTUATION,
                Portees.de("punctuation.separator.key-value.toml"));
        assertEquals(TokenKind.PUNCTUATION, Portees.de("punctuation.definition.string.begin"));
    }

    /** Le balisage léger : brut en encre de chaîne, citation en commentaire. */
    @Test
    void balisage() {
        assertEquals(TokenKind.STRING, Portees.de("markup.raw.block.markdown"));
        assertEquals(TokenKind.STRING, Portees.de("markup.underline.link.markdown"));
        assertEquals(TokenKind.COMMENT, Portees.de("markup.quote.markdown"));
        assertEquals(TokenKind.TYPE, Portees.de("markup.heading.markdown"));
        assertEquals(TokenKind.CONSTANT, Portees.de("markup.bold.markdown"));
    }

    /** Le reste retombe sur l'identificateur. */
    @Test
    void repli() {
        assertEquals(TokenKind.IDENTIFIER, Portees.de("meta.plain"));
        assertEquals(TokenKind.IDENTIFIER, Portees.de("source.toml"));
        assertEquals(TokenKind.IDENTIFIER, Portees.de("inconnu.sous.portee"));
    }
}
