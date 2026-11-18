// android-host — AGP appliqué ici (accesseurs typés), réglages communs dans la
// convention celeris.android-* ; aucune version n'est écrite dans ce
// fichier (catalogue unique, ADR-011).
plugins {
    alias(libs.plugins.android.library)
    id("celeris.android-library-conventions")
    // ADR-011 (décision du propriétaire du 2026-11-11) : publié comme les
    // modules purs — variante release, sources, Javadoc, SBOM, sommes.
    id("celeris.publishing-conventions")
}
dependencies {
    api(project(":core-language"))
    // L'étage de composition de l'hôte (le déménagement des popups,
    // décision du propriétaire du 2026-11-14 : les fonctionnalités
    // vivent dans les modules de l'éditeur, l'exemple consommateur ne teste
    // plus qu'elle-même) : l'orchestration des popups — PopupsEditeur,
    // DocAncre, BordDroit — voit la vue (géométrie et déclencheurs),
    // le kit des popups (cartes et manager), la frontière LSP et
    // les moteurs purs des surfaces réveillées (LecteurTypes…).
    api(project(":android-view"))
    api(project(":android-widgets"))
    api(project(":core-lsp"))
    api(project(":core-intelligence"))
    // ADR-013 : les bancs de montage de l'orchestrateur (cycle de vie
    // C1) tournent sur JVM via Robolectric — lanceur JUnit 4 porté sur
    // la plateforme Jupiter par le moteur vintage, comme android-view.
    testImplementation(libs.robolectric)
    // Le jar android-all du SDK des tests de vue — résolu par Gradle
    // (tâche robolectricDeps de la convention), jamais par le
    // résolveur HTTP de Robolectric.
    "sdkRobolectric"(libs.robolectric.android.all)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage)
}
