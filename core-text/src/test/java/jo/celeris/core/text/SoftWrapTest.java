package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests du modèle de retour à la ligne souple pur (mesure injectée).
 *
 * <p>PIEGE-08 : dans le dépôt de départ, le retour à la ligne souple ne
 * coupait pas un mot plus large que l'espace disponible — le mot débordait
 * du panneau. La coupe par caractères avec recherche dichotomique est
 * obligatoire : chaque rangée produite tient dans la largeur offerte, même
 * quand un mot isolé la dépasse.</p>
 */
class SoftWrapTest {

    /** Mesure déterministe : chaque caractère vaut exactement 10 px. */
    private static final class MesureParCaractere implements SoftWrap.Measurer {
        @Override
        public float width(String text) {
            return text.length() * 10f;
        }
    }

    /** Mesure qui compte ses appels (preuve de la dichotomie). */
    private static final class MesureComptee implements SoftWrap.Measurer {
        private int appels;

        @Override
        public float width(String text) {
            appels++;
            return text.length() * 10f;
        }
    }

    /**
     * PIEGE-08 — un mot plus large que la largeur disponible est coupé par
     * caractères.
     *
     * <p>Le mot de 20 caractères ne tient pas dans 100 px : le modèle le
     * coupe en rangées de 10 caractères exactement, sans jamais laisser
     * déborder, et chaque rangée reste dans le budget.</p>
     */
    @Test
    void softWrap_breaksOversizedWordByCharacter() {
        List<String> rangees = SoftWrap.wrap("a".repeat(20), 100f, new MesureParCaractere());
        assertEquals(List.of("a".repeat(10), "a".repeat(10)), rangees);
        for (String rangee : rangees) {
            assertTrue(rangee.length() * 10f <= 100f, "chaque rangée tient dans la largeur");
        }
    }

    @Test
    void oversizedWordSearch_isDichotomic_notPerCharacter() {
        // Un mot de 512 caractères, 51 caractères par rangée : la recherche
        // du point de coupe est dichotomique (une mesure par itération),
        // pas une mesure par caractère — 512 mesures à une par caractère,
        // une dizaine par coupe en dichotomie.
        MesureComptee mesure = new MesureComptee();
        List<String> rangees = SoftWrap.wrap("b".repeat(512), 510f, mesure);
        assertEquals(11, rangees.size());
        for (String rangee : rangees) {
            assertTrue(rangee.length() * 10f <= 510f, "chaque rangée tient dans la largeur");
        }
        assertTrue(mesure.appels < 512,
                "la coupe doit être dichotomique : " + mesure.appels
                        + " mesures pour 512 caractères");
    }

    @Test
    void narrowerThanOneCharacter_stillYieldsOneCharacterPerRow() {
        // Garde-fou porté : même avec une largeur inférieure à un caractère,
        // chaque rangée porte AU MOINS un caractère (jamais de boucle vide).
        List<String> rangees = SoftWrap.wrap("ab", 5f, new MesureParCaractere());
        assertEquals(List.of("a", "b"), rangees);
    }

    @Test
    void zeroWidth_fallsBackToOneCharacterPerRow() {
        List<String> rangees = SoftWrap.wrap("ab", 0f, new MesureParCaractere());
        assertEquals(List.of("a", "b"), rangees);
    }

    @Test
    void wordsWrapAtBoundaries_andKeepNoLeadingSpace() {
        // Mots de 2, 3 et 3 caractères : « un deu tro » = 10 caractères
        // (100 px) tient dans 120 px, « un deu tro qua » (140 px) dépasse —
        // le quatrième mot passe à la ligne suivante, sans espace de tête.
        List<String> rangees = SoftWrap.wrap("un deu tro qua", 120f, new MesureParCaractere());
        assertEquals(List.of("un deu tro", "qua"), rangees);
    }

    @Test
    void explicitNewlines_areRespected() {
        List<String> rangees = SoftWrap.wrap("aa\nbb", 1000f, new MesureParCaractere());
        assertEquals(List.of("aa", "bb"), rangees);
    }

    @Test
    void doubleNewline_yieldsAnEmptyBreathingRow() {
        List<String> rangees = SoftWrap.wrap("aa\n\nbb", 1000f, new MesureParCaractere());
        assertEquals(List.of("aa", "", "bb"), rangees);
    }

    @Test
    void trailingEmptyParagraphs_areDropped() {
        // Un saut de ligne final ne crée pas de rangée vide de queue
        // (compatibilité du découpage par paragraphes du dépôt de départ).
        List<String> rangees = SoftWrap.wrap("aa\n", 1000f, new MesureParCaractere());
        assertEquals(List.of("aa"), rangees);
    }

    @Test
    void whitespaceOnlyParagraph_isKeptAsIs() {
        // Un paragraphe tout blanc ne contient aucun mot : le modèle ne sait
        // pas le couper, il le rend tel quel.
        List<String> rangees = SoftWrap.wrap("   ", 20f, new MesureParCaractere());
        assertEquals(List.of("   "), rangees);
    }

    @Test
    void tailOfABrokenWord_opensTheRowForTheNextWord() {
        // La queue d'un mot coupé ouvre la rangée courante : le mot suivant
        // peut la rejoindre s'il y tient (porté du dépôt de départ — pas de
        // rangée gaspillée d'un seul fragment).
        List<String> rangees = SoftWrap.wrap("aaaaaaaaaa bb", 70f, new MesureParCaractere());
        // Le mot de 10 ne tient pas dans 70 : coupé en 7 + 3 ; « bb » (20 px)
        // rejoint la queue de 3 (30 px) sur la seconde rangée (60 px ≤ 70).
        assertEquals(List.of("aaaaaaa", "aaa bb"), rangees);
    }

    @Test
    void emptyText_yieldsNoRow() {
        assertEquals(List.of(), SoftWrap.wrap("", 100f, new MesureParCaractere()));
    }

    @Test
    void multipleSpaces_collapseToOneSeparator() {
        // Normalisation héritée : au plus une espace entre deux mots conservés.
        List<String> rangees = SoftWrap.wrap("aa   bb", 200f, new MesureParCaractere());
        assertEquals(List.of("aa bb"), rangees);
    }

    @Test
    void singleWordFittingWidth_staysOnOneRow() {
        List<String> rangees = SoftWrap.wrap("abc", 100f, new MesureParCaractere());
        assertEquals(List.of("abc"), rangees);
    }
}
