package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Remplacement global par la session : la passe s'applique PAR LA VOIE DE
 * TRANSACTION UNIQUE (ADR-003, famille PIEGE-07) — les éditions, une par
 * occurrence en ordre du document, sont ajustées en deltas cumulés pour la
 * sémantique séquentielle de la {@link Transaction} ; tout le remplacement
 * ne forme QU'UNE étape d'annulation, jamais coalescée avec les frappes
 * (une commande n'est pas une frappe). L'annulation et le refaire rejouent
 * par la même voie — index de lignes et projections compris.
 */
class TextSessionReplaceTest {

    private static TextSession session(String texte) {
        return TextSession.of(texte, () -> 0L);
    }

    /** Auditeur d'édition qui compte les notifications. */
    private static final class CompteurEditions implements EditListener {
        private int appels;

        @Override
        public void onTextEdit(int start, int end, String removed, String inserted) {
            appels++;
        }
    }

    @Test
    void replaceAll_mutatesThroughTheSinglePath() {
        TextSession session = session("foo bar foo");
        CompteurEditions auditeur = new CompteurEditions();
        session.addEditListener(auditeur);
        int nombre = session.replaceAll("foo", "X", new FindOptions());
        assertEquals(2, nombre);
        assertEquals("X bar X", session.text());
        assertEquals(2, auditeur.appels, "une notification par édition remplacée");
        assertEquals(1, session.undoDepth(), "un remplacement global = UNE étape d'annulation");
    }

    @Test
    void replaceAll_undoRestores_redoReapplies() {
        TextSession session = session("ab cd ab");
        assertEquals(2, session.replaceAll("ab", "Z", new FindOptions()));
        assertEquals("Z cd Z", session.text());
        assertTrue(session.undo());
        assertEquals("ab cd ab", session.text(), "défaire restaure le texte d'origine");
        assertTrue(session.redo());
        assertEquals("Z cd Z", session.text(), "refaire réapplique le remplacement");
    }

    /** L'index de lignes suit le remplacement (splice par édition, PIEGE-07). */
    @Test
    void replaceAll_maintientLIndexDeLignes() {
        TextSession session = session("l1\nl2\nl3");
        assertEquals(1, session.replaceAll("l2", "x\ny", new FindOptions()));
        assertEquals("l1\nx\ny\nl3", session.text());
        assertEquals(4, session.document().lineCount());
        assertEquals("x", session.document().lineText(1));
        assertEquals("y", session.document().lineText(2));
        assertEquals("l3", session.document().lineText(3));
    }

    /**
     * Une commande ne coalesce JAMAIS : ni avec la chaîne de frappes qui
     * la précède, ni en armant une chaîne pour la frappe qui la suit —
     * chaque remplacement global est une étape propre.
     */
    @Test
    void replaceAll_neverCoalescesWithTyping() {
        TextSession session = session("ab");
        session.replaceRange(0, 0, "c");
        session.replaceRange(1, 1, "c");
        assertEquals(1, session.undoDepth(), "les deux frappes adjacentes coalescent");
        assertEquals(1, session.replaceAll("ab", "X", new FindOptions()));
        assertEquals(2, session.undoDepth(), "le remplacement global ne fusionne pas avec les frappes");
        session.replaceRange(3, 3, "c");
        assertEquals(3, session.undoDepth(), "la frappe suivante ne fusionne pas avec le remplacement");
        // Défaire intégralement : chaque étape se rejoue proprement.
        session.undo();
        session.undo();
        session.undo();
        assertEquals("ab", session.text());
        assertEquals(0, session.undoDepth());
    }

    @Test
    void replaceAll_readOnly_refuses() {
        TextSession session = session("foo");
        session.setReadOnly(true);
        assertEquals(0, session.replaceAll("foo", "X", new FindOptions()));
        assertEquals("foo", session.text());
        assertEquals(0, session.undoDepth());
    }

    /** Motif trop coûteux (horloge scriptée) : la session ne mute RIEN. */
    @Test
    void replaceAll_tooCostly_neMutatesNothing() {
        long[] appels = {0L};
        TextSession session = TextSession.of("a".repeat(2_000) + "bc",
                () -> appels[0]++ < 1 ? 0L : 10_000L);
        CompteurEditions auditeur = new CompteurEditions();
        session.addEditListener(auditeur);
        assertEquals(0, session.replaceAll("(a+)+d", "X", new FindOptions().withRegex(true)));
        assertEquals("a".repeat(2_000) + "bc", session.text());
        assertEquals(0, session.undoDepth());
        assertEquals(0, auditeur.appels, "aucun auditeur notifié");
    }

    @Test
    void replaceAll_regexGroups_throughTheSession() {
        TextSession session = session("v1 v2");
        assertEquals(2, session.replaceAll("v(\\d)", "u$1",
                new FindOptions().withRegex(true)));
        assertEquals("u1 u2", session.text());
        assertTrue(session.undo());
        assertEquals("v1 v2", session.text());
    }

    /** Les plages ancrées glissent du delta du remplacement (pipeline commun). */
    @Test
    void replaceAll_projectsAnchoredRanges() {
        TextSession session = session("aa bb cc");
        AnchoredRanges plages = new AnchoredRanges();
        plages.set(List.of(new AnchoredRange(6, 8)));
        session.addProjector(plages);
        assertEquals(1, session.replaceAll("bb", "bbbb", new FindOptions()));
        assertEquals(new AnchoredRange(8, 10), plages.ranges().get(0));
    }

    /** Aucune occurrence : la commande ne crée aucune étape. */
    @Test
    void replaceAll_noOccurrence_noStep() {
        TextSession session = session("hello");
        assertEquals(0, session.replaceAll("xyz", "X", new FindOptions()));
        assertEquals("hello", session.text());
        assertEquals(0, session.undoDepth());
    }

    /**
     * La passe est plafonnée : la session applique au plus
     * {@link FindReplace#MAX_MATCHES} remplacements, UNE étape d'annulation,
     * et l'annulation restaure le texte intégral.
     */
    @Test
    void replaceAll_capAtMaxMatches() {
        String texte = "aa ".repeat(FindReplace.MAX_MATCHES + 2);
        TextSession session = session(texte);
        assertEquals(FindReplace.MAX_MATCHES,
                session.replaceAll("aa", "X", new FindOptions()));
        assertEquals("X ".repeat(FindReplace.MAX_MATCHES) + "aa aa ", session.text());
        assertEquals(1, session.undoDepth());
        assertTrue(session.undo());
        assertEquals(texte, session.text(), "l'annulation restaure le texte intégral");
    }
}
