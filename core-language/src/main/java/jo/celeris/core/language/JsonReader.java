package jo.celeris.core.language;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecteur JSON strict et minimal (M7.6) : objets, tableaux, chaînes
 * échappées, nombres, booléens et null — assez pour les paquets de
 * langage et les grammaires, sans dépendance externe. Les erreurs
 * lèvent {@link IllegalArgumentException} avec la position (messages
 * ASCII). Déplacé depuis la voie de grammaires (M4) : les paquets en
 * dépendent avant elle, le module du bas l'hérite.
 */
public final class JsonReader {

    /** La sentinelle du mot-clé null, jamais confondue avec une valeur. */
    private static final Object NUL = new Object();

    private final String text;

    private int position;

    private JsonReader(String text) {
        this.text = text;
    }

    /**
     * Lit une valeur JSON.
     *
     * @param text le texte JSON complet
     * @return l'objet ({@link Map}), tableau ({@link List}), chaîne,
     *         nombre ({@link Double}), booléen, ou null
     * @throws IllegalArgumentException si le texte est mal formé
     */
    public static Object read(String text) {
        return new JsonReader(text).value();
    }

    private Object value() {
        skipWhitespace();
        if (position >= text.length()) {
            throw error("unexpected end of input");
        }
        char c = text.charAt(position);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> keyword("true", true);
            case 'f' -> keyword("false", false);
            case 'n' -> keyword("null", NUL);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        position++;
        Map<String, Object> fields = new LinkedHashMap<>();
        skipWhitespace();
        if (pointsAt('}')) {
            position++;
            return fields;
        }
        while (true) {
            skipWhitespace();
            if (!pointsAt('"')) {
                throw error("expected string key");
            }
            String key = string();
            skipWhitespace();
            if (!pointsAt(':')) {
                throw error("expected ':'");
            }
            position++;
            fields.put(key, value());
            skipWhitespace();
            if (pointsAt(',')) {
                position++;
                continue;
            }
            if (pointsAt('}')) {
                position++;
                return fields;
            }
            throw error("expected ',' or '}'");
        }
    }

    private List<Object> array() {
        position++;
        List<Object> elements = new ArrayList<>();
        skipWhitespace();
        if (pointsAt(']')) {
            position++;
            return elements;
        }
        while (true) {
            elements.add(value());
            skipWhitespace();
            if (pointsAt(',')) {
                position++;
                continue;
            }
            if (pointsAt(']')) {
                position++;
                return elements;
            }
            throw error("expected ',' or ']'");
        }
    }

    private String string() {
        position++;
        StringBuilder out = new StringBuilder();
        while (position < text.length()) {
            char c = text.charAt(position++);
            if (c == '"') {
                return out.toString();
            }
            if (c == '\\') {
                out.append(escape());
            } else {
                out.append(c);
            }
        }
        throw error("unterminated string");
    }

    private char escape() {
        if (position >= text.length()) {
            throw error("unterminated escape");
        }
        char c = text.charAt(position++);
        return switch (c) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> unicode();
            default -> throw error("bad escape '\\" + c + "'");
        };
    }

    /** L'échappement unicode à quatre chiffres hexadécimaux. */
    private char unicode() {
        if (position + 4 > text.length()) {
            throw error("bad unicode escape");
        }
        char code = (char) Integer.parseInt(
                text.substring(position, position + 4), 16);
        position += 4;
        return code;
    }

    private Object number() {
        int start = position;
        while (position < text.length()
                && "+-.eE0123456789".indexOf(text.charAt(position)) >= 0) {
            position++;
        }
        if (position == start) {
            throw error("unexpected character");
        }
        String raw = text.substring(start, position);
        try {
            return Double.valueOf(raw);
        } catch (NumberFormatException e) {
            throw error("bad number '" + raw + "'");
        }
    }

    private Object keyword(String expected, Object value) {
        if (!text.startsWith(expected, position)) {
            throw error("unexpected token");
        }
        position += expected.length();
        return value;
    }

    private boolean pointsAt(char c) {
        return position < text.length() && text.charAt(position) == c;
    }

    private void skipWhitespace() {
        while (position < text.length()
                && Character.isWhitespace(text.charAt(position))) {
            position++;
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at " + position);
    }
}
