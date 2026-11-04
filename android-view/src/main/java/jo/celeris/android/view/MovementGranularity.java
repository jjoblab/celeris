package jo.celeris.android.view;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * Les granularités de mouvement d'accessibilité (G-05) : CARACTÈRE
 * par clusters de graphèmes, MOT par mots entiers, LIGNE par RANGÉES
 * visuelles sous coupures (A-18 — la lecture suit l'écran, jamais la
 * ligne logique), PAGE à la hauteur visible. Le calcul délègue au
 * moteur de déplacement du clavier matériel (D-17,
 * {@link CaretsMotion}) : une seule sémantique de mouvement, deux
 * portes d'entrée.
 */
public enum MovementGranularity {

    /** Un cluster de graphèmes à la fois. */
    CHARACTER,

    /** Un mot entier à la fois. */
    WORD,

    /** Une rangée visuelle à la fois, coupures comprises. */
    LINE,

    /** La hauteur visible à la fois. */
    PAGE;

    /**
     * L'offset suivant la granularité, depuis l'offset donné.
     *
     * @param document     l'instantané de document affiché
     * @param projection   la projection rangées ↔ lignes courante
     * @param offset       l'offset de départ
     * @param granularity  la granularité demandée
     * @param pageRows     la hauteur d'une page, en rangées
     * @return l'offset cible, borné au document
     */
    public static int next(Document document, VisualProjection projection,
            int offset, MovementGranularity granularity, int pageRows) {
        return cible(document, projection, offset, granularity, pageRows, true);
    }

    /**
     * L'offset précédent la granularité, depuis l'offset donné.
     *
     * @param document     l'instantané de document affiché
     * @param projection   la projection rangées ↔ lignes courante
     * @param offset       l'offset de départ
     * @param granularity  la granularité demandée
     * @param pageRows     la hauteur d'une page, en rangées
     * @return l'offset cible, borné au document
     */
    public static int previous(Document document, VisualProjection projection,
            int offset, MovementGranularity granularity, int pageRows) {
        return cible(document, projection, offset, granularity, pageRows, false);
    }

    private static int cible(Document document, VisualProjection projection,
            int offset, MovementGranularity granularity, int pageRows,
            boolean enAvant) {
        EditorAction action = switch (granularity) {
            case CHARACTER -> enAvant
                    ? EditorAction.MOVE_RIGHT : EditorAction.MOVE_LEFT;
            case WORD -> enAvant
                    ? EditorAction.MOVE_WORD_RIGHT : EditorAction.MOVE_WORD_LEFT;
            case LINE -> enAvant
                    ? EditorAction.MOVE_DOWN : EditorAction.MOVE_UP;
            case PAGE -> enAvant
                    ? EditorAction.PAGE_DOWN : EditorAction.PAGE_UP;
        };
        MultiSelection apres = CaretsMotion.move(document, projection,
                MultiSelection.of(SelRange.caretAt(offset)), action, pageRows);
        return apres.ranges().get(0).caret();
    }
}
