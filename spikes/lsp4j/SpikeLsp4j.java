// SPDX-License-Identifier: Apache-2.0
package spike.lsp4j;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import org.eclipse.lemminx.XMLLanguageServer;
import org.eclipse.lemminx.customservice.XMLLanguageClientAPI;

import org.eclipse.lsp4j.jsonrpc.Launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Spike du client LSP sur LSP4J (M7.6, alimente l'ADR-015).
 *
 * <p>Question mesurée : LSP4J 1.0.0 (la cible du client, LSP 3.18)
 * peut-il lancer le serveur XML réel embarqué par M8.6 (LemMinX 0.31.2,
 * construit contre LSP4J 0.23.1) — la version du classpath décide, le
 * même harnais tourne sous 1.0.0, 0.24.0 et 0.23.1. Le harnais lance
 * les deux bouts par LSPLauncher sur des tubes à file (jamais les tubes
 * du JDK : la leçon M8.6 — leur lecture se couple à la vivacité du fil
 * écrivain, et le serveur répond depuis ses fils éphémères), puis
 * rejoue la poignée de main, l'ouverture, les diagnostics poussés, la
 * complétion documentée, le survol de DTD et l'extinction — le même
 * fil que les sept tests de ServeurXmlEmbarqueTest.</p>
 *
 * <p>exit() n'est jamais appelé : l'implémentation du serveur y éteint
 * la machine (constat M8.6) ; shutdown() seul termine proprement.</p>
 */
public final class SpikeLsp4j {

    private static final String URI = "file:///exemple.xml";

    private static final String VALIDE = """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE racine [
                <!-- Un catalogue d'exemples. -->
                <!ELEMENT racine (exemple*)>
                <!-- Un exemple nommé, avec sa valeur. -->
                <!ELEMENT exemple (nom, valeur)>
                <!-- Le nom de l'exemple. -->
                <!ELEMENT nom (#PCDATA)>
                <!-- La valeur mesurée de l'exemple. -->
                <!ELEMENT valeur (#PCDATA)>
            ]>
            <racine>
                <exemple>
                    <nom>premier</nom>
                    <valeur>1</valeur>
                </exemple>
            </racine>
            """;

    /** Le document au milieu d'une frappe : un « ex » inachevé (ligne 16). */
    private static final String AVEC_FRAPPE = VALIDE.replace("</racine>",
            "    <ex\n</racine>");

    // ------------------------------------------------- tubes à file --

    /** Direction d'un tube mémoire : la file découple la lecture de l'écrivain. */
    static final class TubeFile {
        private final LinkedBlockingQueue<byte[]> trames = new LinkedBlockingQueue<>();
        private final LinkedBlockingQueue<byte[]> retour = new LinkedBlockingQueue<>();

        OutputStream sortie() {
            return new OutputStream() {
                @Override
                public void write(int b) {
                    sortieEcrire(new byte[]{(byte) b});
                }

                @Override
                public void write(byte[] tampon, int decalage, int longueur) {
                    byte[] trame = new byte[longueur];
                    System.arraycopy(tampon, decalage, trame, 0, longueur);
                    sortieEcrire(trame);
                }

                private void sortieEcrire(byte[] trame) {
                    if (!trames.offer(trame)) {
                        throw new IllegalStateException("file pleine");
                    }
                }
            };
        }

        InputStream entree() {
            return new InputStream() {
                private byte[] trameCourante;
                private int position;

                @Override
                public int read() {
                    if (trameCourante == null || position >= trameCourante.length) {
                        try {
                            trameCourante = trames.poll(30, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return -1;
                        }
                        position = 0;
                        if (trameCourante == null) {
                            return -1;
                        }
                    }
                    return trameCourante[position++] & 0xFF;
                }
            };
        }
    }

    /** Tube complet : deux directions croisées. */
    static final class TubesCroises {
        final TubeFile versServeur = new TubeFile();
        final TubeFile duServeur = new TubeFile();
    }

    // ------------------------------------------------------ client --

    /** Client minimal : capture les diagnostics, ignore le reste. */
    static final class ClientNu implements LanguageClient {
        volatile PublishDiagnosticsParams derniersDiagnostics;
        final CountDownLatch diagnosticsArrives = new CountDownLatch(1);

        @Override
        public void telemetryEvent(Object objet) {
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams params) {
            derniersDiagnostics = params;
            diagnosticsArrives.countDown();
        }

        @Override
        public void showMessage(MessageParams message) {
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(
                ShowMessageRequestParams demande) {
            return CompletableFuture.completedFuture(new MessageActionItem());
        }

        @Override
        public void logMessage(MessageParams message) {
        }
    }

    // -------------------------------------------------------- main --

    public static void main(String[] args) throws Exception {
        System.out.println("== Spike lsp4j × LemMinX 0.31.2 (M7.6) ==");
        System.out.println("lsp4j chargé : "
                + Launcher.class.getProtectionDomain().getCodeSource().getLocation());
        System.out.println("serveur : " + XMLLanguageServer.class.getProtectionDomain()
                .getCodeSource().getLocation());

        TubesCroises tubes = new TubesCroises();
        ExecutorService executeur = Executors.newCachedThreadPool(fil -> {
            Thread daemon = new Thread(fil, "spike-lsp4j");
            daemon.setDaemon(true);
            return daemon;
        });

        long t0 = System.nanoTime();

        // Le serveur, comme la démo le lance : LSPLauncher sur XMLLanguageServer.
        XMLLanguageServer serveur = new XMLLanguageServer();
        Launcher<LanguageClient> lanceurServeur =
                new Launcher.Builder<LanguageClient>()
                        .setLocalService(serveur)
                        .setRemoteInterface(XMLLanguageClientAPI.class)
                        .setInput(tubes.versServeur.entree())
                        .setOutput(tubes.duServeur.sortie())
                        .setExecutorService(executeur)
                        .create();
        serveur.setClient(lanceurServeur.getRemoteProxy());
        lanceurServeur.startListening();

        // Le client : LSPLauncher aussi — la future voie de lsp-lsp4j.
        ClientNu client = new ClientNu();
        Launcher<LanguageServer> lanceurClient =
                new Launcher.Builder<LanguageServer>()
                        .setLocalService(client)
                        .setRemoteInterface(LanguageServer.class)
                        .setInput(tubes.duServeur.entree())
                        .setOutput(tubes.versServeur.sortie())
                        .setExecutorService(executeur)
                        .create();
        lanceurClient.startListening();
        LanguageServer distant = lanceurClient.getRemoteProxy();

        // 1. Poignée de main (à froid : la réflexion Gson s'y paie).
        long t1 = System.nanoTime();
        InitializeParams parametres = new InitializeParams();
        parametres.setProcessId((int) ProcessHandle.current().pid());
        InitializeResult resultat = distant.initialize(parametres).get(30, TimeUnit.SECONDS);
        long t2 = System.nanoTime();
        System.out.printf("initialize : %.0f ms — capacités présentes : %s%n",
                (t2 - t1) / 1e6, resultat.getCapabilities() != null);
        distant.getWorkspaceService();

        // 2. Ouverture du document à la frappe.
        DidOpenTextDocumentParams ouverture = new DidOpenTextDocumentParams();
        ouverture.setTextDocument(new TextDocumentItem(URI, "xml", 1, AVEC_FRAPPE));
        distant.getTextDocumentService().didOpen(ouverture);

        // 3. Diagnostics poussés.
        if (!client.diagnosticsArrives.await(30, TimeUnit.SECONDS)) {
            throw new AssertionError("aucun diagnostic poussé");
        }
        List<Diagnostic> diags = client.derniersDiagnostics.getDiagnostics();
        System.out.printf("publishDiagnostics : %d diagnostic(s) (le document a une "
                + "frappe inachevée)%n", diags.size());

        // 4. Complétion documentée juste après la frappe « <ex » (ligne 16, colonne 7).
        CompletionParams completion = new CompletionParams(
                new TextDocumentIdentifier(URI), new Position(16, 7));
        long t3 = System.nanoTime();
        Object reponse = distant.getTextDocumentService().completion(completion)
                .get(30, TimeUnit.SECONDS);
        long t4 = System.nanoTime();
        int nombreItems = 0;
        int documentes = 0;
        List<CompletionItem> items = null;
        if (reponse instanceof org.eclipse.lsp4j.jsonrpc.messages.Either<?, ?> either) {
            if (either.isLeft() && either.getLeft() instanceof List<?> liste) {
                items = liste.stream().map(o -> (CompletionItem) o).toList();
            } else if (either.isRight() && either.getRight() instanceof CompletionList liste) {
                items = liste.getItems();
            }
        } else if (reponse instanceof CompletionList liste) {
            items = liste.getItems();
        }
        if (items != null) {
            nombreItems = items.size();
            documentes = compterDocumentes(items);
        }
        System.out.printf("completion : %.0f ms — %d item(s), %d documenté(s)%n",
                (t4 - t3) / 1e6, nombreItems, documentes);

        // 5. Survol de DTD sur l'élément « nom » (ligne 13, colonne 9).
        HoverParams survolParams = new HoverParams(
                new TextDocumentIdentifier(URI), new Position(13, 9));
        long t5 = System.nanoTime();
        Hover survol = distant.getTextDocumentService().hover(survolParams)
                .get(30, TimeUnit.SECONDS);
        long t6 = System.nanoTime();
        System.out.printf("hover : %.0f ms — contenu : %s%n", (t6 - t5) / 1e6,
                survol == null || survol.getContents() == null ? "AUCUN"
                        : resume(survol.getContents().getRight() != null
                                ? survol.getContents().getRight().getValue()
                                : String.valueOf(survol.getContents().getLeft())));

        // 6. Extinction sans exit (exit éteindrait la machine, constat M8.6).
        distant.shutdown().get(30, TimeUnit.SECONDS);
        long t7 = System.nanoTime();
        System.out.printf("shutdown : %.0f ms%n", (t7 - t6) / 1e6);
        System.out.printf("AU TOTAL depuis le lancement : %.0f ms%n", (t7 - t0) / 1e6);
        System.out.println("SPIKE OK : poignée de main, diagnostics poussés, complétion, "
                + "survol et extinction passent sur cette version de lsp4j");
        executeur.shutdownNow();
    }

    private static int compterDocumentes(List<CompletionItem> items) {
        int n = 0;
        for (CompletionItem item : items) {
            if (item.getDocumentation() != null) {
                n++;
            }
        }
        return n;
    }

    private static String resume(String texte) {
        String propre = texte.replace("\n", " ").trim();
        return propre.length() > 70 ? propre.substring(0, 70) + "…" : propre;
    }

    private SpikeLsp4j() {
    }
}
