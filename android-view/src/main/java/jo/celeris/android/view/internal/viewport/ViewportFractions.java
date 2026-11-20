package jo.celeris.android.view.internal.viewport;

import jo.celeris.android.view.ScrollGeometry;
import jo.celeris.android.view.internal.access.EditorAccess;


/**
 * Les fractions du défilement (chantier C9) : le collaborateur de la
 * vue qui lit et pose le défilement vertical en FRACTION — 0 en tête
 * du document, 1 au pied. La minimap et le pouce du bord droit s'y
 * abreuvent : la fraction se lit, la part visible suit la fenêtre, le
 * défilement à une fraction se borne à l'image suivante — l'avis de
 * fenêtre y part (M7.5, §7).
 *
 * <p>Pur sur l'état de la vue (projection, métriques, défilement) :
 * aucune horloge, aucun appareil — le banc de vue le couvre.</p>
 */
public final class ViewportFractions  {

    private final EditorAccess acces;

    public ViewportFractions(EditorAccess acces) {
        this.acces = acces;
    }

    /**
     * La fraction verticale du défilement : 0 en tête du document, 1 au
     * pied — 0 quand tout tient, sans course.
     */
    public float fraction() {
        float max = course();
        return max <= 0f ? 0f
                : Math.max(0f, Math.min(1f, acces.scrollY() / max));
    }

    /**
     * La part visible du contenu vertical : 1 quand tout tient dans la
     * fenêtre.
     */
    public float partVisible() {
        float contentHeight = contentHeight();
        return contentHeight <= 0f || acces.getHeight() <= 0f ? 1f
                : Math.max(0f, Math.min(1f, acces.getHeight() / contentHeight));
    }

    /**
     * Défile à la fraction verticale : 0 en tête, 1 au pied — le
     * défilement se borne à l'image suivante, l'avis de fenêtre y part.
     */
    public void defilerA(float fraction) {
        float max = course();
        float target = max <= 0f ? 0f
                : Math.max(0f, Math.min(1f, fraction)) * max;
        if (target != acces.scrollY()) {
            acces.setScrollY(target);
            acces.invalidate();
        }
    }

    /**
     * La première ligne visible : la rangée du haut de la fenêtre, dans
     * les bornes du document.
     */
    public int firstVisibleLine() {
        return Math.max(0, ScrollGeometry.firstRow(acces.scrollY(),
                acces.metrics().lineHeight()));
    }

    /**
     * Le nombre de lignes visibles : la hauteur de la fenêtre en
     * rangées, au moins une.
     */
    public int visibleLines() {
        return Math.max(1, ScrollGeometry.visibleRows(acces.getHeight(),
                acces.metrics().lineHeight()));
    }

    /** La hauteur du contenu visuel — rangées × hauteur de rangée. */
    private float contentHeight() {
        return acces.projection().rowCount() * acces.metrics().lineHeight();
    }

    /** La course verticale du défilement — le maximum déroulable. */
    private float course() {
        return ScrollGeometry.maxScroll(contentHeight(), acces.getHeight());
    }
}
