package jo.celeris.lsp.protocole;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Le point d'entrée LIGNE DE COMMANDE du serveur factice (D-09, le
 * banc des connexions externes) : {@code java … ServeurFacticeMain
 * stdio|socket PORT} monte le factice sur ses flux standards ou sur
 * une socket d'écoute — le banc lance CE processus par
 * {@link ExternalServer} et vérifie la liaison pour de vrai : un
 * processus de l'autre côté du transport.
 *
 * <p>Pur outil de banc : jamais dans le jar principal (il vit dans
 * les sources de test).</p>
 */
public final class ServeurFacticeMain {

    private ServeurFacticeMain() {
    }

    /** Monte le factice selon le mode demandé. */
    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "stdio";
        if ("socket".equals(mode)) {
            int port = Integer.parseInt(args[1]);
            try (ServerSocket listening = new ServerSocket(port)) {
                System.out.println("PRET " + port);
                System.out.flush();
                try (Socket prise = listening.accept()) {
                    tourner(prise.getInputStream(), prise.getOutputStream());
                }
            }
        } else {
            tourner(System.in, System.out);
        }
    }

    /** Le factice sur les flux donnés, jusqu'à extinction. */
    @SuppressWarnings("FutureReturnValueIgnored")
    private static void tourner(InputStream input, OutputStream output) {
        LanguageServer server = new ServeurFactice();
        ExecutorService executor = Executors.newCachedThreadPool(thread -> {
            Thread demon = new Thread(thread, "factice-externe");
            demon.setDaemon(true);
            return demon;
        });
        Launcher<LanguageClient> lanceur = LSPLauncher.createServerLauncher(
                server, input, output, executor, Function -> Function);
        ((ServeurFactice) server).setClient(lanceur.getRemoteProxy());
        lanceur.startListening();
        // le fil principal attend : le processus vit tant que la liaison vit
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
