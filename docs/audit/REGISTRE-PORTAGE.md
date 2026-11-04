# Registre de portage — du dépôt de départ vers celeris

> Règle du mandat : chaque fichier porté est consigné ici (source, décision,
> tests associés). À M0, ce registre contient le **pré-classement issu de
> l'audit** ; les colonnes « statut » et « commit » se remplissent à chaque
> portage réel (M2+). Un fichier non listé ici ne peut pas être porté.
>
> Le dépôt de départ est traité en lecture seule au commit `f310a39` ; le
> nouveau dépôt n'est pas un fork et ne reprend aucun historique.

## Légende

- **Porter** : réutiliser l'algorithme et ses tests après revue ligne à
  ligne ; l'API d'accueil est celle de celeris (types forts, transactions).
  Portage rope (2026-11-04), adaptations consignées  : paquet
  `jo.celeris.core.text`, contract `@NullMarked` (plus de tolérance
  à `null` en entrée), feuille et branche invisibles hors du paquet,
  `instanceof` à motifs (niveau 17), graines du fuzz fixées pour la
  mutation.
- **Spécifier** : réécrire ; l'ancien code sert de spécification et
  d'oracle de test (les sorties observées deviennent les assertions).
- **Abandonner** : ne pas reprendre.

## Cœur texte (`cel-core` → `core-text`)

| Élément source | Décision | Justification (audit/spike) | Tests associés |
| --- | --- | --- | --- |
| `rope/Rope.java` (395 l., imports `java.util` seuls) | Porter | re-mesuré : 0,002 ms/édition p50 sur 2 M car., mémoire 1,22× | portés et étendus (M2, 2026-11-04) : `RopeTest`, `RopeFuzzTest` (graines fixes reproductibles), `RopeVisitLeavesTest`, `RopeProprietesTest` (jqwik, ~10 300 séquences/oracle `StringBuilder`) |
| index de lignes de `document/` | Porter | voie de mutation unique déjà validée | tests de l'index + invariants croisés fuzz |
| `session/EditorSession.java` (1 694 l.) | Spécifier | conforme au plafond 600 l. ; le concept (voie unique, undo par la même voie) est l'acquis, pas la classe | spécification entamée (2026-11-04) : `TextSession` — la voie unique et l'annulation par la même voie (PIEGE-07) ; l'index de lignes, la multi-sélection et les ponts restent à spécifier. Oracle : `UndoPipelineTest` → `undoReplay_goesThroughTransactionPipeline` |
| `session/UndoManager`/`UndoRecorder` (coalescence) | Porter | coalescence mesurée et éprouvée | portés (2026-11-04) : `UndoStack` (horloge injectée, ADR-007 — le moteur n'appelle jamais l'horloge système) et `UndoStep` (copie défensive) ; adaptation consignée : le lot multi-éditions d'une transaction remplace `begin/endBatch`, le dispatch vit dans la session ; tests de coalescence portés et étendus (frontière de délai sur horloge scriptée, coupes de chaîne par undo/lot/remplacement) |
| modèle de sélection (`Selection`, 2 `int`) | Abandonner (remplacer) | mono-curseur par construction ; multi-sélection exigée (A-01) | nouveaux tests multi-sélection |
| recherche/remplacement | Porter | acquis ; regex, casse, mot entier déjà couverts | tests existants → oracle |
| décalage des diagnostics et annotations | Porter | « pipeline commun » du dépôt de départ, cité par PIEGE-07 | porté (2026-11-04) : contrat `PositionProjector` + `AnchoredRanges` (gravités du pipeline commun, chute des plages vides) ; les types porteurs arrivent avec leurs modules |
| plis, retour à la ligne (modèles purs) | Porter | modèles purs documentés | retour à la ligne pur porté (2026-11-04) : `SoftWrap` à mesure injectée, coupe dichotomique par caractères (PIEGE-08 écrit avant) ; la variante code (blancs préservés, coupes par offsets) vient avec la vue (M3) ; plis à venir |
| `ImeBridge` (cœur) | Porter | pont de composition éprouvé par niveau d'API | tests de composition IME |

## Syntaxe (`cel-core/highlight` → `core-syntax`)

| Élément source | Décision | Justification | Tests associés |
| --- | --- | --- | --- |
| 19 tokéniseurs manuels | Abandonner (remplacer par l'arbre, ADR-004) | constat n° 4 : voies séparées, pas d'arbre | corpus de coloration → tests de l'arbre |
| `TokenizerTemplate.java` (2 046 l.), `GenericTokenizer` (1 587 l.) | Abandonner | classes au-delà du plafond, remplies par le socle commun | — |
| voie de grammaires (`cel-textmate`) | Porter (en module `core-syntax-grammars`) | longue traîne (ADR-004) ; **les deux archives binaires versionnées ne sont pas reprises** (redéclarées en dépendances coordonnées, ADR-011) | tests de grammaires existants |
| mapping portées → types de jetons | Porter | contrat stable | tests de mapping |

## Interface (`cel-ui` → `android-view` / `android-widgets`)

| Élément source | Décision | Justification | Tests associés |
| --- | --- | --- | --- |
| `EditorView.java` (4 579 l., 494 membres publics) | Abandonner (refonte totale) | constat n° 2 ; plafond 600 l. ; composition imposée | captures de référence = oracle visuel |
| `render/EditorRenderer.java` | Spécifier | pipeline à couches conservé comme concept ; l'ordre des couches documenté devient la spécification | tests de capture + PIEGE-04 |
| caches de rendu par ligne (`LineRenderCache`) | Porter | acquis (révisions, invalidation) | PIEGE-02 d'abord |
| `EditorShapedLayoutCache` (ligatures) | Spécifier | chemin façonné contre chemin tissé : la dualité est documentée par PIEGE-12 | PIEGE-11/PIEGE-12 |
| `popup/kit/*` (`TextLayoutKit`, `PopupPlacement`, `PopupTokens`, `PopupCard`, `DocRenderModel`) | Porter | logique pure, mesure injectée — modèle du genre | leurs tests (y compris wrap/ellipses : PIEGE-09/10) |
| popups de complétion/signature (rendu) | Spécifier | s'appuient sur le kit porté | tests de placement coordonné |
| gestes (`EditorInputHandler`, `EditorSelectionGestures`, `EditorTapResolver`) | Spécifier | machine à états explicite exigée (PIEGE-03) ; l'actuel sert de spécification | tests gestuels instrumentés |
| `EditorWrapGeometry` (wrap/plis ↔ Y) | Porter | géométrie pure éprouvée | tests de conversion rangées ↔ lignes (PIEGE-04) |
| `BreadcrumbBar`, `EditorSearchBar` (a11y partielle) | Spécifier | seuls fichiers avec `contentDescription` — la vue principale doit rattraper (ADR-008) | auditeur d'accessibilité |

## Sémantique (`cel-lsp*` → `core-lsp` / `core-language`)

| Élément source | Décision | Justification | Tests associés |
| --- | --- | --- | --- |
| contrats `Language` et providers (`cel-lsp-api`) | Porter | acquis cité par le mandat | tests de contrat |
| client LSP sur [EXT-5] | Abandonner (client propre, ADR-005) | 0.22.0 figé par rupture de contrat ; 8 283 l. à remplacer par une surface bornée | traces de serveurs réels → corpus de tests de contrat |
| `LanguageServerWrapper`, `LspEditor` | Spécifier | cycle de vie documenté (déconnexion, resynchronisation) = spécification | `LspDisconnectLifecycleTest` → oracle |
| `MiniJava*` (démo, 22 fichiers, 9 301 l.) | Abandonner | hors bibliothèque ; remplacé par l'arbre (M4) puis l'index (M6) | — |

## Outillage

| Élément source | Décision | Justification |
| --- | --- | --- |
| workflows CI épinglés par SHA + validation du wrapper | Porter | acquis cité par le mandat |
| catalogue de versions unique | Porter | acquis (commentaires de cliquet inclus) |
| contrôles de langue / références externes | Réécrire en plus large | l'audit mesure 115 mentions externes dans la doc du dépôt de départ : preuve que le contrôle doit couvrir tous les fichiers |
| baselines lint (48 entrées) | Abandonner | zéro baseline dès le premier commit (ADR-011) |

## Avancement

| Catégorie | Fichiers portés | Restants |
| --- | --- | --- |
| Cœur texte | `rope/Rope.java` + 3 fichiers de tests (2026-11-04) ; `session/UndoManager`/`UndoRecorder` → `UndoStack`/`UndoStep`, session de texte spécifiée (`TextSession`, `Document`, `Transaction`, projecteurs de positions) et wrap pur (`SoftWrap`), avec leurs tests (2026-11-04, tranche des pièges) | index de lignes, sélection multi, recherche, plis, ImeBridge |
