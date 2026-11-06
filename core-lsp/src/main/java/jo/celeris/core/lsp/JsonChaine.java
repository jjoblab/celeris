package jo.celeris.core.lsp;

/**
 * Une chaîne JSON : le texte décodé, sans guillemets ni échappements —
 * ce que voit le protocole, pas ce que lit le fil.
 */
public record JsonChaine(String texte) implements ValeurJson {

    /** La chaîne d'un texte. */
    public static JsonChaine of(String texte) {
        return new JsonChaine(texte);
    }
}
