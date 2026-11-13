# Rapport de migration du système de popups de Celeris

> La phase 7 de la mission — « migration des popups existants » — se
> rapporte ici : composant par composant, ce qui a migré, ce qui a
> changé, ce qui n'a pas bougé et pourquoi. L'état de départ est celui
> que l'audit constatait (lecture au commit `9c23084`,
> [`POPUP_SYSTEM_AUDIT.md`](POPUP_SYSTEM_AUDIT.md)) ; l'état d'arrivée
> est vérifié au 2026-11-13. La règle de sécurité du mandat a tenu :
> **aucune classe réécrite, aucune classe supprimée** — le coordinateur
> absorbe les duplications, les satellites rétrécissent parce que la
> boucle commune quitte leurs épaules, pas parce qu'on leur a coupé des
> responsabilités utiles.

## 1. Le principe de la migration

La migration n'a pas déplacé les surfaces : elle a extrait la BOUCLE
état→vues qui vivait en six exemplaires chez l'hôte (l'audit P2) et l'a
rendue au kit sous la forme d'un coordinateur — `PopupManager`, l'unique
écrivain de l'état ET des vues. Chaque étape a suivi la même discipline
de mandat : un correctif arrive avec le banc qui échouait avant et passe
après ; les API publiques historiques (`open`, `close`, `show`,
`dismiss`, `onKeystroke`, `onScroll`, `closeAll`) demeurent — les
nouveautés (`ouvrirRendus`, `closeRendus`, `surToucheHorsCartes`,
`lier`, `montrer`, `surTouche`) les COMPLÈTENT, jamais ne les
remplacent. Les trois temps du montage final (lier, déclencher,
déléguer) vivent à l'USAGE §9.

## 2. Les problèmes d'audit et leur traitement

| Problème (audit) | Traitement | Banc qui le prouve | Commit |
| --- | --- | --- | --- |
| P1 — vues partagées entre genres (l'état ment) | `lier` refuse qu'une carte serve deux genres DÈS le câblage ; la démo possède DEUX cartes de documentation (la complétion et le survol), le débordement de toolbar vit son genre propre | `PopupManagerTest.laCartePartageeSeRefuse`, `PopupLayerReposeTest.laMemeVueNeVitJamaisSousDeuxGenres` | `38d315c` |
| P2 — deux sources de vérité synchronisées à la main | le manager tient l'état et la couche d'un seul tenant : `ouvrirRendus`/`closeRendus` rendent l'ensemble des affectés, les exclus et les cascadés quittent la couche d'un seul appel | `PopupControllerTest.openRendusCarryTheExcludedGenres`, `closeRendusCarryTheCascadedGenres` ; `PopupManagerTest` (cascades isochrones) | `b6fa94b`, `38d315c` |
| P3 — positionnement en trois systèmes, `rightEdge` mort | le débordement de toolbar quitte le ad hoc : genre `TOOLBAR_OVERFLOW`, carte liée, posée par la couche ; la couche BORNE désormais toute pose aux murs (défense en profondeur). `rightEdge` CONSERVÉ : ancre publique du kit, bancée (`PopupAnchorTest`), servie aux hôtes tiers — l'écart « aucune production ne l'appelle » reste documenté à l'inventaire | `PopupManagerTest.laPoseSeBorneAuxMurs` | `38d315c` |
| P4 — six copies de l'utilitaire de mesure | `taille(genre)` : LA mesure unique, les ancres de l'hôte y anticipent la pose ; les six `taille()` privés de la démo meurent | `PopupManagerTest.laMesureDuGenreSeRend` (la carte ENTIÈRE, respirations comprises) | `38d315c` |
| P5 — aucune navigation clavier | la DÉLÉGATION : crochet public `EditorView.setSurTouchePopup` consulté en tête de `onKeyDown`, contrat `PopupClavier` (`surFleche`, `surValidation`) que les cartes interactives implémentent, le manager route (complétion → intentions → débordements) — PIEGE-27 préservé : le focus ne quitte JAMAIS l'éditeur | `TouchePopupVueTest` (2), `PopupManagerTest.leClavierDelegueSertLaCompletion`, `.leClavierDelegueSertLeMenu`, `.lechappementRefermeLesBulles` ; la spécification D-10 rejouée par `BancSpecsTest` | `38d315c`, `c400665` |
| P6 — listeners jamais retirés, course du retrait différé | `detacher()` du manager au cycle de l'activité ; le listener global de la toolbar devient retirable (`removeOnGlobalLayoutListener`) ; la course corrigée : une carte re-prise ne se retire jamais d'elle-même | `PopupLayerReposeTest.reposerLaMemeVueNeLaTuePas`, `PopupManagerTest.leDetachementCoupeTout` | `b6fa94b`, `38d315c` |
| P7 — écarts documentation / implémentation | la javadoc et l'USAGE disent SEIZE genres (le « dix popups » mentait) ; le color picker ferme ENFIN au tap hors cartes (l'annulation promise tenue) | `PopupControllerTest.tapOutsideClosesActionCardsAndBubblesOnly`, `PopupManagerTest.leTapHorsCartesRefermeLesGenresQuiCedent` | `38d315c` |
| P8 — feuilles non coordonnées à la couche | `fermerSousVoile` : ce qu'une feuille modale coupe (l'intelligence et les actions) se ferme d'un appel, le chrome survit ; la démo y branche toute action de menu | `PopupManagerTest.laFeuilleCoupeLintelligencePasLeChrome` | `38d315c` |
| P9 — tracé « verre » dupliqué cartes/feuilles | conservé — assumé : deux mondes de surfaces (cartes éphémères de la couche, feuilles modales posées sur la scène), l'unification visait les CARTES ; l'écart mineur vit à l'inventaire | — (choix documenté) | — |
| P10 — vocabulaire d'ancres croisé entre modules | conservé — la séparation rects (android-view) / stratégies (android-widgets) est assumée et documentée des deux côtés ; l'USAGE §9 la dit à l'intégrateur | — (choix documenté) | — |

## 3. La migration composant par composant

### 3.1 Le kit (`android-widgets`)

| Composant | Ce qui a changé |
| --- | --- |
| `PopupController` | + le genre `TOOLBAR_OVERFLOW` (16 genres) ; + `ouvrirRendus`/`closeRendus` (l'ensemble des affectés se rend — les exclusions et cascades ne se codent plus deux fois) ; + `surToucheHorsCartes` (le régime du tap extérieur) ; `onKeystroke`/`onScroll`/`closeAll` rendent désormais leurs fermés (`Set<PopupKind>`). `open`/`close` inchangés de signature — compatibilité binaire |
| `PopupLayer` | + bornage défensif de `show`/`remonter`/`reposition` aux murs de la couche ; + l'avis « tap hors cartes » (`setSurToucheHorsCartes`) qui NOTIFIE sans consommer ; + la course du retrait différé corrigée (le retrait ne s'exécute que si la carte n'a pas été re-posée entre-temps) |
| `PopupPanel` | + l'annulation de mort à la re-pose (la carte re-prise ne meurt pas de sa mort antérieure) |
| `PopupManager` | NOUVEAU (346 lignes) — l'unique écrivain : `lier` (une vue par genre), `montrer` (les exclus quittent la couche AVANT la pose), `fermer`/`fermerTout` (cascades comprises), `surFrappe`/`surDefilement`, `fermerSousVoile`, `surTouche` (le clavier délégué), `taille` (LA mesure), `detacher` |
| `PopupClavier` | NOUVEAU (32 lignes) — le contrat clavier des cartes interactives : `surFleche(pas)`, `surValidation()` |
| `CompletionListView`, `IntentionsMenuView` | implémentent `PopupClavier` : la surbrillance se déplace aux flèches, Enter/Tab valident — la sélection tactile demeure |
| Bancs | + `PopupManagerTest` (13, Robolectric), + `PopupLayerReposeTest` (2), `PopupControllerTest` passe de 5 à 13 — le socle préalable y gagne 3 tests, le manager 5 |

### 3.2 La vue (`android-view`)

| Composant | Ce qui a changé |
| --- | --- |
| `EditorView` | + le crochet public `setSurTouchePopup` consulté en TÊTE de `onKeyDown` : le verdict consommé rend la main sans bouger le curseur ; la composition IME en cours neutralise la délégation (jamais une frappe composée ne sert une carte) |
| `RappelsHote` | + le porteur du verdict (`poserTouchePopup`/`consommerTouche`) — porteur mince, les états d'exécution restent à l'hôte |
| Bancs | + `TouchePopupVueTest` (2) : le verdict consommé sert le popup sans bouger le curseur ; le verdict décliné laisse la flèche à sa vie normale |

### 3.3 La démonstration (`demo`)

| Composant | Ce qui a changé |
| --- | --- |
| `AtelierPopups` | construit et possède le manager ; ses `fermer`/`retirer`/`taille` privés MEURENT (régression de volume, les duplications passent au kit) ; DEUX cartes de documentation (la complétion et le survol ne partagent plus l'instance) ; `detacher()` au cycle de l'activité |
| `DocAncre` | le survol pose SA carte (genre `HOVER`, la sienne), plus jamais celle de la complétion |
| `MenuDebordement` | la carte de débordement de la barre de sélection vit liée à SON genre |
| `AtelierCouleur` | le sélecteur vit lié (`COLOR_PICKER`), l'ancre prend la mesure du manager, le pick applique implicite et referme |
| `AtelierBarreToolbar` | la carte de débordement gagne le genre `TOOLBAR_OVERFLOW` (elle détournait le menu d'intentions — deux surfaces dans un créneau) ; le listener global devient retirable ; `detacher()` |
| `AtelierBordDroit` | chrome conservé TEL QUEL (assumé — bande, minimap, pouce vivent avec le document, l'audit ne demandait pas de les rentrer) |
| `EditorActivity`, `CatalogueActions` | la délégation clavier branchée (`setSurTouchePopup`) ; toute action de menu referme l'intelligence sous voile (`fermerSousVoile`) avant d'ouvrir sa feuille ; `detacher()` au cycle |
| `BancSpecs` | + la spécification D-10 : le clavier de bout en bout — la frappe ouvre, la liste survit en filtrant, les flèches déplacent, Enter accepte, Escape referme (la cascade comprise) ; `ScenarioCatalog` l'enregistre |

## 4. Les fichiers de la mission

**Créés** — le code :

- `android-widgets/src/main/java/jo/celeris/android/widgets/PopupManager.java`
- `android-widgets/src/main/java/jo/celeris/android/widgets/PopupClavier.java`
- `android-widgets/src/test/java/jo/celeris/android/widgets/PopupManagerTest.java`
- `android-widgets/src/test/java/jo/celeris/android/widgets/PopupLayerReposeTest.java`
- `android-view/src/test/java/jo/celeris/android/view/TouchePopupVueTest.java`

**Créés** — les documents (tous dans `docs/`) : `POPUP_SYSTEM_AUDIT.md`,
`POPUP_SYSTEM_ARCHITECTURE.md`, `POPUP_INVENTORY.md`,
`POPUP_MIGRATION_REPORT.md` (le présent), `POPUP_TEST_REPORT.md`,
`POPUP_DEVELOPER_GUIDE.md`, `feuille-de-route-popups.md` (la feuille de
route de la mission). L'USAGE §9 se réécrit au montage en trois temps.

**Modifiés** — le code : `PopupController`, `PopupLayer`, `PopupPanel`,
`CompletionListView`, `IntentionsMenuView` (android-widgets) ;
`EditorView`, `RappelsHote` (android-view) ; `AtelierPopups`,
`AtelierBarreToolbar`, `AtelierBordDroit`, `AtelierCouleur`,
`CatalogueActions`, `DocAncre`, `EditorActivity`, `MenuDebordement`,
`BancSpecs`, `ScenarioCatalog` (demo). **Modifiés** — les documents :
`USAGE.md` (§9), `CHANGELOG.md`, `docs/parite/MATRICE-PARITE.md` (D-10
relue), `docs/feuille-de-route-restante.md` (l'étape de la mission).

**Supprimés** — AUCUN fichier. Les seules morts sont des MÉTHODES
privées de la démo (les `fermer`/`retirer`/`taille` locaux, les doubles
cascades) : leurs responsabilités n'ont pas disparu, elles ont changé
d'étage. Le mandat l'exigeait (« ne supprime jamais une classe
simplement parce qu'elle ne correspond pas à l'architecture ») et la
vérification des références n'a trouvé AUCUNE classe orpheline : le
code mort constaté (l'ancre `rightEdge`, la bulle de survol
stationnaire déjà retirée avant la mission) est bancé ou documenté.

**Dépendances nouvelles** — AUCUNE. Aucune version ne bouge dans
`gradle/libs.versions.toml` entre l'état audité et l'état livré : la
migration n'a ajouté que du code du kit et de la démo, aucun artefact
externe (le mandat §11-4 : « ne crée pas de dépendances inutiles »).

**La suite du signal du 2026-11-13 — les fichiers des deux premières surfaces réveillées** (le pas étendu PAGE_UP/PAGE_DOWN, la carte des usages : voir `feuille-de-route-popups.md` §2, le journal) :

*Créés*
- `android-widgets/src/main/java/jo/celeris/android/widgets/ReferencesView.java`
- `demo/src/main/java/jo/celeris/demo/AtelierUsages.java`
- `android-widgets/src/test/java/jo/celeris/android/widgets/ReferencesViewTest.java`
- `demo/src/test/java/jo/celeris/demo/UsagesReferencesTest.java`

*Modifiés* : `PopupController` (le genre 17e `REFERENCES` et ses régimes — le manager route ses pages et son ordre), `PopupManager`,
`PopupClavier` (`surPage`), `CompletionListView`, `IntentionsMenuView` (android-widgets) ; `AtelierPopups` (câblage du satellite — nominé au plafond relevé
par la décision du propriétaire, justification au `config/checkstyle/suppressions.xml`),
`CatalogueActions` (l'action `action_usages` aux trois peaux), les ressources de la démo
(`values/strings.xml`, `values-fr/strings.xml` — parité vérifiée, `values/ids.xml`, `menu/menu_editeur.xml`) ; les bancs
`PopupManagerTest`, `PopupControllerTest` ; les documents vivants (`USAGE.md` §9, `CHANGELOG.md`, `POPUP_INVENTORY.md`,
`POPUP_SYSTEM_ARCHITECTURE.md`, `POPUP_TEST_REPORT.md`, `POPUP_DEVELOPER_GUIDE.md`, les deux feuilles de route).

*Supprimés* : aucun. *Dépendances nouvelles* : aucune — la règle du chantier tient encore.

## 5. Fonctionnalités terminées / manquantes

**Terminées par le chantier** (le socle, au-delà de l'existant) :

1. la coordination centralisée état+vues (`PopupManager`) ;
2. la navigation clavier des cartes interactives — flèches, Enter, Tab,
   Escape — par délégation sans vol de focus ;
3. la fermeture au tap hors cartes (les genres qui cèdent) ;
4. le bornage défensif des poses aux murs ;
5. la coordination feuilles modales ⇄ couche (le voile coupe
   l'intelligence, épargne le chrome) ;
6. le genre propre du débordement de toolbar (deux menus ne tiennent
   jamais) ;
7. le détachement de fin de vie (les listeners se retirent, le manager
   se dissout) ;
8. la mesure unique aux ancres de l'hôte.

**L'existant conservé sans perte** (non-régression vérifiée, la liste
de l'inventaire §3) : complétion + doc + signature + intentions +
survol + peek + info-bulle + fil d'Ariane + barre de sélection +
débordements + color picker + touchbar + bande + minimap + pouce +
plan + recherche + réglages + renommage + diff + correctifs +
IA/agent + `@Preview` — le tout tactile, au clavier près des cartes
interactives.

**Manquantes** (dormantes sur signal, la feuille de route de la
mission §3 les tient) : PAGE_UP/PAGE_DOWN ; les surfaces usages et
références, hiérarchies, valeurs de débogage ; notifications ;
recherche de symbole/fichier/classe ; documentation externe ; info de
type ; import resolution ; suppress inspection. Rien de tout cela ne
manque au SOCLE : chacune reprendra le manager, les ancres et le
contrat clavier sans y rien ajouter (c'est le critère
d'extensibilité du mandat, tenu par construction).

## 6. Ce que la migration n'a PAS touché — et pourquoi

- la loupe (`Loupe`/`MagnifierGeometry`) et le fantôme IA
  (`FantomePainter`) : choix de conception documentés (bulle et inlay
  IN-CANVAS) — pas des cartes éphémères ;
- le HUD de diagnostic (`DiagnosticOverlay`) : TextView sur la scène,
  cyclique — hors périmètre carte ;
- le chrome du bord droit (`ErrorBandView`, `MinimapView`,
  `FastScrollerView`) : il vit avec le document, l'ADR-002 l'assume ;
- les feuilles modales (`Feuille`, `FeuilleBas`, `FeuilleHaut`) : elles
  PRENNENT le focus par conception (ADR-018) — la migration les a
  COORDONNÉES (le voile) sans les rentrer dans la couche ;
- les moteurs (LSP, syntaxiques, SPI) : aucun contrat n'a bougé — le
  mandat §11-7 (« ne remplace pas le moteur LSP ou syntaxique
  existant »).

## 7. Limitations rencontrées (honnêteté du mandat)

1. La navigation clavier traverse la composition IME par construction :
   une frappe composée ne sert jamais une carte (le crochet rend la
   touche à la vie normale).
2. PAGE_UP/PAGE_DOWN restent à venir (plafond de huit candidats
   visibles dans la liste) ; la signature aide reste passive (le
   stepper tactile y sert) — choix tactile assumé.
3. Une activité = un document dans la démonstration : le changement de
   fichier n'y est pas démontré (le `closeAll`/`fermerTout` existe et
   se banc, la démo ne change simplement jamais de fichier).
4. Le peek de définition vit sur le plan local (`ApercuDefinition`) :
   le serveur Java réel ne sert pas `definition` (audit §5) — aucune
   fausse intégration déclarée.
5. Les correctifs vivent sur le serveur de référence de la démo — même
   clause d'honnêteté.
6. La protection contre les réponses asynchrones concurrentes au sens
   strict (jeton de génération : deux réponses LSP qui reviennent en
   désordre) demeure un mécanisme D'HÔTE : le kit garantit la vue ne
   survit jamais à son état et la re-pose idempotente ; l'hôte qui
   veut trier les réponses garde son jeton chez lui (la démo pose la
   dernière réponse reçue sur le fil UI — `whenComplete` + `post`).
