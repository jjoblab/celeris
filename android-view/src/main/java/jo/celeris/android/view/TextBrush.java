package jo.celeris.android.view;

/**
 * Pinceau de texte : la couleur courante d'un tracé.
 *
 * <p>Le pinceau est mutable par nature (PIEGE-01) : chaque couche possède
 * le sien et repose explicitement sa couleur de base à chaque image — le
 * pipeline ne partage jamais un pinceau sans défense.</p>
 */
public interface TextBrush {

    /**
     * Pose la couleur du pinceau.
     *
     * @param argb la couleur au format ARGB opaque
     */
    void setColor(int argb);

    /** La couleur courante du pinceau, au format ARGB. */
    int color();
}
