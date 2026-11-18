package jo.celeris.sample.lsp;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.lsp.types.ElementCompletion;
import jo.celeris.core.lsp.types.ListeCompletion;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.SurlignageLsp;
import jo.celeris.core.lsp.types.SurvolLsp;
import jo.celeris.core.lsp.types.SymboleLsp;
import jo.celeris.core.lsp.types.PlageLsp;

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
    private static final List<ElementCompletion> MEMBRES_STRING = List.of(
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
    private static ElementCompletion membre(String libelle, String detail,
            String documentation) {
        return new ElementCompletion(libelle, 2, detail, documentation,
                null, null, null, false);
    }

    /** Les identifiants déclarés du document : variables et méthodes
     *  lues des têtes de déclaration. */
    static List<String> declarations(String texte) {
        List<String> noms = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\b(?:[A-Za-z_][\\w<>\\[\\], .]*)\\s+"
                        + "([a-z_][\\w]*)\\s*[=;(]")
                .matcher(texte);
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
    static ListeCompletion completion(String texte, int caret) {
        int debutMot = caret;
        while (debutMot > 0
                && Character.isJavaIdentifierPart(texte.charAt(debutMot - 1))) {
            debutMot--;
        }
        String prefixe = texte.substring(debutMot, caret);
        boolean apresPoint = debutMot > 0 && texte.charAt(debutMot - 1) == '.';
        List<ElementCompletion> elements = new ArrayList<>();
        if (apresPoint) {
            // le récepteur : un littéral de chaîne remonte-t-il au
            // mot précédent le point ?
            int finReceveur = debutMot - 1;
            int debutReceveur = finReceveur;
            while (debutReceveur > 0 && texte.charAt(debutReceveur - 1) != '"'
                    && Character.isJavaIdentifierPart(
                            texte.charAt(debutReceveur - 1))) {
                debutReceveur--;
            }
            String receveur = texte.substring(debutReceveur, finReceveur);
            boolean chaine = debutReceveur > 0
                    && texte.charAt(debutReceveur - 1) == '"';
            if (chaine || "nom".equals(receveur) || "texte".equals(receveur)) {
                for (ElementCompletion membre : MEMBRES_STRING) {
                    if (membre.libelle().startsWith(prefixe)) {
                        elements.add(membre);
                    }
                }
                return ListeCompletion.de(false, elements);
            }
        }
        for (String reserve : MOTS_RESERVES) {
            if (reserve.startsWith(prefixe)) {
                elements.add(new ElementCompletion(reserve, 14,
                        "mot réservé", null, null, null, null, false));
            }
        }
        for (String nom : declarations(texte)) {
            if (nom.startsWith(prefixe) && !nom.equals(prefixe)) {
                elements.add(new ElementCompletion(nom, 6,
                        "déclaré dans ce fichier", null, null, null, null,
                        false));
            }
        }
        return ListeCompletion.de(false, elements);
    }

    /** L'aide aux signatures : l'appel ouvert devant le caret —
     *  println et print du System.out, les méthodes du document. */
    static SignatureLsp signature(String texte, int caret) {
        // l'appel ouvert : remonter au « ( » non fermé le plus proche
        int parenthese = parentheseOuverte(texte, caret);
        if (parenthese < 0) {
            return null;
        }
        int debutMot = parenthese;
        while (debutMot > 0 && (Character.isJavaIdentifierPart(
                texte.charAt(debutMot - 1)) || texte.charAt(debutMot - 1) == '.')) {
            debutMot--;
        }
        String nom = texte.substring(debutMot, parenthese);
        int parametreActif = 0;
        for (int i = parenthese + 1; i < caret && i < texte.length(); i++) {
            if (texte.charAt(i) == ',') {
                parametreActif++;
            }
        }
        if (nom.endsWith("println")) {
            return SignatureLsp.de(List.of(
                    entreePrintln("println(Object x)", "x",
                            "ce qui s'imprime, puis le saut de ligne",
                            "Impression avec saut de ligne."),
                    entreePrintln("println(String s)", "s",
                            "la chaîne imprimée",
                            "Impression d'une chaîne, saut de ligne.")),
                    0, parametreActif);
        }
        if (nom.endsWith("print")) {
            return SignatureLsp.de(List.of(entreePrintln(
                    "print(Object x)", "x",
                    "ce qui s'imprime, sans saut de ligne",
                    "Impression sans saut de ligne.")),
                    0, parametreActif);
        }
        return null;
    }

    /** L'offset du « ( » non fermé qui porte le caret, -1 sans appel ouvert. */
    private static int parentheseOuverte(String texte, int caret) {
        int profondeur = 0;
        for (int i = caret - 1; i >= 0; i--) {
            char c = texte.charAt(i);
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
    private static SignatureLsp.Entree entreePrintln(String libelle,
            String parametre, String documentationParametre,
            String documentation) {
        return new SignatureLsp.Entree(libelle,
                List.of(new SignatureLsp.Parametre(parametre,
                        documentationParametre)),
                documentation);
    }

    /** Le survol : la déclaration locale du mot sous la position. */
    static SurvolLsp survol(String texte, PositionLsp position) {
        int[] ligneColonne = offsetDe(texte, position);
        if (ligneColonne == null) {
            return null;
        }
        int debutMot = ligneColonne[0];
        while (debutMot > 0 && Character.isJavaIdentifierPart(
                texte.charAt(debutMot - 1))) {
            debutMot--;
        }
        int finMot = ligneColonne[0];
        while (finMot < texte.length()
                && Character.isJavaIdentifierPart(texte.charAt(finMot))) {
            finMot++;
        }
        if (finMot <= debutMot) {
            return null;
        }
        String mot = texte.substring(debutMot, finMot);
        // le mot apparaît-il en tête de déclaration ?
        java.util.regex.Matcher declaration = java.util.regex.Pattern
                .compile("\\b(\\w+(?:<[^>]*>)?(?:\\[\\])?)\\s+" 
                        + java.util.regex.Pattern.quote(mot)
                        + "\\s*[=;]")
                .matcher(texte);
        if (declaration.find()) {
            return new SurvolLsp(mot + " : " + declaration.group(1),
                    "variable", null);
        }
        java.util.regex.Matcher methode = java.util.regex.Pattern
                .compile("\\b\\w[\\w<>, \\[\\]]*\\s+"
                        + java.util.regex.Pattern.quote(mot)
                        + "\\s*\\(")
                .matcher(texte);
        if (methode.find()) {
            int finLigne = texte.indexOf('\n', methode.start());
            String entete = texte.substring(methode.start(),
                    finLigne < 0 ? Math.min(texte.length(),
                            methode.start() + 80) : finLigne).trim();
            return new SurvolLsp(entete, "méthode", null);
        }
        if (MOTS_RESERVES.contains(mot)) {
            return new SurvolLsp(mot, "mot réservé", null);
        }
        return null;
    }

    /** L'offset du début de ligne+colonne, null hors texte. */
    private static int[] offsetDe(String texte, PositionLsp position) {
        int ligne = 0;
        int offset = 0;
        while (ligne < position.ligne() && offset < texte.length()) {
            if (texte.charAt(offset) == '\n') {
                ligne++;
            }
            offset++;
        }
        if (ligne != position.ligne()) {
            return null;
        }
        return new int[]{offset + position.colonne()};
    }

    /** Les symboles du document : les classes, les méthodes, lues
     *  des têtes de déclaration. */
    static List<SymboleLsp> symboles(String texte) {
        List<SymboleLsp> symboles = new ArrayList<>();
        String[] lignes = texte.split("\n", -1);
        for (int numero = 0; numero < lignes.length; numero++) {
            String ligne = lignes[numero].trim();
            java.util.regex.Matcher classe = java.util.regex.Pattern
                    .compile("(?:class|interface|enum)\\s+(\\w+)")
                    .matcher(ligne);
            if (classe.find()) {
                symboles.add(new SymboleLsp(classe.group(1), 5,
                        new PlageLsp(new PositionLsp(numero, 0),
                                new PositionLsp(numero + 1, 0)),
                        null, List.of()));
                continue;
            }
            java.util.regex.Matcher methode = java.util.regex.Pattern
                    .compile("(?:public|private|protected|static|final|\\s)*"
                            + "[\\w<>,\\[\\]]+\\s+(\\w+)\\s*\\([^;]*\\)\\s*\\{")
                    .matcher(ligne);
            if (methode.find() && !ligne.startsWith("//")) {
                symboles.add(new SymboleLsp(methode.group(1), 6,
                        new PlageLsp(new PositionLsp(numero, 0),
                                new PositionLsp(numero + 1, 0)),
                        null, List.of()));
            }
        }
        return symboles;
    }

    /** Les occurrences du mot sous la position : chaque apparition
     *  du mot entier, la surlignage du protocole. */
    static List<SurlignageLsp> surlignages(String texte, PositionLsp position) {
        int[] ligneColonne = offsetDe(texte, position);
        if (ligneColonne == null) {
            return List.of();
        }
        int offset = ligneColonne[0];
        if (offset >= texte.length()) {
            return List.of();
        }
        int debutMot = offset;
        while (debutMot > 0 && Character.isJavaIdentifierPart(
                texte.charAt(debutMot - 1))) {
            debutMot--;
        }
        int finMot = offset;
        while (finMot < texte.length()
                && Character.isJavaIdentifierPart(texte.charAt(finMot))) {
            finMot++;
        }
        String mot = texte.substring(debutMot, finMot);
        if (mot.isEmpty()) {
            return List.of();
        }
        List<SurlignageLsp> surlignages = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\b" + java.util.regex.Pattern.quote(mot) + "\\b")
                .matcher(texte);
        int[] offsets = new int[1];
        while (matcher.find()) {
            offsets[0] = matcher.start();
            int[] lc = ligneColonneDe(texte, matcher.start(), matcher.end());
            if (lc != null) {
                surlignages.add(new SurlignageLsp(
                        new PlageLsp(new PositionLsp(lc[0], lc[1]),
                                new PositionLsp(lc[2], lc[3])), 1));
            }
        }
        return surlignages;
    }

    /** Les positions LSP d'une plage d'offsets, null hors texte. */
    private static int[] ligneColonneDe(String texte, int debut, int fin) {
        int ligne = 0;
        int colonne = 0;
        int debutLigne = 0;
        for (int i = 0; i < fin && i < texte.length(); i++) {
            if (i == debut) {
                break;
            }
            if (texte.charAt(i) == '\n') {
                ligne++;
                colonne = 0;
                debutLigne = i + 1;
            } else {
                colonne++;
            }
        }
        return new int[]{ligne, debut - debutLigne, ligne,
                fin - debutLigne};
    }
}
