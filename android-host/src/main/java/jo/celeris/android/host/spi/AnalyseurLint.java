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
     * @param sortie la sortie brute de lint, multi-lignes
     * @return les diagnostics dans l'ordre de la sortie, vide si rien
     */
    public static List<DiagnosticHote> parser(String sortie) {
        List<DiagnosticHote> diagnostiques = new ArrayList<>();
        for (String ligne : sortie.split("\n", -1)) {
            DiagnosticHote diagnostic = convertir(ligne.strip());
            if (diagnostic != null) {
                diagnostiques.add(diagnostic);
            }
        }
        return diagnostiques;
    }

    private static @Nullable DiagnosticHote convertir(String ligne) {
        Matcher forme = FORME_LINT.matcher(ligne);
        if (!forme.matches()) {
            return null;
        }
        String chemin = forme.group(1).strip();
        int numero = Integer.parseInt(forme.group(2));
        String colonne = forme.group(3);
        String message = forme.group(5).strip();
        return new DiagnosticHote(
                CheminDoc.de("/" + chemin),
                numero,
                colonne != null ? Integer.parseInt(colonne) : 0,
                gravite(forme.group(4)),
                message);
    }

    private static GraviteDiagnostic gravite(String ecrite) {
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
     * @param ligne         la ligne cherchée, à partir de un
     * @return le diagnostic porté, null sinon
     */
    public static @Nullable DiagnosticHote aLaLigne(
            List<DiagnosticHote> diagnostiques, String nomFichier, int ligne) {
        for (DiagnosticHote diagnostic : diagnostiques) {
            if (diagnostic.ligne() == ligne
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
        Matcher identifiant = IDENTIFIANT.matcher(message.strip());
        return identifiant.matches() ? identifiant.group(2).strip() : "";
    }

    /**
     * Le message sans son identifiant de règle : « texte [IdRegle] »
     * rend « texte » ; sans crochets, le message entier.
     *
     * @param message le message du diagnostic
     * @return le texte du message
     */
    public static String texte(String message) {
        Matcher identifiant = IDENTIFIANT.matcher(message.strip());
        return identifiant.matches()
                ? identifiant.group(1).strip()
                : message.strip();
    }
}
