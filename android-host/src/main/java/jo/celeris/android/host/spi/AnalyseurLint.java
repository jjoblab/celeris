package jo.celeris.android.host.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * L'analyseur de la sortie d'Android Lint (C-12) : la forme du texte
 * — « chemin:ligne: Gravite: message [IdRegle] », la colonne
 * facultative entre les deux — devient des diagnostics de l'hôte aux
 * lignes exactes. Les gravités de l'outils — Error, Warning,
 * Information — se convertissent ; les résumés et les lignes de bruit
 * ne produisent rien.
 *
 * <p>Les chemins relatifs se racinent dans le système virtuel, comme
 * les problèmes de build (E-05) ; l'identifiant de règle reste porté
 * par le message, entre crochets — la matière du correctif applicable
 * ({@link CorrectifsLint}).</p>
 */
public final class AnalyseurLint {

    private static final Pattern FORME_LINT = Pattern.compile(
            "^([^<>:|*?\"]+):(\\d+):(?:(\\d+):)?\\s*"
                    + "(Error|Warning|Information):\\s*(.+)$");

    private static final Pattern IDENTIFIANT = Pattern.compile("^(.*)\\[([^\\[\\]]+)]$");

    private AnalyseurLint() {
    }

    /**
     * Parse la sortie texte de l'outils, ligne à ligne.
     *
     * @param output la sortie brute de lint, multi-lignes
     * @return les diagnostics dans l'ordre de la sortie, vide si rien
     */
    public static List<HostDiagnostic> parser(String output) {
        List<HostDiagnostic> diagnostiques = new ArrayList<>();
        for (String line : output.split("\n", -1)) {
            HostDiagnostic diagnostic = convertir(line.strip());
            if (diagnostic != null) {
                diagnostiques.add(diagnostic);
            }
        }
        return diagnostiques;
    }

    private static @Nullable HostDiagnostic convertir(String line) {
        Matcher forme = FORME_LINT.matcher(line);
        if (!forme.matches()) {
            return null;
        }
        String chemin = forme.group(1).strip();
        int number = Integer.parseInt(forme.group(2));
        String column = forme.group(3);
        String message = forme.group(5).strip();
        return new HostDiagnostic(
                CheminDoc.of("/" + chemin),
                number,
                column != null ? Integer.parseInt(column) : 0,
                severity(forme.group(4)),
                message);
    }

    private static GraviteDiagnostic severity(String ecrite) {
        return switch (ecrite.strip()) {
            case "Error" -> GraviteDiagnostic.ERREUR;
            case "Warning" -> GraviteDiagnostic.AVERTISSEMENT;
            default -> GraviteDiagnostic.INFO;
        };
    }

    /**
     * Le diagnostic du fichier donné porté par la ligne (à partir de
     * un) — la matière de l'info-bulle au caret ; null quand aucun ne
     * s'y porte.
     *
     * @param diagnostiques les diagnostics courants de l'hôte
     * @param nomFichier    le nom du fichier ouvert
     * @param line         la ligne cherchée, à partir de un
     * @return le diagnostic porté, null sinon
     */
    public static @Nullable HostDiagnostic aLaLigne(
            List<HostDiagnostic> diagnostiques, String nomFichier, int line) {
        for (HostDiagnostic diagnostic : diagnostiques) {
            if (diagnostic.line() == line
                    && diagnostic.chemin().nom().equals(nomFichier)) {
                return diagnostic;
            }
        }
        return null;
    }

    /**
     * L'identifiant de règle porté par un message de lint, entre
     * crochets — « texte [IdRegle] » rend « IdRegle » ; sans crochets,
     * vide.
     *
     * @param message le message du diagnostic
     * @return l'identifiant, ou la chaîne vide
     */
    public static String regle(String message) {
        Matcher identifier = IDENTIFIANT.matcher(message.strip());
        return identifier.matches() ? identifier.group(2).strip() : "";
    }

    /**
     * Le message sans son identifiant de règle : « texte [IdRegle] »
     * rend « texte » ; sans crochets, le message entier.
     *
     * @param message le message du diagnostic
     * @return le texte du message
     */
    public static String text(String message) {
        Matcher identifier = IDENTIFIANT.matcher(message.strip());
        return identifier.matches()
                ? identifier.group(1).strip()
                : message.strip();
    }
}
