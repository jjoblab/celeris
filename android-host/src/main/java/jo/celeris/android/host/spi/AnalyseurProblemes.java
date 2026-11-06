package jo.celeris.android.host.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * L'analyseur de problèmes de build (E-05) : convertit la sortie des
 * compilateurs — la forme « chemin:ligne:colonne: gravite: message »
 * des compileurs Java et Kotlin, et sa forme courte sans colonne — en
 * diagnostics aux plages exactes. Les chemins relatifs se racinent
 * dans le système virtuel ; les lignes de bruit ne produisent rien.
 *
 * <p>Les écritures Windows (« C:\\... ») ne se désambiguïsent pas de
 * la forme « chemin:ligne » sur cette seule grammaire : hors périmètre
 * documenté.</p>
 */
public final class AnalyseurProblemes {

    private static final Pattern FORME_COMPLETE = Pattern.compile(
            "^([^<>:|*?\"]+):(\\d+):(\\d+):\\s*(error|warning|info):\\s*(.+)$");

    private static final Pattern FORME_COURTE = Pattern.compile(
            "^([^<>:|*?\"]+):(\\d+):\\s*(error|warning|info):\\s*(.+)$");

    private AnalyseurProblemes() {
    }

    /**
     * Parse la sortie de build, ligne à ligne.
     *
     * @param sortie la sortie brute du build, multi-lignes
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
        Matcher complete = FORME_COMPLETE.matcher(ligne);
        if (complete.matches()) {
            return new DiagnosticHote(
                    CheminDoc.de("/" + complete.group(1).strip()),
                    Integer.parseInt(complete.group(2)),
                    Integer.parseInt(complete.group(3)),
                    gravite(complete.group(4)),
                    complete.group(5).strip());
        }
        Matcher courte = FORME_COURTE.matcher(ligne);
        if (courte.matches()) {
            return new DiagnosticHote(
                    CheminDoc.de("/" + courte.group(1).strip()),
                    Integer.parseInt(courte.group(2)),
                    0,
                    gravite(courte.group(3)),
                    courte.group(4).strip());
        }
        return null;
    }

    private static GraviteDiagnostic gravite(String ecrite) {
        return switch (ecrite.strip()) {
            case "error" -> GraviteDiagnostic.ERREUR;
            case "warning" -> GraviteDiagnostic.AVERTISSEMENT;
            default -> GraviteDiagnostic.INFO;
        };
    }
}
