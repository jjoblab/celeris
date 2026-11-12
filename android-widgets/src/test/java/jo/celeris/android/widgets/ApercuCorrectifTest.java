package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * L'aperçu d'un correctif (B-18) : les lignes du document avant et
 * après l'édition proposée se lisent en préfixe — « - » l'enlevée,
 * « + » l'ajoutée — le contexte encadre les blocs changés, le
 * hors-bloc ne se montre jamais ; les textes identiques ne se
 * prévisent pas, l'aperçu n'engage à rien.
 */
class ApercuCorrectifTest {

    private static final String AVANT = """
            un
            deux
            trois
            quatre
            cinq
            six
            sept
            huit
            """;

    /** B-18 — le bloc changé se préfixe, le contexte l'encadre. */
    @Test
    void leBlocChangeSePrefixeEtLeContexteEncadre() {
        String apres = AVANT.replace("quatre", "QUATRE!");
        ApercuCorrectif apercu = ApercuCorrectif.de(AVANT, apres);
        assertTrue(apercu.porteChangement(), "l'édition engage des lignes");
        assertEquals(List.of(
                "  deux",
                "  trois",
                "- quatre",
                "+ QUATRE!",
                "  cinq",
                "  six"),
                apercu.rendues(),
                "le contexte par défaut encadre de deux lignes intactes");
    }

    /** B-18 — le hors-bloc ne se montre jamais : les lignes lointaines se taisent. */
    @Test
    void leHorsBlocNeSeMontrePas() {
        String apres = AVANT.replace("quatre", "QUATRE!");
        List<String> rendues = ApercuCorrectif.de(AVANT, apres).rendues();
        assertFalse(rendues.stream().anyMatch(ligne -> ligne.contains("un")),
                "la ligne d'avant-bloc ne se montre pas");
        assertFalse(rendues.stream().anyMatch(ligne -> ligne.contains("huit")),
                "la ligne d'après-bloc ne se montre pas");
        assertFalse(rendues.stream().anyMatch(ligne -> ligne.contains("sept")),
                "la ligne juste hors de portée ne se montre pas");
        assertTrue(rendues.stream().anyMatch(ligne -> ligne.contains("quatre")),
                "la ligne changée se montre, elle");
    }

    /** B-18 — le contexte nul ne montre que les lignes changées. */
    @Test
    void leContexteNulNeMontreQueLesChangees() {
        String apres = AVANT.replace("quatre", "QUATRE!");
        assertEquals(List.of("- quatre", "+ QUATRE!"),
                ApercuCorrectif.de(AVANT, apres, 0).rendues(),
                "sans contexte, seules les lignes changées se montrent");
    }

    /** B-18 — les blocs proches partagent leurs lignes intactes. */
    @Test
    void lesBlocsProchesPartagentLeursLignes() {
        String apres = AVANT.replace("trois", "TROIS!").replace("cinq", "CINQ!");
        assertEquals(List.of(
                "  un",
                "  deux",
                "- trois",
                "+ TROIS!",
                "  quatre",
                "- cinq",
                "+ CINQ!",
                "  six",
                "  sept"),
                ApercuCorrectif.de(AVANT, apres).rendues(),
                "la ligne intacte entre les deux blocs se montre une fois, "
                        + "la portée des deux côtés");
    }

    /** B-18 — l'ajout pur s'ancre : ses lignes se rendent avant celle qui le suit. */
    @Test
    void lajoutPurSAncre() {
        String apres = AVANT.replace("quatre\n", "quatre\nmilieu\n");
        assertEquals(List.of(
                "  trois",
                "  quatre",
                "+ milieu",
                "  cinq",
                "  six"),
                ApercuCorrectif.de(AVANT, apres).rendues(),
                "la ligne ajoutée s'ancre entre les intactes qui l'entourent");
    }

    /** B-18 — l'ajout pur en fin de texte se rend avant la ligne de fin intacte. */
    @Test
    void lajoutPurEnFinSeRendEnDernier() {
        String apres = AVANT.replace("huit\n", "huit\nneuf\n");
        assertEquals(List.of(
                "  sept",
                "  huit",
                "+ neuf",
                "  "),
                ApercuCorrectif.de(AVANT, apres).rendues(),
                "l'ajout de fin se rend avant la ligne vide de fin, intacte elle aussi");
    }

    /** B-18 — la suppression pure se montre en moins, jamais en plus. */
    @Test
    void laSuppressionPureSeMontre() {
        String apres = AVANT.replace("trois\n", "");
        assertEquals(List.of("- trois"),
                ApercuCorrectif.de(AVANT, apres, 0).rendues(),
                "la suppression pure ne montre que l'enlevée");
    }

    /** B-18 — les textes identiques ne se prévisent pas. */
    @Test
    void lesTextesIdentiquesNeSePrevisentPas() {
        ApercuCorrectif apercu = ApercuCorrectif.de(AVANT, AVANT);
        assertTrue(apercu.lignes().isEmpty(), "rien ne change, rien ne se montre");
        assertFalse(apercu.porteChangement());
    }

    /** B-18 — le contexte négatif se refuse d'entrée. */
    @Test
    void leContexteNegatifRefuseDEntree() {
        assertThrows(IllegalArgumentException.class,
                () -> ApercuCorrectif.de(AVANT, AVANT, -1));
    }

    /** B-18 — le préfixe suit le genre : « - », « + », deux blancs. */
    @Test
    void lePrefixeSuitLeGenre() {
        assertEquals("- ", new ApercuCorrectif.Ligne(
                ApercuCorrectif.GenreLigne.ENLEVEE, "x").prefixe());
        assertEquals("+ ", new ApercuCorrectif.Ligne(
                ApercuCorrectif.GenreLigne.AJOUTEE, "x").prefixe());
        assertEquals("  ", new ApercuCorrectif.Ligne(
                ApercuCorrectif.GenreLigne.INTACTE, "x").prefixe());
        assertEquals("+ x", new ApercuCorrectif.Ligne(
                ApercuCorrectif.GenreLigne.AJOUTEE, "x").rendue());
    }
}
