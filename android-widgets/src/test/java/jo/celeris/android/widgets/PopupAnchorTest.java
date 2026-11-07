package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * L'ancrage des popups portés par la couche (M7.5, §7, reprise) :
 * chaque position bornée à la couche — un popup ne sort jamais de
 * l'écran, la bulle ne recouvre jamais le doigt, la bande d'erreurs
 * tient le bord droit pleine hauteur. Pur, sans SDK.
 */
class PopupAnchorTest {

    private static final int LARGEUR = 1080;
    private static final int HAUTEUR = 1920;
    private static final int MARGE = 8;

    /** Sous le curseur si la place y tient, au-dessus sinon. */
    @Test
    void belowOrAboveCaretFollowsTheRoom() {
        PopupPlacer.Cadre popup = new PopupPlacer.Cadre(0, 0, 200, 300);
        PopupPlacer.Cadre dessous = PopupAnchor.belowOrAboveCaret(400, 500,
                popup, LARGEUR, HAUTEUR, MARGE);
        assertEquals(500, dessous.y(), "la place tient : sous le curseur");
        PopupPlacer.Cadre dessus = PopupAnchor.belowOrAboveCaret(400, 1800,
                popup, LARGEUR, HAUTEUR, MARGE);
        assertEquals(1500, dessus.y(),
                "la place manque : au-dessus, collée au bas du curseur");
        // Le bord bas : même en bas de couche, le popup reste dedans.
        PopupPlacer.Cadre borne = PopupAnchor.belowOrAboveCaret(400, 1900,
                popup, LARGEUR, HAUTEUR, MARGE);
        assertTrue(borne.y() + borne.hauteur() <= HAUTEUR - MARGE,
                "jamais hors couche, même au bord");
    }

    /** Au-dessus du curseur si la place y tient — la signature (D-10). */
    @Test
    void aboveOrBelowCaretPrefersAbove() {
        PopupPlacer.Cadre popup = new PopupPlacer.Cadre(0, 0, 300, 200);
        PopupPlacer.Cadre dessus = PopupAnchor.aboveOrBelowCaret(500, 600,
                popup, LARGEUR, HAUTEUR, MARGE);
        assertEquals(400, dessus.y(), "la place tient : au-dessus du curseur");
        // Le curseur tout en haut : la signature passe dessous, dedans.
        PopupPlacer.Cadre bas = PopupAnchor.aboveOrBelowCaret(500, 100,
                popup, LARGEUR, HAUTEUR, MARGE);
        assertTrue(bas.y() >= MARGE && bas.y() + bas.hauteur() <= HAUTEUR,
                "jamais hors couche, même en haut");
    }

    /** La bulle ne recouvre jamais le point : au-dessus, décalée. */
    @Test
    void atPointStaysAboveTheFinger() {
        PopupPlacer.Cadre popup = new PopupPlacer.Cadre(0, 0, 280, 160);
        PopupPlacer.Cadre bulle = PopupAnchor.atPoint(540, 900, popup,
                LARGEUR, HAUTEUR, MARGE);
        assertTrue(bulle.y() + bulle.hauteur() <= 900,
                "la bulle finit au-dessus du point touché");
        assertEquals(400, bulle.x(),
                "centrée sur le point, bornée à la couche");
        // Le point tout en haut : la bulle passe dessous, dans la couche.
        PopupPlacer.Cadre haut = PopupAnchor.atPoint(540, 100, popup,
                LARGEUR, HAUTEUR, MARGE);
        assertTrue(haut.y() >= MARGE, "en haut de couche, la bulle reste dedans");
    }

    /** Le fil d'Ariane : bandeau haut-gauche, borné à la largeur. */
    @Test
    void breadcrumbSitsTopLeft() {
        PopupPlacer.Cadre popup = new PopupPlacer.Cadre(0, 0, 900, 48);
        PopupPlacer.Cadre bandeau = PopupAnchor.topLeftCorner(popup,
                LARGEUR, HAUTEUR, MARGE);
        assertEquals(MARGE, bandeau.x());
        assertEquals(MARGE, bandeau.y());
        assertTrue(bandeau.largeur() <= LARGEUR - 2 * MARGE,
                "le bandeau ne sort jamais de la largeur");
    }

    /** La bande d'erreurs : bord droit, pleine hauteur. */
    @Test
    void errorBandHoldsTheRightEdge() {
        PopupPlacer.Cadre popup = new PopupPlacer.Cadre(0, 0, 24, 1904);
        PopupPlacer.Cadre bande = PopupAnchor.rightEdge(popup,
                LARGEUR, HAUTEUR, MARGE);
        assertEquals(LARGEUR - 24 - MARGE, bande.x(), "collée au bord droit");
        assertEquals(HAUTEUR - 2 * MARGE, bande.hauteur(),
                "pleine hauteur de couche, respirations comprises");
    }

    /** La barre de sélection : centrée sur la sélection, au-dessus. */
    @Test
    void selectionBarCentersAboveTheSelection() {
        PopupPlacer.Cadre popup = new PopupPlacer.Cadre(0, 0, 320, 56);
        PopupPlacer.Cadre barre = PopupAnchor.centeredAboveSelection(
                300, 700, 400, popup, LARGEUR, HAUTEUR, MARGE);
        assertEquals(340, barre.x(), "centrée sur le milieu de la sélection");
        assertEquals(344, barre.y(), "au-dessus de la sélection");
        // La sélection tout en haut : la barre passe dessous, dedans.
        PopupPlacer.Cadre haut = PopupAnchor.centeredAboveSelection(
                300, 700, 20, popup, LARGEUR, HAUTEUR, MARGE);
        assertTrue(haut.y() >= MARGE && haut.y() + haut.hauteur() <= HAUTEUR,
                "jamais hors couche, même en haut");
    }
}
