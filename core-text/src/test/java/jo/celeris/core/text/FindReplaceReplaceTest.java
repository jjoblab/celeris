package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Passe de remplacement du moteur {@link FindReplace} — spécification du
 * comportement du contrôleur du dépôt de départ (registre : l'ancien code
 * sert de spécification et d'oracle de test) : segments d'origine +
 * remplacements développés (groupes {@code $n} en mode regex), motif
 * invalide ou trop coûteux → AUCUN remplacement (le texte n'est jamais
 * muté à moitié). Adaptation consignée : les correspondances de
 * REMPLACEMENT ne se recouvrent jamais — la collecte peut recouvrir
 * (surbrillance), la passe avance au-delà de chaque occurrence remplacée.
 */
class FindReplaceReplaceTest {

    /** Moteur à horloge figée : aucun budget ne peut être dépassé. */
    private final FindReplace moteur = FindReplace.of(() -> 0L);

    @Test
    void replaceAll_plain_literalSegments() {
        FindReplace.ReplaceResult r = moteur.replaceAll("foo bar foo", "foo", "X",
                new FindOptions());
        assertEquals("X bar X", r.text());
        assertEquals(2, r.count());
        assertEquals(List.of(new TextEdit(0, "foo", "X"), new TextEdit(8, "foo", "X")),
                r.edits());
        assertFalse(r.tooCostly());
    }

    /** Le texte retiré de chaque édition porte la casse D'ORIGINE. */
    @Test
    void replaceAll_caseInsensitive_keepsOriginalCasePerMatch() {
        FindReplace.ReplaceResult r = moteur.replaceAll("Foo foo FOO", "foo", "x",
                new FindOptions().withCaseSensitive(false));
        assertEquals("x x x", r.text());
        assertEquals(3, r.count());
        assertEquals(List.of(
                new TextEdit(0, "Foo", "x"),
                new TextEdit(4, "foo", "x"),
                new TextEdit(8, "FOO", "x")), r.edits());
    }

    @Test
    void replaceAll_wholeWord_onlyWholeOccurrences() {
        FindReplace.ReplaceResult r = moteur.replaceAll(
                "hello helloworld hello", "hello", "bye",
                new FindOptions().withWholeWord(true));
        assertEquals("bye helloworld bye", r.text());
        assertEquals(2, r.count());
        assertEquals(List.of(new TextEdit(0, "hello", "bye"),
                new TextEdit(17, "hello", "bye")), r.edits());
    }

    /**
     * Le remplacement ne recouvre jamais : « aaa » dans « aaaaa » est
     * remplacé UNE fois (l'occurrence de gauche), la recherche de la
     * prochaine candidate repart au-delà du remplacement.
     */
    @Test
    void replaceAll_plainMatchesNeverOverlap() {
        FindReplace.ReplaceResult r = moteur.replaceAll("aaaaa", "aaa", "X",
                new FindOptions());
        assertEquals("Xaa", r.text());
        assertEquals(1, r.count());
        assertEquals(List.of(new TextEdit(0, "aaa", "X")), r.edits());
    }

    /** Casse insensible exotique (minuscule qui change la longueur) : repli régional. */
    @Test
    void replaceAll_caseInsensitiveLengthChangingLowercase() {
        FindReplace.ReplaceResult r = moteur.replaceAll("\u0130x\u0130x", "x", "Y",
                new FindOptions().withCaseSensitive(false));
        assertEquals("\u0130Y\u0130Y", r.text());
        assertEquals(2, r.count());
        assertEquals(List.of(new TextEdit(1, "x", "Y"),
                new TextEdit(3, "x", "Y")), r.edits());
    }

    @Test
    void replaceAll_regex_expandsGroups() {
        FindReplace.ReplaceResult r = moteur.replaceAll("abc 123 def 456", "\\d+", "<$0>",
                new FindOptions().withRegex(true));
        assertEquals("abc <123> def <456>", r.text());
        assertEquals(2, r.count());
        assertEquals(new TextEdit(4, "123", "<123>"), r.edits().get(0));
        assertEquals(new TextEdit(12, "456", "<456>"), r.edits().get(1));
    }

    /** Groupes numérotés réordonnés dans le remplacement. */
    @Test
    void replaceAll_regex_reordersGroups() {
        FindReplace.ReplaceResult r = moteur.replaceAll("12 34", "(\\d)(\\d)", "$2$1",
                new FindOptions().withRegex(true));
        assertEquals("21 43", r.text());
        assertEquals(List.of(new TextEdit(0, "12", "21"),
                new TextEdit(3, "34", "43")), r.edits());
    }

    /** Le dollar échappé reste littéral dans le remplacement. */
    @Test
    void replaceAll_regexEscapedDollarStaysLiteral() {
        FindReplace.ReplaceResult r = moteur.replaceAll("5", "\\d", "\\$0",
                new FindOptions().withRegex(true));
        assertEquals("$0", r.text());
        assertEquals(1, r.count());
    }

    /** Motif invalide : aucun remplacement, texte intact (parité du moteur de recherche). */
    @Test
    void replaceAll_invalidPattern_noReplacement() {
        FindReplace.ReplaceResult r = moteur.replaceAll("hello", "[invalid", "X",
                new FindOptions().withRegex(true));
        assertEquals(0, r.count());
        assertEquals("hello", r.text());
        assertFalse(r.tooCostly());
    }

    /** Référence de groupe orpheline ($2 sans groupe 2) : aucun remplacement. */
    @Test
    void replaceAll_orphanGroupReference_noReplacement() {
        FindReplace.ReplaceResult r = moteur.replaceAll("abc", "(a)", "$2",
                new FindOptions().withRegex(true));
        assertEquals(0, r.count());
        assertEquals("abc", r.text());
        assertFalse(r.tooCostly());
    }

    /**
     * Motif trop coûteux : AUCUN remplacement — le document n'est jamais
     * muté à moitié (comportement du contrôleur du dépôt de départ).
     * Budget expiré sur horloge scriptée (déterministe).
     */
    @Test
    void replaceAll_tooCostly_neverMutatesHalfway() {
        long[] appels = {0L};
        FindReplace moteurScripte = FindReplace.of(() -> appels[0]++ < 1 ? 0L : 10_000L);
        String texte = "a".repeat(2_000) + "bc";
        FindReplace.ReplaceResult r = moteurScripte.replaceAll(texte, "(a+)+d", "X",
                new FindOptions().withRegex(true));
        assertTrue(r.tooCostly());
        assertEquals(0, r.count());
        assertEquals(texte, r.text(), "le texte du résultat est le texte d'origine");
        assertTrue(r.edits().isEmpty());
    }

    @Test
    void replaceAll_emptyQuery_none() {
        FindReplace.ReplaceResult r = moteur.replaceAll("hello", "", "X", new FindOptions());
        assertEquals(0, r.count());
        assertEquals("hello", r.text());
    }

    @Test
    void replaceAll_noMatch_none() {
        FindReplace.ReplaceResult r = moteur.replaceAll("hello world", "xyz", "X",
                new FindOptions());
        assertEquals(0, r.count());
        assertEquals("hello world", r.text());
    }

    /**
     * Correspondances de largeur nulle avec remplacement non vide : une
     * insertion par position du moteur regex (sémantique du matcher).
     */
    @Test
    void replaceAll_zeroWidthMatches_insertAtEachPosition() {
        FindReplace.ReplaceResult r = moteur.replaceAll("bbb", "a*", "X",
                new FindOptions().withRegex(true));
        assertEquals("XbXbXbX", r.text());
        assertEquals(4, r.count());
        assertEquals(List.of(
                new TextEdit(0, "", "X"),
                new TextEdit(1, "", "X"),
                new TextEdit(2, "", "X"),
                new TextEdit(3, "", "X")), r.edits());
    }

    /**
     * Correspondances de largeur nulle avec remplacement vide : rien à
     * éditer (une édition sans effet n'a rien à faire sur la voie de
     * mutation) — le texte est inchangé.
     */
    @Test
    void replaceAll_zeroWidthEmptyReplacement_skipsNoOpEdits() {
        FindReplace.ReplaceResult r = moteur.replaceAll("bbb", "a*", "",
                new FindOptions().withRegex(true));
        assertEquals(0, r.count());
        assertEquals("bbb", r.text());
    }

    /** Le remplacement contenant la requête n'est jamais rescanné. */
    @Test
    void replaceAll_replacementIsNotRescanned() {
        FindReplace.ReplaceResult r = moteur.replaceAll("ab", "a", "aa",
                new FindOptions());
        assertEquals("aab", r.text());
        assertEquals(1, r.count());
    }

    /** Contrat de l'enregistrement {@link FindReplace.ReplaceResult}. */
    @Test
    void replaceResult_recordContract() {
        FindReplace.ReplaceResult a =
                new FindReplace.ReplaceResult("txt", List.of(new TextEdit(0, "a", "b")),
                        false, false);
        assertEquals(a, new FindReplace.ReplaceResult("txt",
                List.of(new TextEdit(0, "a", "b")), false, false));
        assertEquals(a.hashCode(), new FindReplace.ReplaceResult("txt",
                List.of(new TextEdit(0, "a", "b")), false, false).hashCode());
        assertNotEquals(a, new FindReplace.ReplaceResult("txt", List.of(), false, false));
        assertNotEquals(a, new FindReplace.ReplaceResult("autre",
                List.of(new TextEdit(0, "a", "b")), false, false));
        assertNotEquals(a, new FindReplace.ReplaceResult("txt",
                List.of(new TextEdit(0, "a", "b")), true, false));
        assertNotEquals(a, new FindReplace.ReplaceResult("txt",
                List.of(new TextEdit(0, "a", "b")), false, true));
        assertEquals(1, a.count());
        assertEquals("txt", a.text());
        assertFalse(a.tooCostly());
        assertFalse(a.truncated());
        assertTrue(a.toString().contains("txt"), a.toString());
    }

    /**
     * La passe est plafonnée (miroir du plafond de collecte) : les éditions
     * s'arrêtent au plafond, l'état tronqué est exposé et le texte garde la
     * queue d'origine — éditions et texte restent cohérents.
     */
    @Test
    void replaceAll_capAtMaxMatches_truncated() {
        String texte = "aa ".repeat(FindReplace.MAX_MATCHES + 2);
        FindReplace.ReplaceResult r = moteur.replaceAll(texte, "aa", "X", new FindOptions());
        assertEquals(FindReplace.MAX_MATCHES, r.count());
        assertTrue(r.truncated(), "l'état tronqué est exposé");
        assertFalse(r.tooCostly());
        assertEquals("X ".repeat(FindReplace.MAX_MATCHES) + "aa aa ", r.text(),
                "les 10 000 premières occurrences remplacées, queue d'origine");
    }

    /** Le plafond borne aussi la passe regex. */
    @Test
    void replaceAll_regexCapAtMaxMatches_truncated() {
        String texte = "a ".repeat(FindReplace.MAX_MATCHES + 2);
        FindReplace.ReplaceResult r = moteur.replaceAll(texte, "a", "X",
                new FindOptions().withRegex(true));
        assertEquals(FindReplace.MAX_MATCHES, r.count());
        assertTrue(r.truncated(), "le plafond borne aussi la passe regex");
    }

    /** Exactement le plafond d'occurrences : tout est remplacé, PAS tronqué. */
    @Test
    void replaceAll_exactlyMaxMatches_notTruncated() {
        String texte = "aa ".repeat(FindReplace.MAX_MATCHES);
        FindReplace.ReplaceResult r = moteur.replaceAll(texte, "aa", "X", new FindOptions());
        assertEquals(FindReplace.MAX_MATCHES, r.count());
        assertFalse(r.truncated(), "toutes les occurrences sont remplacées");
    }
}
