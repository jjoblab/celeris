package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

import org.jspecify.annotations.Nullable;

/**
 * Piles annuler/refaire avec coalescence des frappes simples — portage du
 * gestionnaire d'annulation du dépôt de départ (registre de portage).
 *
 * <p>Les frappes consécutives d'un seul caractère à des positions adjacentes
 * fusionnent en une seule étape d'annulation, dans la limite du délai
 * {@value #COALESCE_TIMEOUT_MS} ms. L'horloge est injectée (ADR-007) : les
 * tests obtiennent un temps déterministe, l'hôte fournit la sienne — le
 * moteur n'appelle jamais l'horloge système.</p>
 *
 * <p>La chaîne de coalescence est pilotée par {@link #armNextCoalesce} et
 * {@link #tryCoalesce} : {@link #pushStep} coupe toujours la chaîne (un lot,
 * un remplacement ou tout empilement direct ne doit jamais fusionner avec la
 * frappe suivante). Sans l'amorçage, la condition d'adjacence était
 * inatteignable et chaque caractère tapé créait une étape séparée — le
 * correctif est porté avec ses tests.</p>
 */
public final class UndoStack {

    /** Délai de coalescence des frappes simples, en millisecondes. */
    public static final long COALESCE_TIMEOUT_MS = 500;

    private final List<UndoStep> undoSteps = new ArrayList<>();
    private final List<UndoStep> redoSteps = new ArrayList<>();
    private final int maxDepth;
    private final LongSupplier clock;

    /** Fin de la chaîne de coalescence en cours (-1 : chaîne coupée). */
    private int chainEnd = -1;
    /** Horodatage de l'amorçage de la chaîne en cours. */
    private long chainTime = 0L;

    /**
     * Crée les piles.
     *
     * @param maxDepth profondeur maximale de la pile d'annulation (la plus
     *                 ancienne étape tombe au-delà)
     * @param clock    l'horloge injectée, en millisecondes croissantes
     * @throws IllegalArgumentException si {@code maxDepth < 1}
     */
    public UndoStack(int maxDepth, LongSupplier clock) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be >= 1: " + maxDepth);
        }
        this.maxDepth = maxDepth;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Empile une étape d'annulation : coupe la chaîne de coalescence et
     * vide la pile de refaire (une nouvelle édition invalide le refaire).
     *
     * @param step l'étape à empiler
     */
    public void pushStep(UndoStep step) {
        undoSteps.add(step);
        if (undoSteps.size() > maxDepth) {
            undoSteps.remove(0);
        }
        redoSteps.clear();
        cutChain();
    }

    /**
     * Amorce la chaîne de coalescence pour l'étape qui vient d'être
     * empilée : si c'est l'insertion simple d'un caractère, la frappe
     * suivante à cette position fusionnera avec elle ; tout autre type
     * d'édition coupe la chaîne.
     *
     * @param edit l'édition qui vient d'être empilée en étape propre
     */
    public void armNextCoalesce(TextEdit edit) {
        if (edit.inserted().length() == 1 && edit.removed().isEmpty()) {
            chainEnd = edit.start() + 1;
            chainTime = clock.getAsLong();
        } else {
            cutChain();
        }
    }

    /**
     * Tente de coalescer une frappe d'un seul caractère avec la dernière
     * étape d'annulation.
     *
     * @param edit la frappe à coalescer
     * @return vrai si elle a fusionné dans l'étape au sommet de la pile
     */
    public boolean tryCoalesce(TextEdit edit) {
        if (undoSteps.isEmpty()) {
            return false;
        }
        long now = clock.getAsLong();
        boolean coalescible = edit.inserted().length() == 1
                && edit.removed().isEmpty()
                && edit.start() == chainEnd
                && now - chainTime < COALESCE_TIMEOUT_MS;
        if (!coalescible) {
            return false;
        }
        UndoStep top = undoSteps.get(undoSteps.size() - 1);
        List<TextEdit> fusionnees = new ArrayList<>(top.edits());
        fusionnees.add(edit);
        undoSteps.set(undoSteps.size() - 1, new UndoStep(fusionnees));
        chainEnd = edit.start() + edit.inserted().length();
        chainTime = now;
        return true;
    }

    /**
     * Dépile la dernière étape d'annulation et la pousse sur la pile de
     * refaire ; la chaîne de coalescence est coupée.
     *
     * @return l'étape à rejouer en inverse, ou {@code null} si la pile est vide
     */
    public @Nullable UndoStep undo() {
        if (undoSteps.isEmpty()) {
            return null;
        }
        UndoStep step = undoSteps.remove(undoSteps.size() - 1);
        redoSteps.add(step);
        cutChain();
        return step;
    }

    /**
     * Dépile la dernière étape de refaire et la repose sur la pile
     * d'annulation — par ajout direct, jamais par {@link #pushStep} : le
     * refaire ne doit ni vider la pile de refaire restante ni passer par la
     * coupe de chaîne de l'édition nouvelle (PIEGE-07 : un undo immédiat
     * revient à l'état d'origine, il ne saute pas d'étape).
     *
     * @return l'étape à rejouer telle quelle, ou {@code null} si la pile est vide
     */
    public @Nullable UndoStep redo() {
        if (redoSteps.isEmpty()) {
            return null;
        }
        UndoStep step = redoSteps.remove(redoSteps.size() - 1);
        undoSteps.add(step);
        cutChain();
        return step;
    }

    /** Vrai s'il existe des étapes annulables. */
    public boolean canUndo() {
        return !undoSteps.isEmpty();
    }

    /** Vrai s'il existe des étapes à refaire. */
    public boolean canRedo() {
        return !redoSteps.isEmpty();
    }

    /** Vide les deux piles et coupe la chaîne de coalescence. */
    public void clear() {
        undoSteps.clear();
        redoSteps.clear();
        cutChain();
    }

    /** Nombre d'étapes annulables. */
    public int undoDepth() {
        return undoSteps.size();
    }

    /** Nombre d'étapes à refaire. */
    public int redoDepth() {
        return redoSteps.size();
    }

    private void cutChain() {
        chainEnd = -1;
        chainTime = 0L;
    }
}
