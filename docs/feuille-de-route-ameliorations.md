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
| C1 | Le cache de rangées survit au changement d'onglet | haute (le retard ressenti) | ☐ à faire |
| C2 | Le retour d'onglet restaure la position de défilement | moyenne | ☐ à faire |
| D1 | Les journaux de chantiers partent à l’archive (docs/historique/) | moyenne | ✅ fait (D1, 2026-11-19) |
| D2 | Les feuilles de route closes se rangent, l'index vit | basse | ☐ à faire |
| P1 | Pilote : lsp-protocole en sous-packages | — (jette les bases) | ☐ à faire |
| P2 | android-view : les peintres et la gouttière en sous-packages | haute (126 classes) | ☐ à faire |
| P3 | android-view : la suite (source, curseurs, plis, fantômes) | haute | ☐ à faire |
| P4 | core-intelligence en familles | moyenne (65 classes) | ☐ à faire |
| P5 | core-text et android-widgets | moyenne | ☐ à faire |
| P6 | Manifestes api et documentation consolidés | — (clôture) | ☐ à faire |

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
  « restante » (docs/feuille-de-route-restante.md) est close depuis
  la 2.0.0.
- **Correctif** : un paragraphe d'orientation en tête de `docs/`
  (README de dossier ou section de l'AGENT) : où vit quoi (ADR =
  décisions, feuilles de route = chantiers en cours, historique =
  journaux clos, audit = preuves). La feuille close rejoint
  l'historique si rien ne la cite.

### P1 — Pilote : lsp-protocole en sous-packages

- **Constat** : 13 classes dont 9 package-privées, 4 publiques
  (`LspSession`, `ServeurEmbarque`, `ServeurExterne`,
  `TubesMemoire`). Le manifeste `api/lsp-protocole.api` ne porte que
  les noms — les classes publiques ne changent PAS de package (le
  contrat public reste `jo.celeris.lsp.protocole`).
- **Correctif pilote** : les 9 internes se rangent en sous-packages
  par rôle (`protocole.correspondance` : les Correspondance* ;
  `protocole.serveur` : le serveur embarqué de banc ; …), les
  publiques restent au paquet racine. Le module est petit : le
  mouvement complet tient en un commit, il JETTE LES BASES du
  chantier P (naming, manifeste régénéré, baseline langue,
  checkstyle, contrôles).
- **Banc** : le module a ses tests (le banc du protocole) ; le
  manifeste `api/lsp-protocole.api` se régénère
  (`./gradlew apiManifest --rerun-tasks`), la baseline des
  identifiants français ne bouge pas (noms inchangés).

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
