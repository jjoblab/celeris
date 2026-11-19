package jo.celeris.sample.lsp;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.lsp.types.CompletionItemLsp;
import jo.celeris.core.lsp.types.CompletionListLsp;
import jo.celeris.core.lsp.types.LspPosition;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.HighlightLsp;
import jo.celeris.core.lsp.types.HoverLsp;
import jo.celeris.core.lsp.types.SymbolLsp;
import jo.celeris.core.lsp.types.RangeLsp;

/**
 * La complétion, les signatures, le survol, les symboles et les
 * occurrences du mini Java — tous lus du MÊME balayage lexical :
 * identifiants, littéraux de chaîne et mots de la ligne. La
 * complétion sert le contexte : mots réservés toujours, membres de
 * String derrière un point, méthodes et variables du document.
 */
final class CompletionJava {

    private CompletionJava() {
    }

    /** Les mots réservés du Java — la base de la complétion. */
    private static final List<String> MOTS_RESERVES = List.of(
            "abstract", "assert", "boolean", "break", "byte", "case",
            "catch", "char", "class", "const", "continue", "default",
            "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long",
            "native", "new", "package", "private", "protected",
            "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "true",
            "false", "null");

    /** Les membres de String les plus servis — après un point. */
    private static final List<CompletionItemLsp> MEMBRES_STRING = List.of(
            membre("length()", "int", "La longueur de la chaîne."),
            membre("charAt(int index)", "char",
                    "Le caractère à l'index donné."),
            membre("substring(int debut)", "String",
                    "La sous-chaîne depuis le début donné."),
            membre("substring(int debut, int fin)", "String",
                    "La sous-chaîne bornée."),
            membre("indexOf(String autre)", "int",
                    "La première occurrence de l'autre chaîne, -1 sinon."),
            membre("equals(Object autre)", "boolean",
                    "Vrai quand les chaînes portent les mêmes caractères."),
            membre("equalsIgnoreCase(String autre)", "boolean",
                    "L'égalité sans égard à la casse."),
            membre("toUpperCase()", "String", "La copie en majuscules."),
            membre("toLowerCase()", "String", "La copie en minuscules."),
            membre("trim()", "String", "La copie sans les blancs des bords."),
            membre("isEmpty()", "boolean", "Vrai quand la chaîne est vide."),
            membre("replace(CharSequence cible, CharSequence remplacement)",
                    "String", "La copie remplacée."),
            membre("split(String regex)", "String[]",
                    "Les morceaux coupés à la regex."),
            membre("contains(CharSequence autre)", "boolean",
                    "Vrai quand l'autre séquence y vit."),
            membre("startsWith(String prefixe)", "boolean",
                    "Vrai quand la chaîne commence par le préfixe."),
            membre("endsWith(String suffixe)", "boolean",
                    "Vrai quand la chaîne finit par le suffixe."));

    /** Un membre de complétion (genre 2 = méthode du protocole). */
    private static CompletionItemLsp membre(String label, String detail,
            String documentation) {
        return new CompletionItemLsp(label, 2, detail, documentation,
                null, null, null, false);
    }

    /** Les identifiants déclarés du document : variables et méthodes
     *  lues des têtes de déclaration. */
    static List<String> declarations(String text) {
        List<String> noms = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\b(?:[A-Za-z_][\\w<>\\[\\], .]*)\\s+"
                        + "([a-z_][\\w]*)\\s*[=;(]")
                .matcher(text);
        while (matcher.find()) {
            String nom = matcher.group(1);
            if (!noms.contains(nom)) {
                noms.add(nom);
            }
        }
        return noms;
    }

    /** La complétion au caret : le mot tapé se lit en remontant aux
     *  bornes de l'identifiant ; un point devant sert les membres de
     *  String, sinon les mots réservés et les déclarations du
     *  document. */
    static CompletionListLsp completion(String text, int caret) {
        int debutMot = debutDuMot(text, caret);
        String prefixe = text.substring(debutMot, caret);
        CompletionListLsp membres = membresDuReceveur(text, debutMot, prefixe);
        if (membres != null) {
            return membres;
        }
        return baseDuDocument(text, prefixe);
    }

    /** L'offset du début du mot tapé, en remontant aux bornes de
     *  l'identifiant. */
    private static int debutDuMot(String text, int caret) {
        int debutMot = caret;
        while (debutMot > 0
                && Character.isJavaIdentifierPart(text.charAt(debutMot - 1))) {
            debutMot--;
        }
        return debutMot;
    }

    /** Les membres de String derrière un point — le récepteur lu du
     *  mot avant le point ; null quand la complétion de base se sert. */
    private static @org.jspecify.annotations.Nullable CompletionListLsp
            membresDuReceveur(String text, int debutMot, String prefixe) {
        boolean apresPoint = debutMot > 0 && text.charAt(debutMot - 1) == '.';
        if (!apresPoint) {
            return null;
        }
        int finReceveur = debutMot - 1;
        int debutReceveur = finReceveur;
        while (debutReceveur > 0 && text.charAt(debutReceveur - 1) != '"'
                && Character.isJavaIdentifierPart(
                        text.charAt(debutReceveur - 1))) {
            debutReceveur--;
        }
        String receveur = text.substring(debutReceveur, finReceveur);
        boolean chaine = debutReceveur > 0
                && text.charAt(debutReceveur - 1) == '"';
        if (!chaine && !"nom".equals(receveur) && !"texte".equals(receveur)) {
            return null;
        }
        List<CompletionItemLsp> elements = new ArrayList<>();
        for (CompletionItemLsp membre : MEMBRES_STRING) {
            if (membre.label().startsWith(prefixe)) {
                elements.add(membre);
            }
        }
        return CompletionListLsp.of(false, elements);
    }

    /** La base du document : mots réservés et déclarations du fichier. */
    private static CompletionListLsp baseDuDocument(String text, String prefixe) {
        List<CompletionItemLsp> elements = new ArrayList<>();
        for (String reserve : MOTS_RESERVES) {
            if (reserve.startsWith(prefixe)) {
                elements.add(new CompletionItemLsp(reserve, 14,
                        "mot réservé", null, null, null, null, false));
            }
        }
        for (String nom : declarations(text)) {
            if (nom.startsWith(prefixe) && !nom.equals(prefixe)) {
                elements.add(new CompletionItemLsp(nom, 6,
                        "déclaré dans ce fichier", null, null, null, null,
                        false));
            }
        }
        return CompletionListLsp.of(false, elements);
    }

    /** L'aide aux signatures : l'appel ouvert devant le caret —
     *  println et print du System.out, les méthodes du document. */
    static SignatureLsp signature(String text, int caret) {
        // l'appel ouvert : remonter au « ( » non fermé le plus proche
        int parenthese = parentheseOuverte(text, caret);
        if (parenthese < 0) {
            return null;
        }
        int debutMot = parenthese;
        while (debutMot > 0 && (Character.isJavaIdentifierPart(
                text.charAt(debutMot - 1)) || text.charAt(debutMot - 1) == '.')) {
            debutMot--;
        }
        String nom = text.substring(debutMot, parenthese);
        int parametreActif = 0;
        for (int i = parenthese + 1; i < caret && i < text.length(); i++) {
            if (text.charAt(i) == ',') {
                parametreActif++;
            }
        }
        if (nom.endsWith("println")) {
            return SignatureLsp.of(List.of(
                    entreePrintln("println(Object x)", "x",
                            "ce qui s'imprime, puis le saut de ligne",
                            "Impression avec saut de ligne."),
                    entreePrintln("println(String s)", "s",
                            "la chaîne imprimée",
                            "Impression d'une chaîne, saut de ligne.")),
                    0, parametreActif);
        }
        if (nom.endsWith("print")) {
            return SignatureLsp.of(List.of(entreePrintln(
                    "print(Object x)", "x",
                    "ce qui s'imprime, sans saut de ligne",
                    "Impression sans saut de ligne.")),
                    0, parametreActif);
        }
        return null;
    }

    /** L'offset du « ( » non fermé qui porte le caret, -1 sans appel ouvert. */
    private static int parentheseOuverte(String text, int caret) {
        int profondeur = 0;
        for (int i = caret - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == ')') {
                profondeur++;
            } else if (c == '(') {
                if (profondeur == 0) {
                    return i;
                }
                profondeur--;
            } else if (c == ';' || c == '{' || c == '}') {
                return -1;
            }
        }
        return -1;
    }

    /** Une entrée de signature d'impression. */
    private static SignatureLsp.Entry entreePrintln(String label,
            String parametre, String documentationParametre,
            String documentation) {
        return new SignatureLsp.Entry(label,
                List.of(new SignatureLsp.Parameter(parametre,
                        documentationParametre)),
                documentation);
    }

    /** Le survol : la déclaration locale du mot sous la position. */
    static HoverLsp hover(String text, LspPosition position) {
        int[] ligneColonne = offsetDe(text, position);
        if (ligneColonne == null) {
            return null;
        }
        int debutMot = ligneColonne[0];
        while (debutMot > 0 && Character.isJavaIdentifierPart(
                text.charAt(debutMot - 1))) {
            debutMot--;
        }
        int finMot = ligneColonne[0];
        while (finMot < text.length()
                && Character.isJavaIdentifierPart(text.charAt(finMot))) {
            finMot++;
        }
        if (finMot <= debutMot) {
            return null;
        }
        String mot = text.substring(debutMot, finMot);
        // le mot apparaît-il en tête de déclaration ?
        java.util.regex.Matcher declaration = java.util.regex.Pattern
                .compile("\\b(\\w+(?:<[^>]*>)?(?:\\[\\])?)\\s+" 
                        + java.util.regex.Pattern.quote(mot)
                        + "\\s*[=;]")
                .matcher(text);
        if (declaration.find()) {
            return new HoverLsp(mot + " : " + declaration.group(1),
                    "variable", null);
        }
        java.util.regex.Matcher methode = java.util.regex.Pattern
                .compile("\\b\\w[\\w<>, \\[\\]]*\\s+"
                        + java.util.regex.Pattern.quote(mot)
                        + "\\s*\\(")
                .matcher(text);
        if (methode.find()) {
            int finLigne = text.indexOf('\n', methode.start());
            String entete = text.substring(methode.start(),
                    finLigne < 0 ? Math.min(text.length(),
                            methode.start() + 80) : finLigne).trim();
            return new HoverLsp(entete, "méthode", null);
        }
        if (MOTS_RESERVES.contains(mot)) {
            return new HoverLsp(mot, "mot réservé", null);
        }
        return null;
    }

    /** L'offset du début de ligne+colonne, null hors texte. */
    private static int[] offsetDe(String text, LspPosition position) {
        int line = 0;
        int offset = 0;
        while (line < position.line() && offset < text.length()) {
            if (text.charAt(offset) == '\n') {
                line++;
            }
            offset++;
        }
        if (line != position.line()) {
            return null;
        }
        return new int[]{offset + position.column()};
    }

    /** Les symboles du document : les classes, les méthodes, lues
     *  des têtes de déclaration. */
    static List<SymbolLsp> symbols(String text) {
        List<SymbolLsp> symbols = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        for (int number = 0; number < lines.length; number++) {
            String line = lines[number].trim();
            java.util.regex.Matcher classe = java.util.regex.Pattern
                    .compile("(?:class|interface|enum)\\s+(\\w+)")
                    .matcher(line);
            if (classe.find()) {
                symbols.add(new SymbolLsp(classe.group(1), 5,
                        new RangeLsp(new LspPosition(number, 0),
                                new LspPosition(number + 1, 0)),
                        null, List.of()));
                continue;
            }
            java.util.regex.Matcher methode = java.util.regex.Pattern
                    .compile("(?:public|private|protected|static|final|\\s)*"
                            + "[\\w<>,\\[\\]]+\\s+(\\w+)\\s*\\([^;]*\\)\\s*\\{")
                    .matcher(line);
            if (methode.find() && !line.startsWith("//")) {
                symbols.add(new SymbolLsp(methode.group(1), 6,
                        new RangeLsp(new LspPosition(number, 0),
                                new LspPosition(number + 1, 0)),
                        null, List.of()));
            }
        }
        return symbols;
    }

    /** Les occurrences du mot sous la position : chaque apparition
     *  du mot entier, la surlignage du protocole. */
    static List<HighlightLsp> highlights(String text, LspPosition position) {
        int[] ligneColonne = offsetDe(text, position);
        if (ligneColonne == null) {
            return List.of();
        }
        int offset = ligneColonne[0];
        if (offset >= text.length()) {
            return List.of();
        }
        int debutMot = offset;
        while (debutMot > 0 && Character.isJavaIdentifierPart(
                text.charAt(debutMot - 1))) {
            debutMot--;
        }
        int finMot = offset;
        while (finMot < text.length()
                && Character.isJavaIdentifierPart(text.charAt(finMot))) {
            finMot++;
        }
        String mot = text.substring(debutMot, finMot);
        if (mot.isEmpty()) {
            return List.of();
        }
        List<HighlightLsp> highlights = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\b" + java.util.regex.Pattern.quote(mot) + "\\b")
                .matcher(text);
        int[] offsets = new int[1];
        while (matcher.find()) {
            offsets[0] = matcher.start();
            int[] lc = lineColumnOf(text, matcher.start(), matcher.end());
            if (lc != null) {
                highlights.add(new HighlightLsp(
                        new RangeLsp(new LspPosition(lc[0], lc[1]),
                                new LspPosition(lc[2], lc[3])), 1));
            }
        }
        return highlights;
    }

    /** Les positions LSP d'une plage d'offsets, null hors texte. */
    private static int[] lineColumnOf(String text, int start, int end) {
        int line = 0;
        int column = 0;
        int lineStart = 0;
        for (int i = 0; i < end && i < text.length(); i++) {
            if (i == start) {
                break;
            }
            if (text.charAt(i) == '\n') {
                line++;
                column = 0;
                lineStart = i + 1;
            } else {
                column++;
            }
        }
        return new int[]{line, start - lineStart, line,
                end - lineStart};
    }
}
