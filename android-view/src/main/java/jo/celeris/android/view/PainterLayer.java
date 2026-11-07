package jo.celeris.android.view;

/**
 * Couche du pipeline de rendu (ADR-006 : texte, chrome, popups — un
 * tracé par image, couche par couche).
 *
 * <p>PIEGE-01 : chaque couche possède son pinceau (pris à sa fabrique à
 * sa construction) et repose explicitement sa couleur de base à CHAQUE
 * image, avant tout tracé — même sous la pire fabrique, celle qui
 * partagerait un pinceau mutable entre toutes les couches, aucune
 * couleur résiduelle ne se propage.</p>
 */
public abstract class PainterLayer {

    private final TextBrush pinceau;

    /**
     * Construit la couche : elle prend son pinceau à la fabrique et le
     * garde pour toutes les images.
     *
     * @param brushes la fabrique de pinceaux de l'hôte
     */
    protected PainterLayer(BrushFactory brushes) {
        this.pinceau = brushes.newBrush();
    }

    /** Le pinceau possédé par la couche. */
    protected final TextBrush brush() {
        return pinceau;
    }

    /**
     * Trace la couche pour l'image donnée : la couleur de base et la
     * police se reposent d'abord, le tracé concret suit — un pinceau
     * fraîchement créé ne trace jamais à la police par défaut de la
     * plateforme (PIEGE-22, même discipline que la couleur).
     *
     * @param surface la surface de dessin de l'image
     * @param frame   l'état d'image courant
     */
    public final void paint(DrawSurface surface, FrameState frame) {
        pinceau.setColor(baseColor(frame));
        pinceau.setFont(frame.metrics().font());
        onPaint(surface, frame);
    }

    /**
     * La couleur de base de la couche, résolue du thème de l'image.
     *
     * @param frame l'état d'image courant
     * @return la couleur de base, en ARGB
     */
    protected abstract int baseColor(FrameState frame);

    /**
     * Le tracé concret de la couche — couleur de base déjà posée.
     *
     * @param surface la surface de dessin de l'image
     * @param frame   l'état d'image courant
     */
    protected abstract void onPaint(DrawSurface surface, FrameState frame);
}
