package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Étiquettes de numéros de gouttière mémoïsées.
 *
 * <p>PIEGE-15 : formater un numéro allouerait une chaîne par rangée et
 * par image dans la boucle de dessin. Les étiquettes sont donc
 * matérialisées une fois par numéro, gardées pour toujours (les numéros
 * d'un document vivent en petite quantité devant les rangées) et servies
 * telles quelles en régime établi — zéro allocation.</p>
 */
public final class GutterNumbers {

    private final List<String> etiquettes = new ArrayList<>();
    private final StringBuilder tampon = new StringBuilder();

    /**
     * L'étiquette du numéro donné, mémoïsée après première demande.
     *
     * @param number le numéro de ligne, ≥ 1
     * @return l'étiquette (instance réutilisée d'une image à l'autre)
     */
    public String label(int number) {
        if (number < 1) {
            throw new IllegalArgumentException("line number must be positive: " + number);
        }
        while (etiquettes.size() < number) {
            tampon.setLength(0);
            tampon.append(etiquettes.size() + 1);
            etiquettes.add(tampon.toString());
        }
        return etiquettes.get(number - 1);
    }

    /**
     * Le nombre de chiffres du numéro donné, servi par la mémoïsation
     * quand il est déjà connu.
     *
     * @param number le numéro de ligne, ≥ 1
     * @return le nombre de chiffres de l'étiquette
     */
    public int digits(int number) {
        return label(number).length();
    }
}
