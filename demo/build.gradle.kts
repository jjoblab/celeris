// demo — terrain de démonstration complet, non publiée ; ne voit que
// l'API publique des modules (règle d'or du mandat, ADR-009).
plugins {
    alias(libs.plugins.android.application)
    id("celeris.android-application-conventions")
}

dependencies {
    implementation(project(":android-view"))
    implementation(project(":android-widgets"))
    implementation(project(":android-host"))
}
