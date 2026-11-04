package jo.celeris.android.view;

/**
 * Rapport de contraste WCAG 2.x entre deux couleurs ARGB.
 *
 * <p>Luminance relative par canal linéarisé, puis rapport
 * (L1 + 0,05) / (L2 + 0,05) — le critère D-13 (« contraste vérifié
 * automatiquement ») s'y appuie : texte ≥ 4,5:1, éléments non textuels
 * ≥ 3:1.</p>
 */
public final class WcagContrast {

    private static final double SEUIL_LINEAIRE = 0.03928;
    private static final double DIVISEUR_LINEAIRE = 12.92;
    private static final double PENTE = 1.055;
    private static final double EXPOSANT = 2.4;
    private static final double MARGE = 0.05;

    private WcagContrast() {
    }

    /**
     * Le rapport de contraste entre deux couleurs opaques.
     *
     * @param premier  la première couleur, au format ARGB
     * @param second   la seconde couleur, au format ARGB
     * @return le rapport de contraste, ≥ 1 (1 : identiques, 21 : noir sur blanc)
     */
    public static double ratio(int premier, int second) {
        double l1 = luminance(premier);
        double l2 = luminance(second);
        double clair = Math.max(l1, l2);
        double fonce = Math.min(l1, l2);
        return (clair + MARGE) / (fonce + MARGE);
    }

    private static double luminance(int argb) {
        double rouge = canal((argb >> 16) & 0xFF);
        double vert = canal((argb >> 8) & 0xFF);
        double bleu = canal(argb & 0xFF);
        return 0.2126 * rouge + 0.7152 * vert + 0.0722 * bleu;
    }

    private static double canal(int composante) {
        double c = composante / 255.0;
        if (c <= SEUIL_LINEAIRE) {
            return c / DIVISEUR_LINEAIRE;
        }
        return Math.pow((c + PENTE - 1.0) / PENTE, EXPOSANT);
    }
}
