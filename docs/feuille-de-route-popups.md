# Feuille de route de la mission popups — audit, restructuration, unification

> La mission du propriétaire — « audit, restructuration et unification du
> popup system de Celeris » — reçue le 2026-11-13, se suit ici : les neuf
> phases du mandat, le journal des clôtures, ce qui reste. L'état est
> vérifié au 2026-11-13 (la phase 9 fermée au commit `7c05767` ; le signal
> du propriétaire du même jour — « lancé 90 % du chantier » — ouvre la
> SUITE : les surfaces qui dormaient se réveillent une à une, le journal
> tient les livraisons). Les documents de la mission vivent tous dans
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
| 5 | Positionnement et comportement | couche + clavier délégué | **clos le 2026-11-13** — bornage défensif des poses, ancres bornées, navigation clavier par délégation (PIEGE-27 préservé) ; le pas étendu PAGE_UP/PAGE_DOWN, resté à venir, se livre au signal du propriétaire (journal §2) |
| 6 | Intégration des moteurs | contrats vérifiés | **clos par constat** — aucune fausse intégration : les popups ne demandent que ce que les moteurs servent réellement (audit §5) |
| 7 | Migration des popups existants | rapport de migration | **clos le 2026-11-13** — aucun composant réécrit, aucune classe supprimée ; le manager absorbe les duplications, les satellites rétrécissent |
| 8 | Tests et validation | rapport de tests | **clos le 2026-11-13** — bancs dédiés au kit, à la vue et à la démo ; la passe de référence se rafraîchit — 2 442 tests, 0 échec, 14 modules (le feu vert du 2026-11-14 a livré les cinq dernières) |
| 9 | Documentation | guide + rapports + la présente | **clos au présent commit** — le guide du développeur (ajouter un popup sans toucher au manager), les deux rapports, la présente feuille de route |

## 2. Journal des clôtures

| Date | Commit | Ce qui se ferme |
| --- | --- | --- |
| 2026-11-13 (nuit et matin) | `b6fa94b` | Le socle préalable — les régimes du contrôleur rendent leurs fermés (`onKeystroke`, `onScroll`, `closeAll` passent de `void` à `Set<PopupKind>`), le re-pose d'une carte ne la tue pas (la course du retrait différée). Bancs : `PopupLayerReposeTest` (2), `PopupControllerTest` +3 (8 tests). |
| 2026-11-13 (matin) | — | L'audit se mène, lecture du dépôt au commit `9c23084` — le socle préalable du matin y est déjà constaté : dix problèmes gradués (P1 vues partagées, P2 deux sources de vérité, P3 positionnement en trois systèmes, P4 six copies de mesure, P5 aucun clavier, P6 cycle de vie, P7–P10 écarts moindres) ; l'inventaire fiche chaque surface, la classification fixe les catégories. |
| 2026-11-13 | `38d315c` | Le cœur du chantier : `PopupManager` (l'unique écrivain de l'état ET des vues), le contrat `PopupClavier`, le genre propre `TOOLBAR_OVERFLOW`, le tap hors cartes, le crochet public `EditorView.setSurTouchePopup` (la délégation clavier, PIEGE-27 préservé), la démo migre (le manager règne, les cartes partagées meurent). Bancs : `PopupManagerTest` (13), `TouchePopupVueTest` (2), `PopupControllerTest` +5 (13 tests). Les trois documents d'audit, d'architecture et d'inventaire se committent. |
| 2026-11-13 | `c400665` | La phase 8 ferme : la spécification D-10 de `BancSpecs` rejouée par le banc (le clavier de bout en bout — frappe, flèches, Enter, Escape), l'USAGE §9 réécrit au montage en trois temps, la typographie des trois documents réparée. |
| 2026-11-13 | présent commit | La phase 9 ferme : les documents de la mission s'achèvent et se rangent dans `docs/` — la feuille de route (la présente), le rapport de migration, le rapport de tests, le guide du développeur ; la feuille de route restante du projet et le changelog suivent. |
| 2026-11-13 (le signal ouvre la suite) | `22b386a` | **Le pas étendu PAGE_UP/PAGE_DOWN** — le premier reste réveillé : le contrat `PopupClavier` gagne `surPage(pas)` (une page par unité), la complétion saute sa surbrillance d'une page de huit candidats (son plafond visible, bornée aux murs de la liste), le menu d'intentions décline (la touche retourne à l'éditeur — le document y défile), le manager route les deux codes dans le même ordre de délégation. Banc : `PopupManagerTest.laPageEtendueSertLaCompletion` (+1, 14 tests). |
| 2026-11-13 (la suite continue) | `066f5ba` | **La carte des usages du symbole — la première surface réveillée** : le genre 17e `REFERENCES` et la carte `ReferencesView` (patron `TextRowsPopup`, en-tête qui compte, rangées cliquables, saut au choix), les régimes (deux surfaces d'actions ne tiennent jamais — elle chasse et est chassée par les menus ; frappe, défilement, tap hors cartes la referment ; le voile la coupe), le routage clavier la sert en dernier (flèches, Enter — la page décline). La démo gagne `AtelierUsages` (le serveur sert les lieux par le fil `references`, l'atelier transpose — le lieu devient occurrence, le début de plage devient l'offset du saut ; le lieu étranger au document ne se devine jamais) et l'action au menu d'intentions, à la puce de débordement et à la carte (`action_usages`) ; `AtelierPopups` se nomme au plafond relevé (612 lignes, ADR-019 — la décision du propriétaire). Bancs : `PopupControllerTest` +2 (exclusions réciproques, trois régimes), `PopupManagerTest.leClavierDelegueSertLesUsages` (+1, 15 tests), `ReferencesViewTest` (3), `UsagesReferencesTest` (démo — les lieux servis par le fil, la transpose du saut). |
| 2026-11-13 (la passe des six surfaces — l'info de type) | `7845790` | **Le fil rouge du guide devient réel** : le genre 18e `TYPE_INFO` et sa carte `TypeInfoView` (une rangée titre, la carte passive ne règne pas sur le clavier), le lecteur local `LecteurTypes` lit la DÉCLARATION (« Type symbole », hors mots de conduite — jamais un type deviné), l'atelier la pose sous le curseur. Bancs : `PopupControllerTest.typeInfoVitLesRegimesDuSurvol` (+1), `TypeInfoViewTest` (2), `TypeInfoTest` (3). |
| 2026-11-13 (la passe — la hiérarchie) | `a5edfc1` | **Les moteurs se réveillent** : le genre 19e `HIERARCHY` et sa carte `HierarchiesView` rendent l'arbre en rangées indentées par PROFONDEUR (le patron `Row` gagne le niveau d'indentation), `Hierarchies.types`/`appelants` (core-intelligence) servent le projet embarqué, le sommet connu SAUTE à sa déclaration, l'introuvable vit muet, le doute se porte en légende. Bancs : `PopupControllerTest.hierarchyExcludesTheActionSurfacesAndReciprocally` (+1), `HierarchiesViewTest` (3), `HierarchiesTransposeTest` (2). |
| 2026-11-13 (la passe — la notification) | `14e5ee0` | **La forme se décide : la bulle éphémère** : le genre 20e `NOTIFICATION` vit le régime UNIQUE de l'annonce (frappe, défilement et geste extérieur l'épargnent, seule son heure la conclut), la carte `NotificationView` s'ancre au coin bas-droit (`PopupAnchor.bottomRightCorner` naît), l'horloge la referme (la re-pose réarme, jamais deux courses), le tap la prend. Les correctifs appliqués et la pause de débogage s'y annoncent. Bancs : `PopupControllerTest.notificationVitLeRegimeUniqueDeLannonce` (+1), `NotificationViewTest` (3), `PopupAnchorTest` (+1). |
| 2026-11-13 (la passe — les valeurs de débogage) | `f50d029` | **La SPI servie trouvait sa surface** : la couche `ValeursPainter` (android-view) rend les valeurs en fin de ligne UTILE (les blancs de queue ne comptent pas, encre de gouttière, ligne de base du code — PIEGE-11), le crochet public `EditorView.setValeursEnLigne` les pose, la pause simulée y lit les affectations écrites de sa ligne (déterministe, jamais deviné) — la SPI efface les valeurs d'une pause d'avant. Bancs : `ValeursPainterTest` (2), `FauxDebogueurTest` (+1), `ValeursPauseTest` (2). |
| 2026-11-13 (la passe — l'aller à) | `0086567` | **Le modèle du plan sert la recherche** : la feuille `FeuilleAller` (feuille du bas, ADR-018) tape quelques lettres, la recherche floue B-13 répond — le modèle pur `ResultatsAller` mélange les fichiers du catalogue et les symboles du plan par score (fichiers d'abord à égalité), le choix SAUTE au symbole ou ouvre le fichier, la requête muette sert le catalogue en raccourci. Bancs : `ResultatsAllerTest` (2). |
| 2026-11-13 (la passe — documentation externe et import) | `4e5fc4d` | **La consultation et la résolution** : le genre 21e `EXTERNAL_DOC` vit le régime de la CONSULTATION (la frappe ne la chasse pas, le défilement la REPOSITIONNE, le geste extérieur la referme), l'atelier `AtelierDocExterne` dérive la carte de documentation hors complétion (instance propre, même chemin de données que le survol) ; le correctif d'import local (catégorie B) rejoint la feuille d'aperçu : `CorrectifImport` résout l'identifiant par le classpath du terrain, l'édition s'y prévoit (B-18, B-17). Bancs : `PopupControllerTest.documentationExterneSeConsulteSansCederALaFrappe` (+1), `CorrectifImportTest` (2). |
| 2026-11-14 (la passe des cinq — le feu vert du propriétaire) | `c371acd` | **La génération se réveille** : le genre 22e `GABARITS` sert les actions au caret — constructeur, accesseurs, equals/hashCode/toString lus des CHAMPS RÉELS de la classe englobante (B-24), l'entourage de la sélection (le moteur pur `Entourer` ceint, indente d'un cran, sélectionne le bloc), le gabarit de méthode à trous liés (A-22 — le canal unique de la refactorisation, la carte n'est qu'une peau de plus). Bancs : `PopupControllerTest` +1, `PopupManagerTest` +1, `EntourerTest` (3), `AtelierGabaritsTest` (2). |
| 2026-11-14 (la passe — la sûreté) | `a2a94bd` | **Les verdicts et les plans se réveillent** : les genres 23e `SAFE_DELETE` et 24e `SAFE_MOVE` servent le moteur B-23 — les usages qui barrent s'affichent en carte d'usages (chacun saute, la suppression attend), la voie libre efface en transaction annulable et l'annonce ; les types candidats au déménagement se proposent, les conditions de sûreté seules décident, le refus s'annonce sans raison inventée. Les exclusions deviennent la table exhaustive `FAMILLE_ACTIONS`. Bancs : `PopupControllerTest` +2, `AtelierSureteTest` (2). |
| 2026-11-14 (la passe — le masquage d'inspection) | `602fee8` | **La marque naît, le lint l'honore** : le genre 25e `SUPPRESS_INSPECTION` vit le régime du menu du caret — les diagnostics réglés de la ligne du caret (l'identifiant entre crochets, jamais une règle devinée) se proposent, le choix pose le commentaire « noinspection » en fin de ligne (le moteur pur `SuppressionInspection`, transaction annulable), l'analyse se relance : la règle masquée ne se rapporte plus, les autres demeurent. Bancs : `PopupControllerTest` +1, `SuppressionInspectionTest` (3 — la boucle complète), `FauxLintTest` +1. |
| 2026-11-14 (la passe — les détails d'exception) | `4b96a53` | **L'exécution et la trace se réveillent** : le genre 26e `EXCEPTION_DETAILS` vit le régime de la consultation — l'exécution simulée passe par la fausse exécution de référence (E-04), l'échec porte la trace du document réel (le type englobant du caret porte la classe, la méthode englobante porte le sommet à la ligne du caret — jamais deviné), la trace se relit par l'analyseur de piles (E-06 se réveille) ; la carte `ExceptionView` du kit rend le type à l'encre d'erreur, le message, la pile — le cadre du fichier ouvert saute, l'étranger vit muet ; le fichier sans méthode ne peut échouer. Bancs : `PopupControllerTest` +1, `ExceptionViewTest` (2), `TraceExecutionTest` (3). |
| 2026-11-14 (la passe — les documents suivent) | présent commit | **La passe se consigne** : l'inventaire marque les cinq surfaces livrées (plus de manquant — les catégories A–E closes, la S-13 triplée meurt), le journal tient les quatre livraisons, le rapport de tests se rafraîchit (2 442 tests, les bancs du feu vert, les cinq échecs tranchés), le changelog et l'USAGE suivent (vingt-six genres), le guide dit la table des exclusions — la nomination du banc de la machine se consigne. |

## 3. Ce qui restait de la mission — le second signal, les cinq dernières se réveillent

Le signal du propriétaire du 2026-11-13 (« lancé 90 % du chantier »)
réveille la suite : les surfaces manquantes de l'inventaire (§3 de
`POPUP_INVENTORY.md`) ; les deux premières — le pas étendu PAGE_UP/PAGE_DOWN, la carte
des usages du symbole — sont livrées au journal §2, chacune
reprend le socle commun sans y rien ajouter :

| LIVRÉ au signal du 2026-11-13 | | |
| Hiérarchies de types et d'appels | surface (catégorie C) | le genre 19e et `HierarchiesView` — journal §2 |
| Valeurs de débogage en ligne | surface (catégorie E) | `ValeursPainter` + `setValeursEnLigne` — journal §2 |
| Notifications | surface (catégorie G) | le genre 20e et `NotificationView` — journal §2 |
| Recherche de symbole / fichier / classe | surface (catégorie C) | `FeuilleAller` + `ResultatsAller` — journal §2 |
| Documentation externe, info de type | surface (catégorie A) | les genres 18e et 21e — journal §2 |
| Import resolution | surface (catégorie B) | `CorrectifImport` + la feuille d'aperçu — journal §2 |
| LIVRÉ au feu vert du 2026-11-14 | | |
| Generate Code / Gabarits, Surround With | surface (catégorie D) | le genre 22e `GABARITS` + `AtelierGabarits` + le moteur `Entourer` — journal §2 |
| Safe Delete / Move | surface (catégorie D) | les genres 23e/24e + `AtelierSurete` — journal §2 |
| Suppress Inspection | surface (catégorie B) | le genre 25e + `SuppressionInspection`, le lint honore la marque — journal §2 |
| Exception Details | surface (catégorie E) | le genre 26e + `TraceExecution` (E-04, E-06 réveillés) — journal §2 |

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
| Les popups existants intégrés sans perte | tenu | non-régression verte : 2 442 tests, 0 échec ; les bancs du kit inchangés dans leurs attentes (seules les tables EXHAUSTIVES des régimes gagnent les genres nouveaux, et les attentes du partage de cartes, remplacées par DEUX cartes distinctes) |
| Les responsabilités clairement séparées | tenu | machine (contrôleur), portage (couche), coordination (manager), rendu (panel/palette), positionnement (placer/anchor) — l'architecture §2 |
| Les conflits correctement gérés | tenu | exclusions binaires, cascades, ordre de routage, voile — bancs `PopupControllerTest` |
| Le positionnement intelligent | tenu | ancres bornées + bornage défensif aux murs — `PopupManagerTest.laPoseSeBorneAuxMurs` |
| Le clavier et le focus corrects | tenu | délégation PIEGE-27 — `TouchePopupVueTest`, `PopupManagerTest.leClavierDelegueSertLaCompletion`, D-10 rejouée |
| Les opérations asynchrones maîtrisées | tenu avec réserve | la course du retrait différé est corrigée, le re-pose ne re-naît pas ; le jeton de génération des réponses concurrentes reste un mécanisme d'hôte (limitation consignée au rapport de migration §7) |
| Le système testable et extensible | tenu | un banc Robolectric par invariant ; le guide du développeur pose la recette de l'extension |
| Ajouter des popups sans toucher à l'architecture | tenu | la recette du guide : lier, déclencher, déléguer — aucun changement au manager pour un genre nouveau |
| Le projet compile et les tests passent | tenu | la passe de référence du 2026-11-14 (`POPUP_TEST_REPORT.md`) — 2 442 tests, 0 échec |

La mission se referme ici : l'infrastructure est livrée, testée et
documentée ; les surfaces nouvelles reprendront le socle au fil des
signaux, chacune par la recette du guide.

