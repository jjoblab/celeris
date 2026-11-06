package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le modèle d'aide aux paramètres (B-07) : la signature active montre le
 * paramètre courant SOULIGNÉ — la plage de caractères du paramètre dans
 * le libellé, cherchée dans l'ordre (les doublons tombent sur le bon) —
 * le placement au-dessus du curseur vit chez PopupPlacer (D-10, livré
 * M5). Pur et testable sans SDK : la popup le lit, le peintre souligne.
 */
class SignatureAideModelTest {

    /** Le paramètre courant : sa plage dans le libellé, dans les bornes. */
    @Test
    void laPlageDuParametreCourant() {
        SignatureAideModel modele = SignatureAideModel.of(
                "ajouter(int a, int b)", List.of("a", "b"), 1);
        assertEquals("ajouter(int a, int b)", modele.libelle());
        assertEquals(1, modele.parametreCourant());
        SignatureAideModel.Plage plage = modele.plageParametreCourant();
        assertEquals(19, plage.debut());
        assertEquals(20, plage.fin());
        assertTrue(plage.debut() >= 0 && plage.fin() <= modele.libelle().length()
                && plage.debut() <= plage.fin(), "la plage reste dans le libelle");
        // le texte souligné est le paramètre lui-même
        assertEquals("b", modele.libelle().substring(plage.debut(), plage.fin()));
    }

    /** Les doublons : la recherche est séquentielle, le second « a » gagne. */
    @Test
    void lesDoublonsCherchesDansLOrdre() {
        SignatureAideModel modele = SignatureAideModel.of(
                "regler(int a, int a)", List.of("a", "a"), 1);
        SignatureAideModel.Plage plage = modele.plageParametreCourant();
        assertEquals(18, plage.debut());
        assertEquals("a", modele.libelle().substring(plage.debut(), plage.fin()));
    }

    /** Le paramètre courant BORNÉ : au-delà de l'arité, le dernier ; négatif, aucun. */
    @Test
    void leParametreCourantBorne() {
        SignatureAideModel genereux = SignatureAideModel.of(
                "ajouter(int a, int b)", List.of("a", "b"), 7);
        assertEquals(1, genereux.parametreCourant(), "borne au dernier");
        SignatureAideModel absent = SignatureAideModel.of(
                "ajouter(int a, int b)", List.of("a", "b"), -1);
        assertEquals(-1, absent.parametreCourant());
        assertNull(absent.plageParametreCourant());
        SignatureAideModel vide = SignatureAideModel.of("fermer()", List.of(), 0);
        assertEquals(-1, vide.parametreCourant());
        assertNull(vide.plageParametreCourant());
    }

    /** Le paramètre introuvable dans le libellé : aucune plage, jamais inventée. */
    @Test
    void leParametreIntrouvable() {
        SignatureAideModel modele = SignatureAideModel.of(
                "ajouter(premier, second)", List.of("a", "b"), 0);
        assertNull(modele.plageParametreCourant(), "aucune plage inventee");
    }
}
