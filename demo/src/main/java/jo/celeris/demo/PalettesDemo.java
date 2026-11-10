package jo.celeris.demo;

import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;

import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.core.syntax.TokenKind;

/**
 * Les palettes des feuilles de la démonstration (ADR-018) : le kit des
 * popups dérive son verre du thème de l'éditeur — les feuilles suivent
 * la même voie quand elles vivent au-dessus de l'éditeur, et l'accueil,
 * qui n'a pas d'éditeur, suit le mode nuit du système.
 */
final class PalettesDemo {

    private PalettesDemo() {
    }

    /**
     * La palette dérivée du thème d'éditeur courant — le calcul du
     * verre du dépôt de départ.
     *
     * @param theme         le thème d'éditeur courant
     * @param tailleTexteSp la taille de texte de l'éditeur, en sp
     * @return la palette des feuilles de l'éditeur
     */
    static PopupPalette de(EditorTheme theme, float tailleTexteSp) {
        return PopupPalette.verre(theme.backgroundColor(),
                theme.baseTextColor(), theme.gutterTextColor(),
                theme.selectionColor(), theme.cursorColor(),
                theme.syntaxe().couleur(TokenKind.KEYWORD), tailleTexteSp);
    }

    /**
     * La palette de l'accueil — le mode nuit du système décide du
     * verre, clair le jour, sombre la nuit.
     *
     * @param activite l'activité d'accueil
     * @return la palette des feuilles de l'accueil
     */
    static PopupPalette de(AppCompatActivity activite) {
        int nuit = activite.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nuit == Configuration.UI_MODE_NIGHT_YES
                ? PopupPalette.dark() : PopupPalette.light();
    }
}
