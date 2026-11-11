# core-syntax-grammars — la voie de grammaires

La seconde voie d'analyse syntaxique : les grammaires réellement
TextMate, chargées en données pour la longue traîne des langages
(ADR-014). Le module ne dit jamais le nom de son moteur d'expressions
rationnelles (joni, consigné au NOTICE et au SBOM) — l'artefact dit sa
fonction.

## En bref

| | |
| --- | --- |
| Nature | JVM pure — se construit et se teste sans SDK Android |
| Coordonnées | `jo.celeris:core-syntax-grammars:1.0.0` (jar, sources, Javadoc, SBOM, sommes — ADR-011) |
| Manifeste d'API | [`api/core-syntax-grammars.api`](../api/core-syntax-grammars.api), vérifié à chaque `check` (ADR-009) |
| Dépendances | `core-syntax`, `core-language` ; moteur d'expressions rationnelles (NOTICE) |
| Bancs | 21 bancs, 106 méthodes `@Test` (2026-11-11) |

## Ce qu'il porte

- **Le chargeur** — `GrammaireParseur` lit la grammaire TextMate en
  données (le nom racine est facultatif : la grammaire PHP du commerce
  s'en passe), `GrammaireLexeur` et `GrammaireSyntax` la branchent sur
  le socle incrémental de `core-syntax`.
- **La garde** — `GardeCatastrophique` coupe les retours en arrière
  ruineux du moteur : un langage hostile ne bloque jamais l'éditeur.
- **Les expressions** — `Expressions` compile les motifs du commerce
  sans modification (les bancs verrouillent l'égalité octet pour octet
  des grammaires reprises).

## Bancs et cliquets

Bancs de conformité par grammaire reprise du commerce (HTML, CSS,
JavaScript, TypeScript, PHP, Ruby — chantier C5) : égalité octet pour
octet, acceptation au-dessus du seuil, rejeu déterministe. Couverture
JaCoCo ≥ 90 % de lignes.

## Pour aller plus loin

- [ADR-014](../docs/adr/014-moteur-expressions-rationnelles.md) — le
  moteur d'expressions rationnelles derrière la frontière ;
  [ADR-017](../docs/adr/017-plateforme-de-langages.md) — la plateforme
  de langages.
- [USAGE.md](../USAGE.md) — les paquets de données et les vôtres (§ 7).
