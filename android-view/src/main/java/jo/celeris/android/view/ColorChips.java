package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * Le balayage des littéraux de couleur (C-05) : chaque ligne porteuse
 * d'un littéral Android — « #RGB », « #ARGB », « #RRGGBB » ou
 * « #AARRGGBB » — rend sa pastille résolue. Une pastille par ligne,
 * la première ; la résolution est celle de la plateforme : la forme
 * courte étend chaque composante en doublon, la forme sans alpha prend
 * l'alpha plein.
 */
public final class ColorChips {

    private static final Pattern LITTERAL = Pattern.compile(
            "#([0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{4}|[0-9a-fA-F]{3})"
                    + "(?![0-9a-fA-F])");

    private ColorChips() {
    }

    /**
     * Les pastilles des lignes données.
     *
     * @param lines les lignes du document, numérotées à partir de zéro
     * @return une pastille par ligne porteuse, dans l'ordre des lignes
     */
    public static List<ColorChip> ofLines(List<String> lines) {
        List<ColorChip> chips = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            ColorChip chip = chip(i, lines.get(i));
            if (chip != null) {
                chips.add(chip);
            }
        }
        return chips;
    }

    /**
     * La pastille de la ligne donnée : la première couleur résolue.
     *
     * @param number la ligne, numérotée à partir de zéro
     * @param line  le texte de la ligne
     * @return la pastille, null si la ligne ne porte aucun littéral
     */
    public static @Nullable ColorChip chip(int number, String line) {
        Matcher chercheur = LITTERAL.matcher(line);
        if (!chercheur.find()) {
            return null;
        }
        String digits = chercheur.group(1);
        return new ColorChip(number, chercheur.start() + 1,
                resoudre(digits), digits.length() + 1);
    }

    /**
     * TOUTES les pastilles de la ligne donnée, dans l'ordre du balayage
     * — le texte encadre chaque littéral de couleur, pas seulement le
     * premier (le constat du propriétaire : « lorsque je tape une
     * couleur par exemple #fff, #ffffff encadré le avec la couleur
     * comme dans l'ancien projet »).
     *
     * @param number la ligne, numérotée à partir de zéro
     * @param line  le texte de la ligne
     * @return les pastilles de la ligne, vide sans littéral
     */
    public static List<ColorChip> tous(int number, String line) {
        List<ColorChip> chips = new ArrayList<>();
        Matcher chercheur = LITTERAL.matcher(line);
        while (chercheur.find()) {
            String digits = chercheur.group(1);
            chips.add(new ColorChip(number, chercheur.start() + 1,
                    resoudre(digits), digits.length() + 1));
        }
        return chips;
    }

    /**
     * Le littéral formaté d'une couleur — l'inverse de la résolution : la
     * largeur du littéral d'origine tient (ses chiffres), la forme courte
     * quantifie chaque composante au nibble le plus proche, la sortie
     * reste en minuscules — le code environnant ne bouge pas, le pick
     * remplace le littéral en place.
     *
     * @param argb    la couleur au format ARGB
     * @param digits la largeur voulue du littéral (ses chiffres, sans le dièse)
     * @return le littéral formaté, dièse compris
     */
    public static String formater(int argb, int digits) {
        return switch (digits) {
            case 3 -> String.format("#%1$x%2$x%3$x",
                    quantifie((argb >>> 16) & 0xFF),
                    quantifie((argb >>> 8) & 0xFF),
                    quantifie(argb & 0xFF));
            case 4 -> String.format("#%1$x%2$x%3$x%4$x",
                    quantifie((argb >>> 24) & 0xFF),
                    quantifie((argb >>> 16) & 0xFF),
                    quantifie((argb >>> 8) & 0xFF),
                    quantifie(argb & 0xFF));
            case 6 -> String.format("#%06x", argb & 0xFFFFFF);
            default -> String.format("#%08x", argb);
        };
    }

    /** La composante quantifiée au nibble le plus proche. */
    private static int quantifie(int composante) {
        return Math.round(composante * 15f / 255f);
    }

    /**
     * La valeur résolue d'un littéral sans son dièse : la forme courte
     * étend ses composantes, la forme sans alpha prend l'alpha plein.
     *
     * @param digits les chiffres hexadécimaux du littéral
     * @return la couleur ARGB résolue
     */
    public static int resoudre(String digits) {
        return switch (digits.length()) {
            case 3 -> 0xFF000000
                    | (etendre(digits.charAt(0)) << 16)
                    | (etendre(digits.charAt(1)) << 8)
                    | etendre(digits.charAt(2));
            case 4 -> (etendre(digits.charAt(0)) << 24)
                    | (etendre(digits.charAt(1)) << 16)
                    | (etendre(digits.charAt(2)) << 8)
                    | etendre(digits.charAt(3));
            case 6 -> 0xFF000000 | Integer.parseInt(digits, 16);
            default -> (int) Long.parseLong(digits, 16);
        };
    }

    /** La composante étendue d'un chiffre court : « f » devient « ff ». */
    private static int etendre(char chiffre) {
        int value = Character.digit(chiffre, 16);
        return value << 4 | value;
    }
}
