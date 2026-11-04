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
     * Trace un rectangle plein aux coordonnées données.
     *
     * @param left   le bord gauche, en pixels
     * @param top    le bord supérieur, en pixels
     * @param right  le bord droit, en pixels
     * @param bottom le bord inférieur, en pixels
     * @param brush  le pinceau, couleur déjà posée
     */
    void drawRect(float left, float top, float right, float bottom, TextBrush brush);
}
