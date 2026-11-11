# ADR-019 — Plafond relevé des bancs volumineux

- **Statut** : Acceptée (décision du propriétaire du 2026-11-11)
- **Date** : 2026-11-11

## Contexte

L'ADR-002 fixe les plafonds de taille vérifiés en CI : 600 lignes par
fichier, 50 par méthode — « aucun cliquet relevable : toute exception
exige un ADR dédié ». Le plafond tient depuis M1 : la production vit
par composition de collaborateurs (l'orchestrateur `EditorView` reste
sous 600 lignes).

Le chantier C13 pousse le volume de tests vers la parité — l'écart
mesuré vit surtout dans l'interface (audit § 4.4). Les bancs
d'`android-view` et d'`android-widgets` grossissent : le banc de la
gouttière (`GutterPainterTest`, dix-neuf tests partageant leurs
appareils) atteint 602 lignes, et la traîne continue. Scinder chaque
banc en deux ne sépare pas des comportements : il sépare un appareil
partagé de ses tests, ou deux familles d'un même collaborateur.

Par ailleurs, sous AGP, les portes Checkstyle ne couvraient que le
source set principal des modules Android : les bancs y échappaient aux
plafonds — une couverture inférieure à celle des modules purs, qui
vérifient main et tests.

Le propriétaire autorise le 2026-11-11 : « élever le plafond de
certains fichiers, si nécessaire ».

## Décision

1. **Le plafond standard reste 600 lignes par fichier** (ADR-002) —
   pour toute la production et la majorité des bancs.
2. **Un plafond relevé à 800 lignes existe pour les bancs de tests
   nommés** : un fichier qui dépasse 600 n'y échappe que par une ligne
   nominative dans `config/checkstyle/suppressions.xml` — le plafond
   de 800 reste bloquant pour lui. Le cliquet demeure, plus haut.
3. La liste nominative ne grandit que par décision du propriétaire,
   consignée ici ; le mode `--maj` de la ligne de base de langue reste
   l'exemple à ne pas suivre : on nomme, on n'augmente pas la base.
4. Les portes Checkstyle des modules Android couvrent désormais le
   source set de tests comme celui de main — mêmes plafonds que les
   modules purs (600/800 par fichier, 50 par méthode, 120 par ligne).
5. Le plafond de 50 lignes par méthode ne se relève pas : un banc
   volumineux se découpe en méthodes, jamais en méthodes interminables.

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| Scinder chaque banc à 600 lignes | écartée | sépare l'appareil partagé de ses tests ; le banc de la gouttière est dix-neuf tests d'un seul collaborateur |
| Relever le plafond global à 800 | écartée | affaiblit le cliquet pour la production, contraire à l'ADR-002 |
| Plafond relevé nominatif à 800 (proposition) | **retenue** | le cliquet tient (600 standard, 800 nommé), la décision reste nominative et consignée |

## Compromis

- Un banc nommé peut atteindre 800 lignes : la relecture d'un gros banc
  se fait par méthodes, la contrainte de 50 lignes par méthode tient.
- La liste nominative est une dette visible : chaque entrée porte son
  justification ; le plafond de 800 reste une porte.

## Conséquences

- `config/checkstyle/checkstyle.xml` porte deux modules `FileLength`
  (600 standard, id `PlafondStandard` ; 800 relevé) et un filtre de
  suppression par identifiant ; `config/checkstyle/suppressions.xml`
  porte les nominations.
- Les conventions de build posent `configDirectory` pour que le filtre
  résolve `${config_loc}` dans Gradle comme en ligne de commande.
- Les bancs Android passent sous les plafonds Checkstyle ; les
  violations existantes (imports inutilisés, lignes longues, méthodes
  trop longues) sont corrigées au passage.

## Actions

- [x] Two-tier `FileLength` + `suppressions.xml` + `configDirectory`
      (2026-11-11).
- [x] Portes Checkstyle des bancs Android (source set de tests).
- [x] Banc de la gouttière : première nomination (602 lignes).
