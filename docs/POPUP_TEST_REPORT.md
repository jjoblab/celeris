# Rapport de tests du système de popups de Celeris

> La phase 8 de la mission — « tests et validation » — se rapporte ici :
> les bancs du chantier, la non-régression, le journal des échecs
> tranchés, le verdict de la passe de référence. État vérifié au
> 2026-11-13 — la passe se rafraîchit AU SIGNAL du propriétaire du même jour (2 393 tests : le pas étendu, la carte des usages). Le mandat est formel : ne pas masquer un échec, ne pas
> désactiver un banc pour verdir — les quatre échecs rencontrés en route
> se tranchent ci-dessous, banc après banc, au verdict près.

## 1. Le verdict de la passe de référence

La passe du 2026-11-13 (le miroir hors ligne du bac à sable, les quatorze
modules compris) :

```bash
./gradlew --offline test --console=plain   # la suite de référence
./gradlew --offline check --console=plain  # les portes qualité entières
```

| Mesure | Verdict |
| --- | --- |
| Tests exécutés (14 modules) | **2 393 — 0 échec, 0 écart** (3 ignorés : les bancs natifs qui sautent sans la bibliothèque JNI construite, ADR-020 — hors de la mission popups) |
| Compilation (sources principales et bancs, 14 modules) | verte |
| Politique de langue (identifiants ASCII, typographie française des Markdown, Javadoc des classes publiques, parité des chaînes) | verte |
| Checkstyle (plafonds 600/800 lignes, 50 par méthode, 120 colonnes) | vert |
| PMD (complexité cyclomatique 15) | vert |
| Error Prone + NullAway (`@NullMarked` JSpecify) | vert |
| Couverture JaCoCo (≥ 90 % de lignes, modules purs) | verte |
| Mutation PIT (≥ 80 %, `core-text`) | verte |
| Manifestes d'API (les neuf modules purs) | à jour — aucune évolution d'API des modules purs |
| `git status` | propre |

Aucun banc désactivé, aucun seuil affaibli, aucun échec masqué : les
quatre échecs rencontrés pendant la rédaction des bancs nouveaux se sont
tranchés à l'analyse (§4), jamais à la suppression.

## 2. Les bancs du chantier

### 2.1 `PopupManagerTest` — le coordinateur (Robolectric, 15 — dont 2 nés au signal du 2026-11-13)

| Banc | Ce qu'il prouve |
| --- | --- |
| `laCartePartageeSeRefuse` | une même carte ne sert jamais deux genres — le refus nomme le genre fautif (P1) |
| `leGenreRefuseNeSeMontrePas` | la documentation sans sa liste, le débordement sans sa barre : l'état et la couche ne divergent jamais |
| `laFrappeRetireLesCartes` | les régimes ferment l'état ET retirent les vues d'un seul tenant (P2) |
| `leTapHorsCartesRefermeLesGenresQuiCedent` | le tap extérieur referme les genres qui cèdent — la complétion suit le curseur, le chrome vit (P7) |
| `laPoseSeBorneAuxMurs` | toute pose se borne aux murs de la couche, largeur ENTIÈRE comprise (P3) |
| `leClavierDelegueSertLaCompletion` | les flèches et Enter servent la carte interactive ouverte — PIEGE-27 (P5) |
| `leClavierDelegueSertLeMenu` | le menu d'intentions reçoit la même délégation |
| `lechappementRefermeLesBulles` | Escape referme la première carte interactive, sinon les bulles éphémères — jamais le chrome |
| `laFeuilleCoupeLintelligencePasLeChrome` | le voile coupe l'intelligence et les actions, le chrome survit (P8) |
| `laMesureDuGenreSeRend` | LA mesure unique rend la carte ENTIÈRE — contenu + respirations (P4) |
| `leDetachementCoupeTout` | la fin de vie ferme tout et coupe l'avis du tap (P6) |
| `fermerLaListeEmporteSaDocumentation` | la cascade se joue d'un seul appel : fermer la liste retire AUSSI la carte de sa documentation — l'état et la couche isochrones |
| `louvertureQuiExclutRetireLaCarteDeLexclu` | ouvrir un menu retire la carte de l'exclu AVANT la pose — les exclusions ne se codent plus deux fois |
| `laPageEtendueSertLaCompletion` | PAGE_UP/PAGE_DOWN sautent la surbrillance d'une page de huit candidats (bornée aux murs de la liste) ; le menu décline, la touche retourne à l'éditeur — le pas étendu du signal |
| `leClavierDelegueSertLesUsages` | la carte des usages reçoit la délégation (flèches, Enter — la page décline) ; son ouverture chasse le menu du caret, sa carte quitte la couche |

### 2.2 `PopupControllerTest` — la machine (15 — dont 3 nés à `b6fa94b`, 5 à `38d315c` et 2 au signal du 2026-11-13)

Les 5 historiques (exclusions, cascades, régimes) restent VERTS sans
modification de leurs attentes ; les bancs nouveaux couvrent : les rendus
des régimes (`keystrokeReturnsTheClosedGenres`,
`scrollReturnsTheClosedGenres`, `closeAllReturnsEveryOpenGenre`), le
genre `TOOLBAR_OVERFLOW` (`toolbarOverflowExcludesTheMenus`,
`toolbarOverflowYieldsToKeystrokeNotToScroll`), les rendus
d'ouvertures/fermetures (`openRendusCarryTheExcludedGenres`,
`closeRendusCarryTheCascadedGenres`) et le régime du tap hors cartes
(`tapOutsideClosesActionCardsAndBubblesOnly`). Le signal du 2026-11-13 ajoute les régimes de la carte des usages (`referencesExcludeTheMenusAndReciprocally` — deux surfaces d'actions ne tiennent jamais, exclusions réciproques avec les menus ;
`referencesYieldToKeystrokeScrollAndOutsideTap` — frappe, défilement, tap hors cartes).

### 2.3 Les autres bancs nés du chantier

| Banc | Ce qu'il prouve |
| --- | --- |
| `PopupLayerReposeTest` (2) | le re-pose de la même vue ne la tue pas (la course du retrait différé corrigée, P6) ; l'invariant une-vue-un-genre de la couche |
| `TouchePopupVueTest` (android-view, 2) | le verdict consommé sert le popup SANS bouger le curseur ; le verdict décliné laisse la flèche à sa vie normale — la délégation de tête de `onKeyDown` |
| `ReferencesViewTest` (3 — le signal du 2026-11-13) | la carte des usages : l'en-tête annonce et ne se prend jamais, l'occurrence se prend au doigt (le relâchement choisit), la flèche se borne aux occurrences, la validation exécute le saut, la page décline, la carte vide décline tout |
| `UsagesReferencesTest` (demo, 2 — le signal du 2026-11-13) | les lieux du MOT ENTIER servis par le fil (`references`) ; la transpose de l'atelier — le début de plage devient l'offset du saut, le curseur porte au point d'usage |
| `BancSpecsTest` (demo) — spécification D-10 | le clavier de BOUT EN BOUT : la frappe ouvre (le prédicat du coach, les mots du code), la liste survit à la frappe suivante (elle filtre — le régime rend la liste vivante), les flèches déplacent, Enter accepte (le candidat remplace le mot au curseur, sa documentation suit), Escape referme (la cascade comprise) |

### 2.4 La non-régression

Les bancs du kit existants (27 classes de banc à l'état audité — 28
aujourd'hui, `PopupManagerTest` naît) et les bancs de la vue et de la
démonstration restent verts SANS modification de leurs attentes : les
28 surfaces fonctionnelles de l'inventaire §3 (complétion, doc,
signature, intentions, survol, peek, info-bulle, fil d'Ariane, barre de
sélection, débordements, color picker, touchbar, bande, minimap, pouce,
plan, recherche, réglages, renommage, diff, correctifs, IA/agent,
`@Preview`) ne perdent rien. Le banc d'appareil
`CompletionPopupOnDeviceTest` (second étage de l'appareil de
vérification, `demo/src/androidTest`) continue de prouver la complétion
sur vitre — l'atelier migré n'y change rien de visible.

## 3. Ce que les bancs ne couvrent pas (encore) — assumé

1. Le changement de fichier : la démonstration vit une activité = un
   document ; `fermerTout` se banc, la scène ne le démontre pas.
2. Le jeton de génération des réponses concurrentes : mécanisme d'hôte
   (limitation consignée au rapport de migration §7) — la démo pose la
   dernière réponse sur le fil UI.
3. La vitre elle-même : le banc d'appareil couvre la complétion et le
   focus (PIEGE-27 sur matériel) ; la navigation clavier déléguée n'a
   pas encore son banc d'appareil — le clavier matériel se prouve au
   JVM (`TouchePopupVueTest`), l'appareil suivra au fil du second étage.

## 4. Le journal des échecs tranchés (la mise au vert des bancs nouveaux)

Quatre échecs ont précédé la passe verte — chacun tranché au verdict,
jamais masqué :

| Échec | Diagnostic | Verdict |
| --- | --- | --- |
| `PopupManagerTest.laCartePartageeSeRefuse` | le message de refus nommait le genre DÉTENTEUR, pas le genre FAUTIF — la phrase accusait le mauvais | **implémentation corrigée** : `lier` nomme désormais le genre fautif (celui qui veut lier) et le détenteur |
| `PopupControllerTest.tapOutsideClosesActionCardsAndBubblesOnly` | l'état d'origine du banc était IMPOSSIBLE (complétion et menu d'intentions ensemble — l'exclusion binaire l'interdit depuis toujours) | **banc restructuré en deux phases** : les menus et le sélecteur ouverts (phase 1, refermés au tap), puis la complétion et sa documentation (phase 2, épargnées au tap) — toutes les assertions d'origine conservées |
| `PopupManagerTest.laMesureDuGenreSeRend` | le banc attendait 100 (le contenu seul), la mesure rend 124 (la carte entière — respirations comprises) | **banc corrigé** : la sémantique établie depuis la première livraison est la carte ENTIÈRE (l'ancre de l'hôte y anticipe la pose bornée) — `git show` de l'historique la confirme |
| `PopupManagerTest.laPoseSeBorneAuxMurs` | le banc attendait la pose au bord du CONTENU, le bornage s'applique à la largeur ENTIÈRE | **banc corrigé** : le mur droit reçoit le bord entier de la carte (cohérent avec la mesure entière du banc précédent) |

Le tranchage s'est fait par la sémantique HISTORIQUE du dépôt (lecture
des versions antérieures du kit par `git show HEAD:<fichier>`) : quand
le banc contredit une sémantique établie et testée ailleurs, c'est le
banc qui cède ; quand le banc révèle un vrai défaut de phrase
(d'accuser le détenteur), c'est l'implémentation qui cède. Le mandat
(« tout correctif de bug arrive avec un test qui échoue avant et passe
après ») est tenu dans les deux sens.

## 5. La méthode des bancs (pour mémoire)

- Un banc par invariant, la langue des identifiants anglais-ASCII, la
  Javadoc et les commentaires français (politique de langue, porte
  bloquante).
- Robolectric pour tout ce qui touche la vue (`ADR-013` : banc de vue
  JVM) ; les assertions d'état pur (`PopupControllerTest`) sans SDK.
- Le banc de spécification de la démo (`BancSpecs`/`BancSpecsTest`)
  rejoue chaque chantier livré sur les mêmes appareils — la
  spécification D-10 y vit.
- Les cliquets restent intacts : couverture ≥ 90 % (modules purs),
  mutation ≥ 80 % (`core-text`), plafonds Checkstyle/PMD, manifestes
  d'API — aucune exemption demandée par le chantier.
