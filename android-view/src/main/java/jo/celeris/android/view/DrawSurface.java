package jo.celeris.android.view;

/**
 * Surface de dessin d'une image : le proxy du canvas côté hôte (le spike
 * de rendu a validé l'approche proxy JVM, ADR-006).
 *
 * <p>PIEGE-15 : les coordonnées de rectangle passent en primitives —
 * jamais un objet rectangle n'est alloué dans la boucle de dessin ; les
 * chaînes et pinceaux qui croisent cette interface sont réutilisés
 * d'une image à l'autre en régime établi.</p>
 */
public interface DrawSurface {

    /**
     * Trace un texte à la position donnée, avec le pinceau donné.
     *
     * @param text  le texte à tracer (instance réutilisée en régime établi)
     * @param x     l'abscisse de départ, en pixels
     * @param y     l'ordonnée de la ligne de base, en pixels
     * @param brush le pinceau, couleur déjà posée
     */
    void drawText(String text, float x, float y, TextBrush brush);

    /**
     * Trace une tranche de texte à la position donnée, avec le
     * pinceau donné — la variante des segments bidirectionnels
     * (ADR-008) : la tranche se sert sans copie ni allocation.
     *
     * @param text  le texte porteur (instance réutilisée en régime établi)
     * @param start le début de la tranche, en unités
     * @param end   la fin exclusive de la tranche, en unités
     * @param x     l'abscisse de départ, en pixels
     * @param y     l'ordonnée de la ligne de base, en pixels
     * @param brush le pinceau, couleur déjà posée
     */
    default void drawText(CharSequence text, int start, int end, float x,
            float y, TextBrush brush) {
        drawText(text.subSequence(start, end).toString(), x, y, brush);
    }

    /**
     * Trace un rectangle plein aux coordonnées données.
     *
     * @param left   le bord gauche, en pixels
     * @param top    le bord supérieur, en pixels
     * @param right  le bord droit, en pixels
     * @param bottom le bord inférieur, en pixels
     * @param brush  le pinceau, couleur déjà posée
     */
    void drawRect(float left, float top, float right, float bottom, TextBrush brush);

    /**
     * Trace un trait entre deux points — les chevrons de pli de la
     * gouttière (D-07) s'y servent, en primitives, sans allocation
     * (PIEGE-15).
     *
     * @param x1    l'abscisse de départ, en pixels
     * @param y1    l'ordonnée de départ, en pixels
     * @param x2    l'abscisse d'arrivée, en pixels
     * @param y2    l'ordonnée d'arrivée, en pixels
     * @param brush le pinceau, couleur déjà posée
     */
    default void drawLine(float x1, float y1, float x2, float y2, TextBrush brush) {
        // les surfaces qui ne tracent pas de trait l'ignorent : la
        // gouttière sans marqueurs n'en dépend jamais
    }

    /**
     * Trace un cercle — les points d'arrêt de la gouttière (D-04) s'y
     * servent : plein posé, creux désactivé. L'épaisseur du pinceau
     * porte le contour du cercle creux.
     *
     * @param cx     l'abscisse du centre, en pixels
     * @param cy     l'ordonnée du centre, en pixels
     * @param rayon  le rayon, en pixels
     * @param plein  vrai pour le disque plein, faux pour le contour seul
     * @param brush  le pinceau, couleur et épaisseur déjà posées
     */
    default void drawCircle(float cx, float cy, float rayon, boolean plein,
            TextBrush brush) {
        // les surfaces qui ne tracent pas de cercle l'ignorent
    }

    /**
     * Trace un triangle PLEIN — les chevrons de pli (D-07) et la
     * flèche d'exécution (D-05) s'y servent : pointant en bas quand
     * la région s'ouvre, à droite quand elle vit repliée. Le demi-côté
     * {@code rayon} porte l'étendue du glyphe.
     *
     * @param centreX  l'abscisse du centre, en pixels
     * @param centreY  l'ordonnée du centre, en pixels
     * @param rayon    le demi-côté du triangle, en pixels
     * @param pointeEnBas vrai pour la pointe en bas (région ouvrante),
     *                    faux pour la pointe à droite (région repliée)
     * @param brush    le pinceau, couleur déjà posée
     */
    default void drawTriangle(float centreX, float centreY, float rayon,
            boolean pointeEnBas, TextBrush brush) {
        // les surfaces qui ne tracent pas de triangle l'ignorent
    }
}
