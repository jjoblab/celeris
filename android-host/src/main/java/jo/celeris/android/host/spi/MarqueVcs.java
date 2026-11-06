package jo.celeris.android.host.spi;

/**
 * La marque VCS d'une ligne (E-02, D-03) : ce que la gouttière trace à
 * côté du numéro.
 */
public enum MarqueVcs {

    /** La ligne existe dans la révision de référence, inchangée. */
    INCHANGE,

    /** La ligne existe dans la révision de référence, son contenu a changé. */
    MODIFIE,

    /** La ligne n'existe pas dans la révision de référence. */
    AJOUTE
}
