# core-syntax — l'analyse syntaxique incrémentale

Le socle d'analyse : jetons, états par ligne, arbre tolérant, plis,
crochets et analyse par fenêtre. Les analyseurs Java, Kotlin, XML,
JSON, Gradle KTS et Groovy vivent ici ; les consommateurs (coloration,
plis, plan, indentation) partagent le même arbre (ADR-004).

## En bref

| | |
| --- | --- |
| Nature | JVM pure — se construit et se teste sans SDK Android |
| Coordonnées | `jo.celeris:core-syntax:1.0.0` (jar, sources, Javadoc, SBOM, sommes — ADR-011) |
| Manifeste d'API | [`api/core-syntax.api`](../api/core-syntax.api), vérifié à chaque `check` (ADR-009) |
| Dépendances | `core-text` |
| Bancs | 44 bancs, 212 méthodes `@Test` (2026-11-11) |

## Ce qu'il porte

- **Le socle incrémental** — `IncrementalLexer`, `IncrementalParser`,
  états par ligne (`LexState`), arbre tolérant (`SynNode`, `Analysis`),
  ré-analyse bornée par budget, annulation en vol (`JetonAnnulation`,
  `AnalyseAnnulee`).
- **Les dialectes** — `JavaSyntax`, `KotlinSyntax`, `XmlSyntax`,
  `JsonSyntax`, `KtsSyntax`, `GroovySyntax` et le socle C-like partagé
  (`CLikeLexer`, `CLikeParser`, `CLikeDialect`).
- **La structure** — crochets (`BracketMatcher`), plis (`FoldSpan`,
  `TreeFolds`, `IndentationFolds`), plan (`OutlineEntry`,
  `TreeOutline`), portées et thèmes de portées (`ScopeStacks`,
  `ScopeSelector`, `ScopeTheme`).
- **La fenêtre d'abord** — `AnalyseFenetre` analyse la fenêtre visible
  avant le reste ; `Charge480kLignesTest` tient le banc de charge
  480 000 lignes (fenêtre 1441 ms, incrément 36 ms — chantier C10).

## Bancs et cliquets

Corpus embarqués (`src/test/resources/corpus/`), snapshots C-like,
propriétés des lexeurs, cascade de commentaires bornée, charge 480 000
lignes au tas dédié de 1 Go. Couverture JaCoCo ≥ 90 % de lignes.

## Pour aller plus loin

- [ADR-004](../docs/adr/004-analyse-syntaxique.md) — analyse
  incrémentale ; [ADR-007](../docs/adr/007-modele-de-threading.md) —
  l'analyse vit hors fil.
- [USAGE.md](../USAGE.md) — les langages servis par le socle (§ 7).
