package jo.celeris.lsp.protocole;

import static jo.celeris.lsp.protocole.DocumentsWriting.position;
import static jo.celeris.lsp.protocole.DocumentsWriting.readRange;
import static jo.celeris.lsp.protocole.DocumentsWriting.writeSelectionRanges;
import static jo.celeris.lsp.protocole.DocumentsWriting.range;
import static jo.celeris.lsp.protocole.DocumentsWriting.writeSignature;
import static jo.celeris.lsp.protocole.DocumentsWriting.writeSymbol;
import static jo.celeris.lsp.protocole.DocumentsWriting.writeEdits;
import static jo.celeris.lsp.protocole.DocumentsWriting.ecrireEspace;
import static jo.celeris.lsp.protocole.DocumentsWriting.formattingOptions;
import static jo.celeris.lsp.protocole.DocumentsWriting.encoderTokens;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentHighlightKind;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintKind;
import org.eclipse.lsp4j.InlayHintLabelPart;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.jspecify.annotations.Nullable;

import jo.celeris.core.lsp.types.CompletionListLsp;
import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.CompletionItemLsp;
import jo.celeris.core.lsp.types.CodeActionLsp;
import jo.celeris.core.lsp.types.WorkspaceEditLsp;
import jo.celeris.core.lsp.types.InlayHintLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.FoldLsp;
import jo.celeris.core.lsp.types.LspPosition;
import jo.celeris.core.lsp.types.RenamePrepareLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.HighlightLsp;
import jo.celeris.core.lsp.types.HoverLsp;
import jo.celeris.core.lsp.types.SymbolLsp;

/**
 * L'adaptateur du SPI du domaine au service de documents de la
 * bibliothèque (ADR-015) : le serveur de référence du montage, la
 * correspondance inverse y vit — le terrain ne connaît que les types
 * du domaine, le protocole reste ici. La couverture du jalon (H-12)
 * y entre méthode par méthode : chaque repli vide du SPI reçoit sa
 * réponse vide du protocole.
 */
final class DocumentsServer implements LanguageServer {

    private final EmbeddedServer.Docs documents;
    /** Le proxy client — posé par la convention setClient du montage. */
    private volatile @Nullable LanguageClient client;

    DocumentsServer(EmbeddedServer.Docs documents) {
        this.documents = documents;
    }

    /** La convention des serveurs embarqués : le proxy client du montage. */
    public void setClient(LanguageClient client) {
        this.client = client;
    }

    /** Publie les diagnostics courants d'un document par le protocole. */
    private void publierDiagnostics(String uri) {
        org.eclipse.lsp4j.services.LanguageClient clientCourant = client;
        if (clientCourant == null) {
            return;
        }
        List<org.eclipse.lsp4j.Diagnostic> ecrits = new ArrayList<>();
        for (DiagnosticLsp diagnostic : documents.diagnosticsOf(uri)) {
            org.eclipse.lsp4j.Diagnostic ecrit = new org.eclipse.lsp4j.Diagnostic();
            ecrit.setRange(new org.eclipse.lsp4j.Range(
                    new org.eclipse.lsp4j.Position(
                            diagnostic.range().start().line(),
                            diagnostic.range().start().column()),
                    new org.eclipse.lsp4j.Position(
                            diagnostic.range().end().line(),
                            diagnostic.range().end().column())));
            ecrit.setSeverity(org.eclipse.lsp4j.DiagnosticSeverity
                    .forValue(diagnostic.severity()));
            ecrit.setMessage(diagnostic.message());
            if (diagnostic.code() != null) {
                ecrit.setCode(Either.forLeft(diagnostic.code()));
            }
            if (diagnostic.source() != null) {
                ecrit.setSource(diagnostic.source());
            }
            ecrits.add(ecrit);
        }
        clientCourant.publishDiagnostics(
                new org.eclipse.lsp4j.PublishDiagnosticsParams(uri, ecrits));
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(
            InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setHoverProvider(Either.forLeft(Boolean.TRUE));
        // la complétion du terrain (le retour du 2026-11-19) : le
        // serveur factice de l'exemple y sert ses candidats
        capabilities.setCompletionProvider(new CompletionOptions());
        capabilities.getCompletionProvider().setTriggerCharacters(
                List.of(".", "("));
        capabilities.setSignatureHelpProvider(
                new SignatureHelpOptions(List.of("(")));
        // la couverture du jalon (H-12) s'annonce : le terrain qui
        // n'en sert pas répond ses vides du protocole
        capabilities.setDocumentSymbolProvider(Either.forLeft(Boolean.TRUE));
        capabilities.setFoldingRangeProvider(Either.forLeft(Boolean.TRUE));
        capabilities.setDocumentFormattingProvider(Boolean.TRUE);
        capabilities.setDocumentHighlightProvider(Either.forLeft(Boolean.TRUE));
        capabilities.setReferencesProvider(Either.forLeft(Boolean.TRUE));
        capabilities.setRenameProvider(Either.forLeft(Boolean.TRUE));
        capabilities.setCodeActionProvider(Either.forLeft(Boolean.TRUE));
        return CompletableFuture.completedFuture(
                new InitializeResult(capabilities));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        // jamais appelé par la session (constat M8.6) ; rien à éteindre
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return new DocumentsService();
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return new WorkspaceService() {
            @Override
            public void didChangeConfiguration(
                    org.eclipse.lsp4j.DidChangeConfigurationParams params) {
                // le terrain ne configure rien
            }

            @Override
            public void didChangeWatchedFiles(
                    org.eclipse.lsp4j.DidChangeWatchedFilesParams params) {
                // le terrain ne surveille rien
            }
        };
    }

    /** Le service de documents : la synchronisation, le survol, la signature. */
    private final class DocumentsService implements TextDocumentService {

        @Override
        public void didOpen(DidOpenTextDocumentParams params) {
            documents.open(params.getTextDocument().getUri(),
                    params.getTextDocument().getLanguageId(),
                    params.getTextDocument().getText());
            publierDiagnostics(params.getTextDocument().getUri());
        }

        @Override
        public void didChange(DidChangeTextDocumentParams params) {
            List<org.eclipse.lsp4j.TextDocumentContentChangeEvent> changements =
                    params.getContentChanges();
            if (!changements.isEmpty()) {
                documents.change(params.getTextDocument().getUri(),
                        params.getTextDocument().getVersion(),
                        changements.get(changements.size() - 1).getText());
            publierDiagnostics(params.getTextDocument().getUri());
            }
        }

        @Override
        public void didClose(DidCloseTextDocumentParams params) {
            documents.close(params.getTextDocument().getUri());
        }

        @Override
        public void didSave(DidSaveTextDocumentParams params) {
            // le terrain ne sauvegarde jamais
        }

        @Override
        public CompletableFuture<@Nullable Hover> hover(HoverParams params) {
            HoverLsp hover = documents.hover(
                    params.getTextDocument().getUri(), position(params));
            if (hover == null || hover.text().isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            MarkupContent contenu = new MarkupContent(
                    hover.kind().isEmpty() ? MarkupKind.MARKDOWN : hover.kind(),
                    hover.text());
            return CompletableFuture.completedFuture(
                    new Hover(contenu, range(hover.range())));
        }

        @Override
        public CompletableFuture<@Nullable SignatureHelp> signatureHelp(
                SignatureHelpParams params) {
            SignatureLsp help = documents.signature(
                    params.getTextDocument().getUri(), position(params));
            if (help == null || help.entrees().isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.completedFuture(writeSignature(help));
        }

        @Override
        public CompletableFuture<Either<List<CompletionItem>,
                CompletionList>> completion(
                        CompletionParams params) {
            CompletionListLsp maison = documents.completion(
                    params.getTextDocument().getUri(), position(params));
            if (maison == null) {
                return CompletableFuture.completedFuture(
                        Either.forLeft(List.of()));
            }
            List<CompletionItem> elements = new ArrayList<>();
            for (CompletionItemLsp element : maison.elements()) {
                CompletionItem item = new CompletionItem();
                item.setLabel(element.label());
                item.setKind(org.eclipse.lsp4j.CompletionItemKind
                        .forValue(element.kind()));
                if (element.detail() != null) {
                    item.setDetail(element.detail());
                }
                if (element.documentation() != null) {
                    item.setDocumentation(new MarkupContent(
                            MarkupKind.MARKDOWN, element.documentation()));
                }
                if (element.insertText() != null) {
                    item.setInsertText(element.insertText());
                }
                item.setDeprecated(element.obsolete());
                elements.add(item);
            }
            return CompletableFuture.completedFuture(
                    Either.forRight(new CompletionList(
                            maison.isIncomplete(), elements)));
        }

        // ── La couverture du jalon (H-12) ───────────────────────

        @Override
        public CompletableFuture<List<Either<SymbolInformation,
                DocumentSymbol>>> documentSymbol(
                        DocumentSymbolParams params) {
            List<DocumentSymbol> symbols =
                    new ArrayList<>();
            for (SymbolLsp symbole : documents.symbols(
                    params.getTextDocument().getUri())) {
                symbols.add(writeSymbol(symbole));
            }
            List<Either<SymbolInformation,
                    DocumentSymbol>> reponse =
                            new ArrayList<>();
            for (DocumentSymbol symbole : symbols) {
                reponse.add(Either.forRight(symbole));
            }
            return CompletableFuture.completedFuture(reponse);
        }

        @Override
        public CompletableFuture<List<FoldingRange>> foldingRange(
                FoldingRangeRequestParams params) {
            List<FoldingRange> folds = new ArrayList<>();
            for (FoldLsp pli : documents.folds(
                    params.getTextDocument().getUri())) {
                // la fin du domaine est EXCLUSIVE : -1 la rend inclusive
                folds.add(new FoldingRange(
                        pli.startLine(), pli.endLine() - 1));
            }
            return CompletableFuture.completedFuture(folds);
        }

        @Override
        public CompletableFuture<List<? extends TextEdit>> formatting(
                DocumentFormattingParams params) {
            return CompletableFuture.completedFuture(writeEdits(
                    documents.formatage(params.getTextDocument().getUri(),
                            formattingOptions(params.getOptions()))));
        }

        @Override
        public CompletableFuture<List<? extends TextEdit>> rangeFormatting(
                DocumentRangeFormattingParams params) {
            return CompletableFuture.completedFuture(writeEdits(
                    documents.rangeFormatting(params.getTextDocument().getUri(),
                            readRange(params.getRange()),
                            formattingOptions(params.getOptions()))));
        }

        @Override
        public CompletableFuture<@Nullable List<org.eclipse.lsp4j.SelectionRange>>
                selectionRange(org.eclipse.lsp4j.SelectionRangeParams params) {
            if (params.getPositions().size() != 1) {
                return CompletableFuture.completedFuture(List.of());
            }
            List<RangeLsp> ranges = documents.selectionRanges(
                    params.getTextDocument().getUri(),
                    new LspPosition(params.getPositions().get(0).getLine(),
                            params.getPositions().get(0).getCharacter()));
            if (ranges.isEmpty()) {
                return CompletableFuture.completedFuture(List.of());
            }
            return CompletableFuture.completedFuture(
                    List.of(writeSelectionRanges(ranges)));
        }

        @Override
        public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(
                DocumentHighlightParams params) {
            List<org.eclipse.lsp4j.DocumentHighlight> highlights =
                    new ArrayList<>();
            for (HighlightLsp surlignage : documents.highlights(
                    params.getTextDocument().getUri(),
                    position(params))) {
                org.eclipse.lsp4j.DocumentHighlight protocole =
                        new org.eclipse.lsp4j.DocumentHighlight();
                protocole.setRange(range(surlignage.range()));
                protocole.setKind(DocumentHighlightKind
                        .forValue(surlignage.kind()));
                highlights.add(protocole);
            }
            return CompletableFuture.completedFuture(highlights);
        }

        @Override
        public CompletableFuture<@Nullable Either3<Range,
                PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(
                                PrepareRenameParams params) {
            RenamePrepareLsp preparation =
                    documents.preparerRennomage(
                            params.getTextDocument().getUri(),
                            position(params));
            if (preparation == null) {
                return CompletableFuture.completedFuture(null);
            }
            PrepareRenameResult protocole =
                    new PrepareRenameResult(
                            range(preparation.range()),
                            preparation.leadingText());
            return CompletableFuture.completedFuture(
                    Either3
                            .forSecond(protocole));
        }

        @Override
        public CompletableFuture<@Nullable WorkspaceEdit> rename(
                RenameParams params) {
            WorkspaceEditLsp espace = documents.rename(
                    params.getTextDocument().getUri(),
                    new LspPosition(params.getPosition().getLine(),
                            params.getPosition().getCharacter()),
                    params.getNewName());
            return CompletableFuture.completedFuture(ecrireEspace(espace));
        }

        @Override
        public CompletableFuture<List<Either<Command,
                CodeAction>>> codeAction(
                        CodeActionParams params) {
            List<Either<Command,
                    CodeAction>> correctifs =
                            new ArrayList<>();
            for (CodeActionLsp correctif : documents.correctifs(
                    params.getTextDocument().getUri(),
                    new RangeLsp(
                            new LspPosition(params.getRange().getStart()
                                    .getLine(), params.getRange().getStart()
                                            .getCharacter()),
                            new LspPosition(params.getRange().getEnd()
                                    .getLine(), params.getRange().getEnd()
                                            .getCharacter())))) {
                CodeAction action =
                        new CodeAction(correctif.titre());
                action.setEdit(ecrireEspace(correctif.edit()));
                correctifs.add(Either.forRight(action));
            }
            return CompletableFuture.completedFuture(correctifs);
        }

        @Override
        public CompletableFuture<@Nullable List<? extends Location>> references(
                ReferenceParams params) {
            List<Location> lieux = new ArrayList<>();
            for (LocationLsp lieu : documents.references(
                    params.getTextDocument().getUri(), position(params))) {
                lieux.add(new Location(lieu.uri(),
                        range(lieu.range())));
            }
            return CompletableFuture.completedFuture(lieux);
        }

        @Override
        public CompletableFuture<SemanticTokens> semanticTokensFull(
                SemanticTokensParams params) {
            SemanticTokensLsp tokens = documents.tokens(
                    params.getTextDocument().getUri());
            SemanticTokens protocole = new SemanticTokens();
            protocole.setResultId(tokens.identifiantResultat());
            protocole.setData(encoderTokens(tokens));
            return CompletableFuture.completedFuture(protocole);
        }

        @Override
        public CompletableFuture<SemanticTokens> semanticTokensRange(
                org.eclipse.lsp4j.SemanticTokensRangeParams params) {
            SemanticTokensLsp tokens = documents.tokensRange(
                    params.getTextDocument().getUri(),
                    readRange(params.getRange()));
            SemanticTokens protocole = new SemanticTokens();
            protocole.setResultId(tokens.identifiantResultat());
            protocole.setData(encoderTokens(tokens));
            return CompletableFuture.completedFuture(protocole);
        }

        @Override
        public CompletableFuture<@Nullable List<InlayHint>> inlayHint(
                InlayHintParams params) {
            List<InlayHint> inlayHints = new ArrayList<>();
            for (InlayHintLsp indice : documents.inlayHints(
                    params.getTextDocument().getUri(),
                    new RangeLsp(
                            new LspPosition(params.getRange().getStart()
                                    .getLine(), params.getRange().getStart()
                                            .getCharacter()),
                            new LspPosition(params.getRange().getEnd()
                                    .getLine(), params.getRange().getEnd()
                                            .getCharacter())))) {
                InlayHint protocole =
                        new InlayHint();
                protocole.setPosition(new Position(indice.position().line(),
                        indice.position().column()));
                List<InlayHintLabelPart> parties =
                        new ArrayList<>();
                for (InlayHintLsp.Part partie : indice.parties()) {
                    InlayHintLabelPart fragment =
                            new InlayHintLabelPart();
                    fragment.setValue(partie.value());
                    if (partie.target() != null) {
                        fragment.setLocation(
                                new Location(
                                        partie.target().uri(),
                                        range(partie.target().range())));
                    }
                    parties.add(fragment);
                }
                protocole.setLabel(Either.forRight(parties));
                if (indice.kind() != 0) {
                    protocole.setKind(InlayHintKind
                            .forValue(indice.kind()));
                }
                protocole.setPaddingLeft(indice.remplissageGauche());
                protocole.setPaddingRight(indice.remplissageDroite());
                inlayHints.add(protocole);
            }
            return CompletableFuture.completedFuture(inlayHints);
        }
    }

    
    
    
    
    
        
        
        
        
        
}
