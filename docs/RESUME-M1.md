# Résumé M1 — fondations de celeris

> **Décisions appliquées au nom du propriétaire et signalées en tête,
> conformément au mandat** (validation M0 du 2026-11-04 : « c'est oui
> pour tes recommandations ») :
>
> 1. **DSL de build** — DSL Kotlin confiné à `build-logic` et aux
>    fichiers de build ; aucun Kotlin dans les artefacts publiés.
> 2. **Cible de publication** — registre de l'hébergeur du dépôt, privé
>    en pré-1.0 ; registre central public réexaminé après validation du
>    domaine.
> 3. **Analyse syntaxique** — option A (analyseurs écrits ici) ; l'option
>    B reste chiffrée en réserve.
> 4. **Appareils de test** — appareil de référence local à M1 ; décision
>    d'infrastructure à revoir à M3.
> 5. **Appareil de référence des budgets** — **non désigné** : point
>    ouvert (ADR-006), la calibration ne peut pas commencer avant
>    désignation.
> 6. **`minSdk` 26** — confirmé.

## Livré

- **Squelette exécuté des dix modules et de la démonstration** (ADR-002)
  : six modules purs JVM, trois modules Android, la cli (exécutable,
  testée) et la démo (catalogue de scénarios vide, écran de réglages en
  direct persistés).
- **`build-logic` à plugins de convention** : publication, lint, options
  javac (`-Xlint:all -Werror`, JDK 21 en construction, artefacts de
  niveau 17), couverture, mutation et manifeste d'API déclarés une seule
  fois ; catalogue de versions unique, toutes vérifiées en ligne le
  2026-11-04.
- **Portes qualité bloquantes dès le premier commit** : Checkstyle 14.4.0
  (600 lignes par fichier, 50 par méthode), PMD 7.29.0 (complexité
  cyclomatique 15), Error Prone 2.52.0 + NullAway 0.14.3 + JSpecify
  1.0.1 (`@NullMarked` sur tous les paquets), JaCoCo 0.8.16 (couverture
  de lignes ≥ 90 % sur les modules purs — exigence **prouvée bloquante**
  sur la cli avant couverture de son point d'entrée), ArchUnit 1.5.1
  (topologie par module : aucun import Android dans le cœur, paquets
  autorisés encodant les dépendances descendantes), Jupiter 6.1.3 +
  jqwik 1.10.2.
- **Mutation branchée** (ADR-010) : PIT 1.30.1 en ligne de commande,
  seuil 80 % sur `core-text`, cibles limitées au code de production —
  **prouvée bloquante** (50 % mesuré sur un essai, refusée), inapplicable
  sur le squelette vide et déclaré comme tel.
- **Manifeste d'API publique** (ADR-009) : généré par module pur
  (`api/<module>.api`, sortie normalisée de javap) et **vérifié dans
  `check`** — toute divergence d'API casse la construction.
- **Publication vérifiée localement** (ADR-011) : artefacts Maven
  complets (jar, sources, Javadoc, POM, module Gradle), SBOM CycloneDX
  et sommes de contrôle SHA-256 de chaque artefact ; cible le registre
  de l'hébergeur (identifiants par environnement, jamais committés).
- **Scripts de politique bloquants** : langue (identifiants ASCII,
  typographie française, Javadoc des classes publiques) et neutralité
  (références externes) exécutés dans `check`.
- **Workflows CI** (ADR-011) : actions épinglées par somme SHA
  (checkout v7.0.1, setup-java v6.1.0, setup-gradle v6.4.1 avec
  validation du wrapper), deux travaux — modules purs **sans SDK
  Android** (critère de sortie M1) et ensemble avec SDK (modules
  Android, démonstration, APK).
- **`AGENT.md`** (contrats d'agent : commandes vérifiées, conventions,
  portes, définition de « terminé ») et **`NOTICE`** consignant
  l'outillage et ses licences.

## Vérifié localement (sans SDK Android)

- `./gradlew check` vert en deux passes, cache de configuration réutilisé
  (ADR-011) ;
- 17 tests verts, 0 échec (dont 7 règles d'architecture et 1 propriété
  jqwik) ;
- `mutationThreshold`, `publishToMavenLocal`, `publicationChecksums`,
  `:cli:run` verts.

## Écarts et limites

1. **Modules Android et démonstration non compilés localement** : aucun
   SDK Android dans l'environnement d'exécution ; leur compilation
   relève de la CI (composants installés par le workflow). Le premier
   passage CI peut exiger un ajustement de composants (notamment la
   version de build-tools qu'AGP 9.5.0 réclame).
2. **Appareil de référence non désigné** (question 5) : les budgets de
   l'ADR-006 ne sont pas calibrés ; c'est le seul critère de sortie M1
   hors de portée sans décision du propriétaire.
3. **Mutation** : aucune ligne de production à muter — le seuil ne se
   prononce pas sur le vide ; il s'applique dès la première ligne (M2).
4. **Publication vers le registre de l'hébergeur configurée mais non
   exercée** : le premier envoi réel accompagnera la première version
   publiée (identifiants requis).
5. **Manifestes d'API et publication des modules Android** : différés à
   l'arrivée du code correspondant (M2/M3), les modules ne contenant
   encore que leur package-info.
6. **Javadoc des modules squelettes** : vide (aucune classe publique à
   documenter — javadoc le signale) ; bloquante dès la première classe.
7. **Secret `CELERIS_FORBIDDEN_REFS`** : à créer sur le dépôt hébergé
   avec les motifs nominatifs — action du propriétaire ; en son absence
   la CI n'applique que les heuristiques génériques (avertissement
   visible).

## Passage de porte

Répondez « M1 validé » (éventuellement en désignant l'appareil de
référence des budgets, question ouverte n° 5) pour déclencher
M2 — Moteur de texte : portage rope + index + annulation, pièges
PIEGE-07 et PIEGE-08 écrits **avant** le code, fuzz jqwik à oracle
`StringBuilder`, gel de l'API de `core-text` en 0.1.0.
