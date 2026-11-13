// core-syntax — les réglages communs vivent dans build-logic (ADR-002) ; ce
// fichier ne déclare que la nature du module et ses dépendances.
plugins {
    id("celeris.java-library-conventions")
    id("celeris.publishing-conventions")
}
dependencies {
    api(project(":core-text"))
}

// Chantier C10 : le banc de charge (Charge480kLignesTest) ouvre des
// documents de 480 000 lignes (~12 Mo de texte, ~90 Mo d'états et
// caches) — le tas par défaut des tests (512 Mo) fait vivre le banc en
// pleine pression GC et rend les mesures non représentatives. 1 Go :
// les mesures restent stables d'une exécution à l'autre.
tasks.withType<Test>().configureEach {
    maxHeapSize = "1g"
}

// Niveau 2 (ADR-020) : la construction de la liaison native — runtime
// et grammaires épinglés se téléchargent puis gcc assemble la
// bibliothèque partagée. Tâche OPTATIVE : jamais dans `check` (la
// chaîne de build du dépôt reste pure), le banc du niveau 2 s'y saute
// sans elle (assumeTrue), la voie retombe sur le repli des paquets.
tasks.register<Exec>("natif") {
    description = "Construit la bibliothèque partagée du niveau 2 (télécharge runtime et grammaires épinglés)."
    group = "build"
    workingDir = projectDir
    commandLine("bash", "natif/construire")
    outputs.file(layout.buildDirectory.file("natif/libliaison.so"))
}
