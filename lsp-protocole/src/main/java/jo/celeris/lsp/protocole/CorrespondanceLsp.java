package jo.celeris.lsp.protocole;

import java.util.ArrayList;
import java.util.List;

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

import jo.celeris.core.lsp.CapacitesServeur;
import jo.celeris.core.lsp.DiagnosticLsp;
import jo.celeris.core.lsp.PlageLsp;
import jo.celeris.core.lsp.PositionLsp;

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
final class CorrespondanceLsp {

    private CorrespondanceLsp() {
        // la correspondance est un choix de fonctions pures, jamais une instance
    }

    /** La position du domaine à envoyer au serveur. */
    static Position ecrirePosition(PositionLsp position) {
        return new Position(position.ligne(), position.colonne());
    }

    /** La position lue du serveur — absente, elle vaut l'origine. */
    static PositionLsp lirePosition(@Nullable Position position) {
        return position == null ? new PositionLsp(0, 0)
                : new PositionLsp(position.getLine(), position.getCharacter());
    }

    /** La plage lue du serveur — absente, elle vaut la plage vide à l'origine. */
    static PlageLsp lirePlage(@Nullable Range plage) {
        if (plage == null) {
            return new PlageLsp(new PositionLsp(0, 0), new PositionLsp(0, 0));
        }
        return new PlageLsp(lirePosition(plage.getStart()),
                lirePosition(plage.getEnd()));
    }

    /** Les diagnostics publiés d'un serveur. */
    static List<DiagnosticLsp> lireDiagnostics(List<Diagnostic> diagnostics) {
        List<DiagnosticLsp> lus = new ArrayList<>();
        for (Diagnostic diagnostic : diagnostics) {
            lus.add(lireDiagnostic(diagnostic));
        }
        return List.copyOf(lus);
    }

    /** Le diagnostic lu — gravité absente vaut zéro, message vide si absent. */
    static DiagnosticLsp lireDiagnostic(Diagnostic diagnostic) {
        return new DiagnosticLsp(lirePlage(diagnostic.getRange()),
                graviteDe(diagnostic.getSeverity()),
                messageDe(diagnostic.getMessage()),
                texteDeCode(diagnostic.getCode()),
                diagnostic.getSource());
    }

    private static int graviteDe(@Nullable DiagnosticSeverity gravite) {
        return gravite == null ? 0 : gravite.getValue();
    }

    /** Le message du protocole : la chaîne nue ou la valeur du contenu balisé. */
    static String messageDe(@Nullable Either<String, MarkupContent> contenu) {
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
    static @Nullable String documentationDe(
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
    private static @Nullable String texteDeCode(
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
    static List<DiagnosticLsp> diagnosticsPublies(PublishDiagnosticsParams params) {
        if (params.getDiagnostics() == null) {
            return List.of();
        }
        return lireDiagnostics(params.getDiagnostics());
    }

    /** Les capacités négociées : ce que le serveur a répondu à la poignée de main. */
    static CapacitesServeur lireCapacites(@Nullable ServerCapabilities capacites) {
        if (capacites == null) {
            return CapacitesServeur.de(-1, false, false, false, false, false,
                    false, false, false, false, List.of(), List.of());
        }
        return CapacitesServeur.de(
                genreSynchro(capacites.getTextDocumentSync()),
                capacites.getCompletionProvider() != null,
                capacites.getHoverProvider() != null,
                capacites.getSignatureHelpProvider() != null,
                capacites.getDefinitionProvider() != null,
                capacites.getReferencesProvider() != null,
                capacites.getRenameProvider() != null,
                capacites.getCodeActionProvider() != null,
                capacites.getDocumentFormattingProvider() != null,
                capacites.getInlayHintProvider() != null,
                legende(capacites, true),
                legende(capacites, false));
    }

    private static int genreSynchro(
            @Nullable Either<TextDocumentSyncKind, TextDocumentSyncOptions> synchro) {
        if (synchro == null) {
            return -1;
        }
        if (synchro.isLeft()) {
            return synchro.getLeft() == null ? -1 : genreDe(synchro.getLeft());
        }
        TextDocumentSyncOptions options = synchro.getRight();
        return options == null || options.getChange() == null
                ? -1 : genreDe(options.getChange());
    }

    /** La valeur du genre au protocole : aucun 0, complet 1, incrémental 2. */
    private static int genreDe(TextDocumentSyncKind genre) {
        return switch (genre) {
            case None -> 0;
            case Full -> 1;
            case Incremental -> 2;
        };
    }

    /** La légende des jetons : les types (vrai) ou les modificateurs (faux). */
    private static List<String> legende(ServerCapabilities capacites, boolean types) {
        if (capacites.getSemanticTokensProvider() == null
                || capacites.getSemanticTokensProvider().getLegend() == null) {
            return List.of();
        }
        List<String> lue = types
                ? capacites.getSemanticTokensProvider().getLegend().getTokenTypes()
                : capacites.getSemanticTokensProvider().getLegend().getTokenModifiers();
        if (lue == null) {
            return List.of();
        }
        return List.copyOf(lue);
    }
}
