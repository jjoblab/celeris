# ADR-015 — Client LSP sur LSP4J derrière une frontière

- **Statut** : Proposition (spike exécuté le 2026-11-08 — les chiffres sont mesurés, la validation reste au propriétaire)
- **Date** : 2026-11-08
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
`lsp-lsp4j`.**

1. `core-lsp` garde l'API du domaine en Java pur sans dépendance — types
   immuables, `OrdonnanceurLsp` (priorités, annulation, fusion),
   transposition UTF-16 (ADR-003). Aucun type LSP4J n'y paraît.
2. Un module **`lsp-lsp4j`**, seul dépendant de `org.eclipse.lsp4j` et
   de Gson, porte `Lsp4jSession`, la correspondance des types, la
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

## Options

| Option | Verdict | Raison mesurée |
| --- | --- | --- |
| A. Client maison étendu (statu quo ADR-005) | écartée | 3 442 lignes pour 17 méthodes ; chaque méthode de plus coûte types et codecs ; la panne réelle du lecteur de documentation (M8.6) est exactement la classe de pièges que la bibliothèque élimine (`Either<String, MarkupContent>`) |
| B. LSP4J 1.0.0 derrière `lsp-lsp4j` | **retenue** | compatibilité prouvée contre le serveur réel embarqué ; arbre de dépendances mince (gson seul) ; 728 Ko / 1 636 méthodes ; la copie unique avec le serveur tient (Gson 2.10.1 dans l'intervalle exigé) |
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

## Ce qui reste à prouver à l'intégration (avant toute suppression de code)

1. **R8** : règles consommateur du module (Gson par réflexion casse en
   silence après réduction) et preuve sur la **démo réduite**
   (`assembleRelease` : poignée de main, complétion, diagnostic, édition
   d'espace rejoués sur ce binaire).
2. **Appareil** : comportement au niveau d'API 26 et au niveau courant
   (le bac n'a pas d'appareil ; plan dans VERIFICATION-APPAREIL).
3. La découpe modifiée du serveur (exclusion lsp4j/Gson) reproduite au
   octet près et les sept tests repassés sur l'archive ainsi découpée.

## Actions

- [ ] M7.6 (étape 7 du jalon) : module `lsp-lsp4j`, parité sur les sept
      tests, suppression du code maison après parité.
- [ ] M7.6 : découpe du serveur sans lsp4j/Gson, copie unique dans
      l'APK.
- [ ] M7.6 : règles R8 consommateur + démo réduite ; catalogue, SBOM,
      NOTICE, empreintes (règle 5).
