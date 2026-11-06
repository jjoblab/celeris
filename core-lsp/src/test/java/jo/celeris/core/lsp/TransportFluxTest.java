package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Le transport à flux (ADR-005) : le cadrage Content-Length sur des
 * flux quelconques — tubes d'un processus serveur, prise réseau — un
 * fil de lecture dédié, l'écriture protégée. Les tubes JVM en servent
 * de banc d'essai : ce qui part d'un bout arrive encadré à l'autre.
 */
class TransportFluxTest {

    private static final java.util.concurrent.atomic.AtomicInteger echecsEcriture =
            new java.util.concurrent.atomic.AtomicInteger();

    private PipedInputStream entree;
    private PipedOutputStream sortieEcrite;
    private TransportFlux transport;
    private final List<String> recues = new ArrayList<>();

    @BeforeEach
    void monter() throws IOException {
        sortieEcrite = new PipedOutputStream();
        entree = new PipedInputStream(sortieEcrite);
        transport = TransportFlux.sur(entree, new PipedOutputStream(), "test");
        transport.brancher(recues::add);
    }

    @AfterEach
    void demonter() {
        transport.close();
    }

    /** L'aller-retour par les tubes : ce qui est envoyé arrive encadré, décodé à l'arrivée. */
    @Test
    void lallerRetourParLesTubes() {
        // l'autre bout du tube écrit dans le flux d'entrée du transport
        new Thread(() -> {
            try {
                sortieEcrite.write(CadreurTrames.tramer("{\"cote\":\"serveur\"}"));
                sortieEcrite.flush();
            } catch (IOException e) {
                echecsEcriture.incrementAndGet();
            }
        }).start();
        attendreCondition(() -> !recues.isEmpty());
        assertEquals(List.of("{\"cote\":\"serveur\"}"), recues);
    }

    /** L'émission : la trame encadrée part sur le flux de sortie. */
    @Test
    void lemissionCadree() throws Exception {
        PipedInputStream lecture = new PipedInputStream();
        PipedOutputStream ecriture = new PipedOutputStream(lecture);
        TransportFlux emetteur = TransportFlux.sur(new PipedInputStream(), ecriture, "emetteur");
        emetteur.envoyer("{\"a\":1}");
        byte[] tampon = new byte[128];
        int lus = lecture.read(tampon);
        assertEquals("Content-Length: 7\r\n\r\n{\"a\":1}",
                new String(tampon, 0, lus, java.nio.charset.StandardCharsets.UTF_8));
        emetteur.close();
    }

    /** Les messages avant branchement : retenus, servis au branchement. */
    @Test
    void lesMessagesAvantBranchement() throws Exception {
        try (PipedInputStream lecture = new PipedInputStream();
                PipedOutputStream ecriture = new PipedOutputStream(lecture)) {
            TransportFlux receveur = TransportFlux.sur(lecture, new PipedOutputStream(), "r");
            Thread ecrivain = new Thread(() -> {
                try {
                    ecriture.write(CadreurTrames.tramer("{\"retenu\":true}"));
                    ecriture.flush();
                } catch (IOException e) {
                    echecsEcriture.incrementAndGet();
                }
            });
            ecrivain.start();
            attendreCondition(() -> receveur.messagesRetenus() == 1);
            List<String> servis = new ArrayList<>();
            receveur.brancher(servis::add);
            assertEquals(List.of("{\"retenu\":true}"), servis);
            receveur.close();
        }
    }

    /** La fermeture : le récepteur en est prévenu, l'émission suivante est refusée. */
    @Test
    void laFermeturePrevient() {
        int[] fermetures = {0};
        transport.brancher(new Recepteur() {
            @Override
            public void recoit(String trame) {
                recues.add(trame);
            }

            @Override
            public void ferme() {
                fermetures[0]++;
            }
        });
        transport.close();
        transport.close();
        assertEquals(1, fermetures[0]);
        assertThrows(IllegalStateException.class, () -> transport.envoyer("{}"));
    }

    /** Attend que la condition soit vraie — sonde courte, bornée. */
    private static void attendreCondition(java.util.function.BooleanSupplier condition) {
        long fin = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < fin) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
