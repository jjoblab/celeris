package jo.celeris.core.lsp;

import org.jspecify.annotations.Nullable;

/**
 * Le transport en processus (ADR-005) : deux bouts reliés en mémoire,
 * livraison synchrone — ce qu'un bout envoie, le récepteur de l'autre
 * le reçoit dans l'appel même. C'est le support des tests du client et
 * des serveurs de langage embarqués : aucun fil, aucune latence, un
 * ordre total évident.
 */
public final class TransportMemoire {

    private TransportMemoire() {
    }

    /**
     * Crée une paire de bouts reliés.
     *
     * @return les deux bouts — boutA et boutB sont interchangeables
     */
    public static Paire creerPaire() {
        Bout boutA = new Bout();
        Bout boutB = new Bout();
        boutA.pair = boutB;
        boutB.pair = boutA;
        return new Paire(boutA, boutB);
    }

    /** Deux bouts reliés : ce que l'un envoie, l'autre le reçoit. */
    public record Paire(Transport boutA, Transport boutB) {
    }

    private static final class Bout implements Transport {

        private @Nullable Bout pair;
        private @Nullable Recepteur recepteur;
        private boolean ferme;

        @Override
        public void envoyer(String trame) {
            if (ferme) {
                throw new IllegalStateException("transport is closed");
            }
            Bout autre = pair;
            if (autre == null || autre.ferme) {
                throw new IllegalStateException("peer transport is closed");
            }
            Recepteur chezAutre = autre.recepteur;
            if (chezAutre == null) {
                throw new IllegalStateException("peer has no receiver attached");
            }
            chezAutre.recoit(trame);
        }

        @Override
        public synchronized void brancher(Recepteur nouveau) {
            recepteur = nouveau;
        }

        @Override
        public synchronized void close() {
            if (ferme) {
                return;
            }
            ferme = true;
            Bout autre = pair;
            if (autre != null) {
                autre.pair = null;
                Recepteur chezAutre = autre.recepteur;
                if (chezAutre != null) {
                    chezAutre.ferme();
                }
            }
        }
    }
}
