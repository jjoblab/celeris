package jo.celeris.android.view;

/**
 * Les gestes du stylet, de la souris et du pavé tactile (M5, D-18) :
 * la molette zoome avec Ctrl — facteur multiplicatif de 1,1 par cran,
 * borné comme le pincement (0,5 à 3,0, D-15) — l'info-bulle du survol
 * paraît après un temps de station sur place, le déplacement la
 * retire. Pure logique de geste, sans SDK : la vue l'appelle.
 */
public final class PointerGestures {

    /** Le facteur multiplicatif d'un cran de molette. */
    public static final float FACTEUR_CRAN = 1.1f;

    /** Le zoom minimal, hérité du pincement (D-15). */
    public static final float ZOOM_MIN = 0.5f;

    /** Le zoom maximal, hérité du pincement (D-15). */
    public static final float ZOOM_MAX = 3.0f;

    private PointerGestures() {
    }

    /**
     * Le zoom après un geste de molette : avec Ctrl, chaque cran
     * multiplie (vers le haut) ou divise (vers le bas) — borné ; sans
     * Ctrl, la molette défile et le zoom tient.
     *
     * @param zoom le zoom courant
     * @param crans les crans de molette (positifs vers le haut)
     * @param ctrl vrai si Ctrl est tenu
     * @return le zoom résultant
     */
    public static float zoomApresMolette(float zoom, int crans, boolean ctrl) {
        if (!ctrl || crans == 0) {
            return borne(zoom);
        }
        float facteur = (float) Math.pow(FACTEUR_CRAN, crans);
        return borne(zoom * facteur);
    }

    /**
     * L'info-bulle du survol : elle paraît après le délai de station
     * sur la même colonne ; le déplacement retarde — la station
     * repart, l'appelant réarme.
     *
     * @param maintenant        l'instant présent
     * @param debutStation      le début de la station courante
     * @param colonnePrecedente la colonne du dernier appel
     * @param colonne           la colonne courante
     * @param delaiMs           le délai de station
     * @return vrai si la bulle doit paraître
     */
    public static boolean bulleVisible(long maintenant, long debutStation,
            int colonnePrecedente, int colonne, long delaiMs) {
        if (colonne != colonnePrecedente) {
            return false;
        }
        return maintenant - debutStation >= delaiMs;
    }

    private static float borne(float zoom) {
        return Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom));
    }
}
