// Module de conventions : les plugins précompilés vivent dans
// src/main/kotlin ; les classes d'appui (manifeste d'API, seuil de
// mutation) dans le même arbre.
plugins {
    `kotlin-dsl`
}

group = "jo.celeris.buildlogic"

dependencies {
    // Sur le classpath d'exécution des conventions : la résolution des
    // identifiants de plugin (com.android.library, net.ltgt.errorprone,
    // org.cyclonedx.bom) se fait par ce classpath, sans version en dur
    // dans les modules.
    implementation(libs.android.gradlePlugin)
    implementation(libs.errorprone.gradlePlugin)
    implementation(libs.cyclonedx.gradlePlugin)
}
