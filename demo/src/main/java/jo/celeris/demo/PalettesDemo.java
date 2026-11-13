package jo.celeris.demo;

import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;

import jo.celeris.android.widgets.PopupPalette;

/**
 * Les palettes de l'accueil de la démonstration : le mode nuit du
 * système décide du verre, clair le jour, sombre la nuit — la
 * dérivation du thème d'éditeur vit désormais chez l'éditeur
 * ({@code PalettesPopups}, le déménagement du 2026-11-14).
 */
final class PalettesDemo {

    private PalettesDemo() {
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
