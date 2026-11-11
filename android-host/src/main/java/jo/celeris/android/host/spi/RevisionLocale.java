package jo.celeris.android.host.spi;

import java.util.Objects;

/**
 * Une révision de l'historique local (A-14) : le contenu intégral d'un
 * fichier à un instant donné, horodaté par l'horloge injectée. La
 * révision appartient à son fichier — restaurer la révision d'un autre
 * fichier refuse.
 *
 * @param chemin      le chemin du fichier révisé
 * @param contenu     le contenu intégral au moment de l'enregistrement
 * @param horodatage  l'instant de l'enregistrement, lu sur l'horloge
 *                    injectée
 */
public record RevisionLocale(CheminDoc chemin, String contenu, long horodatage) {

    /** Constructeur canonique : aucun argument nul. */
    public RevisionLocale {
        Objects.requireNonNull(chemin, "chemin");
        Objects.requireNonNull(contenu, "contenu");
    }
}
