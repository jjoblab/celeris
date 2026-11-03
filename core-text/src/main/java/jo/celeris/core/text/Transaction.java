package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Transaction : ensemble immuable, composable et inversible d'éditions —
 * la seule voie de mutation du document (ADR-003).
 *
 * <p>Les éditions s'appliquent dans l'ordre : les offsets de chacune
 * s'entendent dans l'état du document après les précédentes. L'inverse
 * reprend les éditions en ordre inverse, chacune inversée — l'annulation
 * rejoue par la même voie que l'édition, jamais par une mutation directe
 * (PIEGE-07). Les éditions distantes (agents, hôtes) passent par là
 * également.</p>
 */
public final class Transaction {

    private final List<TextEdit> edits;

    private Transaction(List<TextEdit> edits) {
        this.edits = edits;
    }

    /**
     * Construit une transaction des éditions données.
     *
     * @param edits les éditions, dans leur ordre d'application
     */
    public static Transaction of(TextEdit... edits) {
        return of(Arrays.asList(edits));
    }

    /**
     * Construit une transaction de la liste d'éditions donnée (copie
     * défensive, liste non modifiable ensuite).
     *
     * @param edits les éditions, dans leur ordre d'application
     */
    public static Transaction of(List<TextEdit> edits) {
        return new Transaction(List.copyOf(edits));
    }

    /** Les éditions, dans leur ordre d'application, en lecture seule. */
    public List<TextEdit> edits() {
        return edits;
    }

    /** Vrai quand la transaction ne porte aucune édition. */
    public boolean isEmpty() {
        return edits.isEmpty();
    }

    /**
     * La transaction inverse : éditions reprises en ordre inverse,
     * chacune inversée.
     */
    public Transaction inverse() {
        List<TextEdit> inverses = new ArrayList<>(edits.size());
        for (int i = edits.size() - 1; i >= 0; i--) {
            inverses.add(edits.get(i).inverse());
        }
        return new Transaction(List.copyOf(inverses));
    }

    /**
     * Compose deux transactions : les éditions de la seconde s'entendent
     * dans l'état produit par la première.
     *
     * @param first  la première transaction
     * @param second la seconde transaction
     * @return la transaction composée
     */
    public static Transaction compose(Transaction first, Transaction second) {
        List<TextEdit> composees = new ArrayList<>(first.edits.size() + second.edits.size());
        composees.addAll(first.edits);
        composees.addAll(second.edits);
        return new Transaction(List.copyOf(composees));
    }
}
