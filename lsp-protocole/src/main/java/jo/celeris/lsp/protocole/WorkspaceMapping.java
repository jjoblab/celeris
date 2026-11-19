package jo.celeris.lsp.protocole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jo.celeris.core.lsp.types.DocumentEditLsp;
import jo.celeris.core.lsp.types.WorkspaceEditLsp;
import jo.celeris.core.lsp.types.TemplateEditLsp;
import jo.celeris.core.lsp.types.TextEditLsp;
import org.eclipse.lsp4j.SnippetTextEdit;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jspecify.annotations.Nullable;






/**
 * La correspondance des éditions d'espace de travail (H-12, ADR-015) :
 * la réponse {@code WorkspaceEdit} — forme moderne documentChanges ou
 * carte héritée changes — devient l'édition d'espace du domaine, les
 * édits de gabarit (SnippetTextEdit) rejoignent leur liste — le bloqué
 * historique de la montée de version, débloqué.
 */
final class WorkspaceMapping {

    private WorkspaceMapping() {
        // la correspondance est un choix de fonctions pures, jamais une instance
    }

    /** L'édition d'espace lue : documentChanges d'abord, la carte héritée ensuite. */
    static WorkspaceEditLsp readWorkspace(@Nullable WorkspaceEdit edit) {
        if (edit == null) {
            return WorkspaceEditLsp.EMPTY;
        }
        List<DocumentEditLsp> documents = new ArrayList<>();
        List<TemplateEditLsp> templates = new ArrayList<>();
        if (edit.getDocumentChanges() != null) {
            for (Either<TextDocumentEdit, org.eclipse.lsp4j.ResourceOperation>
                    changement : edit.getDocumentChanges()) {
                if (changement != null && changement.isLeft()
                        && changement.getLeft() != null) {
                    readDocumentEdit(changement.getLeft(), documents, templates);
                }
            }
        }
        if (documents.isEmpty() && templates.isEmpty()
                && edit.getChanges() != null) {
            for (Map.Entry<String, List<TextEdit>> input
                    : edit.getChanges().entrySet()) {
                documents.add(new DocumentEditLsp(input.getKey(), -1,
                        readEdits(input.getValue())));
            }
        }
        return new WorkspaceEditLsp(documents, templates);
    }

    /**
     * Les éditions d'un document : les éditions de texte suivent leur
     * liste, les édits de gabarit de la 1.0.0 (SnippetTextEdit)
     * rejoignent la liste de l'ESPACE — la forme du domaine, le bloqué
     * historique débloqué par la montée de version (ADR-015).
     */
    private static void readDocumentEdit(TextDocumentEdit documentEdit,
            List<DocumentEditLsp> documents,
            List<TemplateEditLsp> templates) {
        List<TextEditLsp> edits = new ArrayList<>();
        for (Either<TextEdit, SnippetTextEdit> either : documentEdit.getEdits()) {
            if (either == null) {
                continue;
            }
            if (either.isLeft() && either.getLeft() != null) {
                edits.add(readEdit(either.getLeft()));
            } else if (either.isRight() && either.getRight() != null) {
                SnippetTextEdit gabarit = either.getRight();
                templates.add(new TemplateEditLsp(uriOf(documentEdit),
                        LspMapping.readPosition(
                                gabarit.getRange() == null ? null
                                        : gabarit.getRange().getStart()),
                        gabarit.getSnippet() == null
                                || gabarit.getSnippet().getValue() == null ? ""
                                : gabarit.getSnippet().getValue()));
            }
        }
        documents.add(new DocumentEditLsp(uriOf(documentEdit),
                versionOf(documentEdit), edits));
    }

    private static String uriOf(TextDocumentEdit edit) {
        return edit.getTextDocument() == null
                || edit.getTextDocument().getUri() == null ? ""
                        : edit.getTextDocument().getUri();
    }

    private static int versionOf(TextDocumentEdit edit) {
        return edit.getTextDocument() == null
                || edit.getTextDocument().getVersion() == null ? -1
                        : edit.getTextDocument().getVersion();
    }

    private static TextEditLsp readEdit(TextEdit edit) {
        return new TextEditLsp(LspMapping.readRange(edit.getRange()),
                edit.getNewText() == null ? "" : edit.getNewText());
    }

    static List<TextEditLsp> readEdits(
            @Nullable List<? extends TextEdit> edits) {
        List<TextEditLsp> lues = new ArrayList<>();
        if (edits != null) {
            for (TextEdit edit : edits) {
                if (edit != null) {
                    lues.add(readEdit(edit));
                }
            }
        }
        return List.copyOf(lues);
    }
}
