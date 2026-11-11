package jo.celeris.serveur.java;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Les diagnostics du mini serveur : trois familles déterministes, sans
 * prétention de compilateur — l'équilibre des accolades, parenthèses
 * et crochets ; les littéraux non fermés ; le point-virgule
 * probablement manquant en fin d'instruction.
 *
 * <p>Le point-virgule reste une PISTE (gravité d'avertissement) :
 * l'heuristique exclut expressément les en-têtes de blocs
 * ({@code for}, {@code if}, {@code while}…), les annotations, les
 * fins d'énumération et toute suite qui poursuit l'instruction —
 * mieux vaut une piste juste qu'un reproche faux.</p>
 */
public final class JavaDiagnostics {

    /** Un diagnostic : la plage en décalages, la gravité, le message. */
    public record Finding(int start, int end, int severity, String message) {
    }

    /** Gravité d'erreur au protocole : 1. */
    public static final int ERROR = 1;

    /** Gravité d'avertissement au protocole : 2. */
    public static final int WARNING = 2;

    private static final List<String> BLOCK_HEADS = List.of(
            "for", "if", "while", "switch", "catch", "synchronized",
            "do", "else", "try", "return", "case", "default", "package",
            "import", "class", "interface", "enum", "record", "assert");

    private static final List<String> CONTINUATIONS = List.of(
            "{", "}", ")", "]", ",", "throws", "extends", "implements",
            "else", "catch", "finally", "&&", "||", "+", "-", "*", "/",
            "=", "?", ":", ".", "->", "::", "<", ">", "|", "&", "^",
            "new");

    private JavaDiagnostics() {
    }

    /**
     * Analyse un document : équilibre des paires, littéraux non fermés
     * (positions consignées par le dépouillement), point-virgule
     * probable.
     *
     * @param source le document suivi
     * @return les constats, dans l'ordre du document
     */
    public static List<Finding> of(JavaSource source) {
        List<Finding> constats = new ArrayList<>();
        JavaSource.StrippedCode code = source.stripped();
        constats.addAll(braces(code));
        for (int ouverture : code.unclosedLiterals()) {
            constats.add(new Finding(ouverture, ouverture + 1, WARNING,
                    "littéral non fermé avant la fin de ligne"));
        }
        constats.addAll(semicolonHints(source, code));
        constats.sort((a, b) -> Integer.compare(a.start(), b.start()));
        return List.copyOf(constats);
    }

    private static List<Finding> braces(JavaSource.StrippedCode code) {
        List<Finding> constats = new ArrayList<>();
        String texte = code.text();
        java.util.Deque<int[]> paires = new java.util.ArrayDeque<>();
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == '{' || c == '(' || c == '[') {
                paires.push(new int[] {c, i});
            } else if (c == '}' || c == ')' || c == ']') {
                if (!paires.isEmpty() && matches((char) paires.peek()[0],
                        c)) {
                    paires.pop();
                } else {
                    constats.add(new Finding(i, i + 1, ERROR,
                            fermanteSeule(c)));
                }
            }
        }
        while (!paires.isEmpty()) {
            int[] ouvrante = paires.pop();
            constats.add(new Finding(ouvrante[1], ouvrante[1] + 1, ERROR,
                    ouvranteNonFermee((char) ouvrante[0])));
        }
        return constats;
    }

    /**
     * Les pistes de point-virgule : une ligne de CODE (dépouillée de
     * ses chaînes et commentaires) qui finit sans terminateur, qui
     * n'ouvre ni ne poursuit rien, et dont la suite non vide ne
     * prolonge pas l'instruction.
     */
    private static List<Finding> semicolonHints(JavaSource source,
            JavaSource.StrippedCode code) {
        List<Finding> constats = new ArrayList<>();
        String texte = code.text();
        for (int line = 0; line < source.lineCount(); line++) {
            String content = strippedLine(texte, source, line);
            if (content.isEmpty() || content.startsWith("@")) {
                continue;
            }
            char fin = content.charAt(content.length() - 1);
            if (";{}:,.(<>=+-*/&|!?".indexOf(fin) >= 0) {
                continue;
            }
            String first = firstWord(content);
            if (BLOCK_HEADS.contains(first)) {
                continue;
            }
            String next = nextStrippedLine(texte, source, line);
            if (next != null
                    && CONTINUATIONS.stream().anyMatch(next::startsWith)) {
                continue;
            }
            int finLigne = source.lineEnd(line);
            constats.add(new Finding(finLigne - 1, finLigne, WARNING,
                    "point-virgule probablement manquant en fin "
                    + "d'instruction"));
        }
        return constats;
    }

    /** La ligne dépouillée, nettoyée des blancs de chaque bout. */
    private static String strippedLine(String texte, JavaSource source,
            int line) {
        int debut = source.lineStart(line);
        int fin = debut + source.line(line).length();
        if (debut >= texte.length()) {
            return "";
        }
        return texte.substring(debut, Math.min(fin, texte.length())).strip();
    }

    /** La première ligne non vide qui suit (dépouillée), ou null. */
    private static @Nullable String nextStrippedLine(String texte,
            JavaSource source, int line) {
        for (int suivante = line + 1; suivante < source.lineCount();
                suivante++) {
            String contenue = strippedLine(texte, source, suivante);
            if (!contenue.isEmpty()) {
                return contenue;
            }
        }
        return null;
    }

    private static String firstWord(String content) {
        int fin = 0;
        while (fin < content.length()
                && Character.isJavaIdentifierStart(content.charAt(fin))) {
            fin++;
        }
        return content.substring(0, fin);
    }

    private static boolean matches(char opener, char closer) {
        return (opener == '{' && closer == '}')
                || (opener == '(' && closer == ')')
                || (opener == '[' && closer == ']');
    }

    /** Le message d'une ouvrante restée seule : l'accord suit le
     * genre — l'accolade et la parenthèse au féminin, le crochet au
     * masculin. */
    private static String ouvranteNonFermee(char c) {
        return switch (c) {
            case '{' -> "accolade ouvrante non fermée";
            case '(' -> "parenthèse ouvrante non fermée";
            default -> "crochet ouvrant non fermé";
        };
    }

    private static String fermanteSeule(char c) {
        return switch (c) {
            case '}' -> "accolade fermante sans ouvrante";
            case ')' -> "parenthèse fermante sans ouvrante";
            default -> "crochet fermant sans ouvrant";
        };
    }
}
