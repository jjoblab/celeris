package jo.celeris.demo;

import java.util.function.BooleanSupplier;

/**
 * Une spécification de scénario (M7.5, §9) : le scénario du catalogue
 * GAGNE sa vérification automatique — le banc joue les gestes du fil
 * réel (les moteurs branchés, pas des figures) et rend un verdict
 * booléen, rejouable à l'ouverture de la démonstration. Le catalogue
 * cesse d'être décoratif : ce qui s'affiche se prouve.
 *
 * @param identifiant l'identifiant de la matrice (par exemple A-05)
 * @param libelle     le geste vérifié, en une phrase
 * @param verification le verdict rejouable — les gestes, puis la preuve
 */
record ScenarioSpec(String identifiant, String libelle,
        BooleanSupplier verification) {
}
