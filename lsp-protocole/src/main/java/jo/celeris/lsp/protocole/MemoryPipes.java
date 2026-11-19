package jo.celeris.lsp.protocole;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

/**
 * Les tubes mémoire de la liaison en processus (ADR-015) : une
 * direction de flux dont l'écriture gare des octets et la lecture les
 * attend — le pont entre la session et un serveur de langage lancé
 * DANS le processus, par son API, sur des flux quelconques.
 *
 * <p>Pourquoi pas les tubes du JDK : leur sémantique de VIVACITÉ — une
 * lecture échoue dès que le fil qui a écrit meurt — casse la liaison
 * quand le serveur répond depuis ses fils de piscine, éphémères par
 * nature (constat M8.6). Ici, les octets vivent dans une file
 * bloquante : aucune filature de fil, la lecture attend des octets ou
 * la fermeture, jamais la mort de l'écrivain.</p>
 */
public final class MemoryPipes {

    private MemoryPipes() {
    }

    /**
     * Crée une direction de la liaison : ce qui entre par la sortie
     * sort par l'entrée, dans l'ordre, sans tampon partagé avec le
     * spectateur.
     *
     * @return la direction : son flux d'écriture et son flux de lecture
     */
    public static Direction create() {
        return new Direction();
    }

    /** Une direction de la liaison : une file d'octets entre deux bouts. */
    public static final class Direction {

        private final LinkedBlockingQueue<byte[]> trames =
                new LinkedBlockingQueue<>();
        private volatile boolean fermee;
        /** Le restant de la dernière trame lue — la lecture reprend là. */
        private byte[] restant = new byte[0];
        private int remainingCursor;

        private final OutputStream output = new OutputStream() {
            @Override
            public void write(int aByte) throws IOException {
                write(new byte[] {(byte) aByte}, 0, 1);
            }

            @Override
            public void write(byte[] buffer, int offset, int length)
                    throws IOException {
                if (fermee) {
                    throw new IOException("tube fermé");
                }
                if (length > 0) {
                    byte[] trame = new byte[length];
                    System.arraycopy(buffer, offset, trame, 0, length);
                    trames.add(trame);
                }
            }

            @Override
            public void close() {
                fermee = true;
            }
        };

        private final InputStream input = new InputStream() {
            @Override
            public int read(byte[] buffer, int offset, int length)
                    throws IOException {
                if (length <= 0) {
                    return 0;
                }
                byte[] trame = trameSuivante();
                if (trame == null) {
                    return -1;
                }
                int livre = Math.min(length, trame.length - remainingCursor);
                System.arraycopy(trame, remainingCursor, buffer, offset, livre);
                remainingCursor += livre;
                if (remainingCursor >= trame.length) {
                    restant = new byte[0];
                    remainingCursor = 0;
                }
                return livre;
            }

            @Override
            public int read() throws IOException {
                byte[] un = new byte[1];
                int lus = read(un, 0, 1);
                return lus < 0 ? -1 : (un[0] & 0xFF);
            }

            @Override
            public void close() {
                fermee = true;
            }
        };

        /** La prochaine trame à livrer : le restant, la file, sinon rien. */
        private byte @Nullable [] trameSuivante() throws IOException {
            if (remainingCursor < restant.length) {
                return restant;
            }
            while (true) {
                byte[] trame = null;
                try {
                    trame = trames.poll(200, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("lecture interrompue", e);
                }
                if (trame != null) {
                    restant = trame;
                    remainingCursor = 0;
                    return trame;
                }
                if (fermee) {
                    byte[] derniere = trames.poll();
                    if (derniere == null) {
                        return null;
                    }
                    restant = derniere;
                    remainingCursor = 0;
                    return derniere;
                }
            }
        }

        /** Le flux d'écriture de la direction. */
        public OutputStream output() {
            return output;
        }

        /** Le flux de lecture de la direction. */
        public InputStream input() {
            return input;
        }

        /**
         * Ferme la direction SANS exception vérifiée — l'écriture
         * suivante échouera, la lecture finit ce qui reste.
         */
        public void close() {
            fermee = true;
        }
    }
}
