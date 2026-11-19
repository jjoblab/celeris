package jo.celeris.core.lsp.types;

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
public final class CompletionListLsp {

    private final boolean incomplete;
    private final List<CompletionItemLsp> elements;

    private CompletionListLsp(boolean incomplete, List<CompletionItemLsp> elements) {
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
    public static CompletionListLsp of(boolean incomplete,
            List<CompletionItemLsp> elements) {
        return new CompletionListLsp(incomplete, elements);
    }

    /** Vrai quand le serveur annonce sa liste tronquée. */
    public boolean isIncomplete() {
        return incomplete;
    }

    /** Les éléments dans l'ordre exact du serveur. */
    public List<CompletionItemLsp> elements() {
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
    public CompletionListLsp filter(String prefixe) {
        if (prefixe.isEmpty()) {
            return this;
        }
        List<CompletionItemLsp> gardes = new ArrayList<>();
        for (CompletionItemLsp element : elements) {
            String text = element.filterText() == null
                    ? element.label() : element.filterText();
            if (commencePar(text, prefixe)) {
                gardes.add(element);
            }
        }
        return new CompletionListLsp(incomplete, gardes);
    }

    /**
     * La liste triée par la clé de tri du serveur puis le libellé —
     * REFUSÉ sur une liste tronquée (PIEGE-17) : la même liste revient.
     *
     * @return la liste triée, ou this quand le serveur a tronqué
     */
    public CompletionListLsp sort() {
        if (incomplete) {
            return this;
        }
        List<CompletionItemLsp> tries = new ArrayList<>(elements);
        tries.sort(Comparator.comparing(
                (CompletionItemLsp element) -> element.sortText() == null
                        ? element.label() : element.sortText())
                .thenComparing(CompletionItemLsp::label));
        return new CompletionListLsp(false, tries);
    }

    private static boolean commencePar(String text, String prefixe) {
        return text.length() >= prefixe.length()
                && text.regionMatches(true, 0, prefixe, 0, prefixe.length());
    }
}
