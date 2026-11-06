package jo.celeris.core.lsp;

/**
 * Un nombre JSON : la valeur en double, exacte pour tous les usages du
 * protocole (identifiants, versions, positions). À l'écriture, une
 * valeur entière se note sans point — « 2 » reste « 2 », seul un vrai
 * décimal porte son point.
 */
public record JsonNombre(double decimal) implements ValeurJson {

    /** Le nombre d'un entier — exact, jamais pourvu d'un point. */
    public static JsonNombre of(long entier) {
        return new JsonNombre(entier);
    }

    /** Le nombre d'un décimal — écrit avec point ou exposant. */
    public static JsonNombre of(double decimal) {
        return new JsonNombre(decimal);
    }

    /** Vrai quand la valeur est un entier mathématique. */
    public boolean estEntier() {
        return decimal == Math.rint(decimal) && !estInfiniOuTropGrand();
    }

    /** La valeur arrondie à l'entier — à n'appeler que sur un entier. */
    public long entier() {
        return (long) decimal;
    }

    private boolean estInfiniOuTropGrand() {
        return Double.isInfinite(decimal)
                || Math.abs(decimal) >= 9.007199254740992e15;
    }
}
