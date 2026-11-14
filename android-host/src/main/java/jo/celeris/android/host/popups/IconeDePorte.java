package jo.celeris.android.host.popups;

import jo.celeris.android.view.ReglagesEditeur;
import jo.celeris.android.widgets.IconeAction;

/**
 * Le pictogramme de chaque porte des réglages (l'audit du propriétaire
 * du 2026-11-14) : l'interrupteur de la carte porte LE glyphe de SA
 * fonctionnalité — jamais la même icône pour toutes. Pur, sans
 * appareil : la correspondance se lit sans monter la scène.
 */
final class IconeDePorte {

    private IconeDePorte() {
    }

    /** Le pictogramme de la porte donnée. */
    static IconeAction de(ReglagesEditeur.Porte porte) {
        return switch (porte) {
            case INDICES_EN_LIGNE -> IconeAction.TYPE_INFO;
            case BARRE_SELECTION -> IconeAction.COUPER;
            case AMPOULE_INTENTIONS -> IconeAction.INTENTIONS;
            case COMPLETION_AUTO -> IconeAction.COMPLETER;
            case VALEURS_PAUSE -> IconeAction.APERCU;
            case FANTOME_IA -> IconeAction.IA_FANTOME;
            case MINIMAP -> IconeAction.HIERARCHIE;
            case DEFILEMENT_RAPIDE -> IconeAction.ALLER_A;
            case NUMEROS_LIGNES -> IconeAction.TOUT;
            case RETOUR_LIGNE -> IconeAction.RETOUR_LIGNE;
        };
    }
}
