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
