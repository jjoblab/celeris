package jo.celeris.core.lsp;

/**
 * Le nul JSON : présent et typé — un membre explicitement null rend
 * cette valeur, un membre absent rend null (la référence). La
 * distinction porte le protocole : « result: null » est une réponse,
 * l'absence de « result » est une erreur d'enveloppe.
 */
public record JsonNul() implements ValeurJson {

    private static final JsonNul INSTANCE = new JsonNul();

    /** Le nul. */
    public static JsonNul nul() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "null";
    }
}
