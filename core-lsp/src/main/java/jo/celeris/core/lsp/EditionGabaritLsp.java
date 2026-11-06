package jo.celeris.core.lsp;

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

    /**
     * Analyse l'édit de gabarit depuis sa forme JSON.
     *
     * @param json l'objet « celeris/templateEdit »
     * @return l'édit lu — uri vide, position zéro et corps vide si la
     *         forme est incomplète
     */
    public static EditionGabaritLsp de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return new EditionGabaritLsp("", new PositionLsp(0, 0), "");
        }
        String uri = "";
        if (objet.membre("textDocument") instanceof JsonObjet document) {
            String lu = document.membreTexte("uri");
            uri = lu == null ? "" : lu;
        }
        PositionLsp position = PositionLsp.de(objet.membreOu("position", JsonObjet.vide()));
        String corps = objet.membreTexte("template");
        return new EditionGabaritLsp(uri, position, corps == null ? "" : corps);
    }

    /** La forme JSON de l'édit de gabarit. */
    public JsonObjet versJson() {
        return JsonObjet.vide()
                .ajouter("kind", JsonChaine.of(SORTE))
                .ajouter("textDocument", JsonObjet.vide()
                        .ajouter("uri", JsonChaine.of(uri)))
                .ajouter("position", position.versJson())
                .ajouter("template", JsonChaine.of(corps));
    }
}
