package jo.celeris.core.lsp;

/**
 * Un booléen JSON : vrai ou faux, rien d'autre à dire.
 */
public record JsonBooleen(boolean valeur) implements ValeurJson {

    /** Le vrai. */
    public static final JsonBooleen VRAI = new JsonBooleen(true);

    /** Le faux. */
    public static final JsonBooleen FAUX = new JsonBooleen(false);

    /** Le booléen d'une valeur. */
    public static JsonBooleen of(boolean valeur) {
        return valeur ? VRAI : FAUX;
    }
}
