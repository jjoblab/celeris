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

    /** La frappe rend les genres fermés — la couche les retire,
     *  la vue ne survit jamais à son état (jamais de zombie). */
    @Test
    void keystrokeReturnsTheClosedGenres() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.HOVER);
        controleur.open(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP);
        controleur.open(PopupController.PopupKind.SELECTION_BAR);
        controleur.open(PopupController.PopupKind.SELECTION_OVERFLOW);
        controleur.open(PopupController.PopupKind.COLOR_PICKER);
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.HOVER,
                        PopupController.PopupKind.DIAGNOSTIC_TOOLTIP,
                        PopupController.PopupKind.SELECTION_BAR,
                        PopupController.PopupKind.SELECTION_OVERFLOW,
                        PopupController.PopupKind.COLOR_PICKER),
                controleur.onKeystroke());
        assertEquals(java.util.EnumSet.noneOf(PopupController.PopupKind.class),
                controleur.onKeystroke(), "la fermeture est idempotente");
    }

    /** Le défilement rend les genres fermés — le retrait de la couche
     *  suit, jamais la vue ne survit à son état. */
    @Test
    void scrollReturnsTheClosedGenres() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.HOVER);
        controleur.open(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP);
        controleur.open(PopupController.PopupKind.COLOR_PICKER);
        controleur.open(PopupController.PopupKind.MINIMAP);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.HOVER,
                        PopupController.PopupKind.DIAGNOSTIC_TOOLTIP,
                        PopupController.PopupKind.COLOR_PICKER),
                controleur.onScroll());
        assertTrue(controleur.isOpen(PopupController.PopupKind.MINIMAP),
                "l'ornement ne cède pas au défilement");
    }

    /** closeAll rend TOUT ce qui vivait — le retrait de la couche
     *  suit, même le vide se rend honnêtement. */
    @Test
    void closeAllReturnsEveryOpenGenre() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.SELECTION_BAR);
        controleur.open(PopupController.PopupKind.BREADCRUMB);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.SELECTION_BAR,
                        PopupController.PopupKind.BREADCRUMB),
                controleur.closeAll());
        assertEquals(Set.of(), controleur.openPopups());
        assertEquals(java.util.EnumSet.noneOf(PopupController.PopupKind.class),
                controleur.closeAll());
    }
}
