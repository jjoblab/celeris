# Résumé M6 — sémantique (en cours)

> **Déclenchement** : clôture du jalon M5 (2026-11-06, critères de
> sortie couverts — catégorie A P0 et D-08/D-09/D-10, quarante-trois
> scénarios). Périmètre (feuille de route) : client LSP propre
> (annulation, priorités, fusion, resynchronisation ;
> transports stdio/réseau/in-process) ; indexeur hors ligne ;
> complétion, aide aux paramètres, survol, définition, usages,
> renommage, correctifs, formatage, indices en ligne, jetons
> sémantiques, diagnostics ; moteur de refactorisation ; édits de
> gabarit (le blocage historique 1.0.0). PIEGE-16 à PIEGE-18 écrits
> avant. Sortie visée : catégorie B P0 couverte, critères testés sur
> projets Android réels (corpus embarqué).

## Tranche 1 — le socle du client et les pièges d'abord (2026-11-06)

### Livré

- **Le piège écrit avant le code** (règle du registre) : les tests
  PIEGE-17 (liste de complétion tronquée honorée telle quelle —
  jamais réordonnée, jamais complétée localement) et PIEGE-16 (les
  blocs typés de la documentation de complétion restent distincts,
  le titre n'est jamais fondu) commités avant toute implémentation,
  échec en compilation documenté. PIEGE-17 et PIEGE-16 sont couverts
  dès cette tranche ; PIEGE-18 (jetons sémantiques indépendants des
  diagnostics) suit à la tranche du client, son test sera écrit
  avant lui.
- **Le socle JSON** (`ValeurJson` et ses six formes scellées,
  `JsonLecteur`, `JsonEcrivain`) : immuable par copie, analyseur
  strict RFC 8259 découpé en trois lecteurs (structures, chaînes,
  nombres) — le zéro en tête, le plus unaire et NaN sont refusés,
  les échappements et l'Unicode échappé passent, les clés en
  doublon voient la dernière écriture l'emporter. Aucune dépendance
  tierce (ADR-005 : l'épisode 0.22.0 figé du dépôt de départ a
  montré le coût de la voie contraire).
- **Les enveloppes JSON-RPC 2.0** (`MessageRpc` scellé :
  `RequeteRpc`, `NotificationRpc`, `ReponseRpc`, `ErreurRpc`) :
  l'aller-retour complet, le tri strict des messages — réponse avec
  résultat ET erreur refusée, réponse sans aucun des deux refusée —,
  les codes du protocole dont les annulations du serveur
  (-32800 requête annulée, -32801 contenu modifié) que la couche
  d'ordonnancement distinguera d'une panne.
- **Le transport** (`Transport`, `Recepteur`, `CadreurTrames`,
  `DecodeurTrames`, `TransportMemoire`) : SPI injectable (ADR-005),
  cadrage « Content-Length » comptant les OCTETS UTF-8 de la charge,
  décodeur à machine à états — les en-têtes se suivent jusqu'à la
  ligne vide dans n'importe quel ordre, le corps se réassemble par
  morceaux de toute taille, chaque trame complète est mise de côté
  entière — et la paire en processus à livraison synchrone, le
  support des tests et des serveurs embarqués : la fermeture d'un
  bout coupe les deux sens et prévient le pair une seule fois.
- **La liste de complétion** (`ListeCompletion`, `ElementCompletion`,
  B-04) : les éléments du serveur dans l'ordre du serveur, la
  coupure `isIncomplete` honorée — le filtrage garde l'ordre
  relatif, le tri local (une optimisation permise aux listes
  complètes) est refusé aux listes tronquées. Les champs absents
  sont nuls, jamais devinés.
- **Les jetons sémantiques** (`JetonsSemantiques`, `JetonSemantique`,
  B-02) : le delta encodé du serveur décodé en positions absolues,
  la colonne remise à zéro à chaque changement de ligne, les
  quintuples exigés — le décodage est une fonction pure de la
  charge, aucun autre flux n'y entre (préambule de PIEGE-18).
- **Le modèle de documentation de complétion**
  (`CompletionDocModel` dans android-widgets, B-09) : les blocs
  typés balisés par clôtures Markdown deviennent des sections
  distinctes — genre et texte séparés, aucun marqueur qui fuit —,
  le texte hors clôtures vit à part, la clôture ouverte est
  tolérée, le genre inconnu conservé tel quel.

### Vérifié localement

- `./gradlew :core-lsp:check` et `:android-widgets:check` verts
  (Checkstyle, PMD, Error Prone + NullAway, JaCoCo ≥ 90 %,
  manifestes d'API compris) ;
- **41 tests core-lsp** (39 neufs : 11 JSON, 7 enveloppes, 6 trames,
  5 transport, 5 liste de complétion, 5 jetons), zéro échec ;
- **6 tests neufs** android-widgets (CompletionDocModel), zéro
  échec ;
- PIEGE-16 et PIEGE-17 couverts (registre à jour) ;
- manifeste d'API `core-lsp` régénéré.

### Écarts et limites

1. **Le transport à flux (stdio, prise réseau) suit à la tranche du
   client** : le cadrage et le décodage sont livrés et testés, le
   branchement à un vrai flux arrive avec le client.
2. **PIEGE-18 vit à la tranche du client** : le décodage pur est
   livré, l'indépendance des FLUX (jetons sans diagnostics,
   diagnostics sans jetons) se prouve sur le client — son test sera
   écrit avant lui.

## Tranche 2 — le client, ses documents et ses deux flux (2026-11-06)

### Livré

- **Le client** (`ClientLsp`) : poignée de main initialize (le nom du
  client et ses capacités aux paramètres, réponse attendue à délai
  borné) puis notification initialized ; documents versionnés à
  synchronisation complète — le texte entier voyage à chaque
  changement, le choix robuste — fermeture qui oublie l'état suivi ;
  la version de chaque uri reste exposée, la resynchronisation de la
  couche d'ordonnancement s'y accrochera.
- **Les capacités du serveur** (`CapacitesServeur`) : la surface
  bornée du protocole que le client consomme — fournisseurs de
  complétion, survol, signature, définition, références, renommage,
  correctifs, formatage ; genre de synchronisation ; légende des
  jetons sémantiques (types et modificateurs).
- **Les diagnostics** (`DiagnosticLsp` + l'auditeur) : publication
  poussée par le serveur, décodage plage/gravité/message/code/source,
  dernier état par uri, l'auditeur prévenu sur le fil de travail.
- **Les jetons sémantiques** (`tirerJetons`, B-02) : TIRÉS par requête
  du client — `textDocument/semanticTokens/full` — réponse décodée,
  stockée, publiée à l'auditeur. **PIEGE-18 couvert** : le tir marche
  sans qu'aucun diagnostic n'existe, une tempête de diagnostics ne
  change rien au jetons rendus, le tir continue après — les deux flux
  vivent seuls, l'audit du dépôt de départ avait documenté le tir
  chaîné aux diagnostics.
- **La transposition des positions** (`PositionLsp`, `PlageLsp`,
  `PositionsLsp`, ADR-005 point 5) : les colonnes LSP comptent les
  unités UTF-16 — le repère des chaînes Java — l'aller-retour
  offset/position, les plages et leurs bornes, le clampage des
  serveurs généreux (ligne au-delà vers la dernière, colonne au-delà
  vers la fin du contenu, négatifs vers zéro) — l'audit avait
  documenté les pièges de bornes, le clampage est le rempart.
- **Le modèle d'exécution** (ADR-007) : un fil de travail propre
  consomme la file entrante — les réponses complètent leurs futurs,
  les notifications nourrissent les auditeurs, les requêtes du serveur
  reçoivent l'erreur de méthode (la surface bornée n'en supporte
  aucune) ; la fermeture demande shutdown puis notifie exit,
  idempotente, le serveur muet n'y peut rien.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt avec SDK Android ;
- **59 tests core-lsp** (18 neufs : 9 client, 6 transposition, 3 flux
  de jetons), zéro échec ;
- PIEGE-18 couvert (registre à jour) — les trois pièges du jalon le
  sont désormais ;
- manifeste d'API `core-lsp` régénéré.

### Écarts et limites

1. **Le transport à flux (stdio, prise réseau) suit à la tranche de
   l'ordonnancement** : le cadrage et le décodeur sont livrés et
   testés (tranche 1), le branchement à un vrai flux arrive avec la
   couche d'annulation/priorité/fusion qui l'accompagne.
2. **Les requêtes interactives (complétion, survol, définition…)
   suivent à la tranche de l'ordonnancement** : le socle de requête à
   futur est en place, les méthodes de surface arrivent avec leur
   garde de version.

## Tranche 3 — l'ordonnancement et le transport à flux (2026-11-06)

### Livré

- **L'ordonnanceur** (`OrdonnanceurLsp`) : la couche qu'il aurait
  fallu écrire au-dessus de toute liaison tierce —
  **fusion** (deux demandes identiques en vol ne font qu'une requête
  sur le fil, les deux appelants partagent le futur gardé),
  **annulation au remplacement** (la demande plus fraîche de la même
  famille émet `$/cancelRequest`, le futur remplacé échoue REMPLACÉE,
  la réponse tardive du serveur tombe sans casse),
  **priorités** (l'interactif devance le fond du même document,
  DEVANCÉE — jamais l'inverse), **resynchronisation** (la réponse
  arrivée après un changement de version est périmée, PÉRIMÉE — la
  garde compare la version au départ du vol et à l'arrivée).
  L'élagage paresseux garde les cartes au vol, jamais à l'histoire.
- **La complétion typée** (`OrdonnanceurLsp.completion`, B-04) : la
  requête part par la clé du document, priorité interactive — la
  réponse est rendue au modèle `ListeCompletion`, la coupure du
  serveur honorée jusqu'au bout (PIEGE-17 réaffirmé au fil de la
  requête : jamais triée ni complétée localement au-delà de la
  coupure).
- **Le transport à flux** (`TransportFlux`) : le cadrage
  Content-Length sur des flux quelconques — les tubes d'un processus
  serveur, une prise réseau — un fil de lecture dédié, l'écriture
  protégée, les messages reçus avant branchement retenus puis servis.
  Les trois transports de l'ADR-005 sont en place : en processus,
  à flux, le cadrage commun.
- **Le serveur factice** gagne les réponses différées : les requêtes
  retenues, achevées à la demande du test — le vol devient observable,
  les quatre règles se prouvent dessus.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt avec SDK Android ;
- **69 tests core-lsp** (10 neufs : 6 ordonnanceur, 4 flux), zéro
  échec ;
- manifeste d'API `core-lsp` régénéré.

### Écarts et limites

1. **Les autres requêtes interactives (survol, définition, références,
   renommage, correctifs, formatage)** rejoignent l'ordonnanceur aux
   tranches qui les consomment — le socle `demander` les portera sans
   changement.
2. **Le lancement du processus serveur ( tubes stdio d'un vrai
   serveur)** vit avec le corpus de traces de la clôture — le
   transport à flux est prouvé sur tubes JVM, le branchement à un
   processus suivra le besoin réel de l'hôte.
