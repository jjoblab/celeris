package jo.celeris.core.text;

/**
 * Plage ancrée dans le document : {@code [start, end)} en offsets UTF-16,
 * projetée à travers les éditions par {@link AnchoredRanges}.
 *
 * <p>Les diagnostics, plis et régions de composition du dépôt de départ
 * partageaient cette forme — c'est la mécanique de projection qui est
 * portée ici, les types porteurs arrivent avec leurs modules.</p>
 *
 * @param start offset de début (inclusif)
 * @param end   offset de fin (exclusif)
 */
public record AnchoredRange(int start, int end) {

    /**
     * Valide la plage.
     *
     * @throws IllegalArgumentException si {@code start < 0} ou
     *         {@code end < start}
     */
    public AnchoredRange {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException(
                "invalid range: [" + start + ", " + end + ")");
        }
    }
}
