// sample — l'exemple consommateur du dépôt (ADR-0024, décision 6) :
// application Android NON PUBLIÉE, jamais sur JitPack (directive du
// propriétaire du 2026-11-18). Elle ne voit QUE l'API publique du kit
// (règle d'or du mandat, ADR-009) : la façade CelerisEditor et le
// chargeur de langues du § 4.2 d'USAGE — le chemin court, celui que
// l'hôte réel suivra. samples/consumer-smoke (B5) reste la preuve
// mécanique d'intégration (mavenLocal, R8) ; sample montre le code.
plugins {
    alias(libs.plugins.android.application)
    id("celeris.android-application-conventions")
}

// L'APK porte son numéro de version (certains installateurs le
// refusent muet) ; la version vit au catalogue (ADR-011).
android {
    namespace = "jo.celeris.sample"
    defaultConfig {
        versionCode = 1
        versionName = libs.versions.sample.get()
    }
}

dependencies {
    // Une seule dépendance : l'agrégateur du cas courant (B1) — vue,
    // widgets, couche hôte (façade, popups) et voie de grammaires.
    // Le LSP embarqué reste optionnel : l'exemple ne le prend pas.
    implementation(project(":celeris"))
}
