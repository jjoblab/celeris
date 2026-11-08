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
 * pas ; la barre de recherche ne se ferme que sur demande. Les dix
 * popups (M7.5, reprise) : la documentation de complétion n'existe
 * pas sans sa liste — fermer la liste ferme la documentation ; la
 * barre de sélection cède à la frappe (la frappe remplace la
 * sélection) mais survit au défilement, et son MENU DE DÉBORDEMENT
 * ne survit jamais à sa barre — la barre fermée l'emporte ; la bande
 * d'erreurs ne cède ni à la frappe ni au défilement — elle vit avec
 * les diagnostics, l'hôte l'ouvre et la ferme.</p>
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

        /** La documentation de complétion — n'existe pas sans sa
         *  liste, fermée avec elle. */
        COMPLETION_DOC,

        /** Le survol de symbole — fermé à la frappe et au défilement. */
        HOVER,

        /** L'info-bulle de diagnostic — fermée à la frappe et au défilement. */
        DIAGNOSTIC_TOOLTIP,

        /** Le fil d'Ariane — suit le curseur. */
        BREADCRUMB,

        /** La barre de recherche — fermée sur demande seule. */
        SEARCH_BAR,

        /** La barre de sélection — cède à la frappe (la frappe remplace
         *  la sélection), survit au défilement. */
        SELECTION_BAR,

        /** Le menu de débordement de la barre de sélection — n'existe
         *  pas sans sa barre, fermé avec elle ; il vit les mêmes
         *  régimes qu'elle (cède à la frappe, survit au défilement en
         *  se repositionnant). */
        SELECTION_OVERFLOW,

        /** La bande d'erreurs — vit avec les diagnostics : ni la frappe
         *  ni le défilement ne la ferment. */
        ERROR_BAND
    }

    private final Set<PopupKind> ouverts = EnumSet.noneOf(PopupKind.class);

    /**
     * Ouvre le popup donné : les exclusions s'appliquent — la
     * complétion et le menu d'intentions ne tiennent jamais ensemble.
     * La documentation de complétion n'ouvre que sur une liste déjà
     * ouverte — elle n'existe pas sans sa liste ; le menu de
     * débordement n'ouvre que sur une barre de sélection déjà ouverte.
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
        } else if (kind == PopupKind.COMPLETION_DOC
                && !ouverts.contains(PopupKind.COMPLETION_LIST)) {
            return false;
        } else if (kind == PopupKind.SELECTION_OVERFLOW
                && !ouverts.contains(PopupKind.SELECTION_BAR)) {
            return false;
        }
        return ouverts.add(kind);
    }

    /**
     * Ferme le popup donné. Fermer la liste de complétion ferme sa
     * documentation — la documentation ne reste jamais seule ; fermer
     * la barre de sélection ferme son menu de débordement — le menu
     * ne survit jamais à sa barre.
     *
     * @param kind le popup à fermer
     * @return vrai si l'état a changé
     */
    public boolean close(PopupKind kind) {
        boolean change = ouverts.remove(kind);
        if (kind == PopupKind.COMPLETION_LIST) {
            change |= ouverts.remove(PopupKind.COMPLETION_DOC);
        } else if (kind == PopupKind.SELECTION_BAR) {
            change |= ouverts.remove(PopupKind.SELECTION_OVERFLOW);
        }
        return change;
    }

    /** Ferme tout — la session se détache, la couche se vide. */
    public void closeAll() {
        ouverts.clear();
    }

    /**
     * La frappe : le survol, l'info-bulle et la barre de sélection se
     * ferment (la frappe remplace la sélection — son menu de
     * débordement suit), la complétion RESTE
     * — elle filtre localement, la requête ne repart pas ; la bande
     * d'erreurs survit — elle vit avec les diagnostics.
     */
    public void onKeystroke() {
        ouverts.remove(PopupKind.HOVER);
        ouverts.remove(PopupKind.DIAGNOSTIC_TOOLTIP);
        ouverts.remove(PopupKind.SELECTION_BAR);
        ouverts.remove(PopupKind.SELECTION_OVERFLOW);
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
