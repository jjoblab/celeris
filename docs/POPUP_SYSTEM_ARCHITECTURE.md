# Architecture unifiée du système de popups de Celeris

> Architecture cible (Phase 2 de la mission), déduite de l'audit
> (`POPUP_SYSTEM_AUDIT.md`) et du dépôt réel. Principe directeur (contrainte
> 15 de la mission) : chaque nouvelle abstraction répond à un déficit
> CONSTATÉ ; rien n'est créé pour l'élégance théorique. La terminologie
> « PopupManager / PopupController / PopupPositioner / PopupRenderer /
> PopupFocusManager / PopupLifecycleManager » de la mission est couverte par
> les composants suivants, dont plusieurs EXISTENT DÉJÀ.

## 1. Vue d'ensemble

```
            hôte (activité / application)
   déclencheurs, contenu, feuilles modales (ADR-018)
        │  montrer(kind, x, y) / fermer(kind) / surTouche(keyCode)
        ▼
┌─────────────────────────────────────────────────────┐
│ PopupManager  (NOUVEAU — l'unique writer)           │
│  • lie une vue par genre (jamais partagée)           │
│  • applique les régimes ET les cascades D'UN SEUL   │
│    TENANT : état (controller) et vues (layer) ne    │
│    peuvent plus diverger                            │
│  • route le clavier délégué vers la carte active    │
│  • ferme au clic extérieur selon la politique       │
│  • mesure unique (taille du genre)                  │
├───────────────┬─────────────────────────────────────┤
│ PopupController│ PopupLayer        (EXISTANTS)      │
│ état pur 15+1  │ portage, voile, PIEGE-27           │
│ genres, régimes│ + bornage des poses (RENFORCÉ)     │
│ + rendus       │ + avis « tap hors cartes » (NEW)  │
│ d'ouvertures/  │ + course du retrait différé        │
│ fermetures (NEW│   corrigée                          │
├───────────────┴─────────────────────────────────────┤
│ PopupPlacer / PopupAnchor  (EXISTANTS) — position   │
│ PopupPanel / TextRowsPopup / PopupPalette / Tokens   │
│ (EXISTANTS) — rendu commun « verre »                 │
│ PopupClavier (NOUVEAU, contrat) — navigation des    │
│ cartes interactives                                 │
└─────────────────────────────────────────────────────┘
        ▲ géométrie        ▲ données
  android-view        core-lsp / core-intelligence /
  (caretBounds,        lsp-protocole / android-host
   selectionBounds,    (aucune dépendance des popups
   ViewportWatch…)      vers l'éditeur — contrats purs)
```

La frontière demeure : **la vue calcule, la couche porte, le manager
décide, les moteurs servent**. Les dépendances ne changent pas :
`android-widgets` ne dépend ni d'`android-view` ni des moteurs ; l'éditeur
ne dépend pas des widgets.

## 2. Correspondance avec les composants de la mission

| Composant de la mission | Réalisation dans Celeris | Statut |
| --- | --- | --- |
| PopupManager (coordination) | `PopupManager` : unique writer de l'état ET des vues | **nouveau** |
| PopupController (comportement individuel) | `PopupController` (régimes, exclusions, cascades) + comportement propre dans chaque vue du kit | existe, étendu |
| PopupPositioner | `PopupPlacer` (trio coordonné) + `PopupAnchor` (ancres bornées) + bornage défensif dans `PopupLayer` | existe, renforcé |
| PopupRenderer | `PopupPanel` / `TextRowsPopup` / `PopupPalette` / `PopupTokens` | existe |
| PopupFocusManager | PIEGE-27 par construction dans `PopupLayer` ; clavier par DÉLÉGATION (`EditorView.setSurTouchePopup` + `PopupManager.surTouche`) | le focus existait ; la délégation est **nouvelle** |
| PopupLifecycleManager | Régimes du contrôleur appliqués par le manager (`surFrappe`, `surDefilement`, `fermerTout`, `detacher`) + correction de la course du retrait différé | consolidé |

## 3. Le contrat commun (le « modèle » de la mission)

Le contrat commun d'un popup est déjà porté par l'énumération
`PopupController.PopupKind` (l'identifiant et la catégorie), les régimes du
contrôleur (les règles de coexistence et de fermeture) et la vue liée (le
comportement). Plutôt qu'un registre parallèle d'objets « PopupSpec »
(duplication de la source de vérité, rejetée), le contrat est ÉTENDU là où
l'audit a constaté le déficit :

1. **Genre 16e : `TOOLBAR_OVERFLOW`** — la carte de débordement de la barre
   d'application vit son PROPRE genre (elle détournait `INTENTIONS_MENU`,
   deux surfaces dans un créneau) : deux menus ne tiennent jamais (parité
   avec le comportement d'aujourd'hui), la frappe la ferme, le défilement
   l'épargne (ancre au coin de scène, pas au texte).
2. **Rendus d'ouvertures/fermetures** — `ouvrirRendus(kind)` et
   `closeRendus(kind)` rendent l'ENSEMBLE des genres affectés (exclus,
   cascadés), pour que le manager synchronise couche et état d'un seul
   tenant ; `open`/`close` historiques conservés (compatibilité binaire).
3. **Navigation clavier des cartes** — le contrat `PopupClavier`
   (`surFleche(pas)`, `surValidation()`) que les cartes interactives
   implémentent (complétion, menu d'intentions) ; le manager route les
   touches déléguées.
4. **Politique du clic extérieur** — `surToucheHorsCartes()` du contrôleur :
   les genres qui cèdent au tap hors cartes (survol, info-bulle, sélecteur
   de couleur, menus d'actions), distincts de ceux qui suivent le curseur
   (complétion, signature, fil d'Ariane) et du chrome (bande, minimap,
   pouce, touchbar).

Le « niveau de priorité » de la mission se lit : exclusions binaires du
contrôleur (complétion ⇄ intentions ⇄ débordement toolbar ; doc sans liste
interdite ; débordement sans barre interdit) + l'ORDRE de routage clavier du
manager (complétion > intentions > débordements). Les groupes de la
mission (EDITOR_INTELLIGENCE / EDITOR_INFORMATION / EDITOR_ACTIONS /
NAVIGATION / DEBUGGING / PROJECT / GLOBAL) sont la GRILLE DE CLASSIFICATION
documentée (POPUP_INVENTORY §2), pas une couche de code supplémentaire :
à ce jour, tous les genres vivent dans l'orbite éditeur, aucune interaction
entre orbites n'existe à fiabiliser.

## 4. PopupManager — l'unique writer

```java
public final class PopupManager {
    public PopupManager(PopupController controleur, PopupLayer couche);
    public void lier(PopupController.PopupKind genre, View vue); // une vue par genre, un genre par vue
    public View vue(PopupController.PopupKind genre);
    public PopupPlacer.Cadre taille(PopupController.PopupKind genre); // LA mesure
    public boolean montrer(PopupController.PopupKind genre, int x, int y);
    public boolean fermer(PopupController.PopupKind genre);
    public Set<PopupController.PopupKind> fermerTout();
    public Set<PopupController.PopupKind> surFrappe();     // onKeystroke + retraits
    public Set<PopupController.PopupKind> surDefilement(); // onScroll + retraits
    public Set<PopupController.PopupKind> fermerSousVoile(); // feuille modale qui s'ouvre
    public boolean surTouche(int keyCode); // clavier délégué (flèches/Enter/Tab/Escape)
    public boolean estOuvert(PopupController.PopupKind genre);
    public PopupController controleur();
    public PopupLayer couche();
}
```

Invariants GARANTIS par le manager (les zombies d'état deviennent
impossibles) :

- `montrer` : l'état s'ouvre (exclusions rendues), les genres exclus voient
  leurs vues retirées, puis la vue liée se pose. Toute divergence
  état/couche disparaît — les exclusions ne sont plus codées deux fois.
- `fermer` : les cascades (`closeRendus` : doc avec liste, débordement avec
  barre) ferment l'ÉTAT et RETIRENT les vues d'un seul appel.
- `lier` refuse qu'une même vue serve deux genres (l'anti-pattern constaté
  P1 devient une erreur explicite dès le câblage, plus au runtime).
- `surTouche` consulte la première carte interactive ouverte dans l'ordre
  complétion → intentions → débordements ; ESCAPE ferme (cascades
  comprises), Enter/Tab valident, les flèches déplacent la surbrillance.
  Les touches non consommées retournent faux — l'éditeur suit sa vie
  normale (PIEGE-27 préservé : le focus ne quitte JAMAIS l'éditeur).
- `fermerSousVoile` : ce qu'une feuille modale coupe (intelligence et
  actions : complétion, doc, signature, survol, info-bulle, sélecteur,
  menus, barre), le chrome survit (fil d'Ariane, bande, minimap, pouce,
  touchbar).

L'hôte (démo ou tiers) netouche plus JAMAIS `couche.dismiss` ni les
doubles cascades : le montage complet tient en trois temps (USAGE §9) —
lier les genres, déclencher, déléguer le clavier.

## 5. Renforcements de la couche (PopupLayer)

1. **Bornage défensif** : `show`/`remonter`/`reposition` bornent la pose
   aux murs de la couche (défense en profondeur ; sans effet sur les poses
   légitimes déjà bornées par les ancres).
2. **Avis « tap hors cartes »** : un `ACTION_DOWN` hors de toute carte
   notifie le manager SANS consommer — le geste poursuit sa vie vers
   l'éditeur (le caret bouge, la barre suit).
3. **Course du retrait différé corrigée** : le retrait ne s'exécute que si
   la vue n'a pas été re-posée entre-temps (une carte re-prise ne se
   retire jamais d'elle-même).

## 6. Clavier (phase 5 de la mission) — la délégation

PIEGE-27 interdit la capture du focus ; la navigation se fait donc par
DÉLÉGATION depuis l'éditeur qui garde le focus :

```
clavier matériel/Robolectric → EditorView.onKeyDown
  → RappelsHote.consommerTouche(composition, keyCode)  [crochet public setSurTouchePopup]
     → PopupManager.surTouche(keyCode)
        → carte PopupClavier de la complétion / du menu
```

Politique (celle d'IntelliJ/Android Studio, adaptée au tactile) :
UP/DOWN déplacent la surbrillance ; ENTER et TAB valident (la complétion
ouverte PRÉCÈDE le fantôme IA sur Tab — la carte visible gagne) ; ESCAPE
referme (la complétion emporte sa documentation) ; LEFT/RIGHT et toute
autre touche retombent dans la vie normale de l'éditeur ; la composition
IME en cours neutralise la délégation. PAGE_UP/PAGE_DOWN restent tactiles
et à venir (plafond de 8 candidats visibles). La signature aide reste
passive (elle suit le curseur, elle ne prend pas les flèches — le stepper
tactile y sert).

## 7. Migration (phase 7) — résumé du plan

Aucune classe réécrite ; les modifications sont chirurgical :

| Composant | Changement |
| --- | --- |
| `PopupController` | + `TOOLBAR_OVERFLOW`, + `ouvrirRendus`/`closeRendus`, + régime « hors cartes » ; `open`/`close` inchangés (compatibilité) |
| `PopupLayer` | bornage, avis hors-cartes, course corrigée |
| `PopupClavier` | nouveau contrat (2 méthodes) |
| `CompletionListView`, `IntentionsMenuView` | implémentent `PopupClavier` (surbrillance + validation par le clavier) |
| `PopupManager` | nouveau (kit) + banc Robolectric dédié |
| `EditorView` + `RappelsHote` | crochet public `setSurTouchePopup` consulté en tête de `onKeyDown` |
| démo `AtelierPopups` | construit/possède le manager ; les `fermer`/`retirer`/`taille` privés meurent (régression de volume) ; une DEUXIÈME `CompletionDocView` pour le survol (plus de carte partagée) ; `detacher()` au cycle |
| `DocAncre`, `MenuDebordement`, `AtelierCouleur`, `AtelierBarreToolbar` | vue PROPRE par genre, câblage par le manager ; genre `TOOLBAR_OVERFLOW` pour la carte toolbar ; listener du layout global retirable |
| `EditorActivity`, `CatalogueActions` | délégation clavier branchée ; toute action de menu referme l'intelligence sous voile |
| `AtelierBordDroit` | chrome conservé tel quel (assumé) |

Règle de sécurité de la mission respectée : aucune suppression de classe ;
les API publiques historiques (`open`, `close`, `show`, `dismiss`…)
demeurent ; les satellites rétrécissent parce que le manager absorbe leurs
duplications, pas parce qu'on leur a coupé des responsabilités utiles.

## 8. Tests (phase 8)

- `PopupManagerTest` (Robolectric) : liaisons (vue partagée refusée),
  montrer/fermer avec cascades — l'état ET la couche restent isochrones,
  régimes frappe/défilement, tap hors cartes, clavier (flèches/Enter/
  Escape/Tab), course du retrait différé, bornage.
- `PopupControllerTest` étendu : `TOOLBAR_OVERFLOW`, rendus d'ouvertures/
  fermetures, cohérence `open`/`ouvrirRendus`.
- `BancSpecs` (banc de spécification de la démo) : scénario clavier de bout
  en bout — frappe ouvre, flèches déplacent, Enter accepte, Escape referme.
- Non-régression : les 28 bancs widgets existants + les bancs vue + les
  bancs démo doivent rester verts SANS modification de leurs attentes
  (sauf ceux qui figent le partage de cartes — remplacés par l'attente
  nouvelle : DEUX cartes distinctes).

## 9. Décisions EXPLICITEMENT rejetées (contrainte 15)

1. Un `PopupSpec`/registre d'objets par popup : la source de vérité des
   régimes vit dans le contrôleur ; un registre parallèle la dupliquerait.
2. Six classes manager séparées : le contrôleur, la couche et le manager
   couvrent déjà les responsabilités sans chevauchement.
3. Rentrer loupe/fantôme/HUD/bord droit dans la couche : ce sont des
   choix de conception documentés (canvas, chrome) — l'unification de la
   mission vise les CARTES éphémères, pas le chrome permanent.
4. Une file de priorités générique à N niveaux : les exclusions binaires
   du contrôleur couvrent TOUS les conflits réellement observables du
   dépôt ; l'ordre de routage clavier fixe la priorité interactive.
5. Remplacer le trio LSP / moteurs / SPI par un « service unique » : les
   contrats existants sont purs et consommés ; les popups ne doivent pas
   connaître les moteurs (mission §6).
