package jo.celeris.android.view;

/**
 * Spécification de police immuable : famille et taille en pixels.
 *
 * <p>Valeur de clé par excellence (PIEGE-02) : deux spécifications égales
 * par la valeur désignent la même police — jamais la même instance
 * n'est exigée, jamais un état mutable ne s'y glisse.</p>
 *
 * @param family la famille de police, libellé non vide
 * @param size   la taille en pixels, strictement positive
 */
public record FontSpec(String family, float size) {

    /** Valide la spécification. */
    public FontSpec {
        if (family == null || family.isBlank()) {
            throw new IllegalArgumentException("family must not be blank");
        }
        if (!(size > 0f)) {
            throw new IllegalArgumentException("size must be positive: " + size);
        }
    }
}
