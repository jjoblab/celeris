// android-view — AGP appliqué ici (accesseurs typés), réglages communs dans la
// convention celeris.android-* ; aucune version n'est écrite dans ce
// fichier (catalogue unique, ADR-011).
plugins {
    alias(libs.plugins.android.library)
    id("celeris.android-library-conventions")
    // Publication Android différée à l'arrivée du code correspondant
    // (M2/M3, écart n° 5 du résumé M1) : la convention commune exige le
    // composant « java » du plugin JVM, qu'AGP ne fournit pas.
}
dependencies {
    api(project(":core-ai"))
    api(project(":core-intelligence"))
    api(project(":core-language"))
    // ADR-013 : les tests de vue tournent sur JVM via Robolectric —
    // lanceur JUnit 4 porté sur la plateforme Jupiter par le moteur
    // vintage (mêmes tâches, mêmes rapports).
    testImplementation(libs.robolectric)
    // ADR-013 : le jar android-all du SDK 35 pour les tests de vue —
    // résolu par Gradle via la configuration de la convention (tâche
    // robolectricDeps), jamais téléchargé par Robolectric au démarrage
    // du test (429 de Maven Central constatés le 2026-11-08).
    "sdkRobolectric"(libs.robolectric.android.all)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage)
}

// Chantier C10 : les bancs de charge (GrosFichiersTest) ouvrent des
// documents de 480 000 lignes — le tas par défaut des tests (512 Mo)
// fait vivre les bancs en pleine pression GC et rend les mesures non
// représentatives. 1 Go, comme le module de syntaxe.
tasks.withType<Test>().configureEach {
    maxHeapSize = "1g"
}
