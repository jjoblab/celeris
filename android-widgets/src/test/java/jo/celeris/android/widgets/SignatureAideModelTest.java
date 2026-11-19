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
        SignatureAideModel model = SignatureAideModel.of(
                "ajouter(int a, int b)", List.of("a", "b"), 1);
        assertEquals("ajouter(int a, int b)", model.label());
        assertEquals(1, model.parametreCourant());
        SignatureAideModel.Plage range = model.plageParametreCourant();
        assertEquals(19, range.start());
        assertEquals(20, range.end());
        assertTrue(range.start() >= 0 && range.end() <= model.label().length()
                && range.start() <= range.end(), "la plage reste dans le libelle");
        // le texte souligné est le paramètre lui-même
        assertEquals("b", model.label().substring(range.start(), range.end()));
    }

    /** Les doublons : la recherche est séquentielle, le second « a » gagne. */
    @Test
    void lesDoublonsCherchesDansLOrdre() {
        SignatureAideModel model = SignatureAideModel.of(
                "regler(int a, int a)", List.of("a", "a"), 1);
        SignatureAideModel.Plage range = model.plageParametreCourant();
        assertEquals(18, range.start());
        assertEquals("a", model.label().substring(range.start(), range.end()));
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
        SignatureAideModel model = SignatureAideModel.of(
                "ajouter(premier, second)", List.of("a", "b"), 0);
        assertNull(model.plageParametreCourant(), "aucune plage inventee");
    }

    // ------------------------------------------------------- riche (C7)

    private static SignatureAideModel troisSurcharges() {
        return SignatureAideModel.of(List.of(
                new SignatureAideModel.Entry("ajouter(int a, int b)",
                        List.of("a", "b"), "Ajoute deux entiers."),
                new SignatureAideModel.Entry("ajouter(int a, int b, int c)",
                        List.of("a", "b", "c"), "Ajoute trois entiers."),
                new SignatureAideModel.Entry("ajouter(String a, String b)",
                        List.of("a", "b"), "Concatène deux chaînes.")),
                1, 2);
    }

    /** Le stepper : la suivante avance, la précédente recule, le cycle revient. */
    @Test
    void leStepperCycle() {
        SignatureAideModel model = troisSurcharges();
        assertEquals(3, model.nombreSignatures());
        assertEquals(1, model.signatureActive());
        assertEquals("ajouter(int a, int b, int c)", model.label());
        assertEquals("ajouter(String a, String b)",
                model.suivante().label());
        // la dernière rend la première — le cycle, jamais bloqué
        assertEquals("ajouter(int a, int b)",
                model.suivante().suivante().label());
        // depuis la première, la précédente rend la dernière
        assertEquals("ajouter(String a, String b)",
                model.suivante().suivante().precedente().label());
    }

    /** La fenêtre ±1 : l'active et ses voisines d'un cran, moins aux bords. */
    @Test
    void laFenetreBorneeAUnCran() {
        SignatureAideModel model = troisSurcharges();
        List<SignatureAideModel.Entry> window = model.window();
        assertEquals(3, window.size());
        assertEquals("ajouter(int a, int b)", window.get(0).label());
        assertEquals(model.label(), window.get(1).label());
        assertEquals("ajouter(String a, String b)", window.get(2).label());
        // au bord bas : la fenêtre rétrécit
        assertEquals(2, model.suivante().window().size());
        // au bord haut : la fenêtre rétrécit aussi
        assertEquals(2, model.precedente().precedente().window().size());
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
        SignatureAideModel model = troisSurcharges();
        // trois paramètres à l'active : l'index 2 tient
        assertEquals(2, model.parametreCourant());
        // deux paramètres à la suivante : l'index retombe au dernier
        assertEquals(1, model.suivante().parametreCourant());
        // la plage du paramètre courant se lit sur la NOUVELLE active
        assertEquals("b", model.suivante().label()
                .substring(model.suivante().plageParametreCourant().start(),
                        model.suivante().plageParametreCourant().end()));
    }

    /** Sans entrée, le modèle refuse d'exception ; l'active négative retombe à la première. */
    @Test
    void sansEntreeRefuseActiveNegativeRetombe() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SignatureAideModel.of(List.of(), 0, 0));
        SignatureAideModel premiere = SignatureAideModel.of(List.of(
                new SignatureAideModel.Entry("a()", List.of(), null),
                new SignatureAideModel.Entry("b()", List.of(), null)),
                -3, 0);
        assertEquals(0, premiere.signatureActive());
        SignatureAideModel auDela = SignatureAideModel.of(List.of(
                new SignatureAideModel.Entry("a()", List.of(), null)),
                9, 0);
        assertEquals(0, auDela.signatureActive());
    }
}
