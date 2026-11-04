package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Étape d'annulation unitaire : une liste ordonnée d'éditions, celle d'une
 * transaction appliquée (les frappes coalescées fusionnent dans la même
 * étape), bornée par les sélections d'avant et d'après (A-13).
 *
 * <p>Copie défensive à la construction (portage du dépôt de départ) : la
 * liste fournie par l'appelant peut être vidée après coup — une simple vue
 * non modifiable resterait une vue de la liste de l'appelant et l'étape
 * enregistrée perderait ses éditions.</p>
 *
 * <p>Les sélections sont {@code null} pour une étape empilée hors de tout
 * suivi de sélection (piles manipulées seules) : l'annulation d'une telle
 * étape ne touche alors pas à la sélection courante.</p>
 *
 * @param edits           les éditions de l'étape, dans leur ordre d'application
 * @param selectionBefore la sélection d'avant le geste, ou {@code null}
 * @param selectionAfter  la sélection d'après le geste, ou {@code null}
 */
public record UndoStep(List<TextEdit> edits,
        @Nullable MultiSelection selectionBefore,
        @Nullable MultiSelection selectionAfter) {

    /** Copie défensive — voir la Javadoc de la classe. */
    public UndoStep {
        edits = List.copyOf(edits);
    }

    /**
     * Une étape sans sélections enregistrées (piles manipulées seules).
     *
     * @param edits les éditions de l'étape, dans leur ordre d'application
     */
    public UndoStep(List<TextEdit> edits) {
        this(edits, null, null);
    }

    /** Une étape à édition unique, sans sélections enregistrées. */
    public static UndoStep of(TextEdit edit) {
        return new UndoStep(List.of(edit), null, null);
    }

    /**
     * L'étape inverse pour refaire : éditions reprises en ordre inverse,
     * chacune inversée, avec les éditions du rejeu en ordre d'application.
     * Les sélections portées sont conservées telles quelles — le rejeu de
     * la session les restitue à son propre sens.
     */
    public UndoStep inverse() {
        List<TextEdit> inverses = new ArrayList<>(edits.size());
        for (int i = edits.size() - 1; i >= 0; i--) {
            inverses.add(edits.get(i).inverse());
        }
        return new UndoStep(inverses, selectionBefore, selectionAfter);
    }
}
