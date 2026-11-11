package jo.celeris.serveur.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.jspecify.annotations.Nullable;

/**
 * Le mini serveur de langage Java : un serveur LSP complet et
 * déterministe pour éprouver l'éditeur — diagnostics poussés à chaque
 * synchronisation, complétion par mots-clés et symboles, survol
 * documenté, symboles hiérarchiques, plis et formatage
 * d'indentation.
 *
 * <p>Le montage suit la convention des serveurs embarqués : la classe
 * vit à portée publique avec son constructeur vide, la méthode
 * {@code setClient(LanguageClient)} reçoit le proxy du client (la
 * convention de la frontière protocolaire l'appelle par réflexion,
 * comme tout serveur tiers monté par désignation). Le serveur se
 * lance aussi SEUL, sur l'entrée-sortie standard, par
 * {@link JavaLanguageServerMain}.</p>
 *
 * <p>La synchronisation est COMPLÈTE : l'hôte envoie le texte entier à
 * chaque changement (le genre annoncé à la poignée de main). Les
 * diagnostics se POUSSENT au client dès l'ouverture et à chaque
 * changement ; la fermeture publie la liste vide. L'analyse est
 * déterministe : un même document rend toujours les mêmes
 * réponses.</p>
 */
public final class JavaLanguageServer implements LanguageServer {

    private final Map<String, JavaSource> documents =
            new ConcurrentHashMap<>();

    private volatile @Nullable LanguageClient client;

    /** Le constructeur vide : le montage par désignation l'exige. */
    public JavaLanguageServer() {
    }

    /**
     * La convention des serveurs embarqués : le proxy client injecté
     * chez le serveur — le montage de la frontière protocolaire
     * l'appelle par réflexion.
     *
     * @param client le proxy du client hôte
     */
    public void setClient(LanguageClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(
            InitializeParams params) {
        ServerCapabilities capacites = new ServerCapabilities();
        capacites.setCompletionProvider(new CompletionOptions());
        capacites.setHoverProvider(Either.forLeft(Boolean.TRUE));
        capacites.setDocumentSymbolProvider(Either.forLeft(Boolean.TRUE));
        capacites.setFoldingRangeProvider(Either.forLeft(Boolean.TRUE));
        capacites.setDocumentFormattingProvider(Boolean.TRUE);
        TextDocumentSyncOptions complete = new TextDocumentSyncOptions();
        complete.setChange(TextDocumentSyncKind.Full);
        capacites.setTextDocumentSync(Either.forRight(complete));
        return CompletableFuture.completedFuture(
                new InitializeResult(capacites));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        // jamais appelé par la session cliente : elle s'arrête par
        // shutdown, sans jamais éteindre la machine de l'hôte
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return new Documents();
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return new WorkspaceService() {
            @Override
            public void didChangeConfiguration(
                    org.eclipse.lsp4j.DidChangeConfigurationParams params) {
                // le serveur ne se configure pas
            }

            @Override
            public void didChangeWatchedFiles(
                    org.eclipse.lsp4j.DidChangeWatchedFilesParams params) {
                // le serveur ne surveille rien
            }
        };
    }

    private final class Documents implements TextDocumentService {

        @Override
        public void didOpen(DidOpenTextDocumentParams params) {
            String uri = params.getTextDocument().getUri();
            documents.put(uri, JavaSource.of(
                    params.getTextDocument().getText()));
            publier(uri);
        }

        @Override
        public void didChange(DidChangeTextDocumentParams params) {
            String uri = params.getTextDocument().getUri();
            List<org.eclipse.lsp4j.TextDocumentContentChangeEvent> changes =
                    params.getContentChanges();
            if (changes.isEmpty()) {
                return;
            }
            // synchronisation complète : le DERNIER changement porte le
            // texte entier
            documents.put(uri, JavaSource.of(
                    changes.get(changes.size() - 1).getText()));
            publier(uri);
        }

        @Override
        public void didClose(DidCloseTextDocumentParams params) {
            String uri = params.getTextDocument().getUri();
            documents.remove(uri);
            LanguageClient present = client;
            if (present != null) {
                present.publishDiagnostics(new PublishDiagnosticsParams(uri,
                        List.of()));
            }
        }

        @Override
        public void didSave(DidSaveTextDocumentParams params) {
            // le serveur resynchronise à chaque changement complet :
            // l'enregistrement ne lui apprend rien
        }

        @Override
        public CompletableFuture<@Nullable Hover> hover(HoverParams params) {
            JavaSource document = documents.get(
                    params.getTextDocument().getUri());
            if (document == null) {
                return CompletableFuture.completedFuture(null);
            }
            int offset = offset(document, params.getPosition());
            return CompletableFuture.completedFuture(JavaHover.at(document,
                    JavaSymbols.of(document), offset));
        }

        @Override
        public CompletableFuture<Either<List<CompletionItem>,
                org.eclipse.lsp4j.CompletionList>> completion(
                CompletionParams params) {
            JavaSource document = documents.get(
                    params.getTextDocument().getUri());
            if (document == null) {
                return CompletableFuture.completedFuture(Either.forLeft(
                        List.of()));
            }
            int offset = offset(document, params.getPosition());
            return CompletableFuture.completedFuture(Either.forLeft(
                    JavaCompletion.at(document, JavaSymbols.of(document),
                            offset)));
        }

        @Override
        public CompletableFuture<List<Either<org.eclipse.lsp4j.SymbolInformation,
                DocumentSymbol>>> documentSymbol(
                DocumentSymbolParams params) {
            JavaSource document = documents.get(
                    params.getTextDocument().getUri());
            if (document == null) {
                return CompletableFuture.completedFuture(List.of());
            }
            return CompletableFuture.completedFuture(
                    symboles(document, JavaSymbols.of(document)));
        }

        @Override
        public CompletableFuture<List<FoldingRange>> foldingRange(
                FoldingRangeRequestParams params) {
            JavaSource document = documents.get(
                    params.getTextDocument().getUri());
            if (document == null) {
                return CompletableFuture.completedFuture(List.of());
            }
            List<FoldingRange> plis = new ArrayList<>();
            for (JavaFolds.Fold pli : JavaFolds.of(document)) {
                plis.add(new FoldingRange(pli.startLine(), pli.endLine()));
            }
            return CompletableFuture.completedFuture(List.copyOf(plis));
        }

        @Override
        public CompletableFuture<List<? extends TextEdit>> formatting(
                DocumentFormattingParams params) {
            JavaSource document = documents.get(
                    params.getTextDocument().getUri());
            if (document == null) {
                return CompletableFuture.completedFuture(List.of());
            }
            String formate = JavaFormatter.format(document,
                    params.getOptions().getTabSize(),
                    params.getOptions().isInsertSpaces());
            if (formate.equals(document.text())) {
                return CompletableFuture.completedFuture(List.of());
            }
            int derniere = document.lineCount() - 1;
            Range plage = new Range(new Position(0, 0),
                    new Position(derniere,
                            document.line(derniere).length()));
            return CompletableFuture.completedFuture(List.of(
                    new TextEdit(plage, formate)));
        }

        private void publier(String uri) {
            JavaSource document = documents.get(uri);
            if (document == null) {
                return;
            }
            LanguageClient present = client;
            if (present == null) {
                return;
            }
            List<Diagnostic> constats = new ArrayList<>();
            for (JavaDiagnostics.Finding constat
                    : JavaDiagnostics.of(document)) {
                Diagnostic diagnostic = new Diagnostic();
                diagnostic.setRange(plage(document, constat.start(),
                        constat.end()));
                diagnostic.setSeverity(DiagnosticSeverity.forValue(
                        constat.severity()));
                diagnostic.setMessage(constat.message());
                constats.add(diagnostic);
            }
            present.publishDiagnostics(new PublishDiagnosticsParams(uri,
                    List.copyOf(constats)));
        }

        private List<Either<org.eclipse.lsp4j.SymbolInformation,
                DocumentSymbol>> symboles(JavaSource document,
                List<JavaSymbols.Symbol> symbols) {
            List<Either<org.eclipse.lsp4j.SymbolInformation,
                    DocumentSymbol>> rendus = new ArrayList<>();
            for (JavaSymbols.Symbol symbole : symbols) {
                rendus.add(Either.forRight(ecrireSymbole(document, symbole)));
            }
            return rendus;
        }

        private DocumentSymbol ecrireSymbole(JavaSource document,
                JavaSymbols.Symbol symbole) {
            List<DocumentSymbol> enfants = new ArrayList<>();
            for (JavaSymbols.Symbol enfant : symbole.children()) {
                enfants.add(ecrireSymbole(document, enfant));
            }
            return new DocumentSymbol(symbole.name(),
                    SymbolKind.forValue(symbole.kind()),
                    plage(document, symbole.start(), symbole.end()),
                    plage(document, symbole.nameStart(),
                            symbole.nameEnd()),
                    null, enfants);
        }

        private Range plage(JavaSource document, int debut, int fin) {
            JavaSource.Spot debutSpot = document.spot(debut);
            JavaSource.Spot finSpot = document.spot(fin);
            return new Range(new Position(debutSpot.line(),
                    debutSpot.column()), new Position(finSpot.line(),
                    finSpot.column()));
        }

        private int offset(JavaSource document, Position position) {
            return document.offset(position.getLine(), position.getCharacter());
        }
    }
}
