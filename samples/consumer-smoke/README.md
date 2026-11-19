# consumer-smoke — l'exemple consommateur « vierge »

> La preuve d'intégration de **celeris** (chantier B5, revue du
> 2026-11-17) : ce build est **indépendant** du dépôt — jamais inclus
> au `settings.gradle.kts` racine. Il consomme les artefacts **publiés**
> comme un hôte réel le ferait, par la dépendance unique `celeris`.

## Ce qu'il prouve à chaque build

| Preuve | Comment |
| --- | --- |
| La dépendance unique suffit (B1) | `implementation("jo.celeris:celeris:…")` — rien d'autre |
| La façade compose tout (B0) | `MainActivity` : `attach` → `open` → `close`, trois lignes |
| R8 lit les règles embarquées (B2) | release **minifiée**, les règles viennent des jars |
| Le manifeste fusionné reste honnête (A2) | pas de LSP embarqué → rien de LSP dans l'APK |
| L'optionnel reste optionnel | `lsp4j` absent du dex tant que `lsp-protocole` n'est pas déclaré |

## Construire

```bash
# 1. publier les artefacts du dépôt en local (depuis la racine du dépôt)
./gradlew :core-text:publishToMavenLocal :core-syntax:publishToMavenLocal \
  :core-syntax-grammars:publishToMavenLocal :core-language:publishToMavenLocal \
  :core-intelligence:publishToMavenLocal :core-lsp:publishToMavenLocal \
  :core-ai:publishToMavenLocal :lsp-protocole:publishToMavenLocal \
  :android-view:publishToMavenLocal :android-widgets:publishToMavenLocal \
  :android-host:publishToMavenLocal :celeris:publishToMavenLocal \
  :celeris-bom:publishToMavenLocal

# 2. assembler l'exemple (release minifié)
cd samples/consumer-smoke
./gradlew :app:assembleRelease
```

La version de celeris se pilote par propriété :
`./gradlew :app:assembleRelease -PcelerisVersion=2.1.0`.

## Après un tag

Le même build consomme JitPack : remplacer `mavenLocal()` par
`maven("https://jitpack.io")` dans `settings.gradle.kts` et la
coordonnée `jo.celeris:celeris` par
`com.github.jjoblab.celeris:celeris:<tag>`. Le job CI
`consumer-smoke` (`.github/workflows/ci.yml`) tient la porte locale
jusque-là.
