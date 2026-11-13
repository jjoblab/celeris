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
        controleur.open(PopupController.PopupKind.TYPE_INFO);
        controleur.open(PopupController.PopupKind.HIERARCHY);
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.HOVER,
                        PopupController.PopupKind.DIAGNOSTIC_TOOLTIP,
                        PopupController.PopupKind.SELECTION_BAR,
                        PopupController.PopupKind.SELECTION_OVERFLOW,
                        PopupController.PopupKind.COLOR_PICKER,
                        PopupController.PopupKind.TYPE_INFO,
                        PopupController.PopupKind.HIERARCHY),
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
        controleur.open(PopupController.PopupKind.TYPE_INFO);
        controleur.open(PopupController.PopupKind.HIERARCHY);
        controleur.open(PopupController.PopupKind.MINIMAP);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.HOVER,
                        PopupController.PopupKind.DIAGNOSTIC_TOOLTIP,
                        PopupController.PopupKind.COLOR_PICKER,
                        PopupController.PopupKind.TYPE_INFO,
                        PopupController.PopupKind.HIERARCHY),
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

    /** La carte de la toolbar (chantier d'unification) vit son propre
     *  genre : deux menus ne tiennent jamais — elle ferme la complétion
     *  et le menu du caret, eux la ferment. */
    @Test
    void toolbarOverflowExcludesTheMenus() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        assertTrue(controleur.open(PopupController.PopupKind.TOOLBAR_OVERFLOW),
                "la carte de la toolbar ouvre…");
        assertFalse(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST),
                "…et ferme la complétion");
        controleur.open(PopupController.PopupKind.INTENTIONS_MENU);
        assertFalse(controleur.isOpen(PopupController.PopupKind.TOOLBAR_OVERFLOW),
                "le menu du caret ferme la carte de la toolbar");
        assertTrue(controleur.open(PopupController.PopupKind.COMPLETION_LIST));
        assertFalse(controleur.isOpen(PopupController.PopupKind.INTENTIONS_MENU),
                "la complétion ferme le menu");
    }

    /** La carte de la toolbar cède à la frappe, survit au défilement —
     *  son ancre vit au coin de scène, pas au texte. */
    @Test
    void toolbarOverflowYieldsToKeystrokeNotToScroll() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.TOOLBAR_OVERFLOW);
        controleur.onKeystroke();
        assertFalse(controleur.isOpen(PopupController.PopupKind.TOOLBAR_OVERFLOW),
                "un menu ne survit pas à la saisie");
        controleur.open(PopupController.PopupKind.TOOLBAR_OVERFLOW);
        controleur.onScroll();
        assertTrue(controleur.isOpen(PopupController.PopupKind.TOOLBAR_OVERFLOW),
                "le coin de scène ne suit pas le texte — le défilement l'épargne");
    }

    /** ouvrirRendus rend l'OUVERT et ses EXCLUS — le coordinateur y
     *  retire d'un seul tenant les vues des exclus (l'état et la
     *  couche ne divergent jamais). */
    @Test
    void openRendusCarryTheExcludedGenres() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.COMPLETION_LIST,
                        PopupController.PopupKind.TOOLBAR_OVERFLOW),
                controleur.ouvrirRendus(PopupController.PopupKind.TOOLBAR_OVERFLOW),
                "l'ouverture rend la carte ET l'exclu");
        assertEquals(java.util.EnumSet.noneOf(PopupController.PopupKind.class),
                controleur.ouvrirRendus(PopupController.PopupKind.TOOLBAR_OVERFLOW),
                "le re-pose rend vide — la carte se re-monte quand même");
        assertEquals(java.util.EnumSet.noneOf(PopupController.PopupKind.class),
                controleur.ouvrirRendus(PopupController.PopupKind.COMPLETION_DOC),
                "la documentation sans sa liste ne rend rien : la machine refuse");
    }

    /** closeRendus rend le fermé et ses CASCADES — la documentation
     *  suit sa liste, le débordement suit sa barre. */
    @Test
    void closeRendusCarryTheCascadedGenres() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        controleur.open(PopupController.PopupKind.COMPLETION_DOC);
        controleur.open(PopupController.PopupKind.SELECTION_BAR);
        controleur.open(PopupController.PopupKind.SELECTION_OVERFLOW);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.COMPLETION_LIST,
                        PopupController.PopupKind.COMPLETION_DOC),
                controleur.closeRendus(PopupController.PopupKind.COMPLETION_LIST));
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.SELECTION_BAR,
                        PopupController.PopupKind.SELECTION_OVERFLOW),
                controleur.closeRendus(PopupController.PopupKind.SELECTION_BAR));
        assertFalse(controleur.open(PopupController.PopupKind.COMPLETION_DOC),
                "open et ouvrirRendus restent d'un seul tenant");
    }

    /** Le tap hors cartes (chantier d'unification) : les cartes
     *  d'actions et les bulles cèdent — la complétion, la signature, le
     *  fil d'Ariane et le chrome suivent le curseur ou le document.
     *  Le menu d'intentions et la complétion ne tenant JAMAIS ensemble
     *  (l'exclusion mutuelle), le banc prouve les survivants en deux
     *  temps : le menu d'abord, la complétion rouverte ensuite. */
    @Test
    void tapOutsideClosesActionCardsAndBubblesOnly() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.SIGNATURE_HELP);
        controleur.open(PopupController.PopupKind.BREADCRUMB);
        controleur.open(PopupController.PopupKind.INTENTIONS_MENU);
        controleur.open(PopupController.PopupKind.MINIMAP);
        controleur.open(PopupController.PopupKind.COLOR_PICKER);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.INTENTIONS_MENU,
                        PopupController.PopupKind.COLOR_PICKER),
                controleur.surToucheHorsCartes(),
                "les menus et le sélecteur cèdent au geste extérieur");
        assertTrue(controleur.isOpen(PopupController.PopupKind.SIGNATURE_HELP),
                "la signature suit le curseur");
        assertTrue(controleur.isOpen(PopupController.PopupKind.MINIMAP),
                "le chrome vit avec le document");
        // la complétion rouverte : le menu cédé, l'exclusion ne la chasse plus
        assertTrue(controleur.open(PopupController.PopupKind.COMPLETION_LIST),
                "la complétion se rouvre sur le menu cédé");
        assertTrue(controleur.open(PopupController.PopupKind.COMPLETION_DOC),
                "la documentation suit sa liste rouverte");
        assertEquals(
                java.util.EnumSet.noneOf(PopupController.PopupKind.class),
                controleur.surToucheHorsCartes(),
                "le second geste ne referme rien : rien ne cède");
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST),
                "la complétion suit le curseur, pas le doigt");
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_DOC),
                "la documentation suit sa liste, pas le doigt");
    }

    /** La carte des usages (le signal du propriétaire du 2026-11-13,
     *  la première surface réveillée) vit les régimes des surfaces
     *  d'actions : deux surfaces ne tiennent jamais — elle ferme le
     *  menu du caret et le débordement de la toolbar, eux la ferment ;
     *  la complétion cohabite (elle suit le curseur, l'usage décrit
     *  le symbole, la saisie la chassera). */
    @Test
    void referencesExcludeTheMenusAndReciprocally() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.INTENTIONS_MENU);
        assertTrue(controleur.open(PopupController.PopupKind.REFERENCES),
                "la carte des usages ouvre…");
        assertFalse(controleur.isOpen(PopupController.PopupKind.INTENTIONS_MENU),
                "…et ferme le menu du caret");
        controleur.open(PopupController.PopupKind.TOOLBAR_OVERFLOW);
        assertFalse(controleur.isOpen(PopupController.PopupKind.REFERENCES),
                "le débordement de la toolbar ferme les usages");
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        assertFalse(controleur.isOpen(PopupController.PopupKind.TOOLBAR_OVERFLOW),
                "la complétion ferme le débordement");
        controleur.open(PopupController.PopupKind.REFERENCES);
        assertTrue(controleur.isOpen(PopupController.PopupKind.REFERENCES),
                "les usages cohabitent avec la complétion");
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST),
                "la complétion survit : la carte décrit, la saisie chassera");
    }

    /** La carte des usages cède à la frappe (la saisie remplace son
     *  objet), au défilement (son ancre vit au curseur) et au tap
     *  hors cartes (la carte d'actions cède au geste extérieur) — le
     *  chrome survit. */
    @Test
    void referencesYieldToKeystrokeScrollAndOutsideTap() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.REFERENCES);
        controleur.open(PopupController.PopupKind.MINIMAP);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.REFERENCES),
                controleur.onKeystroke(), "la saisie remplace l'objet décrit");
        controleur.open(PopupController.PopupKind.REFERENCES);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.REFERENCES),
                controleur.onScroll(), "l'ancre vit au curseur, le défilement la chasse");
        controleur.open(PopupController.PopupKind.REFERENCES);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.REFERENCES),
                controleur.surToucheHorsCartes(),
                "la carte d'actions cède au geste extérieur");
        assertTrue(controleur.isOpen(PopupController.PopupKind.MINIMAP),
                "le chrome survit aux trois régimes");
        controleur.open(PopupController.PopupKind.HIERARCHY);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.HIERARCHY),
                controleur.surToucheHorsCartes(),
                "la hiérarchie, carte d'actions, cède au geste extérieur");
    }

    /** La hiérarchie (catégorie C de l'inventaire — les moteurs
     *  vivaient, la surface dormait) joint la famille des surfaces
     *  d'actions : elle ferme le menu du caret, le débordement de la
     *  toolbar ET la carte des usages — eux la ferment ; la
     *  complétion cohabite (la saisie la chassera à la frappe). */
    @Test
    void hierarchyExcludesTheActionSurfacesAndReciprocally() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.INTENTIONS_MENU);
        controleur.open(PopupController.PopupKind.REFERENCES);
        assertTrue(controleur.open(PopupController.PopupKind.HIERARCHY),
                "la hiérarchie ouvre…");
        assertFalse(controleur.isOpen(PopupController.PopupKind.INTENTIONS_MENU),
                "…et ferme le menu du caret");
        assertFalse(controleur.isOpen(PopupController.PopupKind.REFERENCES),
                "…et ferme la carte des usages — une surface d'actions à la fois");
        controleur.open(PopupController.PopupKind.TOOLBAR_OVERFLOW);
        assertFalse(controleur.isOpen(PopupController.PopupKind.HIERARCHY),
                "le débordement de la toolbar ferme la hiérarchie");
        controleur.open(PopupController.PopupKind.REFERENCES);
        assertFalse(controleur.isOpen(PopupController.PopupKind.TOOLBAR_OVERFLOW),
                "la carte des usages ferme le débordement");
        controleur.open(PopupController.PopupKind.HIERARCHY);
        assertFalse(controleur.isOpen(PopupController.PopupKind.REFERENCES),
                "la hiérarchie ferme la carte des usages");
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        assertTrue(controleur.isOpen(PopupController.PopupKind.HIERARCHY),
                "la hiérarchie cohabite avec la complétion — elle décrit le"
                        + " symbole, la saisie la chassera");
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST),
                "la complétion survit : la carte décrit, la saisie chassera");
    }

    /** La documentation externe (catégorie A de l'inventaire — la carte
     *  de documentation se dérive) vit le régime de la CONSULTATION :
     *  ouverte, elle se lit — la frappe ne la chasse pas (on tape à
     *  côté), le défilement ne la ferme pas non plus (elle se
     *  repositionne chez l'hôte), le tap hors cartes la referme (la
     *  consultation cède au geste qui déplace le curseur) ; elle
     *  cohabite avec tout : une documentation ne chasse personne. */
    @Test
    void documentationExterneSeConsulteSansCederALaFrappe() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        assertTrue(controleur.open(PopupController.PopupKind.EXTERNAL_DOC),
                "la documentation ouvre sur la complétion ouverte");
        controleur.onKeystroke();
        assertTrue(controleur.isOpen(PopupController.PopupKind.EXTERNAL_DOC),
                "la frappe ne chasse pas la consultation");
        controleur.onScroll();
        assertTrue(controleur.isOpen(PopupController.PopupKind.EXTERNAL_DOC),
                "le défilement la repositionne, il ne la ferme pas");
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST),
                "la complétion poursuit sa vie");
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.EXTERNAL_DOC),
                controleur.surToucheHorsCartes(),
                "le geste extérieur referme la consultation");
        controleur.open(PopupController.PopupKind.EXTERNAL_DOC);
        controleur.close(PopupController.PopupKind.EXTERNAL_DOC);
        assertFalse(controleur.isOpen(PopupController.PopupKind.EXTERNAL_DOC),
                "la fermeture sur demande reste franche");
    }

    /** La notification (catégorie G de l'inventaire — la forme restait
     *  à décider, la bulle éphémère l'emporte) vit le régime UNIQUE :
     *  elle annonce, elle ne décrit pas le texte — la frappe et le
     *  défilement l'épargnent, le tap hors cartes l'épargne aussi
     *  (l'annonce vit son quart, le geste ne l'interrompt pas, la
     *  queue du fil la referme) ; le voile modal la coupe — la feuille
     *  la recouvre ; aucune exclusion : elle cohabite avec tout. */
    @Test
    void notificationVitLeRegimeUniqueDeLannonce() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        controleur.open(PopupController.PopupKind.NOTIFICATION);
        controleur.onKeystroke();
        controleur.onScroll();
        controleur.surToucheHorsCartes();
        assertTrue(controleur.isOpen(PopupController.PopupKind.NOTIFICATION),
                "l'annonce vit son quart — frappe, défilement et geste"
                        + " extérieur ne l'interrompent pas");
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST),
                "la saisie poursuit sa vie, l'annonce ne chasse personne");
        controleur.closeAll();
        assertFalse(controleur.isOpen(PopupController.PopupKind.NOTIFICATION),
                "la queue du fil la referme, la couche suit");
    }

    /** L'info de type (catégorie A de l'inventaire — le fil rouge du
     *  guide du développeur, la seconde surface réveillée) vit les
     *  régimes du survol : elle décrit, elle ne choisit pas — la
     *  frappe et le défilement la referment (la saisie remplace son
     *  objet, son ancre vit au curseur), le tap hors cartes aussi —
     *  elle cohabite avec tout (aucune exclusion : une bulle qui
     *  décrit ne chasse personne). */
    @Test
    void typeInfoVitLesRegimesDuSurvol() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        assertTrue(controleur.open(PopupController.PopupKind.TYPE_INFO),
                "l'info de type ouvre sur la complétion ouverte");
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST),
                "la bulle qui décrit ne chasse pas la saisie");
        controleur.onKeystroke();
        assertFalse(controleur.isOpen(PopupController.PopupKind.TYPE_INFO),
                "la saisie remplace l'objet décrit");
        assertTrue(controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST),
                "la complétion filtre, elle reste");
        controleur.open(PopupController.PopupKind.TYPE_INFO);
        controleur.onScroll();
        assertFalse(controleur.isOpen(PopupController.PopupKind.TYPE_INFO),
                "l'ancre vit au curseur, le défilement la chasse");
        controleur.open(PopupController.PopupKind.TYPE_INFO);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.TYPE_INFO),
                controleur.surToucheHorsCartes(),
                "la bulle cède au geste extérieur");
    }

    /** La carte de gabarits (catégorie D de l'inventaire — les moteurs
     *  de génération vivaient, la surface dormait) joint la famille des
     *  surfaces d'actions : elle ferme le menu du caret, le
     *  débordement de la toolbar, la carte des usages ET la hiérarchie
     *  — eux la ferment ; la saisie la chasse quand la complétion
     *  s'ouvre (un menu ne survit pas à la liste) ; elle vit les
     *  régimes du menu du caret — la frappe seule et le défilement
     *  l'épargnent (elle se repositionne), le tap hors cartes la
     *  referme. */
    @Test
    void gabaritsExcludeTheActionSurfacesAndReciprocally() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.INTENTIONS_MENU);
        controleur.open(PopupController.PopupKind.REFERENCES);
        assertTrue(controleur.open(PopupController.PopupKind.GABARITS),
                "la carte de gabarits ouvre…");
        assertFalse(controleur.isOpen(PopupController.PopupKind.INTENTIONS_MENU),
                "…et ferme le menu du caret");
        assertFalse(controleur.isOpen(PopupController.PopupKind.REFERENCES),
                "…et ferme la carte des usages — une surface d'actions à la fois");
        controleur.open(PopupController.PopupKind.TOOLBAR_OVERFLOW);
        assertFalse(controleur.isOpen(PopupController.PopupKind.GABARITS),
                "le débordement de la toolbar ferme les gabarits");
        controleur.open(PopupController.PopupKind.GABARITS);
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        assertFalse(controleur.isOpen(PopupController.PopupKind.GABARITS),
                "la complétion chasse le menu — la saisie prend le relais");
        controleur.open(PopupController.PopupKind.GABARITS);
        controleur.onKeystroke();
        assertTrue(controleur.isOpen(PopupController.PopupKind.GABARITS),
                "la frappe seule ne referme pas le menu du caret");
        controleur.onScroll();
        assertTrue(controleur.isOpen(PopupController.PopupKind.GABARITS),
                "le défilement le repositionne, il ne le ferme pas");
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.GABARITS),
                controleur.surToucheHorsCartes(),
                "le geste extérieur referme la carte d'actions");
    }

    /** La carte des usages qui barrent (catégorie D de l'inventaire —
     *  le moteur B-23 servait les verdicts, la surface dormait) vit
     *  les régimes de la carte des usages : famille des surfaces
     *  d'actions (elle ferme les menus et la hiérarchie, eux la
     *  ferment — la cible du déménagement comprise, une surface
     *  d'actions à la fois), la frappe la ferme (la saisie remplace
     *  son objet), le défilement la ferme (son ancre vit au
     *  curseur), le tap hors cartes la referme — elle cohabite avec
     *  la complétion (elle décrit, la saisie la chassera). */
    @Test
    void safeDeleteVitLesRegimesDeLaCarteDesUsages() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.INTENTIONS_MENU);
        controleur.open(PopupController.PopupKind.HIERARCHY);
        assertTrue(controleur.open(PopupController.PopupKind.SAFE_DELETE),
                "la carte des usages qui barrent ouvre…");
        assertFalse(controleur.isOpen(PopupController.PopupKind.INTENTIONS_MENU),
                "…et ferme le menu du caret");
        assertFalse(controleur.isOpen(PopupController.PopupKind.HIERARCHY),
                "…et ferme la hiérarchie — une surface d'actions à la fois");
        controleur.open(PopupController.PopupKind.SAFE_MOVE);
        assertFalse(controleur.isOpen(PopupController.PopupKind.SAFE_DELETE),
                "la carte des cibles ferme la carte des usages qui barrent");
        controleur.open(PopupController.PopupKind.SAFE_DELETE);
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        assertTrue(controleur.isOpen(PopupController.PopupKind.SAFE_DELETE),
                "la complétion cohabite : la carte décrit, la saisie chassera");
        controleur.onKeystroke();
        assertFalse(controleur.isOpen(PopupController.PopupKind.SAFE_DELETE),
                "la saisie remplace l'objet décrit");
        controleur.open(PopupController.PopupKind.SAFE_DELETE);
        controleur.onScroll();
        assertFalse(controleur.isOpen(PopupController.PopupKind.SAFE_DELETE),
                "l'ancre vit au curseur, le défilement la chasse");
        controleur.open(PopupController.PopupKind.SAFE_DELETE);
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.SAFE_DELETE),
                controleur.surToucheHorsCartes(),
                "la carte d'actions cède au geste extérieur");
    }

    /** La carte des cibles du déménagement (catégorie D de
     *  l'inventaire — le moteur B-23 servait les plans, la surface
     *  dormait) vit les régimes du menu du caret : la complétion la
     *  chasse (un menu ne survit pas à la liste), la frappe seule et
     *  le défilement l'épargnent (elle se repositionne), le tap hors
     *  cartes la referme — la famille des surfaces d'actions se
     *  referme sur elle. */
    @Test
    void safeMoveVitLesRegimesDuMenuDuCaret() {
        PopupController controleur = new PopupController();
        controleur.open(PopupController.PopupKind.INTENTIONS_MENU);
        assertTrue(controleur.open(PopupController.PopupKind.SAFE_MOVE),
                "la carte des cibles ouvre…");
        assertFalse(controleur.isOpen(PopupController.PopupKind.INTENTIONS_MENU),
                "…et ferme le menu du caret");
        controleur.open(PopupController.PopupKind.GABARITS);
        assertFalse(controleur.isOpen(PopupController.PopupKind.SAFE_MOVE),
                "la carte de gabarits ferme la carte des cibles");
        controleur.open(PopupController.PopupKind.SAFE_MOVE);
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        assertFalse(controleur.isOpen(PopupController.PopupKind.SAFE_MOVE),
                "la complétion chasse le menu — la saisie prend le relais");
        controleur.open(PopupController.PopupKind.SAFE_MOVE);
        controleur.onKeystroke();
        assertTrue(controleur.isOpen(PopupController.PopupKind.SAFE_MOVE),
                "la frappe seule ne referme pas le menu du caret");
        controleur.onScroll();
        assertTrue(controleur.isOpen(PopupController.PopupKind.SAFE_MOVE),
                "le défilement le repositionne, il ne le ferme pas");
        assertEquals(
                java.util.EnumSet.of(PopupController.PopupKind.SAFE_MOVE),
                controleur.surToucheHorsCartes(),
                "le geste extérieur referme la carte d'actions");
    }
}
