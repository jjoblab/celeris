# Résumé M6 — sémantique (en cours)

> **Déclenchement** : clôture du jalon M5 (2026-11-06, critères de
> sortie couverts — catégorie A P0 et D-08/D-09/D-10, quarante-trois
> scénarios). Périmètre (feuille de route) : client LSP propre
> (annulation, priorités, fusion, resynchronisation ;
> transports stdio/réseau/in-process) ; indexeur hors ligne ;
> complétion, aide aux paramètres, survol, définition, usages,
> renommage, correctifs, formatage, indices en ligne, jetons
> sémantiques, diagnostics ; moteur de refactorisation ; édits de
> gabarit (le blocage historique 1.0.0). PIEGE-16 à PIEGE-18 écrits
> avant. Sortie visée : catégorie B P0 couverte, critères testés sur
> projets Android réels (corpus embarqué).

## Tranche 1 — le socle du client et les pièges d'abord (2026-11-06)

### Livré

- **Le piège écrit avant le code** (règle du registre) : les tests
  PIEGE-17 (liste de complétion tronquée honorée telle quelle —
  jamais réordonnée, jamais complétée localement) et PIEGE-16 (les
  blocs typés de la documentation de complétion restent distincts,
  le titre n'est jamais fondu) commités avant toute implémentation,
  échec en compilation documenté. PIEGE-17 et PIEGE-16 sont couverts
  dès cette tranche ; PIEGE-18 (jetons sémantiques indépendants des
  diagnostics) suit à la tranche du client, son test sera écrit
  avant lui.
- **Le socle JSON** (`ValeurJson` et ses six formes scellées,
  `JsonLecteur`, `JsonEcrivain`) : immuable par copie, analyseur
  strict RFC 8259 découpé en trois lecteurs (structures, chaînes,
  nombres) — le zéro en tête, le plus unaire et NaN sont refusés,
  les échappements et l'Unicode échappé passent, les clés en
  doublon voient la dernière écriture l'emporter. Aucune dépendance
  tierce (ADR-005 : l'épisode 0.22.0 figé du dépôt de départ a
  montré le coût de la voie contraire).
- **Les enveloppes JSON-RPC 2.0** (`MessageRpc` scellé :
  `RequeteRpc`, `NotificationRpc`, `ReponseRpc`, `ErreurRpc`) :
  l'aller-retour complet, le tri strict des messages — réponse avec
  résultat ET erreur refusée, réponse sans aucun des deux refusée —,
  les codes du protocole dont les annulations du serveur
  (-32800 requête annulée, -32801 contenu modifié) que la couche
  d'ordonnancement distinguera d'une panne.
- **Le transport** (`Transport`, `Recepteur`, `CadreurTrames`,
  `DecodeurTrames`, `TransportMemoire`) : SPI injectable (ADR-005),
  cadrage « Content-Length » comptant les OCTETS UTF-8 de la charge,
  décodeur à machine à états — les en-têtes se suivent jusqu'à la
  ligne vide dans n'importe quel ordre, le corps se réassemble par
  morceaux de toute taille, chaque trame complète est mise de côté
  entière — et la paire en processus à livraison synchrone, le
  support des tests et des serveurs embarqués : la fermeture d'un
  bout coupe les deux sens et prévient le pair une seule fois.
- **La liste de complétion** (`ListeCompletion`, `ElementCompletion`,
  B-04) : les éléments du serveur dans l'ordre du serveur, la
  coupure `isIncomplete` honorée — le filtrage garde l'ordre
  relatif, le tri local (une optimisation permise aux listes
  complètes) est refusé aux listes tronquées. Les champs absents
  sont nuls, jamais devinés.
- **Les jetons sémantiques** (`JetonsSemantiques`, `JetonSemantique`,
  B-02) : le delta encodé du serveur décodé en positions absolues,
  la colonne remise à zéro à chaque changement de ligne, les
  quintuples exigés — le décodage est une fonction pure de la
  charge, aucun autre flux n'y entre (préambule de PIEGE-18).
- **Le modèle de documentation de complétion**
  (`CompletionDocModel` dans android-widgets, B-09) : les blocs
  typés balisés par clôtures Markdown deviennent des sections
  distinctes — genre et texte séparés, aucun marqueur qui fuit —,
  le texte hors clôtures vit à part, la clôture ouverte est
  tolérée, le genre inconnu conservé tel quel.

### Vérifié localement

- `./gradlew :core-lsp:check` et `:android-widgets:check` verts
  (Checkstyle, PMD, Error Prone + NullAway, JaCoCo ≥ 90 %,
  manifestes d'API compris) ;
- **41 tests core-lsp** (39 neufs : 11 JSON, 7 enveloppes, 6 trames,
  5 transport, 5 liste de complétion, 5 jetons), zéro échec ;
- **6 tests neufs** android-widgets (CompletionDocModel), zéro
  échec ;
- PIEGE-16 et PIEGE-17 couverts (registre à jour) ;
- manifeste d'API `core-lsp` régénéré.

### Écarts et limites

1. **Le transport à flux (stdio, prise réseau) suit à la tranche du
   client** : le cadrage et le décodage sont livrés et testés, le
   branchement à un vrai flux arrive avec le client.
2. **PIEGE-18 vit à la tranche du client** : le décodage pur est
   livré, l'indépendance des FLUX (jetons sans diagnostics,
   diagnostics sans jetons) se prouve sur le client — son test sera
   écrit avant lui.
