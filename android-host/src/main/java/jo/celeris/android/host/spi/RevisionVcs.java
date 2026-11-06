package jo.celeris.android.host.spi;

/**
 * Une révision de l'historique VCS (E-02).
 *
 * @param id      l'identifiant de la révision
 * @param auteur  l'auteur
 * @param date    la date, écriture libre
 * @param message le message de la révision
 */
public record RevisionVcs(String id, String auteur, String date, String message) {
}
