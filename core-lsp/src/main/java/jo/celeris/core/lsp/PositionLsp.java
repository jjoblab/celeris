package jo.celeris.core.lsp;

/**
 * Une position LSP : ligne et colonne — la colonne compte les unités
 * UTF-16, le repère du protocole. La transposition vers les offsets du
 * document vit dans {@link PositionsLsp}, frontière explicite (ADR-005).
 *
 * @param ligne    la ligne, à partir de zéro
 * @param colonne  la colonne en unités UTF-16, à partir de zéro
 */
public record PositionLsp(int ligne, int colonne) {

    /**
     * Analyse la position depuis sa forme JSON.
     *
     * @param json l'objet « line »/« character »
     * @return la position lue, zéros si la forme est incomplète
     */
    public static PositionLsp de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return new PositionLsp(0, 0);
        }
        Long ligne = objet.membreEntier("line");
        Long colonne = objet.membreEntier("character");
        return new PositionLsp(ligne == null ? 0 : ligne.intValue(),
                colonne == null ? 0 : colonne.intValue());
    }

    /** La forme JSON de la position. */
    public JsonObjet versJson() {
        return JsonObjet.vide()
                .ajouter("line", JsonNombre.of(ligne))
                .ajouter("character", JsonNombre.of(colonne));
    }
}
