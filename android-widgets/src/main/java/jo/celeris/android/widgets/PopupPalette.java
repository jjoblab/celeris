package jo.celeris.android.widgets;

/**
 * La palette des popups (M7.5, §7, reprise) : les encres et fonds des
 * panneaux portés par la couche — l'hôte la dérive de son thème, la
 * couche ne connaît aucun thème d'éditeur. Les deux palettes de
 * référence (sombre, claire) servent la démonstration.
 *
 * <p>Le fond est un VERRE TRANSLUCIDE (M7.5, redressement — les popups
 * du dépôt de départ laissaient transparaître le code dessous) : le
 * code reste lisible en transparence, le liseré et les encres
 * pleinement opaques portent le contraste. La barre de sélection
 * partage le même panneau, donc le même verre.</p>
 *
 * @param background le fond du panneau, translucide
 * @param border     le liseré du panneau, opaque
 * @param text       l'encre du texte, opaque
 * @param accent     l'encre d'accent (sélection courante, soulignement)
 * @param error      l'encre d'erreur (diagnostics, bande)
 */
public record PopupPalette(int background, int border, int text, int accent,
        int error) {

    /** La palette sombre de référence — la démonstration sombre, verre
     *  fumé à quatre-vingt-cinq pour cent. */
    public static PopupPalette dark() {
        return new PopupPalette(0xD92E2E32, 0xFF52525A, 0xFFE6E6EC,
                0xFF7EC8F7, 0xFFF26D78);
    }

    /** La palette claire de référence — la démonstration claire, verre
     *  dépoli à quatre-vingt-dix pour cent. */
    public static PopupPalette light() {
        return new PopupPalette(0xE6F7F7FA, 0xFFB9B9C4, 0xFF25252B,
                0xFF1A6FC4, 0xFFC1352F);
    }
}
