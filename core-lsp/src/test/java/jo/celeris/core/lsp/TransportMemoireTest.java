package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le transport en processus (ADR-005) : deux bouts reliés en mémoire,
 * livraison synchrone au récepteur de l'autre bout — ce qu'un bout
 * envoie, le pair le reçoit dans l'appel même. Envoyer exige un
 * récepteur pair branché ; fermer un bout coupe la liaison dans les
 * deux sens et prévient le pair une seule fois.
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

    /** La paire : ce qu'un bout envoie, le récepteur du pair le reçoit — et réciproquement. */
    @Test
    void laPaireRelieLesDeuxBouts() {
        TransportMemoire.Paire paire = TransportMemoire.creerPaire();
        RecepteurJournal coteServeur = new RecepteurJournal();
        RecepteurJournal coteClient = new RecepteurJournal();
        paire.boutB().brancher(coteServeur);
        paire.boutA().brancher(coteClient);
        paire.boutA().envoyer("{\"vers\":\"serveur\"}");
        paire.boutB().envoyer("{\"vers\":\"client\"}");
        assertEquals(List.of("{\"vers\":\"serveur\"}"), coteServeur.recus);
        assertEquals(List.of("{\"vers\":\"client\"}"), coteClient.recus);
    }

    /** Sans récepteur pair branché, l'envoi est refusé — jamais silencieusement perdu. */
    @Test
    void lenvoiSansRecepteurPairEstRefuse() {
        TransportMemoire.Paire paire = TransportMemoire.creerPaire();
        assertThrows(IllegalStateException.class,
                () -> paire.boutA().envoyer("{}"));
        RecepteurJournal recepteur = new RecepteurJournal();
        paire.boutB().brancher(recepteur);
        paire.boutA().envoyer("{}");
        assertEquals(1, recepteur.recus.size());
    }

    /** La fermeture d'un bout prévient le pair — et la liaison est coupée dans les deux sens. */
    @Test
    void laFermetureCoupeLesDeuxSens() {
        TransportMemoire.Paire paire = TransportMemoire.creerPaire();
        RecepteurJournal coteServeur = new RecepteurJournal();
        RecepteurJournal coteClient = new RecepteurJournal();
        paire.boutB().brancher(coteServeur);
        paire.boutA().brancher(coteClient);
        paire.boutB().close();
        // le pair est prévenu : le client apprend la fermeture de son serveur
        assertEquals(1, coteClient.fermetures);
        assertEquals(0, coteServeur.fermetures);
        // le bout fermé refuse d'émettre, le bout vivant ne peut plus joindre son pair
        assertThrows(IllegalStateException.class,
                () -> paire.boutB().envoyer("{}"));
        assertThrows(IllegalStateException.class,
                () -> paire.boutA().envoyer("{}"));
        // rien n'a fuité avant la coupure
        assertEquals(List.of(), coteServeur.recus);
    }

    /** La fermeture est idempotente : fermer deux fois ne prévient qu'une fois. */
    @Test
    void laFermetureEstIdempotente() {
        TransportMemoire.Paire paire = TransportMemoire.creerPaire();
        RecepteurJournal coteServeur = new RecepteurJournal();
        paire.boutB().brancher(coteServeur);
        paire.boutA().close();
        paire.boutA().close();
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
