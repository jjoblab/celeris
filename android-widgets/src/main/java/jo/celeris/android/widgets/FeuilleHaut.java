package jo.celeris.android.widgets;

import android.content.Context;
import android.view.Gravity;

/**
 * La feuille du haut de l'ADR-018 : le rappel contextuel qui glisse
 * depuis le haut de l'écran — revues par blocs, barres d'outils
 * contextuelles, consentements. Même chrome que la feuille du bas
 * (poignée, titre, fermeture, voile, retour), la largeur respire à
 * 760 dp au plus, le glissement qui l'emporte monte — la maquette la
 * porte sous la barre d'état, l'encart s'y cale.
 *
 * <p>La feuille se pose sur le cadre de contenu de la fenêtre —
 * typiquement {@code android.R.id.content}, toujours un FrameLayout.
 * Maquette de référence : {@code docs/maquettes/popups.html}
 * (2026-11-10).</p>
 */
public final class FeuilleHaut extends Feuille {

    /**
     * Construit la feuille du haut sur la palette de l'hôte.
     *
     * @param context le contexte de la fenêtre hôte
     * @param palette la palette dérivée du thème de l'éditeur
     */
    public FeuilleHaut(Context context, PopupPalette palette) {
        super(context, palette);
    }

    @Override
    int gravite() {
        return Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    }

    @Override
    float largeurMaxDp() {
        return PopupTokens.LARGEUR_MAX_FEUILLE_HAUT_DP;
    }

    @Override
    float[] rayonsDp() {
        float coin = PopupTokens.RAYON_FEUILLE_DP;
        return new float[]{0f, 0f, 0f, 0f, coin, coin, coin, coin};
    }

    @Override
    boolean coteBas() {
        return false;
    }
}
