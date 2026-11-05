package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * L'historique de navigation (B-14, M5) : la pile bornée des positions
 * visitées, le retour et l'avance, la visite qui coupe l'avance, la
 * fermeture de fichier qui vide l'historique — les positions d'un
 * fichier fermé ne se suivent plus.
 */
class NavigationHistoryTest {

    /** La pile est bornée : la plus ancienne position tombe. */
    @Test
    void pileBornee() {
        NavigationHistory historique = new NavigationHistory(3);
        historique.visit(10);
        historique.visit(20);
        historique.visit(30);
        historique.visit(40);
        assertEquals(3, historique.profondeur());
        // la plus ancienne (10) est tombée : retour → 30, 20, puis plus rien
        assertEquals(30, historique.back());
        assertEquals(20, historique.back());
        assertFalse(historique.canBack());
    }

    /** Le retour et l'avance traversent la pile. */
    @Test
    void retourEtAvance() {
        NavigationHistory historique = new NavigationHistory(10);
        historique.visit(1);
        historique.visit(2);
        historique.visit(3);
        assertEquals(2, historique.back());
        assertEquals(1, historique.back());
        assertTrue(historique.canForward());
        assertEquals(2, historique.forward());
        assertEquals(3, historique.forward());
        assertFalse(historique.canForward());
        assertTrue(historique.canBack());
    }

    /** La visite après un retour coupe l'avance. */
    @Test
    void visiteApresRetourCoupeLAvance() {
        NavigationHistory historique = new NavigationHistory(10);
        historique.visit(1);
        historique.visit(2);
        historique.back();
        assertTrue(historique.canForward());
        historique.visit(9);
        assertFalse(historique.canForward());
        assertEquals(1, historique.back());
        assertEquals(9, historique.forward());
    }

    /** La visite répétée de la même position ne l'empile pas deux fois. */
    @Test
    void visiteRepeteeNeEmpilePas() {
        NavigationHistory historique = new NavigationHistory(10);
        historique.visit(5);
        historique.visit(5);
        assertEquals(1, historique.profondeur());
    }

    /** La fermeture de fichier vide l'historique (B-14). */
    @Test
    void fermetureDeFichierVide() {
        NavigationHistory historique = new NavigationHistory(10);
        historique.visit(1);
        historique.visit(2);
        historique.fichierFerme();
        assertFalse(historique.canBack());
        assertFalse(historique.canForward());
        assertEquals(0, historique.profondeur());
    }

    /** Une pile vide ne rend rien, sans lever. */
    @Test
    void pileVideNeRendRien() {
        NavigationHistory historique = new NavigationHistory(10);
        assertNull(historique.back());
        assertNull(historique.forward());
    }
}
