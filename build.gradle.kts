// Build racine : coordonnées communes, tâches de politique bloquantes
// (ADR-011) et orchestration du contrôle global. Les réglages par module
// vivent dans build-logic — rien ne se duplique ici.

subprojects {
    group = "jo.celeris"
    version = "0.1.0-SNAPSHOT"
}

// Politique de langue : identifiants ASCII, typographie française,
// Javadoc des classes publiques (ADR-009/011).
tasks.register<Exec>("policyLanguageCheck") {
    group = "verification"
    description = "Politique de langue du dépôt (scripts/check-language-policy)."
    commandLine("python3", "scripts/check-language-policy")
}

// Neutralité : aucun projet externe nommé (ADR-011) ; les motifs
// nominatifs viennent de l'environnement, jamais du dépôt.
tasks.register<Exec>("policyExternalRefsCheck") {
    group = "verification"
    description = "Neutralité du dépôt envers les projets externes (scripts/check-external-refs)."
    commandLine("python3", "scripts/check-external-refs")
    environment(
        "CELERIS_FORBIDDEN_REFS",
        providers.environmentVariable("CELERIS_FORBIDDEN_REFS").getOrElse(""),
    )
    environment(
        "FORBIDDEN_REFS_EXTRA",
        providers.environmentVariable("FORBIDDEN_REFS_EXTRA").getOrElse(""),
    )
}

tasks.register("policyCheck") {
    group = "verification"
    description = "Contrôles de politique (langue et références externes)."
    dependsOn("policyLanguageCheck", "policyExternalRefsCheck")
}

// Contrôle global : politique + portes qualité de chaque module. Les
// scripts bloquent tout build tant qu'une règle est violée.
tasks.register("check") {
    group = "verification"
    description = "Tous les contrôles : politique du dépôt + qualité de chaque module."
    dependsOn("policyCheck")
    subprojects.forEach { sousProjet ->
        dependsOn("${sousProjet.path}:check")
    }
}
