// Convention des modules purs publiés : bibliothèque Java + manifeste
// d'API publique généré et vérifié (ADR-009) + sources/Javadoc.

import jo.celeris.buildlogic.ApiCheckTask
import jo.celeris.buildlogic.ApiManifestTask

plugins {
    `java-library`
    id("celeris.java-conventions")
}

// Sources et Javadoc font partie de la publication (ADR-011).
extensions.configure(org.gradle.api.plugins.JavaPluginExtension::class.java) {
    withSourcesJar()
    withJavadocJar()
}

// Manifeste d'API : régénéré dans build/api/manifest.txt, comparé au
// manifeste committé api/<module>.api (ADR-009 : ensemble fermé et listé).
val apiManifest = tasks.register<ApiManifestTask>("apiManifest") {
    group = "verification"
    description = "Régénère le manifeste d'API publique du module."
    classesDirs.from(sourceSets.main.get().output.classesDirs)
    manifestFile.set(layout.buildDirectory.file("api/manifest.txt"))
}

val apiCheck = tasks.register<ApiCheckTask>("apiCheck") {
    group = "verification"
    description = "Vérifie le manifeste d'API committé contre l'API compilée."
    generated.set(layout.buildDirectory.file("api/manifest.txt"))
    committed.set(rootProject.file("api/${project.name}.api"))
    dependsOn(apiManifest)
}

tasks.named("check") {
    dependsOn(apiCheck)
}
