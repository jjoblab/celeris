package jo.celeris.core.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.SyntaxIssue;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.TextEdit;

/**
 * Tests de l'API d'outils de l'éditeur pour agents (F-05, ADR-012) :
 * lire des plages et des instantanés, lister les symboles, lire les
 * diagnostics, appliquer une transaction avec détection de conflit —
 * le critère de la matrice : transaction refusée si l'instantané de
 * référence est périmé, refus explicite.
 */
class OutilsEditeurTest {

    private static OutilsEditeur outils(TextSession session) {
        return new OutilsEditeurSurSession(session,
                document -> List.of(new OutlineEntry(
                        NodeCategory.FUNCTION, "principal", 0,
                        document.length(), 1, 1)),
                () -> List.of(new SyntaxIssue(0, 1, "accolade non fermee")));
    }

    /**
     * F-05 — les lectures portent la révision servie : l'agent qui veut
     * écrire rend cette révision en référence, la plage se lit aux
     * bornes exactes.
     */
    @Test
    void lesLecturesPortentLaRevisionServie() {
        TextSession session = TextSession.of("fun principal() {}", System::nanoTime);
        OutilsEditeur outils = outils(session);

        assertEquals(new PlageLue(0, "fun"), outils.lire(0, 3));
        assertEquals(new PlageLue(0, "fun principal() {}"), outils.instantane());
        assertThrows(IndexOutOfBoundsException.class, () -> outils.lire(0, 99),
                "une plage hors document ne se lit pas");
    }

    /**
     * F-05 — les symboles et les diagnostics viennent des sources
     * injectées : l'outil ne cache rien, il sert.
     */
    @Test
    void symbolesEtDiagnosticsServis() {
        TextSession session = TextSession.of("fun principal() {}", System::nanoTime);
        OutilsEditeur outils = outils(session);

        assertEquals(1, outils.symboles().size());
        assertEquals("principal", outils.symboles().get(0).name());
        assertEquals(1, outils.diagnostics().size());
        assertEquals("accolade non fermee", outils.diagnostics().get(0).message());
    }

    /**
     * F-05 — le critère de la matrice : la transaction refusée si
     * l'instantané de référence est périmé, refus explicite avec
     * message lisible ; le document qui n'a pas bougé s'applique en
     * transaction annulable.
     */
    @Test
    void transactionRefuseeSiInstantanePerime() {
        TextSession session = TextSession.of("val a = 1", System::nanoTime);
        OutilsEditeur outils = outils(session);
        PlageLue lue = outils.instantane();

        session.replaceRange(9, 9, " ");
        TransactionOutil perimee = new TransactionOutil(lue.revision(),
                List.of(new TextEdit(0, "val", "var")));
        TransactionOutilReponse refus = outils.appliquer(perimee);
        assertEquals(TransactionOutilReponse.Sortie.REFUSEE_CONFLIT, refus.sortie());
        assertTrue(refus.message().contains("relire"),
                "le refus dit quoi faire : " + refus.message());
        assertEquals("val a = 1 ", session.document().text(),
                "le document refuse reste intact");

        TransactionOutil fraiche = new TransactionOutil(
                session.document().revision(),
                List.of(new TextEdit(0, "val", "var")));
        TransactionOutilReponse reponse = outils.appliquer(fraiche);
        assertEquals(TransactionOutilReponse.Sortie.APPLIQUEE, reponse.sortie());
        assertEquals("var a = 1 ", session.document().text());
        assertTrue(session.undo(), "l'application s'annule d'un geste");
    }

    /**
     * F-05 — les autres refus, explicites eux aussi : session protégée,
     * éditions qui ne collent pas, transaction vide.
     */
    @Test
    void refusExplicitesProtectionEditsEtVide() {
        TextSession session = TextSession.of("val a = 1", System::nanoTime);
        OutilsEditeur outils = outils(session);
        int revision = session.document().revision();

        session.setReadOnly(true);
        assertEquals(TransactionOutilReponse.Sortie.REFUSEE_PROTECTION,
                outils.appliquer(new TransactionOutil(revision,
                        List.of(new TextEdit(0, "val", "var")))).sortie());
        session.setReadOnly(false);

        assertEquals(TransactionOutilReponse.Sortie.REFUSEE_EDITS,
                outils.appliquer(new TransactionOutil(revision,
                        List.of(new TextEdit(0, "VAR", "var")))).sortie(),
                "le texte retire inexact ne s'applique pas");

        assertEquals(TransactionOutilReponse.Sortie.REFUSEE_VIDE,
                outils.appliquer(new TransactionOutil(revision, List.of()))
                        .sortie());
    }

    /**
     * F-05 — les éditions multiples s'appliquent dans l'ordre du
     * document, coordonnées de référence : chaque édition décale les
     * suivantes de son delta.
     */
    @Test
    void editionsMultiplesEnCoordonneesDeReference() {
        TextSession session = TextSession.of("un deux trois", System::nanoTime);
        OutilsEditeur outils = outils(session);

        TransactionOutilReponse reponse = outils.appliquer(new TransactionOutil(
                session.document().revision(), List.of(
                        new TextEdit(0, "un", "1"),
                        new TextEdit(3, "deux", "2"),
                        new TextEdit(8, "trois", "3"))));
        assertEquals(TransactionOutilReponse.Sortie.APPLIQUEE, reponse.sortie());
        assertEquals("1 2 3", session.document().text());
    }
}
