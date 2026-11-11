package jo.celeris.android.view;

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
final class ViewportFractions {

    private final EditorView vue;

    ViewportFractions(EditorView vue) {
        this.vue = vue;
    }

    /**
     * La fraction verticale du défilement : 0 en tête du document, 1 au
     * pied — 0 quand tout tient, sans course.
     */
    float fraction() {
        float max = course();
        return max <= 0f ? 0f
                : Math.max(0f, Math.min(1f, vue.scrollY / max));
    }

    /**
     * La part visible du contenu vertical : 1 quand tout tient dans la
     * fenêtre.
     */
    float partVisible() {
        float hauteurContenu = hauteurContenu();
        return hauteurContenu <= 0f || vue.getHeight() <= 0f ? 1f
                : Math.max(0f, Math.min(1f, vue.getHeight() / hauteurContenu));
    }

    /**
     * Défile à la fraction verticale : 0 en tête, 1 au pied — le
     * défilement se borne à l'image suivante, l'avis de fenêtre y part.
     */
    void defilerA(float fraction) {
        float max = course();
        float cible = max <= 0f ? 0f
                : Math.max(0f, Math.min(1f, fraction)) * max;
        if (cible != vue.scrollY) {
            vue.scrollY = cible;
            vue.invalidate();
        }
    }

    /**
     * La première ligne visible : la rangée du haut de la fenêtre, dans
     * les bornes du document.
     */
    int premiereLigneVisible() {
        return Math.max(0, ScrollGeometry.firstRow(vue.scrollY,
                vue.metrics.lineHeight()));
    }

    /**
     * Le nombre de lignes visibles : la hauteur de la fenêtre en
     * rangées, au moins une.
     */
    int lignesVisibles() {
        return Math.max(1, ScrollGeometry.visibleRows(vue.getHeight(),
                vue.metrics.lineHeight()));
    }

    /** La hauteur du contenu visuel — rangées × hauteur de rangée. */
    private float hauteurContenu() {
        return vue.projection.rowCount() * vue.metrics.lineHeight();
    }

    /** La course verticale du défilement — le maximum déroulable. */
    private float course() {
        return ScrollGeometry.maxScroll(hauteurContenu(), vue.getHeight());
    }
}
