// Convention de publication (ADR-011) : artefacts Maven complets (sources,
// Javadoc, POM), SBOM CycloneDX et sommes de contrôle SHA-256 à chaque
// publication ; cible confirmée par le propriétaire : le registre de
// paquets de l'hébergeur, privé en pré-1.0. Les identifiants viennent de
// l'environnement (REGISTRY_USER / REGISTRY_TOKEN), jamais du dépôt.

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    `maven-publish`
    id("org.cyclonedx.bom")
}

afterEvaluate {
    extensions.configure(org.gradle.api.publish.PublishingExtension::class.java) {
        publications {
            register<MavenPublication>("celeris") {
                // Modules Android : variante release ; modules purs : java.
                val varianteAndroid = components.findByName("release")
                if (varianteAndroid != null) {
                    from(varianteAndroid)
                } else {
                    from(components.getByName("java"))
                }
                artifactId = project.name
                pom {
                    name.set("celeris ${project.name}")
                    description.set("Module ${project.name} de la bibliothèque celeris.")
                    url.set("https://github.com/jjoblab/celeris")
                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/jjoblab/celeris.git")
                        developerConnection.set("scm:git:git@github.com:jjoblab/celeris.git")
                        url.set("https://github.com/jjoblab/celeris")
                    }
                }
            }
        }
        repositories {
            maven {
                name = "hebergeur"
                url = uri("https://maven.pkg.github.com/jjoblab/celeris")
                credentials {
                    username = System.getenv("REGISTRY_USER") ?: ""
                    password = System.getenv("REGISTRY_TOKEN") ?: ""
                }
            }
        }
    }
}

// SBOM généré à chaque publication (ADR-011).
tasks.named("publish") {
    dependsOn("cyclonedxBom")
}

// Sommes de contrôle SHA-256 des artefacts produits (POM, module, jar,
// sources, Javadoc — ADR-011), écrites à côté d'eux.
tasks.register("publicationChecksums") {
    group = "publication"
    description = "Écrit les sommes de contrôle SHA-256 des artefacts publiés."
    dependsOn("publishCelerisPublicationToMavenLocal")
    val publicationsDir = layout.buildDirectory.dir("publications/celeris")
    val libsDir = layout.buildDirectory.dir("libs")
    doLast {
        val fichiers = mutableListOf<java.io.File>()
        publicationsDir.get().asFile.listFiles()?.forEach { fichiers.add(it) }
        libsDir.get().asFile.listFiles()?.forEach { fichiers.add(it) }
        val artefacts = fichiers
            .filter { f -> f.isFile && !f.name.endsWith(".sha256") }
            .sortedBy { f -> f.name }
        for (f in artefacts) {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(f.readBytes())
            val hex = digest.joinToString("") { octet -> "%02x".format(octet) }
            java.io.File(f.parentFile, "${f.name}.sha256").writeText("$hex\n")
        }
        logger.lifecycle("sommes de contrôle : {} artefact(s)", artefacts.size)
    }
}
tasks.named("publish") {
    finalizedBy("publicationChecksums")
}
