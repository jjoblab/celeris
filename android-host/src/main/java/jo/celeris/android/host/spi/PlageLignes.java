package jo.celeris.android.host.spi;

/**
 * Une plage de lignes, bornes inclusives, numérotées à partir de un —
 * la forme des suppressions VCS (E-02) : « les lignes 9 à 10 ont
 * disparu ».
 *
 * @param debut la première ligne de la plage, ≥ 1
 * @param fin   la dernière ligne de la plage, ≥ debut
 */
public record PlageLignes(int debut, int fin) {

    /** Valide la plage. */
    public PlageLignes {
        if (debut < 1 || fin < debut) {
            throw new IllegalArgumentException(
                    "invalid line range: " + debut + "-" + fin);
        }
    }

    /** Vrai si la ligne donnée tombe dans la plage, bornes comprises. */
    public boolean contient(int ligne) {
        return ligne >= debut && ligne <= fin;
    }
}
