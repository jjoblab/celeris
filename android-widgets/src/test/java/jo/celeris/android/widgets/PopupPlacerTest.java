package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Le placement coordonné des popups unifiées (D-10, M5) : complétion
 * sous le curseur (au-dessus en bord bas), documentation au flanc de
 * la complétion (à droite, à gauche en bord droit), signature du côté
 * opposé du curseur (au-dessus quand la complétion est en dessous,
 * repliée sous la complétion en bord haut) — jamais superposées, jamais
 * hors écran. L'accessibilité est annoncée : chaque popup porte son
 * rôle au rendu (le peintre l'annonce à l'explorateur, tranche vue).
 */
class PopupPlacerTest {

    /** La complétion sous le curseur, la documentation au flanc, la signature au-dessus. */
    @Test
    void placementCoordonne() {
        PopupPlacer.Placement place = PopupPlacer.placer(
                new PopupPlacer.Cadre(100, 200, 2, 20),
                new PopupPlacer.Cadre(0, 0, 80, 120),
                new PopupPlacer.Cadre(0, 0, 60, 100),
                new PopupPlacer.Cadre(0, 0, 70, 30),
                400, 600);
        // complétion : sous le curseur
        assertEquals(100, place.completion().x());
        assertEquals(220, place.completion().y());
        // documentation : à droite de la complétion, alignée dessus
        assertEquals(180, place.documentation().x());
        assertEquals(220, place.documentation().y());
        // signature : au-dessus du curseur (côté opposé à la complétion)
        assertEquals(100, place.signature().x());
        assertEquals(170, place.signature().y());
    }

    /** En bord bas, la complétion passe au-dessus, la signature repliée au-dessus d'elle. */
    @Test
    void enBordBasLaCompletionePasseAuDessus() {
        PopupPlacer.Placement place = PopupPlacer.placer(
                new PopupPlacer.Cadre(100, 580, 2, 20),
                new PopupPlacer.Cadre(0, 0, 80, 120),
                new PopupPlacer.Cadre(0, 0, 60, 100),
                new PopupPlacer.Cadre(0, 0, 70, 30),
                400, 600);
        // 600 + 120 dépasse : la complétion passe au-dessus du curseur
        assertEquals(460, place.completion().y());
        // sous le curseur, 630 dépasse : la signature se replie au-dessus de la complétion
        assertEquals(430, place.signature().y());
    }

    /** En bord droit, la documentation passe à gauche de la complétion. */
    @Test
    void enBordDroitDocumentationAGauche() {
        PopupPlacer.Placement place = PopupPlacer.placer(
                new PopupPlacer.Cadre(390, 200, 2, 20),
                new PopupPlacer.Cadre(0, 0, 80, 120),
                new PopupPlacer.Cadre(0, 0, 60, 100),
                new PopupPlacer.Cadre(0, 0, 70, 30),
                400, 600);
        // la complétion est ramenée dans l'écran
        assertEquals(320, place.completion().x());
        // 320 + 80 + 60 dépasse 400 : la documentation passe à gauche
        assertEquals(260, place.documentation().x());
    }

    /** La signature sans place au-dessus du curseur se replie sous la complétion. */
    @Test
    void signatureReplieeSousLaCompletion() {
        PopupPlacer.Placement place = PopupPlacer.placer(
                new PopupPlacer.Cadre(5, 5, 2, 20),
                new PopupPlacer.Cadre(0, 0, 200, 150),
                new PopupPlacer.Cadre(0, 0, 100, 120),
                new PopupPlacer.Cadre(0, 0, 150, 30),
                400, 400);
        // complétion sous le curseur, documentation à droite
        assertEquals(25, place.completion().y());
        assertEquals(205, place.documentation().x());
        // au-dessus du curseur (5 - 30) il n'y a pas de place : sous la complétion
        assertEquals(175, place.signature().y());
    }

    /** Aucune popup ne sort de l'écran, aucune superposition. */
    @Test
    void rienNeSortRienNeSeSuperpose() {
        PopupPlacer.Placement place = PopupPlacer.placer(
                new PopupPlacer.Cadre(5, 5, 2, 20),
                new PopupPlacer.Cadre(0, 0, 200, 150),
                new PopupPlacer.Cadre(0, 0, 100, 120),
                new PopupPlacer.Cadre(0, 0, 150, 30),
                400, 400);
        assertTrue(dansLEcran(place.completion(), 400, 400), "complétion dans l'écran");
        assertTrue(dansLEcran(place.documentation(), 400, 400), "documentation dans l'écran");
        assertTrue(dansLEcran(place.signature(), 400, 400), "signature dans l'écran");
        assertTrue(!superposees(place.completion(), place.documentation()),
                "complétion et documentation disjointes");
        assertTrue(!superposees(place.completion(), place.signature()),
                "complétion et signature disjointes");
        assertTrue(!superposees(place.documentation(), place.signature()),
                "documentation et signature disjointes");
    }

    private static boolean dansLEcran(PopupPlacer.Cadre cadre, int largeur, int hauteur) {
        return cadre.x() >= 0 && cadre.y() >= 0
                && cadre.x() + cadre.largeur() <= largeur
                && cadre.y() + cadre.hauteur() <= hauteur;
    }

    private static boolean superposees(PopupPlacer.Cadre a, PopupPlacer.Cadre b) {
        return a.x() < b.x() + b.largeur() && b.x() < a.x() + a.largeur()
                && a.y() < b.y() + b.hauteur() && b.y() < a.y() + a.hauteur();
    }
}
