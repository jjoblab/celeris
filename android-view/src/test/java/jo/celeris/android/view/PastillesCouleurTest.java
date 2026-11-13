package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Les littéraux de couleur de la gouttière (C-05) : chaque forme
 * Android se résout — la courte étend ses composantes, la forme sans
 * alpha prend l'alpha plein — et une pastille par ligne, la première.
 */
class PastillesCouleurTest {

    @Test
    void lesFormesLonguesSeLisentTellesQuelles() {
        assertEquals(0xFFFF5722, PastillesCouleur.resoudre("FF5722"));
        assertEquals(0x80FF5722, PastillesCouleur.resoudre("80FF5722"));
        assertEquals(0xFFFFFFFF, PastillesCouleur.resoudre("FFFFFF"));
    }

    @Test
    void lesFormesCourtesEtendentLeursComposantes() {
        assertEquals(0xFFFF0000, PastillesCouleur.resoudre("F00"));
        assertEquals(0x00FFFFFF, PastillesCouleur.resoudre("0FFF"));
        assertEquals(0xFF00FF00, PastillesCouleur.resoudre("0F0"));
    }

    @Test
    void laPastilleSePoseSurLaLignePorteuse() {
        PastilleCouleur pastille = PastillesCouleur.pastille(4,
                "    <color name=\"accent\">#FF5722</color>");
        assertTrue(pastille != null);
        assertEquals(4, pastille.ligne());
        assertEquals(26, pastille.colonne());
        assertEquals(0xFFFF5722, pastille.argb());
        assertEquals("#FF5722".length(), pastille.longueur());
    }

    @Test
    void laPremiereCouleurSeuleCompteParLigne() {
        PastilleCouleur pastille = PastillesCouleur.pastille(0,
                "de #F00 puis #0F0 sur la meme ligne");
        assertTrue(pastille != null);
        assertEquals(0xFFFF0000, pastille.argb());
    }

    @Test
    void laLigneSansCouleurNePortePasDePastille() {
        assertNull(PastillesCouleur.pastille(0, "android:text=\"sans couleur\""));
        assertNull(PastillesCouleur.pastille(0, "#12G45 n'est pas hexa"));
        assertNull(PastillesCouleur.pastille(0, "#FF572 trop court"));
    }

    @Test
    void leLitteralProlongeNeComptePas() {
        assertNull(PastillesCouleur.pastille(0, "#FF57223"));
    }

    @Test
    void tousLesLitterauxDuneLigneSEncadrent() {
        List<PastilleCouleur> pastilles =
                PastillesCouleur.tous(3, "a #fff b #0000CD c");
        assertEquals(2, pastilles.size());
        assertEquals(3, pastilles.get(0).ligne());
        assertEquals(3, pastilles.get(0).colonne());
        assertEquals(0xFFFFFFFF, pastilles.get(0).argb());
        assertEquals(4, pastilles.get(0).longueur());
        assertEquals(10, pastilles.get(1).colonne());
        assertEquals(0xFF0000CD, pastilles.get(1).argb());
        assertEquals(7, pastilles.get(1).longueur());
        assertTrue(PastillesCouleur.tous(0, "sans couleur").isEmpty());
    }

    @Test
    void leFormatTientLaLargeurDuLitteral() {
        assertEquals("#e43", PastillesCouleur.formater(0xFFF44336, 3));
        assertEquals("#822196f3", PastillesCouleur.formater(0x822196F3, 8));
        assertEquals("#2196f3", PastillesCouleur.formater(0xFF2196F3, 6));
        assertEquals("#8f00", PastillesCouleur.formater(0x80FF0000, 4));
    }

    @Test
    void lesLignesRendentLeursPastillesDansLOrdre() {
        List<PastilleCouleur> pastilles = PastillesCouleur.desLignes(List.of(
                "<resources>",
                "    <color name=\"a\">#F00</color>",
                "    <color name=\"b\">#00FF00</color>",
                "</resources>"));
        assertEquals(2, pastilles.size());
        assertEquals(1, pastilles.get(0).ligne());
        assertEquals(0xFFFF0000, pastilles.get(0).argb());
        assertEquals(2, pastilles.get(1).ligne());
        assertEquals(0xFF00FF00, pastilles.get(1).argb());
    }

    @Test
    void lesPastillesViventDansLesMarquesDeGouttiere() {
        MarquesGouttiere marques = new MarquesGouttiere();
        assertEquals(0, marques.pastille(1));
        marques.setPastilles(PastillesCouleur.desLignes(List.of(
                "<color name=\"a\">#F00</color>")));
        assertEquals(0xFFFF0000, marques.pastille(0));
        assertEquals(0, marques.pastille(1));
        marques.setPastilles(List.of());
        assertEquals(0, marques.pastille(0));
    }
}
