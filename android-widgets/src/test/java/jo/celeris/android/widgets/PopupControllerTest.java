package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * La machine à états des popups (M7.5, §7) : exclusions, cohabitations
 * et fermetures — pures, avant la première vue. Table état × événement
 * du contrôleur, exhaustive sur les règles énoncées.
 */
class PopupControllerTest {

    /** La complétion et le menu d'intentions s'excluent mutuellement. */
    @Test
    void completionAndIntentionsExcludeEachOther() {
        PopupController controleur = new PopupController();
        assertTrue(controleur.open(PopupController.PopupKind.COMPLETION_LIST));
        assertTrue(controleur.open(PopupController.PopupKind.INTENTIONS_MENU),
                "le menu d'intentions ouvre…");
        assertFalse(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST),
                "…et ferme la complétion");
        assertTrue(controleur.open(PopupController.PopupKind.COMPLETION_LIST));
        assertFalse(controleur.isOpen(PopupController.PopupKind.INTENTIONS_MENU),
                "la complétion, elle aussi, ferme le menu");
    }

    /** L'aide aux paramètres cohabite avec la complétion. */
    @Test
    void signatureHelpCohabitsWithCompletion() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        controleur.open(PopupController.PopupKind.SIGNATURE_HELP);
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST));
        assertTrue(controleur.isOpen(PopupController.PopupKind.SIGNATURE_HELP),
                "l'aide aux paramètres cohabite avec la complétion");
    }

    /** La frappe ferme le survol et l'info-bulle, garde la complétion. */
    @Test
    void keystrokeClosesHoverButKeepsCompletion() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        controleur.open(PopupController.PopupKind.HOVER);
        controleur.open(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP);
        controleur.onKeystroke();
        assertFalse(controleur.isOpen(PopupController.PopupKind.HOVER),
                "le survol se ferme à la frappe");
        assertFalse(controleur.isOpen(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP),
                "l'info-bulle se ferme à la frappe");
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST),
                "la complétion reste ouverte : elle filtre localement");
    }

    /** Le défilement ferme le survol, garde la complétion et le fil. */
    @Test
    void scrollClosesHoverButKeepsTheRest() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        controleur.open(PopupController.PopupKind.BREADCRUMB);
        controleur.open(PopupController.PopupKind.HOVER);
        controleur.onScroll();
        assertFalse(controleur.isOpen(PopupController.PopupKind.HOVER));
        assertTrue(controleur.isOpen(PopupController.PopupKind.BREADCRUMB),
                "le fil d'Ariane suit, il ne se ferme pas");
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST),
                "la complétion se repositionne, elle ne se ferme pas");
    }

    /** closeAll vide la couche — la session se détache. */
    @Test
    void closeAllEmptiesTheLayer() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.SEARCH_BAR);
        controleur.open(PopupController.PopupKind.SIGNATURE_HELP);
        controleur.closeAll();
        assertEquals(Set.of(), controleur.openPopups());
    }
}
