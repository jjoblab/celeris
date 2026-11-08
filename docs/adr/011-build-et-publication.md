# ADR-011 — Build, publication et sécurité des dépendances

- **Statut** : Acceptée (validée par le propriétaire le 2026-11-04)
- **Date** : 2026-11-04

## Contexte

L'audit du dépôt de départ mesure la dette de build : sept
`build.gradle.kts` dont six blocs de publication dupliqués, `compileSdk` 34
épinglé (des dépendances exigent 35+), deux archives binaires versionnées
(moteur d'expressions rationnelles du module de grammaires, intégrées faute
de propagation d'un miroir), 48 entrées de baseline lint figées, et une
documentation qui nomme des projets externes en plus de cent reprises malgré
sa propre règle — le contrôle n'y était visiblement pas appliqué. Le mandat
impose : plugins de convention, catalogue unique, publication définie une
fois, `-Werror`, lint bloquant sans baseline, zéro binaire versionné, SBOM,
et les versions stables vérifiées en ligne à la date d'exécution (fait :
voir ADR-001 — AGP 9.5.0, Gradle 9.8.2, `compileSdk`/`targetSdk` 36).

## Décision proposée

1. **`build-logic` à plugins de convention** : publication, lint, options
   `javac` (`-Xlint:all -Werror`), couverture, mutation et validateur
   d'API déclarés **une seule fois** ; les modules ne déclarent que leur
   nom, leur nature et leurs dépendances. Cache de configuration activé.
2. **Catalogue de versions unique** (`gradle/libs.versions.toml`) — acquis
   du dépôt de départ, conservé tel quel avec sa discipline : toute version
   en dur dans un module casse la CI.
3. **`minSdk` 26** : portée cumulée mesurée ≈ 99 % des appareils actifs
   (agrégateurs de distribution, données d'octobre 2026) ; le dépôt de
   départ occupait 24. Le gain de 24 à 26 simplifie l'Unicode natif et la
   gestion mémoire ; l'écart restant (~1 %) est le prix annoncé.
   `targetSdk`/`compileSdk` 36 (exigence du magasin depuis août 2026).
4. **Publication** : artefacts Maven avec sources, Javadoc et POM complets,
   sommes de contrôle et SBOM générés à chaque publication ; cible
   proposée : le registre de paquets de l'hébergeur du dépôt (authentifié,
   privé en pré-1.0) — décision de cible confirmée par le propriétaire
   le 2026-11-04 (registre de l'hébergeur en pré-1.0) ; le registre public central reste possible
   après validation de la propriété du domaine de l'espace de noms.
5. **Zéro archive binaire versionnée** (fin de `libs/*.jar`) : les
   dépendances vivent par coordonnées avec somme de contrôle vérifiée ;
   le moteur d'expressions rationnelles du module de grammaires redevient
   une dépendance normale (le miroir incriminé a depuis propagé).
6. **Lint bloquant, zéro baseline** dès le premier commit : la baseline
   mesurée à 48 entrées dans le dépôt de départ est le contre-exemple
   exact (les entrées « version plus récente disponible » y masquaient le
   retard des versions).
7. **CI** : actions épinglées par SHA (acquis du dépôt de départ, conservé),
   validation du wrapper par somme de contrôle, exécution des scripts
   `check-language-policy`, `check-forbidden-refs` (M7.6, ex
   `check-external-refs`) et `check-dependency-provenance` (M7.6) sur
   **tous** les fichiers versionnés — y compris documentation et
   changelog, là où le dépôt de départ a dérivé.
8. **Sécurité** : aucun accès réseau par défaut dans les modules ; les
   serveurs de langage tournent en processus isolés ; validation des
   chemins contre la traversée de répertoires pour les éditions d'espace
   de travail ; revue de sécurité avant 1.0 (M9).

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| A. Convention + catalogue + registre de l'hébergeur (proposition) | **retenue** | publication privée immédiate, zéro infrastructure tierce à valider ; migration centrale possible ensuite |
| B. Publication directe au registre central public | différée | exige la validation du domaine de l'espace de noms `jo.celeris` par le propriétaire ; inutilement bloquante avant 1.0 |
| C. Publication locale (`mavenLocal`) | écartée | satisfaisante pour la démo, invisible pour les consommateurs réels |

## Compromis

- Le registre de l'hébergeur lie la consommation du dépôt à un compte :
   acceptable en pré-1.0 ; la 1.0 publique tranche la cible définitive.
- `minSdk` 26 abandonne ~1 % d'appareils anciens : mesuré, affiché,
  réversible par ADR si le propriétaire juge le coût inacceptable.

## Conséquences

- La CI refuse : version en dur, archive binaire versionnée, entrée de
  baseline lint, action non épinglée par SHA, dépendance sans licence
  consignée dans `NOTICE`.
- Les chiffres de build (nombre de modules, seuils, versions) sont générés
  dans la documentation d'avancement, jamais saisis.
- Les tests de contrat de montée de version (AGP, Gradle, dépendances)
  accompagnent toute montée — la « gelée de versions » du dépôt de départ
  est un anti-pattern à bannir (mandat, section 10).

## Actions

- [x] M1 : `build-logic` + catalogue + publication (vérifiée localement ; cible le
      registre de l'hébergeur, identifiants par environnement).
- [x] M1 : workflows CI épinglés par SHA, wrapper validé, scripts de
      politique branchés et bloquants.
- [x] M1 : génération SBOM et sommes de contrôle à la publication.
