# core-intelligence — l'intelligence de l'éditeur

L'index de symboles, la complétion, les refactorings et le formatage :
tout ce qui comprend le code plutôt que de l'afficher. Le module ne
connaît ni le LSP ni Android — il consomme l'arbre de `core-syntax` et
sert la vue et la cli.

## En bref

| | |
| --- | --- |
| Nature | JVM pure — se construit et se teste sans SDK Android |
| Coordonnées | `jo.celeris:core-intelligence:1.1.0` (jar, sources, Javadoc, SBOM, sommes — ADR-011) |
| Manifeste d'API | [`api/core-intelligence.api`](../api/core-intelligence.api), vérifié à chaque `check` (ADR-009) |
| Dépendances | `core-text`, `core-syntax`, `core-language` |
| Bancs | 37 bancs, 309 méthodes `@Test` (relevé du 2026-11-12) |

## Ce qu'il porte

- **L'index et la recherche** — `IndexSymboles` (l'index hors fil),
  `RechercheProjet` (la recherche dans le projet embarqué, A-21),
  `RechercheFloue` (le chercheur tolérant).
- **La complétion** — `CompletionMatcher` (six paliers d'appariement),
  `AcceptanceHistory` (l'apprentissage du classement), `ContextualCompletion`
  (constantes d'énumération, attributs d'annotation), `FusedFloor` (le
  plancher fusionné), `WordCompletion`, `PostfixCompletion` (A-23),
  `CompletionsXml`, `CompletionsManifeste`.
- **Les refactorings** — `Extraction` (les cinq variantes, B-20),
  `Inline`/`InlineSafety` (aligner, B-21), `SignatureChange` (B-22),
  `SafeDelete`/`SafeMove` (B-23), `Renommage`, `XmlLinkedTags` (le
  renommage lié des balises), `EditPlan`.
- **La frappe et le format** — `SmartTyping` (l'Entrée entre
  accolades fait les deux sauts — le repli textuel tient même sans
  arbre, reprise du 2026-11-12), `JavaSmartTyping` (découpage de
  chaînes, cran des labels de cas — C8), `IndentEngine`,
  `EditorConfig`, `Formateur`, `PasteReindenter`, `ImportAuto`,
  `OptimiseurImports`, `ClasspathIndex`/`ClasspathImports`
  (l'auto-import classpath, C8), `Commenter`, `LineActions`,
  `LineSorter`.

## Exemple

```java
// L'Entrée entre accolades : « {\n} » devient deux lignes indentées,
// le curseur au milieu — même sans arbre d'analyse (reprise 2026-11-12).
Document doc = session.document();
EditPlan plan = SmartTyping.enter(doc, null,
        session.selection(), IndentStyle.spaces(4));
session.apply(plan.transaction(), plan.selection());
```

Chaque geste intelligent rend un `EditPlan` : la transaction et la
sélection qui suivent voyagent ensemble, l'annulation défait le geste
entier, jamais la moitié.

## Bancs et cliquets

Bancs par collaborateur, tests de caractère contre l'oracle du dépôt
de départ (les raffinements de complétion, C6), bancs de charge de la
recherche projet. Couverture JaCoCo ≥ 90 % de lignes.

## Pour aller plus loin

- [ADR-005](../docs/adr/005-semantique-index-lsp.md) — sémantique,
  index et client LSP.
- [USAGE.md](../USAGE.md) — complétion, refactorings et formatage côté
  hôte (§ 9, § 10).
