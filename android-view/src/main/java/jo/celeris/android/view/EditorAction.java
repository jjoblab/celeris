package jo.celeris.android.view;

/**
 * Les actions du clavier matériel (D-17) : mouvements, sélections et
 * gestes d'édition. Les suppressions, l'annulation et le retablissement
 * se passent au pilote de session ; les mouvements et sélections se
 * calculent par {@link CaretsMotion}.
 */
public enum EditorAction {
    /** Curseur d'un cluster à gauche. */
    MOVE_LEFT,
    /** Curseur d'un cluster à droite. */
    MOVE_RIGHT,
    /** Curseur d'une rangée en haut. */
    MOVE_UP,
    /** Curseur d'une rangée en bas. */
    MOVE_DOWN,
    /** Curseur d'un mot à gauche. */
    MOVE_WORD_LEFT,
    /** Curseur d'un mot à droite. */
    MOVE_WORD_RIGHT,
    /** Début de la rangée visuelle. */
    LINE_START,
    /** Fin de la rangée visuelle. */
    LINE_END,
    /** Une page (hauteur visible) en haut. */
    PAGE_UP,
    /** Une page (hauteur visible) en bas. */
    PAGE_DOWN,
    /** Origine du document. */
    DOC_START,
    /** Fin du document. */
    DOC_END,
    /** Étend la sélection d'un cluster à gauche, ancre figée. */
    SELECT_LEFT,
    /** Étend la sélection d'un cluster à droite, ancre figée. */
    SELECT_RIGHT,
    /** Étend la sélection d'une rangée en haut, ancre figée. */
    SELECT_UP,
    /** Étend la sélection d'une rangée en bas, ancre figée. */
    SELECT_DOWN,
    /** Étend la sélection d'un mot à gauche, ancre figée. */
    SELECT_WORD_LEFT,
    /** Étend la sélection d'un mot à droite, ancre figée. */
    SELECT_WORD_RIGHT,
    /** Étend la sélection jusqu'au début de la rangée. */
    SELECT_LINE_START,
    /** Étend la sélection jusqu'à la fin de la rangée. */
    SELECT_LINE_END,
    /** Sélectionne tout le document. */
    SELECT_ALL,
    /** Supprime en arrière par clusters de graphèmes. */
    DELETE_BACKWARD,
    /** Supprime en avant par clusters de graphèmes. */
    DELETE_FORWARD,
    /** Annule le dernier geste. */
    UNDO,
    /** Retablit le geste annulé. */
    REDO
}
