package jo.celeris.core.lsp;

/**
 * Une plage LSP : début inclus, fin exclusive — deux positions en
 * unités UTF-16.
 *
 * @param debut la position de début
 * @param fin   la position de fin
 */
public record PlageLsp(PositionLsp debut, PositionLsp fin) {

    /**
     * Analyse la plage depuis sa forme JSON.
     *
     * @param json l'objet « start »/« end »
     * @return la plage lue, zéros si la forme est incomplète
     */
    public static PlageLsp de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return new PlageLsp(new PositionLsp(0, 0), new PositionLsp(0, 0));
        }
        return new PlageLsp(PositionLsp.de(objet.membreOu("start", JsonObjet.vide())),
                PositionLsp.de(objet.membreOu("end", JsonObjet.vide())));
    }

    /** La forme JSON de la plage. */
    public JsonObjet versJson() {
        return JsonObjet.vide()
                .ajouter("start", debut.versJson())
                .ajouter("end", fin.versJson());
    }
}
