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
    // Le terrain d'essai ouvre de vrais fichiers : sessions, analyseurs
    // dédiés et voie de grammaires (C-14 à C-16, tranche 1 M7).
    implementation(project(":core-text"))
    implementation(project(":core-syntax"))
    implementation(project(":core-syntax-grammars"))
    // L'interface de la démonstration : Material 3, barres d'application,
    // cartes, liste et bord-à-bord — les seules dépendances externes du
    // dépôt côté exécution (la démo n'est pas publiée, ADR-011).
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.material)
}
