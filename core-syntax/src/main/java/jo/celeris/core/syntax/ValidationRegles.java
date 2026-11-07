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
    public static List<SyntaxIssue> valider(Document doc) {
        List<SyntaxIssue> problemes = new ArrayList<>();
        int profondeur = 0;
        int nombreLignes = doc.lineCount();
        for (int ligne = 0; ligne < nombreLignes; ligne++) {
            String texte = texteDe(doc, ligne);
            int debut = doc.lineStart(ligne);
            String horsCommentaire = couperCommentaire(texte);
            if (horsCommentaire.isBlank()) {
                continue;
            }
            problemes.addAll(reglesInconnues(horsCommentaire, debut));
            if (profondeur > 0) {
                problemes.addAll(pointsVirgulesManquants(horsCommentaire, debut));
            }
            profondeur = problemesAccolades(horsCommentaire, debut,
                    profondeur, problemes);
        }
        return problemes;
    }

    /** Le texte d'une ligne, sans la marque de fin. */
    private static String texteDe(Document doc, int ligne) {
        int debut = doc.lineStart(ligne);
        int fin = doc.lineEnd(ligne);
        return doc.text().substring(debut, fin);
    }

    /** La ligne privée de son commentaire final. */
    private static String couperCommentaire(String ligne) {
        int commentaire = ligne.indexOf('#');
        return commentaire >= 0 ? ligne.substring(0, commentaire) : ligne;
    }

    /** Les règles inconnues : tiret suivi d'un nom hors catalogue. */
    private static List<SyntaxIssue> reglesInconnues(String ligne, int debut) {
        List<SyntaxIssue> problemes = new ArrayList<>();
        for (int i = 0; i < ligne.length(); i++) {
            if (ligne.charAt(i) != '-' || !precedeMot(ligne, i)) {
                continue;
            }
            int fin = finMot(ligne, i + 1);
            String nom = ligne.substring(i + 1, fin);
            if (!nom.isEmpty() && !REGLES.contains(nom) && nom.chars().allMatch(
                    c -> Character.isLowerCase(c) || c == '-')) {
                problemes.add(new SyntaxIssue(debut + i, nom.length() + 1,
                        "règle inconnue : -" + nom));
            }
            i = fin;
        }
        return problemes;
    }

    /** Vrai quand le tiret ouvre un mot de règle (début ou après espace). */
    private static boolean precedeMot(String ligne, int position) {
        if (position + 1 >= ligne.length()) {
            return false;
        }
        char suivant = ligne.charAt(position + 1);
        if (!Character.isLetter(suivant)) {
            return false;
        }
        return position == 0 || Character.isWhitespace(ligne.charAt(position - 1));
    }

    /** La fin du mot commençant à la position donnée. */
    private static int finMot(String ligne, int debut) {
        int fin = debut;
        while (fin < ligne.length() && (Character.isLetterOrDigit(ligne.charAt(fin))
                || ligne.charAt(fin) == '-')) {
            fin++;
        }
        return fin;
    }

    /**
     * Les accolades : chaque bloc de membres se referme, l'accolade
     * orpheline se signale ; la profondeur rendue mène à la ligne
     * suivante.
     */
    private static int problemesAccolades(String ligne, int debut,
            int profondeur, List<SyntaxIssue> problemes) {
        for (int i = 0; i < ligne.length(); i++) {
            char c = ligne.charAt(i);
            if (c == '{') {
                profondeur++;
            } else if (c == '}') {
                profondeur--;
                if (profondeur < 0) {
                    problemes.add(new SyntaxIssue(debut + i, 1,
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
    private static List<SyntaxIssue> pointsVirgulesManquants(String ligne,
            int debut) {
        List<SyntaxIssue> problemes = new ArrayList<>();
        String epuree = ligne.strip();
        if (epuree.contains("{") || epuree.contains("}")
                || epuree.endsWith(";") || epuree.endsWith("...")) {
            return problemes;
        }
        problemes.add(new SyntaxIssue(debut + ligne.indexOf(epuree),
                epuree.length(),
                "membre sans point-virgule terminal : « " + epuree + " »"));
        return problemes;
    }
}
