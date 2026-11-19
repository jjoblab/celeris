package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jo.celeris.core.text.Document;

/**
 * Le garde-fou de syntaxe des règles R8/ProGuard (C-09) : les règles
 * inconnues — un tiret suivi d'un nom hors catalogue —, les accolades
 * non appariées et les points-virgules manquants des membres se
 * signalent à la plage exacte. Le fichier de règles n'est pas un
 * langage de programmation : la validation reste délibérément basique,
 * les faux positifs coûtent plus cher que les silences.
 */
public final class ValidationRegles {

    /** Les règles du catalogue officiel, préfixées du tiret. */
    private static final Set<String> REGLES = Set.of(
            "keep", "keepnames", "keepclassmembers", "keepclasseswithmembers",
            "keepclasseswithmembernames", "keepattributes", "dontwarn", "dontnote",
            "dontobfuscate", "dontshrink", "dontoptimize", "dontpreverify",
            "optimizations", "optimizationpasses", "allowaccessmodification",
            "repackageclasses", "flattenpackagehierarchy", "useuniqueclassmembernames",
            "mergeinterfacesaggressively", "overloadaggressively", "verbose",
            "printconfiguration", "printmapping", "printusage", "printseeds",
            "include", "basedirectory", "injars", "outjars", "libraryjars",
            "dontskipnonpubliclibraryclasses", "dontskipnonpubliclibraryclassmembers",
            "whyareyoukeeping");

    private ValidationRegles() {
    }

    /**
     * Valide les règles du document.
     *
     * @param doc le document du fichier de règles
     * @return les problèmes trouvés, dans l'ordre du document
     */
    public static List<SyntaxIssue> validate(Document doc) {
        List<SyntaxIssue> problemes = new ArrayList<>();
        int profondeur = 0;
        int nombreLignes = doc.lineCount();
        for (int line = 0; line < nombreLignes; line++) {
            String text = textOf(doc, line);
            int start = doc.lineStart(line);
            String horsCommentaire = cutComment(text);
            if (horsCommentaire.isBlank()) {
                continue;
            }
            problemes.addAll(reglesInconnues(horsCommentaire, start));
            if (profondeur > 0) {
                problemes.addAll(pointsVirgulesManquants(horsCommentaire, start));
            }
            profondeur = braceProblems(horsCommentaire, start,
                    profondeur, problemes);
        }
        return problemes;
    }

    /** Le texte d'une ligne, sans la marque de fin. */
    private static String textOf(Document doc, int line) {
        int start = doc.lineStart(line);
        int end = doc.lineEnd(line);
        return doc.text().substring(start, end);
    }

    /** La ligne privée de son commentaire final. */
    private static String cutComment(String line) {
        int commentaire = line.indexOf('#');
        return commentaire >= 0 ? line.substring(0, commentaire) : line;
    }

    /** Les règles inconnues : tiret suivi d'un nom hors catalogue. */
    private static List<SyntaxIssue> reglesInconnues(String line, int start) {
        List<SyntaxIssue> problemes = new ArrayList<>();
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != '-' || !precedeMot(line, i)) {
                continue;
            }
            int end = finMot(line, i + 1);
            String nom = line.substring(i + 1, end);
            if (!nom.isEmpty() && !REGLES.contains(nom) && nom.chars().allMatch(
                    c -> Character.isLowerCase(c) || c == '-')) {
                problemes.add(new SyntaxIssue(start + i, nom.length() + 1,
                        "règle inconnue : -" + nom));
            }
            i = end;
        }
        return problemes;
    }

    /** Vrai quand le tiret ouvre un mot de règle (début ou après espace). */
    private static boolean precedeMot(String line, int position) {
        if (position + 1 >= line.length()) {
            return false;
        }
        char suivant = line.charAt(position + 1);
        if (!Character.isLetter(suivant)) {
            return false;
        }
        return position == 0 || Character.isWhitespace(line.charAt(position - 1));
    }

    /** La fin du mot commençant à la position donnée. */
    private static int finMot(String line, int start) {
        int end = start;
        while (end < line.length() && (Character.isLetterOrDigit(line.charAt(end))
                || line.charAt(end) == '-')) {
            end++;
        }
        return end;
    }

    /**
     * Les accolades : chaque bloc de membres se referme, l'accolade
     * orpheline se signale ; la profondeur rendue mène à la ligne
     * suivante.
     */
    private static int braceProblems(String line, int start,
            int profondeur, List<SyntaxIssue> problemes) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '{') {
                profondeur++;
            } else if (c == '}') {
                profondeur--;
                if (profondeur < 0) {
                    problemes.add(new SyntaxIssue(start + i, 1,
                            "accolade fermante sans ouvrante"));
                    profondeur = 0;
                }
            }
        }
        return profondeur;
    }

    /**
     * Les points-virgules manquants : dans un bloc de membres, une
     * ligne de déclaration qui ne se termine ni par « ; », ni par une
     * accolade, ni par « ... » se signale — la signature la plus
     * simple reste la forme attendue. Les lignes hors bloc (spéc de
     * classe, règles) et les blocs d'une seule ligne ne se sondent
     * pas : les faux positifs coûtent plus cher que les silences.
     */
    private static List<SyntaxIssue> pointsVirgulesManquants(String line,
            int start) {
        List<SyntaxIssue> problemes = new ArrayList<>();
        String epuree = line.strip();
        if (epuree.contains("{") || epuree.contains("}")
                || epuree.endsWith(";") || epuree.endsWith("...")) {
            return problemes;
        }
        problemes.add(new SyntaxIssue(start + line.indexOf(epuree),
                epuree.length(),
                "membre sans point-virgule terminal : « " + epuree + " »"));
        return problemes;
    }
}
