package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

import org.jspecify.annotations.Nullable;

/**
 * Piles annuler/refaire avec coalescence des frappes — portage du
 * gestionnaire d'annulation du dépôt de départ (registre de portage),
 * étendu à la frappe multi-curseurs.
 *
 * <p>Les frappes consécutives fusionnent en une seule étape d'annulation
 * dans la limite du délai {@value #COALESCE_TIMEOUT_MS} ms : un geste est
 * une frappe (insertion d'un seul caractère) ou une suppression arrière
 * (retrait d'un seul caractère) à chaque curseur, et les curseurs du geste
 * suivant doivent être exactement ceux laissés par le précédente — le cas
 * mono-curseur retrouve la règle d'adjacence du dépôt de départ.
 * L'horloge est injectée (ADR-007) : les tests obtiennent un temps
 * déterministe, l'hôte fournit la sienne — le moteur n'appelle jamais
 * l'horloge système.</p>
 *
 * <p>La chaîne de coalescence est pilotée par {@link #armNextCoalesce} et
 * {@link #tryCoalesce} : {@link #pushStep} coupe toujours la chaîne (un lot,
 * un remplacement ou tout empilement direct ne doit jamais fusionner avec la
 * frappe suivante). Sans l'amorçage, la condition d'adjacence était
 * inatteignable et chaque caractère tapé créait une étape séparée — le
 * correctif est porté avec ses tests.</p>
 */
public final class UndoStack {

    /** Délai de coalescence des frappes, en millisecondes. */
    public static final long COALESCE_TIMEOUT_MS = 500;

    private final List<UndoStep> undoSteps = new ArrayList<>();
    private final List<UndoStep> redoSteps = new ArrayList<>();
    private final int maxDepth;
    private final LongSupplier clock;

    /** Curseurs attendus de la prochaine frappe (vide : chaîne coupée). */
    private List<Integer> chainEnds = List.of();

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
     * Amorce la chaîne de coalescence pour le geste qui vient d'être
     * empilé en étape propre : si chaque édition est une frappe simple
     * (insertion d'un seul caractère) ou une suppression arrière simple
     * (retrait d'un seul caractère), le geste suivant sur ces mêmes
     * curseurs fusionnera avec elle ; tout autre type d'édition coupe la
     * chaîne.
     *
     * @param edit l'édition qui vient d'être empilée en étape propre
     */
    public void armNextCoalesce(TextEdit edit) {
        armNextCoalesce(List.of(edit));
    }

    /**
     * Amorce la chaîne de coalescence pour le lot d'éditions qui vient
     * d'être empilé en étape propre (frappe ou suppression
     * multi-curseurs).
     *
     * @param edits les éditions du geste, dans leur ordre d'application
     */
    public void armNextCoalesce(List<TextEdit> edits) {
        List<Integer> carets = caretsAfterKeystroke(edits);
        if (carets == null) {
            cutChain();
        } else {
            chainEnds = carets;
            chainTime = clock.getAsLong();
        }
    }

    /**
     * Tente de coalescer une frappe d'un seul caractère avec la dernière
     * étape d'annulation, sans suivi de sélection.
     *
     * @param edit la frappe à coalescer
     * @return vrai si elle a fusionné dans l'étape au sommet de la pile
     */
    public boolean tryCoalesce(TextEdit edit) {
        return tryCoalesce(List.of(edit), null);
    }

    /**
     * Tente de coalescer un geste (une frappe ou une suppression d'un
     * seul caractère par curseur) avec la dernière étape d'annulation :
     * les curseurs du geste doivent être exactement ceux laissés par le
     * geste précédent, dans la fenêtre de coalescence. La sélection
     * d'après de l'étape fusionnée devient celle du geste courant ; la
     * sélection d'avant reste celle du premier geste de la chaîne.
     *
     * @param edits          les éditions du geste, dans leur ordre
     *                       d'application
     * @param selectionAfter la sélection d'après le geste courant, ou
     *                       {@code null} pour conserver celle de l'étape
     * @return vrai si le geste a fusionné dans l'étape au sommet
     */
    public boolean tryCoalesce(List<TextEdit> edits, @Nullable MultiSelection selectionAfter) {
        if (undoSteps.isEmpty() || chainEnds.isEmpty()) {
            return false;
        }
        List<Integer> carets = caretsAfterKeystroke(edits);
        long now = clock.getAsLong();
        if (carets == null || carets.size() != chainEnds.size()
                || !landingPositions(edits).equals(chainEnds)
                || now - chainTime >= COALESCE_TIMEOUT_MS) {
            return false;
        }
        UndoStep top = undoSteps.get(undoSteps.size() - 1);
        List<TextEdit> fusionnees = new ArrayList<>(top.edits());
        fusionnees.addAll(edits);
        MultiSelection apres = selectionAfter != null ? selectionAfter : top.selectionAfter();
        undoSteps.set(undoSteps.size() - 1,
                new UndoStep(fusionnees, top.selectionBefore(), apres));
        chainEnds = carets;
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

    /**
     * Les curseurs laissés par un geste : {@code null} si une quelconque
     * édition n'est ni une frappe simple ni une suppression simple. Le
     * curseur suit son insertion ({@code start + 1}) ou se pose sur le
     * départ de sa suppression ({@code start}) : les éditions d'un geste
     * sont ordonnées, aucune ne décale les curseurs précédents.
     */
    private static @Nullable List<Integer> caretsAfterKeystroke(List<TextEdit> edits) {
        List<Integer> carets = new ArrayList<>(edits.size());
        for (TextEdit edit : edits) {
            boolean insertion = edit.removed().isEmpty() && edit.inserted().length() == 1;
            boolean deletion = edit.inserted().isEmpty() && edit.removed().length() == 1;
            if (!insertion && !deletion) {
                return null;
            }
            carets.add(insertion ? edit.start() + 1 : edit.start());
        }
        return carets;
    }

    /**
     * Les positions des curseurs qui ont produit le geste, dans les
     * coordonnées d'avant la transaction : la frappe atterrit sur son
     * insertion, la suppression arrière sur la fin de la plage retirée —
     * chaque édition séquentielle étant ramenée du décalage cumulé des
     * précédentes.
     */
    private static List<Integer> landingPositions(List<TextEdit> edits) {
        List<Integer> positions = new ArrayList<>(edits.size());
        int shift = 0;
        for (TextEdit edit : edits) {
            boolean insertion = edit.removed().isEmpty() && edit.inserted().length() == 1;
            positions.add(insertion ? edit.start() - shift : edit.start() + 1 - shift);
            shift += edit.inserted().length() - edit.removed().length();
        }
        return positions;
    }

    private void cutChain() {
        chainEnds = List.of();
        chainTime = 0L;
    }
}
