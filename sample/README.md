# sample — l'exemple consommateur du dépôt

L'application d'exemple **née après la purge de la démonstration
historique** (ADR-0024, F.7 du 2026-11-18) : un module neuf, volontairement
petit, qui montre **le chemin court d'un hôte réel** — la façade
[`CelerisEditor`](../android-host) et le chargeur de langues
[`LanguageKit`](../core-language), rien d'autre.

```java
LanguageKit langages = LanguageKit.builder(LanguageKit.sharedDaemon())
        .grammarFactory(PackGrammars.INSTANCE)
        .builtinPacks()
        .build();
langages.preload();

CelerisEditor editeur = CelerisEditor.attach(vue, couche, langages);
editeur.open("Salut.java", texte);
editeur.settings().setTheme(EditorThemes.SOMBRE);
editeur.settings().setFontSizeSp(14f);
// fin de vie : editeur.close();
```

## Ce qu'elle est — et ce qu'elle n'est pas

- **Elle est** : le code d'exemple lisible, le montage minimal du § 4.2
  d'`USAGE.md` en une activité unique (`MainActivity`), sans ressource de
  gabarit — la scène se pose en code pour que tout ce que l'hôte écrit
  reste visible au même endroit.
- **Elle n'est pas** : un terrain de démonstration complet (l'ancienne
  démonstration portait les surfaces d'agent, de correctif, d'IA… tout ce
  qui a déménagé dans le kit lui-même) ni une preuve d'intégration
  mécanique — [`samples/consumer-smoke`](../samples/consumer-smoke) reste
  la preuve R8/mavenLocal de la CI (chantier B5).

## Règles du module

- **Jamais publiée** : aucune convention de publication, jamais sur
  JitPack, jamais dans la BOM ni l'agrégateur (directive du propriétaire
  du 2026-11-18, ADR-0024 décision 6).
- **Une seule dépendance** : l'agrégateur `celeris` — le LSP embarqué
  reste optionnel et l'exemple ne le prend pas.
- **Règle d'or du mandat** (ADR-009) : elle ne voit QUE l'API publique du
  kit ; le jour où `sample` a besoin d'un symbole privé, c'est le kit qui
  manque d'une API, pas l'exemple qui triche.
- Mêmes portes qualité que le reste du dépôt (convention application :
  Checkstyle, PMD, Error Prone, NullAway, lint bloquant) ; la CI
  l'assemble à chaque poussée.

## Construire

```bash
./gradlew :sample:assembleDebug
```

L'APK sort dans `sample/build/outputs/apk/debug/`. La version vit au
catalogue (`libs.versions.toml`, `sample`).
