// demo — terrain de démonstration complet, non publiée ; ne voit que
// l'API publique des modules (règle d'or du mandat, ADR-009).
plugins {
    alias(libs.plugins.android.application)
    id("celeris.android-application-conventions")
}

// L'APK doit porter un numéro de version : sans versionCode ni
// versionName, le manifeste reste muet et certains installateurs
// refusent le paquet. La version vit au catalogue (ADR-011).
// Le coureur d'AndroidX porte JUnit 4 sur l'appareil du second étage
// (C11) — le banc d'instrumentation vit dans src/androidTest.
android {
    defaultConfig {
        versionCode = 1
        versionName = libs.versions.demo.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    // C11 — l'appareil géré du second étage : l'image ATD (allégée pour
    // la CI, KVM du runner Ubuntu) ne se télécharge que pour la tâche
    // d'instrumentation, jamais pour le build courant. L'hôte sans
    // virtualisation matérielle la laisse à la CI — la commande exacte
    // vit au document de vérification. (AGP 9 : le conteneur local
    // des appareils virtuels porte le nom localDevices.)
    testOptions {
        managedDevices {
            localDevices {
                register("pixel2api34") {
                    device = "Pixel 2"
                    apiLevel = 34
                    systemImageSource = "aosp-atd"
                }
            }
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
    // Le mini serveur de langage Java (JVM pure) : la démonstration le
    // déclare en donnée (serveurs/servers.json) et le monte EN PROCESSUS
    // par la désignation — diagnostics poussés, complétion et survol
    // réels pour les fichiers Java, sans archive tierce.
    implementation(project(":serveur-lsp-java"))
    // L'interface de la démonstration : Material 3, barres d'application,
    // cartes, liste et bord-à-bord — les seules dépendances externes du
    // dépôt côté exécution (la démo n'est pas publiée, ADR-011).
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.material)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    // Étage appareil (C11) : le coureur et le couplage ext portent
    // JUnit 4 sur l'appareil géré ; UiAutomator pince, tape et capture
    // sur la vitre — jamais dans les artefacts publiés (la démo n'est
    // pas publiée, ADR-011).
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.android.test.ext.junit)
    androidTestImplementation(libs.android.uiautomator)
}
