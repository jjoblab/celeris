# Audit du système de popups de Celeris

> Audit de l'existant (Phase 1 de la mission « Audit, restructuration et
> unification du popup system »). Lecture du dépôt au commit de départ
> `9c23084`. Chaque constat est sourcé par le fichier réel ; rien n'est
> présupposé. Les corrections appartiennent aux phases suivantes — ce
> document ne décrit que l'état constaté.

## 1. Périmètre et méthode

L'audit a couvert : la structure complète des modules (§2), le module
porte-popups `android-widgets` (§3), les composants qui se comportent comme
des popups hors de ce module (§4), les moteurs de données purs qui alimentent
les popups (§5), puis les problèmes architecturaux (§6). L'inventaire
composant par composant vit dans `POPUP_INVENTORY.md` ; le présent document
traite l'architecture.

## 2. Structure réelle du projet

Modules (settings.gradle.kts) : `core-text`, `core-syntax`,
`core-syntax-grammars`, `core-language`, `core-intelligence`, `core-lsp`,
`core-ai`, `lsp-protocole`, `serveur-lsp-java`, `cli` (JVM pure) ;
`android-view`, `android-widgets`, `android-host`, `demo` (Android).
Le cœur ne connaît pas Android (ARCHITECTURE.md) ; les dépendances ne vont
que vers le bas, figées par des tests d'architecture ArchUnit.

Répartition réelle des responsabilités « popups » :

| Module | Rôle popup |
| --- | --- |
| `android-widgets` | le KIT : machine à états (`PopupController`), couche porteuse (`PopupLayer`), carte de base (`PopupPanel`, `TextRowsPopup`), placement (`PopupPlacer`, `PopupAnchor`), thème (`PopupPalette`, `PopupTokens`), vues spécialisées, feuilles modales (`Feuille`, `FeuilleBas`, `FeuilleHaut`). Publié `jo.celeris:android-widgets:1.1.0`. |
| `android-view` | la GÉOMÉTRIE et les DÉCLENCHEURS : rects du caret (`PopupAnchors`, `CaretAnchors`), avis de fenêtre (`ViewportWatch` → `EditorView.setSurViewportChange`), loupe (`Loupe`, `MagnifierGeometry`), pastilles de couleur (`PastillesCouleur`), repères `@Preview` (`ReperesPreview`), concentrateur de complétion (`IntelligenceHub`). AUCUNE dépendance vers `android-widgets`. |
| `demo` | l'ORCHESTRATION : `AtelierPopups` (~600 lignes) et ses satellites (`DocAncre`, `MenuDebordement`, `AtelierCouleur`, `AtelierBordDroit`, `AtelierBarreToolbar`, `AtelierRecherche`), la table d'actions (`CatalogueActions`), le contenu de repli (`PopupCoach`). |
| `core-lsp`, `lsp-protocole`, `serveur-lsp-java` | les DONNÉES LSP : types immuables (`ListeCompletion`, `SurvolLsp`, `SignatureLsp`, `CorrectifLsp`, `LieuLsp`, `SymboleLsp`, `DiagnosticLsp`), la frontière protocolaire (`LspSession`), le serveur Java réel (`JavaLanguageServer`). |
| `core-intelligence` | les MOTEURS hors ligne : `CompletionMots`, `CompletionMatcher`, `IndexSymboles`, `Renommage`, `SafeDelete`, `Gabarits`, `RechercheFloue`, etc. |
| `android-host` | les SPI de l'hôte : `DiagnosticHote`, `GraviteDiagnostic`, `CorrectifsLint`, `ValeurEnLigne`. |

Constat clé : la séparation vue/kit/données est réelle et saine. Le
déficit se concentre dans l'orchestration (§6.1, §6.2).

## 3. Le kit `android-widgets` (ce qui existe déjà)

Le kit est écrit en **Java** (pas de Kotlin), package unique
`jo.celeris.android.widgets`, testé par 28 bancs JUnit 4 + Robolectric
(ADR-013). Il fournit DÉJÀ :

- **`PopupController`** (206 l.) : machine à états pure, sans vue ni SDK,
  15 genres (`PopupKind`), exclusions (complétion ⇄ intentions), cascades
  (fermer la liste ferme sa doc ; fermer la barre ferme son débordement),
  régimes de frappe/défilement (`onKeystroke`, `onScroll`, `closeAll`).
- **`PopupLayer`** (183 l.) : ViewGroup porteur dans la même fenêtre,
  PIEGE-27 par construction (jamais de focus, jamais de `PopupWindow`),
  invariant « une vue ne vit jamais sous deux genres », voile de naissance
  (120 ms) / de mort (100 ms) avec retrait différé, `show` / `reposition` /
  `remonter` / `dismiss`.
- **`PopupPanel`** / **`TextRowsPopup`** : carte de base verre + liseré
  hairline + coins arrondis + clip, palette dérivable, rangées typées.
- **`PopupPlacer`** (placement coordonné complétion/doc/signature) et
  **`PopupAnchor`** (ancres simples bornées) : positionnement borné à
  l'écran, pur et testé.
- **`PopupPalette`** / **`PopupTokens`** : thème dérivé du fond de
  l'éditeur (`verre(...)`, `dark()`, `light()`), jetons dp/sp.
- Douze vues spécialisées sans logique d'ouverture (§4 de l'inventaire) et
  trois feuilles modales (ADR-018) qui prennent le focus par conception.

Constat clé : le kit n'est PAS un chaos de gestionnaires parallèles — il
n'existe qu'UN `PopupController` et qu'UNE couche. Les besoins « Manager /
Controller / Positioner / Renderer / FocusManager / LifecycleManager » de
la mission y sont déjà couverts en partie : état (controller), portage
(layer), rendu (panel/palette/tokens), positionnement (placer/anchor),
focus (PIEGE-27 dans la couche, banc `PopupLayerFocusTest`). Ce qui manque
est la LIAISON entre eux (§6.1) et le clavier (§6.5).

## 4. Composants qui se comportent comme des popups hors du kit

- `Loupe` + `MagnifierGeometry` (`android-view`) : bulle d'agrandissement
  dessinée DANS le canvas de l'éditeur pendant le dragage — choix assumé
  (photographie de frame), hors couche, correctement factorisée.
- `FantomePainter` / `FantomeEditeur` (`android-view`) : texte fantôme IA
  in-canvas — inlay éphémère, pas une carte.
- `DiagnosticOverlay` + `surimpression` (`demo`, `activity_editor.xml`) :
  HUD de diagnostic sur la scène — TextView hors couche, cyclique (400 ms).
- Cartes « pseudo-popup » du kit non portées par `PopupPanel` :
  `ErrorBandView`, `MinimapView`, `FastScrollerView` (extends View brut,
  palette locale) — chrome permanent du bord droit, vivant avec le
  document ; le plafond ADR-002 les assume comme chrome, non comme cartes.

Constat clé : ces cas sont des CHOIX documentés (chrome, canvas, HUD), pas
des doublons accidentels. L'audit ne demande pas de les rentrer de force
dans la couche — la classification de la phase 3 les fixe explicitement
hors du périmètre « carte éphémère ».

## 5. Moteurs de données disponibles (contrats réels)

- **LSP** (`lsp-protocole.LspSession`) : `completion`, `survol`,
  `signature`, `definition`, `references`, `symboles`, `surlignages`,
  `rennomage` (+ préparation), `correctifs`, `formatage` (+ plage),
  `plis`, `plagesSelection`, `jetons` (+ plage), `indices`,
  `diagnosticsDe`, auditeur de diagnostics.
- **Serveur Java réel** (`serveur-lsp-java.JavaLanguageServer`) : sert
  hover, signatureHelp, completion, documentSymbol, foldingRange,
  formatting, diagnostics poussés. NE sert PAS : codeAction, definition,
  references, rename, semanticTokens, inlayHints.
- **Moteurs hors ligne** (`core-intelligence`) : `CompletionMots` +
  `CompletionMatcher`, `IndexSymboles`, `Renommage`, `SafeDelete`,
  `SafeMove`, `Hierarchies`, `ExtractionRessource`, `Gabarits`,
  `SquelettesMethode`, `RechercheFloue`, `Formateur`, `Commenter`,
  `LineActions`, `PostfixCompletion`, `CompletionsXml`.
- **SPI hôte** (`android-host`) : `DiagnosticHote`/`GraviteDiagnostic`,
  `CorrectifsLint`/`AnalyseurLint`, `ValeurEnLigne`,
  `HexadecimalPreview`, `IntegrationDebogueur.valeursEnLigne`.

Attention (capacités réelles vérifiées) : la définition (peek) vit sur le
plan `OutlineEntry` local (`ApercuDefinition`), les correctifs vivent sur
le serveur de référence de la démo — le serveur Java déclaré ne les sert
pas. Aucune intégration « déclarée mais fausse » : les popups ne demandent
que ce que les moteurs servent réellement.

## 6. Problèmes architecturaux constatés

Les problèmes sont classés par gravité décroissante. Les références sont
exactes.

### P1 — Vues partagées entre genres : l'état ment (critique)

`AtelierPopups` (demo) pose la MÊME instance de vue sous deux genres :

- `CompletionDocView docCompletion` sert `COMPLETION_DOC` **et** `HOVER`
  (`AtelierPopups.java:131` → `DocAncre(..., docCompletion, ...)`,
  `DocAncre.poser()` fait `couche.show(HOVER, vue, ...)`).
- `IntentionsMenuView menuIntentions` sert `INTENTIONS_MENU`
  **et** `SELECTION_OVERFLOW` (`MenuDebordement`) **et** la carte de
  débordement de la toolbar (`AtelierBarreToolbar.ouvrirDebordement` —
  même genre `INTENTIONS_MENU` que le menu du caret).

`PopupLayer.show` applique bien son invariant « une vue ne vit jamais sous
deux genres » (`popups.values().removeIf(vue -> vue == popup)`,
`PopupLayer.java:69`) — mais il le fait SANS prévenir le contrôleur :
quand le survol prend la carte, le contrôleur croit toujours
`COMPLETION_DOC` ouvert alors que la couche n'a plus la vue. Symétriquement
la frappe suivante repose la doc sous son genre et vole le `HOVER`. Même
désynchronisation pour la carte d'intentions partagée entre le menu du
caret et le débordement de toolbar (deux usages CONCURRENTS du même genre).
Le banc `PopupLayerReposeTest.laMemeVueNeVitJamaisSousDeuxGenres` démontre
le mécanisme de couche — c'est le niveau orchestration qui crée le bug,
pas la couche.

### P2 — Deux sources de vérité synchronisées à la main (critique)

`PopupController` (l'état) et `PopupLayer` (les vues) sont deux mondes que
chaque hôte doit marier lui-même : le contrat « la vue ne survit jamais à
son état » (`onKeystroke`/`onScroll`/`closeAll` rendent les genres fermés →
l'hôte appelle `couche.dismiss`) est entièrement à la charge de l'hôte.
Les cascades sont codées EN DOUBLE : dans `PopupController.close` (état)
ET dans `AtelierPopups.fermer` (vues, lignes 528–537). Aucun coordinateur
n'est fourni par le kit : `USAGE.md` §9 le dit explicitement, la démo le
prouve par ~600 lignes de câblage (`AtelierPopups`) et six satellites qui
possèdent chacun sa copie du pattern `controleur.open → vue.set… → mesure
→ ancre → couche.show` et de `fermer()`. Tout hôte tiers doit recâbler la
boucle état→vues pour ne rien perdre des régimes.

### P3 — Positionnement en trois systèmes, dont du code mort (majeur)

1. `PopupPlacer` (trio coordonné complétion/doc/signature) ;
2. `PopupAnchor` (ancres simples bornées) ;
3. le ad hoc : `AtelierBordDroit.bordDroit()` (empilement minimap ← pouce
   ← bande), `AtelierBarreToolbar.ouvrirDebordement` (coin haut-droit) et
   `reposerBarre` (bas centré).

En particulier `PopupAnchor.rightEdge` est DU CODE MORT : aucune
production ne l'appelle (uniquement `PopupAnchorTest`) alors que
`AtelierBordDroit` réimplémente le même ancrage bord droit avec sa pile
d'empilement. `PopupLayer.show(x, y)` ne borne PAS les coordonnées reçues
(défense d'absence : `onLayout` vide, commentaire `PopupLayer.java:179`).

### P4 — Six copies de l'utilitaire de mesure (majeur)

Le pattern `vue.measure(UNSPECIFIED, UNSPECIFIED) → Cadre` est copié-collé
six fois : `AtelierPopups.taille()`, `MenuDebordement.taille()`,
`DocAncre.taille()`, `AtelierCouleur.taille()`,
`AtelierBordDroit.taille()`, `AtelierBarreToolbar.tailleMesuree()` — plus
la re-mesure interne de `PopupLayer.show/remonter` (double mesure à chaque
pose : l'ancre mesure, puis la couche re-mesure).

### P5 — Aucune navigation clavier des popups (majeur)

PIEGE-27 (jamais de vol de focus) est respecté et testé — mais il n'existe
AUCUNE navigation clavier déléguée : aucun `KEYCODE_DPAD_UP/DOWN`,
`KEYCODE_ENTER`, `KEYCODE_ESCAPE` ne nourrit les popups (recherche
exhaustive : seule la maquette HTML `docs/maquettes/popups.html` en
mentionne l'idée). La complétion ne se parcourt qu'au doigt ; Enter
n'accepte rien ; Escape ne referme rien. Le chemin matériel existe pourtant
(`EditorView.onKeyDown` → `KeyActions.action` → `KeyBindings.actionOf`),
mais rien n'y délègue aux popups. La mission exige flèches/Enter/Escape —
la voie est la DÉLÉGATION depuis l'éditeur qui garde le focus.

### P6 — Cycle de vie : listeners jamais retirés, closeAll orphelin (majeur)

- `AtelierBarreToolbar` (demo, lignes 65–66) :
  `couche.getViewTreeObserver().addOnGlobalLayoutListener(this::reposerBarre)`
  — référence de méthode impossible à retirer sans conservation de la
  référence ; fuite si l'atelier était reconstruit.
- `AtelierPopups` : `session.addEditListener`, `addSelectionListener`,
  `editeur.setSurViewportChange` posés au constructeur, JAMAIS retirés ;
  aucun `dispose()`/détachement n'est offert — toléré au cycle de
  l'activité, structurant pour un hôte tiers.
- `PopupController.closeAll()` (« la session se détache, la couche se
  vide ») n'est appelé NULLE PART en production — uniquement en bancs. Aucun
  changement de document n'est d'ailleurs géré par la démo (une activité =
  un document).
- `SignatureHelpView` : `Handler` de l'appui long sans nettoyage au
  détachement (`postDelayed` peut armer le peek sur une carte fermée).
- `PopupLayer.refermer` → `PopupPanel.mourir(() -> removeView(popup))` :
  FENÊTRE DE COURSE — si la carte est re-posée après que
  `post(retraitDiffere)` est parti mais avant son exécution,
  `annulerMort()` ne peut plus annuler le `removeView` déjà en file : la
  carte re-posée se retire d'elle-même (retrait d'une carte « montrée »).

### P7 — Écarts documentation / implémentation (moyen)

- « Les DIX popups » (`AtelierPopups.java:35`, `EditorActivity.java:48`,
  javadoc de `PopupController`) : le contrôleur énumère QUINZE genres.
- « la complétion FILTRE localement, la requête ne repart pas »
  (javadoc `PopupController`) : en réalité `AtelierPopups.surEdition` →
  `basculerCompletion()` relance la requête (serveur ou
  `IntelligenceHub`) à CHAQUE frappe ; seule la VUE ne re-naît pas.
- `ColorPickerView` : « l'annulation est le tap hors carte » — aucun code
  ne ferme `COLOR_PICKER` au tap extérieur (seuls frappe/défilement le
  ferment ; un tap sur l'éditeur déplace le caret sans fermer la carte).

### P8 — Deux mondes de surfaces non coordonnés (moyen)

Les feuilles modales (ADR-018) ignorent le contrôleur : ouvrir une feuille
ne ferme PAS les popups de la couche (une liste de complétion peut rester
ouverte sous le voile — le retour système, le voile et le bouton ferment la
feuille, rien ne prévient la couche). Le genre `SEARCH_BAR` est un état
SANS vue dans la couche (la vraie barre est une `FeuilleBas` posée sur
`android.R.id.content`) : confusion conceptuelle état/surface, assumée en
commentaire mais fragile.

### P9 — Rendu dupliqué entre sous-systèmes de surfaces (mineur)

Le tracé « verre + liseré 1 px + coins arrondis + clip » existe deux fois
(`PopupPanel.dessinerCarte` et `Feuille.onDraw/poserContour`), la pose de
palette trois fois (`PopupPanel.poserPalette`, `Feuille.poserPalette`, les
mini-palettes de `ErrorBandView`/`MinimapView`/`FastScrollerView`). Les
trois vues de bord droit contournent `PopupPanel` (pas de voile) — assumé
pour du chrome permanent, mais la palette est réécrite à chaque fois.

### P10 — Vocabulaire d'ancres croisé entre modules (mineur)

`android-view.PopupAnchors`/`CaretAnchors` (rects purs) vs
`android-widgets.PopupAnchor`/`PopupPlacer` (stratégies) : séparation
assumée et documentée des deux côtés, mais les noms quasi identiques
peuvent perdre un intégrateur.

## 7. Ce qui est SAIN et doit être conservé

L'audit ne recommande AUCUNE réécriture : les fondations sont bonnes et
certaines sont exemplaires.

1. PIEGE-27 (jamais de focus, jamais de `PopupWindow`, couche dans la même
   fenêtre) — prouvé par `PopupLayerFocusTest` et le banc instrumenté
   `CompletionPopupOnDeviceTest`. Toute l'architecture clavier de la phase 5
   doit le préserver (déLÉGATION, pas capture).
2. La machine à états pure `PopupController` (testable, sans vue) et la
   couche unique `PopupLayer` : UN seul de chaque dans tout le dépôt.
3. L'invariant « une vue ne vit jamais sous deux genres » de la couche —
   le bug est en amont (orchestration), pas dans l'invariant.
4. `PopupPanel`/`TextRowsPopup` (une seule base de rendu verre), les vues
   spécialisées SANS logique d'ouverture, `PopupPlacer`/`PopupAnchor`
   (placement pur borné), `PopupPalette`/`PopupTokens` (thème unique).
5. La séparation kit/orchestration : les vues du kit ne demandent jamais
   rien aux moteurs ; `AtelierPopups` (+ `PopupCoach`) joue le contenu.
6. L'ADR-018 (zéro dialogue système, tout en popups/feuilles) — vérifié :
   aucun `AlertDialog`/`Toast`/`Snackbar` dans le dépôt.
7. La convention de nommage française du projet et les plafonds ADR-002
   (600 lignes/classe) — `AtelierPopups` (598 l.) et `EditorView`
   (598 l.) sont AU plafond : toute croissance doit passer par extraction.

## 8. Conclusion de l'audit

Le système de popups de Celeris possède déjà un noyau de kit sain, testé
et documenté. Ses déficits réels, par ordre de priorité :

1. l'absence de coordinateur fourni par le kit entre `PopupController` et
   `PopupLayer` (P2), dont découlent le partage d'instances entre genres
   (P1), les six copies de mesure (P4) et les cascades en double ;
2. l'absence totale de navigation clavier (P5) ;
3. le positionnement en trois systèmes avec code mort (P3) et la
   non-fermeture au clic extérieur du color picker (P7) ;
4. les déficits de cycle de vie (P6) : listeners irrécupérables, course
   du retrait différé, `closeAll` jamais branché ;
5. la non-coordination feuilles/couche (P8) et le vocabulaire (P10).

La phase 2 conçoit l'architecture cible qui répond à ces déficits SANS
toucher à ce qui fonctionne.
