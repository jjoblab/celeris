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

    // ------------------------------------------------------- riche (C7)

    private static SignatureAideModel troisSurcharges() {
        return SignatureAideModel.of(List.of(
                new SignatureAideModel.Entree("ajouter(int a, int b)",
                        List.of("a", "b"), "Ajoute deux entiers."),
                new SignatureAideModel.Entree("ajouter(int a, int b, int c)",
                        List.of("a", "b", "c"), "Ajoute trois entiers."),
                new SignatureAideModel.Entree("ajouter(String a, String b)",
                        List.of("a", "b"), "Concatène deux chaînes.")),
                1, 2);
    }

    /** Le stepper : la suivante avance, la précédente recule, le cycle revient. */
    @Test
    void leStepperCycle() {
        SignatureAideModel modele = troisSurcharges();
        assertEquals(3, modele.nombreSignatures());
        assertEquals(1, modele.signatureActive());
        assertEquals("ajouter(int a, int b, int c)", modele.libelle());
        assertEquals("ajouter(String a, String b)",
                modele.suivante().libelle());
        // la dernière rend la première — le cycle, jamais bloqué
        assertEquals("ajouter(int a, int b)",
                modele.suivante().suivante().libelle());
        // depuis la première, la précédente rend la dernière
        assertEquals("ajouter(String a, String b)",
                modele.suivante().suivante().precedente().libelle());
    }

    /** La fenêtre ±1 : l'active et ses voisines d'un cran, moins aux bords. */
    @Test
    void laFenetreBorneeAUnCran() {
        SignatureAideModel modele = troisSurcharges();
        List<SignatureAideModel.Entree> fenetre = modele.fenetre();
        assertEquals(3, fenetre.size());
        assertEquals("ajouter(int a, int b)", fenetre.get(0).libelle());
        assertEquals(modele.libelle(), fenetre.get(1).libelle());
        assertEquals("ajouter(String a, String b)", fenetre.get(2).libelle());
        // au bord bas : la fenêtre rétrécit
        assertEquals(2, modele.suivante().fenetre().size());
        // au bord haut : la fenêtre rétrécit aussi
        assertEquals(2, modele.precedente().precedente().fenetre().size());
    }

    /** Le peek : la documentation de la signature active, jamais inventée. */
    @Test
    void lePeekServLaDocumentation() {
        assertEquals("Ajoute trois entiers.", troisSurcharges().peek());
        // une entrée sans documentation ne montre rien
        SignatureAideModel muette = SignatureAideModel.of("fermer()",
                List.of(), -1);
        assertNull(muette.peek());
    }

    /** Le paramètre courant suit l'active du stepper, borné à son arité. */
    @Test
    void leParametreCourantSuitLePas() {
        SignatureAideModel modele = troisSurcharges();
        // trois paramètres à l'active : l'index 2 tient
        assertEquals(2, modele.parametreCourant());
        // deux paramètres à la suivante : l'index retombe au dernier
        assertEquals(1, modele.suivante().parametreCourant());
        // la plage du paramètre courant se lit sur la NOUVELLE active
        assertEquals("b", modele.suivante().libelle()
                .substring(modele.suivante().plageParametreCourant().debut(),
                        modele.suivante().plageParametreCourant().fin()));
    }

    /** Sans entrée, le modèle refuse d'exception ; l'active négative retombe à la première. */
    @Test
    void sansEntreeRefuseActiveNegativeRetombe() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SignatureAideModel.of(List.of(), 0, 0));
        SignatureAideModel premiere = SignatureAideModel.of(List.of(
                new SignatureAideModel.Entree("a()", List.of(), null),
                new SignatureAideModel.Entree("b()", List.of(), null)),
                -3, 0);
        assertEquals(0, premiere.signatureActive());
        SignatureAideModel auDela = SignatureAideModel.of(List.of(
                new SignatureAideModel.Entree("a()", List.of(), null)),
                9, 0);
        assertEquals(0, auDela.signatureActive());
    }
}
