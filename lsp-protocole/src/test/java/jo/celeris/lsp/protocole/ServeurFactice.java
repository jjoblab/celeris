package jo.celeris.lsp.protocole;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.jspecify.annotations.Nullable;

/**
 * Le serveur factice du banc de la frontière (ADR-015) : un serveur de
 * langage complet en processus, branché sur les mêmes flux que la
 * session — il rejoue poignée de main, documents versionnés,
 * diagnostics poussés (les deux formes de message), complétion
 * documentée, survol balisé, signature à décalages, définition, jetons
 * encodés et extinction. Le protocole circule pour de vrai : cadrage,
 * sérialisation, fils de lecture.
 */
final class ServeurFactice implements LanguageServer {

    /** L'état observable du serveur après la session : qui a été appelé. */
    static final class Etat {
        final AtomicBoolean arretDemande = new AtomicBoolean();
        final AtomicBoolean sortieDemandee = new AtomicBoolean();
        final AtomicReference<String> dernierTexte = new AtomicReference<>("");
        /** Arme le mode « complétion tenue » : la prochaine réponse ne part pas. */
        final AtomicBoolean tenirCompletion = new AtomicBoolean();
        /** Les vols de complétion tenus, dans l'ordre d'arrivée — le banc les achève. */
        final List<CompletableFuture<Either<List<CompletionItem>, CompletionList>>> volsCompletion =
                new CopyOnWriteArrayList<>();
    }

    private final Etat etat = new Etat();
    private volatile @Nullable LanguageClient client;

    /**
     * La convention des serveurs embarqués : le proxy client injecté
     * chez le serveur — le montage du module l'appelle pour les
     * serveurs montés par objet opaque (comme le serveur tiers retiré).
     */
    public void setClient(LanguageClient client) {
        this.client = client;
    }

    /**
     * Démarre le serveur factice sur un bout des flux du banc.
     *
     * <p>Le futur d'écoute vit autant que le fil démon qui le porte :
     * le banc ne l'annule jamais, il meurt avec le processus — la
     * suppression de l'avertissement le consigne.</p>
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    static Etat demarrer(InputStream entree, OutputStream sortie) {
        ServeurFactice serveur = new ServeurFactice();
        ExecutorService executeur = Executors.newCachedThreadPool(fil -> {
            Thread daemon = new Thread(fil, "serveur-factice");
            daemon.setDaemon(true);
            return daemon;
        });
        Launcher<LanguageClient> lanceur = LSPLauncher.createServerLauncher(
                serveur, entree, sortie, executeur, Function.identity());
        serveur.client = lanceur.getRemoteProxy();
        lanceur.startListening();
        return serveur.etat;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities capacites = new ServerCapabilities();
        CompletionOptions completion = new CompletionOptions();
        capacites.setCompletionProvider(completion);
        capacites.setHoverProvider(Either.forLeft(Boolean.TRUE));
        capacites.setDefinitionProvider(Either.forLeft(Boolean.TRUE));
        capacites.setSignatureHelpProvider(new SignatureHelpOptions());
        SemanticTokensWithRegistrationOptions jetons =
                new SemanticTokensWithRegistrationOptions();
        jetons.setLegend(new SemanticTokensLegend(
                List.of("type", "fonction"), List.of("declaration")));
        capacites.setSemanticTokensProvider(jetons);
        capacites.setReferencesProvider(Either.forLeft(Boolean.TRUE));
        capacites.setDocumentHighlightProvider(Either.forLeft(Boolean.TRUE));
        capacites.setDocumentSymbolProvider(Either.forLeft(Boolean.TRUE));
        capacites.setFoldingRangeProvider(Either.forLeft(Boolean.TRUE));
        capacites.setDocumentFormattingProvider(Boolean.TRUE);
        capacites.setRenameProvider(Either.forLeft(Boolean.TRUE));
        capacites.setCodeActionProvider(Either.forLeft(Boolean.TRUE));
        capacites.setInlayHintProvider(Either.forLeft(Boolean.TRUE));
        capacites.setSelectionRangeProvider(Either.forLeft(Boolean.TRUE));
        TextDocumentSyncOptions incrementale = new TextDocumentSyncOptions();
        incrementale.setChange(TextDocumentSyncKind.Incremental);
        capacites.setTextDocumentSync(Either.forRight(incrementale));
        return CompletableFuture.completedFuture(new InitializeResult(capacites));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        etat.arretDemande.set(true);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        etat.sortieDemandee.set(true);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return new ServiceDocuments();
    }

    /** Le service de documents du banc : chaque méthode rejoue un fragment du protocole. */
    private final class ServiceDocuments implements TextDocumentService {

        @Override
        public void didOpen(DidOpenTextDocumentParams params) {
            etat.dernierTexte.set(params.getTextDocument().getText());
            publierNu(params.getTextDocument().getUri(), DiagnosticSeverity.Hint,
                    "document ouvert", null, "factice");
        }

        @Override
        public void didChange(DidChangeTextDocumentParams params) {
            etat.dernierTexte.set(params.getContentChanges().get(0).getText());
            MarkupContent balise = new MarkupContent("markdown",
                    "l'élément **n'est pas déclaré**");
            publierBalise(params.getTextDocument().getUri(),
                    DiagnosticSeverity.Error, balise, 42);
        }

        @Override
        public void didClose(DidCloseTextDocumentParams params) {
            // l'état suivi s'arrête, comme le protocole l'exige
        }

        @Override
        public void didSave(DidSaveTextDocumentParams params) {
            // le banc ne sauvegarde jamais
        }

        @Override
        public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
                CompletionParams params) {
            if (etat.tenirCompletion.get()) {
                // le banc tient la réponse : la demande vit en vol, son
                // annulation au fil s'observera sur ce futur côté serveur
                CompletableFuture<Either<List<CompletionItem>, CompletionList>> vol =
                        new CompletableFuture<>();
                etat.volsCompletion.add(vol);
                return vol;
            }
            CompletionItem element = new CompletionItem();
            element.setLabel("ex:exemplaire");
            element.setKind(CompletionItemKind.Field);
            element.setDetail("l'élément exemplaire");
            element.setDocumentation(new MarkupContent("markdown",
                    "La documentation de l'élément exemplaire."));
            CompletionList liste = new CompletionList(true, List.of(element));
            return CompletableFuture.completedFuture(Either.forRight(liste));
        }

        @Override
        public CompletableFuture<@Nullable Hover> hover(HoverParams params) {
            return CompletableFuture.completedFuture(new Hover(
                    new MarkupContent("markdown", "La documentation survolée."),
                    new Range(new Position(0, 0), new Position(0, 2))));
        }

        @Override
        public CompletableFuture<@Nullable SignatureHelp> signatureHelp(
                SignatureHelpParams params) {
            org.eclipse.lsp4j.ParameterInformation aDecalage =
                    new org.eclipse.lsp4j.ParameterInformation();
            aDecalage.setLabel(Either.forRight(
                    org.eclipse.lsp4j.jsonrpc.messages.Tuple.two(2, 3)));
            org.eclipse.lsp4j.ParameterInformation bChaine =
                    new org.eclipse.lsp4j.ParameterInformation("b");
            SignatureHelp aide = new SignatureHelp();
            aide.setSignatures(List.of(new SignatureInformation(
                    "f(a, b)", (String) null, List.of(aDecalage, bChaine))));
            aide.setActiveSignature(0);
            aide.setActiveParameter(1);
            return CompletableFuture.completedFuture(aide);
        }

        @Override
        public CompletableFuture<Either<List<? extends Location>,
                List<? extends org.eclipse.lsp4j.LocationLink>>> definition(
                DefinitionParams params) {
            Location lieu = new Location(params.getTextDocument().getUri(),
                    new Range(new Position(0, 0), new Position(0, 1)));
            return CompletableFuture.completedFuture(Either.forLeft(List.of(lieu)));
        }

        // ── La couverture du jalon (H-12), chaque forme servie ──────

        @Override
        public CompletableFuture<List<Either<org.eclipse.lsp4j.SymbolInformation,
                org.eclipse.lsp4j.DocumentSymbol>>> documentSymbol(
                        org.eclipse.lsp4j.DocumentSymbolParams params) {
            org.eclipse.lsp4j.DocumentSymbol racine =
                    new org.eclipse.lsp4j.DocumentSymbol("exemplaire",
                            org.eclipse.lsp4j.SymbolKind.Field,
                            new Range(new Position(0, 0), new Position(2, 0)),
                            new Range(new Position(0, 0), new Position(0, 10)),
                            null,
                            List.of(new org.eclipse.lsp4j.DocumentSymbol(
                                    "imbrique", org.eclipse.lsp4j.SymbolKind.Function,
                                    new Range(new Position(1, 0), new Position(1, 5)),
                                    new Range(new Position(1, 0), new Position(1, 8)))));
            return CompletableFuture.completedFuture(
                    List.of(Either.forRight(racine)));
        }

        @Override
        public CompletableFuture<List<org.eclipse.lsp4j.FoldingRange>> foldingRange(
                org.eclipse.lsp4j.FoldingRangeRequestParams params) {
            // fin INCLUSIVE au protocole : la ligne 4 ferme la ligne 1
            return CompletableFuture.completedFuture(List.of(
                    new org.eclipse.lsp4j.FoldingRange(1, 4)));
        }

        @Override
        public CompletableFuture<List<? extends org.eclipse.lsp4j.TextEdit>> formatting(
                org.eclipse.lsp4j.DocumentFormattingParams params) {
            return CompletableFuture.completedFuture(List.of(
                    new org.eclipse.lsp4j.TextEdit(
                            new Range(new Position(0, 0), new Position(0, 2)),
                            "  ")));
        }

        @Override
        public CompletableFuture<List<? extends org.eclipse.lsp4j.TextEdit>> rangeFormatting(
                org.eclipse.lsp4j.DocumentRangeFormattingParams params) {
            return CompletableFuture.completedFuture(List.of(
                    new org.eclipse.lsp4j.TextEdit(
                            params.getRange(), "formate")));
        }

        @Override
        public CompletableFuture<List<? extends org.eclipse.lsp4j.DocumentHighlight>> documentHighlight(
                org.eclipse.lsp4j.DocumentHighlightParams params) {
            org.eclipse.lsp4j.DocumentHighlight surlignage =
                    new org.eclipse.lsp4j.DocumentHighlight();
            surlignage.setRange(new Range(new Position(0, 4),
                    new Position(0, 9)));
            surlignage.setKind(org.eclipse.lsp4j.DocumentHighlightKind.Read);
            return CompletableFuture.completedFuture(List.of(surlignage));
        }

        @Override
        public CompletableFuture<org.eclipse.lsp4j.jsonrpc.messages.Either3<
                org.eclipse.lsp4j.Range, org.eclipse.lsp4j.PrepareRenameResult,
                org.eclipse.lsp4j.PrepareRenameDefaultBehavior>> prepareRename(
                        org.eclipse.lsp4j.PrepareRenameParams params) {
            return CompletableFuture.completedFuture(
                    org.eclipse.lsp4j.jsonrpc.messages.Either3.forSecond(
                            new org.eclipse.lsp4j.PrepareRenameResult(
                                    new Range(new Position(0, 4),
                                            new Position(0, 9)),
                                    "ancien")));
        }

        @Override
        public CompletableFuture<org.eclipse.lsp4j.WorkspaceEdit> rename(
                org.eclipse.lsp4j.RenameParams params) {
            org.eclipse.lsp4j.WorkspaceEdit edition =
                    new org.eclipse.lsp4j.WorkspaceEdit();
            edition.getChanges().put(params.getTextDocument().getUri(),
                    List.of(new org.eclipse.lsp4j.TextEdit(
                            new Range(new Position(0, 4), new Position(0, 9)),
                            params.getNewName())));
            return CompletableFuture.completedFuture(edition);
        }

        @Override
        public CompletableFuture<List<Either<org.eclipse.lsp4j.Command,
                org.eclipse.lsp4j.CodeAction>>> codeAction(
                        org.eclipse.lsp4j.CodeActionParams params) {
            org.eclipse.lsp4j.CodeAction action =
                    new org.eclipse.lsp4j.CodeAction("Entourer d'accolades");
            org.eclipse.lsp4j.WorkspaceEdit edition =
                    new org.eclipse.lsp4j.WorkspaceEdit();
            edition.getChanges().put(params.getTextDocument().getUri(),
                    List.of(new org.eclipse.lsp4j.TextEdit(
                            params.getRange(), "{sélection}")));
            action.setEdit(edition);
            return CompletableFuture.completedFuture(List.of(
                    Either.forRight(action)));
        }

        @Override
        public CompletableFuture<List<? extends org.eclipse.lsp4j.Location>> references(
                org.eclipse.lsp4j.ReferenceParams params) {
            Location lieu = new Location(params.getTextDocument().getUri(),
                    new Range(new Position(3, 1), new Position(3, 6)));
            return CompletableFuture.completedFuture(List.of(lieu));
        }

        @Override
        public CompletableFuture<org.eclipse.lsp4j.SemanticTokens> semanticTokensFull(
                org.eclipse.lsp4j.SemanticTokensParams params) {
            org.eclipse.lsp4j.SemanticTokens jetons =
                    new org.eclipse.lsp4j.SemanticTokens();
            jetons.setResultId("factice-1");
            // un quintuple : ligne 2, colonne 4, 5 caractères, type 1
            jetons.setData(List.of(2, 4, 5, 1, 0));
            return CompletableFuture.completedFuture(jetons);
        }

        @Override
        public CompletableFuture<org.eclipse.lsp4j.SemanticTokens> semanticTokensRange(
                org.eclipse.lsp4j.SemanticTokensRangeParams params) {
            org.eclipse.lsp4j.SemanticTokens jetons =
                    new org.eclipse.lsp4j.SemanticTokens();
            jetons.setData(List.of(0, 0, 3, 0, 0));
            return CompletableFuture.completedFuture(jetons);
        }

        @Override
        public CompletableFuture<List<org.eclipse.lsp4j.InlayHint>> inlayHint(
                org.eclipse.lsp4j.InlayHintParams params) {
            org.eclipse.lsp4j.InlayHintLabelPart deuxPoints =
                    new org.eclipse.lsp4j.InlayHintLabelPart();
            deuxPoints.setValue("Double");
            deuxPoints.setLocation(new Location(params.getTextDocument().getUri(),
                    new Range(new Position(9, 0), new Position(9, 3))));
            org.eclipse.lsp4j.InlayHintLabelPart etiquette =
                    new org.eclipse.lsp4j.InlayHintLabelPart();
            etiquette.setValue("total");
            org.eclipse.lsp4j.InlayHint indice = new org.eclipse.lsp4j.InlayHint();
            indice.setPosition(new Position(4, 2));
            indice.setLabel(Either.forRight(List.of(deuxPoints, etiquette)));
            indice.setKind(org.eclipse.lsp4j.InlayHintKind.Type);
            indice.setPaddingLeft(Boolean.TRUE);
            return CompletableFuture.completedFuture(List.of(indice));
        }

        @Override
        public CompletableFuture<List<org.eclipse.lsp4j.SelectionRange>> selectionRange(
                org.eclipse.lsp4j.SelectionRangeParams params) {
            org.eclipse.lsp4j.SelectionRange parent =
                    new org.eclipse.lsp4j.SelectionRange();
            parent.setRange(new Range(new Position(0, 0), new Position(0, 20)));
            org.eclipse.lsp4j.SelectionRange intime =
                    new org.eclipse.lsp4j.SelectionRange();
            intime.setRange(new Range(new Position(0, 4), new Position(0, 9)));
            intime.setParent(parent);
            return CompletableFuture.completedFuture(List.of(intime));
        }
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return new WorkspaceService() {
            @Override
            public void didChangeConfiguration(
                    org.eclipse.lsp4j.DidChangeConfigurationParams params) {
                // le banc ne configure rien
            }

            @Override
            public void didChangeWatchedFiles(
                    org.eclipse.lsp4j.DidChangeWatchedFilesParams params) {
                // le banc ne surveille rien
            }
        };
    }

    /** Publie un diagnostic au message nu. */
    private void publierNu(String uri, DiagnosticSeverity gravite, String message,
            @Nullable Integer code, @Nullable String source) {
        LanguageClient clientCourant = client;
        if (clientCourant == null) {
            return;
        }
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(new Range(new Position(0, 0), new Position(0, 1)));
        diagnostic.setSeverity(gravite);
        diagnostic.setMessage(Either.forLeft(message));
        if (code != null) {
            diagnostic.setCode(Either.forRight(code));
        }
        diagnostic.setSource(source);
        clientCourant.publishDiagnostics(
                new PublishDiagnosticsParams(uri, List.of(diagnostic)));
    }

    /** Publie un diagnostic au message balisé, le code en chaîne. */
    private void publierBalise(String uri, DiagnosticSeverity gravite,
            MarkupContent message, int code) {
        LanguageClient clientCourant = client;
        if (clientCourant == null) {
            return;
        }
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(new Range(new Position(1, 0), new Position(1, 2)));
        diagnostic.setSeverity(gravite);
        diagnostic.setMessage(Either.forRight(message));
        diagnostic.setCode(Either.forLeft(String.valueOf(code)));
        clientCourant.publishDiagnostics(
                new PublishDiagnosticsParams(uri, List.of(diagnostic)));
    }
}
