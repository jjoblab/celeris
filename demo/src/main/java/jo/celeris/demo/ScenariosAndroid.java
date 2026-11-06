package jo.celeris.demo;

import java.util.List;

/**
 * Les scénarios Android du jalon M7 — le bloc langage (C-02, C-03,
 * C-04, C-06, C-08) livré par la tranche 4 et la peinture de
 * gouttière des intégrations (D-03, D-04, D-05) livrée par la
 * tranche 5 — séparés du catalogue général, qui tient sous ses
 * plafonds de taille (ADR-002).
 */
final class ScenariosAndroid {

    private ScenariosAndroid() {
    }

    /** C-02 à C-06 — le bloc XML et manifeste, avant les dialectes Gradle. */
    static List<Scenario> langagePreGradle() {
        return List.of(
                new Scenario(
                        "C-02",
                        "XML : complétion guidée par le schéma",
                        "Le contexte du curseur décide — nom de balise, attribut, "
                                + "valeur — et le schéma embarqué répond : "
                                + "attributs de la balise, unités dp et sp, "
                                + "préfixes @string/ et @drawable/."),
                new Scenario(
                        "C-03",
                        "XML : validation et diagnostics",
                        "La fermeture étrangère porte sa plage exacte et nomme "
                                + "l'attendu comme le trouvé ; la fermeture "
                                + "d'ancêtre signale les traversés à leur "
                                + "ouverture ; l'élément resté ouvert porte la sienne."),
                new Scenario(
                        "C-04",
                        "XML : navigation des références",
                        "@string/x mène au <string name=\"x\"> du fichier de "
                                + "valeurs, @layout/x au fichier de mise en page ; "
                                + "les usages du projet se recensent à l'offset "
                                + "exact — la référence prolongée ne compte pas."),
                new Scenario(
                        "C-06",
                        "Manifeste : complétion et diagnostics",
                        "Les attributs de chaque balise du manifeste — manifeste, "
                                + "application, activité, service, récepteur, "
                                + "permission — et les permissions usuelles, "
                                + "suggérées par leur préfixe."));
    }

    /** C-08 — les catalogues de versions, après les dialectes Gradle. */
    static List<Scenario> cataloguesVersions() {
        return List.of(
                new Scenario(
                        "C-08",
                        "Catalogues de versions : complétion liée",
                        "libs.versions.toml se lit section par section — versions, "
                                + "bibliothèques, bundles, greffons — et les alias "
                                + "se suggèrent dans les builds : tirets en points, "
                                + "bundles dans leur espace de noms."));
    }

    /** D-03 à D-05 — la peinture de gouttière des intégrations. */
    static List<Scenario> gouttiere() {
        return List.of(
                new Scenario(
                        "D-03",
                        "Gouttière : marques de modification VCS",
                        "La barre de modification se peint au bord gauche de la "
                                + "coulisse — modifiée à l'encre de sélection, "
                                + "ajoutée à l'encre de chaîne ; le terrain d'essai "
                                + "rafraîchit à la frappe le diff face au contenu "
                                + "d'origine."),
                new Scenario(
                        "D-04",
                        "Gouttière : points d'arrêt",
                        "Le clic dans le champ des numéros pose et retire l'arrêt "
                                + "de la ligne visée — carré plein posé, carré "
                                + "creux désactivé, l'arrêt désactivé reste en "
                                + "place."),
                new Scenario(
                        "D-05",
                        "Gouttière : repères d'exécution",
                        "La flèche de la ligne courante se peint à l'encre du "
                                + "curseur, le chevron discret des lignes de pile "
                                + "l'accompagne ; le bouton de débogage simule la "
                                + "pause au premier arrêt actif et la reprise."));
    }
}
