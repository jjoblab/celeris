// android-widgets — AGP appliqué ici (accesseurs typés), réglages communs dans la
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
    api(project(":core-intelligence"))
    // ADR-013 : les tests de vue de la couche de popups tournent sur
    // JVM via Robolectric — lanceur JUnit 4 porté par le moteur vintage.
    testImplementation(libs.robolectric)
    // ADR-013 : le jar android-all du SDK 35 pour les tests de la couche
    // de popups — résolu par Gradle via la configuration de la convention
    // (tâche robolectricDeps), jamais téléchargé par Robolectric au
    // démarrage du test (429 de Maven Central constatés le 2026-11-08).
    "sdkRobolectric"(libs.robolectric.android.all)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage)
}

// ADR-013 : la façade Robolectric porte des annotations @RequiresApi
// que la lecture de son classfile signale sans android.annotation au
// classpath de test — le lint « classfile » se coupe ici, uniquement
// pour ce bruit tiers ; -Werror reste actif pour le reste.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-classfile")
}
