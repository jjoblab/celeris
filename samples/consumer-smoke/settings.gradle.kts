// L'exemple consommateur « vierge » (chantier B5, revue du
// 2026-11-17) : un build INDÉPENDANT du dépôt — jamais inclus au
// settings.gradle.kts racine. Il prouve l'intégration comme un hôte
// réel la vivrait : dépendance unique `celeris` résolue depuis un
// dépôt Maven (mavenLocal() en CI avant le tag, JitPack après), la
// façade seule, la variante release MINIFIÉE (R8 lit les règles
// embarquées de lsp-protocole — chantier B2 — et le manifeste
// fusionné reste honnête — chantier A2).
//
// La version de celeris se pilote par propriété (-PcelerisVersion=…),
// mavenLocal() d'abord : le même build sert au contrôle continu et,
// après un tag, à la validation JitPack (jitpack.yml du dépôt).
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "consumer-smoke"
include(":app")
