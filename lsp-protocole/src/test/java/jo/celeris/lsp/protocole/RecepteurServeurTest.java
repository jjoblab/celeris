package jo.celeris.lsp.protocole;

import jo.celeris.core.lsp.types.DiagnosticLsp;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.junit.jupiter.api.Test;



/**
 * Le récepteur des requêtes du serveur vers le client (ADR-015) : les
 * diagnostics publiés alimentent l'état et les auditeurs ; les
 * requêtes de génération suivante reçoivent les refus polis de
 * l'éditeur — l'édition d'espace n'est JAMAIS appliquée en aveugle,
 * les messages ne s'affichent pas seuls, les journaux restent au
 * banc — la surface du module les étoffera au fil des jalons (H-12).
 */
class RecepteurServeurTest {

    private static final String URI = "file:///banc/recepteur.xml";

    /** Les diagnostics publiés : l'état se garde, l'auditeur entend. */
    @Test
    void lesDiagnosticsPubliesServentLEtatEtLAuditeur() {
        RecepteurServeur recepteur = new RecepteurServeur();
        java.util.List<DiagnosticLsp> entendues = new java.util.ArrayList<>();
        recepteur.ajouterAuditeurDiagnostics(
                (uri, diags) -> entendues.addAll(diags));
        PublishDiagnosticsParams params = new PublishDiagnosticsParams(URI,
                java.util.List.of());
        recepteur.publishDiagnostics(params);
        assertTrue(recepteur.diagnosticsDe(URI).isEmpty(),
                "la publication vide se garde comme état vide");
        assertTrue(entendues.isEmpty(), "l'auditeur entend la publication vide");
        assertTrue(recepteur.diagnosticsDe("file:///autre").isEmpty(),
                "un document jamais publié reste vide");
    }

    /** Le refus poli de l'édition d'espace : jamais appliquée en aveugle. */
    @Test
    void leditionDEspaceEstRefusee() throws Exception {
        RecepteurServeur recepteur = new RecepteurServeur();
        CompletableFuture<org.eclipse.lsp4j.ApplyWorkspaceEditResponse> refus =
                recepteur.applyEdit(new ApplyWorkspaceEditParams());
        assertFalse(refus.get().isApplied(),
                "l'édition d'espace reçoit son refus poli");
    }

    /** Les messages du serveur : le banc ne s'interrompt pas pour eux. */
    @Test
    void lesMessagesDuServeurRestentAuBanc() throws Exception {
        RecepteurServeur recepteur = new RecepteurServeur();
        recepteur.showMessage(new MessageParams());
        recepteur.logMessage(new MessageParams());
        recepteur.telemetryEvent(null);
        MessageActionItem reponse = recepteur.showMessageRequest(
                new ShowMessageRequestParams()).get();
        assertNotNull(reponse,
                "la demande de message reçoit une réponse, jamais un silence");
    }
}
