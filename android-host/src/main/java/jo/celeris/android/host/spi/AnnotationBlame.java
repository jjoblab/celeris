package jo.celeris.android.host.spi;

/**
 * L'annotation de blame d'une ligne (E-02) : qui a écrit cette ligne,
 * dans quelle révision, avec quel message.
 *
 * @param ligne    la ligne annotée, numérotée à partir de un
 * @param revision l'identifiant de la révision
 * @param auteur   l'auteur de la révision
 * @param message  le message de la révision
 */
public record AnnotationBlame(int ligne, String revision, String auteur,
                              String message) {
}
