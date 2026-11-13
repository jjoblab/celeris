# Feuille de route de la mission popups — audit, restructuration, unification

> La mission du propriétaire — « audit, restructuration et unification du
> popup system de Celeris » — reçue le 2026-11-13, se suit ici : les neuf
> phases du mandat, le journal des clôtures, ce qui reste. L'état est
> vérifié au 2026-11-13 (la phase 8 fermée au commit `c400665`, la phase 9
> ferme au présent commit). Les documents de la mission vivent tous dans
> `docs/` : l'audit ([`POPUP_SYSTEM_AUDIT.md`](POPUP_SYSTEM_AUDIT.md)),
> l'inventaire ([`POPUP_INVENTORY.md`](POPUP_INVENTORY.md)),
> l'architecture cible ([`POPUP_SYSTEM_ARCHITECTURE.md`]\
> (POPUP_SYSTEM_ARCHITECTURE.md)), le rapport de migration
> ([`POPUP_MIGRATION_REPORT.md`](POPUP_MIGRATION_REPORT.md)), le rapport
> de tests ([`POPUP_TEST_REPORT.md`](POPUP_TEST_REPORT.md)) et le guide du
> développeur ([`POPUP_DEVELOPER_GUIDE.md`](POPUP_DEVELOPER_GUIDE.md)).
> Le reste du projet se suit dans
> [`feuille-de-route-restante.md`](feuille-de-route-restante.md) ; le
> journal des versions vit au [`CHANGELOG.md`](../CHANGELOG.md).

## 1. Vue d'ensemble — les neuf phases du mandat

| # | Phase du mandat | Livrable | Statut |
| --- | --- | --- | --- |
| 1 | Audit complet | audit + inventaire | **clos le 2026-11-13** — lecture du dépôt au commit `9c23084` ; dix problèmes constatés (P1–P10), sept composantes saines à conserver, aucune réécriture recommandée |
| 2 | Architecture unifiée | architecture cible | **clos le 2026-11-13** — le manager unique écrivain, le contrat commun étendu là où l'audit constatait le déficit, cinq décisions explicitement rejetées |
| 3 | Classification et organisation | inventaire §2 | **clos le 2026-11-13** — les catégories A–G, chaque surface avec sa forme propre (carte ancrée, feuille, infobulle, chrome), les manquants nommés |
| 4 | Priorités et conflits | contrôleur + manager | **clos le 2026-11-13** — exclusions binaires (deux menus ne tiennent jamais), ordre de routage clavier, régime sous voile, tap hors cartes |
| 5 | Positionnement et comportement | couche + clavier délégué | **clos le 2026-11-13** — bornage défensif des poses, ancres bornées, navigation clavier par délégation (PIEGE-27 préservé) ; PAGE_UP/PAGE_DOWN à venir |
| 6 | Intégration des moteurs | contrats vérifiés | **clos par constat** — aucune fausse intégration : les popups ne demandent que ce que les moteurs servent réellement (audit §5) |
| 7 | Migration des popups existants | rapport de migration | **clos le 2026-11-13** — aucun composant réécrit, aucune classe supprimée ; le manager absorbe les duplications, les satellites rétrécissent |
| 8 | Tests et validation | rapport de tests | **clos le 2026-11-13** — bancs dédiés au kit, à la vue et à la démo ; la passe de référence reste verte (2 384 tests, 0 échec, 14 modules) |
| 9 | Documentation | guide + rapports + la présente | **clos au présent commit** — le guide du développeur (ajouter un popup sans toucher au manager), les deux rapports, la présente feuille de route |

## 2. Journal des clôtures

| Date | Commit | Ce qui se ferme |
| --- | --- | --- |
| 2026-11-13 (nuit et matin) | `b6fa94b` | Le socle préalable — les régimes du contrôleur rendent leurs fermés (`onKeystroke`, `onScroll`, `closeAll` passent de `void` à `Set<PopupKind>`), le re-pose d'une carte ne la tue pas (la course du retrait différée). Bancs : `PopupLayerReposeTest` (2), `PopupControllerTest` +3 (8 tests). |
| 2026-11-13 (matin) | — | L'audit se mène, lecture du dépôt au commit `9c23084` — le socle préalable du matin y est déjà constaté : dix problèmes gradués (P1 vues partagées, P2 deux sources de vérité, P3 positionnement en trois systèmes, P4 six copies de mesure, P5 aucun clavier, P6 cycle de vie, P7–P10 écarts moindres) ; l'inventaire fiche chaque surface, la classification fixe les catégories. |
| 2026-11-13 | `38d315c` | Le cœur du chantier : `PopupManager` (l'unique écrivain de l'état ET des vues), le contrat `PopupClavier`, le genre propre `TOOLBAR_OVERFLOW`, le tap hors cartes, le crochet public `EditorView.setSurTouchePopup` (la délégation clavier, PIEGE-27 préservé), la démo migre (le manager règne, les cartes partagées meurent). Bancs : `PopupManagerTest` (13), `TouchePopupVueTest` (2), `PopupControllerTest` +5 (13 tests). Les trois documents d'audit, d'architecture et d'inventaire se committent. |
| 2026-11-13 | `c400665` | La phase 8 ferme : la spécification D-10 de `BancSpecs` rejouée par le banc (le clavier de bout en bout — frappe, flèches, Enter, Escape), l'USAGE §9 réécrit au montage en trois temps, la typographie des trois documents réparée. |
| 2026-11-13 | présent commit | La phase 9 ferme : les documents de la mission s'achèvent et se rangent dans `docs/` — la feuille de route (la présente), le rapport de migration, le rapport de tests, le guide du développeur ; la feuille de route restante du projet et le changelog suivent. |

## 3. Ce qui reste de la mission — sur signal

La mission a livré l'INFRASTRUCTURE ; les surfaces manquantes de
l'inventaire (§3 de `POPUP_INVENTORY.md`) dorment jusqu'au signal du
propriétaire, chacune reprendra le socle commun sans y rien ajouter :

| Reste | Nature | Ce qui existe déjà |
| --- | --- | --- |
| PAGE_UP/PAGE_DOWN dans la complétion | navigation clavier | le routage délégué vit (`PopupManager.surTouche`) ; seul le pas étendu manque, au plafond de huit candidats visibles |
| Usages / références | surface (catégorie C) | les données sont servies (`LanguageSession.references`) ; la carte à rangées (`TextRowsPopup`) attend |
| Hiérarchies de types et d'appels | surface (catégorie C) | les moteurs vivent (`Hierarchies`, core-intelligence) |
| Valeurs de débogage en ligne | surface (catégorie E) | la SPI est servie (`ValeurEnLigne`, `FauxDebogueur` de la démo) |
| Notifications | surface (catégorie G) | rien — la forme reste à décider (bulle éphémère, vraisemblablement) |
| Recherche de symbole / fichier / classe | surface (catégorie C) | le plan (`FeuillePlan`) sert de modèle à la feuille de recherche |
| Documentation externe, info de type | surface (catégorie A) | la carte de documentation (`CompletionDocView`) se dérive |
| Import resolution, suppress inspection | surface (catégorie B) | les correctifs LSP vivent en feuille (`AtelierCorrectif`) |

Deux fins de non-recevoir assumées : les surfaces Git/Gradle/exécution
(catégorie F) ressortent de l'hôte — la SPI existe, l'éditeur n'intègre
pas d'atelier ; le chrome permanent (minimap, bande, pouce, touchbar)
n'entre pas dans la couche — ce sont des choix documentés (audit §4),
pas des doublons.

## 4. Les décisions du propriétaire consignées

- **Plafond de lignes relevable à 1 200** (signal du 2026-11-13) : les
  fichiers du chantier popups peuvent vivre jusqu'à 1 200 lignes et des
  méthodes plus longues — la décision s'applique PAR NOMINATION ciblée
  au jour où un fichier bute (le mécanisme de l'ADR-019 :
  `config/checkstyle/suppressions.xml`, chaque nomination porte sa
  justification), jamais par un relèvement global des seuils — les
  cliquets demeurent. Aucune nomination n'est encore nécessaire :
  aucun fichier du chantier ne bute (le plus gros : `BancSpecs.java`,
  770 lignes, banc nommé sous 800 ; `Feuille.java`, 534 ; `PopupManager`,
  346).
- **Le volume de tests sans quota** (2026-11-11, consigné à la feuille
  de route restante) : les bancs naissent au fil des chantiers ; les
  cliquets font foi.
- **La fluidité des cartes en voile** (2026-11-13) : naissance et mort
  des cartes en voile — livré (`b6fa94b`, la course du retrait corrigée).
- **Le color picker au double-clic d'un littéral** (2026-11-13) :
  livré (commit `90c192b`, atelier `AtelierCouleur`).
- **Le survol au double-clic et au « i » seuls** (2026-11-13) : livré
  (l'appui long retiré).

## 5. Les critères de réussite du mandat — où en est chacun

| Critère (mission §14) | Verdict | Preuve |
| --- | --- | --- |
| Une infrastructure de popups unifiée | tenu | `PopupManager` — l'unique écrivain de l'état et des vues (`USAGE.md` §9) |
| Les popups existants intégrés sans perte | tenu | non-régression verte : 2 384 tests, 0 échec ; les 28 bancs du kit inchangés dans leurs attentes (sauf les attentes du partage de cartes, remplacées par DEUX cartes distinctes) |
| Les responsabilités clairement séparées | tenu | machine (contrôleur), portage (couche), coordination (manager), rendu (panel/palette), positionnement (placer/anchor) — l'architecture §2 |
| Les conflits correctement gérés | tenu | exclusions binaires, cascades, ordre de routage, voile — bancs `PopupControllerTest` |
| Le positionnement intelligent | tenu | ancres bornées + bornage défensif aux murs — `PopupManagerTest.laPoseSeBorneAuxMurs` |
| Le clavier et le focus corrects | tenu | délégation PIEGE-27 — `TouchePopupVueTest`, `PopupManagerTest.leClavierDelegueSertLaCompletion`, D-10 rejouée |
| Les opérations asynchrones maîtrisées | tenu avec réserve | la course du retrait différé est corrigée, le re-pose ne re-naît pas ; le jeton de génération des réponses concurrentes reste un mécanisme d'hôte (limitation consignée au rapport de migration §7) |
| Le système testable et extensible | tenu | un banc Robolectric par invariant ; le guide du développeur pose la recette de l'extension |
| Ajouter des popups sans toucher à l'architecture | tenu | la recette du guide : lier, déclencher, déléguer — aucun changement au manager pour un genre nouveau |
| Le projet compile et les tests passent | tenu | la passe de référence du 2026-11-13 (`POPUP_TEST_REPORT.md`) |

La mission se referme ici : l'infrastructure est livrée, testée et
documentée ; les surfaces nouvelles reprendront le socle au fil des
signaux, chacune par la recette du guide.
