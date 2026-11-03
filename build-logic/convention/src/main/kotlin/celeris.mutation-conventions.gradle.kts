// Convention de mutation (ADR-010) : exécution de PIT en ligne de commande
// sur les tests du module, puis vérification du score au seuil. Appliquée
// à core-text ; extensible à tout module pur en conservant le seuil.
//
// Compatibilité cache de configuration : les lambdas doFirst/doLast ne
// capturent que des locaux purs (chemins java.io.File) déclarés dans le
// bloc d'enregistrement — jamais une val de niveau script.

import jo.celeris.buildlogic.MutationThresholdCheck
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

// Lanceur PIT résolu par coordonnées (le seuil et le rapport appartiennent
// au module, pas à un plugin tiers).
val pitestRunner = configurations.create("pitestRunner") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    "pitestRunner"(libs.findLibrary("pitest-cli").get())
    "pitestRunner"(libs.findLibrary("pitest-junit5").get())
}

val reportDir = layout.buildDirectory.dir("reports/pitest")
val cible = "jo.celeris.${project.name.replace('-', '.')}.*"
// Les classes de test ne sont jamais des cibles de mutation : seules les
// lignes de production comptent pour le score (ADR-010).
val ciblesExclues = "jo.celeris.${project.name.replace('-', '.')}.*Test"
val sourceSets = extensions.getByType<SourceSetContainer>()

tasks.register<JavaExec>("mutationCheck") {
    group = "verification"
    description = "Exécute la mutation (PIT) sur les tests du module."
    val dirRapport = reportDir.get().asFile
    // Un rapport périmé ne doit jamais passer pour un résultat frais.
    doFirst { dirRapport.deleteRecursively() }
    // Sans mutation, PIT n'écrit pas de rapport : en produire un vide
    // explicite pour que le seuil distingue « rien à muter » d'un échec.
    doLast {
        val rapport = dirRapport.resolve("mutations.xml")
        if (!rapport.exists()) {
            rapport.parentFile.mkdirs()
            rapport.writeText(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<mutations/>\n",
            )
        }
    }
    classpath = pitestRunner + sourceSets.named("main").get().runtimeClasspath +
        sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
    args(
        "--targetClasses", cible,
        "--targetTests", cible,
        "--excludedClasses", ciblesExclues,
        "--sourceDirs", "src/main/java",
        "--reportDir", dirRapport.absolutePath,
        "--outputFormats", "XML",
        "--mutators", "DEFAULTS",
        // Le squelette n'a aucune ligne de production à muter : l'absence
        // de mutation n'est pas un échec, le seuil (mutationThreshold)
        // reste bloquant dès la première ligne réelle.
        "--failWhenNoMutations", "false",
        "--verbose",
    )
}

tasks.register<MutationThresholdCheck>("mutationThreshold") {
    group = "verification"
    description = "Vérifie le score de mutation (>= 80 %, ADR-010)."
    report.set(reportDir.map { it.file("mutations.xml") })
    threshold.set(80)
    dependsOn("mutationCheck")
}
