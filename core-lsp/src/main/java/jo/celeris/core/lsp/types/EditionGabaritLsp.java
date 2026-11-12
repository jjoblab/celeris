package jo.celeris.core.lsp.types;

/**
 * L'édit de gabarit — le bloqué historique du dépôt de départ. La
 * version 1.0.0 de la liaison tierce changeait le contrat des éditions
 * d'espace de travail (édits de type gabarit) et n'avait jamais pu être
 * absorbée : la forme vit ici, nôtre, documentée — le protocole
 * standard n'en définit aucune.
 *
 * <p>Le corps porte ses trous {@code ${nom:defaut}} — le moteur de
 * gabarits du M5 les rend, les défauts se posent, les curseurs y
 * tombent dans l'ordre de tabulation (B-06). L'application compose le
 * client (cette forme) et le moteur (core-intelligence) chez le
 * consommateur — les modules purs restent latéraux (ADR-002).</p>
 *
 * @param uri      l'uri du document qui reçoit le gabarit
 * @param position la position d'insertion du gabarit
 * @param corps    le corps du gabarit, trous compris
 */
public record EditionGabaritLsp(String uri, PositionLsp position, String corps) {

    /** La sorte d'entrée « documentChanges » qui désigne un édit de gabarit. */
    public static final String SORTE = "celeris/templateEdit";
}
