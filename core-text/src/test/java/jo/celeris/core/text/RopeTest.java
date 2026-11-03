package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de la structure de données rope équilibrée et immuable.
 *
 * <p>Portage des tests du dépôt de départ, étendus pour les frontières de
 * découpage (MAX_LEAF exact, fusion d'épine aux bornes) : ces frontières
 * sont les lignes que la mutation doit le plus chercher à casser.</p>
 */
class RopeTest {

    /** Starts globaux des feuilles, en ordre de visite croissant. */
    private static List<Integer> leafStarts(Rope rope) {
        List<Integer> starts = new ArrayList<>();
        rope.visitLeaves(0, rope.length(), (data, start) -> {
            starts.add(start);
            return true;
        });
        return starts;
    }

    // ── Construction ──────────────────────────────────────────────

    @Test
    void emptyRope_hasZeroLength() {
        Rope r = Rope.EMPTY;
        assertEquals(0, r.length());
        assertEquals("", r.toString());
    }

    @Test
    void fromString_shortText_createsLeaf() {
        Rope r = Rope.fromString("hello");
        assertEquals(5, r.length());
        assertEquals("hello", r.toString());
        assertInstanceOf(Rope.Leaf.class, r);
    }

    @Test
    void fromString_longText_createsBranches() {
        String text = "A".repeat(2000);
        Rope r = Rope.fromString(text);
        assertEquals(2000, r.length());
        assertEquals(text, r.toString());
        assertInstanceOf(Rope.Branch.class, r);
    }

    @Test
    void fromString_empty_returnsEmpty() {
        Rope r = Rope.fromString("");
        assertSame(Rope.EMPTY, r);
    }

    @Test
    void fromString_exactMaxLeaf_staysSingleLeaf() {
        // Frontière exacte : MAX_LEAF tient dans UNE feuille.
        Rope r = Rope.fromString("a".repeat(Rope.MAX_LEAF));
        assertInstanceOf(Rope.Leaf.class, r);
        assertEquals(1, leafStarts(r).size());
    }

    @Test
    void fromString_overMaxLeaf_splitsAtExactBoundary() {
        // Une unité de plus : la coupe tombe EXACTEMENT à MAX_LEAF.
        Rope r = Rope.fromString("a".repeat(Rope.MAX_LEAF + 1));
        assertEquals(List.of(0, Rope.MAX_LEAF), leafStarts(r));
        assertEquals(Rope.MAX_LEAF + 1, r.length());
    }

    @Test
    void fromString_doubleMaxLeaf_splitsIntoTwoLeaves() {
        // Exactement 2 × MAX_LEAF : deux feuilles pleines, pas de troisième
        // vide en queue de découpage.
        Rope r = Rope.fromString("a".repeat(2 * Rope.MAX_LEAF));
        assertEquals(List.of(0, Rope.MAX_LEAF), leafStarts(r));
        assertEquals(2 * Rope.MAX_LEAF, r.length());
    }

    // ── charAt ────────────────────────────────────────────────────

    @Test
    void charAt_returnsCorrectChars() {
        Rope r = Rope.fromString("abcdef");
        assertEquals('a', r.charAt(0));
        assertEquals('c', r.charAt(2));
        assertEquals('f', r.charAt(5));
    }

    @Test
    void charAt_throwsOnNegativeIndex() {
        Rope r = Rope.fromString("hello");
        assertThrows(IndexOutOfBoundsException.class, () -> r.charAt(-1));
    }

    @Test
    void charAt_throwsOnOutOfBounds() {
        Rope r = Rope.fromString("hello");
        assertThrows(IndexOutOfBoundsException.class, () -> r.charAt(5));
    }

    @Test
    void charAt_outOfBounds_reportsRopeContract() {
        // Le message distingue le contrôle de bornes de la rope de celui,
        // interne, de String : « Index: …, Length: … ».
        Rope r = Rope.fromString("hello");
        Throwable negatif = assertThrows(IndexOutOfBoundsException.class, () -> r.charAt(-1));
        String messageBas = negatif.getMessage();
        assertTrue(messageBas != null && messageBas.contains("Index:"),
            "message: " + messageBas);
        Throwable haut = assertThrows(IndexOutOfBoundsException.class, () -> r.charAt(5));
        String messageHaut = haut.getMessage();
        assertTrue(messageHaut != null && messageHaut.contains("Index:"),
            "message: " + messageHaut);
    }

    @Test
    void charAt_atLeafBoundaries_isStable() {
        // 1 200 caractères : frontières de feuilles à 512 et 1 024 — la
        // descente doit basculer exactement à ces frontières (la frontière
        // des lettres, elle, est à 600).
        String text = "A".repeat(600) + "B".repeat(600);
        Rope r = Rope.fromString(text);
        assertEquals('A', r.charAt(511));
        assertEquals('A', r.charAt(512));
        assertEquals('A', r.charAt(513));
        assertEquals('A', r.charAt(599));
        assertEquals('B', r.charAt(600));
        assertEquals('B', r.charAt(1023));
        assertEquals('B', r.charAt(1024));
        assertEquals('B', r.charAt(1025));
    }

    @Test
    void charAt_worksAcrossBranches() {
        String text = "A".repeat(600) + "B".repeat(600);
        Rope r = Rope.fromString(text);
        assertInstanceOf(Rope.Branch.class, r);
        assertEquals('A', r.charAt(0));
        assertEquals('A', r.charAt(599));
        assertEquals('B', r.charAt(600));
        assertEquals('B', r.charAt(1199));
    }

    // ── substring / sub ───────────────────────────────────────────

    @Test
    void sub_fullRange_returnsSameContent() {
        Rope r = Rope.fromString("hello world");
        assertEquals("hello world", r.sub(0, 11).toString());
    }

    @Test
    void sub_fullRangeOnBranch_returnsSameInstance() {
        // Une coupe pleine sur une branche ne re-concatène rien : identité.
        Rope r = Rope.fromString("A".repeat(1200));
        assertSame(r, r.sub(0, r.length()));
    }

    @Test
    void substring_partial_returnsCorrectSlice() {
        Rope r = Rope.fromString("hello world");
        assertEquals("world", r.sub(6, 11).toString());
        assertEquals("lo wo", r.sub(3, 8).toString());
    }

    @Test
    void substring_emptyRange_returnsEmpty() {
        Rope r = Rope.fromString("hello");
        Rope sub = r.sub(3, 3);
        assertEquals(0, sub.length());
        // Le singleton vide, pas une feuille vide fraîche.
        assertSame(Rope.EMPTY, sub);
    }

    @Test
    void sub_fullLeafRange_returnsSameInstance() {
        Rope r = Rope.fromString("hello");
        assertSame(r, r.sub(0, 5));
    }

    @Test
    void sub_spanningBranchBoundary_matchesSubstring() {
        // 1 200 caractères : la branche gauche s'arrête à 512 — la coupe
        // [500, 700) traverse la frontière et repasse par concat.
        String text = "A".repeat(600) + "B".repeat(600);
        Rope r = Rope.fromString(text);
        assertEquals(text.substring(500, 700), r.sub(500, 700).toString());
        assertEquals(text.substring(0, 100), r.sub(0, 100).toString());
        assertEquals(text.substring(1100, 1200), r.sub(1100, 1200).toString());
    }

    @Test
    void subSequence_matchesSubstring() {
        Rope r = Rope.fromString("abcdefghij");
        CharSequence cs = r.subSequence(2, 7);
        assertEquals("cdefg", cs.toString());
    }

    // ── replace ───────────────────────────────────────────────────

    @Test
    void replace_insertAtStart() {
        Rope r = Rope.fromString("world");
        Rope result = r.replace(0, 0, "hello ");
        assertEquals("hello world", result.toString());
    }

    @Test
    void replace_insertAtEnd() {
        Rope r = Rope.fromString("hello");
        Rope result = r.replace(5, 5, " world");
        assertEquals("hello world", result.toString());
    }

    @Test
    void replace_deleteRange() {
        Rope r = Rope.fromString("hello cruel world");
        Rope result = r.replace(6, 12, "");
        assertEquals("hello world", result.toString());
    }

    @Test
    void replace_replaceRange() {
        Rope r = Rope.fromString("hello world");
        Rope result = r.replace(6, 11, "there");
        assertEquals("hello there", result.toString());
    }

    @Test
    void replace_singleCharInsert() {
        Rope r = Rope.fromString("hllo");
        Rope result = r.replace(1, 1, "e");
        assertEquals("hello", result.toString());
    }

    @Test
    void replace_noOp_returnsSameInstance() {
        // Position identique et insertion vide : AUCUNE nouvelle rope.
        Rope r = Rope.fromString("hello");
        assertSame(r, r.replace(2, 2, ""));
    }

    @Test
    void replace_invalidRange_throws() {
        Rope r = Rope.fromString("hello");
        assertThrows(IndexOutOfBoundsException.class, () -> r.replace(-1, 2, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> r.replace(4, 6, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> r.replace(4, 2, "x"));
    }

    // ── concat ────────────────────────────────────────────────────

    @Test
    void concat_twoRopes() {
        Rope a = Rope.fromString("hello ");
        Rope b = Rope.fromString("world");
        Rope c = Rope.concat(a, b);
        assertEquals("hello world", c.toString());
    }

    @Test
    void concat_withEmpty() {
        Rope a = Rope.fromString("hello");
        assertSame(a, Rope.concat(a, Rope.EMPTY));
        assertSame(a, Rope.concat(Rope.EMPTY, a));
    }

    @Test
    void concat_smallLeavesMerge() {
        Rope a = Rope.fromString("ab");
        Rope b = Rope.fromString("cd");
        Rope c = Rope.concat(a, b);
        assertEquals("abcd", c.toString());
        assertInstanceOf(Rope.Leaf.class, c);
    }

    @Test
    void concat_leavesAtExactMax_mergeIntoSingleLeaf() {
        // 256 + 256 = 512 = MAX_LEAF exact : fusion quand même.
        Rope c = Rope.concat(
            Rope.fromString("a".repeat(256)), Rope.fromString("b".repeat(256)));
        assertInstanceOf(Rope.Leaf.class, c);
        assertEquals(1, leafStarts(c).size());
        assertEquals(512, c.length());
    }

    @Test
    void concat_overfullLeaves_stayApart() {
        // 300 + 300 = 600 > MAX_LEAF : la branche reste double-feuille.
        Rope c = Rope.concat(
            Rope.fromString("a".repeat(300)), Rope.fromString("b".repeat(300)));
        assertEquals("a".repeat(300) + "b".repeat(300), c.toString());
        assertEquals(List.of(0, 300), leafStarts(c));
    }

    @Test
    void concat_leafIntoLeftEdge_mergesAtExactBoundary() {
        // 212 + 300 = 512 = MAX_LEAF exact : la feuille gauche se replie
        // dans la feuille la plus à gauche de la branche droite.
        Rope right = Rope.concat(
            Rope.fromString("a".repeat(300)), Rope.fromString("b".repeat(300)));
        Rope c = Rope.concat(Rope.fromString("x".repeat(212)), right);
        assertEquals(List.of(0, 512), leafStarts(c));
        assertEquals(512 + 300, c.length());
    }

    @Test
    void concat_leafIntoLeftEdge_overBoundaryStaysApart() {
        // 213 + 300 = 513 > MAX_LEAF : pas de repli, trois feuilles.
        Rope right = Rope.concat(
            Rope.fromString("a".repeat(300)), Rope.fromString("b".repeat(300)));
        Rope c = Rope.concat(Rope.fromString("x".repeat(213)), right);
        assertEquals(List.of(0, 213, 513), leafStarts(c));
    }

    @Test
    void concat_leafIntoRightEdge_mergesAtExactBoundary() {
        // fromString(600) = Branch(512, 88) ; 88 + 424 = 512 exact : la
        // feuille entrante se replie dans le bord droit.
        Rope left = Rope.fromString("a".repeat(600));
        Rope c = Rope.concat(left, Rope.fromString("b".repeat(424)));
        assertEquals(List.of(0, 512), leafStarts(c));
        assertEquals(1024, c.length());
    }

    @Test
    void concat_leafIntoRightEdge_overBoundaryStaysApart() {
        Rope left = Rope.fromString("a".repeat(600));
        Rope c = Rope.concat(left, Rope.fromString("b".repeat(425)));
        assertEquals(List.of(0, 512, 600), leafStarts(c));
        assertEquals(1025, c.length());
    }

    @Test
    void concat_rightmostIsBranch_noMergeButSameContent() {
        // Bord droit déjà ramifié : aucun repli possible, contenu intact.
        Rope left = Rope.fromString("a".repeat(1026));
        Rope c = Rope.concat(left, Rope.fromString("b".repeat(11)));
        assertEquals("a".repeat(1026) + "b".repeat(11), c.toString());
        assertEquals(1037, c.length());
    }

    // ── Équilibrage ─────────────────────────────────────────────

    @Test
    void balance_maintainsInvariant() {
        Rope r = Rope.EMPTY;
        for (int i = 0; i < 1000; i++) {
            r = Rope.concat(r, Rope.fromString(String.valueOf(i)));
        }
        String expected = r.toString();
        assertTrue(expected.startsWith("01234567891011"));
        assertEquals(2890, r.length());
    }

    @Test
    void balance_invariant_afterRebalance() {
        // Construire une rope profondément déséquilibrée
        Rope r = Rope.fromString("x");
        for (int i = 0; i < 200; i++) {
            r = Rope.concat(r, Rope.fromString("y"));
        }
        // Rééquilibrer
        Rope balanced = r.rebalance();
        assertEquals(r.toString(), balanced.toString());
        // L'arbre équilibré doit avoir une profondeur raisonnable
        assertTrue(balanced.depth <= 20, "Depth should be reasonable after rebalance, got " + balanced.depth);
    }

    @Test
    void rebalance_onBalancedRope_returnsSameInstance() {
        // Une rope déjà équilibrée ne se reconstruit PAS (identité).
        Rope r = Rope.fromString("a".repeat(2000));
        assertSame(r, r.rebalance());
    }

    @Test
    void rebalance_unbalancedChain_rebuildsShallowTree() {
        // Chaîne pendue à droite : trente concaténations de feuilles pleines
        // sans fusion possible — la profondeur croît linéairement, la
        // longueur aussi : l'invariant de Fibonacci finit rompu.
        Rope r = Rope.fromString("x");
        for (int i = 0; i < 30; i++) {
            r = Rope.concat(r, Rope.fromString("y".repeat(Rope.MAX_LEAF)));
        }
        Rope balanced = r.rebalance();
        assertNotSame(r, balanced);
        assertEquals(r.toString(), balanced.toString());
        assertEquals(r.length(), balanced.length());
        assertTrue(balanced.depth < r.depth,
            "la reconstruction doit aplatir l'arbre : " + balanced.depth + " vs " + r.depth);
        assertTrue(balanced.depth <= 20, "profondeur raisonnable après rééquilibrage");
    }

    @Test
    void rebalance_afterManyConcats() {
        Rope r = Rope.fromString("start");
        for (int i = 0; i < 100; i++) {
            r = r.replace(r.length(), r.length(), "x");
        }
        assertEquals("start" + "x".repeat(100), r.toString());
        assertEquals(105, r.length());
    }

    // ── Texte volumineux ────────────────────────────────────────

    @Test
    void largeText_operations() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Line ").append(i).append(" of the document.\n");
        }
        String text = sb.toString();
        Rope r = Rope.fromString(text);

        assertEquals(text.length(), r.length());
        assertEquals(text.charAt(0), r.charAt(0));
        assertEquals(text.charAt(text.length() - 1), r.charAt(text.length() - 1));

        int mid = text.length() / 2;
        Rope edited = r.replace(mid, mid + 5, "XXXXX");
        assertEquals(text.length(), edited.length());
        assertEquals('X', edited.charAt(mid));
    }

    // ── Contrat CharSequence ───────────────────────────────────

    @Test
    void implementsCharSequence() {
        Rope r = Rope.fromString("hello");
        CharSequence cs = r;
        assertEquals(5, cs.length());
        assertEquals('h', cs.charAt(0));
        assertEquals("llo", cs.subSequence(2, 5).toString());
    }

    // ── Immutabilité ────────────────────────────────────────────

    @Test
    void replace_doesNotMutateOriginal() {
        Rope r = Rope.fromString("hello");
        Rope modified = r.replace(0, 0, "X");
        assertEquals("hello", r.toString());
        assertEquals("Xhello", modified.toString());
    }

    @Test
    void concat_doesNotMutateOriginals() {
        Rope a = Rope.fromString("hello");
        Rope b = Rope.fromString("world");
        Rope c = Rope.concat(a, b);
        assertEquals("hello", a.toString());
        assertEquals("world", b.toString());
        assertEquals("helloworld", c.toString());
    }
}
