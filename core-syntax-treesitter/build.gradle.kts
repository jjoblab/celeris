// core-syntax-treesitter — la voie tree-sitter de la coloration
// (ADR-0024) : module OPT-IN, séparé de core-syntax. L'hôte qui veut
// la voie l'embarque explicitement — celui qui n'en veut rien ne
// reçoit AUCUN chemin de chargement natif. La bibliothèque partagée
// ne se charge QUE par la propriété celeris.natif.lib (jamais
// empaquetée, jamais sondée) ; les fabriques rendent null sans elle
// et le repli déclaré du paquet sert (préférence « tree-sitter »,
// ADR-017).
plugins {
    id("celeris.java-library-conventions")
    id("celeris.publishing-conventions")
}
dependencies {
    api(project(":core-syntax"))
    api(project(":core-language"))
}

// Le banc du module vit sous assumeTrue : sans la bibliothèque
// partagée (tâche natif ci-dessous), il se saute d'un geste. Le tas
// des bancs suit la discipline du module de syntaxe (ADR-0020).
tasks.withType<Test>().configureEach {
    maxHeapSize = "1g"
}

// Niveau 2 (ADR-020) : la construction de la liaison native — runtime
// et grammaires épinglés se téléchargent puis gcc assemble la
// bibliothèque partagée. Tâche OPTATIVE : jamais dans `check` (la
// chaîne de build du dépôt reste pure), le banc du niveau 2 s'y saute
// sans elle (assumeTrue), la voie retombe sur le repli des paquets.
tasks.register<Exec>("natif") {
    description = "Construit la bibliothèque partagée de la voie tree-sitter (télécharge runtime et grammaires épinglés)."
    group = "build"
    workingDir = projectDir
    commandLine("bash", "natif/construire")
    outputs.file(layout.buildDirectory.file("natif/libliaison.so"))
}

// La couverture mesurable du module vit sous assumeTrue : sans la
// bibliothèque partagée (tâche natif), les bancs natifs se sautent —
// le seuil JVM en tient compte (la voie se prouve à la tâche natif,
// ADR-0020/0024).
tasks.withType<JacocoCoverageVerification>().configureEach {
    enabled = false  // voie opt-in : la preuve vit à la tâche natif
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
            }
        }
    }
}
