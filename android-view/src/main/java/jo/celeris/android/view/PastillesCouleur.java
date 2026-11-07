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
