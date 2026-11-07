package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Les régimes des dix popups (M7.5, §7, reprise) : la documentation
 * suit sa liste, la barre de sélection cède à la frappe mais survit
 * au défilement, la bande d'erreurs ne cède à rien — pures, avant les
 * vues.
 */
class PopupRegimesTest {

    /** La documentation n'ouvre pas sans sa liste. */
    @Test
    void completionDocNeedsItsList() {
        PopupController controleur = new PopupController();
        assertFalse(controleur.open(PopupController.PopupKind.COMPLETION_DOC),
                "la documentation seule refuse d'ouvrir");
        assertTrue(controleur.open(PopupController.PopupKind.COMPLETION_LIST));
        assertTrue(controleur.open(PopupController.PopupKind.COMPLETION_DOC),
                "sur sa liste ouverte, la documentation ouvre");
    }

    /** Fermer la liste ferme la documentation — jamais seule. */
    @Test
    void closingTheListClosesItsDoc() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        controleur.open(PopupController.PopupKind.COMPLETION_DOC);
        controleur.close(PopupController.PopupKind.COMPLETION_LIST);
        assertFalse(controleur.isOpen(PopupController.PopupKind.COMPLETION_DOC),
                "la documentation ne survit pas à sa liste");
    }

    /** La barre de sélection cède à la frappe, survit au défilement. */
    @Test
    void selectionBarYieldsToTypingButSurvivesScroll() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.SELECTION_BAR);
        controleur.onScroll();
        assertTrue(controleur.isOpen(PopupController.PopupKind.SELECTION_BAR),
                "au défilement, la barre se REPOSITIONNE, elle ne se ferme pas");
        controleur.onKeystroke();
        assertFalse(controleur.isOpen(PopupController.PopupKind.SELECTION_BAR),
                "la frappe remplace la sélection — la barre se ferme");
    }

    /** La bande d'erreurs ne cède ni à la frappe ni au défilement. */
    @Test
    void errorBandSurvivesTypingAndScroll() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.ERROR_BAND);
        controleur.onKeystroke();
        controleur.onScroll();
        assertTrue(controleur.isOpen(PopupController.PopupKind.ERROR_BAND),
                "la bande vit avec les diagnostics, la frappe et le défilement ne la touchent pas");
    }

    /** L'aide aux paramètres cohabite avec la liste ET sa documentation. */
    @Test
    void signatureHelpCohabitsWithListAndDoc() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        controleur.open(PopupController.PopupKind.COMPLETION_DOC);
        controleur.open(PopupController.PopupKind.SIGNATURE_HELP);
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST));
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_DOC));
        assertTrue(controleur.isOpen(PopupController.PopupKind.SIGNATURE_HELP),
                "les trois popups unifiées tiennent ensemble (D-10)");
    }
}
