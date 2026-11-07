package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.ai.PropositionAgent;
import jo.celeris.core.ai.RevueAgent;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Le faux agent de la démonstration (F-04) : la proposition est
 * déterministe — trois insertions éparses, donc trois blocs — et
 * fidèle au document de référence ; la boucle complète de la revue se
 * joue sans appareil, décision par décision jusqu'à la transaction
 * annulable.
 */
class FauxAgentTest {

    /**
     * La proposition : trois blocs épars, datés de la révision du
     * document — l'agent factice ne triche pas sur le contrat.
     */
    @Test
    void troisBlocsDeterministes() {
        FauxAgent agent = new FauxAgent();
        Document document = Document.of("fun main() {\n    println()\n}\n");

        PropositionAgent proposition = agent.proposer(document);
        RevueAgent revue = RevueAgent.de(document, proposition);

        assertEquals(3, revue.nombreDeBlocs(),
                "tete, milieu, pied : trois insertions, trois blocs");
        assertEquals(FauxAgent.ETIQUETTE, revue.etiquette(),
                "l'intention de l'agent se lit a la revue");
        assertTrue(revue.lignesDuBloc(0).get(0).startsWith("+ "),
                "les insertions se lisent en lignes « + »");
    }

    /**
     * La boucle complète : accepter, refuser, revenir au point de
     * contrôle, tout accepter — la fusion s'applique en une transaction
     * annulable, l'annulation remet le document d'origine.
     */
    @Test
    void laBoucleCompleteDecidePuisApplique() {
        FauxAgent agent = new FauxAgent();
        String texte = "fun main() {\n    println()\n}\n";
        TextSession session = TextSession.of(texte, System::nanoTime);

        RevueAgent revue = RevueAgent.de(session.document(),
                agent.proposer(session.document()));
        RevueAgent pointDeControle = revue.pointDeControle();

        RevueAgent melange = pointDeControle
                .decider(0, RevueAgent.Decision.ACCEPTE)
                .decider(1, RevueAgent.Decision.REFUSE);
        RevueAgent revenue = melange.retourAuPointDeControle(pointDeControle);
        RevueAgent tout = revenue
                .decider(0, RevueAgent.Decision.ACCEPTE)
                .decider(1, RevueAgent.Decision.ACCEPTE)
                .decider(2, RevueAgent.Decision.ACCEPTE);

        assertEquals(RevueAgent.Sortie.APPLIQUEE, tout.appliquer(session));
        assertTrue(session.document().text().contains("(tête)"));
        assertTrue(session.document().text().contains("(pied)"));
        assertTrue(session.undo(), "la fusion s'annule d'un geste");
        assertEquals(texte, session.document().text());
    }
}
