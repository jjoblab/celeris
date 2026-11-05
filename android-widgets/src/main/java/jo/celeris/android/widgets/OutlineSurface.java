package jo.celeris.android.widgets;

/**
 * Surface de dessin du plan (B-16) : le proxy du canvas côté hôte — le
 * peintre ne connaît ni le canvas ni le pinceau, la vue adapte. Les
 * coordonnées et couleurs passent en primitives : jamais un objet n'est
 * alloué dans la boucle de dessin (PIEGE-15).
 */
public interface OutlineSurface {

    /**
     * Trace un texte à la position donnée.
     *
     * @param text   le texte à tracer
     * @param x      l'abscisse de départ, en pixels
     * @param y      l'ordonnée de la ligne de base, en pixels
     * @param couleur la couleur, en ARGB
     */
    void drawText(String text, float x, float y, int couleur);

    /**
     * Trace un trait entre deux points — le tiret des champs.
     *
     * @param x1      l'abscisse de départ, en pixels
     * @param y1      l'ordonnée de départ, en pixels
     * @param x2      l'abscisse d'arrivée, en pixels
     * @param y2      l'ordonnée d'arrivée, en pixels
     * @param couleur la couleur, en ARGB
     */
    void drawLine(float x1, float y1, float x2, float y2, int couleur);

    /**
     * Trace un rectangle plein — le carré des types.
     *
     * @param left    le bord gauche, en pixels
     * @param top     le bord supérieur, en pixels
     * @param right   le bord droit, en pixels
     * @param bottom  le bord inférieur, en pixels
     * @param couleur la couleur, en ARGB
     */
    void drawRect(float left, float top, float right, float bottom, int couleur);

    /**
     * Trace un disque plein — le cercle des fonctions.
     *
     * @param cx      l'abscisse du centre, en pixels
     * @param cy      l'ordonnée du centre, en pixels
     * @param rayon   le rayon, en pixels
     * @param couleur la couleur, en ARGB
     */
    void drawCircle(float cx, float cy, float rayon, int couleur);
}
