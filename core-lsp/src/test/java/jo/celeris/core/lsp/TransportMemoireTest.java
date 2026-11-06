package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le transport en processus (ADR-005) : deux bouts reliés en mémoire,
 * livraison synchrone au récepteur branché — le support des tests du
 * client et des serveurs embarqués. Envoyer exige un récepteur branché,
 * fermer prévient le pair, tout après fermeture est refusé.
 */
class TransportMemoireTest {

    /** Un récepteur qui enregistre tout ce qu'il reçoit. */
    private static final class RecepteurJournal implements Recepteur {
        final List<String> recus = new ArrayList<>();
        int fermetures;

        @Override
        public void recoit(String trame) {
            recus.add(trame);
        }

        @Override
        public void ferme() {
            fermetures++;
        }
    }

    /** La paire : ce qu'un bout envoie, l'autre le reçoit — et réciproquement. */
    @Test
    void laPaireRelieLesDeuxBouts() {
        TransportMemoire.Paire paire = TransportMemoire.creerPaire();
        RecepteurJournal coteServeur = new RecepteurJournal();
        RecepteurJournal coteClient = new RecepteurJournal();
        paire.boutA().brancher(coteServeur);
        paire.boutB().brancher(coteClient);
        paire.boutA().envoyer("{\"vers\":\"serveur\"}");
        paire.boutB().envoyer("{\"vers\":\"client\"}");
        assertEquals(List.of("{\"vers\":\"serveur\"}"), coteServeur.recus);
        assertEquals(List.of("{\"vers\":\"client\"}"), coteClient.recus);
    }

    /** Sans récepteur branché, l'envoi est refusé — jamais silencieusement perdu. */
    @Test
    void lenvoiSansRecepteurEstRefuse() {
        TransportMemoire.Paire paire = TransportMemoire.creerPaire();
        assertThrows(IllegalStateException.class,
                () -> paire.boutA().envoyer("{}"));
        RecepteurJournal recepteur = new RecepteurJournal();
        paire.boutA().brancher(recepteur);
        paire.boutA().envoyer("{}");
        assertEquals(1, recepteur.recus.size());
    }

    /** La fermeture d'un bout prévient le pair — et l'autre sens continue de vivre. */
    @Test
    void laFermeturePrevientLePair() {
        TransportMemoire.Paire paire = TransportMemoire.creerPaire();
        RecepteurJournal coteServeur = new RecepteurJournal();
        RecepteurJournal coteClient = new RecepteurJournal();
        paire.boutA().brancher(coteServeur);
        paire.boutB().brancher(coteClient);
        paire.boutB().close();
        assertEquals(1, coteServeur.fermetures);
        assertEquals(0, coteClient.fermetures);
        // le bout fermé refuse tout, le bout vivant émet encore
        assertThrows(IllegalStateException.class,
                () -> paire.boutB().envoyer("{}"));
        paire.boutA().envoyer("{\"apres\":true}");
        assertEquals(List.of("{\"apres\":true}"), coteServeur.recus);
    }

    /** La fermeture est idempotente : fermer deux fois ne prévient qu'une fois. */
    @Test
    void laFermetureEstIdempotente() {
        TransportMemoire.Paire paire = TransportMemoire.creerPaire();
        RecepteurJournal coteServeur = new RecepteurJournal();
        paire.boutA().brancher(coteServeur);
        paire.boutB().close();
        paire.boutB().close();
        assertEquals(1, coteServeur.fermetures);
    }

    /** Le rebranchement : brancher remplace, l'ancien récepteur ne reçoit plus. */
    @Test
    void leRebranchementRemplace() {
        TransportMemoire.Paire paire = TransportMemoire.creerPaire();
        RecepteurJournal premier = new RecepteurJournal();
        RecepteurJournal second = new RecepteurJournal();
        paire.boutA().brancher(premier);
        paire.boutA().brancher(second);
        paire.boutB().envoyer("{\"un\":1}");
        assertEquals(0, premier.recus.size());
        assertEquals(List.of("{\"un\":1}"), second.recus);
    }
}
