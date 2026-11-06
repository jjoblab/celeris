package jo.celeris.core.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;

/**
 * Le transport à flux (ADR-005) : le cadrage Content-Length sur des
 * flux quelconques — les tubes d'un processus serveur, une prise
 * réseau — un fil de lecture dédié, l'écriture protégée. Les serveurs
 * de langage tournent dans des processus isolés : c'est leur porte.
 */
public final class TransportFlux implements Transport {

    private final OutputStream sortie;
    private final Thread lecteur;
    private final Object verrouEcriture = new Object();
    private final DecodeurTrames decodeur = new DecodeurTrames();
    private final List<String> retenus = new ArrayList<>();
    private final AtomicBoolean fermetureAnnoncee = new AtomicBoolean();
    private volatile boolean ferme;
    private @Nullable Recepteur recepteur;

    private TransportFlux(InputStream entree, OutputStream sortie, String nom) {
        this.sortie = sortie;
        this.lecteur = new Thread(() -> lire(entree), "celeris-lsp-flux-" + nom);
        this.lecteur.setDaemon(true);
        this.lecteur.start();
    }

    /**
     * Crée le transport sur un couple de flux.
     *
     * @param entree le flux lu (les messages du pair)
     * @param sortie le flux écrit (les messages vers le pair)
     * @param nom    le nom du fil de lecture, pour le diagnostic
     * @return le transport démarré — la lecture commence aussitôt
     */
    public static TransportFlux sur(InputStream entree, OutputStream sortie, String nom) {
        return new TransportFlux(entree, sortie, nom);
    }

    private void lire(InputStream entree) {
        byte[] tampon = new byte[8192];
        while (!ferme) {
            int lus;
            try {
                lus = entree.read(tampon);
            } catch (IOException e) {
                annoncerFermeture();
                return;
            }
            if (lus < 0) {
                annoncerFermeture();
                return;
            }
            decodeur.ajouter(tampon, 0, lus);
            livrer();
        }
    }

    private synchronized void livrer() {
        for (String message : decodeur.messages()) {
            if (recepteur != null) {
                recepteur.recoit(message);
            } else {
                retenus.add(message);
            }
        }
    }

    private void annoncerFermeture() {
        Recepteur chezNous = recepteur;
        if (chezNous != null && fermetureAnnoncee.compareAndSet(false, true)) {
            chezNous.ferme();
        }
    }

    @Override
    public void envoyer(String trame) {
        if (ferme) {
            throw new IllegalStateException("transport is closed");
        }
        synchronized (verrouEcriture) {
            try {
                CadreurTrames.ecrire(sortie, trame);
            } catch (IOException e) {
                throw new IllegalStateException("write failed", e);
            }
        }
    }

    @Override
    public synchronized void brancher(Recepteur nouveau) {
        recepteur = nouveau;
        for (String retenu : retenus) {
            nouveau.recoit(retenu);
        }
        retenus.clear();
    }

    /** Le nombre de messages reçus sans récepteur branché — sonde des tests. */
    synchronized int messagesRetenus() {
        return retenus.size();
    }

    @Override
    public void close() {
        if (ferme) {
            return;
        }
        ferme = true;
        lecteur.interrupt();
        annoncerFermeture();
    }
}
