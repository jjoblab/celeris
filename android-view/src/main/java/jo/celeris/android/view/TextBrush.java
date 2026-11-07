package jo.celeris.android.view;

/**
 * Pinceau de texte : la couleur et la police courantes d'un tracé.
 *
 * <p>Le pinceau est mutable par nature (PIEGE-01) : chaque couche possède
 * le sien et repose explicitement sa couleur de base à chaque image — le
 * pipeline ne partage jamais un pinceau sans défense. La police suit la
 * même discipline (PIEGE-21) : reposée à chaque image depuis les
 * métriques, un pinceau fraîchement créé ne trace jamais à la police
 * par défaut de la plateforme — 12 pixels à chasse variable — pendant
 * que les positions se calculent à la police de l'éditeur.</p>
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

    /**
     * Pose la police du tracé — famille et taille en pixels.
     *
     * <p>Le repli par défaut n'opère rien : les pinceaux d'essai
     * restent muets ; le pinceau concret de la plateforme en dérive
     * sa police et sa taille. Toute couche qui trace du texte doit
     * recevoir la police de l'image avant de tracer.</p>
     *
     * @param police la spécification de police, en pixels
     */
    default void setFont(FontSpec police) {
        // repli muet — voir la documentation
    }
}
