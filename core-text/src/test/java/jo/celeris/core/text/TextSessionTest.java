package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de la session de texte : la {@code Transaction} est la seule voie de
 * mutation (ADR-003) et l'annulation rejoue par cette même voie.
 *
 * <p>PIEGE-07 : dans le dépôt de départ, l'annulation mutait le document en
 * dehors de la voie de transaction unique — annotations, plis et composition
 * IME se désynchronisaient, la garde lecture seule était contournée et les
 * auditeurs d'édition (pont LSP) n'étaient pas notifiés. Ici, le rejeu de
 * l'annulation traverse exactement le même pipeline que toute édition :
 * projection des plages ancrées, notification des auditeurs, garde lecture
 * seule et pile intacte.</p>
 */
class TextSessionTest {

    private static final String DOC = "int a = 1;\nint b = 2;\n";

    /** Auditeur d'édition qui enregistre « début:fin:texte inséré ». */
    private static final class JournalAuditeur implements EditListener {

        private final List<String> appels = new ArrayList<>();

        @Override
        public void onTextEdit(int start, int end, String removed, String inserted) {
            appels.add(start + ":" + end + ":" + inserted);
        }

        List<String> appels() {
            return appels;
        }
    }

    /**
     * PIEGE-07 — le rejeu de l'annulation passe par la voie de transaction
     * unique.
     *
     * <p>L'édition directe décale la plage ancrée (diagnostic) et notifie
     * l'auditeur ; l'annulation doit produire les MÊMES effets observables :
     * la plage ancrée revient à sa position d'origine (le chemin direct du
     * dépôt de départ la laissait décalée) et l'auditeur est notifié de
     * l'édition inverse — le pont LSP ne doit jamais tirer sur une version
     * périmée du document.</p>
     */
    @Test
    void undoReplay_goesThroughTransactionPipeline() {
        TextSession session = TextSession.of(DOC, () -> 0L);
        AnchoredRanges diagnostics = new AnchoredRanges();
        diagnostics.set(List.of(new AnchoredRange(11, 22)));
        session.addProjector(diagnostics);
        JournalAuditeur auditeur = new JournalAuditeur();
        session.addEditListener(auditeur);

        assertTrue(session.replaceRange(0, 0, "// "));
        assertEquals("// " + DOC, session.text());
        // L'édition directe décale le diagnostic de +3 (gravité droite).
        assertEquals(List.of(new AnchoredRange(14, 25)), diagnostics.ranges());

        assertTrue(session.undo());
        assertEquals(DOC, session.text());
        // PIEGE-07 : la plage ancrée revient exactement à sa position
        // d'origine — le chemin direct ne décalait pas au retour.
        assertEquals(List.of(new AnchoredRange(11, 22)), diagnostics.ranges(),
                "le diagnostic doit revenir à sa position d'origine");
        // L'annulation notifie l'auditeur de l'édition inverse : plage
        // [0, 3) remplacée par le vide.
        assertEquals("0:3:", auditeur.appels().get(auditeur.appels().size() - 1));
        assertFalse(session.canUndo());
    }

    @Test
    void undoRespectsReadOnly_andKeepsTheStackIntact() {
        TextSession session = TextSession.of(DOC, () -> 0L);
        assertTrue(session.replaceRange(0, 0, "x"));
        assertEquals("x" + DOC, session.text());

        session.setReadOnly(true);
        // PIEGE-07 : l'annulation ne modifie RIEN en lecture seule — et ne
        // consomme pas l'étape (elle reste disponible après déverrouillage).
        assertFalse(session.undo(), "undo en lecture seule doit échouer proprement");
        assertEquals("x" + DOC, session.text());
        assertTrue(session.canUndo(), "l'étape ne doit pas être consommée");

        session.setReadOnly(false);
        assertTrue(session.undo());
        assertEquals(DOC, session.text());
    }

    @Test
    void applyIsRejectedInReadOnly_withoutSideEffect() {
        TextSession session = TextSession.of(DOC, () -> 0L);
        session.setReadOnly(true);
        assertFalse(session.apply(Transaction.of(new TextEdit(0, "", "x"))));
        assertEquals(DOC, session.text());
        assertEquals(0, session.undoDepth());
    }

    @Test
    void redoAppliesThroughPipelineToo_andDoesNotPolluteUndoStack() {
        TextSession session = TextSession.of(DOC, () -> 0L);
        JournalAuditeur auditeur = new JournalAuditeur();
        session.addEditListener(auditeur);

        assertTrue(session.replaceRange(0, 0, "x"));
        assertTrue(session.undo());
        auditeur.appels().clear();

        assertTrue(session.redo());
        assertEquals("x" + DOC, session.text());
        assertFalse(auditeur.appels().isEmpty(), "redo doit notifier les auditeurs d'édition");
        assertEquals("0:0:x", auditeur.appels().get(auditeur.appels().size() - 1));
        // Le redo ne doit pas empiler une NOUVELLE étape d'annulation : le
        // undo immédiat doit revenir à l'état d'origine, pas sauter une étape.
        assertTrue(session.undo());
        assertEquals(DOC, session.text());
    }

    @Test
    void consecutiveTyping_coalescesIntoOneUndoStep() {
        TextSession session = TextSession.of("", () -> 0L);
        for (char c : "hello world".toCharArray()) {
            assertTrue(session.replaceRange(session.length(), session.length(),
                    String.valueOf(c)));
        }
        assertEquals("hello world", session.text());
        // Les frappes consécutives fusionnent en UNE étape (coalescence
        // portée du dépôt de départ).
        assertEquals(1, session.undoDepth());
        assertTrue(session.undo());
        assertEquals("", session.text());
    }

    @Test
    void coalescenceTimeout_breaksTheChain() {
        long[] maintenant = {0L};
        TextSession session = TextSession.of("", () -> maintenant[0]);
        maintenant[0] = 1_000L;
        assertTrue(session.replaceRange(0, 0, "a"));
        maintenant[0] = 1_000L + UndoStack.COALESCE_TIMEOUT_MS;
        assertTrue(session.replaceRange(1, 1, "b"));
        // Le délai de coalescence est expiré : deux étapes distinctes.
        assertEquals(2, session.undoDepth());
        assertTrue(session.undo());
        assertEquals("a", session.text());
        assertTrue(session.undo());
        assertEquals("", session.text());
    }

    @Test
    void replaceBreaksTheCoalesceChain() {
        TextSession session = TextSession.of("", () -> 0L);
        assertTrue(session.replaceRange(0, 0, "h"));
        assertTrue(session.replaceRange(1, 1, "i"));
        // Un remplacement multi-caractères coupe la chaîne.
        assertTrue(session.replaceRange(0, 2, "HI!"));
        assertTrue(session.replaceRange(3, 3, "x"));
        assertEquals("HI!x", session.text());
        // Trois étapes attendues : « hi » coalescé, le remplacement, « x ».
        assertEquals(3, session.undoDepth());
        assertTrue(session.undo());
        assertEquals("HI!", session.text());
        assertTrue(session.undo());
        assertEquals("hi", session.text());
        assertTrue(session.undo());
        assertEquals("", session.text());
        assertFalse(session.undo(), "exactement trois étapes");
    }

    @Test
    void undoBreaksTheChain() {
        TextSession session = TextSession.of("", () -> 0L);
        assertTrue(session.replaceRange(0, 0, "AB"));
        assertTrue(session.replaceRange(2, 2, "c"));
        assertTrue(session.replaceRange(3, 3, "d"));
        assertEquals("ABcd", session.text());
        // L'annulation coupe la chaîne de coalescence.
        assertTrue(session.undo());
        assertEquals("AB", session.text());
        assertTrue(session.replaceRange(2, 2, "e"));
        assertEquals("ABe", session.text());
        // La frappe post-undo ne doit PAS fusionner avec l'étape restée au
        // sommet de la pile.
        assertEquals(2, session.undoDepth());
        assertTrue(session.undo());
        assertEquals("AB", session.text());
        assertTrue(session.undo());
        assertEquals("", session.text());
        assertFalse(session.undo());
    }

    @Test
    void multiEditTransaction_isOneUndoStep() {
        TextSession session = TextSession.of("aba caba", () -> 0L);
        // Renommer les deux premiers « a » en « o » : deux éditions (les
        // coordonnées de la seconde s'entendent après la première, même
        // longueur donc sans décalage), une transaction, une seule étape
        // d'annulation (le lot remplace begin/endBatch).
        assertTrue(session.apply(Transaction.of(
                new TextEdit(0, "a", "o"),
                new TextEdit(2, "a", "o"))));
        assertEquals("obo caba", session.text());
        assertEquals(1, session.undoDepth(), "un lot multi-éditions = une seule étape");
        assertTrue(session.undo());
        assertEquals("aba caba", session.text());
    }

    @Test
    void composeAppliesLikeSequentialTransactions() {
        TextSession session = TextSession.of("abcdef", () -> 0L);
        Transaction remplacement = Transaction.of(new TextEdit(2, "bc", "XY"));
        Transaction insertion = Transaction.of(new TextEdit(0, "", "!"));
        // Composer puis appliquer = appliquer l'un puis l'autre (les offsets
        // de la seconde s'entendent dans l'état après la première : « bc »
        // vit en [2, 4) une fois « ! » inséré devant).
        assertTrue(session.apply(Transaction.compose(insertion, remplacement)));
        assertEquals("!aXYdef", session.text());
        assertTrue(session.undo());
        assertEquals("abcdef", session.text());
    }

    @Test
    void emptyTransaction_isANoOp() {
        // Session montée sur un instantané existant (lecteurs sur
        // instantanés, ADR-007) : même voie de mutation.
        TextSession session = TextSession.of(Document.of(DOC), () -> 0L);
        JournalAuditeur auditeur = new JournalAuditeur();
        session.addEditListener(auditeur);
        assertTrue(session.apply(Transaction.of()));
        assertEquals(DOC, session.text());
        assertEquals(0, session.undoDepth());
        assertTrue(auditeur.appels().isEmpty());
    }

    @Test
    void replaceRangeNoOp_changesNothing() {
        TextSession session = TextSession.of(DOC, () -> 0L);
        assertTrue(session.replaceRange(3, 3, ""));
        assertEquals(DOC, session.text());
        assertEquals(0, session.undoDepth());
    }

    @Test
    void removedTextMismatch_failsFast() {
        TextSession session = TextSession.of("abcdef", () -> 0L);
        // L'édition prétend que « xy » occupe [2, 4) alors que le document
        // contient « cd » : le contrat de la Transaction est rompu, le moteur
        // refuse avant de muter (annulation corrompue évitée).
        assertThrows(IllegalStateException.class,
                () -> session.apply(Transaction.of(new TextEdit(2, "xy", "Z"))));
        assertEquals("abcdef", session.text());
        assertEquals(0, session.undoDepth());
    }

    @Test
    void outOfBoundsTransaction_failsAtomically() {
        TextSession session = TextSession.of("abcdef", () -> 0L);
        assertThrows(IndexOutOfBoundsException.class, () -> session.apply(
                Transaction.of(new TextEdit(0, "ab", "AB"),
                        new TextEdit(90, "z", "Z"))));
        // La validation précède toute mutation : le document est intact.
        assertEquals("abcdef", session.text());
        assertEquals(0, session.undoDepth());
    }
}
