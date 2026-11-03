package jo.celeris.demo;

import java.util.List;

/**
 * Catalogue des scénarios de démonstration : vide au squelette M1 ;
 * chaque jalon y ajoute les scénarios de la matrice de parité qu'il
 * couvre, avec leur test de fumée correspondant.
 */
public final class ScenarioCatalog {

    private ScenarioCatalog() {
    }

    /** Scénarios disponibles, dans l'ordre de la matrice de parité. */
    public static List<Scenario> scenarios() {
        return List.of();
    }
}
