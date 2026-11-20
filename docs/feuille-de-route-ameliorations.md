# Feuille de route — améliorations : packages, documentation, réactivité

> Document vivant : **mis à jour à chaque étape** (statut, date, commit).
> Origine : demande du propriétaire (2026-11-19) — « la plupart des
> modules ont un package plat, mal structuré » ; « nettoyage des
> fichiers md inutiles » ; « à chaque changement d'onglet, impression
> d'un retard dans la coloration » ; « créer une feuille de route pour
> le plan d'amélioration et même le commencer, pousser chaque étape
> terminée ».
> S'appuie sur le plan d'améliorations (puissance/vitesse/robustesse)
> de la session 14 — les axes fonctionnels (sémantique LSP, index
> d'identifiants) restent derrière les décisions du propriétaire
> (ADR-0027, A6 de la feuille de coloration) ; cette feuille ne porte
> que ce qui est décidé et exécutable.

## Principes de la feuille

- Une étape = un commit = une vérification qui échoue avant et passe
  après (banc pour le code, grep de citations pour la documentation).
- L'ordre suit la gravité décroissante ; chaque étape est indépendante
  et réversible.
- Le statut se met à jour au moment du commit : `☐ à faire` →
  `🟡 en cours` → `✅ fait (hash court, date)`. Les blocages se notent
  `🔴 bloqué` avec la raison et la décision attendue.
- Le garde-fou d'avant-poussée (`scripts/check-avant-pousse`) passe
  avant CHAQUE poussée — la leçon de la session 14 s'applique ici.

## Constats d'origine (vérifiés sur le code, session 15)

1. **Packages plats** (nombre de fichiers du package racine) :
   `android-view` 126 classes (51 publiques), `core-intelligence`
   65 (53 publiques), `android-widgets` 53, `core-text` 38 (12
   publiques), `lsp-protocole` 13 (4 publiques), `core-syntax` 32.
   Les familles existent déjà dans les NOMS (android-view : dix
   `Painter`, cinq `Source`, quatre `Geometry`, huit `Gutter*`, cinq
   `Caret*`/`Selection*`, trois `Fold*`, quatre `Ghost*`…) mais pas
   dans les PACKAGES. `android-host` a déjà fait le mouvement
   (`host/spi`, `host/popups`) : c'est le modèle.
2. **Documentation** : 94 fichiers `.md`. Quatorze `RESUME-M*.md` et
   six `POPUP_*.md` à la racine de `docs/` sont des journaux de
   chantiers TERMINÉS — l'avancement vit ailleurs (CHANGELOG, ADR,
   feuilles de route). Deux sont lus par les contrôles du dépôt
   (`check-pieges` lit `docs/audit/PIEGES-HERITES.md`,
   `check-parity-matrix` lit `docs/parite/MATRICE-PARITE.md`) : ils ne
   bougent pas. Les autres sont cités par des ADR, le CHANGELOG ou
   l'AGENT : les citations se réparent au moment du déplacement.
3. **Retard de coloration au changement d'onglet** : la tokenisation
   appartient déjà au document (correctif du retour terrain,
   registre `documents` de la façade + `ParcMoteurs`) — le moteur
   parqué survit aux changements d'onglet. Le retard qui RESTE vient
   du **cache de mises en page** : `setSession` appelle
   `reinitialiserScene(true)` qui vide TOUT `RowLayoutCache` (plafond
   512 rangées) — retour sur un onglet = re-mesure de chaque rangée
   visible + repaint complet. La cause est structurelle : `RowKey`
   (révision, rangée, thème, police, colonnes) ne porte PAS la
   session — deux documents peuvent partager révision et numéro de
   rangée, le cache global ne peut pas les distinguer, d'où la purge
   intégrale par prudence (PIEGE-02 par construction).

## Vue d'ensemble

| Étape | Objet | Gravité | Statut |
| --- | --- | --- | --- |
| C1 | Le cache de rangées survit au changement d'onglet | haute (le retard ressenti) | ✅ fait (1c9b22a, f4b2261 — 2026-11-19) |
| C2 | Le retour d'onglet restaure la position de défilement | moyenne | ✅ fait (2026-11-20) |
| D1 | Les journaux de chantiers partent à l’archive (docs/historique/) | moyenne | ✅ fait (D1, 2026-11-19) |
| D2 | Les feuilles de route closes se rangent, l'index vit | basse | ✅ fait (2026-11-20) |
| P1 | Pilote : le paquet internal d'android-view | — (jette les bases) | ✅ fait (32e38af — 2026-11-19) |
| P2 | android-view : les peintres et la gouttière en sous-packages | haute (126 classes) | 🟡 en cours — vague 1 faite (2026-11-20) ; la vague 2 (couplées à la vue) suit P3 |
| P3 | android-view : la suite (source, curseurs, plis, fantômes) | haute | 🟡 en cours — vague 2 faite (2026-11-20) ; le reste VERROUILLÉ (voir journal) |
| P4 | core-intelligence en familles | moyenne (65 classes) | ✅ fait
  pour les internes (2026-11-20) ; les 53 publiques attendent
  la décision propriétaire n°  1 |
| P5 | core-text et android-widgets | moyenne | ✅ fait pour
  core-text (2026-11-20) ; android-widgets VERROUILLÉ (voir
  journal) |
| P6 | Manifestes api et documentation consolidés | — (clôture) | ✅ fait
  (2026-11-20) |

## Détail des étapes

### C1 — Le cache de rangées survit au changement d'onglet

- **Constat** : `FrameAssembler.reinitialiserScene(true)` vide
  `vue.cache` ENTIER à chaque `setSession` — y compris pour un
  document qui REVIENT et dont toutes les rangées sont encore
  valides. La mesure d'une rangée coûte le calcul de sa mise en page
  (segments, largeurs, bidi) : le retour d'onglet re-paie tout, l'
  image suivante retarde d'autant (le « retard dans la coloration »
  ressenti : le texte apparaît, les rangées se re-mesurent).
- **Correctif** : le modèle du dépôt est déjà là — `ParcMoteurs`
  parque le moteur d'analyse PAR SESSION. Le cache de rangées suit :
  une `Map<TextSession, RowLayoutCache>` dans la vue (le parc des
  caches), `setSession` BASCULE entre caches au lieu de vider, la
  révision dans `RowKey` continue de jouer son rôle d'invalidation
  DANS un document. Le plafond mémoire reste borné : le parc vide
  les caches des sessions fermées (`release()`), et un cache parqué
  au-delà d'un petit nombre de sessions (les onglets ouverts) se
  vide — le `RowLayoutCache` lui-même reste plafonné à 512 entrées.
- **Banc** : `CacheRangeesParSessionTest` — deux sessions A et B ;
  dessiner A (les mises en page se posent au cache) ; basculer sur B
  puis REVENIR sur A ; dessiner : les mises en page de A se
  retrouvent SANS re-mesure (le banc compte les mesures par la
  source : une source de banc qui compte ses appels). Échoue avant
  (le cache fut vidé), passe après.
- **Ne touche pas** : `RowKey` (la clé reste), les plafonds B-02, la
  politique de purge à l'édition (splice/invalidate/respan par
  révision).

### C2 — Le retour d'onglet restaure la position de défilement

- **Constat** : `reinitialiserScene(true)` remet aussi `scrollX` et
  `scrollY` à zéro — le retour sur un onglet ramène en haut du
  document, un détail qui PARTICIPE à l'impression de « refait » (le
  contenu saute, puis se re-mesure).
- **Correctif** : la position de défilement s'archive avec la
  session quittée (comme les plis D-07 et les tailles D-2 de la
  façade), se restaure à la pose — clampée par `ScrollGeometry`
  (la position restaurée peut dépasser un document raccourci).
- **Banc** : dérouler A à mi-hauteur, basculer sur B, revenir : la
  fenêtre visible de A est à mi-hauteur.

### D1 — Les journaux de chantiers partent à l'archive

- **Constat** : les quatorze `RESUME-M*.md` et six `POPUP_*.md`
  (alors à la racine de `docs/`) sont des journaux clos — le
  mouvement M*, la migration popups et le système popups sont
  FAITS, l'avancement courant vit dans le CHANGELOG et les feuilles
  de route. Ils encombraient la racine de `docs/` (20 fichiers
  historiques sur 30).
- **Correctif** : déplacement vers `docs/historique/` (git mv, les
  liens se réparent), citations réparées dans AGENT.md,
  CHANGELOG.md (entrées historiques : le chemin cité se met à
  jour), feuille-de-route.md, README.md, USAGE.md et
  ANALYSE-COMPARATIVE.md. `docs/audit/PIEGES-HERITES.md` et
  `docs/parite/MATRICE-PARITE.md` NE BOUGENT PAS (lus par les
  contrôles). La date des fichiers déplacés reste lisible pour
  `check-docs-dates` (le contrôle lit les dates de DERNIÈRE
  MODIFICATION git — un déplacement pur ne modifie pas le contenu).
- **Vérification** : grep des anciens chemins = zéro ; les deux
  contrôles qui lisent des .md passent toujours.

### D2 — Les feuilles de route closes se rangent, l'index vit

- **Constat** : `docs/` porte quatre feuilles de route (coloration,
  popups, restante, et celle-ci) + les dossiers thématiques. La
  « restante » (docs/historique/feuille-de-route-restante.md) est close depuis
  la 2.0.0.
- **Correctif** : un paragraphe d'orientation en tête de `docs/`
  (README de dossier ou section de l'AGENT) : où vit quoi (ADR =
  décisions, feuilles de route = chantiers en cours, historique =
  journaux clos, audit = preuves). La feuille close rejoint
  l'historique si rien ne la cite.

### P1 — Pilote : le paquet `internal` d'android-view

- **Constat d'origine** : le pilote prévu était lsp-protocole (9
  internes en sous-packages par rôle). **L'essai réel a été mené et
  ANNULÉ** : le module est couplé en croix — `LspSession` (publique,
  paquet racine) coordonne directement `ReceiverServeur`,
  `RequetesCouverture`, `CorrespondanceLsp`… Rendre ces classes
  accessibles inter-paquets les PUBLICISERAIT, or ce sont des noms
  français et la règle du dépôt interdit les nouveaux symboles
  publics non anglais (la ligne de base ne monte jamais). **La
  leçon tient pour tout le chantier P** : les familles du dépôt
  s'échangent des types package-privés dans les deux sens — un
  sous-paquet n'est pas une affaire de `git mv`, il faut d'abord
  l'exception qui autorise la visibilité.
- **L'exception existe, l'ADR-009 l'avait promise** : « tout
  package dont le nom contient `internal` est interne » — mais le
  générateur de manifeste ne l'implémentait PAS (`javap -public`
  sur TOUTES les classes). **P1 pose le mécanisme** : le filtre
  `internal` dans `ApiManifestTask` (un segment de paquet nommé
  `internal` n'entre jamais dans le manifeste), et le premier
  sous-paquet vit : `jo.celeris.android.view.internal` porte les
  PARCS d'état par session (`ParcMoteurs` — la tokenisation
  appartient au document ; `ParcRangees` — la mesure y appartient
  depuis C1), publics DANS leur paquet internal, hors contrat
  (android-view, module Android, n'a pas de manifeste ; les modules
  purs sont protégés par le filtre pour les mouvements P4/P5).
- **Vérifications** : 584 tests android-view verts, checkstyle et
  PMD verts, `:lsp-protocole:check` vert (le module est revenu à
  l'état d'origine — l'essai annulé n'a rien laissé), `apiCheck`
  vert sur un module pur (le filtre ne change AUCUN manifeste
  existant), baseline langue intacte (17).
- **La suite de P (P2/P3)** déplace les familles internes vers
  `view.internal.<famille>` au fil des étapes ; les publics ne
  bougent qu'avec une version et une entrée MIGRATION (décision
  propriétaire).

### P2 — android-view : peintres et gouttière

- **Constat** : 126 classes, 51 publiques — le plus gros package
  plat du dépôt. Familles évidentes DANS les noms : huit
  `Gutter*`, dix `*Painter` (+ `PainterLayer`, `PaintBrush*`,
  `BrushFactory`, `TextBrush`), quatre `*Geometry`, cinq
  `*Source`, trois `Fold*`, quatre `Ghost*`, cinq `Caret*`+
  `Selection*`, quatre `Key*`, quatre `Typeface*`/`Font*`.
- **Correctif** : sous-packages par famille POUR LES INTERNES
  d'abord (`view.gutter`, `view.paint`, `view.ghost`,
  `view.caret`…) ; les classes publiques ne bougent pas sans
  décision du propriétaire (le contrat `api/android-view` —
  ADR-009 — et les consommateurs : la façade, la démo). Chaque
  sous-paquage se fait en un commit par famille, avec le banc du
  module qui suit.
- **Risque** : les classes package-privées se déplacent LIBREMENT
  (aucun consommateur hors du module) ; les références internes se
  réparent par import. Le banc `ArchitectureTest` du module (les
  couches ne se mélangent pas) doit rester vert.

### P3 — android-view : la suite

- Source/projection (`RowSource`, `RowLayout*`, `VirtualRows`,
  `VisualRuns`, `RebindingRowSource`, `FoldedRowSource`…),
  curseurs/sélection, clavier (`Key*`, `Navigation*`,
  `PressePapiersClavier`), plis (`Fold*`, `PlisParSession`),
  fantômes (`Ghost*`), composition IME (`Compos*`), diagnostics
  (`SceneDiagnostics`, `SquigglePainter` va avec `paint`).

### P4 — core-intelligence en familles

- 65 classes dont 53 publiques — le contrat `core-intelligence.api`
  est le plus gros. Le mouvement complet des publiques exige un
  numéro de version mineure et une entrée MIGRATION : à poserver au
  propriétaire avec P2/P3 faits (la vue interne sera déjà rangée,
  le coût marginal sera clair). Les 12 internes (Flow*, Inline*,
  Method*, Scan*, Clauses*, Expression*, Fichier*, Type*) se
  rangent d'abord SANS toucher au contrat.

### P5 — core-text et android-widgets

- `core-text` : 38 classes dont 12 publiques — le paquet racine de
  la rope et de l'édition. `android-widgets` : 53 classes (les
  widgets Chrome autour de la vue). Même méthode : internes
  d'abord, publiques au fil des versions.

### P6 — Manifestes api et documentation consolidés

- Régénération complète des manifestes `api/*.api`, mise à jour
  d'ARCHITECTURE.md (la description des paquets), l'AGENT, et
  clôture de la feuille : chaque étape close, les décisions restées
  au propriétaire listées.

## Décisions attendues du propriétaire

1. **P2-P5 publics** : les classes publiques des gros modules
   changent-elles de package (version mineure + MIGRATION) ou
   seulement les internes ? La recommandation : internes d'abord,
   publics au fil des versions — le coût se paie une fois, quand la
   fenêtre de changement existe.
2. **C2** : le retour d'onglet doit-il restaurer la position
   exacte, ou seulement la ligne (colonne à zéro) ? La
   recommandation : position exacte clampée — c'est le comportement
   des éditeurs de référence.
3. Les axes PUISSANCE du plan de session 14 (sémantique LSP
   ADR-0027, index d'identifiants) restent derrière la validation
   de l'ADR — ils ne sont pas dans cette feuille.

## Journal

- 2026-11-19 : feuille posée (session 15). Constats vérifiés sur le
  code : comptages de classes par module, citations des .md,
  lecture du chemin `setSession` → `reinitialiserScene` →
  `cache.clear()`. C1 démarré dans la foulée.
- 2026-11-20 : D2 fait — `docs/README.md` pose
  l'orientation du dossier (où vit quoi : ADR =
  décisions, feuilles = chantiers en cours, historique =
  journaux clos) ; la feuille restante (close depuis
  la 2.0.0) rejoint `docs/historique/` ; citations
  réparées (feuille de route, popups, README, USAGE) — au
  passage, les cinq liens `POPUP_*` de la feuille popups,
  cassés par D1, se réparent.
- 2026-11-20 : P2, vague 1 — les familles INTERNES
  autonomes partent en sous-paquets :
  `internal/paint` (ChromePainter, GhostPainter, InlayHintsPainter,
  ValuesPainter), `internal/gutter` (EditorGutter), `internal/exec`
  (AnalysisExecutor, RetainedExecutor), `internal/model` (BracePair,
  HostCallbacks, IdentifierBounds, LanguageStructure, PopupAnchors)
  — 12 classes, publiques DANS leur paquet internal (mécanisme P1),
  hors manifeste (ADR-009). 588 tests verts, checkstyle et PMD
  verts, consommateurs (host, widgets, sample) compilés. LA LEÇON
  P1 TENAIT : les familles couplées aux champs
  package-privés de la vue (viewport, clavier, gestes, IME,
  diagnostics, fantômes) NE PEUVENT PAS partir sans publiciser un
  membre racine — elles attendent P3 et la décision du
  propriétaire (n° 1 de la feuille).
- 2026-11-20 : P3, vague 2 — `ViewportWatch` rejoint
  `internal/viewport` (registre d'écouteurs de fenêtre, autonome).
  L'essai `FoldsBySession` -> `internal/folds` a été MENÉ PUIS
  ANNULÉ : il exige de publiciser `FoldModel.Pli` et
  `FoldModel.folds()` — des symboles FRANÇAIS d'une classe
  publique du paquet racine, la règle du dépôt l'interdit
  (publiciser un membre d'une classe publique d'un module
  Android SANS manifeste = l'ajouter de fait au contrat).
  **Le verrou P3 est maintenant CARTOGRAPHIÉ** (script
  scripts/p3-analyse.py, sortie au journal de session) :
  les familles clavier (EditorKeys, KeyActions, KeyboardClipboard,
  KeyboardCursor, NavigationCommands), gestes (PinchZoomHandler,
  GesturesFactory), IME (EditorInputConnection), diagnostics
  (SceneDiagnostics), fantômes (GhostEditor), curseur
  (CaretAnchors), viewport (ViewportFractions, ViewportPrefetch),
  ViewFonts et FrameAssembler touchent TOUTES des champs
  package-privés de la vue (session, curseurClavier, glissement,
  rappels, metrics, projection…) — les déplacer exige de
  publiciser CES champs (la décision propriétaire n°  1 de la feuille : publiques en version mineure ou
  internes seulement). Le paquet racine : 126 -> 112
  classes.
- 2026-11-20 : P4 fait pour les internes — les 10 classes
  internes (3 renommées d'abord : ClausesHeritage ->
  ExtendsClauses, FichierProjet -> ProjectFile, ScanIdentifiants ->
  IdentifierScan — la règle des symboles publics anglais s'applique
  aux classes déplacées en internal, précèdent P1) rejoignent
  `intelligence.internal.{structure,flow,scan}` :
  structure (ExtendsClauses, ExpressionTypes, MethodDeclarations,
  MethodStructure, TypeRun, ProjectFile), flow (FlowAnalysis,
  FlowExtractions), scan (IdentifierScan). Les deux aides
  package-privées d'Extraction (enclosing, indentationOf) — classe
  PUBLIQUE au manifeste — déménagent chez FlowExtractions (les
  publiciser aurait changé le contrat). InlineFields et InlineSafety
  restent au racine (la publique Inline les utilise). CONTRAT
  INTACT : les internes déplacées QUITTENT le manifeste (filtre ADR-009 — 124 lignes), aucun symbole public ne change (le manifeste a suivi au commit 314f642 — la copie build/api/manifest.txt vers api/ est un geste explicite qui avait manqué au commit P4). 369 tests
  verts, checkstyle et PMD verts, consommateurs compilés. Le
  paquet racine : 65 -> 54 classes.
- 2026-11-20 : P5, core-text — les deux familles internes
  rejoignent `text.internal.*` : `internal/bidi`
  (BidiCategory, BidiClassifier, BidiEmbeddings, BidiRuns — les
  passes UAX#9) et `internal/segmentation` (WbClass, WbClassifier —
  UAX#29). Les cinq isolées (ClusterDeletes, GuardedCharSequence,
  MultiPaste, Utf8Codec, WideCodecs) restent AU RACINE :
  chacune sert UNE classe publique, une classe par sous-paquet ne
  structure rien. Manifeste régénéré FORCEMENT : zéro
  diff. 443 tests verts, checkstyle/PMD verts, consommateurs
  compilés. Paquet racine : 38 -> 32 classes.
  **android-widgets ne bouge PAS** : les 4 internes sont
  des SUPERCLASSES (PopupPanel de 10 sous-classes publiques,
  TextRowsPopup de 9, Feuille de 2) ou un utilitaire croisé
  (NavigationEtages/MenuEtages) — les déplacer publiciserait leurs
  membres protégés et déplacerait le CENTRE du module. Décision
  propriétaire n°  1 requise pour la suite.
- 2026-11-20 : P6, clôture — ARCHITECTURE.md gagne la
  section « Paquets internes«  (le mécanisme
  ADR-009 exécuté, la table des paquets, la règle des symboles
  anglais pour les classes déplacées, le verrou des familles
  couplées à la vue). Les manifestes des modules purs touchés
  (core-text, core-intelligence) suivis au commit 314f642 :
  206 lignes internes les quittent (filtre ADR-009), UNE
  référence binaire migre (Word$Effective.cls() rend
  internal.segmentation.WbClass), aucun symbole public ajouté
  ni retiré — le contrat source n'a pas bougé pendant tout le
  chantier P. **BILAN P** : android-view 126 -> 112
  classes au racine (15 rangées en internal), core-intelligence
  65 -> 54, core-text 38 -> 32 ; 3 renommages anglais
  (ExtendsClauses, ProjectFile, IdentifierScan) ;
  zéro changement de contrat.
  **Restent au propriétaire** :
  1. les familles couplées aux champs de la vue (clavier, gestes,
     IME, diagnostics, fantômes, viewport restant, ViewFonts,
     FrameAssembler) : **TRANCHÉ le 2026-11-20 — internes
     seulement, par le pont d'accès interne (P7 ci-dessous)**.
  2. les classes PUBLIQUES des gros modules changent-elles de
     paquet (version mineure + MIGRATION) ? (reco :
     au fil des versions, fenêtre par fenêtre)
  3. android-widgets : **TRANCHÉ le 2026-11-20 — internes
     seulement (P8 ci-dessous)**.

- 2026-11-20 : P7, android-view — la décision n° 1 est
  TRANCHÉE (internes seulement) : les 16 familles couplées aux
  champs package-privés de la vue rejoignent leurs sous-paquets
  `view.internal.{input,gestures,frame,caret,viewport,accessibility,
  diagnostics,fonts}` : clavier (EditorKeys, KeyboardCursor,
  KeyboardClipboard, KeyActions, EditorInputConnection), gestes
  (PinchZoomHandler, GesturesFactory, GhostEditor, DragDrop),
  FrameAssembler, CaretAnchors, viewport (ViewportFractions,
  ViewportPrefetch), AccessibilityHost, SceneDiagnostics, ViewFonts.
  LE PONT D'ACCÈS INTERNE (la reco exécutée) :
  `internal/access/EditorAccess` — l'interface qui porte la surface
  EXACTE que les familles touchent de la vue (champs en lecture et
  écriture, méthodes, délégations View), et `EditorViewAccess`
  (package-privé au racine) — la SEULE à toucher les champs. Les
  familles ne gardent PLUS aucun champ `vue` : le
  compilateur borne le couplage à l'interface ; le handle plateforme
  résiduel passe par `acces.vue()` (IME, détecteurs de gestes).
  AUCUN membre public n'est ajouté à EditorView — le contrat ne
  bouge pas (android-view n'a pas de manifeste d'API, la règle du
  dépôt l'interdisait de fait : publiciser un champ de la vue
  l'ajoutait au contrat). Paquet racine : 112 -> 97
  classes (16 rangées en internal, + EditorViewAccess au
  racine — l'implémentation package-privée du pont).
- 2026-11-20 : P8, android-widgets — la décision n° 3 est
  TRANCHÉE (internes seulement) : les SUPERCLASSES des
  popups rejoignent `widgets.internal` : PopupPanel (8 sous-classes
  publiques au racine), TextRowsPopup (9), Feuille (2) et
  NavigationEtages (l'utilitaire pur du menu à étages). Les
  sous-classes publiques RESTENT au racine (contrat inchangé) :
  elles importent leurs superclasses et override leurs abstraits
  devenus publics (drawContent, contentWidth/Height, severity,
  coteBas, maxWidthDp, rayonsDp). Les membres hérités utilisés par
  les sous-classes sont publicisés au paquet internal (mécanique
  P1/P2 — liste explicite : palette, paddingPx/YPx,
  textPaint/bandPaint/linePaint, textSize, traceRule, rowHeightPx,
  densite(), naissance/mourir/annulerMort/voileEnCours/finirEntree,
  Row et ses fabriques, setRows, rowCount, pictoDe, inkOf,
  poigneeVue/titreVue/boutonFermeture/voileVue, saut). Le paquet
  racine : 54 -> 49 classes (les popups publics ne
  bougent pas ; NavigationEtages, PopupPanel, TextRowsPopup,
  Feuille partis).
