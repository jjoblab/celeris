package jo.celeris.serveur.java;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * Le point d'entrée autonome du mini serveur : l'entrée-sortie
 * standard, rien d'autre — pour éprouver l'éditeur contre un serveur
 * lancé comme un processus séparé (les bancs et l'intégration
 * continue y gagnent un serveur réel sans appareil).
 *
 * <p>Le serveur ne surveille jamais la mort d'un processus parent (le
 * piège des points d'entrée de certains serveurs tiers, fatal aux
 * hôtes embarqués — constat M8.6) : la boucle vit et meurt avec son
 * flux d'entrée, la fermeture du flux termine l'écoute, la piscine de
 * fils démons suit.</p>
 */
public final class JavaLanguageServerMain {

    private JavaLanguageServerMain() {
    }

    /**
     * Lance le serveur sur l'entrée-sortie standard et attend la fin
     * du flux d'entrée.
     *
     * @param args aucun argument attendu, ignorés sinon
     * @throws Exception si l'écoute du flux échoue
     */
    public static void main(String[] args) throws Exception {
        lancer(System.in, System.out);
    }

    /**
     * Lance le serveur sur des flux donnés et attend la fin du flux
     * d'entrée — le banc y passe des tubes à file, le point d'entrée
     * l'entrée-sortie standard.
     *
     * @param entree le flux d'entrée du protocole
     * @param sortie le flux de sortie du protocole
     * @throws Exception si l'écoute du flux échoue
     */
    static void lancer(java.io.InputStream entree,
            java.io.OutputStream sortie) throws Exception {
        JavaLanguageServer serveur = new JavaLanguageServer();
        ExecutorService executeur = Executors.newCachedThreadPool(fil -> {
            Thread daemon = new Thread(fil, "celeris-serveur-java");
            daemon.setDaemon(true);
            return daemon;
        });
        Launcher<LanguageClient> lanceur = LSPLauncher.createServerLauncher(
                serveur, entree, sortie, executeur,
                Function.identity());
        serveur.setClient(lanceur.getRemoteProxy());
        lanceur.startListening().get();
        executeur.shutdown();
    }
}
