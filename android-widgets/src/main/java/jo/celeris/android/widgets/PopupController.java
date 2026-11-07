package jo.celeris.android.widgets;

import java.util.EnumSet;
import java.util.Set;

/**
 * La machine à états des popups de l'éditeur (M7.5, §7) : qui est
 * ouvert, qui s'exclut, qui cohabite — les règles de la couche, pures
 * et testées, avant la première vue.
 *
 * <p>Règles : la liste de complétion et le menu d'intentions
 * s'excluent mutuellement ; l'aide aux paramètres cohabite avec la
 * complétion (au-dessus de la ligne) ; le survol et l'info-bulle se
 * ferment à la frappe et au défilement ; la complétion FILTRE
 * localement à la frappe — elle reste ouverte, la requête ne repart
 * pas ; la barre de recherche ne se ferme que sur demande.</p>
 *
 * <p>PIEGE-27 par construction : le contrôleur ne connaît AUCUNE vue —
 * le focus d'entrée reste à l'éditeur, les vues ne font qu'obéir aux
 * états.</p>
 */
public final class PopupController {

    /** Les popups de l'éditeur, chacun son régime d'exclusion. */
    public enum PopupKind {

        /** La liste de complétion — exclue le menu d'intentions. */
        COMPLETION_LIST,

        /** Le menu d'intentions — exclut la liste de complétion. */
        INTENTIONS_MENU,

        /** L'aide aux paramètres — cohabite avec la complétion. */
        SIGNATURE_HELP,

        /** Le survol de symbole — fermé à la frappe et au défilement. */
        HOVER,

        /** L'info-bulle de diagnostic — fermée à la frappe et au défilement. */
        DIAGNOSTIC_TOOLTIP,

        /** Le fil d'Ariane — suit le curseur. */
        BREADCRUMB,

        /** La barre de recherche — fermée sur demande seule. */
        SEARCH_BAR
    }

    private final Set<PopupKind> ouverts = EnumSet.noneOf(PopupKind.class);

    /**
     * Ouvre le popup donné : les exclusions s'appliquent — la
     * complétion et le menu d'intentions ne tiennent jamais ensemble.
     *
     * @param kind le popup à ouvrir
     * @return vrai si l'état a changé
     */
    public boolean open(PopupKind kind) {
        if (ouverts.contains(kind)) {
            return false;
        }
        if (kind == PopupKind.COMPLETION_LIST) {
            ouverts.remove(PopupKind.INTENTIONS_MENU);
        } else if (kind == PopupKind.INTENTIONS_MENU) {
            ouverts.remove(PopupKind.COMPLETION_LIST);
        }
        return ouverts.add(kind);
    }

    /**
     * Ferme le popup donné.
     *
     * @param kind le popup à fermer
     * @return vrai si l'état a changé
     */
    public boolean close(PopupKind kind) {
        return ouverts.remove(kind);
    }

    /** Ferme tout — la session se détache, la couche se vide. */
    public void closeAll() {
        ouverts.clear();
    }

    /**
     * La frappe : le survol et l'info-bulle se ferment, la complétion
     * RESTE — elle filtre localement, la requête ne repart pas.
     */
    public void onKeystroke() {
        ouverts.remove(PopupKind.HOVER);
        ouverts.remove(PopupKind.DIAGNOSTIC_TOOLTIP);
    }

    /**
     * Le défilement : le survol et l'info-bulle se ferment — les
     * popups qui restent se REPOSITIONNENT, ils ne se ferment pas.
     */
    public void onScroll() {
        ouverts.remove(PopupKind.HOVER);
        ouverts.remove(PopupKind.DIAGNOSTIC_TOOLTIP);
    }

    /** Vrai si le popup donné est ouvert. */
    public boolean isOpen(PopupKind kind) {
        return ouverts.contains(kind);
    }

    /** Les popups ouverts — copie défensive, l'état reste au contrôleur. */
    public Set<PopupKind> openPopups() {
        return EnumSet.copyOf(ouverts);
    }
}
