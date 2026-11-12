package jo.celeris.lsp.protocole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jo.celeris.core.lsp.types.EditionDocumentLsp;
import jo.celeris.core.lsp.types.EditionEspaceLsp;
import jo.celeris.core.lsp.types.EditionGabaritLsp;
import jo.celeris.core.lsp.types.EditionTexteLsp;
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
final class CorrespondanceEspace {

    private CorrespondanceEspace() {
        // la correspondance est un choix de fonctions pures, jamais une instance
    }

    /** L'édition d'espace lue : documentChanges d'abord, la carte héritée ensuite. */
    static EditionEspaceLsp lireEspace(@Nullable WorkspaceEdit edition) {
        if (edition == null) {
            return EditionEspaceLsp.VIDE;
        }
        List<EditionDocumentLsp> documents = new ArrayList<>();
        List<EditionGabaritLsp> gabarits = new ArrayList<>();
        if (edition.getDocumentChanges() != null) {
            for (Either<TextDocumentEdit, org.eclipse.lsp4j.ResourceOperation>
                    changement : edition.getDocumentChanges()) {
                if (changement != null && changement.isLeft()
                        && changement.getLeft() != null) {
                    lireEditionDocument(changement.getLeft(), documents, gabarits);
                }
            }
        }
        if (documents.isEmpty() && gabarits.isEmpty()
                && edition.getChanges() != null) {
            for (Map.Entry<String, List<TextEdit>> entree
                    : edition.getChanges().entrySet()) {
                documents.add(new EditionDocumentLsp(entree.getKey(), -1,
                        lireEditions(entree.getValue())));
            }
        }
        return new EditionEspaceLsp(documents, gabarits);
    }

    /**
     * Les éditions d'un document : les éditions de texte suivent leur
     * liste, les édits de gabarit de la 1.0.0 (SnippetTextEdit)
     * rejoignent la liste de l'ESPACE — la forme du domaine, le bloqué
     * historique débloqué par la montée de version (ADR-015).
     */
    private static void lireEditionDocument(TextDocumentEdit edition,
            List<EditionDocumentLsp> documents,
            List<EditionGabaritLsp> gabarits) {
        List<EditionTexteLsp> editions = new ArrayList<>();
        for (Either<TextEdit, SnippetTextEdit> edit : edition.getEdits()) {
            if (edit == null) {
                continue;
            }
            if (edit.isLeft() && edit.getLeft() != null) {
                editions.add(lireEdition(edit.getLeft()));
            } else if (edit.isRight() && edit.getRight() != null) {
                SnippetTextEdit gabarit = edit.getRight();
                gabarits.add(new EditionGabaritLsp(uriDe(edition),
                        CorrespondanceLsp.lirePosition(
                                gabarit.getRange() == null ? null
                                        : gabarit.getRange().getStart()),
                        gabarit.getSnippet() == null
                                || gabarit.getSnippet().getValue() == null ? ""
                                : gabarit.getSnippet().getValue()));
            }
        }
        documents.add(new EditionDocumentLsp(uriDe(edition),
                versionDe(edition), editions));
    }

    private static String uriDe(TextDocumentEdit edition) {
        return edition.getTextDocument() == null
                || edition.getTextDocument().getUri() == null ? ""
                        : edition.getTextDocument().getUri();
    }

    private static int versionDe(TextDocumentEdit edition) {
        return edition.getTextDocument() == null
                || edition.getTextDocument().getVersion() == null ? -1
                        : edition.getTextDocument().getVersion();
    }

    private static EditionTexteLsp lireEdition(TextEdit edition) {
        return new EditionTexteLsp(CorrespondanceLsp.lirePlage(edition.getRange()),
                edition.getNewText() == null ? "" : edition.getNewText());
    }

    static List<EditionTexteLsp> lireEditions(
            @Nullable List<? extends TextEdit> editions) {
        List<EditionTexteLsp> lues = new ArrayList<>();
        if (editions != null) {
            for (TextEdit edition : editions) {
                if (edition != null) {
                    lues.add(lireEdition(edition));
                }
            }
        }
        return List.copyOf(lues);
    }
}
