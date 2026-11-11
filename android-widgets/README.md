# android-widgets — le kit de popups et d'ornements

Les surfaces fonctionnelles autour de la vue : le kit de popups de
l'ADR-018 (feuille du bas, feuille du haut, popup ancrée — jamais
d'écran plein cadre, jamais de dialogue système), la complétion,
l'aide aux paramètres, la recherche, le plan, et les ornements du
chrome (minimap, fil d'Ariane, bande d'erreurs, défilement rapide).

## En bref

| | |
| --- | --- |
| Nature | bibliothèque Android (`minSdk` 26, `compileSdk` 36 — ADR-011) |
| Coordonnées | `jo.celeris:android-widgets:1.0.0` (AAR de la variante release, sources, Javadoc, SBOM, sommes — depuis le 2026-11-11) |
| Manifeste d'API | pas de manifeste (ADR-009 couvre les modules purs) — l'API publique vit dans `src/main/java` |
| Dépendances | `core-intelligence` |
| Bancs | 22 bancs, 123 méthodes `@Test` (2026-11-11) — Robolectric sur JVM (ADR-013) |

## Ce qu'il porte

- **Les feuilles (ADR-018)** — `FeuilleBas` et `FeuilleHaut` : poignée
  de glissement, rangée de titre, bouton de fermeture, corps défilant,
  voile, quatre fermetures (bouton, voile, glissement au-delà du
  seuil, retour).
- **Les popups ancrées** — `PopupLayer`, `PopupController`,
  `PopupPlacer`, `PopupAnchor`, `PopupPalette`, `PopupTokens` :
  complétion (`CompletionListView`, `CompletionDocModel`),
  aide aux paramètres riche (`SignatureHelpView`, `SignatureAideModel`
  — stepper, fenêtre ±1, peek, chantier C7), diagnostic
  (`DiagnosticTooltipView`), intentions (`IntentionsMenuView`), aperçu
  de définition (`ApercuDefinition`).
- **Les barres** — recherche (`SearchBarModel`), plan
  (`OutlineView`, `OutlineModel`, `OutlinePainter`), fil d'Ariane
  (`BreadcrumbModel`, `BreadcrumbView`), sélection
  (`SelectionBarView`), bande d'erreurs (`ErrorBandView`,
  `ErrorBandModel`).
- **Les ornements (chantier C9)** — minimap (`MinimapModel`,
  `MinimapView`) et défilement rapide (`FastScrollerView`) ; la revue
  par blocs de l'agent (`RevueDiff`, `DiffBlocs`, ADR-012).

## Bancs et cliquets

Tests de vue Robolectric (ADR-013) : les feuilles et leurs fermetures,
le stepper au toucher, le peek à l'appui long, le saut de la minimap,
le glissement du défilement rapide. Checkstyle/PMD aux mêmes plafonds
que les modules purs (ADR-002/019), lint Android bloquant sans
baseline.

## Pour aller plus loin

- [ADR-018](../docs/adr/018-interface-tout-en-popups.md) — l'interface
  tout en popups ; maquettes réactives
  ([`docs/maquettes/popups.html`](../docs/maquettes/popups.html)).
- [USAGE.md](../USAGE.md) — les popups et feuilles chez l'hôte (§ 6).
