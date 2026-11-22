package jo.celeris.core.syntax.treesitter;

import java.util.HashMap;
import java.util.Map;

import jo.celeris.core.syntax.TokenKind;

/**
 * Les captures de surbrillance du chantier D (prompt du
 * 2026-11-22) : les requêtes {@code highlights.scm} capturent des
 * nœuds nommés ({@code @keyword}, {@code @function}, {@code
 * @type}, {@code @variable.parameter}…) — la carte ici traduit une
 * capture vers le {@link TokenKind} du socle, la teinte du thème.
 *
 * <p>La table couvre les captures des requêtes de référence
 * (modèle tree-sitter : la convention des captures des grammaires
 * publiées). Les captures inconnues retombent sur IDENTIFIER : la
 * requête peut évoluer sans casser la teinte, le nom de la capture
 * reste lu.</p>
 */
final class CapturesSurbrillance {

    /** La capture vers sa teinte — les conventions de référence. */
    private static final Map<String, TokenKind> CAPTURES = Map.ofEntries(
            // Mots-clés — le langage réserve.
            Map.entry("keyword", TokenKind.KEYWORD),
            Map.entry("keyword.function", TokenKind.KEYWORD),
            Map.entry("keyword.operator", TokenKind.OPERATOR),
            Map.entry("keyword.control", TokenKind.KEYWORD),
            Map.entry("keyword.return", TokenKind.KEYWORD),
            Map.entry("keyword.import", TokenKind.KEYWORD),
            // Fonctions — l'appel, la définition.
            Map.entry("function", TokenKind.IDENTIFIER),
            Map.entry("function.method", TokenKind.IDENTIFIER),
            Map.entry("function.builtin", TokenKind.IDENTIFIER),
            Map.entry("function.call", TokenKind.IDENTIFIER),
            Map.entry("function.definition", TokenKind.IDENTIFIER),
            // Types — la classe, l'interface, le type primitif.
            Map.entry("type", TokenKind.IDENTIFIER),
            Map.entry("type.builtin", TokenKind.IDENTIFIER),
            Map.entry("type.definition", TokenKind.IDENTIFIER),
            Map.entry("class", TokenKind.IDENTIFIER),
            Map.entry("type.identifier", TokenKind.IDENTIFIER),
            // Variables — la portée distingue le champ du paramètre.
            Map.entry("variable", TokenKind.IDENTIFIER),
            Map.entry("variable.builtin", TokenKind.IDENTIFIER),
            Map.entry("variable.other", TokenKind.IDENTIFIER),
            Map.entry("variable.parameter", TokenKind.IDENTIFIER),
            Map.entry("variable.field", TokenKind.IDENTIFIER),
            // Constantes et nombres.
            Map.entry("constant", TokenKind.NUMBER),
            Map.entry("constant.builtin", TokenKind.KEYWORD),
            Map.entry("constant.character", TokenKind.STRING),
            Map.entry("number", TokenKind.NUMBER),
            // Chaînes, commentaires, attributs.
            Map.entry("string", TokenKind.STRING),
            Map.entry("string.special", TokenKind.STRING),
            Map.entry("comment", TokenKind.COMMENT),
            Map.entry("doc", TokenKind.COMMENT),
            Map.entry("attribute", TokenKind.ANNOTATION),
            Map.entry("decorator", TokenKind.ANNOTATION),
            // Opérateurs et ponctuation.
            Map.entry("operator", TokenKind.OPERATOR),
            Map.entry("punctuation", TokenKind.PUNCTUATION),
            Map.entry("punctuation.bracket", TokenKind.OPEN_BRACKET),
            Map.entry("punctuation.delimiter", TokenKind.PUNCTUATION),
            // Les constructions littérales.
            Map.entry("embedded", TokenKind.STRING),
            Map.entry("label", TokenKind.KEYWORD),
            Map.entry("tag", TokenKind.IDENTIFIER),
            Map.entry("escape", TokenKind.STRING));

    /** Les captures composées déjà servies — la carte des lectures. */
    private final Map<String, TokenKind> locales = new HashMap<>();

    /** La teinte d'une capture — IDENTIFIER pour l'inconnue (la
     *  requête évolue sans casser la teinte, le nom reste lu). */
    TokenKind teinte(String capture) {
        TokenKind directe = CAPTURES.get(capture);
        if (directe != null) {
            return directe;
        }
        // La capture composée (variable.parameter) : le PREMIER
        // segment porte la famille — variable/keyword/comment y
        // suffisent, le suffixe affine ailleurs.
        return locales.computeIfAbsent(capture, nom -> {
            int point = nom.indexOf('.');
            if (point > 0) {
                TokenKind famille = CAPTURES.get(nom.substring(0, point));
                if (famille != null) {
                    return famille;
                }
            }
            return TokenKind.IDENTIFIER;
        });
    }
}
