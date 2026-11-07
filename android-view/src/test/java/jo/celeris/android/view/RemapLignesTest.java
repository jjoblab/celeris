package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jo.celeris.core.syntax.EditSite;

import org.junit.jupiter.api.Test;

/**
 * Tests du remappage des lignes d'une publication périmée : la zone
 * touchée d'une édition ne se remappe jamais, les lignes d'avant gardent
 * leur index, celles d'après glissent du delta — composées de la plus
 * récente à la plus ancienne (PIEGE-20 par construction : seule une
 * ligne au contenu démontré identique se colore).
 */
class RemapLignesTest {

    /** Un site d'édition en lignes, premier constructeur public du record. */
    private static EditSite site(int premiere, int derniere, int delta) {
        return new EditSite(premiere, derniere, delta);
    }

    /** Ligne avant la zone touchée : même index, contenu identique. */
    @Test
    void ligneAvantLaZoneToucheeGardeSonIndex() {
        RemapLignes remap = RemapLignes.de(List.of(site(2, 2, 0)));
        assertEquals(1, remap.lignePubliee(1, 5));
        assertEquals(0, remap.lignePubliee(0, 5));
    }

    /** Ligne dans la zone touchée : jamais remappée, le contenu a changé. */
    @Test
    void ligneDansLaZoneToucheeNeSeRemappePas() {
        RemapLignes remap = RemapLignes.de(List.of(site(2, 4, 2)));
        assertEquals(-1, remap.lignePubliee(2, 8));
        assertEquals(-1, remap.lignePubliee(3, 8));
        assertEquals(-1, remap.lignePubliee(4, 8));
    }

    /** Ligne après la zone touchée : glissée du delta, contenu identique. */
    @Test
    void ligneApresLaZoneGlisseDuDelta() {
        RemapLignes remap = RemapLignes.de(List.of(site(2, 4, 2)));
        assertEquals(3, remap.lignePubliee(5, 8));
        assertEquals(5, remap.lignePubliee(7, 8));
    }

    /** Une ligne résolue hors des bornes du document publié ne se colore pas. */
    @Test
    void ligneHorsBornesDuPublieNeSeRemappePas() {
        RemapLignes remap = RemapLignes.de(List.of(site(2, 4, 0)));
        assertEquals(-1, remap.lignePubliee(9, 5), "au-delà de la dernière ligne publiée");
        assertEquals(-1, remap.lignePubliee(-1, 5), "avant la première ligne");
    }

    /**
     * Deux éditions composées : la plus récente se rejoue d'abord, la
     * plus ancienne referme le trajet vers le document publié.
     */
    @Test
    void deuxEditionsSeComposentDeLaPlusRecenteALaPlusAncienne() {
        // La première édition insère une ligne à la ligne 1 ; la seconde
        // édite la ligne 5 (index du document intermédiaire).
        RemapLignes remap = RemapLignes.de(List.of(site(1, 1, 1), site(5, 5, 0)));
        assertEquals(0, remap.lignePubliee(0, 8), "avant tout : identité");
        assertEquals(-1, remap.lignePubliee(1, 8), "zone de la première édition");
        assertEquals(2, remap.lignePubliee(3, 8),
                "entre les deux zones : seul le premier delta se rejoue");
        assertEquals(-1, remap.lignePubliee(5, 8),
                "zone de la seconde édition");
        assertEquals(6, remap.lignePubliee(7, 8),
                "après tout : les deux deltas se composent");
    }

    /** Une suppression de lignes compose comme une insertion, delta négatif. */
    @Test
    void suppressionDeLignesComposeAvecDeltaNegatif() {
        // La première édition insère une ligne à la ligne 1 ; la seconde
        // supprime les lignes 2 à 4 du document intermédiaire.
        RemapLignes remap = RemapLignes.de(List.of(site(1, 1, 1), site(2, 4, -3)));
        assertEquals(0, remap.lignePubliee(0, 8), "avant tout : identité");
        assertEquals(-1, remap.lignePubliee(1, 8), "la ligne insérée n'existait pas");
        assertEquals(-1, remap.lignePubliee(2, 8), "zone de la suppression");
        assertEquals(7, remap.lignePubliee(5, 8),
                "au-delà de la suppression : les deltas se composent (+3 puis -1)");
    }

    /** Chaîne vide : l'identité, bornée au document publié. */
    @Test
    void chaineVideServitIdentiteBornee() {
        RemapLignes remap = RemapLignes.de(List.of());
        assertEquals(2, remap.lignePubliee(2, 5));
        assertEquals(-1, remap.lignePubliee(5, 5));
    }
}
