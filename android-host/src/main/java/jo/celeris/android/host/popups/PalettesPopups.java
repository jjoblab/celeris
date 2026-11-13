package jo.celeris.android.host.popups;

import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.core.syntax.TokenKind;

/**
 * Les palettes des popups de l'éditeur : le kit dérive son verre du
 * thème de l'éditeur — le fond devient le verre, la sélection et le
 * curseur teintent l'accent, le mot-clé signe la surbrillance. La
 * dérivation vit chez l'hôte de l'éditeur : l'application ne calcule
 * rien, elle tourne le thème.
 */
public final class PalettesPopups {

    private PalettesPopups() {
    }

    /**
     * La palette dérivée du thème d'éditeur courant — le calcul du
     * verre du dépôt de départ.
     *
     * @param theme         le thème d'éditeur courant
     * @param tailleTexteSp la taille de texte de l'éditeur, en sp
     * @return la palette des popups de l'éditeur
     */
    public static PopupPalette de(EditorTheme theme, float tailleTexteSp) {
        return PopupPalette.verre(theme.backgroundColor(),
                theme.baseTextColor(), theme.gutterTextColor(),
                theme.selectionColor(), theme.cursorColor(),
                theme.syntaxe().couleur(TokenKind.KEYWORD), tailleTexteSp);
    }
}
