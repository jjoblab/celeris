package jo.celeris.core.lsp;

import java.util.List;
import jo.celeris.core.lsp.types.DiagnosticLsp;

/**
 * L'auditeur des diagnostics publiés par le serveur : le contrat du
 * domaine (ADR-005), indépendant de la voie protocolaire qui les
 * apporte — la frontière protocolaire (ADR-015) y branche ses
 * auditeurs, l'éditeur y reçoit les types immuables du cœur.
 *
 * <p>Les appels partent du fil de lecture du protocole : l'auditeur
 * doit être prompt, jamais bloquant.</p>
 */
public interface AuditeurDiagnostics {

    /**
     * Des diagnostics publiés pour un document.
     *
     * @param uri        l'uri du document
     * @param diagnostics les diagnostics, le dernier état du serveur
     */
    void diagnostics(String uri, List<DiagnosticLsp> diagnostics);
}
