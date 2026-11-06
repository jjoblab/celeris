package jo.celeris.core.intelligence;

import java.util.List;

/**
 * Les squelettes de méthode (B-06) : implémenter ou surcharger une
 * méthode — le squelette naît de la signature que le serveur a donnée,
 * les trous y reçoivent les curseurs dans l'ordre de tabulation. Le
 * moteur de gabarits du M5 fait le reste : valeurs par défaut posées,
 * occurrences liées.
 */
public final class SquelettesMethode {

    private SquelettesMethode() {
    }

    /**
     * Le corps du squelette d'une méthode.
     *
     * @param nom              le nom de la méthode
     * @param typesParametres  les types des paramètres, dans l'ordre de
     *                         la signature — chaque paramètre devient un
     *                         trou {@code pN} portant le défaut
     *                         {@code type argN}
     * @param typeRetour       le type de retour
     * @return le corps du gabarit, trous compris — le corps de la
     *         méthode est le dernier trou
     */
    public static String squelette(String nom, List<String> typesParametres,
                                   String typeRetour) {
        StringBuilder corps = new StringBuilder("@Override\npublic ")
                .append(typeRetour).append(' ').append(nom).append('(');
        for (int i = 0; i < typesParametres.size(); i++) {
            if (i > 0) {
                corps.append(", ");
            }
            corps.append("${p").append(i + 1).append(':')
                    .append(typesParametres.get(i)).append(" arg").append(i).append('}');
        }
        corps.append(") {\n    ${corps}\n}");
        return corps.toString();
    }
}
