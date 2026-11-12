package jo.celeris.lsp.protocole;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jo.celeris.core.lsp.types.DiagnosticLsp;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.jspecify.annotations.Nullable;

import jo.celeris.core.lsp.AuditeurDiagnostics;


/**
 * Le récepteur des requêtes du serveur vers le client (ADR-015) : les
 * diagnostics publiés alimentent l'état et les auditeurs du domaine ;
 * les requêtes de génération suivante — édition d'espace, enregistrement
 * de capacités, progression, configuration — tombent sur les réponses
 * par défaut de la bibliothèque (refus poli), la surface du module les
 * étoffera au fil des jalons (H-12).
 *
 * <p>Le protocole n'est jamais composé ici : la bibliothèque a déjà
 * décodé, la correspondance traduit, le domaine reçoit des types
 * immuables.</p>
 */
final class RecepteurServeur implements LanguageClient {

    private final Map<String, List<DiagnosticLsp>> diagnostics =
            new ConcurrentHashMap<>();
    private final List<AuditeurDiagnostics> auditeurs =
            new CopyOnWriteArrayList<>();

    @Override
    public void telemetryEvent(@Nullable Object valeur) {
        // rien à télémétrer : l'éditeur n'écoute pas la télémétrie du serveur
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams params) {
        String uri = params.getUri();
        List<DiagnosticLsp> figee = CorrespondanceLsp.diagnosticsPublies(params);
        diagnostics.put(uri, figee);
        for (AuditeurDiagnostics auditeur : auditeurs) {
            auditeur.diagnostics(uri, figee);
        }
    }

    @Override
    public void showMessage(MessageParams params) {
        // le banc affiche les messages par ses propres canaux ; rien ici
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(
            ShowMessageRequestParams params) {
        // aucun choix proposé à l'utilisateur : la réponse porte le premier
        // élément, la bibliothèque n'exige rien de plus
        return CompletableFuture.completedFuture(new MessageActionItem());
    }

    @Override
    public void logMessage(MessageParams params) {
        // silencieux : les journaux du serveur n'entrent pas dans l'éditeur
    }

    @Override
    public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(
            org.eclipse.lsp4j.ApplyWorkspaceEditParams params) {
        // refus poli : les éditions d'espace (H-12) passent par le moteur
        // de gabarits du domaine, jamais appliquées en aveugle
        return CompletableFuture.completedFuture(
                new ApplyWorkspaceEditResponse(false));
    }

    /** Les derniers diagnostics publiés pour un document — vide par défaut. */
    List<DiagnosticLsp> diagnosticsDe(String uri) {
        return diagnostics.getOrDefault(uri, List.of());
    }

    /** Branche un auditeur de diagnostics du domaine. */
    void ajouterAuditeurDiagnostics(AuditeurDiagnostics auditeur) {
        auditeurs.add(auditeur);
    }

    /** Purge les diagnostics d'un document fermé : la carte ne
     * vieillit pas avec les documents visités — une session longue
     * n'accumule rien au didClose. */
    void purgerDiagnostics(String uri) {
        diagnostics.remove(uri);
    }
}
