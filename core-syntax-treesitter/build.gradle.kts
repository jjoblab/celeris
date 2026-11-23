// core-syntax-treesitter — la voie tree-sitter de la coloration
// (ADR-0024, révisée ADR-0025, généralisée ADR-0033) : module OPT-IN,
// bibliothèque Android qui consomme le CŒUR de la liaison AndroidIDE
// par Maven — le dépôt ne compile aucun C, n'embarque AUCUNE
// grammaire : les grammaires vivent chez l'hôte (l'artefact
// tree-sitter-<langue> qu'il choisit), enregistrées par
// TreeSitterGrammars.enregistrer avec SA requête highlights.scm.
// L'hôte qui veut la voie l'embarque explicitement (jamais dans la
// BOM ni l'agrégateur) ; sur JVM (bancs), le chargement échoue
// proprement : presente() rend false, les bancs natifs se sautent
// (assumeTrue).
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
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Les grammaires des BANCS (ADR-0033) : le module n'embarque
    // plus AUCUNE grammaire (elles vivent chez l'hôte) — les bancs
    // natifs json/python en prennent deux, le temps de prouver le
    // châssis (le modèle hôte : l'artefact de la grammaire choisie).
    testImplementation(libs.tree.sitter.json)
    testImplementation(libs.tree.sitter.python)

    // Le jar android-all du SDK des bancs (la convention crée la
    // configuration sdkRobolectric et sa tâche robolectricDeps —
    // SANS cette déclaration la configuration reste VIDE et le
    // provider de la tâche casse compileDebugJavaWithJavac :
    // « Cannot query the value of this provider » ; constaté à la
    // base le 2026-11-23, module jamais compilé depuis l'ADR-0025).
    // Les bancs actuels du module sont JVM purs (assumeTrue saute la
    // liaison absente) : le jar ne les sert pas encore, les bancs
    // Robolectric à venir le trouveront déjà résolu.
    "sdkRobolectric"(libs.robolectric.android.all)
}
