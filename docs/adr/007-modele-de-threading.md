# ADR-007 — Modèle d'exécution et concurrence

- **Statut** : Acceptée (validée par le propriétaire le 2026-11-04)
- **Date** : 2026-11-04

## Contexte

L'audit du dépôt de départ mesure la concurrence dispersée : `volatile`
dans 47 fichiers, `AtomicXxx` dans 37, exécuteurs créés dans 32 — sans
modèle unique documenté (constat n° 10). Dans le même temps, le principe
d'architecture n° 11 impose l'injectabilité de l'horloge et des exécuteurs,
et l'expérience du dépôt de départ montre des benchmarks trompés par le
bruit du runner (PIEGE-13) et des tests de vue contraints par réflexion
(constat : injection de métriques par réflexion, à bannir).

## Décision proposée

1. **Écrivain unique** : toute mutation d'état partagé a lieu sur le fil UI
   (ou le fil d'écriture dédié du cli). Pas de verrou interne : la
   synchronisation est architecturale, pas locale.
2. **Lecteurs sur instantanés** : les travaux d'arrière-plan (analyse,
   index, recherche, pré-calcul de rendu) reçoivent un instantané immuable
   et rendent un résultat daté par instantané ; les résultats périmés sont
   jetés, jamais fusionnés à la hâte.
3. **Exécuteurs et horloge injectés** : aucune création d'exécuteur dans le
   code de production ; le conteneur d'exécution est fourni par l'hôte
   (vue Android, cli, tests). Les tests obtiennent ainsi un temps
   déterministe — aucun `sleep` nulle part.
4. **Jetons d'annulation partout** : toute opération d'arrière-plan longue
   (analyse, requête LSP, prédiction IA) accepte un jeton ; annuler coûte
   moins cher que terminer. Les jetons propagent l'annulation aux couches
   basses (transport compris).
5. **Fil documenté par classe** : annotation `@UiThread` / `@WorkerThread`
   sur chaque classe d'API, reprise dans la Javadoc française ; la règle est
   vérifiée en revue et par un test d'architecture (le nom de l'annotation
   est qualifié pour éviter la confusion avec l'annotation du SDK Android,
   que le cœur ignore).
6. **Pas de réflexion dans les tests** : les points d'injection sont des
   interfaces (`FontMetrics`, `Clock`, `FileSystem`, executors) ; le
   dépôt de départ a mesuré le coût contraire (PIEGE-05).

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| A. Écrivain unique + instantanés (proposition) | **retenue** | suppression des verrous internes ; testabilité déterministe ; aligne le modèle sur l'immuabilité d'ADR-003 |
| B. Verrous fins par structure | écartée | l'audit mesure 146 `volatile` et 158 `AtomicXxx` dispersés : la preuve par l'échec |
| C. File d'événements unique type acteur | écartée | detours inutiles sur le fil UI d'Android ; mêmes invariants plus difficiles à exprimer |

## Compromis

- L'écrivain unique fait du fil UI un goulot pour les grosses transactions :
   les transactions massives (ouverture, ré-indentation de fichier) sont
   découpées en tranches budgétées par image, comme le prévoit l'ouverture
   progressive (matrice G-01).
- L'annulation généralisée verbe chaque signature ; c'est le prix du
  budget de latence IA (ADR-012) et de la resynchronisation LSP (ADR-005).

## Conséquences

- Les fichiers du cœur ne déclarent ni `volatile`, ni `AtomicXxx`, ni
  exécuteur : un test d'architecture le vérifie (règle greppable).
- Un benchmark qui mesure sans exécuteur injecté et sans minimum de trois
  tours ne passe pas la revue (PIEGE-13).
- `System.currentTimeMillis` et `nanoTime` n'apparaissent que dans
  l'outillage de mesure, jamais dans le code de production.

## Actions

- [ ] M1 : définir les annotations de fil et le test d'architecture associé.
- [x] M2 : horloge et exécuteurs injectés dans `core-text` (première
      consommatrice : l'annulation avec coalescence — `UndoStack`,
      2026-11-04).
