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

// Modules Android : inclus uniquement si un SDK est détecté — l'absence de
// SDK ne doit jamais casser le build des modules purs ; la CI, elle,
// installe le SDK et construit l'ensemble. Le refus explicite
// CELERIS_SKIP_ANDROID=1 prime sur toute détection : les runners d'hébergement
// préinstallent un SDK que le travail « modules purs » ne doit pas voir.
val refusAndroid = providers.environmentVariable("CELERIS_SKIP_ANDROID").orNull == "1"
val sdkAndroid = providers.environmentVariable("ANDROID_HOME").orNull
    ?: providers.environmentVariable("ANDROID_SDK_ROOT").orNull
    ?: run {
        val proprietes = file("local.properties")
        if (proprietes.exists()) {
            java.util.Properties().apply {
                proprietes.inputStream().use { load(it) }
            }.getProperty("sdk.dir")
        } else {
            null
        }
    }

if (!refusAndroid && sdkAndroid != null) {
    include(":android-view")
    include(":android-widgets")
    include(":android-host")
    include(":demo")
} else {
    logger.lifecycle(
        "celeris : SDK Android absent — modules Android non inclus " +
            "(les modules purs restent constructibles et testables)",
    )
}
