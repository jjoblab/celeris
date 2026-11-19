package jo.celeris.android.host.spi;

/**
 * Une plage de lignes, bornes inclusives, numérotées à partir de un —
 * la forme des suppressions VCS (E-02) : « les lignes 9 à 10 ont
 * disparu ».
 *
 * @param start la première ligne de la plage, ≥ 1
 * @param end   la dernière ligne de la plage, ≥ debut
 */
public record LineRange(int start, int end) {

    /** Valide la plage. */
    public LineRange {
        if (start < 1 || end < start) {
            throw new IllegalArgumentException(
                    "invalid line range: " + start + "-" + end);
        }
    }

    /** Vrai si la ligne donnée tombe dans la plage, bornes comprises. */
    public boolean contient(int line) {
        return line >= start && line <= end;
    }
}
