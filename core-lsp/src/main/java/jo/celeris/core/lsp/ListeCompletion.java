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
     * Construit la liste de ses éléments résolus — la voie de la
     * frontière protocolaire (ADR-015) : la correspondance y dépose
     * les éléments déjà lus de la bibliothèque, la coupure du serveur
     * portée par l'objet liste du protocole.
     *
     * @param incomplete le serveur annonce sa liste tronquée
     * @param elements   les éléments dans l'ordre exact du serveur
     * @return la liste construite
     */
    public static ListeCompletion de(boolean incomplete,
            List<ElementCompletion> elements) {
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
