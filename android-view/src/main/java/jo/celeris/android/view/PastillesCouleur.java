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
public final class PastillesCouleur {

    private static final Pattern LITTERAL = Pattern.compile(
            "#([0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{4}|[0-9a-fA-F]{3})"
                    + "(?![0-9a-fA-F])");

    private PastillesCouleur() {
    }

    /**
     * Les pastilles des lignes données.
     *
     * @param lignes les lignes du document, numérotées à partir de zéro
     * @return une pastille par ligne porteuse, dans l'ordre des lignes
     */
    public static List<PastilleCouleur> desLignes(List<String> lignes) {
        List<PastilleCouleur> pastilles = new ArrayList<>();
        for (int i = 0; i < lignes.size(); i++) {
            PastilleCouleur pastille = pastille(i, lignes.get(i));
            if (pastille != null) {
                pastilles.add(pastille);
            }
        }
        return pastilles;
    }

    /**
     * La pastille de la ligne donnée : la première couleur résolue.
     *
     * @param numero la ligne, numérotée à partir de zéro
     * @param ligne  le texte de la ligne
     * @return la pastille, null si la ligne ne porte aucun littéral
     */
    public static @Nullable PastilleCouleur pastille(int numero, String ligne) {
        Matcher chercheur = LITTERAL.matcher(ligne);
        if (!chercheur.find()) {
            return null;
        }
        String chiffres = chercheur.group(1);
        return new PastilleCouleur(numero, chercheur.start() + 1,
                resoudre(chiffres), chiffres.length() + 1);
    }

    /**
     * TOUTES les pastilles de la ligne donnée, dans l'ordre du balayage
     * — le texte encadre chaque littéral de couleur, pas seulement le
     * premier (le constat du propriétaire : « lorsque je tape une
     * couleur par exemple #fff, #ffffff encadré le avec la couleur
     * comme dans l'ancien projet »).
     *
     * @param numero la ligne, numérotée à partir de zéro
     * @param ligne  le texte de la ligne
     * @return les pastilles de la ligne, vide sans littéral
     */
    public static List<PastilleCouleur> tous(int numero, String ligne) {
        List<PastilleCouleur> pastilles = new ArrayList<>();
        Matcher chercheur = LITTERAL.matcher(ligne);
        while (chercheur.find()) {
            String chiffres = chercheur.group(1);
            pastilles.add(new PastilleCouleur(numero, chercheur.start() + 1,
                    resoudre(chiffres), chiffres.length() + 1));
        }
        return pastilles;
    }

    /**
     * Le littéral formaté d'une couleur — l'inverse de la résolution : la
     * largeur du littéral d'origine tient (ses chiffres), la forme courte
     * quantifie chaque composante au nibble le plus proche, la sortie
     * reste en minuscules — le code environnant ne bouge pas, le pick
     * remplace le littéral en place.
     *
     * @param argb    la couleur au format ARGB
     * @param chiffres la largeur voulue du littéral (ses chiffres, sans le dièse)
     * @return le littéral formaté, dièse compris
     */
    public static String formater(int argb, int chiffres) {
        return switch (chiffres) {
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
     * @param chiffres les chiffres hexadécimaux du littéral
     * @return la couleur ARGB résolue
     */
    public static int resoudre(String chiffres) {
        return switch (chiffres.length()) {
            case 3 -> 0xFF000000
                    | (etendre(chiffres.charAt(0)) << 16)
                    | (etendre(chiffres.charAt(1)) << 8)
                    | etendre(chiffres.charAt(2));
            case 4 -> (etendre(chiffres.charAt(0)) << 24)
                    | (etendre(chiffres.charAt(1)) << 16)
                    | (etendre(chiffres.charAt(2)) << 8)
                    | etendre(chiffres.charAt(3));
            case 6 -> 0xFF000000 | Integer.parseInt(chiffres, 16);
            default -> (int) Long.parseLong(chiffres, 16);
        };
    }

    /** La composante étendue d'un chiffre court : « f » devient « ff ». */
    private static int etendre(char chiffre) {
        int valeur = Character.digit(chiffre, 16);
        return valeur << 4 | valeur;
    }
}
