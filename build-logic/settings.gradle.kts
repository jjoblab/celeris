// build-logic — build isolé qui compile les plugins de convention.
// Le DSL Kotlin reste confiné ici et dans les fichiers de build des
// modules (ADR-001) : aucun Kotlin ne fuit dans les artefacts publiés.
dependencyResolutionManagement {
    repositories {
        // PATCH LOCAL SANDBOX (jamais commité) : miroir Aliyun en tête.
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
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
