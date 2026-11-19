package jo.celeris.android.host.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * L'analyseur de piles d'appel (E-06) : chaque cadre « at » rend sa
 * classe, sa méthode, son fichier et sa ligne exacte — la matière du
 * saut lien `fichier:ligne`. Les en-têtes, les causes et les ellipses
 * « ... N more » ne sont pas des cadres : elles ne produisent rien.
 */
public final class StackAnalyzer {

    private static final Pattern CADRE = Pattern.compile(
            "^\\s*at\\s+([\\w.$]+)\\.([\\w$<>]+)\\(([\\w.$-]+\\.\\w+):(-?\\d+)\\)$");

    private static final Pattern FRAME_WITHOUT_LINE = Pattern.compile(
            "^\\s*at\\s+([\\w.$]+)\\.([\\w$<>]+)\\(([\\w.$-]+\\.\\w+)\\)$");

    private StackAnalyzer() {
    }

    /**
     * Parse la pile, cadre par cadre, dans l'ordre de la trace.
     *
     * @param pile la trace brute, multi-lignes
     * @return les cadres dans l'ordre, vide si aucun
     */
    public static List<StackFrame> parser(String pile) {
        List<StackFrame> cadres = new ArrayList<>();
        for (String line : pile.split("\n", -1)) {
            StackFrame cadre = convertir(line);
            if (cadre != null) {
                cadres.add(cadre);
            }
        }
        return cadres;
    }

    private static @Nullable StackFrame convertir(String line) {
        Matcher complet = CADRE.matcher(line);
        if (complet.matches()) {
            int ligneLue = Integer.parseInt(complet.group(4));
            return new StackFrame(complet.group(1), complet.group(2),
                    complet.group(3), Math.max(0, ligneLue));
        }
        Matcher sansLigne = FRAME_WITHOUT_LINE.matcher(line);
        if (sansLigne.matches()) {
            return new StackFrame(sansLigne.group(1), sansLigne.group(2),
                    sansLigne.group(3), 0);
        }
        return null;
    }
}
