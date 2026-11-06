package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * La liste de complétion du protocole (B-04) : les éléments du serveur
 * dans l'ordre du serveur, la coupure honorée.
 *
 * <p>PIEGE-17 : une liste tronquée ({@code isIncomplete} vrai) est
 * honorée telle quelle — jamais réordonnée, jamais complétée
 * localement. Le filtrage garde l'ordre relatif ; le tri local, une
 * optimisation permise aux listes complètes, est REFUSÉ aux listes
 * tronquées : réordonner ce que le serveur a daigné envoyer gomme sa
 * priorité, y ajouter des mots locaux invente des candidats au-delà
 * de la coupure.</p>
 */
public final class ListeCompletion {

    private final boolean incomplete;
    private final List<ElementCompletion> elements;

    private ListeCompletion(boolean incomplete, List<ElementCompletion> elements) {
        this.incomplete = incomplete;
        this.elements = List.copyOf(elements);
    }

    /**
     * Analyse la réponse du serveur — la liste objet (avec sa coupure)
     * ou le tableau nu (toujours complet).
     *
     * @param json la valeur « result » de la requête de complétion
     * @return la liste lue, vide si la réponse est vide ou nulle
     */
    public static ListeCompletion de(ValeurJson json) {
        if (json instanceof JsonTableau tableau) {
            return deTableau(tableau, false);
        }
        if (!(json instanceof JsonObjet objet)) {
            return new ListeCompletion(false, List.of());
        }
        boolean incomplete = Boolean.TRUE.equals(objet.membreBooleen("isIncomplete"));
        return deTableau(objet.membreOu("items", JsonTableau.vide()), incomplete);
    }

    private static ListeCompletion deTableau(ValeurJson items, boolean incomplete) {
        List<ElementCompletion> elements = new ArrayList<>();
        if (items instanceof JsonTableau tableau) {
            for (int i = 0; i < tableau.taille(); i++) {
                elements.add(ElementCompletion.de(tableau.element(i)));
            }
        }
        return new ListeCompletion(incomplete, elements);
    }

    /** Vrai quand le serveur annonce sa liste tronquée. */
    public boolean estIncomplete() {
        return incomplete;
    }

    /** Les éléments dans l'ordre exact du serveur. */
    public List<ElementCompletion> elements() {
        return elements;
    }

    /**
     * Les éléments dont le texte de filtrage — ou le libellé à défaut —
     * commence par le préfixe, insensible à la casse. L'ordre relatif
     * du serveur est conservé, aucun élément n'est ajouté.
     *
     * @param prefixe le préfixe cherché, vide garde tout
     * @return la liste filtrée
     */
    public ListeCompletion filtrer(String prefixe) {
        if (prefixe.isEmpty()) {
            return this;
        }
        List<ElementCompletion> gardes = new ArrayList<>();
        for (ElementCompletion element : elements) {
            String texte = element.texteFiltre() == null
                    ? element.libelle() : element.texteFiltre();
            if (commencePar(texte, prefixe)) {
                gardes.add(element);
            }
        }
        return new ListeCompletion(incomplete, gardes);
    }

    /**
     * La liste triée par la clé de tri du serveur puis le libellé —
     * REFUSÉ sur une liste tronquée (PIEGE-17) : la même liste revient.
     *
     * @return la liste triée, ou this quand le serveur a tronqué
     */
    public ListeCompletion trier() {
        if (incomplete) {
            return this;
        }
        List<ElementCompletion> tries = new ArrayList<>(elements);
        tries.sort(Comparator.comparing(
                (ElementCompletion element) -> element.texteTri() == null
                        ? element.libelle() : element.texteTri())
                .thenComparing(ElementCompletion::libelle));
        return new ListeCompletion(false, tries);
    }

    private static boolean commencePar(String texte, String prefixe) {
        return texte.length() >= prefixe.length()
                && texte.regionMatches(true, 0, prefixe, 0, prefixe.length());
    }
}
