package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.core.syntax.FoldSpan;

import org.junit.jupiter.api.Test;

/**
 * Tests des marques de gouttière (D-02, D-07) : pliable, replié,
 * erreur — les trois états par ligne, servis par les mêmes consommateurs
 * de l'arbre (A-15 : régions de pli, D-02 : lignes en erreur).
 *
 * <p>La marque ERRA l'emporte sur la marque de pli dans la voie du
 * tracé : la priorité erreur &gt; avertissement de la matrice, ici à un
 * seul niveau (le socle ne rapporte que des erreurs).</p>
 */
class GutterMarksTest {

    /** Les en-têtes pliables portent la marque, les autres lignes non. */
    @Test
    void lesEnTetesPliablesPortentLaMarque() {
        FoldModel replis = new FoldModel();
        GutterMarks marques = GutterMarks.of(
                List.of(new FoldSpan(1, 3), new FoldSpan(7, 9)), replis, new int[0]);

        assertTrue(marques.pliable(1));
        assertTrue(marques.pliable(7));
        assertFalse(marques.pliable(0), "une ligne sans région n'est pas pliable");
        assertFalse(marques.pliable(2), "le corps d'une région n'est pas pliable");
        assertFalse(marques.pliable(4));
        assertFalse(marques.pliable(-1), "hors bornes : jamais pliable");
        assertFalse(marques.pliable(100));
    }

    /** Un pli replié se distingue d'une région simplement pliable. */
    @Test
    void lePliReplieSeDistingue() {
        FoldModel replis = new FoldModel();
        replis.fold(1, 3);
        GutterMarks marques = GutterMarks.of(
                List.of(new FoldSpan(1, 3)), replis, new int[0]);

        assertTrue(marques.pliable(1));
        assertTrue(marques.replie(1), "l'en-tête replié porte l'état replié");
        assertFalse(marques.replie(7), "aucune autre ligne n'est repliée");
    }

    /** Les lignes en erreur portent la marque de diagnostic (D-02). */
    @Test
    void lesLignesEnErreurPortentLaMarque() {
        GutterMarks marques = GutterMarks.of(
                List.of(), new FoldModel(), new int[] {2, 5, 5});

        assertTrue(marques.erreur(2));
        assertTrue(marques.erreur(5), "les doublons ne comptent pas double");
        assertFalse(marques.erreur(0));
        assertFalse(marques.erreur(3));
        assertFalse(marques.erreur(-3));
    }

    /** Une ligne peut porter pli ET erreur : les deux états coexistent. */
    @Test
    void pliEtErreurCoexistent() {
        FoldModel replis = new FoldModel();
        replis.fold(2, 4);
        GutterMarks marques = GutterMarks.of(
                List.of(new FoldSpan(2, 4)), replis, new int[] {2});

        assertTrue(marques.pliable(2));
        assertTrue(marques.replie(2));
        assertTrue(marques.erreur(2));
    }

    /** Les marques vides ne marquent rien : la gouttiere reste muette. */
    @Test
    void lesMarquesVidesNeMarquentRien() {
        GutterMarks marques = GutterMarks.VIDE;
        for (int ligne = -1; ligne < 10; ligne++) {
            assertFalse(marques.pliable(ligne));
            assertFalse(marques.replie(ligne));
            assertFalse(marques.erreur(ligne));
        }
    }

    /** Les diagnostics importes du build (C-12) se fondent aux erreurs. */
    @Test
    void lesErreursImporteesSeFondentAuxErreursDeSyntaxe() {
        GutterMarks marques = GutterMarks.of(
                List.of(), new FoldModel(), new int[] {4});

        GutterMarks fondues = marques.avecErreurs(new int[] {4, 9});

        assertTrue(fondues.erreur(4), "la ligne commune se marque une fois");
        assertTrue(fondues.erreur(9), "la ligne importee se marque");
        assertFalse(fondues.erreur(0));
        assertFalse(fondues.erreur(5));
        assertTrue(marques.erreur(4));
        assertFalse(marques.erreur(9), "la source reste intacte");
    }

    /** Les erreurs importees vides ne changent rien : la fusion est gratuite. */
    @Test
    void lesErreursImporteesVidesNeChangentRien() {
        GutterMarks marques = GutterMarks.of(
                List.of(), new FoldModel(), new int[] {4});

        assertSame(marques, marques.avecErreurs(new int[0]));
    }
}
