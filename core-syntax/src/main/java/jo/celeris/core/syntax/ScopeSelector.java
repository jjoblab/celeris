package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Un sélecteur de portées au format TextMate (M7.6, H-07) : la virgule
 * sépare les sélecteurs complexes, l'espace les éléments qui doivent
 * matcher en DESCENDANT (chacun plus profond que le précédent, pas
 * forcément voisin), le tiret {@code -} exclut une portée de la pile.
 * Un élément {@code keyword.operator} matche la portée
 * {@code keyword.operator.assignment} — le point est un préfixe de
 * hiérarchie, pas un joker.
 *
 * <p>Exemple : {@code meta.function entity.name.function - meta.decorator}
 * matche le nom d'une fonction hors décorateur. La spécificité se
 * compte : la profondeur de la portée la plus profonde couplée (plus
 * c'est profond, plus c'est précis), puis le nombre d'éléments
 * couplés — les ex æquo se départagent par l'ordre de déclaration du
 * thème, le premier déclaré gagne.</p>
 */
public final class ScopeSelector {

    private final List<Complex> complexes;

    private ScopeSelector(List<Complex> complexes) {
        this.complexes = List.copyOf(complexes);
    }

    /**
     * Lit un sélecteur de son texte : chaque sélecteur complexe se
     * découpe en éléments et exclusions, la première faute nomme sa
     * position.
     *
     * @param text le texte du sélecteur (virgules comprises)
     * @return le sélecteur lu
     */
    public static ScopeSelector parse(String text) {
        List<Complex> complexes = new ArrayList<>();
        int position = 0;
        for (String morceau : text.split(",", -1)) {
            String nettoye = morceau.trim();
            if (nettoye.isEmpty()) {
                throw new IllegalArgumentException(
                        "scope selector part " + position + " is empty: "
                                + text);
            }
            complexes.add(complexe(nettoye, position));
            position++;
        }
        if (complexes.isEmpty()) {
            throw new IllegalArgumentException(
                    "scope selector is empty: " + text);
        }
        return new ScopeSelector(complexes);
    }

    /** Le sélecteur complexe d'un morceau sans virgule. */
    private static Complex complexe(String morceau, int position) {
        String[] mots = morceau.split("\\s+", -1);
        List<String> elements = new ArrayList<>();
        List<String> exclusions = new ArrayList<>();
        boolean exclu = false;
        for (String mot : mots) {
            if (mot.equals("-")) {
                exclu = true;
                continue;
            }
            if (mot.startsWith("-")) {
                exclusions.add(mot.substring(1));
                continue;
            }
            if (mot.isEmpty() || mot.startsWith(".")) {
                throw new IllegalArgumentException(
                        "scope selector part " + position + " is malformed: "
                                + morceau);
            }
            if (exclu) {
                exclusions.add(mot);
            } else {
                elements.add(mot);
            }
        }
        if (elements.isEmpty()) {
            throw new IllegalArgumentException(
                    "scope selector part " + position + " has no scope: "
                            + morceau);
        }
        return new Complex(elements, exclusions);
    }

    /**
     * Le score du sélecteur contre une pile : la profondeur du dernier
     * élément couplé d'abord (plus c'est profond, plus c'est précis),
     * le nombre d'éléments couplés ensuite, la LONGUEUR du dernier
     * élément enfin — {@code keyword.operator} est plus précis que
     * {@code keyword} sur la même portée. Null quand aucun sélecteur
     * complexe ne matche.
     *
     * @param scopes les portées de la pile, de la racine au sommet
     * @return le score du meilleur sélecteur complexe, ou null
     */
    public @Nullable Integer score(List<String> scopes) {
        Integer meilleur = null;
        for (Complex complexe : complexes) {
            Integer score = complexe.score(scopes);
            if (score != null && (meilleur == null || score > meilleur)) {
                meilleur = score;
            }
        }
        return meilleur;
    }

    @Override
    public String toString() {
        return String.join(", ",
                complexes.stream().map(Object::toString).toList());
    }

    /** Un sélecteur complexe : éléments en descendant, exclusions. */
    private record Complex(List<String> elements, List<String> exclusions) {

        private @Nullable Integer score(List<String> scopes) {
            for (String exclusion : exclusions) {
                if (porteeDansLaPile(scopes, exclusion)) {
                    return null;
                }
            }
            int profondeur = -1;
            int couplable = 0;
            for (String element : elements) {
                profondeur = premierPlusProfond(scopes, element, profondeur);
                if (profondeur < 0) {
                    return null;
                }
                couplable++;
            }
            return profondeur * 8 + couplable * 4
                    + elements.get(elements.size() - 1).length();
        }

        /** La première position plus profonde qui matche, ou -1. */
        private static int premierPlusProfond(List<String> scopes,
                String element, int apres) {
            for (int i = apres + 1; i < scopes.size(); i++) {
                if (matche(scopes.get(i), element)) {
                    return i;
                }
            }
            return -1;
        }

        /** Vrai quand une portée de la pile matche l'élément donné. */
        private static boolean porteeDansLaPile(List<String> scopes,
                String element) {
            for (String portee : scopes) {
                if (matche(portee, element)) {
                    return true;
                }
            }
            return false;
        }

        /** Le préfixe hiérarchique : keyword matche keyword.operator. */
        private static boolean matche(String portee, String element) {
            if (!portee.startsWith(element)) {
                return false;
            }
            return portee.length() == element.length()
                    || portee.charAt(element.length()) == '.';
        }

        @Override
        public String toString() {
            return String.join(" ", elements)
                    + (exclusions.isEmpty() ? ""
                            : " - " + String.join(" ", exclusions));
        }
    }
}
