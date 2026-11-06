package jo.celeris.core.lsp;

/**
 * Une édition de texte du protocole : la plage remplacée et le texte
 * nouveau — en positions LSP (unités UTF-16), la transposition en
 * offsets vivant dans {@link EditsEspaceLsp}.
 *
 * @param plage        la plage remplacée
 * @param nouveauTexte le texte qui la remplace
 */
public record EditionTexteLsp(PlageLsp plage, String nouveauTexte) {

    /**
     * Analyse l'édition depuis sa forme JSON.
     *
     * @param json l'objet édition
     * @return l'édition lue — plage vide et texte vide si la forme est
     *         incomplète
     */
    public static EditionTexteLsp de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return new EditionTexteLsp(new PlageLsp(new PositionLsp(0, 0),
                    new PositionLsp(0, 0)), "");
        }
        PlageLsp plage = PlageLsp.de(objet.membreOu("range", JsonObjet.vide()));
        String texte = objet.membreTexte("newText");
        return new EditionTexteLsp(plage, texte == null ? "" : texte);
    }

    /** La forme JSON de l'édition. */
    public JsonObjet versJson() {
        return JsonObjet.vide()
                .ajouter("range", plage.versJson())
                .ajouter("newText", JsonChaine.of(nouveauTexte));
    }
}
