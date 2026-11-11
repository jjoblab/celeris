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
    // La frontière protocolaire du LSP (ADR-015) touche les API Java
    // au-delà du niveau 26 : le désucrage des bibliothèques les rend
    // sur l'appareil.
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
    // La démo réduite (ADR-015) : la preuve R8 du jalon — les règles
    // CONSOMMATEUR de la frontière protocolaire gardent le fil
    // réflexif (rouage JSON, proxys, TypeToken), celles du terrain
    // gardent le serveur embarqué monté par désignation.
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                project(":lsp-protocole").file("regles-conservation.pro"),
                "regles-r8.pro"
            )
        }
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
    // La frontière protocolaire du LSP (ADR-015) : la session qui
    // porte le protocole et l'ordonnancement du domaine — le banc de
    // parité y branche le serveur de référence comme tout serveur
    // déclaré par l'hôte.
    implementation(project(":lsp-protocole"))
    // L'interface de la démonstration : Material 3, barres d'application,
    // cartes, liste et bord-à-bord — les seules dépendances externes du
    // dépôt côté exécution (la démo n'est pas publiée, ADR-011).
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.material)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
