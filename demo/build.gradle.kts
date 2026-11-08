// demo — terrain de démonstration complet, non publiée ; ne voit que
// l'API publique des modules (règle d'or du mandat, ADR-009).
plugins {
    alias(libs.plugins.android.application)
    id("celeris.android-application-conventions")
}

// L'APK doit porter un numéro de version : sans versionCode ni
// versionName, le manifeste reste muet et certains installateurs
// refusent le paquet. La version vit au catalogue (ADR-011).
android {
    defaultConfig {
        versionCode = 1
        versionName = libs.versions.demo.get()
    }
    // Le serveur XML embarqué (M8.6, LemMinX — voir NOTICE) touche
    // les API Java 11 au-delà du niveau 26 : le désucrage des
    // bibliothèques du cœur les rend sur l'appareil.
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
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
    // Le terrain d'IA native : contrats fantôme, prédiction et revue
    // d'agent (F-01 à F-10, ADR-012) — faux modèles déterministes, la
    // démonstration ne branche aucun réseau.
    implementation(project(":core-ai"))
    // L'interface de la démonstration : Material 3, barres d'application,
    // cartes, liste et bord-à-bord — les seules dépendances externes du
    // dépôt côté exécution (la démo n'est pas publiée, ADR-011).
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.material)
    // Le serveur de langage XML RÉEL (M8.6 — LemMinX, identité, licence
    // et provenance au NOTICE) : découpe sur mesure de l'artefact
    // ubérifié — le serveur, sa liaison et ses analyseurs, la
    // bibliothèque de collections réduite à sa fermeture de portée,
    // les doublons de plateforme ôtés. Lancé EN PROCESSUS par son API,
    // jamais par sa ligne de commande (voir ServeurXmlEmbarque).
    implementation(files("libs/serveur-xml-0.31.2.jar"))
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
