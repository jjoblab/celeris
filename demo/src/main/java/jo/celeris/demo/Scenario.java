package jo.celeris.demo;

/**
 * Un scénario de démonstration : identifiant de la matrice de parité,
 * libellé affiché et critère d'acceptation rappelé à l'écran.
 *
 * @param identifiant l'identifiant de la capacite (par exemple A-13)
 * @param libelle le libelle affiché dans la liste
 * @param objectif le critere d'acceptation de la matrice de parite
 */
public record Scenario(String identifiant, String libelle, String objectif) {
}
