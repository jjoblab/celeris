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
| Coordonnées | `jo.celeris:android-widgets:1.1.0` (AAR de la variante release, sources, Javadoc, SBOM, sommes — depuis le 2026-11-11) |
| Manifeste d'API | pas de manifeste (ADR-009 couvre les modules purs) — l'API publique vit dans `src/main/java` |
| Dépendances | `core-intelligence` |
| Bancs | 23 bancs, 134 méthodes `@Test` (relevé du 2026-11-12) — Robolectric sur JVM (ADR-013) |

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
  `ErrorBandModel`), barre de touches de code (`TouchBarView` : Tab,
  commentaire, déplacements, duplication — au-dessus du clavier, les
  états vivants suivent le caret).
- **Les ornements (chantier C9)** — minimap (`MinimapModel`,
  `MinimapView`) et défilement rapide (`FastScrollerView`) ; la revue
  par blocs de l'agent (`RevueDiff`, `DiffBlocs`, ADR-012). La
  minimap se rend COLORÉE depuis le 2026-11-12 : chaque rangée
  porte la charge de son bouquet ET les segments encrés de sa ligne
  représentative — code, chaînes, commentaires, nombres, mots-clés —
  la silhouette rend l'essentiel de l'éditeur, tout langage confondu.

## Exemple

```java
// La silhouette colorée d'un document : une rangée par cran.
MinimapModel carte = MinimapModel.of(session.document(), hauteurRangees);
for (MinimapModel.MapRow rangee : carte.rangees()) {
    peindre(rangee.slot(), rangee.load(), rangee.segments());
}

// Une touche de code : libellé, état vivant, action.
new TouchBarView.Touche("Tab", null, () -> indenterBloc());
```

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
