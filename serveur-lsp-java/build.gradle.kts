// serveur-lsp-java — mini serveur de langage Java (JVM pure) : le
// compagnon de banc de l'éditeur, montable en processus par la
// frontière protocolaire ou exécutable seul sur l'entrée-sortie
// standard. Aucun Android, aucun réseau, aucune dépendance au-delà de
// la bibliothèque de protocole.
plugins {
    id("celeris.java-library-conventions")
    id("celeris.publishing-conventions")
}

dependencies {
    // La bibliothèque de protocole : détail interne du module, jamais
    // dans son API (comme lsp-protocole, ADR-015).
    implementation(libs.lsp4j)
    // Copie unique maîtrisée à l'octet (ADR-015, décision 3) : la
    // bibliothèque exige gson dans l'intervalle [2.9.1, 3.0) — la
    // contrainte STRICTE verrouille la version épinglée au catalogue,
    // jamais la plus récente venue du réseau.
    constraints {
        implementation(libs.gson) {
            version { strictly(libs.versions.gson.get()) }
        }
    }
    // Le banc de bout en bout monte le serveur par la session cliente
    // de la frontière : la preuve traverse les deux bouts.
    testImplementation(project(":lsp-protocole"))
}
