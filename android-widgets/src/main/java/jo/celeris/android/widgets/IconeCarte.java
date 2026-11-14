package jo.celeris.android.widgets;

/**
 * Une icône de rangée — le petit glyphe vectoriel qui précède le texte
 * (l'audit Android Studio du 2026-11-14 : les menus de l'IDE de
 * référence portent leurs icônes, l'ampoule des intentions la première
 * — le propriétaire : « les icônes, je ne me souviens pas avoir vu
 * d'icône dans les nôtres »). Le tracé vit au patron
 * {@link TextRowsPopup}, l'encre se résout ici par la palette du
 * thème : chaque icône porte sa couleur sémantique.
 */
public enum IconeCarte {

    /** L'ampoule des intentions — le jaune de l'IDE de référence
     *  (l'encre d'avertissement du thème), le glyphe du menu
     *  Alt+Enter : chaque intention la porte. */
    AMP;

    /** L'encre de l'icône — chaque glyphe porte sa sémantique.
     *
     * @param palette la palette courante des popups
     * @return l'entier ARGB de l'encre
     */
    int encre(PopupPalette palette) {
        return palette.warning();
    }
}
