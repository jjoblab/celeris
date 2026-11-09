// lsp-protocole — la frontière protocolaire du LSP (ADR-015, M7.6) :
// seul module du dépôt dépendant de la bibliothèque LSP4J et de Gson.
// L'artefact dit sa fonction, jamais sa marque (règle de nommage du
// propriétaire, 2026-11-09) ; le module porte SessionLsp, la
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
    // contrainte STRICTE verrouille la version que le serveur XML
    // embarqué apporte déjà, jamais la plus récente venue du réseau.
    constraints {
        implementation(libs.gson) {
            version { strictly(libs.versions.gson.get()) }
            because("l'intervalle de la bibliothèque laisse flotter la version ; la copie unique exige la maîtrise de l'octet")
        }
    }
}
