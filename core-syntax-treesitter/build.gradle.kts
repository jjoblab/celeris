// core-syntax-treesitter — la voie tree-sitter de la coloration
// (ADR-0024, révision ADR-0025) : module OPT-IN, bibliothèque
// Android qui consomme la liaison AndroidIDE par Maven — les .so
// suivent les AAR, le dépôt ne compile plus aucun C. L'hôte qui veut
// la voie l'embarque explicitement (jamais dans la BOM ni
// l'agrégateur) ; sur JVM (bancs), le chargement échoue proprement :
// presentE() rend false, les bancs natifs se sautent (assumeTrue).
plugins {
    alias(libs.plugins.android.library)
    id("celeris.android-library-conventions")
    id("celeris.publishing-conventions")
}

android {
    defaultConfig {
        minSdk = 26
    }
    // L'AAR de la liaison exige le désucrage des bibliothèques du
    // cœur (métadonnées coreLibraryDesugaringEnabled, ADR-0025) — la
    // même chaîne que la frontière protocolaire du LSP.
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
    // Les règles de conservation de la liaison suivent l'artefact :
    // R8 les lit chez l'hôte (ADR-0025).
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(project(":core-syntax"))
    api(project(":core-language"))
    // La liaison (ADR-0025) : aucun type de la liaison ne paraît
    // dans l'API du module — implementation, pas api.
    implementation(libs.android.tree.sitter)
    implementation(libs.tree.sitter.json)
    implementation(libs.tree.sitter.python)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
