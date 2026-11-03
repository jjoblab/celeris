# ADR-001 — Langage et chaîne d'outils

- **Statut** : Acceptée (validée par le propriétaire le 2026-11-04)
- **Date** : 2026-11-04

## Contexte

Le dépôt de départ est écrit en Java (aucun Kotlin), rendu Canvas, et impose
déjà le niveau de langage Java 17 avec un JDK 17+. L'audit
(`docs/audit/ETAT-DES-LIEUX.md`) confirme une base saine sur ce point : le
choix du Java facilite le portage des algorithmes validés (rope, annulation,
recherche) et maintient le cœur consommable sans exécuter de runtime tiers.
Le mandat impose en parallèle les dernières versions stables d'AGP, Gradle,
`compileSdk` et `targetSdk`, à vérifier en ligne à la date d'exécution.

Vérifications faites le 2026-11-04 (sources : notes de version officielles de
la plateforme et du système de build, agrégateurs de distribution) :

| Élément | Dernière stable vérifiée | Source |
| --- | --- | --- |
| Plugin Gradle Android (AGP) | 9.5.0 (octobre 2026) | notes de version officielles |
| Gradle | 9.8.2 (octobre 2026) | site officiel des versions |
| `compileSdk` | 36 (Android 16 stable ; le niveau 37 est en bêta) | documentation plateforme |
| `targetSdk` | 36 — imposé par le magasin pour les mises à jour depuis août 2026 | politique de publication |
| JDK | 21 LTS utilisé pour construire ; artefacts de niveau 17 | — |

## Décision proposée

1. **Java uniquement** pour tout code versionné (bibliothèques, démo, cli,
   tests), niveau de langage **17** pour les artefacts publiés ; construction
   et CI sur JDK 21 LTS. Le niveau 17 maximise la compatibilité des
   consommateurs ; l'absence de Kotlin tient le cœur exempt de runtime tiers.
2. **Groovy est écarté** ; les scripts de build utilisent le DSL Kotlin, à
   raison : le DSL Kotlin reste confiné aux fichiers de build et à
   `build-logic` — **aucun Kotlin ne fuit dans les artefacts publiés**, dont
   le bytecode reste 100 % produit par `javac`. (Question soumise au
   propriétaire : voir le résumé M0 — une préférence stricte « zéro Kotlin
   dans le dépôt » imposerait le DSL Groovy au prix d'un outillage de
   convention moins robuste.)
3. Outils figés par le catalogue de versions unique : AGP 9.5.0, Gradle
   9.8.2 (wrapper validé par somme de contrôle), `compileSdk` 36,
   `targetSdk` 36, `minSdk` 26 (justifié dans l'ADR-011 : portée cumulée
   mesurée ≈ 99 % des appareils actifs).
4. Le rendu reste **Canvas pur** : ni bibliothèque de composition d'interface
   ni un afficheur de texte par ligne — exigence du mandat, confirmée par
   l'audit du dépôt de départ comme socle de performance viable.

## Options

| Option | Avantages | Inconvénients | Verdict |
| --- | --- | --- | --- |
| A. Java 17, DSL Kotlin pour le build seul | portage direct ; artefacts purs ; outillage de convention éprouvé | deux langages dans le dépôt (build seul) | **retenue** |
| B. Java 17, DSL Groovy partout | un seul écosystème « javac/Groovy » | plugins de convention Groovy fragiles, typage faible, erreurs au moment de l'exécution | écartée |
| C. Kotlin pour le code | concision | contredit le mandat ; runtime tiers dans le cœur ; portage complet à réécrire | écartée |

## Compromis

- Maintenir deux grammaires de build (Kotlin DSL pour décrire du Java) est un
  coût d'apprentissage mineur, payé une fois, contre la sûreté de typage des
  plugins de convention.
- Le niveau 17 renonce aux API JDK 21 dans le code publié ; les gains
  éventuels (séquences virtualisées, etc.) sont différés jusqu'à ce qu'un ADR
  relève le plancher.

## Conséquences

- Le compilateur refuse tout identifiant non ASCII (politique de langue,
  vérifiée en CI) et compile avec `-Xlint:all -Werror`.
- Toute montée de version d'AGP/Gradle passe par le catalogue et une revue
  de contrat (tests de build), jamais par une version en dur.

## Actions

- [x] M1 : créer `build-logic` avec les plugins de convention (ADR-011).
- [x] M1 : épingler le wrapper Gradle avec sa somme de contrôle (9.8.2, vérifiée).
- [x] M1 : décider avec le propriétaire DSL Kotlin contre Groovy —
      décision du 2026-11-04 : DSL Kotlin confiné au build (question 1 du
      résumé M0).
