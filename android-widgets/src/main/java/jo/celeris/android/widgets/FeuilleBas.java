package jo.celeris.android.widgets;

import android.content.Context;
import android.view.Gravity;

/**
 * La feuille du bas de l'ADR-018 : la surface fonctionnelle dense qui
 * glisse depuis le bas de l'écran — réglages, plan,
 * recherche/remplacement, palette de commandes, toute saisie qui doit
 * tenir le focus. Le voile couvre la scène, la feuille respire dans la
 * largeur de la maquette (660 dp au plus), le corps défile, la
 * fermeture vit au bouton, au voile, au glissement de la poignée et au
 * retour ({@link #surRetour()}).
 *
 * <p>La feuille se pose sur le cadre de contenu de la fenêtre —
 * typiquement {@code android.R.id.content}, toujours un FrameLayout :
 * elle couvre le chrome de l'hôte, l'hôte reprend la main à la
 * fermeture. Maquette de référence :
 * {@code docs/maquettes/popups.html} (2026-11-10).</p>
 */
public final class FeuilleBas extends Feuille {

    /**
     * Construit la feuille du bas sur la palette de l'hôte.
     *
     * @param context le contexte de la fenêtre hôte
     * @param palette la palette dérivée du thème de l'éditeur
     */
    public FeuilleBas(Context context, PopupPalette palette) {
        super(context, palette);
    }

    @Override
    int gravite() {
        return Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
    }

    @Override
    float largeurMaxDp() {
        return PopupTokens.LARGEUR_MAX_FEUILLE_BAS_DP;
    }

    @Override
    float[] rayonsDp() {
        float coin = PopupTokens.RAYON_FEUILLE_DP;
        return new float[]{coin, coin, coin, coin, 0f, 0f, 0f, 0f};
    }

    @Override
    boolean coteBas() {
        return true;
    }
}
