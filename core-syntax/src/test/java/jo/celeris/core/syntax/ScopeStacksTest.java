package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Les tests de l'internement des piles de portées (M7.6, H-07) : la
 * pile vit une fois, désignée par un entier ; ses portées se relisent
 * de la racine au sommet ; la sorte du jeton devient une catégorie
 * DÉRIVÉE de la portée de tête — la couleur, elle, se résout depuis
 * la pile par le thème.
 */
final class ScopeStacksTest {

    @Test
    void unePileVitUneFoisEtSePartage() {
        int racine = ScopeStacks.root("source.demo");
        int premiere = ScopeStacks.push(racine, "meta.bloc");
        int seconde = ScopeStacks.push(racine, "meta.bloc");
        assertEquals(premiere, seconde,
                "même pile, même entier — l'internement partage");
        assertEquals(List.of("source.demo", "meta.bloc"),
                ScopeStacks.scopesOf(premiere));
    }

    @Test
    void lesPorteesSeRelisentDeLaRacineAuSommet() {
        int pile = ScopeStacks.push(
                ScopeStacks.push(ScopeStacks.root("text.html"), "meta.tag"),
                "entity.name.tag");
        assertEquals(List.of("text.html", "meta.tag", "entity.name.tag"),
                ScopeStacks.scopesOf(pile));
    }

    @Test
    void laPileVideNEmpilleRien() {
        assertEquals(ScopeStacks.NONE, ScopeStacks.push(ScopeStacks.NONE, ""));
        assertEquals(ScopeStacks.NONE, ScopeStacks.push(ScopeStacks.NONE, null));
        assertEquals(List.of(), ScopeStacks.scopesOf(ScopeStacks.NONE));
    }

    @Test
    void laSorteSeDeriveDeLaPorteeDeTete() {
        assertEquals(TokenKind.COMMENT,
                ScopeStacks.kindOf(ScopeStacks.push(
                        ScopeStacks.root("source.py"), "comment.line")));
        assertEquals(TokenKind.DOC,
                ScopeStacks.kindOf(ScopeStacks.push(
                        ScopeStacks.root("source.py"),
                        "comment.block.documentation.python")));
        assertEquals(TokenKind.NUMBER,
                ScopeStacks.kindOf(ScopeStacks.push(
                        ScopeStacks.root("source.py"), "constant.numeric")));
        assertEquals(TokenKind.OPERATOR,
                ScopeStacks.kindOf(ScopeStacks.push(
                        ScopeStacks.root("source.py"), "keyword.operator")));
        assertEquals(TokenKind.TYPE,
                ScopeStacks.kindOf(ScopeStacks.push(
                        ScopeStacks.root("source.py"), "entity.name.type")));
        assertEquals(TokenKind.OPEN_BRACKET,
                ScopeStacks.kindOf(ScopeStacks.push(
                        ScopeStacks.root("source.py"),
                        "punctuation.section.string.begin")));
        assertEquals(TokenKind.CLOSE_BRACKET,
                ScopeStacks.kindOf(ScopeStacks.push(
                        ScopeStacks.root("source.py"),
                        "punctuation.section.string.end")));
        assertEquals(TokenKind.ANNOTATION,
                ScopeStacks.kindOf(ScopeStacks.push(
                        ScopeStacks.root("source.py"), "meta.annotation")));
        assertEquals(TokenKind.ERROR,
                ScopeStacks.kindOf(ScopeStacks.push(
                        ScopeStacks.root("source.py"), "invalid.illegal")));
        assertEquals(TokenKind.IDENTIFIER,
                ScopeStacks.kindOf(ScopeStacks.push(
                        ScopeStacks.root("source.py"), "meta.struct")));
    }

    @Test
    void laProfondeurSeBorne() {
        int pile = ScopeStacks.root("source.profond");
        for (int i = 0; i < 200; i++) {
            pile = ScopeStacks.push(pile, "meta.niveau" + (i % 4));
        }
        assertTrue(ScopeStacks.scopesOf(pile).size() <= 65,
                "la pile se borne en profondeur, la mémoire avec elle : "
                        + ScopeStacks.scopesOf(pile).size());
    }
}
