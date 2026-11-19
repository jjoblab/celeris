// L'application d'exemple (chantier B5) : le cas commun d'un hôte —
// une dépendance (celeris), le gabarit XML d'USAGE § 4.1, la façade
// d'USAGE § 4.2, RIEN d'autre. La variante release est minifiée : la
// preuve B2 (R8 lit les règles embarquées) y passe à chaque build.
plugins {
    // Version explicite : ce build est indépendant, il ne partage ni
    // catalogue ni build-logic avec le dépôt (chantier B5).
    id("com.android.application") version "9.5.1"
}

android {
    namespace = "com.example.consumer"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        // La cible suit le SDK compilé ; l'exemple ne court pas sur
        // store, il prouve l'intégration.
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            // La preuve R8 : minifié, avec les règles par défaut —
            // celles de celeris viennent des jars (B2).
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}

dependencies {
    // LA dépendance du cas courant (chantier B1) : l'agrégateur.
    // La version vient de la propriété du build racine (CI, JitPack)
    // avec repli sur la version publiée courante.
    implementation("jo.celeris:celeris:${project.findProperty("celerisVersion") ?: "2.0.0"}")
}
