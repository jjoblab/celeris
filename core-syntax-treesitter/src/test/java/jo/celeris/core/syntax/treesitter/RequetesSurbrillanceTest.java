package jo.celeris.core.syntax.treesitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.TokenKind;

/**
 * Le banc du chantier D (prompt du 2026-11-22) : les requêtes de
 * surbrillance se LISENT et traduisent leurs captures — le lecteur
 * nourrit la teinte des feuilles de la matérialisation sans la
 * table de mots codée en dur. Le banc vit sur JVM (la lecture est
 * pure, la liaison n'y paraît pas) : les arbres synthétiques du
 * banc de matérialisation portent le reste du chemin.
 */
class RequetesSurbrillanceTest {

    /** Un extrait REALISTE de highlights.scm (formes réelles). */
    private static final String EXTRAIT = """
            ; Les fonctions et les champs
            (method_declaration
              name: (identifier) @function.method)
            (function_definition
              name: (identifier) @function)

            ; Les types
            (type_identifier) @type
            (class_definition
              name: (identifier) @class)

            ; Les littéraux
            "if" @keyword
            "return" @keyword.return
            "true" @constant.builtin
            (string) @string
            (comment) @comment
            (integer) @number
            (decorator) @decorator
            (escape_sequence) @escape
            """;

    /** Les entrées se lisent dans l'ordre du texte, capture comprise. */
    @Test
    void lesEntreesSeLisentAvecLeursCaptures() {
        List<LecteurRequetes.Entree> entrees =
                LecteurRequetes.lire(EXTRAIT);
        assertTrue(entrees.size() >= 12,
                "l'extrait rend au moins 12 entrées : " + entrees.size());
        assertEquals("identifier", entrees.get(0).noeud());
        assertEquals("function.method", entrees.get(0).capture());
        assertEquals("identifier", entrees.get(1).noeud());
        assertEquals("function", entrees.get(1).capture());
        assertEquals("type_identifier", entrees.get(2).noeud());
        assertEquals("type", entrees.get(2).capture());
        // La capture de class_definition s'attache au nom capturé.
        assertEquals("identifier", entrees.get(3).noeud());
        assertEquals("class", entrees.get(3).capture());
        // Les littéraux gardent leur texte comme nom.
        assertEquals("if", entrees.get(4).noeud());
        assertEquals("keyword", entrees.get(4).capture());
        assertEquals("return", entrees.get(5).noeud());
        assertEquals("keyword.return", entrees.get(5).capture());
        // Les motifs atomiques se lisent aussi.
        assertEquals("string", entrees.get(7).noeud());
        assertEquals("string", entrees.get(7).capture());
        assertEquals("integer", entrees.get(9).noeud());
        assertEquals("number", entrees.get(9).capture());
    }

    /** Les captures se traduisent : la teinte du socle, les
     *  composées par leur famille, l'inconnue en identifiant. */
    @Test
    void lesCapturesSeTraduisentEnTeintes() {
        CapturesSurbrillance captures = new CapturesSurbrillance();
        assertEquals(TokenKind.KEYWORD, captures.teinte("keyword"));
        assertEquals(TokenKind.COMMENT, captures.teinte("comment"));
        assertEquals(TokenKind.STRING, captures.teinte("string"));
        assertEquals(TokenKind.NUMBER, captures.teinte("number"));
        assertEquals(TokenKind.ANNOTATION, captures.teinte("decorator"));
        // La composée : la famille porte.
        assertEquals(TokenKind.KEYWORD, captures.teinte("keyword.return"));
        assertEquals(TokenKind.IDENTIFIER,
                captures.teinte("variable.parameter"));
        // L'inconnue : l'identifiant (la requête évolue sans casser).
        assertEquals(TokenKind.IDENTIFIER, captures.teinte("novelle.capture"));
    }
}
