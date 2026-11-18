# ADR-0021 — Les modules Android toujours construits

- **Statut** : Acceptée (décision du propriétaire, revue du 2026-11-17)
- **Date** : 2026-11-17
- **Amende** : le critère de sortie M1 (ADR-002) — « modules purs constructibles
  et testables sans SDK Android »

## Contexte

Depuis M1, `settings.gradle.kts` n'inclut `:android-view`, `:android-widgets`
et `:android-host` que si un SDK Android est détecté
(`ANDROID_HOME`, `ANDROID_SDK_ROOT` ou `local.properties`) et si
`CELERIS_SKIP_ANDROID` ne vaut pas `1`. Le critère de sortie M1 exigeait que
les modules purs se construisent et se testent sans SDK.

La revue du 2026-11-17 (chantier JitPack) a relevé le coût réel de ce
mécanisme : un build sans SDK **réussit silencieusement** en n'incluant que
les modules purs. Sur un service de construction par étiquette comme JitPack,
un environnement mal configuré publierait 8 artefacts sur 11 sans aucune
erreur — l'absence se confondrait avec la réussite.

Le propriétaire décide le 2026-11-17 : la détection du SDK disparaît, les
trois bibliothèques Android sont toujours construites (§ 0.1, point 2 de la
revue).

## Décision

1. `settings.gradle.kts` inclut **sans condition** `:android-view`,
   `:android-widgets` et `:android-host` ; la variable
   `CELERIS_SKIP_ANDROID`, la détection de SDK et la lecture de
   `local.properties` disparaissent du fichier.
2. **Tout build du dépôt exige désormais un SDK Android** — y compris pour
   construire ou tester les modules purs. L'exigence est assumée et
   documentée (AGENT.md, USAGE.md § 19.9, README).
3. La pureté du cœur — la garantie que M1 cherchait — reste assurée **au
   niveau du code** : chaque module pur (`core-*`, `lsp-protocole`, `cli`)
   porte déjà un test ArchUnit « aucune dépendance
   Android » (`noClasses().should().dependOnClassesThat()
   .resideInAPackage("android..")`) qui échoue au premier `import android.*`.
   Le garde-fou existe donc déjà dans les dix modules ; il n'y a rien à
   ajouter, seulement à le consigner ici. La garantie est même plus forte
   qu'avant : elle tient dans tout build, SDK présent ou non.
4. La CI (`ci.yml`) supprime `CELERIS_SKIP_ANDROID` : tous les travaux
   exigent le SDK (les runners `ubuntu-latest` en préinstallent un ;
   l'étape `sdkmanager` existante installe `platforms;android-36` et
   `build-tools;36.0.0`).

## Options

- **A. Conserver la détection** : le piège de la publication tronquée
  demeure ; refuse.
- **B. Détection + échec explicite sans SDK** : tout build exige déjà le SDK
  par A2/AGP (l'inclusion inconditionnelle produit une erreur AGP claire si
  le SDK manque) ; une double barrière ne simplifie rien ; refuse.
- **C. Inclusion inconditionnelle, garantie par ArchUnit** (retenue) : le
  build échoue tôt et clairement sans SDK, la pureté du cœur est vérifiée
  par du code qui tourne à chaque `check`.

## Compromis

- Un contributeur sans SDK Android ne peut plus rien construire du dépôt —
  y compris les modules purs. C'est le prix payé pour qu'aucune publication
  ne soit tronquée en silence ; la CI reste le chemin sans installation
  locale.
- `CELERIS_SKIP_ANDROID` disparaît : tout script qui l'utilise doit être
  mis à jour (les mentions documentées sont retirées dans le même commit).

## Conséquences

- Le critère M1 se reformule : « la pureté du cœur est garantie par les
  tests ArchUnit de chaque module pur, vérifiés à chaque `check` » — et non
  plus par l'absence des modules Android du graphe.
- JitPack construit les onze modules publiés à chaque étiquette ; une
  absence de SDK y échoue bruyamment au lieu de publier huit artefacts.
- L'étape CI « modules purs » devient « modules purs (SDK installé) » : le
  même travail de qualité sur les modules purs, sans la variable de refus.

## Actions

- [x] `settings.gradle.kts` : inclusion inconditionnelle, suppression de la
      détection et de `CELERIS_SKIP_ANDROID`.
- [x] `ci.yml` : suppression de `CELERIS_SKIP_ANDROID`, tous les travaux
      avec SDK.
- [x] Consigner les dix tests ArchUnit existants comme garant de la
      pureté du cœur (cette ADR).
- [x] AGENT.md, USAGE.md § 19.9, README : la mention « sans SDK, les
      modules Android ne sont pas inclus » devient « tout build exige un
      SDK Android ».
