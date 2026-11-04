# Pièges hérités — à convertir en tests de non-régression

> Règle du mandat : chaque piège connu du dépôt de départ devient un **test
> nommé** avant que la fonctionnalité correspondante ne soit codée. Le présent
> registre fait foi ; une ligne n'est « couverte » que lorsque le test existe
> et est vert.
>
> Sources : la section « Pièges connus » d'`AGENT.md` (audit v3.38.0 du dépôt
> de départ) et les sections « Corrigé » / « Performance » du `CHANGELOG.md`
> du même dépôt (lues au commit `f310a39`). Les descriptions sont reformulées
> en termes génériques : aucun projet tiers n'y est nommé.

## Statuts

- **à écrire** : le test n'existe pas encore (état attendu à M0) ;
- **couvert** : le test existe, est vert et référence l'identifiant du piège
  (`PIEGE-nn`) dans son nom ou sa Javadoc.

## Registre

| Id | Piège hérité | Source | Test cible (nom anglais) | Module | Jalon | Statut |
| --- | --- | --- | --- | --- | --- | --- |
| PIEGE-01 | Un pinceau de texte partagé par tout le pipeline et muté en cours d'image : une couche qui ne repose pas explicitement la couleur de base hérite de la couleur résiduelle de la couche précédente | `AGENT.md` | `textPainter_layerOwnsItsPaint_perFrame` | `android-view` | M3 | couvert (2026-11-04, `TextPainterTest`) |
| PIEGE-02 | Une clé de cache signée par un état mutable d'un objet partagé (couleur du pinceau) : deux contenus distincts partagent une entrée de cache | `AGENT.md` | `layoutCache_keyIncludesImmutableValueOnly` | `android-view` | M3 | couvert (2026-11-04, `RowLayoutCacheTest`) |
| PIEGE-03 | L'ancre d'un glissement relue à chaque MOVE saute quand le doigt franchit l'autre borne : l'ancre doit être figée au DOWN | `AGENT.md` | `selectionDrag_anchorFrozenAtDown` | `android-view` | M3 | couvert (2026-11-04, `SelectionDragTest`) |
| PIEGE-04 | Les indices de première/dernière ligne visible sont des rangées visuelles, pas des lignes du document : toute plage calculée sous des plis est fausse | `AGENT.md` | `renderer_visibleRowsConvertThroughFoldModel` | `android-view` | M3 | couvert (2026-11-04, `VisualProjectionTest`) |
| PIEGE-05 | Les métriques de police recalculées à chaque changement de taille invalident les valeurs injectées dans les tests : réinjecter après tout changement | `AGENT.md` | `editorMetrics_fontSizeChangeInvalidatesDerivedMetrics` | `android-view` | M3 | couvert (2026-11-04, `EditorMetricsTest`) |
| PIEGE-06 | La conversion position←pixel des tests de vue arrondit au plus proche : viser le quart gauche d'un caractère, jamais son centre | `AGENT.md` | `viewTestHitTestAimsAtLeftQuarterOfGlyph` | `android-view` | M3 | couvert (2026-11-04, `HitTestTest`) |
| PIEGE-07 | L'annulation qui mute le document en dehors de la voie de transaction unique désynchronise annotations, plis et composition IME | `AGENT.md` | `undoReplay_goesThroughTransactionPipeline` | `core-text` | M2 | couvert (2026-11-04, `TextSessionTest`) |
| PIEGE-08 | Le retour à la ligne souple ne coupait pas un mot plus large que l'espace disponible : la coupe par caractère avec recherche dichotomique est obligatoire | `CHANGELOG.md` | `softWrap_breaksOversizedWordByCharacter` | `core-text` | M2 | couvert (2026-11-04, `SoftWrapTest`) |
| PIEGE-09 | L'ellipse tronquait systématiquement deux caractères de trop : la coupe dichotomique doit conserver le maximum de texte | `CHANGELOG.md` | `ellipsisKeepsMaximumUsefulText` | `android-widgets` | M5 | à écrire |
| PIEGE-10 | L'ellipse doit se calculer par largeur rendue, jamais par nombre de caractères | `CHANGELOG.md` | `ellipsisClipsByPixelWidth_notByCharCount` | `android-widgets` | M5 | à écrire |
| PIEGE-11 | Un indice en ligne (inlay) ancré sur une grille monospace doit occuper exactement une cellule d'encre pleine par caractère, sur la même ligne de base que le code : ni espace fantôme, ni chevauchement | `CHANGELOG.md` | `inlayOccupiesExactInkCells_onCodeBaseline` | `android-view` | M6 | à écrire |
| PIEGE-12 | Le chemin de rendu par mise en page façonnée ne tisse pas les indices en ligne : les lignes qui en portent doivent retomber sur le chemin tissé, au prix des ligatures sur ces lignes | `CHANGELOG.md` | `linesWithInlaysFallBackToWovenPath` | `android-view` | M6 | à écrire |
| PIEGE-13 | Un benchmark d'annulation mesuré en un seul tour échoue au CI par simple bruit du runner : retenir le minimum de trois tours par phase | `CHANGELOG.md` | `undoBenchmarkUsesMinOfThreeRuns` | `core-text` | M2 | couvert (2026-11-04, `BenchmarkHarnessTest`) |
| PIEGE-14 | La création de variantes de police en gras à chaque segment et à chaque image : le couple (base, gras) doit être mémoïsé et invalidé quand la police change | `CHANGELOG.md` | `boldTypefaceMemoized_untilFontChange` | `android-view` | M3 | couvert (2026-11-04, `TypefaceCacheTest`) |
| PIEGE-15 | Aucune allocation par image en régime établi : les rectangles et chemins de travail passent en paramètre, jamais alloués dans la boucle de dessin | `CHANGELOG.md` | `scrollFrame_allocatesNothing` | `android-view` | M3 | couvert (2026-11-04, `ScrollFrameTest`) |
| PIEGE-16 | Le panneau de documentation de complétion fusionnait titre et élément en ignorant la signature (`fence`) du serveur : les blocs typés doivent rester distincts | `CHANGELOG.md` | `completionDocModelKeepsTypedSections` | `android-widgets` | M5 | à écrire |
| PIEGE-17 | Une liste de complétion tronquée par le serveur doit être honorée telle quelle : jamais réordonnée ni complétée localement au-delà de la coupure | `CHANGELOG.md` | `truncatedCompletionList_isNeverReordered` | `core-lsp` | M6 | à écrire |
| PIEGE-18 | Le tir sémantique ne doit pas dépendre de l'état des diagnostics : deux sources de données indépendantes, jamais chaînées | `CHANGELOG.md` | `semanticTokens_independentOfDiagnostics` | `core-lsp` | M6 | à écrire |

## Règle de traçabilité

1. Avant d'écrire la fonctionnalité concernée, créer le test nommé
   ci-dessus ; il référence `PIEGE-nn` dans sa Javadoc.
2. Le test échoue d'abord (le comportement hérité n'est pas encore
   implémenté), puis passe.
3. La matrice de parité et le scénario de démonstration associés citent
   l'identifiant `PIEGE-nn` quand le piège est visible par l'utilisateur.
4. Un piège ne peut être retiré de ce registre que par décision explicite du
   propriétaire, consignée dans un ADR.

## Constat d'audit associé

La documentation du dépôt de départ (changelog et dossier `docs/`) nomme des
projets externes en plus de cent reprises, en violation de sa propre règle de
désignation neutre : le contrôle n'y était visiblement pas appliqué. Dans
celeris, `scripts/check-external-refs` couvre **tous** les fichiers versionnés,
y compris la documentation et le changelog (voir ADR-011).
