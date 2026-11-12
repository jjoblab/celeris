// Build racine : coordonnées communes, tâches de politique bloquantes
// (ADR-011) et orchestration du contrôle global. Les réglages par module
// vivent dans build-logic — rien ne se duplique ici.

subprojects {
    group = "jo.celeris"
    version = "1.1.0"
}

// Politique de langue : identifiants ASCII, typographie française,
// Javadoc des classes publiques (ADR-009/011).
tasks.register<Exec>("policyLanguageCheck") {
    group = "verification"
    description = "Politique de langue du dépôt (scripts/check-language-policy)."
    commandLine("python3", "scripts/check-language-policy")
}

// Références interdites (M7.6, règle 7) : deux projets et leurs
// artefacts restent interdits ; les autres se nomment librement.
// Les motifs nominatifs viennent de l'environnement, jamais du dépôt.
tasks.register<Exec>("policyForbiddenRefsCheck") {
    group = "verification"
    description = "Références interdites du dépôt (scripts/check-forbidden-refs)."
    commandLine("python3", "scripts/check-forbidden-refs")
    environment(
        "CELERIS_FORBIDDEN_REFS",
        providers.environmentVariable("CELERIS_FORBIDDEN_REFS").getOrElse(""),
    )
    environment(
        "FORBIDDEN_REFS_EXTRA",
        providers.environmentVariable("FORBIDDEN_REFS_EXTRA").getOrElse(""),
    )
}

// Auto-test du contrôle des références interdites (M7.6) : motif
// trouvé, motif absent, variable vide, motif invalide, exonérations.
tasks.register<Exec>("policyForbiddenRefsSelfTest") {
    group = "verification"
    description = "Tests du contrôle des références interdites (scripts/test-check-forbidden-refs)."
    commandLine("python3", "scripts/test-check-forbidden-refs")
}

// Provenance des dépendances (M7.6, règle 7) : aucun artefact publié
// par les organisations ou auteurs interdits n'entre dans le build —
// catalogue, métadonnées de vérification et URL de dépôts balayés.
tasks.register<Exec>("policyDependencyProvenanceCheck") {
    group = "verification"
    description = "Provenance des dépendances (scripts/check-dependency-provenance)."
    commandLine("python3", "scripts/check-dependency-provenance")
    environment(
        "CELERIS_FORBIDDEN_REFS",
        providers.environmentVariable("CELERIS_FORBIDDEN_REFS").getOrElse(""),
    )
    environment(
        "FORBIDDEN_REFS_EXTRA",
        providers.environmentVariable("FORBIDDEN_REFS_EXTRA").getOrElse(""),
    )
}

// Auto-test du contrôle de provenance (M7.6).
tasks.register<Exec>("policyDependencyProvenanceSelfTest") {
    group = "verification"
    description = "Tests du contrôle de provenance (scripts/test-check-dependency-provenance)."
    commandLine("python3", "scripts/test-check-dependency-provenance")
}

// Enregistrement des langages (M7.6, étape 4) : aucun littéral
// d'identifiant de langage hors des paquets et du code
// d'enregistrement — ajouter un langage ne demande que des données.
tasks.register<Exec>("policyLanguageRegistrationCheck") {
    group = "verification"
    description = "Enregistrement des langages (scripts/check-language-registration)."
    commandLine("python3", "scripts/check-language-registration")
}

// Auto-test du contrôle d'enregistrement (M7.6).
tasks.register<Exec>("policyLanguageRegistrationSelfTest") {
    group = "verification"
    description = "Tests du contrôle d'enregistrement (scripts/test-check-language-registration)."
    commandLine("python3", "scripts/test-check-language-registration")
}

// Auditeur automatique d'accessibilité (M9, ADR-008) : la promesse de
// M3 vit enfin en CI — labels des interactifs (propres ou labelFor),
// aucun littéral, titres des menus, parité en/fr des clés, câblage de
// l'arbre virtuel (AccessibilityHost exposé, sélection annoncée).
tasks.register<Exec>("policyAccessibilityCheck") {
    group = "verification"
    description = "Auditeur d'accessibilité du dépôt (scripts/check-accessibilite)."
    commandLine("python3", "scripts/check-accessibilite")
}

// Auto-test de l'auditeur d'accessibilité (M9) : chaque règle, une
// fixture, un verdict.
tasks.register<Exec>("policyAccessibilitySelfTest") {
    group = "verification"
    description = "Tests de l'auditeur d'accessibilité (scripts/test-check-accessibilite)."
    commandLine("python3", "scripts/test-check-accessibilite")
}

tasks.register("policyCheck") {
    group = "verification"
    description = "Contrôles de politique (langue, références interdites, provenance, enregistrement des langages, accessibilité, rempart, matrice, pièges, démo, dates)."
    dependsOn("policyLanguageCheck", "policyForbiddenRefsCheck", "policyForbiddenRefsSelfTest",
            "policyDependencyProvenanceCheck", "policyDependencyProvenanceSelfTest",
            "policyLanguageRegistrationCheck", "policyLanguageRegistrationSelfTest",
            "policyAccessibilityCheck", "policyAccessibilitySelfTest")
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
