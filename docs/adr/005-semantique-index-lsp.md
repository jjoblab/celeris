# ADR-005 — Sémantique, index et client LSP

- **Statut** : Acceptée (validée par le propriétaire le 2026-11-04)
- **Date** : 2026-11-04

## Contexte

La sémantique du dépôt de départ repose entièrement sur un client LSP
 externe : 8 283 lignes dans `cel-lsp` construites sur une bibliothèque
tierce de liaison LSP LSP4J, restée en version 0.22.0 **par refus
délibéré** documenté dans le catalogue de versions du dépôt de départ : la
version 1.0.0 de cette bibliothèque change le contrat des éditions d'espace
de travail (édits de type gabarit) et n'a pas pu être absorbée. L'audit
relève en outre que la bibliothèque LSP4J est offerte sous licence EPL-2.0
ou BSD-3-Clause — consommable sous BSD-3-Clause donc compatible Apache-2.0,
mais l'épisode 0.22.0→1.0.0 montre une dépendance structurelle au rythme de
publication d'un tiers.

Le mandat impose : un client LSP moderne et asynchrone (annulation,
priorités, fusion des requêtes, resynchronisation) plus un indexeur léger
sur l'appareil pour Java, Kotlin et le XML Android.

## Décision proposée

1. **Client LSP écrit dans `core-lsp`, sans bibliothèque tierce** : JSON-RPC
   et structures de messages dont nous avons besoin, seulement celles-là.
   Motifs mesurables :
   - l'historique 0.22.0 figé prouve qu'une liaison tierce complète coûte
     plus cher à maintenir qu'elle ne protège ;
   - la surface réellement utilisée est bornée (initialisation,
     `textDocument/*`, `workspace/*`, diagnostics, jetons sémantiques,
     éditions d'espace de travail avec édits de gabarit — justement ce que
     le refus 1.0.0 bloquait) ;
   - l'annulation, la priorité et la fusion demandent de toute façon notre
     propre couche d'ordonnancement au-dessus de toute liaison ;
   - zéro dépendance du cœur (principe d'architecture n° 1).
2. **Transport injectable** : tubes stdio, prise réseau locale, appel direct
   in-process (tests). Les serveurs de langage tournent dans des processus
   isolés (sécurité, mémoire) — jamais dans le processus de l'hôte de la vue.
3. **Indexeur léger sur l'appareil** (`core-intelligence`) : index de
   symboles persistant, incrémental, à budget mémoire, alimenté par l'arbre
   syntaxique (ADR-004) — il répond à la navigation hors ligne (« aller à
   un symbole », usages de base) quand aucun serveur LSP n'est disponible.
   Règle des rôles :
   - l'index répond **vite et hors ligne** (symboles du fichier et du
     projet, navigation locale) ;
   - le serveur LSP répond **juste** (complétion typée, refactorings,
     diagnostics au niveau projet) ;
   - jamais de chaînage : la vue ne sait pas qui a répondu.
4. Les jetons sémantiques et les diagnostics sont deux flux **indépendants**
   (PIEGE-18, hérité du dépôt de départ).
5. Les positions LSP (UTF-16) traversent une frontière explicite de
   transposition avec `DocOffset` (ADR-003) — l'audit du dépôt de départ a
   documenté les pièges de bornes.

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| A. Client propre, surface LSP bornée | **retenue** | dépendance zéro ; contrat maîtrisé ; le refus 1.0.0 documenté montre le coût de la voie contraire |
| B. Liaison tierce LSP4J en version 1.0.0 | écartée | re-risque de blocage à la prochaine rupture ; licence consommable mais validation de fichier par fichier à refaire ; l'annulation/priorité reste à écrire nous-mêmes |
| C. Tout par l'indexeur maison, sans LSP | écartée | réécrire la sémantique de Java et Kotlin dépasse le mandat ; les serveurs existants de l'écosystème restent la source de vérité |

## Compromis

- Écrire JSON-RPC et les structures nous coûte quelques semaines à M6 ;
  en échange, la montée de version LSP devient un changement de **nos** tests
  de contrat, pas une migration de dépendance.
- L'indexeur maison ne rivalisera jamais avec un vrai compilateur pour la
  complétion typée : il ne le tente pas (bornage explicite des rôles).

## Conséquences

- `core-lsp` est testable sur JVM pure avec un serveur factice in-process.
- Le protocole d'éditions de gabarit (bloqué chez le dépôt de départ) entre
  dans le périmètre M6 avec ses tests.
- Un test de contrat rejoue les traces enregistrées de vrais serveurs
  (corpus embarqué) à chaque exécution CI.

## Révision du 2026-11-08 (jalon M7.6)

La bibliothèque écartée se nomme enfin : **LSP4J** (EPL-2.0 ou
BSD-3-Clause, consommable sous BSD-3-Clause donc compatible
Apache-2.0). L'argument du refus — la rupture 0.22.0 → 1.0.0 sur les
édits de gabarit — décrit un mauvais usage (une liaison sans couche
d'isolation), pas la bibliothèque : la 1.0.0 de LSP4J modélise
désormais ces édits (`SnippetTextEdit`), et le client maison mesure
déjà 3 442 lignes pour 17 méthodes quand un éditeur ambitieux en
demande cinquante ou plus. Le jalon M7.6 adopte LSP4J **derrière une
frontière** : `core-lsp` garde l'API du domaine en Java pur (types
immuables, ordonnancement, annulation, UTF-16), un module `lsp-protocole`
porte seul la dépendance et la correspondance des types. Le banc de la frontière les exécute ; restent à prouver le comportement sous R8 et sur appareil, objet du spike et de l'ADR du jalon (**ADR-015, spike exécuté le 2026-11-08 : la voie 1.0.0 est prouvée, compatibilité et mesures consignées**) qui remplace la décision 1 et le tableau d'options ci-dessus. Les bancs de la démonstration servent de critère de parité : ils doivent passer sans modification sur la voie LSP4J.

## Actions

- [ ] M6 : spécifier la surface LSP retenue (matrice B) et son test de contrat.
- [ ] M6 : indexeur hors ligne Java + XML (Kotlin suit) avec budget mémoire.
- [ ] M6 : écrire PIEGE-17 et PIEGE-18 avant le client.
