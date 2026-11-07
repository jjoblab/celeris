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
    description = "Contrôles de politique (langue, références, rempart, matrice, pièges, démo, dates)."
    dependsOn("policyLanguageCheck", "policyExternalRefsCheck")
}

// Rempart de langue (M7.5, §11) : la ligne de base des identifiants
// français ne monte jamais — les nouveaux identifiants sont en anglais.
tasks.register<Exec>("policyLanguageBaselineCheck") {
    group = "verification"
    description = "Rempart de langue du dépôt (scripts/check-language-baseline)."
    commandLine("python3", "scripts/check-language-baseline")
}

// Vérité des tableaux de bord (M7.5, §11) : la matrice ne ment plus —
// statuts du cadre M7.5, tests cités existants, scénarios cités connus.
tasks.register<Exec>("policyParityMatrixCheck") {
    group = "verification"
    description = "Matrice de parité honnête (scripts/check-parity-matrix)."
    commandLine("python3", "scripts/check-parity-matrix")
}

// Registre des pièges (M7.5, §11, règle 5) : chaque test cité existe.
tasks.register<Exec>("policyPiegesCheck") {
    group = "verification"
    description = "Citations du registre des pièges (scripts/check-pieges)."
    commandLine("python3", "scripts/check-pieges")
}

// Couverture de la démonstration (M7.5, §9/§11) : une ligne démontrée
// cite son scénario, un scénario cite une ligne connue.
tasks.register<Exec>("policyDemoCoverageCheck") {
    group = "verification"
    description = "Couverture de la démo (scripts/check-demo-coverage)."
    commandLine("python3", "scripts/check-demo-coverage")
}

// Dates des documents (M7.5, §11) : aucun résumé ne se date au-delà
// du dernier commit.
tasks.register<Exec>("policyDocsDatesCheck") {
    group = "verification"
    description = "Dates des documents contre l'historique git (scripts/check-docs-dates)."
    commandLine("python3", "scripts/check-docs-dates")
}

// Contrôle global : politique + portes qualité de chaque module. Les
// scripts bloquent tout build tant qu'une règle est violée.
tasks.register("check") {
    group = "verification"
    description = "Tous les contrôles : politique du dépôt + qualité de chaque module."
    dependsOn("policyCheck", "policyLanguageBaselineCheck",
            "policyParityMatrixCheck", "policyPiegesCheck",
            "policyDemoCoverageCheck", "policyDocsDatesCheck")
    subprojects.forEach { sousProjet ->
        dependsOn("${sousProjet.path}:check")
    }
}
