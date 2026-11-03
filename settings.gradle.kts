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
include(":core-language")
include(":core-intelligence")
include(":core-lsp")
include(":core-ai")
include(":cli")

// Modules Android : inclus uniquement si un SDK est détecté — l'absence de
// SDK ne doit jamais casser le build des modules purs ; la CI, elle,
// installe le SDK et construit l'ensemble.
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

if (sdkAndroid != null) {
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
