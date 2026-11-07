package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Session d'édition : détient l'instantané courant, la sélection multiple,
 * les piles d'annulation, les auditeurs d'édition et les projecteurs de
 * positions.
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
 * <p>La sélection multiple est de première classe (ADR-003, parité A-01) :
 * la session la détient, la projette à travers chaque édition de la voie
 * unique — éditions distantes et rejeu de l'annulation compris — et chaque
 * étape d'historique restitue la sélection exacte d'avant à l'annulation,
 * d'après au rétablissement (A-13 : pluralité, direction et positions
 * comprises). {@link #type} frappe le même texte sur toutes les plages de
 * la sélection : la frappe multi-curseurs est une transaction ordinaire.</p>
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
    private final LongSupplier clock;
    private final List<EditListener> listeners = new ArrayList<>();
    private final List<SelectionListener> selectionListeners = new ArrayList<>();
    private final List<PositionProjector> projectors = new ArrayList<>();
    private MultiSelection selection = MultiSelection.caretsAt(0);
    private boolean readOnly;

    private TextSession(Document document, LongSupplier clock) {
        this.document = document;
        this.clock = clock;
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
     * La sélection multiple courante (ADR-003) — un curseur à l'origine sur
     * une session neuve.
     *
     * @return la sélection courante, projetée par chaque édition de la voie
     *         unique
     */
    public MultiSelection selection() {
        return selection;
    }

    /**
     * Remplace la sélection courante. La sélection ne produit aucune entrée
     * d'historique : seules les transactions s'annulent, et chaque étape
     * restitue la sélection enregistrée avec elle (A-13).
     *
     * @param newSelection la sélection, dont chaque plage doit rester dans
     *        les bornes du document
     * @throws IllegalArgumentException si une plage sort du document
     */
    public void setSelection(MultiSelection newSelection) {
        for (SelRange range : newSelection.ranges()) {
            if (range.end() > document.length()) {
                throw new IllegalArgumentException("selection " + range
                        + " beyond document length " + document.length());
            }
        }
        MultiSelection avant = selection;
        selection = newSelection;
        notifierSelection(avant);
    }

    /**
     * Pose la garde lecture seule : {@link #apply}, {@link #replaceRange},
     * {@link #type}, {@link #undo} et {@link #redo} sont alors refusés —
     * sans consommer la moindre étape d'annulation.
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
     * Désabonne un auditeur d'édition — le détachement explicite évite de
     * retenir un pont (analyse syntaxique, LSP) sur une session qui vit.
     *
     * @param listener l'auditeur à détacher
     */
    public void removeEditListener(EditListener listener) {
        listeners.remove(listener);
    }

    /**
     * Abonne un auditeur de sélection (notifié aussi par la projection
     * des éditions, l'annulation et le refaire) — la barre de sélection
     * des popups (M7.5, §7) y vit, de toute source de changement.
     *
     * @param listener l'auditeur
     */
    public void addSelectionListener(SelectionListener listener) {
        selectionListeners.add(listener);
    }

    /**
     * Désabonne un auditeur de sélection.
     *
     * @param listener l'auditeur à détacher
     */
    public void removeSelectionListener(SelectionListener listener) {
        selectionListeners.remove(listener);
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
     * positions et de la sélection puis notification des auditeurs, édition
     * après édition — puis enregistrement d'une étape d'annulation
     * (coalescence comprise).
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
        MultiSelection avant = selection;
        appliquer(editions);
        enregistrer(editions, avant, selection);
        notifierSelection(avant);
        return true;
    }

    /**
     * Applique une transaction avec sa sélection d'après explicite : le
     * geste intelligent (M5) place ses curseurs là où l'usage les attend —
     * entre les deux caractères d'une paire insérée, après l'indentation
     * d'une Entrée — là où la projection automatique ne peut pas les
     * deviner. L'étape d'annulation enregistre la sélection explicite :
     * annuler restitue celle d'avant (A-13), refaire restitue l'explicite.
     * Une transaction vide déplace la sélection sans créer d'étape (le
     * saut par-dessus un fermant n'est pas une édition).
     *
     * @param transaction     la transaction à appliquer
     * @param selectionAfter  la sélection d'après le geste, dans les
     *                        coordonnées du document résultant
     * @return vrai si appliquée ; faux en lecture seule (rien ne bouge)
     * @throws IndexOutOfBoundsException si une édition sort du document
     * @throws IllegalStateException si le texte retiré d'une édition ne
     *         correspond pas au contenu courant
     */
    public boolean apply(Transaction transaction, MultiSelection selectionAfter) {
        if (readOnly) {
            return false;
        }
        if (transaction.isEmpty()) {
            MultiSelection avant = selection;
            selection = selectionAfter;
            notifierSelection(avant);
            return true;
        }
        List<TextEdit> editions = transaction.edits();
        validerBornes(editions);
        MultiSelection avant = selection;
        appliquer(editions);
        selection = selectionAfter;
        enregistrer(editions, avant, selectionAfter);
        notifierSelection(avant);
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
        MultiSelection avant = selection;
        appliquer(editions);
        enregistrer(editions, avant, selection);
        notifierSelection(avant);
        return true;
    }

    /**
     * Frappe le texte donné sur toutes les plages de la sélection : la
     * frappe multi-curseurs (parité A-01), une transaction ordinaire par la
     * voie unique. Chaque plage est remplacée par le texte, les curseurs se
     * posent après l'insertion qui les concerne, et le geste coalescence
     * avec les frappes précédentes dans la fenêtre de l'historique (A-13 :
     * annuler restitue le texte et toutes les sélections d'avant).
     *
     * @param text le texte à frapper sur chaque plage
     * @return vrai si la frappe a eu lieu ; faux en lecture seule, sur une
     *         sélection vide ou si rien ne change
     */
    public boolean type(String text) {
        if (readOnly || selection.isEmpty()) {
            return false;
        }
        List<TextEdit> editions = frappeSur(selection, text);
        if (editions.isEmpty()) {
            return false;
        }
        MultiSelection avant = selection;
        validerBornes(editions);
        appliquer(editions);
        enregistrer(editions, avant, selection);
        notifierSelection(avant);
        return true;
    }

    /**
     * Supprime en arrière, curseur par curseur, par clusters de
     * graphèmes entiers (ADR-003 : jamais une demi-paire de substitut,
     * jamais un emoji amputé de son sélecteur).
     *
     * <p>Une plage non vide est remplacée par le vide ; un curseur retire
     * le cluster qui le précède ; les plages de suppression issues de
     * curseurs voisins fusionnent si elles se touchent. Le geste
     * coalescence avec la frappe dans la fenêtre de l'historique et
     * s'annule avec restauration de la sélection (A-13).</p>
     *
     * @return vrai si une suppression a eu lieu ; faux en lecture seule,
     *         sur une sélection vide ou curseur à l'origine du document
     */
    public boolean deleteBackward() {
        if (readOnly || selection.isEmpty()) {
            return false;
        }
        List<TextEdit> editions = new ClusterDeletes(document).backward(selection);
        if (editions.isEmpty()) {
            return false;
        }
        MultiSelection avant = selection;
        validerBornes(editions);
        appliquer(editions);
        enregistrer(editions, avant, selection);
        notifierSelection(avant);
        return true;
    }

    /**
     * Supprime en avant, curseur par curseur, par clusters de graphèmes
     * entiers (ADR-003) — la discipline symétrique de la suppression
     * arrière, exigée par le clavier matériel (D-17).
     *
     * <p>Une plage non vide est remplacée par le vide ; un curseur retire
     * le cluster qui le suit ; les plages de suppression issues de
     * curseurs voisins fusionnent si elles se touchent. Le geste
     * coalescence avec la frappe dans la fenêtre de l'historique et
     * s'annule avec restauration de la sélection (A-13).</p>
     *
     * @return vrai si une suppression a eu lieu ; faux en lecture seule,
     *         sur une sélection vide ou un curseur en fin de document
     */
    public boolean deleteForward() {
        if (readOnly || selection.isEmpty()) {
            return false;
        }
        List<TextEdit> editions = new ClusterDeletes(document).forward(selection);
        if (editions.isEmpty()) {
            return false;
        }
        MultiSelection avant = selection;
        validerBornes(editions);
        appliquer(editions);
        enregistrer(editions, avant, selection);
        notifierSelection(avant);
        return true;
    }

    /**
     * Remplace TOUTES les occurrences de la requête par le remplacement
     * donné — moteur {@link FindReplace} à l'horloge de la session
     * (ADR-007), lecture du texte courant puis mutation PAR LA VOIE
     * UNIQUE (famille PIEGE-07) : les éditions, une par occurrence en
     * ordre du document, sont ajustées en deltas cumulés pour la
     * sémantique séquentielle de la {@link Transaction}. Tout le
     * remplacement forme UNE étape d'annulation — jamais coalescée avec
     * les frappes (une commande n'est pas une frappe). Les modes regex
     * (groupes {@code $n} développés), mot entier et casse suivent les
     * options ; un motif trop coûteux ou invalide ne mute rien. La passe
     * est plafonnée à {@link FindReplace#MAX_MATCHES} remplacements (état
     * {@code truncated} du moteur pur — l'appelant qui veut les signaux
     * l'utilise directement).
     *
     * @param query       la requête de recherche
     * @param replacement le texte de remplacement
     * @param opts        les options (casse, mot entier, regex)
     * @return le nombre de remplacements effectués
     */
    public int replaceAll(String query, String replacement, FindOptions opts) {
        if (readOnly) {
            return 0;
        }
        FindReplace moteur = FindReplace.of(clock);
        FindReplace.ReplaceResult passe = moteur.replaceAll(document.text(), query,
                replacement, opts);
        List<TextEdit> passees = passe.edits();
        if (passees.isEmpty()) {
            return 0;
        }
        List<TextEdit> ajustees = new ArrayList<>(passees.size());
        int delta = 0;
        for (TextEdit edition : passees) {
            ajustees.add(new TextEdit(edition.start() + delta, edition.removed(),
                    edition.inserted()));
            delta += edition.inserted().length() - edition.removed().length();
        }
        MultiSelection avant = selection;
        appliquer(ajustees);
        undoStack.pushStep(new UndoStep(ajustees, avant, selection));
        notifierSelection(avant);
        return ajustees.size();
    }

    /**
     * Annule la dernière étape : rejeu des éditions inverses, en ordre
     * inverse, PAR LA VOIE UNIQUE — projections et notifications comprises,
     * sans réenregistrer d'étape — puis restitution de la sélection
     * exacte d'avant le geste (A-13 : pluralité, direction et positions
     * comprises).
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
        MultiSelection avant = selection;
        if (step.selectionBefore() != null) {
            selection = step.selectionBefore();
        }
        notifierSelection(avant);
        return true;
    }

    /**
     * Refait la dernière étape annulée : rejeu des éditions telles quelles,
     * PAR LA VOIE UNIQUE — sans empiler de nouvelle étape (un undo immédiat
     * revient à l'état d'origine, il ne saute pas d'étape) — puis
     * restitution de la sélection d'après le geste (A-13).
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
        MultiSelection avant = selection;
        if (step.selectionAfter() != null) {
            selection = step.selectionAfter();
        }
        notifierSelection(avant);
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

    private void notifierSelection(MultiSelection avant) {
        if (avant.equals(selection)) {
            return;
        }
        for (SelectionListener listener : selectionListeners) {
            listener.onSelectionChanged(selection);
        }
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
            selection = selection.project(span);
            for (PositionProjector projector : projectors) {
                projector.project(span);
            }
            for (EditListener listener : listeners) {
                listener.onTextEdit(edition.start(), edition.end(), edition.removed(),
                        edition.inserted());
            }
        }
    }

    /**
     * Les éditions de la frappe du texte sur chaque plage de la sélection :
     * plages dans l'ordre du document, positions séquentielles ajustées du
     * décalage cumulé pour la sémantique de la {@link Transaction} — le
     * texte retiré se lit aux positions d'origine, les caractères sont les
     * mêmes. Une plage qui ne change rien (curseur sur un texte vide) ne
     * produit pas d'édition.
     */
    private List<TextEdit> frappeSur(MultiSelection aFrapper, String texte) {
        List<TextEdit> editions = new ArrayList<>(aFrapper.caretCount());
        int decalage = 0;
        for (SelRange plage : aFrapper.ranges()) {
            String retire = document.subText(plage.start(), plage.end());
            if (!retire.isEmpty() || !texte.isEmpty()) {
                editions.add(new TextEdit(plage.start() + decalage, retire, texte));
                decalage += texte.length() - retire.length();
            }
        }
        return editions;
    }

    private void enregistrer(List<TextEdit> editions, MultiSelection avant,
            MultiSelection apres) {
        if (undoStack.tryCoalesce(editions, apres)) {
            return;
        }
        // Une étape entière : le lot remplace les begin/endBatch du dépôt de
        // départ ; la frappe (une insertion d'un caractère par curseur)
        // amorce la chaîne de coalescence.
        undoStack.pushStep(new UndoStep(editions, avant, apres));
        undoStack.armNextCoalesce(editions);
    }
}
