package jo.celeris.android.view;

/**
 * Zoom au pincement (D-15) : la taille de police monte par PALIERS —
 * jamais une valeur libre qui reclasserait le texte à chaque pixel —
 * et le défilement se réajuste pour que le point du document sous le
 * doigt y reste.
 */
public final class PinchZoom {

    /** L'échelle des tailles autorisées, croissante. */
    private static final float[] PALIERS = {10f, 12f, 14f, 16f, 18f, 20f, 24f, 28f};

    private PinchZoom() {
    }

    /**
     * La taille palier la plus proche du produit courant × facteur,
     * bornée à l'échelle.
     *
     * @param currentSize la taille de police courante, en sp
     * @param scaleFactor le facteur d'agrandissement du pincement
     * @return la taille palier retenue
     */
    public static float steppedSize(float currentSize, float scaleFactor) {
        float cible = currentSize * scaleFactor;
        int meilleur = 0;
        for (int i = 1; i < PALIERS.length; i++) {
            if (Math.abs(PALIERS[i] - cible) < Math.abs(PALIERS[meilleur] - cible)) {
                meilleur = i;
            }
        }
        return PALIERS[meilleur];
    }

    /**
     * D-15 — le défilement horizontal qui garde la colonne sous le
     * doigt : la colonne visée avant le zoom se retrouve à la même
     * abscisse après.
     *
     * @param focalX      l'abscisse du point focal, en pixels
     * @param gutterWidth la largeur de gouttière, en pixels
     * @param scrollX     le défilement horizontal d'avant, en pixels
     * @param oldCharWidth la largeur de caractère d'avant, en pixels
     * @param newCharWidth la largeur de caractère d'après, en pixels
     * @return le défilement horizontal d'après, en pixels
     */
    public static float scrollXKeepingColumn(float focalX, float gutterWidth,
            float scrollX, float oldCharWidth, float newCharWidth) {
        float colonne = (focalX - gutterWidth + scrollX) / Math.max(1e-6f, oldCharWidth);
        return Math.max(0f, colonne * newCharWidth - (focalX - gutterWidth));
    }

    /**
     * D-15 — le défilement vertical qui garde la rangée sous le doigt.
     *
     * @param focalY        l'ordonnée du point focal, en pixels
     * @param scrollY       le défilement vertical d'avant, en pixels
     * @param oldLineHeight la hauteur de rangée d'avant, en pixels
     * @param newLineHeight la hauteur de rangée d'après, en pixels
     * @return le défilement vertical d'après, en pixels
     */
    public static float scrollYKeepingRow(float focalY, float scrollY,
            float oldLineHeight, float newLineHeight) {
        float rangee = (focalY + scrollY) / Math.max(1e-6f, oldLineHeight);
        return Math.max(0f, rangee * newLineHeight - focalY);
    }
}
