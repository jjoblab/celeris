# core-language — la plateforme de langages

Les contrats de langage, les paquets de données et le registre
(ADR-017) : le point où un langage se déclare — configuration,
grammaire, capacités — sans que le moteur ne connaisse aucun langage en
dur. Vingt-deux langages intégrés en paquets de données.

## En bref

| | |
| --- | --- |
| Nature | JVM pure — se construit et se teste sans SDK Android |
| Coordonnées | `jo.celeris:core-language:1.0.0` (jar, sources, Javadoc, SBOM, sommes — ADR-011) |
| Manifeste d'API | [`api/core-language.api`](../api/core-language.api), vérifié à chaque `check` (ADR-009) |
| Dépendances | `core-text`, `core-syntax` |
| Bancs | 14 bancs, 110 méthodes `@Test` (2026-11-11) |

## Ce qu'il porte

- **Les contrats** — `LanguageContributor` (la contribution SPI),
  `LanguageConfiguration` (paires, commentaires, plis, indentation,
  règles de portée), `GrammarFactory`, `LanguageCapability`.
- **Les paquets intégrés** — `src/main/resources/langages/` : java,
  kotlin, kts, xml, json, groovy, c, cpp, css, html, javascript,
  typescript, markdown, php, python, ruby, shell, sql, toml, yaml,
  proguard, properties — chaque paquet en `language.json`,
  `grammar.json`, `pack.json` (grammaires du commerce reprises sans
  modification, licences au NOTICE, chantier C5).
- **Le lecteur** — `JsonReader` et `ConfigurationParser` lisent les
  paquets ; le registre s'assemble hors fil UI (ADR-007).

## Bancs et cliquets

Bancs de conformité des paquets intégrés (épingles, acceptations,
déterminisme), registre et contributions SPI. Couverture JaCoCo ≥ 90 %
de lignes.

## Pour aller plus loin

- [ADR-017](../docs/adr/017-plateforme-de-langages.md) — paquets,
  modèle et coutures ; le paquet jouet H-04 y est la preuve.
- [USAGE.md](../USAGE.md) — déclarer les langages et vos propres
  paquets (§ 7) ; serveurs de langage déclarés en données (§ 8).
