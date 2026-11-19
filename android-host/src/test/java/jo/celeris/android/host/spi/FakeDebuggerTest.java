package jo.celeris.android.host.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests du faux débogueur (E-03, D-04/D-05) : pose, suppression et
 * état désactivé des arrêts, ligne courante, survol borné à la pause.
 */
final class FakeDebuggerTest {

    /** Pose, état actif, suppression : le cycle de vie d'un point d'arrêt. */
    @Test
    void poseEtatSuppressionDBreakpoint() {
        FakeDebugger debogueur = new FakeDebugger();
        CheminDoc fichier = CheminDoc.de("/src/Main.java");

        debogueur.poserBreakpoint(fichier, 18);
        assertEquals(1, debogueur.arrets().size());
        assertTrue(debogueur.arrets().get(0).actif(), "pose : actif par defaut");
        assertEquals(18, debogueur.arrets().get(0).ligne());

        debogueur.basculerBreakpoint(fichier, 18);
        assertFalse(debogueur.arrets().get(0).actif(), "bascule : desactive");

        debogueur.basculerBreakpoint(fichier, 18);
        assertTrue(debogueur.arrets().get(0).actif(), "re-bascule : active");

        debogueur.retirerBreakpoint(fichier, 18);
        assertEquals(0, debogueur.arrets().size(), "suppression");
    }

    /** Un même fichier porte plusieurs arrêts distincts par ligne. */
    @Test
    void arretsDistinctsParLigne() {
        FakeDebugger debogueur = new FakeDebugger();
        CheminDoc fichier = CheminDoc.de("/src/Main.java");
        debogueur.poserBreakpoint(fichier, 3);
        debogueur.poserBreakpoint(fichier, 17);
        debogueur.poserBreakpoint(CheminDoc.de("/src/Autre.java"), 5);
        assertEquals(3, debogueur.arrets().size());
        debogueur.retirerBreakpoint(fichier, 3);
        assertEquals(2, debogueur.arrets().size());
    }

    /** Retirer ou basculer un arrêt absent ne casse rien : muet par conception. */
    @Test
    void arretAbsentMuet() {
        FakeDebugger debogueur = new FakeDebugger();
        debogueur.retirerBreakpoint(CheminDoc.de("/a"), 4);
        debogueur.basculerBreakpoint(CheminDoc.de("/a"), 4);
        assertEquals(0, debogueur.arrets().size());
    }

    /** Hors pause, aucune ligne courante : la colonne d'exécution se tait. */
    @Test
    void horsPauseAucuneLigneCourante() {
        FakeDebugger debogueur = new FakeDebugger();
        assertNull(debogueur.ligneCourante());
        assertTrue(debogueur.pileCourante().isEmpty(),
                "hors pause, la pile est vide — jamais null");
    }

    /** La pause signale la ligne et la pile ; l'exécution reprise libère. */
    @Test
    void pauseSignaleLigneEtPile() {
        FakeDebugger debogueur = new FakeDebugger();
        CheminDoc fichier = CheminDoc.de("/src/Main.java");
        debogueur.simulerPause(fichier, 18,
                List.of(new CadrePile("App", "main", "App.java", 30),
                        new CadrePile("Service", "inscrire", "Service.java", 18)));

        BreakpointLine courante = debogueur.ligneCourante();
        assertNotNull(courante, "en pause, la ligne courante existe");
        assertEquals(fichier, courante.fichier());
        assertEquals(18, courante.ligne());
        assertEquals(2, debogueur.pileCourante().size());
        assertEquals("Service.java", debogueur.pileCourante().get(1).fichier());

        debogueur.simulerReprise();
        assertNull(debogueur.ligneCourante());
    }

    /** Le survol est borné : sans pause, refus net, jamais de valeur fantôme. */
    @Test
    void survolBorneSansPause() {
        FakeDebugger debogueur = new FakeDebugger();
        assertThrows(IllegalStateException.class,
                () -> debogueur.evaluerAuSurvol(CheminDoc.de("/a"), 3, 7, "nom"));
    }

    /** En pause, le survol sert la valeur enregistrée pour la ligne. */
    @Test
    void survolEnPauseSertLaValeur() {
        FakeDebugger debogueur = new FakeDebugger();
        CheminDoc fichier = CheminDoc.de("/src/Main.java");
        debogueur.simulerPause(fichier, 18, List.of());
        debogueur.enregistrerSurvol(18, "nom", "\"amelie\"");

        assertEquals("\"amelie\"", debogueur.evaluerAuSurvol(fichier, 18, 7, "nom"));
        assertThrows(IllegalStateException.class,
                () -> debogueur.evaluerAuSurvol(fichier, 4, 7, "nom"),
                "ligne hors pile courante : survol refuse");
    }

    /** Les valeurs en ligne se servent par ligne, pour le rendu en marge. */
    @Test
    void valeursEnLigneParLigne() {
        FakeDebugger debogueur = new FakeDebugger();
        CheminDoc fichier = CheminDoc.de("/src/Main.java");
        debogueur.simulerPause(fichier, 18, List.of());
        debogueur.enregistrerValeurEnLigne(18, "compteur", "3");

        List<ValeurEnLigne> valeurs = debogueur.valeursEnLigne(18);
        assertEquals(1, valeurs.size());
        assertEquals("compteur", valeurs.get(0).expression());
        assertEquals("3", valeurs.get(0).valeur());
        assertEquals(0, debogueur.valeursEnLigne(4).size());
    }

    /** La pause d'après retire les valeurs d'avant : chaque pause
     *  recommence à zéro, l'hôte y re-sert ses valeurs fraîches. */
    @Test
    void laPauseRecommenceLesValeursAZero() {
        FakeDebugger debogueur = new FakeDebugger();
        CheminDoc fichier = CheminDoc.de("/src/Main.java");
        debogueur.simulerPause(fichier, 18, List.of());
        debogueur.enregistrerValeurEnLigne(18, "compteur", "3");
        assertEquals(1, debogueur.valeursEnLigne(18).size());

        debogueur.simulerPause(fichier, 22, List.of());
        assertEquals(0, debogueur.valeursEnLigne(18).size(),
                "les valeurs de la pause d'avant se retirent");
        assertEquals(0, debogueur.valeursEnLigne(22).size(),
                "la pause fraîche ne sert rien tant que l'hôte n'y pose");
    }
}
