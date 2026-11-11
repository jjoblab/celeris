# lsp-protocole — la frontière protocolaire du LSP

Le seul module qui connaît la bibliothèque de protocole (LSP4J,
consignée au NOTICE) et sa liaison de données : les sessions, les
serveurs embarqués, les tubes mémoire et la correspondance des
documents. L'artefact dit sa fonction, jamais sa marque — votre code
hôte n'en voit jamais le nom (ADR-015).

## En bref

| | |
| --- | --- |
| Nature | JVM pure — se construit et se teste sans SDK Android |
| Coordonnées | `jo.celeris:lsp-protocole:1.0.0` (jar, sources, Javadoc, SBOM, sommes — ADR-011) |
| Manifeste d'API | [`api/lsp-protocole.api`](../api/lsp-protocole.api), vérifié à chaque `check` (ADR-009) |
| Dépendances | `core-lsp` ; la bibliothèque de protocole et sa liaison de données (NOTICE) |
| Bancs | 9 bancs, 62 méthodes `@Test` (2026-11-11) |

## Ce qu'il porte

- **La session** — `LspSession` (la session complète, documents et
  espaces), `RecepteurServeur` (le récepteur des réponses),
  `ServeurEmbarque` (le montage en processus), les tubes mémoire
  (`TubesMemoire`) pour les serveurs embarqués.
- **La correspondance** — `CorrespondanceLsp`, `CorrespondanceDocuments`,
  `CorrespondanceEspace` : le protocole se traduit vers les types
  immuables de `core-lsp`, la conversion porte TOUTES les entrées.
- **La couverture** — `RequetesCouverture` et
  `CorrespondanceCouverture` traduisent les réponses riches (surcharges
  de signature, chantier C7).

## Bancs et cliquets

Bancs contre des serveurs réels en corpus de traces (ADR-005) ;
repli local sur réponse vide. Couverture JaCoCo ≥ 90 % de lignes.

## Pour aller plus loin

- [ADR-015](../docs/adr/015-client-lsp-sur-lsp4j.md) — le client LSP
  sur la bibliothèque derrière la frontière.
- [USAGE.md](../USAGE.md) — serveurs de langage déclarés en données,
  tubes mémoire, règles de conservation R8 (§ 8, § 19).
