# ADR-004 — Analyse syntaxique incrémentale

- **Statut** : Acceptée (validée par le propriétaire le 2026-11-04)
- **Date** : 2026-11-04

## Contexte

Le dépôt de départ n'a **aucun arbre syntaxique** : l'audit mesure dix-neuf
tokéniseurs écrits à la main en production, la seule intelligence Java vivant
dans la démo (22 fichiers, 9 301 lignes), et toute la structure (plis,
crochets, indentation, plan) recalculée par des voies séparées. Le mandat
impose un arbre concret, sans perte, tolérant aux erreurs, incrémental,
calculé hors fil UI et annulable, et demande un spike chiffré avant de
trancher entre trois familles de solutions.

Résultats du spike (`docs/audit/SPIKES.md`, JVM de bureau, 50 000 lignes) :

| Mesure | Valeur |
| --- | --- |
| Lexicographie complète | 8,05 ms (min de 3 tours) |
| Re-lexicographie incrémentale après 1 caractère, p50 / p95 | 1,6 µs / 4,5 µs |
| Lignes re-lexicographiées en moyenne | 1,0 |
| Budget de ré-analyse du mandat | 10 ms hors fil UI |

## Décision proposée

**Analyseurs incrémentaux écrits ici (option A) pour les langages P0**
(Java, Kotlin, XML, JSON, Gradle KTS), **voie de grammaires (format
TextMate) pour la longue traîne**, et conservation d'un comparatif chiffré
pour réviser la décision :

- Un arbre par langage P0, produit par un analyseur récursif descendant
  écrit en Java, tolérant aux erreurs (nœuds « erreur » plutôt qu'échec),
  incrémental par repli sur les bornes d'état (le spike mesure l'approche à
  l'échelle de la lexicographie ; l'analyse complète coûtera quelques fois
  ce plancher, d'où l'incrémental par construction).
- Un **socle commun** : jetons, états transportés par ligne, arbre de nœuds
  typés, plages sources, crochets appariés — les dix-neuf tokéniseurs du
  dépôt de départ deviennent des consommateurs de l'arbre ou disparaissent.
- La coloration, les plis, les crochets, l'indentation, la sélection
  structurelle, le fil d'Ariane et le plan **lisent le même arbre**.
- Les grammaires au format TextMate (module `core-syntax-grammars`, voie
  héritée du dépôt de départ) couvrent les langages P2 sans analyseur
  dédié : coloration et plis seulement, pas d'arbre.

## Options

| Option | Avantages | Inconvénients | Verdict |
| --- | --- | --- | --- |
| A. Analyseurs Java écrits ici, incrémentaux | cœur sans dépendance native ; testables JVM ; contrôle total de la tolérance aux erreurs ; budgets mesurés | effort par langage (2 à 4 semaines pièce) | **retenue** |
| B. Moteur d'analyse incrémental par grammaires, écrit en C, intégré par liaison native [EXT-4] | des dizaines de grammaires existantes ; incrémental éprouvé | charge native par ABI (arm64, x86_64…) ; taille d'artefact ; JNI dans le chemin chaud de rendu ; grammaires tierces à auditer ; complexité de débogage sur appareil | écartée à M0, révisable par ADR si les budgets d'effort dérapent |
| C. Générateur d'analyseurs non incrémental | grammaire déclarative | re-analyse complète à chaque frappe : le spike mesure déjà 8 ms pour la seule lexicographie, l'analyse complète dépasserait le budget | écartée |

## Compromis

- L'option A accepte un effort récurrent par langage P0/P1 contre la
  maîtrise du chemin chaud, de la tolérance aux erreurs et de la licence
  (zéro grammaire tierce critique).
- Les cascades d'invalidation existent (l'édition de l'ouverture d'un
  commentaire de bloc invalide toutes les lignes suivantes) : elles doivent
  être bornées et testées (constat du spike : maximum isolé à 33 ms sur le
  premier tour — un piège à inscrire au registre, voir PIEGES).

## Conséquences

- `core-syntax` expose un arbre versionné par instantané de document :
  l'analyse est annulable et rejouable sur éditions distantes (agents).
- Le budget de ré-analyse (10 ms) devient un seuil de benchmark en CI à
  partir de M4, sur corpus embarqués de 50 000 lignes par langage.
- L'effort de M4 est le plus gros de la feuille de route (voir
  `docs/feuille-de-route.md`) ; la voie de grammaires sécurise la longue
  traîne pendant que les analyseurs P0 arrivent.

## Actions

- [ ] M4 : socle commun (jetons, états par ligne, arbre, crochets) + Java.
- [ ] M4 : Kotlin, XML, JSON, Gradle KTS ; corpus et budgets en CI.
- [ ] M4 : écrire les tests de cascade de commentaire de bloc avant l'implémentation.
- [ ] M4 : prototype de comparaison option B, chiffré, si l'effort P0 dépasse
      le plan de charge — décision de révision documentée ici même.
