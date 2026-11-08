package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Les régimes des popups (M7.5, §7, reprise) : la documentation
 * suit sa liste, le menu de débordement suit sa barre — il n'ouvre
 * pas sans elle et ne lui survit pas —, la barre de sélection cède à
 * la frappe mais survit au défilement, la bande d'erreurs ne cède à
 * rien — pures, avant les vues.
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

    /**
     * Le menu de débordement suit sa barre (le redressement du
     * propriétaire : le toolbar de sélection porte son menu overflow au
     * design du kit) : sans barre il n'ouvre pas, la barre fermée
     * l'emporte, la frappe les ferme ensemble, le défilement les garde.
     */
    @Test
    void selectionOverflowFollowsItsBar() {
        PopupController controleur = new PopupController();
        assertFalse(controleur.open(PopupController.PopupKind.SELECTION_OVERFLOW),
                "le menu de débordement n'ouvre pas sans sa barre");
        controleur.open(PopupController.PopupKind.SELECTION_BAR);
        assertTrue(controleur.open(PopupController.PopupKind.SELECTION_OVERFLOW),
                "sur sa barre ouverte, le menu ouvre");
        controleur.onScroll();
        assertTrue(controleur.isOpen(PopupController.PopupKind.SELECTION_OVERFLOW),
                "au défilement, le menu se REPOSITIONNE avec sa barre");
        controleur.close(PopupController.PopupKind.SELECTION_BAR);
        assertFalse(controleur.isOpen(PopupController.PopupKind.SELECTION_OVERFLOW),
                "le menu ne survit jamais à sa barre");
    }

    /** La frappe referme le menu de débordement avec sa barre. */
    @Test
    void selectionOverflowYieldsToTypingWithItsBar() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.SELECTION_BAR);
        controleur.open(PopupController.PopupKind.SELECTION_OVERFLOW);
        controleur.onKeystroke();
        assertFalse(controleur.isOpen(PopupController.PopupKind.SELECTION_BAR),
                "la frappe remplace la sélection — la barre se ferme");
        assertFalse(controleur.isOpen(PopupController.PopupKind.SELECTION_OVERFLOW),
                "le menu suit sa barre dans la fermeture");
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
