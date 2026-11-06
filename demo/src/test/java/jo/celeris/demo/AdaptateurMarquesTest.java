package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import jo.celeris.android.host.spi.Arret;
import jo.celeris.android.host.spi.CadrePile;
import jo.celeris.android.host.spi.CheminDoc;
import jo.celeris.android.host.spi.EtatLignesVcs;
import jo.celeris.android.host.spi.LigneArret;
import jo.celeris.android.host.spi.MarqueVcs;
import jo.celeris.android.view.EtatArret;
import jo.celeris.android.view.MarqueLigne;
import jo.celeris.android.view.MarquesIntegration;

import org.junit.jupiter.api.Test;

/**
 * Tests de l'adaptateur des marques d'intégration de la démonstration
 * (D-03, D-04, D-05) : l'état des contrats de l'hôte — numérotés à
 * partir de un — devient les marques de la vue — numérotées à partir
 * de zéro — les arrêts et la pile se filtrent au fichier ouvert.
 */
final class AdaptateurMarquesTest {

    private static final CheminDoc OUVERT = CheminDoc.de("exemple.kt");

    /** Les marques VCS de l'hôte passent aux lignes de la vue. */
    @Test
    void marquesVcsPasseesAuxLignesDeLaVue() {
        EtatLignesVcs etat = EtatLignesVcs.de(Map.of(2, MarqueVcs.MODIFIE), List.of());
        MarquesIntegration marques = AdaptateurMarques.depuis(etat, List.of(),
                OUVERT, null, List.of());

        assertEquals(MarqueLigne.MODIFIE, marques.marqueVcs(1),
                "la ligne 2 de l'hôte est la ligne 1 de la vue");
        assertEquals(MarqueLigne.INCHANGE, marques.marqueVcs(0));
    }

    /** Les arrêts se filtrent au fichier ouvert, posés et désactivés. */
    @Test
    void arretsFiltresAuFichierOuvert() {
        Arret iciActif = new Arret(OUVERT, 3, true);
        Arret iciDesactive = new Arret(OUVERT, 7, false);
        Arret ailleurs = new Arret(CheminDoc.de("autre.kt"), 4, true);
        MarquesIntegration marques = AdaptateurMarques.depuis(
                EtatLignesVcs.vide(), List.of(iciActif, iciDesactive, ailleurs),
                OUVERT, null, List.of());

        assertEquals(EtatArret.POSE, marques.arret(2));
        assertEquals(EtatArret.DESACTIVE, marques.arret(6));
        assertEquals(EtatArret.AUCUN, marques.arret(3),
                "l'arrêt d'un autre fichier ne se peint pas ici");
    }

    /** La pause rend sa ligne courante et les lignes de sa pile. */
    @Test
    void pauseRendLigneCouranteEtPile() {
        LigneArret pause = new LigneArret(OUVERT, 5);
        List<CadrePile> pile = List.of(
                new CadrePile("Exemple", "pas", "exemple.kt", 5),
                new CadrePile("Exemple", "appelant", "exemple.kt", 2),
                new CadrePile("Autre", "ailleurs", "autre.kt", 9));
        MarquesIntegration marques = AdaptateurMarques.depuis(
                EtatLignesVcs.vide(), List.of(), OUVERT, pause, pile);

        assertEquals(4, marques.ligneCourante());
        assertTrue(marques.dansPile(4));
        assertTrue(marques.dansPile(1));
        assertFalse(marques.dansPile(8),
                "le cadre d'un autre fichier ne se peint pas ici");
    }

    /** La pause d'un autre fichier reste invisible ici. */
    @Test
    void pauseDAutreFichierInvisible() {
        LigneArret pause = new LigneArret(CheminDoc.de("autre.kt"), 5);
        MarquesIntegration marques = AdaptateurMarques.depuis(
                EtatLignesVcs.vide(), List.of(), OUVERT, pause, List.of());
        assertNull(marques.ligneCourante());
    }

    /** L'état vide de l'hôte rend l'état vide de la vue. */
    @Test
    void etatVideRendEtatVide() {
        MarquesIntegration marques = AdaptateurMarques.depuis(
                EtatLignesVcs.vide(), List.of(), OUVERT, null, List.of());
        assertTrue(marques.estVide());
    }
}
