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
    // La voie native (core-syntax-treesitter) exige le désucrage des
    // bibliothèques du cœur — même chaîne que le module lui-même.
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
        targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
    }
    defaultConfig {
        versionCode = 2
        versionName = libs.versions.sample.get()
    }
    testOptions {
        // ADR-013 : le banc du montage (Robolectric) lit les vraies
        // ressources de l'application — layouts, thème, menu, icônes.
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Robolectric référence android.annotation.RequiresApi depuis son jar ;
// le classpath de TEST de l'application ne le porte pas (contrairement
// aux modules de bibliothèque, dont le classpath de test inclut
// android.jar entier) : javac émet un warning classfile à la lecture de
// Robolectric.class, et -Werror le porte en erreur. Le warning se coupe
// pour le SEUL travail de compilation des bancs — le source principal
// garde -Xlint:all -Werror entier.
tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().matching {
    it.name == "compileDebugUnitTestJavaWithJavac"
}.configureEach {
    options.compilerArgs.add("-Xlint:-classfile")
}

dependencies {
    // Une seule dépendance : l'agrégateur du cas courant (B1) — vue,
    // widgets, couche hôte (façade, popups) et voie de grammaires.
    // Le LSP embarqué reste optionnel : l'exemple ne le prend pas.
    implementation(project(":celeris"))
    // Le serveur factice de l'exemple (le retour terrain du
    // 2026-11-19) : le montage embarqué du protocole — un VRAI LSP
    // in-process, sans sous-processus.
    implementation(project(":lsp-protocole"))
    // Le retour terrain du 2026-11-19 : « utiliser de vrais layout
    // pour le demo, qu'il soit plus moderne » — Material 3 porte le
    // tiroir (DrawerLayout + NavigationView), la barre d'outils et
    // la barre d'onglets ; l'exemple n'est pas publiée (ADR-0024),
    // la dépendance ne traverse jamais le kit.
    implementation(libs.google.material)

    // L'option tree-sitter (le retour terrain du 2026-11-23 :
    // « ajoute aussi l'option pour tester tree-sitter ») : le module
    // opt-in de la voie native s'inscrit par ServiceLoader (ADR-0024)
    // — sa seule présence au classpath branche la voie native dès que
    // le langage servi (json, python) se reconnaît ; la préférence
    // déclarée du paquet reprend sinon les grammaires TextMate.
    implementation(project(":core-syntax-treesitter"))
    // La voie native exige le désucrage des bibliothèques du cœur
    // (même chaîne que core-syntax-treesitter : le sample n'avait pas
    // encore cette dépendance AVANT l'option tree-sitter).
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Le banc du montage (ADR-013) : l'activité gonfle, branche et
    // vit — tiroir, onglets, journal, éditeur. Robolectric porte la
    // scène sur JVM ; le jar android-all se résout par la
    // configuration sdkRobolectric de la même convention que les
    // modules de vue (jamais téléchargé par Robolectric au
    // démarrage).
    testImplementation(libs.robolectric)
    "sdkRobolectric"(libs.robolectric.android.all)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage)
}
