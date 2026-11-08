package jo.celeris.android.widgets;

/**
 * La palette des popups (portage du verre du dépôt de départ) : les
 * encres et fonds des panneaux portés par la couche — l'hôte la dérive
 * de son thème par {@link #verre}, la couche ne connaît aucun thème
 * d'éditeur.
 *
 * <p>Le fond est le VERRE du dépôt de départ : le fond de l'éditeur à
 * alpha 0xDB (thème sombre) ou 0xE0 (thème clair) — le code reste
 * lisible en transparence — et le liseré un trait hairline blanc à
 * dix pour cent (sombre) ou noir à huit pour cent (clair), pleinement
 * porteur du contour. Les encres sémantiques suivent : l'atténué (le
 * texte de gouttière), la teinte de sélection (les bandes douces),
 * l'accent (les runs de match, le paramètre actif), le mot-clé (les
 * libellés de candidats) et les trois gravités de diagnostic. La barre
 * de sélection partage le même panneau, donc le même verre.</p>
 *
 * @param background  le fond du panneau, verre translucide
 * @param border      le liseré du panneau, translucide
 * @param text        l'encre du texte, opaque
 * @param muted       l'encre atténuée (détails, à venir), opaque
 * @param selection   la teinte des bandes douces (sélection courante)
 * @param accent      l'encre d'accent (runs de match, paramètre actif)
 * @param keyword     l'encre des libellés (candidats de complétion)
 * @param error       l'encre d'erreur (diagnostics, bande)
 * @param warning     l'encre d'avertissement (diagnostics, bande)
 * @param info        l'encre d'information (diagnostics, bande)
 * @param textBaseSp  la taille de texte de base des popups, en sp —
 *                    l'échelle typographique de {@link PopupTokens}
 *                    s'y multiplie
 */
public record PopupPalette(int background, int border, int text, int muted,
        int selection, int accent, int keyword, int error, int warning,
        int info, float textBaseSp) {

    /** La taille de texte de base de référence — celle de l'éditeur. */
    private static final float BASE_TEXTE_SP = 14f;

    /**
     * La palette dérivée du thème de l'éditeur hôte — le calcul du
     * verre du dépôt de départ : fond de l'éditeur à alpha 0xDB sur un
     * thème sombre, 0xE0 sur un thème clair ; liseré blanc à dix pour
     * cent (sombre) ou noir à huit pour cent (clair). Les gravités
     * suivent la luminance du fond (les valeurs de référence du dépôt
     * de départ).
     *
     * @param fondEditeur    le fond de la zone de texte de l'éditeur
     * @param encreTexte     l'encre de base du texte de l'éditeur
     * @param encreGouttiere l'encre atténuée (texte de gouttière)
     * @param teinteSelection la teinte de sélection de l'éditeur
     * @param encreAccent    l'encre d'accent (le curseur de l'éditeur)
     * @param encreMotCle    l'encre des mots réservés de l'éditeur
     * @param baseTexteSp    la taille de texte de l'éditeur, en sp
     * @return la palette des popups de l'hôte
     */
    public static PopupPalette verre(int fondEditeur, int encreTexte,
            int encreGouttiere, int teinteSelection, int encreAccent,
            int encreMotCle, float baseTexteSp) {
        boolean sombre = (fondEditeur & 0xFFFFFF) < 0x808080;
        int fond = (fondEditeur & 0x00FFFFFF)
                | (sombre ? 0xDB000000 : 0xE0000000);
        int lisere = sombre ? 0x1AFFFFFF : 0x14000000;
        int erreur = sombre ? 0xFFF44747 : 0xFFFF0000;
        int avertissement = sombre ? 0xFFCCA700 : 0xFFCC7800;
        int info = 0xFF0078D4;
        return new PopupPalette(fond, lisere, encreTexte, encreGouttiere,
                teinteSelection, encreAccent, encreMotCle, erreur,
                avertissement, info, baseTexteSp);
    }

    /**
     * La palette sombre de référence — le verre du dépôt de départ sur
     * son thème sombre : fond à alpha 0xDB, liseré blanc à dix pour
     * cent.
     *
     * @return la palette sombre de référence
     */
    public static PopupPalette dark() {
        return verre(0xFF1E1E1E, 0xFFD4D4D4, 0xFF858585, 0xFF264F78,
                0xFFDCDCAA, 0xFF569CD6, BASE_TEXTE_SP);
    }

    /**
     * La palette claire de référence — le verre du dépôt de départ sur
     * son thème clair : fond à alpha 0xE0, liseré noir à huit pour
     * cent.
     *
     * @return la palette claire de référence
     */
    public static PopupPalette light() {
        return verre(0xFFFFFFFF, 0xFF000000, 0xFF808080, 0xFFADD6FF,
                0xFF795E26, 0xFF0000FF, BASE_TEXTE_SP);
    }
}
