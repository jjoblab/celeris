# android-view — la vue Canvas

La vue d'édition Android : rendu Canvas sans allocation par image,
curseurs et sélections multiples tactiles, défilement, retour à la
ligne souple, IME par niveau d'API, accessibilité, gouttière et
pincement. Le moteur vit dans les modules purs — la vue ne fait que
peindre et écouter (ADR-002, ADR-006).

## En bref

| | |
| --- | --- |
| Nature | bibliothèque Android (`minSdk` 26, `compileSdk` 36 — ADR-011) |
| Coordonnées | `jo.celeris:android-view:1.0.0` (AAR de la variante release, sources, Javadoc, SBOM, sommes — depuis le 2026-11-11) |
| Manifeste d'API | pas de manifeste (ADR-009 couvre les modules purs) — l'API publique vit dans `src/main/java` |
| Dépendances | `core-ai`, `core-intelligence`, `core-language` |
| Bancs | 90 bancs, 438 méthodes `@Test` (2026-11-11) — Robolectric sur JVM (ADR-013) |

## Ce qu'il porte

- **La vue** — `EditorView` (le point d'entrée, `View` Android
  ordinaire), `ScrollFrame` et `ViewportFractions` (le défilement par
  fraction, chantier C9), `PainterLayer`/`TextPainter` (le rendu),
  `RowLayout` et le cache de rangées, `StickyLines` (lignes collantes),
  `FoldModel`/`FoldTracker` (les plis).
- **Les entrées** — curseurs multiples et sélections
  (`CaretsMotion` y compris par mots à bosses de chameau, A-04 ;
  `CaretGeometry`, A-18 ; `SelectionHandles`, `SelectionDrag`,
  `ColumnSelection`), pincement (`PinchZoom`), glisser-déposer
  (`GlisserDeposer`, D-20), clavier (`KeyBinding`, `KeyBindings`),
  IME par niveau d'API (`SurroundingTexts`, `CompositionState`),
  loupe (`MagnifierGeometry`).
- **Les ornements du canvas** — gouttière (`GutterPainter`,
  `GutterLanes`, `GutterNumbers`, `GutterMarks`), guides
  d'indentation (`IndentGuides`, A-17), caractères invisibles,
  surlignage des occurrences, ligatures (`setLigatures`, D-14).
- **Les adaptations** — thèmes clair/sombre/contraste élevé
  (`EditorTheme`, `EditorThemes`, `WcagContrast`), arbre
  d'accessibilité virtuel, mode gros fichiers tri-état
  (`LargeFileMode`, chantier C10), raccord de l'intelligence
  (`IntelligenceHub`, `PluginHost`).

## Bancs et cliquets

Tests de vue Robolectric (ADR-013) : rendu capturé, gestes, IME,
accessibilité, budgets de peinture (ADR-006). Checkstyle/PMD aux
mêmes plafonds que les modules purs (600/800 lignes par fichier,
ADR-002/019), lint Android bloquant sans baseline.

## Pour aller plus loin

- [ADR-006](../docs/adr/006-rendu-et-budgets.md) — rendu et budgets ;
  [ADR-013](../docs/adr/013-banc-de-test-de-vue.md) — le banc de test
  de vue.
- [USAGE.md](../USAGE.md) — le montage de la vue chez l'hôte (§ 4).
