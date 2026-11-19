package jo.celeris.lsp.protocole;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import jo.celeris.core.lsp.types.CodeActionLsp;
import jo.celeris.core.lsp.types.WorkspaceEditLsp;
import jo.celeris.core.lsp.types.TextEditLsp;
import jo.celeris.core.lsp.types.InlayHintLsp;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.lsp.types.FormatOptionsLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.FoldLsp;
import jo.celeris.core.lsp.types.LspPosition;
import jo.celeris.core.lsp.types.RenamePrepareLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.HighlightLsp;
import jo.celeris.core.lsp.types.SymbolLsp;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.ImplementationParams;
import org.eclipse.lsp4j.TypeDefinitionParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SelectionRangeParams;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SemanticTokensRangeParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jspecify.annotations.Nullable;







import jo.celeris.core.lsp.LspScheduler;








/**
 * Les demandes de la couverture du jalon (H-12, ADR-015) : les treize
 * méthodes nouvelles du protocole, émises au-dessus de l'ordonnanceur
 * du domaine — la session les délègue ici, son API publique ne bouge
 * pas. La garde d'initialisation reste celle de la session : aucune
 * demande ne part avant la poignée de main.
 *
 * <p>Chaque demande suit la même discipline : les paramètres du
 * protocole se construisent des types du domaine, l'émission passe par
 * l'ordonnanceur (fusion, remplacement, priorités — les interactives
 * devant, les travaux de fond derrière), la correspondance des
 * réponses se fait APRÈS le vol, jamais dedans.</p>
 */
final class CoverageRequests {

    private final LanguageServer server;

    private final LspScheduler scheduler;

    /** La garde de la session : exige l'initialisation avant l'émission. */
    private final Runnable guard;

    /** L'identité de fusion d'une demande positionnée : document et position. */
    private record Request(String uri, LspPosition position) {
    }

    CoverageRequests(LanguageServer server, LspScheduler scheduler,
            Runnable guard) {
        this.server = server;
        this.scheduler = scheduler;
        this.guard = guard;
    }

    /** Les références d'un symbole — les lieux qui le citent. */
    CompletableFuture<List<LocationLsp>> references(String uri,
            LspPosition position) {
        guard.run();
        return interactive("references", uri, position, () -> server
                .getTextDocumentService().references(new ReferenceParams(
                        identifier(uri),
                        LspMapping.writePosition(position),
                        new ReferenceContext(true))))
                .thenApply(CoverageMapping::readFlatLocations);
    }

    /**
     * La définition du TYPE (D-10, l'analyse croisée) : le lieu où le
     * type du symbole est déclaré — « aller à la définition du type »,
     * la navigation des langages typés.
     */
    CompletableFuture<List<LocationLsp>> typeDefinition(String uri,
            LspPosition position) {
        guard.run();
        return interactive("typeDefinition", uri, position, () -> server
                .getTextDocumentService().typeDefinition(
                        new TypeDefinitionParams(identifier(uri),
                                LspMapping.writePosition(position))))
                .thenApply(DocumentsMapping::readLocations);
    }

    /**
     * Les implémentations d'un symbole (D-10) : les lieux qui le
     * réalisent — les classes qui implémentent l'interface, les
     * méthodes qui surchargent.
     */
    CompletableFuture<List<LocationLsp>> implementations(String uri,
            LspPosition position) {
        guard.run();
        return interactive("implementation", uri, position, () -> server
                .getTextDocumentService().implementation(
                        new ImplementationParams(identifier(uri),
                                LspMapping.writePosition(position))))
                .thenApply(DocumentsMapping::readLocations);
    }

    /**
     * La définition SUPÉRIEURE (D-10) : la méthode que celle-ci
     * surcharge — la requête maison {@code superDefinition} des
     * serveurs Java (jdt.ls), par la voie des requêtes personnalisées
     * du protocole. Un serveur qui ne la sert rend vide : jamais
     * d'échec, la navigation retombe sur la définition simple.
     */
    CompletableFuture<List<LocationLsp>> superDefinition(String uri,
            LspPosition position) {
        guard.run();
        return interactive("superDefinition", uri, position, () -> server
                .getWorkspaceService().executeCommand(
                        new org.eclipse.lsp4j.ExecuteCommandParams(
                                "java.superDefinition",
                                java.util.List.of(uri,
                                        position.line(),
                                        position.column()))))
                .thenApply(CoverageRequests::readSuperResponse);
    }

    /**
     * La réponse de la requête maison superDefinition : le serveur
     * rend une liste de lieux (maps Gson) — lue avec défense, champ
     * par champ ; toute autre forme rend vide, jamais d'échec.
     */
    private static List<LocationLsp> readSuperResponse(
            @org.jspecify.annotations.Nullable Object reponse) {
        if (!(reponse instanceof List<?> elements)) {
            return List.of();
        }
        List<LocationLsp> lieux = new java.util.ArrayList<>();
        for (Object element : elements) {
            if (element instanceof org.eclipse.lsp4j.Location lieu) {
                lieux.add(new LocationLsp(lieu.getUri(),
                        LspMapping.readRange(lieu.getRange())));
            } else if (element instanceof java.util.Map<?, ?> carte) {
                LocationLsp lu = readLocationFromMap(carte);
                if (lu != null) {
                    lieux.add(lu);
                }
            }
        }
        return lieux;
    }

    /** Un lieu lu d'une map Gson (uri + range) — null sans ces champs. */
    private static @org.jspecify.annotations.Nullable LocationLsp readLocationFromMap(
            java.util.Map<?, ?> carte) {
        Object uri = carte.get("uri");
        Object range = carte.get("range");
        if (!(uri instanceof String text) || !(range instanceof Map<?, ?> p)) {
            return null;
        }
        Object start = p.get("start");
        Object end = p.get("end");
        if (!(start instanceof Map<?, ?> d) || !(end instanceof Map<?, ?> f)) {
            return null;
        }
        return new LocationLsp(text, LspMapping.readRange(
                new org.eclipse.lsp4j.Range(readPosition(d), readPosition(f))));
    }

    /** Une position LSP lue d'une map Gson (line/character). */
    private static org.eclipse.lsp4j.Position readPosition(java.util.Map<?, ?> p) {
        Object line = p.get("line");
        Object column = p.get("character");
        return new org.eclipse.lsp4j.Position(
                line instanceof Number n ? n.intValue() : 0,
                column instanceof Number c ? c.intValue() : 0);
    }

    /** Les symboles du document — le plan, le fil d'Ariane. */
    CompletableFuture<List<SymbolLsp>> symbols(String uri) {
        guard.run();
        return fond("symboles", uri, uri, () -> server
                .getTextDocumentService().documentSymbol(
                        new DocumentSymbolParams(identifier(uri))))
                .thenApply(CoverageMapping::readSymbols);
    }

    /** Les surlignages d'occurrences sous le curseur. */
    CompletableFuture<List<HighlightLsp>> highlights(String uri,
            LspPosition position) {
        guard.run();
        return interactive("surlignages", uri, position, () -> server
                .getTextDocumentService().documentHighlight(
                        new DocumentHighlightParams(identifier(uri),
                                LspMapping.writePosition(position))))
                .thenApply(CoverageMapping::readHighlights);
    }

    /** La préparation du renommage — null quand le serveur ne prépare pas. */
    CompletableFuture<@Nullable RenamePrepareLsp> preparerRennomage(
            String uri, LspPosition position) {
        guard.run();
        return interactive("preparerRennomage", uri, position, () -> server
                .getTextDocumentService().prepareRename(
                        new PrepareRenameParams(identifier(uri),
                                LspMapping.writePosition(position))))
                .thenApply(CoverageMapping::lirePreparationRennomage);
    }

    /** Le renommage — l'édition d'espace qui porte le nouveau nom. */
    CompletableFuture<WorkspaceEditLsp> rename(String uri,
            LspPosition position, String nouveauNom) {
        guard.run();
        return interactive("rennomage", uri, position, () -> server
                .getTextDocumentService().rename(new RenameParams(
                        identifier(uri),
                        LspMapping.writePosition(position),
                        nouveauNom)))
                .thenApply(WorkspaceMapping::readWorkspace);
    }

    /** Les correctifs rapides d'une plage — le menu d'intentions. */
    CompletableFuture<List<CodeActionLsp>> correctifs(String uri,
            RangeLsp range) {
        guard.run();
        return scheduler.schedule("correctifs", uri,
                LspScheduler.Priority.INTERACTIF, range, () -> server
                        .getTextDocumentService().codeAction(
                                new CodeActionParams(identifier(uri),
                                        LspMapping.writeRange(range),
                                        new CodeActionContext())))
                .thenApply(CoverageMapping::readCodeActions);
    }

    /** Le formatage du document entier — la capacité Formatter branchée. */
    CompletableFuture<List<TextEditLsp>> formatage(String uri,
            FormatOptionsLsp options) {
        guard.run();
        return fond("formatage", uri, uri, () -> server
                .getTextDocumentService().formatting(
                        new DocumentFormattingParams(identifier(uri),
                                formattingOptions(options))))
                .thenApply(CoverageMapping::readFormatting);
    }

    /** Le formatage d'une plage — le remaniement local. */
    CompletableFuture<List<TextEditLsp>> rangeFormatting(String uri,
            RangeLsp range, FormatOptionsLsp options) {
        guard.run();
        return fond("formatagePlage", uri, range, () -> server
                .getTextDocumentService().rangeFormatting(
                        new DocumentRangeFormattingParams(identifier(uri),
                                formattingOptions(options),
                                LspMapping.writeRange(range))))
                .thenApply(CoverageMapping::readFormatting);
    }

    /**
     * Les plis du serveur : la fin inclusive du protocole devient
     * EXCLUSIVE — la convention de l'arbre commun.
     */
    CompletableFuture<List<FoldLsp>> folds(String uri) {
        guard.run();
        return fond("plis", uri, uri, () -> server
                .getTextDocumentService().foldingRange(
                        new FoldingRangeRequestParams(identifier(uri))))
                .thenApply(CoverageMapping::readFolds);
    }

    /** Les plages de sélection — la chaîne des englobantes. */
    CompletableFuture<List<RangeLsp>> selectionRanges(String uri,
            LspPosition position) {
        guard.run();
        return interactive("plagesSelection", uri, position, () -> server
                .getTextDocumentService().selectionRange(
                        new SelectionRangeParams(identifier(uri),
                                List.of(LspMapping
                                        .writePosition(position)))))
                .thenApply(CoverageMapping::readSelectionRanges);
    }

    /**
     * Les jetons sémantiques du document entier : la charge encodée, le
     * décodage relatif-à-absolu au domaine — deux flux indépendants,
     * jamais chaînés aux diagnostics (PIEGE-18).
     */
    CompletableFuture<SemanticTokensLsp> tokens(String uri) {
        guard.run();
        return fond("jetons", uri, uri, () -> server
                .getTextDocumentService().semanticTokensFull(
                        new SemanticTokensParams(identifier(uri))))
                .thenApply(CoverageMapping::lireTokens);
    }

    /** Les jetons sémantiques d'une plage — la mise à jour locale. */
    CompletableFuture<SemanticTokensLsp> tokensRange(String uri,
            RangeLsp range) {
        guard.run();
        return fond("jetonsPlage", uri, range, () -> server
                .getTextDocumentService().semanticTokensRange(
                        new SemanticTokensRangeParams(identifier(uri),
                                LspMapping.writeRange(range))))
                .thenApply(CoverageMapping::lireTokens);
    }

    /** Les indices en ligne d'une plage — la marge, sans décalage de saisie. */
    CompletableFuture<List<InlayHintLsp>> inlayHints(String uri, RangeLsp range) {
        guard.run();
        return fond("indices", uri, range, () -> server
                .getTextDocumentService().inlayHint(
                        new InlayHintParams(identifier(uri),
                                LspMapping.writeRange(range))))
                .thenApply(CoverageMapping::readInlayHints);
    }

    /** Ordonnance une demande positionnée — priorité interactive. */
    private <T> CompletableFuture<T> interactive(String family, String uri,
            LspPosition position, Supplier<CompletableFuture<T>> emission) {
        return scheduler.schedule(family, uri,
                LspScheduler.Priority.INTERACTIF,
                new Request(uri, position), emission);
    }

    /** Ordonnance une demande de fond : symboles, formatage, plis, jetons. */
    private <T> CompletableFuture<T> fond(String family, String uri,
            Object identite, Supplier<CompletableFuture<T>> emission) {
        return scheduler.schedule(family, uri,
                LspScheduler.Priority.FOND, identite, emission);
    }

    private static TextDocumentIdentifier identifier(String uri) {
        return new TextDocumentIdentifier(uri);
    }

    /** Les options de formatage du domaine au protocole. */
    private static FormattingOptions formattingOptions(
            FormatOptionsLsp options) {
        FormattingOptions protocole = new FormattingOptions();
        protocole.setTabSize(options.tabWidth());
        protocole.setInsertSpaces(options.espaces());
        return protocole;
    }
}
