package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.List;

/**
 * Un tableau JSON : les éléments dans l'ordre d'écriture, immuable par
 * copie — {@link #ajouter} rend un nouveau tableau.
 */
public final class JsonTableau implements ValeurJson {

    private final List<ValeurJson> elements;

    private JsonTableau(List<ValeurJson> elements) {
        this.elements = elements;
    }

    /** Le tableau vide. */
    public static JsonTableau vide() {
        return new JsonTableau(new ArrayList<>());
    }

    /**
     * Rend un nouveau tableau : celui-ci plus l'élément donné.
     *
     * @param valeur l'élément ajouté à la fin
     * @return la copie étendue, this n'est pas modifié
     */
    public JsonTableau ajouter(ValeurJson valeur) {
        List<ValeurJson> copie = new ArrayList<>(elements);
        copie.add(valeur);
        return new JsonTableau(copie);
    }

    /** Le nombre d'éléments. */
    public int taille() {
        return elements.size();
    }

    /**
     * L'élément à la position donnée.
     *
     * @param position l'indice, à partir de zéro
     * @return l'élément
     * @throws IndexOutOfBoundsException si l'indice sort du tableau
     */
    public ValeurJson element(int position) {
        return elements.get(position);
    }

    /** Les éléments dans l'ordre — une copie, la modifier ne touche pas au tableau. */
    public List<ValeurJson> elements() {
        return new ArrayList<>(elements);
    }

    @Override
    public boolean equals(Object autre) {
        return autre instanceof JsonTableau tableau && elements.equals(tableau.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return JsonEcrivain.ecrire(this);
    }
}
