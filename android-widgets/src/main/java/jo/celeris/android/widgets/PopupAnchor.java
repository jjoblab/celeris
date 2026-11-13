package jo.celeris.android.widgets;

/**
 * L'ancrage des popups portés par la couche (M7.5, §7 — les dix
 * popups, reprise) : autour du curseur (complétion, intentions),
 * au côté opposé (signature), près du point (survol, info-bulle),
 * en bandeau haut (fil d'Ariane), au bord droit (bande d'erreurs),
 * centrée sur la sélection (barre de sélection) — chaque position
 * BORNÉE à la couche : un popup ne sort jamais de l'écran. Pur et
 * testable sans SDK : le placement coordonné de la complétion, de sa
 * documentation et de la signature vit déjà chez {@link PopupPlacer}
 * (D-10) — ici vivent les ancres simples des autres popups.
 */
public final class PopupAnchor {

    private PopupAnchor() {
    }

    /**
     * Sous le curseur si la place y tient, au-dessus sinon —
     * l'ancre de la complétion et du menu d'intentions.
     *
     * @param caretX      l'abscisse du curseur, en pixels de couche
     * @param caretBottom le bas du rectangle du curseur
     * @param popup       la taille du popup
     * @param layerWidth  la largeur de la couche
     * @param layerHeight la hauteur de la couche
     * @param margin      la respiration aux bords, en pixels
     * @return le cadre borné du popup
     */
    public static PopupPlacer.Cadre belowOrAboveCaret(int caretX, int caretBottom,
            PopupPlacer.Cadre popup, int layerWidth, int layerHeight, int margin) {
        int x = borneX(caretX, popup, layerWidth, margin);
        boolean dessous = caretBottom + popup.hauteur() <= layerHeight - margin;
        int y = dessous ? caretBottom
                : Math.max(margin, caretBottom - popup.hauteur());
        return new PopupPlacer.Cadre(x, y, popup.largeur(), popup.hauteur());
    }

    /**
     * Au-dessus du curseur si la place y tient, en dessous sinon —
     * l'ancre de l'aide aux paramètres, au côté opposé de la
     * complétion.
     *
     * @param caretX   l'abscisse du curseur, en pixels de couche
     * @param caretTop le haut du rectangle du curseur
     * @param popup    la taille du popup
     * @param layerWidth  la largeur de la couche
     * @param layerHeight la hauteur de la couche
     * @param margin   la respiration aux bords, en pixels
     * @return le cadre borné du popup
     */
    public static PopupPlacer.Cadre aboveOrBelowCaret(int caretX, int caretTop,
            PopupPlacer.Cadre popup, int layerWidth, int layerHeight, int margin) {
        int x = borneX(caretX, popup, layerWidth, margin);
        boolean dessus = caretTop - popup.hauteur() >= margin;
        int y = dessus ? caretTop - popup.hauteur()
                : Math.min(layerHeight - margin - popup.hauteur(), caretTop);
        return new PopupPlacer.Cadre(x, Math.max(margin, y),
                popup.largeur(), popup.hauteur());
    }

    /**
     * Près du point touché, au-dessus décalé — l'ancre du survol et
     * de l'info-bulle : la bulle ne recouvre jamais le doigt.
     *
     * @param pointX l'abscisse du point, en pixels de couche
     * @param pointY l'ordonnée du point
     * @param popup  la taille du popup
     * @param layerWidth  la largeur de la couche
     * @param layerHeight la hauteur de la couche
     * @param margin la respiration aux bords, en pixels
     * @return le cadre borné du popup
     */
    public static PopupPlacer.Cadre atPoint(int pointX, int pointY,
            PopupPlacer.Cadre popup, int layerWidth, int layerHeight, int margin) {
        int x = borneX(pointX - popup.largeur() / 2, popup, layerWidth, margin);
        int y = pointY - popup.hauteur() >= margin
                ? pointY - popup.hauteur()
                : Math.min(layerHeight - margin - popup.hauteur(), pointY);
        return new PopupPlacer.Cadre(x, Math.max(margin, y),
                popup.largeur(), popup.hauteur());
    }

    /**
     * Le bandeau haut-gauche de la couche — l'ancre du fil d'Ariane.
     *
     * @param popup    la taille du popup
     * @param layerWidth  la largeur de la couche
     * @param layerHeight la hauteur de la couche
     * @param margin   la respiration aux bords, en pixels
     * @return le cadre borné du popup
     */
    public static PopupPlacer.Cadre topLeftCorner(PopupPlacer.Cadre popup,
            int layerWidth, int layerHeight, int margin) {
        return new PopupPlacer.Cadre(margin, margin,
                Math.min(popup.largeur(), layerWidth - 2 * margin),
                Math.min(popup.hauteur(), layerHeight - 2 * margin));
    }

    /**
     * Le bord droit, pleine hauteur de couche — l'ancre de la bande
     * d'erreurs, parallèle au défilement (D-08).
     *
     * @param popup    la taille du popup
     * @param layerWidth  la largeur de la couche
     * @param layerHeight la hauteur de la couche
     * @param margin   la respiration aux bords, en pixels
     * @return le cadre borné du popup
     */
    public static PopupPlacer.Cadre rightEdge(PopupPlacer.Cadre popup,
            int layerWidth, int layerHeight, int margin) {
        return new PopupPlacer.Cadre(
                Math.max(margin, layerWidth - popup.largeur() - margin),
                margin, popup.largeur(),
                Math.max(margin, layerHeight - 2 * margin));
    }

    /**
     * Le coin bas-droit de la couche — l'ancre de la notification
     * (2026-11-14) : la carte éphémère s'y pose comme un avis de la
     * scène, jamais au-dessus du texte, bornée aux murs.
     *
     * @param popup    la taille du popup
     * @param layerWidth  la largeur de la couche
     * @param layerHeight la hauteur de la couche
     * @param margin   la respiration aux bords, en pixels
     * @return le cadre borné du popup
     */
    public static PopupPlacer.Cadre bottomRightCorner(PopupPlacer.Cadre popup,
            int layerWidth, int layerHeight, int margin) {
        // la taille se borne AUX MURS d'abord, la position suit — la
        // carte trop grande ne dépasse jamais le bas ni la droite
        int largeur = Math.min(popup.largeur(), layerWidth - 2 * margin);
        int hauteur = Math.min(popup.hauteur(), layerHeight - 2 * margin);
        return new PopupPlacer.Cadre(
                Math.max(margin, layerWidth - largeur - margin),
                Math.max(margin, layerHeight - hauteur - margin),
                largeur, hauteur);
    }

    /**
     * Centrée au-dessus de la sélection, sous elle si la place
     * manque — l'ancre de la barre de sélection.
     *
     * @param selLeft  le bord gauche de la sélection
     * @param selRight le bord droit de la sélection
     * @param selTop   le haut de la sélection
     * @param popup    la taille du popup
     * @param layerWidth  la largeur de la couche
     * @param layerHeight la hauteur de la couche
     * @param margin   la respiration aux bords, en pixels
     * @return le cadre borné du popup
     */
    public static PopupPlacer.Cadre centeredAboveSelection(int selLeft, int selRight,
            int selTop, PopupPlacer.Cadre popup, int layerWidth, int layerHeight,
            int margin) {
        int centre = (selLeft + selRight) / 2;
        int x = borneX(centre - popup.largeur() / 2, popup, layerWidth, margin);
        int y = selTop - popup.hauteur() >= margin
                ? selTop - popup.hauteur()
                : Math.min(layerHeight - margin - popup.hauteur(),
                        selTop + margin);
        return new PopupPlacer.Cadre(x, Math.max(margin, y),
                popup.largeur(), popup.hauteur());
    }

    /** L'abscisse bornée : le popup tient dans la couche, respiration comprise. */
    private static int borneX(int x, PopupPlacer.Cadre popup, int layerWidth,
            int margin) {
        return Math.max(margin, Math.min(x, layerWidth - popup.largeur()
                - margin));
    }
}
