// demo — terrain de démonstration complet, non publiée ; ne voit que
// l'API publique des modules (règle d'or du mandat, ADR-009).
plugins {
    alias(libs.plugins.android.application)
    id("celeris.android-application-conventions")
}

dependencies {
    implementation(project(":android-view"))
    // Point de composition des éditions d'espace de travail : la démo
    // voit le client LSP et le moteur de gabarits — les modules purs
    // restent latéraux (ADR-002, tranche 6 M6).
    implementation(project(":core-lsp"))
    implementation(project(":android-widgets"))
    implementation(project(":android-host"))
}
