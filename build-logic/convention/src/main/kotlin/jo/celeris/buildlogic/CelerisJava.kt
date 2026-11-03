// Classes et fonctions d'appui partagées par les plugins de convention.
// Le code de build est le seul Kotlin autorisé dans le dépôt (ADR-001) ;
// les commentaires y sont en français.

package jo.celeris.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import java.io.File

/** Lit une version dans le catalogue unique du dépôt consommateur. */
fun Project.catalogVersion(alias: String): String =
    extensions.getByType(VersionCatalogsExtension::class.java)
        .named("libs").findVersion(alias).get().toString()

/**
 * Réglages javac des modules purs : construction sur JDK 21 (toolchain),
 * artefacts de niveau 17 (ADR-001), avertissements bloquants.
 */
fun Project.configurePureJavac() {
    extensions.findByType(JavaPluginExtension::class.java)?.let { java ->
        java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }
    tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
    }
}

/**
 * Réglages javac des modules Android : mêmes avertissements bloquants ;
 * la compatibilité (niveau 17) est portée par compileOptions, géré par
 * AGP — l'option release est réservée aux modules purs (elle est
 * exclusive des options -source/-target qu'AGP positionne).
 */
fun Project.configureAndroidJavac() {
    tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
    }
}

/**
 * Javadoc française : encodage cohérent (ADR-009 : Javadoc en français).
 * Un module squelette ne documente que des package-info — javadoc signale
 * alors « aucune classe à documenter » en erreur : toléré tant qu'aucune
 * classe réelle n'existe, bloquant dès la première (M2).
 */
fun Project.configureJavadoc() {
    val sources = extensions.findByType(
        org.gradle.api.tasks.SourceSetContainer::class.java,
    )
    val aDesClasses = sources?.named("main")?.get()?.allJava
        ?.filter { it.name != "package-info.java" }
        ?.files?.isNotEmpty()
        ?: true
    tasks.withType<org.gradle.api.tasks.javadoc.Javadoc>().configureEach {
        options.encoding = "UTF-8"
        isFailOnError = aDesClasses
    }
}

/** Plateforme JUnit commune (Jupiter + jqwik + ArchUnit, ADR-010). */
fun Project.configureTestPlatform() {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging.events("failed")
    }
}

/**
 * Génère le manifeste d'API publique d'un module : sortie normalisée de
 * javap -public pour chaque classe compilée (tri par nom binaire). Le
 * manifeste committé fait foi ; toute divergence casse la construction
 * via la tâche de vérification (ADR-009).
 */
abstract class ApiManifestTask : org.gradle.api.DefaultTask() {

    @get:InputFiles
    abstract val classesDirs: ConfigurableFileCollection

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val roots = classesDirs.files.filter { it.isDirectory }
        val entries = roots
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.extension == "class" }
                    .map { it.relativeTo(root).path }
            }
            .sorted()
        val javap = File(System.getProperty("java.home"), "bin/javap")
        val classpath = roots.joinToString(File.pathSeparator)
        val out = StringBuilder()
        for (relative in entries) {
            val binary = relative.removeSuffix(".class").replace('/', '.')
            val process = ProcessBuilder(
                javap.absolutePath, "-public", "-cp", classpath, binary,
            ).redirectErrorStream(true).start()
            val text = process.inputStream.readBytes().decodeToString().trim()
            process.waitFor()
            out.append("### ").append(binary).append('\n')
            out.append(text).append("\n\n")
        }
        manifestFile.get().asFile.writeText(out.toString())
        logger.lifecycle("manifeste d'API : {} classe(s)", entries.size)
    }
}

/**
 * Compare le manifeste d'API régénéré au manifeste committé ; échoue avec
 * la consigne de régénération en cas d'écart (ADR-009 : compatibilité
 * binaire vérifiée mécaniquement).
 */
abstract class ApiCheckTask : org.gradle.api.DefaultTask() {

    @get:InputFile
    abstract val generated: RegularFileProperty

    @get:InputFile
    abstract val committed: RegularFileProperty

    /** Nom du module — capturé à la configuration, jamais relu à l'exécution
     * (cache de configuration : Task.project y est interdit). */
    @get:Input
    abstract val moduleName: Property<String>

    @TaskAction
    fun check() {
        val produced = generated.get().asFile.readText().trim()
        val reference = committed.get().asFile.readText().trim()
        if (produced != reference) {
            throw GradleException(
                "manifeste d'API divergent — régénérer (./gradlew apiManifest), " +
                    "relire la rupture (SemVer, ADR-009) puis committer api/${moduleName.get()}.api",
            )
        }
    }
}

/**
 * Vérifie le score de mutation lu dans le rapport XML de PIT contre le
 * seuil (80 % sur core-text, ADR-010). Sans mutation — squelette sans
 * ligne de production — le contrôle le déclare et passe : le seuil
 * s'applique dès la première ligne mutable.
 */
abstract class MutationThresholdCheck : org.gradle.api.DefaultTask() {

    @get:InputFile
    abstract val report: RegularFileProperty

    @get:Input
    abstract val threshold: org.gradle.api.provider.Property<Int>

    @TaskAction
    fun check() {
        val xml = report.get().asFile.readText()
        val statuses = Regex("""status=["']([A-Z_]+)["']""")
            .findAll(xml).map { it.groupValues[1] }.toList()
        if (statuses.isEmpty()) {
            logger.lifecycle(
                "mutation : aucune mutation (aucune ligne de production) — seuil inapplicable avant M2",
            )
            return
        }
        val killed = statuses.count { it == "KILLED" }
        val score = killed.toDouble() / statuses.size
        if (score * 100 < threshold.get()) {
            throw GradleException(
                "score de mutation %.1f%% inférieur au seuil de %d%%".format(score * 100, threshold.get()),
            )
        }
        logger.lifecycle("mutation : %.1f%% (%d/%d)".format(score * 100, killed, statuses.size))
    }
}
