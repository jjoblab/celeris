// build-logic — build isolé qui compile les plugins de convention.
// Le DSL Kotlin reste confiné ici et dans les fichiers de build des
// modules (ADR-001) : aucun Kotlin ne fuit dans les artefacts publiés.
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            // Catalogue unique : celui du dépôt racine (ADR-011).
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"

include(":convention")
