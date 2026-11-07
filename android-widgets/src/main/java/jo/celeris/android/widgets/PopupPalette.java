package jo.celeris.android.widgets;

/**
 * La palette des popups (M7.5, §7, reprise) : les encres et fonds des
 * panneaux portés par la couche — l'hôte la dérive de son thème, la
 * couche ne connaît aucun thème d'éditeur. Les deux palettes de
 * référence (sombre, claire) servent la démonstration.
 *
 * @param background le fond du panneau
 * @param border     le liseré du panneau
 * @param text       l'encre du texte
 * @param accent     l'encre d'accent (sélection courante, soulignement)
 * @param error      l'encre d'erreur (diagnostics, bande)
 */
public record PopupPalette(int background, int border, int text, int accent,
        int error) {

    /** La palette sombre de référence — la démonstration sombre. */
    public static PopupPalette dark() {
        return new PopupPalette(0xF22E2E32, 0xFF52525A, 0xFFE6E6EC,
                0xFF7EC8F7, 0xFFF26D78);
    }

    /** La palette claire de référence — la démonstration claire. */
    public static PopupPalette light() {
        return new PopupPalette(0xFFF7F7FA, 0xFFB9B9C4, 0xFF25252B,
                0xFF1A6FC4, 0xFFC1352F);
    }
}
