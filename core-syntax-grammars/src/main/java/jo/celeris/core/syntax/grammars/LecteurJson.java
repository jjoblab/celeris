package jo.celeris.core.syntax.grammars;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Lecteur JSON strict et minimal pour les grammaires : objets, tableaux,
 * chaînes échappées, nombres, booléens et null — assez pour le format
 * déclaratif, sans dépendance externe. Les erreurs lèvent
 * {@link IllegalArgumentException} avec la position (messages ASCII).
 */
final class LecteurJson {

    /** La sentinelle du mot-clé null — les grammaires ne s'en servent pas. */
    private static final Object NUL = new Object();

    private final String texte;
    private int position;

    private LecteurJson(String texte) {
        this.texte = texte;
    }

    /**
     * Lit une valeur JSON.
     *
     * @param texte le texte JSON complet
     * @return l'objet, tableau, chaîne, nombre ou booléen lus
     * @throws IllegalArgumentException si le texte est mal formé
     */
    static Object lire(String texte) {
        return new LecteurJson(texte).valeur();
    }

    private Object valeur() {
        sauterBlancs();
        if (position >= texte.length()) {
            throw erreur("unexpected end of input");
        }
        char c = texte.charAt(position);
        return switch (c) {
            case '{' -> objet();
            case '[' -> tableau();
            case '"' -> chaine();
            case 't' -> mot("true", true);
            case 'f' -> mot("false", false);
            case 'n' -> mot("null", NUL);
            default -> nombre();
        };
    }

    private Map<String, Object> objet() {
        position++;
        Map<String, Object> champs = new LinkedHashMap<>();
        sauterBlancs();
        if (pointe('}')) {
            position++;
            return champs;
        }
        while (true) {
            sauterBlancs();
            if (!pointe('"')) {
                throw erreur("expected string key");
            }
            String cle = chaine();
            sauterBlancs();
            if (!pointe(':')) {
                throw erreur("expected ':'");
            }
            position++;
            champs.put(cle, valeur());
            sauterBlancs();
            if (pointe(',')) {
                position++;
                continue;
            }
            if (pointe('}')) {
                position++;
                return champs;
            }
            throw erreur("expected ',' or '}'");
        }
    }

    private List<Object> tableau() {
        position++;
        List<Object> elements = new ArrayList<>();
        sauterBlancs();
        if (pointe(']')) {
            position++;
            return elements;
        }
        while (true) {
            elements.add(valeur());
            sauterBlancs();
            if (pointe(',')) {
                position++;
                continue;
            }
            if (pointe(']')) {
                position++;
                return elements;
            }
            throw erreur("expected ',' or ']'");
        }
    }

    private String chaine() {
        position++;
        StringBuilder sortie = new StringBuilder();
        while (position < texte.length()) {
            char c = texte.charAt(position++);
            if (c == '"') {
                return sortie.toString();
            }
            if (c == '\\') {
                sortie.append(echappement());
            } else {
                sortie.append(c);
            }
        }
        throw erreur("unterminated string");
    }

    private char echappement() {
        if (position >= texte.length()) {
            throw erreur("unterminated escape");
        }
        char c = texte.charAt(position++);
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
            default -> throw erreur("bad escape '\\" + c + "'");
        };
    }

    /** L'échappement unicode à quatre chiffres hexadécimaux. */
    private char unicode() {
        if (position + 4 > texte.length()) {
            throw erreur("bad unicode escape");
        }
        char code = (char) Integer.parseInt(
                texte.substring(position, position + 4), 16);
        position += 4;
        return code;
    }

    private Object nombre() {
        int debut = position;
        while (position < texte.length()
                && "+-.eE0123456789".indexOf(texte.charAt(position)) >= 0) {
            position++;
        }
        if (position == debut) {
            throw erreur("unexpected character");
        }
        String brut = texte.substring(debut, position);
        try {
            return Double.valueOf(brut);
        } catch (NumberFormatException e) {
            throw erreur("bad number '" + brut + "'");
        }
    }

    private Object mot(String attendu, Object valeur) {
        if (!texte.startsWith(attendu, position)) {
            throw erreur("unexpected token");
        }
        position += attendu.length();
        return valeur;
    }

    private boolean pointe(char c) {
        return position < texte.length() && texte.charAt(position) == c;
    }

    private void sauterBlancs() {
        while (position < texte.length()
                && Character.isWhitespace(texte.charAt(position))) {
            position++;
        }
    }

    private IllegalArgumentException erreur(String message) {
        return new IllegalArgumentException(message + " at " + position);
    }
}
