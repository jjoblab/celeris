// core-language — les réglages communs vivent dans build-logic (ADR-002) ; ce
// fichier ne déclare que la nature du module et ses dépendances.
import java.io.File

plugins {
    id("celeris.java-library-conventions")
    id("celeris.publishing-conventions")
}
dependencies {
    api(project(":core-text"))
    api(project(":core-syntax"))
}

// L'index des paquets intégrés (M7.6) : la liste des dossiers sous
// resources/langages s'écrit dans langages/index.json à chaque
// construction — l'énumération directe du classpath ne traverse ni les
// jar ni les dex de la même façon, l'index si. Ajouter un langage =
// ajouter un dossier : l'index suit, personne ne tient la liste à la
// main (la double liste de LangagesGrammaires ne renaîtra pas).
val dossierPaquets = "src/main/resources/langages"
val tacheIndex = tasks.register<GeneratePackIndexTask>("generatePackIndex") {
    packsDirectory.set(layout.projectDirectory.dir(dossierPaquets))
    indexFile.set(layout.buildDirectory.file("generated-resources/langages/index.json"))
}
// La sortie de la tâche vit sous une racine de ressources déclarée au
// source set : la racine est le PARENT — le fichier garde son chemin
// relatif {langages/index.json} dans la sortie (dependsOn explicite —
// le cache de configuration n'aime pas les tâches dans les srcDir).
sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.dir("generated-resources"))
        }
    }
}
tasks.named<ProcessResources>("processResources") {
    dependsOn(tacheIndex)
}
// Le jar de sources embarque les ressources du source set (le jar de
// sources de la convention) : la dépendance sur l'index se déclare.
tasks.named<Jar>("sourcesJar") {
    dependsOn(tacheIndex)
}

/** Écrit l'index trié des paquets d'un dossier. */
abstract class GeneratePackIndexTask : DefaultTask() {
    @get:InputFiles
    abstract val packsDirectory: DirectoryProperty

    @get:OutputFile
    abstract val indexFile: RegularFileProperty

    @TaskAction
    fun write() {
        val dossier = packsDirectory.get().asFile
        val ids = dossier.listFiles { f: File -> f.isDirectory }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
        val sortie = indexFile.get().asFile
        sortie.parentFile.mkdirs()
        // Tableau JSON plat, trié : la forme lue par Packs.classpath.
        sortie.writeText(ids.joinToString(prefix = "[", postfix = "]\n",
            separator = ", ") { "\"$it\"" })
        logger.lifecycle("index des paquets : {} langage(s)", ids.size)
    }
}
