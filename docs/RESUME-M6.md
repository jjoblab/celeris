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

## Tranche 4 — l'indexeur hors ligne (2026-11-06)

### Livré

- **L'index de symboles** (`IndexSymboles`, `Symbole`) : alimenté par
  le plan du même arbre que la coloration (B-16, livré M4),
  remplacé fichier par fichier à chaque ré-analyse — jamais il ne
  cumule — retiré à la fermeture. La règle des rôles de l'ADR-005
  s'y lit : l'index répond **vite et hors ligne** (aller à un
  symbole, usages de base), le serveur répond **juste** (complétion
  typée, raffinement) — jamais de chaînage.
- **Les usages** (B-11) : occurrences ENTIÈRES du nom à travers les
  fichiers indexés, casse exacte, déclarations comprises. Les jetons
  du lexique décident à l'indexation : jamais un usage dans une
  chaîne ou un commentaire ; les balises du balisage comptent à
  l'ouverture et à la fermeture (le chevron fermant s'ôte du nom).
- **La recherche floue** (`RechercheFloue`, B-13) : identique >
  préfixe > début de mot interne (camelCase ou souligné) >
  sous-chaîne > sous-suite, insensible à la casse — l'aller à
  symbole ordonne par score puis par nom, la requête muette montre
  tout.
- **Le budget mémoire** : les occurrences de tous les fichiers sont
  bornées (200 000 par défaut), la saturation est signée par
  fichier, l'ancien état rend son budget avant le nouveau décompte —
  la dégradation est documentée, jamais silencieuse.
- **La persistance** : forme sérialisée compacte (une entrée par
  ligne, en-tête signé) qui refait l'index identique — symboles,
  usages, compte.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt avec SDK Android ;
- **88 tests core-intelligence** (16 neufs : 9 index, 7 recherche
  floue), zéro échec ;
- manifeste d'API `core-intelligence` régénéré.

### Écarts et limites

1. **L'incrément est par fichier** : la ré-analyse d'un fichier
   remplace son état dans l'index — la granularité du plan M4, pas
   celle du symbole. Suffisant au geste (une édition, un fichier),
   documenté.
2. **La navigation B-10 côté vue** : l'index donne les coordonnées
   (aller à la déclaration, retour par l'historique M5-t4), le
   raccord aux commandes de la vue vit au jalon qui le consomme.

## Tranche 5 — la complétion de mots et les imports (2026-11-06)

### Livré

- **La complétion de base** (`CompletionMots`, B-03) : le préfixe
  tape, les mots du fichier répondent — sans serveur, sans réseau,
  au mot près. Les mots du CODE seulement : les jetons du lexique
  décident, jamais les chaînes ni les commentaires. Le mot en cours
  de frappe n'est pas son propre candidat — un mot identique
  ailleurs, lui, l'est. Les mots réservés du langage se joignent par
  paramètre, le tri ignore la casse, le plafond est de cent
  candidats.
- **L'import automatique** (`ImportAuto`, B-05) : le candidat
  s'insère en tête du bloc des imports — après la déclaration de
  paquet, avant le premier import, à la position zéro sans paquet —
  par une ÉDITION DE POSITION UNIQUE, la sélection posée au début de
  l'insert. L'import déjà présent ne se re-insère jamais : la
  présence se vérifie à la ligne d'import entière, jamais dans une
  chaîne.
- **L'optimisation des imports** (`OptimiseurImports`, B-27) : chaque
  import rapporté utilisé ou non — l'usage compte dans le code, hors
  des lignes d'import elles-mêmes, jamais dans les chaînes ni les
  commentaires ; l'import statique porte son dernier segment.
  SIGNALÉ, jamais supprimé en silence : l'analyseur éclaire, l'hôte
  décide.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt avec SDK Android ;
- **107 tests core-intelligence** (19 neufs : 8 complétion, 5 import,
  6 optimisation), zéro échec ;
- manifeste d'API `core-intelligence` régénéré.

### Écarts et limites

1. **La complétion hors ligne propose des mots, pas des symboles
   typés** : le raffinement (surcharge, arguments attendus) appartient
   au serveur LSP (B-04, livré au fil de l'ordonnanceur) — la règle
   des rôles de l'ADR-005 est tenue.
2. **Le tri des candidats est alphabétique** : le score par fréquence
   ou par distance au curseur suivra le besoin réel — la surface est
   bornée à cent.

## Tranche 6 — les éditions d'espace et le bloqué historique (2026-11-06)

### Livré

- **Le modèle d'édition d'espace** (`EditionEspaceLsp`,
  `EditionDocumentLsp`, `EditionTexteLsp`, `EditionGabaritLsp`) : les
  éditions de document ET les édits de gabarit — la forme que la
  version 1.0.0 de la liaison tierce du dépôt de départ imposait et
  qui n'avait jamais pu être absorbée. La forme est nôtre, documentée
  (sorte `celeris/templateEdit`), le protocole standard n'en définit
  aucune. La forme héritée « changes » (carte uri → éditions, sans
  version) se plie dans les documents ; les réponses vides sont
  tolérées.
- **La transposition** (`EditsEspaceLsp`) : les plages LSP (unités
  UTF-16, bornes clampées) deviennent une transaction de la voie
  unique — diffable, annulable (B-17), ordre DESCROISSANT
  d'application : les offsets originaux du serveur restent valides
  au fil de la transaction séquentielle.
- **Le renommage multi-fichiers** (`Renommage`, B-19) : le plan se
  construit sur l'index hors ligne — une édition par occurrence,
  fichier par fichier. Les conflits sont SIGNALÉS, jamais tranchés :
  ambiguïté (deux déclarations du même nom), collision (nouveau nom
  déjà pris) — le plan tient, l'hôte lit puis décide. Le renommage
  AU SERVEUR rejoint l'ordonnanceur (`textDocument/rename`,
  priorité interactive, garde de version).
- **Les correctifs rapides** (`CorrectifLsp` + l'ordonnanceur, B-17) :
  les titres à montrer, les éditions à appliquer par transaction
  annulable — le correctif sans édition reste un correctif, l'hôte
  montre le titre.
- **Les squelettes de méthode** (`SquelettesMethode`, B-06) : le
  squelette naît de la signature que le serveur a donnée — trous
  `p1..pN` aux paramètres (défauts `argN` posés au rendu), corps en
  dernier — curseurs aux trous dans l'ordre de tabulation, le moteur
  de gabarits du M5 fait le reste.
- **La composition** (`EditsEspaceDemoTest` dans la démo) : le point
  de composition voit le client LSP et le moteur de gabarits — les
  modules purs restent latéraux (ADR-002), la démo gagne la
  dépendance `core-lsp` à cet effet. Renommage appliqué et annulé,
  édit de gabarit inséré par la voie unique — défauts posés,
  premier trou sélectionné, annulation intégrale : **le bloqué
  historique débloqué de bout en bout**.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt avec SDK Android ;
- **75 tests core-lsp** (6 neufs), **115 core-intelligence** (8
  neufs), 5 à la démo (2 neufs de composition), zéro échec ;
- manifestes d'API `core-lsp` et `core-intelligence` régénérés.

### Écarts et limites

1. **Les édits de gabarit ne vivent que dans la forme celeris** : le
   protocole standard n'en définit pas — la forme est documentée,
   les traces de contrat de la clôture la portent.
2. **Le renommage au serveur est la requête** : la résolution
   sémantique (quel symbole exactement) appartient au serveur ; le
   chemin hors ligne signale ses ambiguïtés, il ne les tranche pas.

## Tranche 7 — formatage, editorconfig et génération (2026-11-06)

### Livré

- **Le formatage** (`Formateur`, B-25) : l'indentation se règle sur
  les niveaux de l'arbre — le même arbre que la coloration
  (ADR-004) —, chaque ligne porte le retrait de son niveau, la
  ligne qui ferme un groupe porte le niveau de ce groupe, les
  lignes vides ou blanches restent vides sans indentation inventée.
  **IDEMPOTENT par construction** : les niveaux ne dépendent pas du
  blanc, reformater le formaté redit le formaté — le critère du
  jalon, prouvé sur Java, Kotlin et XML, le style à tabulations
  compris.
- **Le respect de `.editorconfig`** (`EditorConfig`, B-26) : les
  sections se matchent par extension — la plus spécifique gagne —,
  les propriétés hors section (la racine) portent les défauts, le
  chemin profond décide par sa fin. Les clés inconnues et les
  commentaires sont ignorés, `indent_size` à `tab` suit le style,
  quatre par défaut ; les fins de ligne converties selon la règle
  (`crlf`, `lf`), sans règle le texte reste tel quel. Les règles
  résolues exposent style, fin de ligne et encodage — les défauts
  canoniques sans section.
- **La génération de code** (`Generateurs`, B-24) : les champs lus
  dans l'arbre — types compris : les primitives sont des mots
  réservés du lexique, jamais des noms de champ — dans l'ordre du
  source. Le constructeur (un paramètre et une affectation par
  champ), les accesseurs aux conventions de nommage (le booléen en
  `is`, jamais `get`), `equals`/`hashCode`/`toString` tous champs —
  primitifs comparés, objets à `Objects.equals` —, la position
  d'insertion avant l'accolade fermante de la classe porteuse.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt avec SDK Android ;
- **134 tests core-intelligence** (19 neufs : 6 formatage,
  7 editorconfig, 6 génération), zéro échec ;
- manifeste d'API `core-intelligence` régénéré ;
- correctif du contrôle Javadoc de la politique de langue : les
  blocs de texte Java portent du code factice — leurs lignes ne
  sont plus prises pour des déclarations (le contrôle échouait à
  tort sur le test de génération, vert après).

### Écarts et limites

1. **Le formatage règle le retrait, pas le reste de la ligne** :
   l'espacement intra-ligne (opérateurs, virgules) reste tel que
   tapé — les niveaux de l'arbre décident, la typographie fine
   suivra le besoin réel de l'hôte.
2. **Les motifs de section restent par extension** : les motifs
   glob (`**`, chemins composés) du format `.editorconfig` ne sont
   pas interprétés — la racine porte les défauts, l'extension
   décide, le reste suivra le besoin réel.
