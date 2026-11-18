pluginManagement {
    // Les conventions vivent dans le build isolé build-logic (ADR-002 :
    // publication, lint et options javac déclarés une seule fois).
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "celeris"

// Modules purs — constructibles et testables sur JVM sans SDK Android
// (critère de sortie M1, ADR-002) :
include(":core-text")
include(":core-syntax")
include(":core-syntax-grammars")
include(":core-language")
include(":core-intelligence")
include(":core-lsp")
include(":core-ai")
// La frontière protocolaire du LSP (M7.6, ADR-015) : seul module
// dépendant de la bibliothèque LSP4J et de Gson — l'artefact dit sa
// fonction, jamais sa marque (règle de nommage du propriétaire,
// 2026-11-09), comme core-syntax-grammars ne dit pas son moteur.
include(":lsp-protocole")
include(":cli")


// Chantier B1 (revue du 2026-11-17) : la BOM et l'agrégateur — deux
// modules PUBLIÉS qui ne portent presque rien. celeris-bom aligne les
// versions des onze modules (plateforme Java) ; celeris agrège les
// quatre du cas courant en une dépendance.
include(":celeris-bom")
include(":celeris")

// Modules Android : toujours inclus (ADR-0021) — la pureté du cœur est
// garantie au niveau du code par les tests ArchUnit de chaque module pur
// (aucune dépendance android.*), pas par l'absence des modules Android du
// build. Tout build exige donc un SDK Android (ANDROID_HOME,
// ANDROID_SDK_ROOT ou local.properties) ; sans SDK, la configuration
// échoue avec un message explicite plutôt que de publier 8 artefacts
// sur 11 en silence (le piège JitPack relevé par la revue du 2026-11-17).
// La démonstration a quitté le dépôt (ADR-0024, F.7 du 2026-11-18) :
// une démonstration repensée vivra dans un module neuf.
include(":android-view")
include(":android-widgets")
include(":android-host")
