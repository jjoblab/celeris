package jo.celeris.demo;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * La carte d'un fichier d'exemple pour l'écran d'accueil : monogramme,
 * couleur de badge, nom de langage et compte de lignes — tout ce que
 * la carte affiche, calculé sans Android (testable Jupiter). Les
 * couleurs reprennent les encres syntaxiques du thème sombre de
 * l'éditeur ({@code EditorThemes.SOMBRE}) pour que l'accueil et la
 * toile parlent la même palette.
 *
 * @param nom        le nom du fichier, extension comprise
 * @param monogramme une ou deux lettres du badge de langage
 * @param couleur    la couleur ARGB du badge
 * @param langage    le nom de langage affiché
 * @param lignes     le nombre de lignes du contenu
 */
public record CarteFichier(String nom, String monogramme, int couleur,
        String langage, int lignes) {

    private static final int KOTLIN = 0xFFF06292;
    private static final int JAVA = 0xFFCC7832;
    private static final int XML = 0xFFB0BEC5;
    private static final int GRADLE = 0xFF7FDBCA;
    private static final int TOML = 0xFFFFCB6B;
    private static final int JSON = 0xFFC3E88D;
    private static final int MARKDOWN = 0xFF9AA0A6;
    private static final int PYTHON = 0xFF82AAFF;
    private static final int PROGUARD = 0xFFF07178;
    private static final int TEXTE = 0xFF9AA0A6;

    /**
     * La carte du fichier d'exemple donné.
     *
     * @param fichier le fichier du catalogue
     * @return sa carte complète
     */
    public static CarteFichier depuis(FichierExemple fichier) {
        String extension = extensionDe(fichier.nom());
        return new CarteFichier(fichier.nom(), monogrammePour(extension),
                couleurPour(extension), langagePour(extension),
                compterLignes(fichier.contenu()));
    }

    /**
     * Les cartes du catalogue complet, dans l'ordre d'affichage.
     *
     * @return les cartes, non modifiables
     */
    public static List<CarteFichier> catalogue() {
        return FichiersExemple.catalogue().stream()
                .map(CarteFichier::depuis).collect(Collectors.toList());
    }

    private static String extensionDe(String nom) {
        int point = nom.lastIndexOf('.');
        return point < 0 ? "" : nom.substring(point + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Le compte de lignes du contenu : le saut final ne compte pas
     * pour une ligne ({@code String#lines} exige l'API 33, le comptage
     * reste portable jusqu'à 26).
     *
     * @param contenu le contenu intégral du fichier
     * @return le nombre de lignes affichées
     */
    private static int compterLignes(String contenu) {
        if (contenu.isEmpty()) {
            return 0;
        }
        int lignes = 1;
        for (int i = 0; i < contenu.length(); i++) {
            if (contenu.charAt(i) == '\n') {
                lignes++;
            }
        }
        return contenu.endsWith("\n") ? lignes - 1 : lignes;
    }

    private static String monogrammePour(String extension) {
        return switch (extension) {
            case "kt" -> "Kt";
            case "java" -> "Jv";
            case "xml" -> "Xm";
            case "kts", "gradle" -> "Ks";
            case "toml" -> "Tm";
            case "json" -> "Js";
            case "md" -> "Md";
            case "py" -> "Py";
            case "pro" -> "R8";
            default -> "Tx";
        };
    }

    private static int couleurPour(String extension) {
        return switch (extension) {
            case "kt" -> KOTLIN;
            case "java" -> JAVA;
            case "xml" -> XML;
            case "kts", "gradle" -> GRADLE;
            case "toml" -> TOML;
            case "json" -> JSON;
            case "md" -> MARKDOWN;
            case "py" -> PYTHON;
            case "pro" -> PROGUARD;
            default -> TEXTE;
        };
    }

    private static String langagePour(String extension) {
        return switch (extension) {
            case "kt" -> "Kotlin";
            case "java" -> "Java";
            case "xml" -> "XML";
            case "kts", "gradle" -> "Gradle";
            case "toml" -> "TOML";
            case "json" -> "JSON";
            case "md" -> "Markdown";
            case "py" -> "Python";
            case "pro" -> "ProGuard";
            default -> "Text";
        };
    }
}
