package jo.celeris.lsp.protocole;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import jo.celeris.core.lsp.types.CorrectifLsp;
import jo.celeris.core.lsp.types.EditionEspaceLsp;
import jo.celeris.core.lsp.types.EditionTexteLsp;
import jo.celeris.core.lsp.types.IndiceLsp;
import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.lsp.types.OptionsFormatageLsp;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PliLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.lsp.types.PreparationRennomageLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.SurlignageLsp;
import jo.celeris.core.lsp.types.SymboleLsp;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
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







import jo.celeris.core.lsp.OrdonnanceurLsp;








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
final class RequetesCouverture {

    private final LanguageServer serveur;

    private final OrdonnanceurLsp ordonnanceur;

    /** La garde de la session : exige l'initialisation avant l'émission. */
    private final Runnable garde;

    /** L'identité de fusion d'une demande positionnée : document et position. */
    private record Demande(String uri, PositionLsp position) {
    }

    RequetesCouverture(LanguageServer serveur, OrdonnanceurLsp ordonnanceur,
            Runnable garde) {
        this.serveur = serveur;
        this.ordonnanceur = ordonnanceur;
        this.garde = garde;
    }

    /** Les références d'un symbole — les lieux qui le citent. */
    CompletableFuture<List<LieuLsp>> references(String uri,
            PositionLsp position) {
        garde.run();
        return interactive("references", uri, position, () -> serveur
                .getTextDocumentService().references(new ReferenceParams(
                        identifiant(uri),
                        CorrespondanceLsp.ecrirePosition(position),
                        new ReferenceContext(true))))
                .thenApply(CorrespondanceCouverture::lireLieuxPlats);
    }

    /** Les symboles du document — le plan, le fil d'Ariane. */
    CompletableFuture<List<SymboleLsp>> symboles(String uri) {
        garde.run();
        return fond("symboles", uri, uri, () -> serveur
                .getTextDocumentService().documentSymbol(
                        new DocumentSymbolParams(identifiant(uri))))
                .thenApply(CorrespondanceCouverture::lireSymboles);
    }

    /** Les surlignages d'occurrences sous le curseur. */
    CompletableFuture<List<SurlignageLsp>> surlignages(String uri,
            PositionLsp position) {
        garde.run();
        return interactive("surlignages", uri, position, () -> serveur
                .getTextDocumentService().documentHighlight(
                        new DocumentHighlightParams(identifiant(uri),
                                CorrespondanceLsp.ecrirePosition(position))))
                .thenApply(CorrespondanceCouverture::lireSurlignages);
    }

    /** La préparation du renommage — null quand le serveur ne prépare pas. */
    CompletableFuture<@Nullable PreparationRennomageLsp> preparerRennomage(
            String uri, PositionLsp position) {
        garde.run();
        return interactive("preparerRennomage", uri, position, () -> serveur
                .getTextDocumentService().prepareRename(
                        new PrepareRenameParams(identifiant(uri),
                                CorrespondanceLsp.ecrirePosition(position))))
                .thenApply(CorrespondanceCouverture::lirePreparationRennomage);
    }

    /** Le renommage — l'édition d'espace qui porte le nouveau nom. */
    CompletableFuture<EditionEspaceLsp> rennomage(String uri,
            PositionLsp position, String nouveauNom) {
        garde.run();
        return interactive("rennomage", uri, position, () -> serveur
                .getTextDocumentService().rename(new RenameParams(
                        identifiant(uri),
                        CorrespondanceLsp.ecrirePosition(position),
                        nouveauNom)))
                .thenApply(CorrespondanceEspace::lireEspace);
    }

    /** Les correctifs rapides d'une plage — le menu d'intentions. */
    CompletableFuture<List<CorrectifLsp>> correctifs(String uri,
            PlageLsp plage) {
        garde.run();
        return ordonnanceur.ordonnancer("correctifs", uri,
                OrdonnanceurLsp.Priorite.INTERACTIF, plage, () -> serveur
                        .getTextDocumentService().codeAction(
                                new CodeActionParams(identifiant(uri),
                                        CorrespondanceLsp.ecrirePlage(plage),
                                        new CodeActionContext())))
                .thenApply(CorrespondanceCouverture::lireCorrectifs);
    }

    /** Le formatage du document entier — la capacité Formatter branchée. */
    CompletableFuture<List<EditionTexteLsp>> formatage(String uri,
            OptionsFormatageLsp options) {
        garde.run();
        return fond("formatage", uri, uri, () -> serveur
                .getTextDocumentService().formatting(
                        new DocumentFormattingParams(identifiant(uri),
                                optionsFormatage(options))))
                .thenApply(CorrespondanceCouverture::lireFormatage);
    }

    /** Le formatage d'une plage — le remaniement local. */
    CompletableFuture<List<EditionTexteLsp>> formatagePlage(String uri,
            PlageLsp plage, OptionsFormatageLsp options) {
        garde.run();
        return fond("formatagePlage", uri, plage, () -> serveur
                .getTextDocumentService().rangeFormatting(
                        new DocumentRangeFormattingParams(identifiant(uri),
                                optionsFormatage(options),
                                CorrespondanceLsp.ecrirePlage(plage))))
                .thenApply(CorrespondanceCouverture::lireFormatage);
    }

    /**
     * Les plis du serveur : la fin inclusive du protocole devient
     * EXCLUSIVE — la convention de l'arbre commun.
     */
    CompletableFuture<List<PliLsp>> plis(String uri) {
        garde.run();
        return fond("plis", uri, uri, () -> serveur
                .getTextDocumentService().foldingRange(
                        new FoldingRangeRequestParams(identifiant(uri))))
                .thenApply(CorrespondanceCouverture::lirePlis);
    }

    /** Les plages de sélection — la chaîne des englobantes. */
    CompletableFuture<List<PlageLsp>> plagesSelection(String uri,
            PositionLsp position) {
        garde.run();
        return interactive("plagesSelection", uri, position, () -> serveur
                .getTextDocumentService().selectionRange(
                        new SelectionRangeParams(identifiant(uri),
                                List.of(CorrespondanceLsp
                                        .ecrirePosition(position)))))
                .thenApply(CorrespondanceCouverture::lirePlagesSelection);
    }

    /**
     * Les jetons sémantiques du document entier : la charge encodée, le
     * décodage relatif-à-absolu au domaine — deux flux indépendants,
     * jamais chaînés aux diagnostics (PIEGE-18).
     */
    CompletableFuture<SemanticTokensLsp> jetons(String uri) {
        garde.run();
        return fond("jetons", uri, uri, () -> serveur
                .getTextDocumentService().semanticTokensFull(
                        new SemanticTokensParams(identifiant(uri))))
                .thenApply(CorrespondanceCouverture::lireJetons);
    }

    /** Les jetons sémantiques d'une plage — la mise à jour locale. */
    CompletableFuture<SemanticTokensLsp> jetonsPlage(String uri,
            PlageLsp plage) {
        garde.run();
        return fond("jetonsPlage", uri, plage, () -> serveur
                .getTextDocumentService().semanticTokensRange(
                        new SemanticTokensRangeParams(identifiant(uri),
                                CorrespondanceLsp.ecrirePlage(plage))))
                .thenApply(CorrespondanceCouverture::lireJetons);
    }

    /** Les indices en ligne d'une plage — la marge, sans décalage de saisie. */
    CompletableFuture<List<IndiceLsp>> indices(String uri, PlageLsp plage) {
        garde.run();
        return fond("indices", uri, plage, () -> serveur
                .getTextDocumentService().inlayHint(
                        new InlayHintParams(identifiant(uri),
                                CorrespondanceLsp.ecrirePlage(plage))))
                .thenApply(CorrespondanceCouverture::lireIndices);
    }

    /** Ordonnance une demande positionnée — priorité interactive. */
    private <T> CompletableFuture<T> interactive(String famille, String uri,
            PositionLsp position, Supplier<CompletableFuture<T>> emission) {
        return ordonnanceur.ordonnancer(famille, uri,
                OrdonnanceurLsp.Priorite.INTERACTIF,
                new Demande(uri, position), emission);
    }

    /** Ordonnance une demande de fond : symboles, formatage, plis, jetons. */
    private <T> CompletableFuture<T> fond(String famille, String uri,
            Object identite, Supplier<CompletableFuture<T>> emission) {
        return ordonnanceur.ordonnancer(famille, uri,
                OrdonnanceurLsp.Priorite.FOND, identite, emission);
    }

    private static TextDocumentIdentifier identifiant(String uri) {
        return new TextDocumentIdentifier(uri);
    }

    /** Les options de formatage du domaine au protocole. */
    private static FormattingOptions optionsFormatage(
            OptionsFormatageLsp options) {
        FormattingOptions protocole = new FormattingOptions();
        protocole.setTabSize(options.largeurTab());
        protocole.setInsertSpaces(options.espaces());
        return protocole;
    }
}
