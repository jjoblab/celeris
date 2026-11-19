package jo.celeris.lsp.protocole;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import jo.celeris.core.lsp.types.DocumentEditLsp;
import jo.celeris.core.lsp.types.WorkspaceEditLsp;
import jo.celeris.core.lsp.types.TextEditLsp;
import jo.celeris.core.lsp.types.FormatOptionsLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.LspPosition;
import jo.celeris.core.lsp.types.SemanticTokenLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.SymbolLsp;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * L'écriture du domaine vers le protocole (ADR-015) : les
 * convertisseurs du monteur de référence — plages, signatures,
 * symboles, éditions, espaces et jetons sémantiques — nés de la
 * scission des plafonds de ServeurDocuments (ADR-002).
 */
final class DocumentsWriting {

    private DocumentsWriting() {
    }

/** La position du domaine des paramètres de la bibliothèque. */
    static LspPosition position(
        org.eclipse.lsp4j.TextDocumentPositionParams params) {
    return new LspPosition(params.getPosition().getLine(),
            params.getPosition().getCharacter());
    }

/** La plage du protocole au domaine — jamais nulle ici, l'appel est né d'elle. */
    static RangeLsp readRange(Range range) {
    return new RangeLsp(
            new LspPosition(range.getStart().getLine(),
                    range.getStart().getCharacter()),
            new LspPosition(range.getEnd().getLine(),
                    range.getEnd().getCharacter()));
    }

/**
     * La chaîne des plages englobantes au protocole : la plus large
     * devient le parent de la précédente, la plus intime répond.
     */
    static org.eclipse.lsp4j.SelectionRange writeSelectionRanges(
        List<RangeLsp> ranges) {
    org.eclipse.lsp4j.SelectionRange chaine =
            new org.eclipse.lsp4j.SelectionRange(
                    range(ranges.get(ranges.size() - 1)), null);
    for (int i = ranges.size() - 2; i >= 0; i--) {
        chaine = new org.eclipse.lsp4j.SelectionRange(
                range(ranges.get(i)), chaine);
    }
    return chaine;
    }

/** La plage du domaine au Range de la bibliothèque — null reste null. */
    static @Nullable Range range(@Nullable RangeLsp range) {
    return range == null ? null : new Range(
            new Position(range.start().line(), range.start().column()),
            new Position(range.end().line(), range.end().column()));
    }

/** L'aide aux paramètres du domaine à la réponse du protocole. */
    static SignatureHelp writeSignature(SignatureLsp help) {
    List<SignatureInformation> signatures = new ArrayList<>();
    for (SignatureLsp.Entry input : help.entrees()) {
        List<ParameterInformation> parameters = new ArrayList<>();
        for (SignatureLsp.Parameter parametre : input.parameters()) {
            parameters.add(new ParameterInformation(parametre.label()));
        }
        signatures.add(new SignatureInformation(input.label(),
                input.documentation(), parameters));
    }
    SignatureHelp reponse = new SignatureHelp();
    reponse.setSignatures(signatures);
    if (help.signatureActiveIndex() >= 0
            && help.signatureActiveIndex() < signatures.size()) {
        reponse.setActiveSignature(help.signatureActiveIndex());
    }
    if (help.parametreCourant() >= 0) {
        reponse.setActiveParameter(help.parametreCourant());
    }
    return reponse;
    }

/** Le symbole du domaine au symbole du protocole, ses enfants suivent. */
    static DocumentSymbol writeSymbol(
            SymbolLsp symbole) {
        List<DocumentSymbol> enfants = new ArrayList<>();
        for (SymbolLsp enfant : symbole.enfants()) {
            enfants.add(writeSymbol(enfant));
        }
        return new DocumentSymbol(symbole.nom(),
                SymbolKind.forValue(symbole.kind()),
                range(symbole.range()),
                symbole.plageSelection() == null ? range(symbole.range())
                        : range(symbole.plageSelection()),
                null, enfants);
    }

/** Les éditions du domaine aux éditions du protocole. */
    static List<TextEdit> writeEdits(
            List<TextEditLsp> edits) {
        List<TextEdit> protocole = new ArrayList<>();
        for (TextEditLsp edit : edits) {
            protocole.add(new TextEdit(
                    range(edit.range()), edit.newText()));
        }
        return protocole;
    }

/**
     * L'édition d'espace du domaine au protocole : la forme moderne
     * {@code documentChanges} porte les versions, les gabarits y
     * voyagent en édits de gabarit — le aller-retour ne perd rien.
     */
    static @Nullable WorkspaceEdit ecrireEspace(
            WorkspaceEditLsp espace) {
        if (espace.documents().isEmpty() && espace.templates().isEmpty()) {
            return null;
        }
        WorkspaceEdit protocole = new WorkspaceEdit();
        List<Either<TextDocumentEdit,
                org.eclipse.lsp4j.ResourceOperation>> changements =
                        new ArrayList<>();
        for (DocumentEditLsp document : espace.documents()) {
            List<Either<TextEdit,
                    org.eclipse.lsp4j.SnippetTextEdit>> edits =
                            new ArrayList<>();
            for (TextEditLsp edit : document.edits()) {
                edits.add(Either.forLeft(new TextEdit(
                        range(edit.range()), edit.newText())));
            }
            changements.add(Either.forLeft(new TextDocumentEdit(
                    new org.eclipse.lsp4j.VersionedTextDocumentIdentifier(
                            document.uri(), document.version()),
                    edits)));
        }
        for (jo.celeris.core.lsp.types.TemplateEditLsp gabarit
                : espace.templates()) {
            List<Either<TextEdit,
                    org.eclipse.lsp4j.SnippetTextEdit>> edits =
                            new ArrayList<>();
            Range ancrage = new Range(
                    new Position(gabarit.position().line(),
                            gabarit.position().column()),
                    new Position(gabarit.position().line(),
                            gabarit.position().column()));
            org.eclipse.lsp4j.StringValue body =
                    new org.eclipse.lsp4j.StringValue();
            body.setValue(gabarit.body());
            edits.add(Either.forRight(
                    new org.eclipse.lsp4j.SnippetTextEdit(ancrage,
                            body)));
            changements.add(Either.forLeft(new TextDocumentEdit(
                    new org.eclipse.lsp4j.VersionedTextDocumentIdentifier(
                            gabarit.uri(), -1),
                    edits)));
        }
        protocole.setDocumentChanges(changements);
        return protocole;
    }

/** Les options de formatage du protocole au domaine. */
    static FormatOptionsLsp formattingOptions(
            org.eclipse.lsp4j.FormattingOptions options) {
        return new FormatOptionsLsp(options.getTabSize(),
                options.isInsertSpaces());
    }

/** Les jetons absolus du domaine redeviennent la charge encodée du protocole. */
    static List<Integer> encoderTokens(SemanticTokensLsp tokens) {
        List<Integer> donnees = new ArrayList<>();
        int lignePrecedente = 0;
        int colonnePrecedente = 0;
        for (SemanticTokenLsp token : tokens.tokens()) {
            donnees.add(token.line() - lignePrecedente);
            donnees.add(token.line() == lignePrecedente
                    ? token.column() - colonnePrecedente
                    : token.column());
            donnees.add(token.length());
            donnees.add(token.type());
            donnees.add(token.modificateurs());
            lignePrecedente = token.line();
            colonnePrecedente = token.column();
        }
        return donnees;
    }
}
