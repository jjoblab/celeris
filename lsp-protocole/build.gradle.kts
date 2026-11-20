import org.gradle.api.tasks.testing.Test

// lsp-protocole — la frontière protocolaire du LSP (ADR-015, M7.6) :
// seul module du dépôt dépendant de la bibliothèque LSP4J et de Gson.
// L'artefact dit sa fonction, jamais sa marque (règle de nommage du
// propriétaire, 2026-11-09) ; le module porte LspSession, la
// correspondance des types et la négociation de capacités — aucun type
// de la bibliothèque ne paraît dans son API (implementation, comme
// joni dans core-syntax-grammars).
plugins {
    id("celeris.java-library-conventions")
    id("celeris.publishing-conventions")
}
dependencies {
    api(project(":core-lsp"))
    // La bibliothèque et son rouage JSON : détails internes du module.
    implementation(libs.lsp4j)
    // Copie unique maîtrisée à l'octet (ADR-015, décision 3) : la
    // bibliothèque exige gson dans l'intervalle [2.9.1, 3.0) — la
    // contrainte STRICTE verrouille la version épinglée au catalogue,
    // jamais la plus récente venue du réseau.
    constraints {
        implementation(libs.gson) {
            version { strictly(libs.versions.gson.get()) }
            because("l'intervalle de la bibliothèque laisse flotter la version ; la copie unique exige la maîtrise de l'octet")
        }
    }
}

// Chantier B2 (revue du 2026-11-17) : les règles de conservation se
// publient DANS le jar — R8 lit META-INF/proguard/ des jars de
// dépendances, l'hôte n'a plus à copier le fichier à la main. La
// source unique reste regles-conservation.pro à la racine (la
// exemple consommateur, hors périmètre, l'y lit) ; cette tâche la recopie
// vers les ressources générées — jamais deux fois le même texte à
// maintenir.
val reglesEmbarquees = tasks.register<Sync>("embarquerReglesR8") {
    from("regles-conservation.pro")
    into(layout.buildDirectory.dir("generated-resources/proguard/META-INF/proguard"))
    rename { "celeris-lsp.pro" }
}
// La racine des ressources générées se déclare au source set (le
// PARENT de META-INF — le fichier garde son chemin relatif).
sourceSets {
    main {
        resources.srcDir(layout.buildDirectory.dir("generated-resources/proguard"))
    }
}
tasks.named("processResources") {
    dependsOn(reglesEmbarquees)
}
tasks.named("sourcesJar") {
    dependsOn(reglesEmbarquees)
}
// D-09 : le banc des connexions externes lance le factice en
// SOUS-PROCESSUS — il reçoit le classpath d'exécution des tests.
tasks.named<Test>("test") {
    // le sous-processus du banc hérite du classpath d'exécution des tests
    systemProperty("celeris.banc.classpath",
            sourceSets["test"].runtimeClasspath.asPath)
}

// Le banc des règles embarquées lit le JAR compilé (build/libs) :
// Gradle doit le bâtier AVANT les tests — sans ce pont, l'ordre des
// tâches laisse le banc lire un jar d'une construction passée.
tasks.named("test") {
    dependsOn(tasks.named("jar"))
}
