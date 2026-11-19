package jo.celeris.lsp.protocole;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

/**
 * Les connexions aux serveurs de langage EXTERNES (D-09, l'analyse
 * croisée) : la session parle à un serveur hors du processus — par
 * SES flux standards (stdio, le lanceur classique des serveurs LSP)
 * ou par une socket TCP (le mode distant des serveurs lourds). Le
 * dépôt de départ n'offrait que l'embarqué (ADR-015) : l'éditeur y
 * gagne les vrais serveurs — jdtls, pyright, clangd.
 *
 * <p>Le cycle de vie est HONNÊTE : la connexion ouvre la session,
 * {@link #close()} éteint dans l'ORDRE — shutdown au serveur, flux
 * fermés, processus DÉTRUIT (forclos si le serveur traîne), socket
 * refermée. Un serveur qui meurt sous la session ne pend pas la
 * fermeture : les délais sont bornés.</p>
 *
 * <p>Pur JDK : aucune dépendance Android, la façade de l'hôte y
 * câble ses déclarations de serveurs (le nom de commande, le port).</p>
 */
public final class ExternalServer implements AutoCloseable {

    /** Le délai d'extinction du processus externe, en secondes. */
    private static final long DELAI_EXTINCTION = 5;

    /** La session ouverte sur le serveur externe. */
    private final LspSession session;

    /** Le processus du serveur (stdio), null en mode socket. */
    private final @Nullable Process processus;

    /** La socket du serveur (TCP), null en mode stdio. */
    private final @Nullable Socket socket;

    private ExternalServer(LspSession session, @Nullable Process processus,
            @Nullable Socket socket) {
        this.session = session;
        this.processus = processus;
        this.socket = socket;
    }

    /**
     * Monte la session sur un serveur EXTERNE par ses flux standards
     * (stdio) : la commande donnée lance le processus, ses flux
     * deviennent le transport. La poignée de main reste à faire
     * ({@link LspSession#initialize()}).
     *
     * @param command la commande du serveur et ses arguments
     * @param clientName le nom annoncé à la poignée de main
     * @return la connexion : session prête, processus vivant
     * @throws IOException le processus ne démarre pas
     */
    public static ExternalServer viaStdio(String[] command, String clientName)
            throws IOException {
        ProcessBuilder constructeur = new ProcessBuilder(command);
        constructeur.redirectErrorStream(false);
        Process proc = constructeur.start();
        LspSession session = LspSession.on(proc.getInputStream(),
                proc.getOutputStream(), clientName);
        return new ExternalServer(session, proc, null);
    }

    /**
     * Monte la session sur un serveur EXTERNE par socket TCP : le
     * serveur écoute (jdtls, pyright-langserver --socket), la session
     * s'y connecte. La poignée de main reste à faire.
     *
     * @param host l'hôte du serveur (par exemple {@code 127.0.0.1})
     * @param port le port d'écoute du serveur
     * @param clientName le nom annoncé à la poignée de main
     * @return la connexion : session prête, socket ouverte
     * @throws IOException la connexion ne s'établit pas
     */
    public static ExternalServer viaSocket(String host, int port,
            String clientName) throws IOException {
        Socket prise = new Socket(InetAddress.getByName(host), port);
        prise.setTcpNoDelay(true);
        LspSession session = LspSession.on(prise.getInputStream(),
                prise.getOutputStream(), clientName);
        return new ExternalServer(session, null, prise);
    }

    /**
     * La session de la connexion — la façade y fait sa poignée de
     * main et ses requêtes.
     *
     * @return la session LSP ouverte sur le serveur externe
     */
    public LspSession session() {
        return session;
    }

    /**
     * Éteint la connexion dans l'ORDRE : shutdown au serveur (borné),
     * la session se ferme, les flux et le PROCESSUS suivent (forclos
     * si le serveur traîne — jamais une fermeture pendante), la
     * socket se referme. Idempotent.
     */
    @Override
    public void close() {
        try {
            session.close();
        } finally {
            if (processus != null) {
                processus.destroy();
                attendreExtinction(processus);
                processus.destroyForcibly();
            }
            if (socket != null) {
                fermerSocket();
            }
        }
    }

    /** Le processus meurt en silence : le délai borné, puis force. */
    private static void attendreExtinction(Process proc) {
        try {
            if (!proc.waitFor(DELAI_EXTINCTION, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            proc.destroyForcibly();
        }
    }

    /** La socket se referme en silence — la fermeture ne remonte pas. */
    private void fermerSocket() {
        Socket prise = socket;
        if (prise == null) {
            return;
        }
        try {
            prise.close();
        } catch (IOException e) {
            // déjà refermée côté serveur : la fermeture est idempotente
        }
    }
}
