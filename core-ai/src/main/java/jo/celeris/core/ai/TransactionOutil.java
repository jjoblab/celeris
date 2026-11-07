package jo.celeris.core.ai;

import java.util.List;

import jo.celeris.core.text.TextEdit;

/**
 * La transaction d'un agent par la voie des outils (F-05, ADR-012) :
 * des éditions et l'instantané de référence sur lequel elles se sont
 * calculées — la révision que l'agent a lue avant d'écrire.
 *
 * <p>Les éditions vivent en coordonnées de cet instantané, la
 * discipline de {@link PropositionAgent} : ordonnées, disjointes,
 * textes retirés exacts. L'application refuse si le document a bougé
 * depuis — jamais de fusion silencieuse, le refus porte son message
 * (voir {@link ReponseTransaction}).</p>
 *
 * @param revisionAttendue la révision de l'instantané lu par l'agent
 * @param edits            les éditions, ordonnées par position croissante
 */
public record TransactionOutil(int revisionAttendue, List<TextEdit> edits) {

    /** Copie défensive — la transaction ne se modifie jamais après coup. */
    public TransactionOutil {
        edits = List.copyOf(edits);
    }
}
