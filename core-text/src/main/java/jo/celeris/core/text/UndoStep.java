package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Étape d'annulation unitaire : une liste ordonnée d'éditions, celle d'une
 * transaction appliquée (les frappes coalescées fusionnent dans la même
 * étape).
 *
 * <p>Copie défensive à la construction (portage du dépôt de départ) : la
 * liste fournie par l'appelant peut être vidée après coup — une simple vue
 * non modifiable resterait une vue de la liste de l'appelant et l'étape
 * enregistrée perderait ses éditions.</p>
 *
 * <p>La restauration de sélection appartient à la multi-sélection (A-13,
 * tranches suivantes) : l'étape ne porte encore que des éditions.</p>
 *
 * @param edits les éditions de l'étape, dans leur ordre d'application
 */
public record UndoStep(List<TextEdit> edits) {

    /** Copie défensive — voir la Javadoc de la classe. */
    public UndoStep {
        edits = List.copyOf(edits);
    }

    /** Une étape à édition unique. */
    public static UndoStep of(TextEdit edit) {
        return new UndoStep(List.of(edit));
    }

    /**
     * L'étape inverse pour refaire : éditions reprises en ordre inverse,
     * chacune inversée, avec les éditions du rejeu en ordre d'application.
     */
    public UndoStep inverse() {
        List<TextEdit> inverses = new ArrayList<>(edits.size());
        for (int i = edits.size() - 1; i >= 0; i--) {
            inverses.add(edits.get(i).inverse());
        }
        return new UndoStep(inverses);
    }
}
