package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de la correspondance rechercher/remplacer — portage des tests du
 * dépôt de départ (registre de portage : ces tests sont l'oracle du moteur).
 *
 * <p>Le moteur est à horloge injectée (ADR-007) : l'horloge figée à zéro
 * laisse tous les budgets ouverts — aucune recherche de ce fichier ne peut
 * être interrompue par le garde-fou.</p>
 */
class FindReplaceTest {

    /** Moteur à horloge figée : aucun budget ne peut être dépassé. */
    private final FindReplace moteur = FindReplace.of(() -> 0L);

    @Test
    void findMatches_caseSensitive() {
        List<Match> m = moteur.findMatches("Hello hello HELLO", "hello",
            new FindOptions(true, false, false));
        assertEquals(1, m.size());
        assertEquals(6, m.get(0).start());
        assertEquals(11, m.get(0).end());
    }

    @Test
    void findMatches_caseInsensitive() {
        List<Match> m = moteur.findMatches("Hello hello HELLO", "hello",
            new FindOptions(false, false, false));
        assertEquals(3, m.size());
    }

    @Test
    void findMatches_wholeWord() {
        List<Match> m = moteur.findMatches("hello helloworld hello", "hello",
            new FindOptions(true, true, false));
        assertEquals(2, m.size());
        assertEquals(0, m.get(0).start());
        assertEquals(5, m.get(0).end());
        // "hello helloworld hello" — le second "hello" commence à 17
        assertEquals(17, m.get(1).start());
    }

    @Test
    void findMatches_regex() {
        List<Match> m = moteur.findMatches("abc 123 def 456", "\\d+",
            new FindOptions(false, false, true));
        assertEquals(2, m.size());
        assertEquals(4, m.get(0).start());
        assertEquals(7, m.get(0).end());
    }

    @Test
    void findMatches_regex_invalid_returnsEmpty() {
        List<Match> m = moteur.findMatches("hello", "[invalid",
            new FindOptions(false, false, true));
        assertTrue(m.isEmpty());
    }

    @Test
    void findMatches_emptyQuery() {
        List<Match> m = moteur.findMatches("hello", "", new FindOptions());
        assertTrue(m.isEmpty());
    }

    @Test
    void findMatches_noMatch() {
        List<Match> m = moteur.findMatches("hello world", "xyz", new FindOptions());
        assertTrue(m.isEmpty());
    }

    @Test
    void findMatches_multipleOverlapping() {
        List<Match> m = moteur.findMatches("aaaaa", "aaa",
            new FindOptions(true, false, false));
        // La COLLECTE tolère le recouvrement (surbrillance) : elle avance
        // d'un caractère après chaque correspondance trouvée.
        assertTrue(m.size() >= 2, "Expected at least 2 matches, got " + m.size());
    }

    @Test
    void matchIndexFrom_wraps() {
        List<Match> m = moteur.findMatches("a b a b", "a",
            new FindOptions(true, false, false));
        assertEquals(2, m.size());
        // Depuis un caret après la dernière correspondance → boucle sur 0
        int idx = FindReplace.matchIndexFrom(m, 10);
        assertEquals(0, idx);
    }

    @Test
    void matchIndexFrom_exact() {
        List<Match> m = moteur.findMatches("a b a b", "a",
            new FindOptions(true, false, false));
        int idx = FindReplace.matchIndexFrom(m, 0);
        assertEquals(0, idx);
    }

    @Test
    void matchIndexFrom_between() {
        List<Match> m = moteur.findMatches("a b a b", "a",
            new FindOptions(true, false, false));
        // Caret à 2 (entre le premier 'a' à 0 et le second 'a' à 4)
        int idx = FindReplace.matchIndexFrom(m, 2);
        assertEquals(1, idx);
    }

    @Test
    void matchIndexFrom_empty_returnsNeg1() {
        int idx = FindReplace.matchIndexFrom(List.of(), 0);
        assertEquals(-1, idx);
    }

    /**
     * Casse insensible exotique : U+0130 (İ) se minuscule en DEUX
     * caractères — la voie rapide par minuscules complètes donnerait des
     * indices hors du texte original. Le repli par comparaison régionale
     * garde les indices du texte original (correctif porté, consigné au
     * registre de portage).
     */
    @Test
    void findMatches_caseInsensitiveLengthChangingLowercase_keepsOriginalIndices() {
        List<Match> m = moteur.findMatches("\u0130x\u0130x", "x",
            new FindOptions(false, false, false));
        assertEquals(2, m.size());
        assertEquals(1, m.get(0).start());
        assertEquals(2, m.get(0).end());
        assertEquals(3, m.get(1).start());
        assertEquals(4, m.get(1).end());
    }

    /** La requête elle-même peut changer de longueur à la minuscule. */
    @Test
    void findMatches_caseInsensitiveLowercasedQuery_fallsBackToRegion() {
        List<Match> m = moteur.findMatches("\u0130i", "i",
            new FindOptions(false, false, false));
        assertEquals(2, m.size());
        assertEquals(0, m.get(0).start());
        assertEquals(1, m.get(0).end());
        assertEquals(1, m.get(1).start());
        assertEquals(2, m.get(1).end());
    }

    /** La casse insensible ordinaire reste sur la voie rapide (indices intacts). */
    @Test
    void findMatches_caseInsensitiveFastPath_plainText() {
        List<Match> m = moteur.findMatches("Un Deux un", "un",
            new FindOptions(false, false, false));
        assertEquals(2, m.size());
        assertEquals(0, m.get(0).start());
        assertEquals(8, m.get(1).start());
    }

    /** Correspondance collée à la fin du texte : pas de troncature (fin de boucle par condition). */
    @Test
    void findMatches_plainMatchAtTextEnd_notTruncated() {
        FindReplace.SearchResult r = moteur.find("xxhello", "hello", new FindOptions());
        assertEquals(1, r.matches().size());
        assertEquals(2, r.matches().get(0).start());
        assertFalse(r.truncated(), "la collecte s'est épuisée, pas tronquée");
        assertFalse(r.tooCostly());
    }

    /** Mot entier : plus aucune candidate après la dernière correspondance, pas de troncature. */
    @Test
    void findMatches_wholeWordNoMoreCandidates_notTruncated() {
        FindReplace.SearchResult r = moteur.find("hello xy", "hello",
                new FindOptions().withWholeWord(true));
        assertEquals(1, r.matches().size());
        assertEquals(0, r.matches().get(0).start());
        assertFalse(r.truncated(), "la collecte s'est épuisée, pas tronquée");
    }

    /** Mot entier collé à la fin du texte : pas de troncature (fin de boucle par condition). */
    @Test
    void findMatches_wholeWordMatchAtTextEnd_notTruncated() {
        FindReplace.SearchResult r = moteur.find("aa hello", "hello",
                new FindOptions().withWholeWord(true));
        assertEquals(1, r.matches().size());
        assertEquals(3, r.matches().get(0).start());
        assertFalse(r.truncated(), "la collecte s'est épuisée, pas tronquée");
    }

    /** Requête exotique (minuscule en deux caractères) sur texte ordinaire : repli régional. */
    @Test
    void findMatches_caseInsensitiveExoticQuery_matchesOriginalChar() {
        List<Match> m = moteur.findMatches("i", "\u0130",
            new FindOptions(false, false, false));
        assertEquals(1, m.size());
        assertEquals(0, m.get(0).start());
        assertEquals(1, m.get(0).end());
    }

    /** Requête et texte exotiques : la correspondance garde la longueur de la requête ORIGINALE. */
    @Test
    void findMatches_caseInsensitiveExoticBoth_keepsOriginalQueryLength() {
        List<Match> m = moteur.findMatches("\u0130", "\u0130",
            new FindOptions(false, false, false));
        assertEquals(1, m.size());
        assertEquals(0, m.get(0).start());
        assertEquals(1, m.get(0).end());
    }

    /** Repli régional sans correspondance : liste vide (fin de boucle du repli). */
    @Test
    void findMatches_caseInsensitiveFallbackNoMatch() {
        List<Match> m = moteur.findMatches("\u0130x", "y",
            new FindOptions(false, false, false));
        assertTrue(m.isEmpty());
    }

    /** Contrat de l'enregistrement {@link Match} : égalité, hachage, texte. */
    @Test
    void match_recordContract() {
        Match a = new Match(4, 7);
        assertEquals(a, new Match(4, 7));
        assertEquals(a.hashCode(), new Match(4, 7).hashCode());
        assertNotEquals(a, new Match(4, 8));
        assertNotEquals(a, new Match(5, 7));
        assertNotEquals(a, null);
        assertNotEquals(a, "Match(4, 7)");
        assertEquals(3, a.length());
        assertTrue(a.toString().contains("4"), a.toString());
        assertTrue(a.toString().contains("7"), a.toString());
    }

    /** Contrat de l'enregistrement {@link FindOptions} : fabriques et égalité. */
    @Test
    void findOptions_witherAndRecordContract() {
        FindOptions defauts = new FindOptions();
        assertFalse(defauts.caseSensitive());
        assertFalse(defauts.wholeWord());
        assertFalse(defauts.regex());
        assertEquals(new FindOptions(true, false, true),
                defauts.withCaseSensitive(true).withRegex(true));
        assertEquals(new FindOptions(false, true, false), defauts.withWholeWord(true));
        assertEquals(new FindOptions(true, false, false),
                defauts.withCaseSensitive(true).withRegex(false));
        assertEquals(defauts, defauts.withRegex(false));
        assertEquals(defauts, new FindOptions());
        assertEquals(defauts.hashCode(), new FindOptions().hashCode());
        assertNotEquals(defauts, defauts.withRegex(true));
        assertTrue(defauts.toString().contains("caseSensitive"), defauts.toString());
    }

    /** Contrat de l'enregistrement {@link FindReplace.SearchResult}. */
    @Test
    void searchResult_recordContract() {
        FindReplace.SearchResult a =
                new FindReplace.SearchResult(List.of(new Match(0, 1)), true, false);
        assertEquals(a, new FindReplace.SearchResult(List.of(new Match(0, 1)), true, false));
        assertEquals(a.hashCode(),
                new FindReplace.SearchResult(List.of(new Match(0, 1)), true, false).hashCode());
        assertNotEquals(a, new FindReplace.SearchResult(List.of(), true, false));
        assertNotEquals(a, new FindReplace.SearchResult(List.of(new Match(0, 1)), false, false));
        assertNotEquals(a, new FindReplace.SearchResult(List.of(new Match(0, 1)), true, true));
        assertEquals(1, a.matches().size());
        assertNotNull(a.matches().get(0));
        assertTrue(a.truncated());
        assertFalse(a.tooCostly());
        assertTrue(a.toString().contains("truncated"), a.toString());
    }
}
