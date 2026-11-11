package jo.celeris.serveur.java;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.junit.jupiter.api.Test;

/**
 * Le banc unitaire du serveur : les angles que le banc de bout en bout
 * ne croise pas — l'extinction polie, l'espace de travail muet,
 * l'enregistrement sans effet, et les demandes sur un document jamais
 * ouvert (la réponse reste vide, jamais une exception).
 */
class JavaLanguageServerUnitTest {

    private static final String URI_INCONNUE = "file:///inconnu.java";

    @Test
    void lextinctionSePolitSansEffetDeBord() throws Exception {
        JavaLanguageServer serveur = new JavaLanguageServer();
        assertNull(serveur.shutdown().get(1, TimeUnit.SECONDS));
        serveur.exit();
    }

    @Test
    void lespaceDeTravailResteMuet() {
        JavaLanguageServer serveur = new JavaLanguageServer();
        WorkspaceService espace = serveur.getWorkspaceService();
        espace.didChangeConfiguration(null);
        espace.didChangeWatchedFiles(null);
    }

    @Test
    void lenregistrementNapprendRienAuServeur() {
        JavaLanguageServer serveur = new JavaLanguageServer();
        serveur.getTextDocumentService().didSave(
                new DidSaveTextDocumentParams());
    }

    @Test
    void lesDemandesSansDocumentRestentVides() throws Exception {
        JavaLanguageServer serveur = new JavaLanguageServer();
        TextDocumentService documents = serveur.getTextDocumentService();
        HoverParams survol = new HoverParams();
        survol.setTextDocument(new TextDocumentIdentifier(URI_INCONNUE));
        survol.setPosition(new Position(0, 0));
        assertNull(documents.hover(survol).get(1, TimeUnit.SECONDS));
        CompletionParams completion = new CompletionParams();
        completion.setTextDocument(new TextDocumentIdentifier(URI_INCONNUE));
        completion.setPosition(new Position(0, 0));
        assertTrue(documents.completion(completion).get(1, TimeUnit.SECONDS)
                .getLeft().isEmpty());
        DocumentSymbolParams symboles = new DocumentSymbolParams();
        symboles.setTextDocument(new TextDocumentIdentifier(URI_INCONNUE));
        assertTrue(documents.documentSymbol(symboles).get(1, TimeUnit.SECONDS)
                .isEmpty());
        FoldingRangeRequestParams plis = new FoldingRangeRequestParams();
        plis.setTextDocument(new TextDocumentIdentifier(URI_INCONNUE));
        assertTrue(documents.foldingRange(plis).get(1, TimeUnit.SECONDS)
                .isEmpty());
        DocumentFormattingParams formatage = new DocumentFormattingParams();
        formatage.setTextDocument(new TextDocumentIdentifier(URI_INCONNUE));
        formatage.setOptions(new FormattingOptions());
        assertTrue(documents.formatting(formatage).get(1, TimeUnit.SECONDS)
                .isEmpty());
    }

    @Test
    void leChangementSansContenuNeBousculeRien() {
        JavaLanguageServer serveur = new JavaLanguageServer();
        DidChangeTextDocumentParams changement =
                new DidChangeTextDocumentParams();
        changement.setTextDocument(
                new VersionedTextDocumentIdentifier(URI_INCONNUE, 2));
        serveur.getTextDocumentService().didChange(changement);
    }

    @Test
    void lePointDEntreeSeLanceSurDesFluxDonnes() throws Exception {
        // le point d'entrée délègue à des flux : un flux d'entrée déjà
        // fermé termine l'écoute immédiatement, la piscine s'éteint
        jo.celeris.lsp.protocole.TubesMemoire.Direction entree =
                jo.celeris.lsp.protocole.TubesMemoire.creer();
        jo.celeris.lsp.protocole.TubesMemoire.Direction sortie =
                jo.celeris.lsp.protocole.TubesMemoire.creer();
        entree.fermer();
        JavaLanguageServerMain.lancer(entree.entree(), sortie.sortie());
        sortie.fermer();
    }
}
