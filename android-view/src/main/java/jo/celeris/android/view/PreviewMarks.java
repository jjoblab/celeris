package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Le balayage des repères {@code @Preview} de Compose (C-11) : chaque
 * annotation {@code @Preview} — arguments compris — ancre un repère à
 * sa ligne ; le nom du composable se lit sur la ligne même ou sur la
 * suivante — la déclaration {@code fun} la plus proche. Le rendu
 * reste délégué à l'hôte : le repère ne peint qu'un glyphe, l'hôte
 * décide de ce qu'il montre.
 */
public final class PreviewMarks {

    private static final Pattern ANNOTATION = Pattern.compile("@Preview\\b");

    private static final Pattern COMPOSABLE = Pattern.compile(
            "(?:fun|val)\\s+(\\w+)");

    private PreviewMarks() {
    }

    /**
     * Les repères des lignes données.
     *
     * @param lines les lignes du document, numérotées à partir de zéro
     * @return un repère par annotation, dans l'ordre des lignes
     */
    public static List<PreviewMark> ofLines(List<String> lines) {
        List<PreviewMark> marks = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (ANNOTATION.matcher(lines.get(i)).find()) {
                marks.add(new PreviewMark(i, composable(lines, i)));
            }
        }
        return marks;
    }

    /**
     * Le nom du composable annoté : la première déclaration {@code fun}
     * de la ligne même ou des deux suivantes — « » si aucune ne se lit.
     */
    private static String composable(List<String> lines, int annotation) {
        for (int i = annotation; i < Math.min(annotation + 3, lines.size()); i++) {
            Matcher chercheur = COMPOSABLE.matcher(lines.get(i));
            if (chercheur.find()) {
                return chercheur.group(1);
            }
        }
        return "";
    }

    /**
     * Un repère {@code @Preview} ancré à sa ligne (C-11) : le numéro de
     * la ligne porteuse et le nom du composable, la chaîne vide si le
     * nom ne se lit pas.
     *
     * @param line      la ligne de l'annotation, numérotée à partir de zéro
     * @param composable le nom du composable annoté
     */
    public record PreviewMark(int line, String composable) {

        /**
         * Le libellé du repère : le nom du composable, ou le numéro de
         * ligne quand le nom ne se lit pas.
         *
         * @return le libellé jamais vide
         */
        public String label() {
            return composable.isEmpty() ? "ligne " + (line + 1) : composable;
        }
    }
}
