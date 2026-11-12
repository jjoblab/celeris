package jo.celeris.lsp.protocole;

import static jo.celeris.lsp.protocole.EcritureDocuments.position;
import static jo.celeris.lsp.protocole.EcritureDocuments.lirePlage;
import static jo.celeris.lsp.protocole.EcritureDocuments.ecrirePlagesSelection;
import static jo.celeris.lsp.protocole.EcritureDocuments.plage;
import static jo.celeris.lsp.protocole.EcritureDocuments.ecrireSignature;
import static jo.celeris.lsp.protocole.EcritureDocuments.ecrireSymbole;
import static jo.celeris.lsp.protocole.EcritureDocuments.ecrireEditions;
import static jo.celeris.lsp.protocole.EcritureDocuments.ecrireEspace;
import static jo.celeris.lsp.protocole.EcritureDocuments.optionsFormatage;
import static jo.celeris.lsp.protocole.EcritureDocuments.encoderJetons;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
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

import jo.celeris.core.lsp.types.CorrectifLsp;
import jo.celeris.core.lsp.types.EditionEspaceLsp;
import jo.celeris.core.lsp.types.IndiceLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PliLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.lsp.types.PreparationRennomageLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.SurlignageLsp;
import jo.celeris.core.lsp.types.SurvolLsp;
import jo.celeris.core.lsp.types.SymboleLsp;

/**
 * L'adaptateur du SPI du domaine au service de documents de la
 * bibliothèque (ADR-015) : le serveur de référence du montage, la
 * correspondance inverse y vit — le terrain ne connaît que les types
 * du domaine, le protocole reste ici. La couverture du jalon (H-12)
 * y entre méthode par méthode : chaque repli vide du SPI reçoit sa
 * réponse vide du protocole.
 */
final class ServeurDocuments implements LanguageServer {

    private final ServeurEmbarque.Documents documents;

    ServeurDocuments(ServeurEmbarque.Documents documents) {
        this.documents = documents;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(
            InitializeParams params) {
        ServerCapabilities capacites = new ServerCapabilities();
        capacites.setHoverProvider(Either.forLeft(Boolean.TRUE));
        capacites.setSignatureHelpProvider(
                new SignatureHelpOptions(List.of("(")));
        // la couverture du jalon (H-12) s'annonce : le terrain qui
        // n'en sert pas répond ses vides du protocole
        capacites.setDocumentSymbolProvider(Either.forLeft(Boolean.TRUE));
        capacites.setFoldingRangeProvider(Either.forLeft(Boolean.TRUE));
        capacites.setDocumentFormattingProvider(Boolean.TRUE);
        capacites.setDocumentHighlightProvider(Either.forLeft(Boolean.TRUE));
        capacites.setReferencesProvider(Either.forLeft(Boolean.TRUE));
        capacites.setRenameProvider(Either.forLeft(Boolean.TRUE));
        capacites.setCodeActionProvider(Either.forLeft(Boolean.TRUE));
        return CompletableFuture.completedFuture(
                new InitializeResult(capacites));
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
        return new ServiceDocuments();
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
    private final class ServiceDocuments implements TextDocumentService {

        @Override
        public void didOpen(DidOpenTextDocumentParams params) {
            documents.ouvrir(params.getTextDocument().getUri(),
                    params.getTextDocument().getLanguageId(),
                    params.getTextDocument().getText());
        }

        @Override
        public void didChange(DidChangeTextDocumentParams params) {
            List<org.eclipse.lsp4j.TextDocumentContentChangeEvent> changements =
                    params.getContentChanges();
            if (!changements.isEmpty()) {
                documents.changer(params.getTextDocument().getUri(),
                        params.getTextDocument().getVersion(),
                        changements.get(changements.size() - 1).getText());
            }
        }

        @Override
        public void didClose(DidCloseTextDocumentParams params) {
            documents.fermer(params.getTextDocument().getUri());
        }

        @Override
        public void didSave(DidSaveTextDocumentParams params) {
            // le terrain ne sauvegarde jamais
        }

        @Override
        public CompletableFuture<@Nullable Hover> hover(HoverParams params) {
            SurvolLsp survol = documents.survol(
                    params.getTextDocument().getUri(), position(params));
            if (survol == null || survol.texte().isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            MarkupContent contenu = new MarkupContent(
                    survol.genre().isEmpty() ? MarkupKind.MARKDOWN : survol.genre(),
                    survol.texte());
            return CompletableFuture.completedFuture(
                    new Hover(contenu, plage(survol.plage())));
        }

        @Override
        public CompletableFuture<@Nullable SignatureHelp> signatureHelp(
                SignatureHelpParams params) {
            SignatureLsp aide = documents.signature(
                    params.getTextDocument().getUri(), position(params));
            if (aide == null || aide.entrees().isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.completedFuture(ecrireSignature(aide));
        }

        // ── La couverture du jalon (H-12) ───────────────────────

        @Override
        public CompletableFuture<List<Either<SymbolInformation,
                DocumentSymbol>>> documentSymbol(
                        DocumentSymbolParams params) {
            List<DocumentSymbol> symboles =
                    new ArrayList<>();
            for (SymboleLsp symbole : documents.symboles(
                    params.getTextDocument().getUri())) {
                symboles.add(ecrireSymbole(symbole));
            }
            List<Either<SymbolInformation,
                    DocumentSymbol>> reponse =
                            new ArrayList<>();
            for (DocumentSymbol symbole : symboles) {
                reponse.add(Either.forRight(symbole));
            }
            return CompletableFuture.completedFuture(reponse);
        }

        @Override
        public CompletableFuture<List<FoldingRange>> foldingRange(
                FoldingRangeRequestParams params) {
            List<FoldingRange> plis = new ArrayList<>();
            for (PliLsp pli : documents.plis(
                    params.getTextDocument().getUri())) {
                // la fin du domaine est EXCLUSIVE : -1 la rend inclusive
                plis.add(new FoldingRange(
                        pli.ligneDebut(), pli.ligneFin() - 1));
            }
            return CompletableFuture.completedFuture(plis);
        }

        @Override
        public CompletableFuture<List<? extends TextEdit>> formatting(
                DocumentFormattingParams params) {
            return CompletableFuture.completedFuture(ecrireEditions(
                    documents.formatage(params.getTextDocument().getUri(),
                            optionsFormatage(params.getOptions()))));
        }

        @Override
        public CompletableFuture<List<? extends TextEdit>> rangeFormatting(
                DocumentRangeFormattingParams params) {
            return CompletableFuture.completedFuture(ecrireEditions(
                    documents.formatagePlage(params.getTextDocument().getUri(),
                            lirePlage(params.getRange()),
                            optionsFormatage(params.getOptions()))));
        }

        @Override
        public CompletableFuture<@Nullable List<org.eclipse.lsp4j.SelectionRange>>
                selectionRange(org.eclipse.lsp4j.SelectionRangeParams params) {
            if (params.getPositions().size() != 1) {
                return CompletableFuture.completedFuture(List.of());
            }
            List<PlageLsp> plages = documents.plagesSelection(
                    params.getTextDocument().getUri(),
                    new PositionLsp(params.getPositions().get(0).getLine(),
                            params.getPositions().get(0).getCharacter()));
            if (plages.isEmpty()) {
                return CompletableFuture.completedFuture(List.of());
            }
            return CompletableFuture.completedFuture(
                    List.of(ecrirePlagesSelection(plages)));
        }

        @Override
        public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(
                DocumentHighlightParams params) {
            List<org.eclipse.lsp4j.DocumentHighlight> surlignages =
                    new ArrayList<>();
            for (SurlignageLsp surlignage : documents.surlignages(
                    params.getTextDocument().getUri(),
                    position(params))) {
                org.eclipse.lsp4j.DocumentHighlight protocole =
                        new org.eclipse.lsp4j.DocumentHighlight();
                protocole.setRange(plage(surlignage.plage()));
                protocole.setKind(DocumentHighlightKind
                        .forValue(surlignage.genre()));
                surlignages.add(protocole);
            }
            return CompletableFuture.completedFuture(surlignages);
        }

        @Override
        public CompletableFuture<@Nullable Either3<Range,
                PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(
                                PrepareRenameParams params) {
            PreparationRennomageLsp preparation =
                    documents.preparerRennomage(
                            params.getTextDocument().getUri(),
                            position(params));
            if (preparation == null) {
                return CompletableFuture.completedFuture(null);
            }
            PrepareRenameResult protocole =
                    new PrepareRenameResult(
                            plage(preparation.plage()),
                            preparation.texteAmorce());
            return CompletableFuture.completedFuture(
                    Either3
                            .forSecond(protocole));
        }

        @Override
        public CompletableFuture<@Nullable WorkspaceEdit> rename(
                RenameParams params) {
            EditionEspaceLsp espace = documents.rennomage(
                    params.getTextDocument().getUri(),
                    new PositionLsp(params.getPosition().getLine(),
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
            for (CorrectifLsp correctif : documents.correctifs(
                    params.getTextDocument().getUri(),
                    new PlageLsp(
                            new PositionLsp(params.getRange().getStart()
                                    .getLine(), params.getRange().getStart()
                                            .getCharacter()),
                            new PositionLsp(params.getRange().getEnd()
                                    .getLine(), params.getRange().getEnd()
                                            .getCharacter())))) {
                CodeAction action =
                        new CodeAction(correctif.titre());
                action.setEdit(ecrireEspace(correctif.edition()));
                correctifs.add(Either.forRight(action));
            }
            return CompletableFuture.completedFuture(correctifs);
        }

        @Override
        public CompletableFuture<@Nullable List<? extends Location>> references(
                ReferenceParams params) {
            List<Location> lieux = new ArrayList<>();
            for (LieuLsp lieu : documents.references(
                    params.getTextDocument().getUri(), position(params))) {
                lieux.add(new Location(lieu.uri(),
                        plage(lieu.plage())));
            }
            return CompletableFuture.completedFuture(lieux);
        }

        @Override
        public CompletableFuture<SemanticTokens> semanticTokensFull(
                SemanticTokensParams params) {
            SemanticTokensLsp jetons = documents.jetons(
                    params.getTextDocument().getUri());
            SemanticTokens protocole = new SemanticTokens();
            protocole.setResultId(jetons.identifiantResultat());
            protocole.setData(encoderJetons(jetons));
            return CompletableFuture.completedFuture(protocole);
        }

        @Override
        public CompletableFuture<SemanticTokens> semanticTokensRange(
                org.eclipse.lsp4j.SemanticTokensRangeParams params) {
            SemanticTokensLsp jetons = documents.jetonsPlage(
                    params.getTextDocument().getUri(),
                    lirePlage(params.getRange()));
            SemanticTokens protocole = new SemanticTokens();
            protocole.setResultId(jetons.identifiantResultat());
            protocole.setData(encoderJetons(jetons));
            return CompletableFuture.completedFuture(protocole);
        }

        @Override
        public CompletableFuture<@Nullable List<InlayHint>> inlayHint(
                InlayHintParams params) {
            List<InlayHint> indices = new ArrayList<>();
            for (IndiceLsp indice : documents.indices(
                    params.getTextDocument().getUri(),
                    new PlageLsp(
                            new PositionLsp(params.getRange().getStart()
                                    .getLine(), params.getRange().getStart()
                                            .getCharacter()),
                            new PositionLsp(params.getRange().getEnd()
                                    .getLine(), params.getRange().getEnd()
                                            .getCharacter())))) {
                InlayHint protocole =
                        new InlayHint();
                protocole.setPosition(new Position(indice.position().ligne(),
                        indice.position().colonne()));
                List<InlayHintLabelPart> parties =
                        new ArrayList<>();
                for (IndiceLsp.Partie partie : indice.parties()) {
                    InlayHintLabelPart fragment =
                            new InlayHintLabelPart();
                    fragment.setValue(partie.valeur());
                    if (partie.cible() != null) {
                        fragment.setLocation(
                                new Location(
                                        partie.cible().uri(),
                                        plage(partie.cible().plage())));
                    }
                    parties.add(fragment);
                }
                protocole.setLabel(Either.forRight(parties));
                if (indice.genre() != 0) {
                    protocole.setKind(InlayHintKind
                            .forValue(indice.genre()));
                }
                protocole.setPaddingLeft(indice.remplissageGauche());
                protocole.setPaddingRight(indice.remplissageDroite());
                indices.add(protocole);
            }
            return CompletableFuture.completedFuture(indices);
        }
    }

    
    
    
    
    
        
        
        
        
        
}
