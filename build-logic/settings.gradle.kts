// build-logic — build isolé qui compile les plugins de convention.
// Le DSL Kotlin reste confiné ici et dans les fichiers de build des
// modules (ADR-001) : aucun Kotlin ne fuit dans les artefacts publiés.
dependencyResolutionManagement {
    repositories {
        // Le portail des plugins : les marqueurs des plugins des
        // conventions (errorprone, cyclonedx…) vivent lui — le dépôt
        // pointait sur un miroir tiers à leur place (audit D2).
        gradlePluginPortal()
        google()
        mavenCentral()
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
