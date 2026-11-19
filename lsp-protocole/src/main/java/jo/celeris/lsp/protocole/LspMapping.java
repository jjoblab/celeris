package jo.celeris.lsp.protocole;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.LspPosition;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jspecify.annotations.Nullable;

import jo.celeris.core.lsp.ServerCapsLsp;




/**
 * La correspondance des structures et de la négociation (ADR-015) :
 * positions, plages, diagnostics et capacités y deviennent les types
 * immuables du domaine de core-lsp — la bibliothèque ne paraît dans
 * aucun type rendu. Les champs absents suivent la discipline de la
 * voie maison : jamais devinés, nuls ou vides selon le domaine.
 *
 * <p>Le lecteur des trois formes du protocole (PIEGE-16, le
 * redressement M8.6) vit ici : la chaîne nue, le contenu balisé
 * {@code {kind, value}} et la liste de contenus balisés.</p>
 */
final class LspMapping {

    private LspMapping() {
        // la correspondance est un choix de fonctions pures, jamais une instance
    }

    /** La position du domaine à envoyer au serveur. */
    static Position writePosition(LspPosition position) {
        return new Position(position.line(), position.column());
    }

    /** La position lue du serveur — absente, elle vaut l'origine. */
    static LspPosition readPosition(@Nullable Position position) {
        return position == null ? new LspPosition(0, 0)
                : new LspPosition(position.getLine(), position.getCharacter());
    }

    /** La plage du domaine à envoyer au serveur. */
    static Range writeRange(RangeLsp range) {
        return new Range(writePosition(range.start()),
                writePosition(range.end()));
    }

    /** La plage lue du serveur — absente, elle vaut la plage vide à l'origine. */
    static RangeLsp readRange(@Nullable Range range) {
        if (range == null) {
            return new RangeLsp(new LspPosition(0, 0), new LspPosition(0, 0));
        }
        return new RangeLsp(readPosition(range.getStart()),
                readPosition(range.getEnd()));
    }

    /** Les diagnostics publiés d'un serveur. */
    static List<DiagnosticLsp> readDiagnostics(List<Diagnostic> diagnostics) {
        List<DiagnosticLsp> lus = new ArrayList<>();
        for (Diagnostic diagnostic : diagnostics) {
            lus.add(readDiagnostic(diagnostic));
        }
        return List.copyOf(lus);
    }

    /** Le diagnostic lu — gravité absente vaut zéro, message vide si absent. */
    static DiagnosticLsp readDiagnostic(Diagnostic diagnostic) {
        return new DiagnosticLsp(readRange(diagnostic.getRange()),
                severityOf(diagnostic.getSeverity()),
                messageOf(diagnostic.getMessage()),
                textOfCode(diagnostic.getCode()),
                diagnostic.getSource());
    }

    private static int severityOf(@Nullable DiagnosticSeverity severity) {
        return severity == null ? 0 : severity.getValue();
    }

    /** Le message du protocole : la chaîne nue ou la valeur du contenu balisé. */
    static String messageOf(@Nullable Either<String, MarkupContent> contenu) {
        if (contenu == null) {
            return "";
        }
        if (contenu.isRight()) {
            MarkupContent balise = contenu.getRight();
            return balise == null || balise.getValue() == null ? "" : balise.getValue();
        }
        return contenu.getLeft() == null ? "" : contenu.getLeft();
    }

    /** La documentation dans ses formes du protocole : chaîne, contenu balisé — ou absente. */
    static @Nullable String documentationOf(
            @Nullable Either<String, MarkupContent> documentation) {
        if (documentation == null) {
            return null;
        }
        if (documentation.isRight()) {
            MarkupContent balise = documentation.getRight();
            return balise == null ? null : balise.getValue();
        }
        return documentation.getLeft();
    }

    /** Le code du protocole : la chaîne ou l'entier rendu en texte, null si absent. */
    private static @Nullable String textOfCode(
            @Nullable Either<String, Integer> code) {
        if (code == null) {
            return null;
        }
        if (code.isRight()) {
            Integer entier = code.getRight();
            return entier == null ? null : String.valueOf(entier);
        }
        return code.getLeft();
    }

    /** Le diagnostic du domaine relu pour l'auditeur — jamais exposé par la bibliothèque. */
    static List<DiagnosticLsp> publishedDiagnostics(PublishDiagnosticsParams params) {
        if (params.getDiagnostics() == null) {
            return List.of();
        }
        return readDiagnostics(params.getDiagnostics());
    }

    /** Les capacités négociées : ce que le serveur a répondu à la poignée de main. */
    static ServerCapsLsp readCapabilities(
            @Nullable ServerCapabilities capabilities) {
        if (capabilities == null) {
            return ServerCapsLsp.empty();
        }
        return ServerCapsLsp.of(
                syncKindOf(capabilities.getTextDocumentSync()),
                capabilities.getCompletionProvider() != null,
                capabilities.getHoverProvider() != null,
                capabilities.getSignatureHelpProvider() != null,
                capabilities.getDefinitionProvider() != null,
                capabilities.getReferencesProvider() != null,
                capabilities.getDocumentHighlightProvider() != null,
                capabilities.getDocumentSymbolProvider() != null,
                capabilities.getFoldingRangeProvider() != null,
                capabilities.getSelectionRangeProvider() != null,
                capabilities.getRenameProvider() != null,
                capabilities.getCodeActionProvider() != null,
                capabilities.getDocumentFormattingProvider() != null,
                capabilities.getInlayHintProvider() != null,
                legend(capabilities, true),
                legend(capabilities, false));
    }

    private static int syncKindOf(
            @Nullable Either<TextDocumentSyncKind, TextDocumentSyncOptions> synchro) {
        if (synchro == null) {
            return -1;
        }
        if (synchro.isLeft()) {
            return synchro.getLeft() == null ? -1 : kindOf(synchro.getLeft());
        }
        TextDocumentSyncOptions options = synchro.getRight();
        return options == null || options.getChange() == null
                ? -1 : kindOf(options.getChange());
    }

    /** La valeur du genre au protocole : aucun 0, complet 1, incrémental 2. */
    private static int kindOf(TextDocumentSyncKind kind) {
        return switch (kind) {
            case None -> 0;
            case Full -> 1;
            case Incremental -> 2;
        };
    }

    /** La légende des jetons : les types (vrai) ou les modificateurs (faux). */
    private static List<String> legend(
            ServerCapabilities capabilities, boolean types) {
        if (capabilities.getSemanticTokensProvider() == null
                || capabilities.getSemanticTokensProvider().getLegend() == null) {
            return List.of();
        }
        List<String> lue = types
                ? capabilities.getSemanticTokensProvider().getLegend().getTokenTypes()
                : capabilities.getSemanticTokensProvider().getLegend().getTokenModifiers();
        if (lue == null) {
            return List.of();
        }
        return List.copyOf(lue);
    }
}
