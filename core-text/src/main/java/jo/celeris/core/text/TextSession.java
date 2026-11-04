package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Session d'édition : détient l'instantané courant, les piles
 * d'annulation, les auditeurs d'édition et les projecteurs de positions.
 *
 * <p><b>La {@link Transaction} est la seule voie de mutation</b> (ADR-003) :
 * {@link #apply} valide, applique, projette et notifie ; l'annulation et le
 * refaire <em>rejouent par cette même voie</em> — projections comprises —
 * sans réenregistrer d'étape (PIEGE-07 : le chemin direct du dépôt de
 * départ désynchronisait annotations, plis et composition IME, contournait
 * la garde lecture seule et privait les auditeurs — pont LSP — de la
 * notification). {@link #replaceRange} est l'entrée commode d'une
 * transaction à édition unique : le texte retiré y est lu sur l'instantané
 * courant, jamais affirmé par l'appelant.</p>
 *
 * <p>Modèle d'exécution (ADR-007) : écrivain unique — la session vit sur
 * son fil d'écriture (fil d'interface, ou fil dédié de la cli), sans verrou
 * interne ; les lecteurs distants reçoivent l'{@link Document instantané
 * immuable}, lisible sans verrou depuis tout fil. L'horloge de coalescence
 * est injectée par l'hôte. La profondeur d'annulation par défaut est de
 * {@value #DEFAULT_MAX_DEPTH} étapes.</p>
 */
public final class TextSession {

    /** Profondeur d'annulation par défaut (portage du dépôt de départ). */
    public static final int DEFAULT_MAX_DEPTH = 1000;

    private Document document;
    private final UndoStack undoStack;
    private final List<EditListener> listeners = new ArrayList<>();
    private final List<PositionProjector> projectors = new ArrayList<>();
    private boolean readOnly;

    private TextSession(Document document, LongSupplier clock) {
        this.document = document;
        this.undoStack = new UndoStack(DEFAULT_MAX_DEPTH, clock);
    }

    /**
     * Crée une session sur le texte donné.
     *
     * @param text  le contenu initial
     * @param clock l'horloge de coalescence, injectée par l'hôte
     * @return la session prête à éditer
     */
    public static TextSession of(String text, LongSupplier clock) {
        return new TextSession(Document.of(text), clock);
    }

    /**
     * Crée une session sur l'instantané donné.
     *
     * @param document l'instantané initial
     * @param clock    l'horloge de coalescence, injectée par l'hôte
     * @return la session prête à éditer
     */
    public static TextSession of(Document document, LongSupplier clock) {
        return new TextSession(document, clock);
    }

    /** L'instantané courant — lisible sans verrou depuis tout fil. */
    public Document document() {
        return document;
    }

    /** Le texte complet de l'instantané courant. */
    public String text() {
        return document.text();
    }

    /** La longueur courante du document. */
    public int length() {
        return document.length();
    }

    /** Vrai quand la session refuse toute mutation. */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Pose la garde lecture seule : {@link #apply}, {@link #replaceRange},
     * {@link #undo} et {@link #redo} sont alors refusés — sans consommer
     * la moindre étape d'annulation.
     *
     * @param readOnly vrai pour verrouiller la session
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Abonne un auditeur d'édition (notifié aussi par l'annulation et le
     * refaire).
     *
     * @param listener l'auditeur
     */
    public void addEditListener(EditListener listener) {
        listeners.add(listener);
    }

    /**
     * Abonne un projecteur de positions (appelé aussi par l'annulation et
     * le refaire).
     *
     * @param projector le projecteur
     */
    public void addProjector(PositionProjector projector) {
        projectors.add(projector);
    }

    /**
     * Applique une transaction : validation des bornes (strictement, avant
     * toute mutation), application par la voie unique — projection des
     * positions puis notification des auditeurs, édition après édition —
     * puis enregistrement d'une étape d'annulation (coalescence comprise).
     *
     * @param transaction la transaction à appliquer
     * @return vrai si appliquée ; faux en lecture seule (rien ne bouge)
     * @throws IndexOutOfBoundsException si une édition sort du document
     * @throws IllegalStateException si le texte retiré d'une édition ne
     *         correspond pas au contenu courant
     */
    public boolean apply(Transaction transaction) {
        if (readOnly) {
            return false;
        }
        if (transaction.isEmpty()) {
            return true;
        }
        List<TextEdit> editions = transaction.edits();
        validerBornes(editions);
        appliquer(editions);
        enregistrer(editions);
        return true;
    }

    /**
     * Remplace la plage {@code [start, end)} par l'insertion donnée :
     * l'entrée commode d'une transaction à édition unique, le texte retiré
     * étant lu sur l'instantané courant. Une édition sans effet ne produit
     * ni mutation, ni étape, ni notification.
     *
     * @param start     offset de début (inclusif)
     * @param end       offset de fin (exclusif)
     * @param insertion le texte de remplacement
     * @return vrai si appliquée ; faux en lecture seule (rien ne bouge)
     * @throws IndexOutOfBoundsException si la plage sort du document
     */
    public boolean replaceRange(int start, int end, String insertion) {
        if (readOnly) {
            return false;
        }
        if (start == end && insertion.isEmpty()) {
            return true;
        }
        TextEdit edition = new TextEdit(start, document.subText(start, end), insertion);
        List<TextEdit> editions = List.of(edition);
        appliquer(editions);
        enregistrer(editions);
        return true;
    }

    /**
     * Annule la dernière étape : rejeu des éditions inverses, en ordre
     * inverse, PAR LA VOIE UNIQUE — projections et notifications comprises,
     * sans réenregistrer d'étape.
     *
     * @return vrai si une étape a été annulée ; faux si la pile est vide ou
     *         la session en lecture seule (l'étape reste alors disponible)
     */
    public boolean undo() {
        if (readOnly) {
            return false;
        }
        UndoStep step = undoStack.undo();
        if (step == null) {
            return false;
        }
        appliquer(editionsInverses(step));
        return true;
    }

    /**
     * Refait la dernière étape annulée : rejeu des éditions telles quelles,
     * PAR LA VOIE UNIQUE — sans empiler de nouvelle étape (un undo immédiat
     * revient à l'état d'origine, il ne saute pas d'étape).
     *
     * @return vrai si une étape a été refaite ; faux si la pile de refaire
     *         est vide ou la session en lecture seule
     */
    public boolean redo() {
        if (readOnly) {
            return false;
        }
        UndoStep step = undoStack.redo();
        if (step == null) {
            return false;
        }
        appliquer(step.edits());
        return true;
    }

    /** Vrai s'il existe une étape annulable. */
    public boolean canUndo() {
        return undoStack.canUndo();
    }

    /** Vrai s'il existe une étape à refaire. */
    public boolean canRedo() {
        return undoStack.canRedo();
    }

    /** Nombre d'étapes annulables. */
    public int undoDepth() {
        return undoStack.undoDepth();
    }

    /** Nombre d'étapes à refaire. */
    public int redoDepth() {
        return undoStack.redoDepth();
    }

    private static List<TextEdit> editionsInverses(UndoStep step) {
        List<TextEdit> inverses = new ArrayList<>(step.edits().size());
        for (int i = step.edits().size() - 1; i >= 0; i--) {
            inverses.add(step.edits().get(i).inverse());
        }
        return inverses;
    }

    private void validerBornes(List<TextEdit> editions) {
        int longueur = document.length();
        for (TextEdit edition : editions) {
            if (edition.end() > longueur) {
                throw new IndexOutOfBoundsException("edit [" + edition.start() + ", "
                        + edition.end() + ") exceeds document length " + longueur);
            }
            longueur += edition.inserted().length() - edition.removed().length();
        }
    }

    private void appliquer(List<TextEdit> editions) {
        for (TextEdit edition : editions) {
            String present = document.subText(edition.start(), edition.end());
            if (!present.equals(edition.removed())) {
                throw new IllegalStateException("removed text mismatch at ["
                        + edition.start() + ", " + edition.end() + "): expected \""
                        + edition.removed() + "\", found \"" + present + "\"");
            }
            document = document.replace(edition.start(), edition.end(), edition.inserted());
            EditSpan span = new EditSpan(edition.start(), edition.removed().length(),
                    edition.inserted().length());
            for (PositionProjector projector : projectors) {
                projector.project(span);
            }
            for (EditListener listener : listeners) {
                listener.onTextEdit(edition.start(), edition.end(), edition.removed(),
                        edition.inserted());
            }
        }
    }

    private void enregistrer(List<TextEdit> editions) {
        if (editions.size() == 1) {
            TextEdit edition = editions.get(0);
            if (undoStack.tryCoalesce(edition)) {
                return;
            }
            undoStack.pushStep(UndoStep.of(edition));
            undoStack.armNextCoalesce(edition);
        } else {
            // Un lot multi-éditions est une étape entière : la transaction
            // remplace les begin/endBatch du dépôt de départ.
            undoStack.pushStep(new UndoStep(editions));
        }
    }
}
