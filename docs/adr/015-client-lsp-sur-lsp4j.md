# ADR-015 — Client LSP sur LSP4J derrière une frontière

- **Statut** : Acceptée (spike exécuté le 2026-11-08 ; validée par le propriétaire le 2026-11-09 — module séparé confirmé, nommage neutre gravé)
- **Date** : 2026-11-08 (révisée le 2026-11-09)
- **Remplace** : la décision 1 et le tableau d'options de l'ADR-005 (qui reçoit sa note de révision)

## Contexte

Le prompt M7.6 impose d'adopter LSP4J **derrière une frontière** : le
protocole et le JSON-RPC viennent de la bibliothèque, l'ordonnancement,
les priorités et les types du domaine restent chez nous. Le client
maison couvre 17 méthodes en 3 442 lignes ; un éditeur ambitieux en
demande cinquante ou plus. L'argument historique du refus (ADR-005 : la
rupture 0.22.0 → 1.0.0 sur les édits de gabarit) décrit un mauvais
usage — une liaison sans couche d'isolation — et la 1.0.0 modélise
désormais ces édits (`SnippetTextEdit`). Depuis M8.6, l'APK de
démonstration embarque déjà LSP4J (0.23.1) et Gson 2.10.1 par le
serveur tiers retiré (serveur-retire 0.31.2) : la question de la copie unique se
pose dès ce jalon.

Faits vérifiés en ligne le 2026-11-08 (dépôt, POM, releases) et par le
spike `spikes/lsp4j/` (résultats bruts dans `spikes/lsp4j/results/`) :

| Point | Constat |
| --- | --- |
| Version cible | 1.0.0 (10 février 2026, LSP 3.18 **non finalisée**) ; repli : 0.24.0 (LSP 3.17) |
| Licence | `EPL-2.0 OR BSD-3-Clause` (consommable sous BSD-3-Clause, compatible Apache-2.0) ; Gson Apache-2.0 |
| Arbre des dépendances (POM) | `org.eclipse.lsp4j:1.0.0` → `org.eclipse.lsp4j.jsonrpc:1.0.0` → `gson [2.9.1, 3.0)` — **ni guava, ni autre** ; Gson 2.10.1 (la version déjà embarquée par le serveur) est dans l'intervalle : **une seule copie suffit** |
| Version exigée par le serveur embarqué | serveur-retire 0.31.2 est construit contre LSP4J **0.23.1** (`serveur-retire-parent-0.31.2.pom`) |
| Compatibilité à l'exécution | **prouvée par le spike** : le harnais lance serveur-retire 0.31.2 par `LSPLauncher` **de la 1.0.0** (classpath 1.0.0 devant l'archive découpée) et rejoue poignée de main, ouverture, diagnostics poussés, complétion documentée (1 item, documenté), survol de DTD et extinction — tout passe, identiquement sous 1.0.0, 0.24.0 et 0.23.1 |
| Premier message (à froid, JVM de bureau) | initialize : 123–143 ms (la réflexion Gson s'y paie) ; complétion ~60 ms ; survol 5–25 ms ; total du fil ~600 ms |
| Android statique (d8 + dexdump, lsp4j + jsonrpc + gson) | **728 Ko de dex, 1 636 méthodes définies, zéro référence** à sun.misc.Unsafe ou jdk.internal |
| Ruptures connues de la 1.0.0 | API obsolète supprimée (dont le paquet websocket) ; `TextDocumentEdit.edits` en `Either<TextEdit, SnippetTextEdit>` ; `Diagnostic.message` en `Either<String, MarkupContent>` |

## Décision

**LSP4J 1.0.0, épinglée au catalogue, derrière la frontière
`lsp-protocole`.**

1. `core-lsp` garde l'API du domaine en Java pur sans dépendance — types
   immuables, `OrdonnanceurLsp` (priorités, annulation, fusion),
   transposition UTF-16 (ADR-003). Aucun type LSP4J n'y paraît.
2. Un module **`lsp-protocole`** (l'artefact dit sa fonction, jamais sa marque — style maison de `core-syntax-grammars`), seul dépendant de `org.eclipse.lsp4j` et
   de Gson, porte `LspSession`, la correspondance des types, la
   négociation de capacités (enregistrement dynamique compris) et les
   requêtes serveur→client (`workspace/applyEdit`, `registerCapability`,
   `workDoneProgress`, `showMessage`, `workspace/configuration`) — le
   client minimal du spike les couvre déjà par les méthodes par défaut.
3. **Copie unique** : la découpe du serveur tiers exclut désormais
   `org/eclipse/lsp4j/` et `com/google/gson/` de l'archive embarquée ;
   le catalogue fournit la 1.0.0 pour les deux bouts — la compatibilité
   serveur-retire 0.31.2 × LSP4J 1.0.0 est prouvée ci-dessus sur JVM.
4. **Transport** : écrit ici sur des flux (prise TCP, tube à file pour
   la démo et les tests, entrée-sortie standard pour le cli sur JVM) —
   le paquet websocket de la bibliothèque a disparu en 1.0.0 et aucune
   connexion sortante ne part sans consentement (F-10, vérifié en CI).
5. **`exit()` n'est jamais appelé** : l'implémentation du serveur y
   éteint la machine (constat M8.6) ; `shutdown()` puis fermeture des
   transports, le spike le rejoue.
6. La montée de version de LSP4J devient un changement de la couche de
   correspondance et des tests de contrat, pas une migration (la
   frontière est là pour cela).

## Révision du 2026-11-09 (validation du propriétaire)

- **Le module séparé est confirmé.** La piste du mono-module (`core-lsp`
  portant lui-même la liaison) a été examinée et tranchée : elle
  convertirait la frontière mécanique (chemin de classes Gradle + ArchUnit)
  en discipline de relecture — précisément ce que l'audit fondateur
  (ADR-002) a condamné. La liste blanche d'ArchUnit de `core-lsp` reste
  donc ce qu'elle est ; preuve, pas promesse.
- **Nom d'artefact : `lsp-protocole`.** Le prompt orthographiait
  « `lsp-lsp4j` » (ligne H-11 de la matrice) ; l'amendement est
  consigné ici et pris en responsabilité par le propriétaire ; la
  matrice et les ADR concernés sont corrigés dans le même commit.
- **Règle de nommage gravée (directive du propriétaire, « très important »)** :
  aucun nom de bibliothèque tierce dans un identifiant — classe, méthode,
  paquet. Le module porte donc `LspSession`, `CorrespondanceLsp`, paquet
  `jo.celeris.lsp.protocole`. LSP4J et Gson se nomment dans la
  documentation, le NOTICE et le SBOM (règle 5), jamais dans le code.
  La règle vaut pour tout le dépôt et vit dans `AGENT.md`.
  Le retournement du suffixe — `LspSession`, jamais `SessionLsp` —
  obéit au rempart de langue (M7.5) : « Session » y est un radical
  français figé, les nouveaux identifiants sont anglais et le décompte
  ne peut que descendre.
- **L'éditeur prime sur la démo (directive du propriétaire).** Le module
  sert d'abord la pile de l'éditeur — cli, hôte Android, vue ; la
  démonstration n'est que le banc de validation de l'éditeur, jamais sa
  finalité. La parité des sept tests reste le critère d'entrée, mais
  l'ordonnancement (priorités, annulation, fusion) et la transposition
  UTF-16 de `core-lsp` continuent de servir l'éditeur d'abord.

## Options

| Option | Verdict | Raison mesurée |
| --- | --- | --- |
| A. Client maison étendu (statu quo ADR-005) | écartée | 3 442 lignes pour 17 méthodes ; chaque méthode de plus coûte types et codecs ; la panne réelle du lecteur de documentation (M8.6) est exactement la classe de pièges que la bibliothèque élimine (`Either<String, MarkupContent>`) |
| B. LSP4J 1.0.0 derrière `lsp-protocole` | **retenue** | compatibilité prouvée contre le serveur réel embarqué ; arbre de dépendances mince (gson seul) ; 728 Ko / 1 636 méthodes ; la copie unique avec le serveur tient (Gson 2.10.1 dans l'intervalle exigé) |
| C. LSP4J 0.24.0 (LSP 3.17 finalisée) | repli documenté | si un blocage apparaît à l'intégration (R8, appareil) ; le spike la montre aussi compatible avec le serveur |

## Compromis

- LSP 3.18 n'est pas finalisée : la version est épinglée, la frontière
  borne le coût d'un éventuel retour à 3.17 (une couche, des tests).
- La 1.0.0 sert serveur-retire 0.31.2 au-delà de sa version de construction
  (0.23.1) : le spike prouve le fil exercé ; tout chemin non exercé du
  serveur reste sous garde des sept tests de `ServeurRetireTest`,
  qui doivent passer **sans modification** sur la voie LSP4J avant
  toute suppression du code maison.
- 1 636 méthodes et 728 Ko de plus dans l'APK : mesurés, consignés,
  assumés contre 3 442 lignes à écrire et maintenir.

## Preuves d'intégration (exigées avant la suppression du code)

1. **R8 — fait le 2026-11-10.** Les règles de conservation vivent en
   deux fichiers : `lsp-protocole/regles-conservation.pro` (règles
   CONSOMMATEUR du module — le rouage JSON réflexif, les proxys du
   cadre, les TypeToken) et `demo/regles-r8.pro` (le serveur embarqué
   monté par désignation et son archive découpée au octet près — ce
   qu'elle porte se garde entier). `assembleRelease` est vert :
   6,3 Mo et 6 383 classes contre 10,3 Mo et 14 005 au débogage
   (−39 % / −54 %) ; le dex vérifié porte la bibliothèque (568
   classes) et Gson (217, la copie unique) à leurs noms d'origine,
   les 671 classes du serveur entières, les services ServiceLoader
   rendus. Le rejeu des scénarios sur le binaire réduit (R8 en mode
   classfile, mêmes règles, hôte de bureau — le bac n'a pas
   d'appareil) passe la poignée de main, la complétion documentée,
   les diagnostics de validation et l'édition d'espace — cinq
   scénarios, le témoin non réduit à l'identique. La réduction y a
   révélé un défaut latent : le lexeur de grammaires journalisait
   par l'API System.Logger, absente de la plateforme avant le
   niveau 33 — corrigé vers l'API de journalisation du JDK, présente
   des deux côtés depuis toujours.
2. **Appareil** : comportement au niveau d'API 26 et au niveau courant
   (le bac n'a pas d'appareil ; plan dans VERIFICATION-APPAREIL).
3. **Fait le 2026-11-10** : la découpe modifiée du serveur (exclusion
   lsp4j/Gson) rejouée depuis l'ubérifié d'origine (somme vérifiée)
   reproduit le paquet embarqué au octet près (`f03644…`), et les sept
   tests repassent sur l'archive ainsi découpée — **la parité est
   prononcée** (7/7 verts, banc non modifié).

## Actions

- [x] M7.6 (étape 7 du jalon) : module `lsp-protocole` monté, parité
      sur les sept tests prononcée le 2026-11-10
      (`ServeurRetireTest` 7/7 verts, banc non modifié, serveur
      XML réel sur tubes mémoire, archive découpée copie unique).
- [x] M7.6 : suppression du code maison (décodages JSON, cadrage RPC,
      `ClientLsp`, transports maison) — faite le 2026-11-10 : la parité
      prononcée, le corpus de traces migre derrière la frontière (le
      Gson de la bibliothèque lit les quatre traces sans alternative
      ambiguë ; la définition se lit en `List<Location>` posée en
      `Either.forLeft`, l'`Either` de deux listes étant ambigu sans le
      résolveur de requêtes), la couverture du domaine se reconstruit
      par construction directe, `check` reste vert.
- [x] M7.6 : découpe du serveur sans lsp4j/Gson, copie unique tenue
      (reproduction au octet près vérifiée le 2026-11-10, empreinte au
      NOTICE).
- [x] M7.6 : règles R8 consommateur + démo réduite (le 2026-11-10,
      voir les preuves d'intégration) ; le SBOM CycloneDX de
      publication et les empreintes suivent la porte de publication
      (ADR-011), la règle 5 tient (catalogue et NOTICE à jour).
