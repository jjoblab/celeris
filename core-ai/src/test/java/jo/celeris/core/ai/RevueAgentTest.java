package jo.celeris.core.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.TextEdit;

/**
 * Tests de la revue des modifications d'agent (F-04, ADR-012) : trois
 * blocs, le refus du milieu, le réalignement des suivants et le retour
 * au point de contrôle — le critère de la matrice de parité, mot pour
 * mot.
 */
class RevueAgentTest {

    private static final EtiquetteAgent ETIQUETTE =
            new EtiquetteAgent("agent-exemple", "Nettoyer les trois lignes");

    /** Le document de référence et sa proposition à trois blocs. */
    private static PropositionAgent propositionTroisBlocs(Document reference) {
        return PropositionAgent.de(reference, ETIQUETTE, List.of(
                new TextEdit(0, "alpha", "ALPHA"),
                new TextEdit(6, "beta", "BETA"),
                new TextEdit(11, "gamma", "GAMMA")));
    }

    /**
     * F-04 — trois blocs se découvrent, le refus du milieu laisse les
     * autres vivre, le réalignement suit : les plages de référence ne
     * bougent jamais, le bloc refusé se saute.
     */
    @Test
    void troisBlocsRefusDuMilieuRealignement() {
        Document reference = Document.of("alpha beta gamma");
        RevueAgent revue = RevueAgent.de(reference, propositionTroisBlocs(reference));

        assertEquals(3, revue.nombreDeBlocs(), "trois blocs, trois éditions éparses");
        assertEquals(RevueAgent.Decision.EN_ATTENTE, revue.decision(0));

        RevueAgent refuseMilieu = revue.decider(1, RevueAgent.Decision.REFUSE);
        assertEquals("alpha beta gamma", refuseMilieu.texte(),
                "seul le refus est posé, les autres attendent, rien ne s'applique");

        RevueAgent complete = refuseMilieu
                .decider(0, RevueAgent.Decision.ACCEPTE)
                .decider(2, RevueAgent.Decision.ACCEPTE);
        assertEquals("ALPHA beta GAMMA", complete.texte(),
                "le bloc refusé se saute, les acceptés suivants ne se décalent pas");
    }

    /**
     * F-04 — le retour au point de contrôle : l'état enregistré
     * redevient l'état courant, décisions et fusion comprises.
     */
    @Test
    void retourAuPointDeControle() {
        Document reference = Document.of("alpha beta gamma");
        RevueAgent revue = RevueAgent.de(reference, propositionTroisBlocs(reference));
        RevueAgent pointDeControle = revue
                .decider(0, RevueAgent.Decision.ACCEPTE)
                .decider(1, RevueAgent.Decision.ACCEPTE)
                .pointDeControle();

        RevueAgent modifiee = pointDeControle
                .decider(1, RevueAgent.Decision.REFUSE)
                .decider(2, RevueAgent.Decision.REFUSE);
        assertEquals("ALPHA beta gamma", modifiee.texte());

        RevueAgent revenue = modifiee.retourAuPointDeControle(pointDeControle);
        assertEquals("ALPHA BETA gamma", revenue.texte(),
                "le retour au point de contrôle restaure les décisions d'alors");
        assertEquals(RevueAgent.Decision.ACCEPTE, revenue.decision(1));
    }

    /**
     * F-04 — l'application : les éditions acceptées deviennent une
     * seule transaction, une étape d'annulation — le geste annule la
     * fusion entière. Le document qui a bougé refuse, la session
     * protégée refuse, aucun bloc accepté ne s'applique pas.
     */
    @Test
    void applicationTransactionAnnulableEtRefusExplicites() {
        Document reference = Document.of("alpha beta gamma");
        RevueAgent revue = RevueAgent.de(reference, propositionTroisBlocs(reference))
                .decider(0, RevueAgent.Decision.ACCEPTE)
                .decider(1, RevueAgent.Decision.REFUSE)
                .decider(2, RevueAgent.Decision.ACCEPTE);

        TextSession session = TextSession.of("alpha beta gamma", System::nanoTime);
        assertEquals(RevueAgent.Sortie.APPLIQUEE, revue.appliquer(session));
        assertEquals("ALPHA beta GAMMA", session.document().text());
        assertTrue(session.undo(), "la fusion s'annule d'un geste");
        assertEquals("alpha beta gamma", session.document().text(),
                "l'annulation remet le document de référence");

        TextSession enRetard = TextSession.of("alpha beta gamma", System::nanoTime);
        enRetard.type(" !");
        assertEquals(RevueAgent.Sortie.REFUSEE_REVISION, revue.appliquer(enRetard),
                "le document qui a bougé refuse — jamais de fusion silencieuse");

        TextSession protegee = TextSession.of("alpha beta gamma", System::nanoTime);
        protegee.setReadOnly(true);
        assertEquals(RevueAgent.Sortie.REFUSEE_PROTECTION, revue.appliquer(protegee));

        RevueAgent sansAccepte = RevueAgent.de(reference, propositionTroisBlocs(reference));
        assertEquals(RevueAgent.Sortie.VIDE, sansAccepte.appliquer(session));
    }

    /**
     * Le découpage en blocs : des éditions contiguës forment un même
     * bloc, la lecture en ligne rend les « - » et les « + ».
     */
    @Test
    void editionsContiguesMemeBlocEtLectureEnLigne() {
        Document reference = Document.of("un deux trois");
        PropositionAgent proposition = PropositionAgent.de(reference, ETIQUETTE,
                List.of(
                        new TextEdit(0, "un", "1 "),
                        new TextEdit(2, "", "et "),
                        new TextEdit(8, "trois", "3")));
        RevueAgent revue = RevueAgent.de(reference, proposition);

        assertEquals(2, revue.nombreDeBlocs(),
                "les deux premières éditions se touchent, un même bloc");
        assertEquals(2, revue.editionsDuBloc(0).size());
        assertEquals(List.of("- un", "+ 1 ", "+ et "), revue.lignesDuBloc(0),
                "le remplacement rend le retiré puis l'inséré, les insertions suivent");
        assertEquals(List.of("- trois", "+ 3"), revue.lignesDuBloc(1),
                "le remplacement rend le retiré puis l'inséré");
    }

    /**
     * La proposition qui ne colle pas au document de référence ne
     * s'ouvre pas : texte retiré inexact, plage hors bornes, plages
     * chevauchantes — la revue ne répare pas ce qu'elle reçoit.
     */
    @Test
    void propositionInfideleRefuseeALOuverture() {
        Document reference = Document.of("alpha beta");
        assertThrows(IllegalArgumentException.class, () -> PropositionAgent.de(
                reference, ETIQUETTE,
                List.of(new TextEdit(0, "ALPHA", "X"))),
                "texte retire inexact");
        assertThrows(IllegalArgumentException.class, () -> PropositionAgent.de(
                reference, ETIQUETTE,
                List.of(new TextEdit(0, "alpha", "X"), new TextEdit(2, "pha", "Y"))),
                "plages chevauchantes");
        Document edite = reference.replace(0, 0, "x");
        assertThrows(IllegalArgumentException.class, () -> RevueAgent.de(
                reference, PropositionAgent.de(edite, ETIQUETTE,
                        List.of(new TextEdit(1, "alpha", "X")))),
                "la revue veut le document de la révision de la proposition");
    }

    /**
     * L'étiquette : l'auteur et l'intention se lisent à la revue —
     * une proposition anonyme ne se révise pas.
     */
    @Test
    void etiquettePresente() {
        Document reference = Document.of("alpha");
        RevueAgent revue = RevueAgent.de(reference, PropositionAgent.de(reference,
                ETIQUETTE, List.of(new TextEdit(0, "alpha", "X"))));
        assertEquals(ETIQUETTE, revue.etiquette());
        assertThrows(IllegalArgumentException.class,
                () -> new EtiquetteAgent("", "intention"));
        assertThrows(IllegalArgumentException.class,
                () -> new EtiquetteAgent("auteur", " "));
    }
}
