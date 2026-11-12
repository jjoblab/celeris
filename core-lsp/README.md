# core-lsp — le domaine du LSP

Les types immuables du domaine LSP : diagnostics, complétions,
corrections, éditions d'espace — avec l'ordonnancement des versions et
la conversion UTF-16 (ADR-015). Le module ne connaît AUCUN transport :
ni processus, ni réseau, ni bibliothèque de protocole — c'est le vocabulaire partagé entre le
moteur et la frontière protocolaire.

## En bref

| | |
| --- | --- |
| Nature | JVM pure — se construit et se teste sans SDK Android |
| Coordonnées | `jo.celeris:core-lsp:1.1.0` (jar, sources, Javadoc, SBOM, sommes — ADR-011) |
| Manifeste d'API | [`api/core-lsp.api`](../api/core-lsp.api), vérifié à chaque `check` (ADR-009) |
| Dépendances | `core-language` |
| Bancs | 9 bancs, 37 méthodes `@Test` (relevé du 2026-11-12) |

## Ce qu'il porte

- **Les types du domaine** — `DiagnosticLsp`, `CorrectifLsp`,
  `ElementCompletion`, `ListeCompletion`, `EditionTexteLsp`,
  `EditionDocumentLsp`, `EditionEspaceLsp`, `EditionGabaritLsp`,
  `IndiceLsp`, `LieuLsp`, `CapacitesServeur`, `AuditeurDiagnostics`.
- **La conversion UTF-16** — les positions LSP (UTF-16) se projettent
  vers les offsets du modèle, jamais l'inverse (ADR-015).
- **L'ordonnancement** — les versions de document s'ordonnent, les
  réponses tardives se rejettent, les jetons d'annulation se propagent.

## Exemple

```java
// Le vocabulaire partagé : un diagnostic arrive immuable, la gravité
// et la plage déjà converties hors UTF-16.
DiagnosticLsp constat = new DiagnosticLsp(plage,
        DiagnosticLsp.ERREUR, message, null, null);
```

Les types ne connaissent aucun transport : ils traversent le moteur
et la frontière protocolaire sans se réinterpréter (ADR-015).

## Bancs et cliquets

Bancs de conversion, d'ordonnancement et d'immuabilité ; corpus de
traces de serveurs réels (ADR-005, tests de contrat). Couverture
JaCoCo ≥ 90 % de lignes.

## Pour aller plus loin

- [ADR-015](../docs/adr/015-client-lsp-sur-lsp4j.md) — le client LSP
  derrière la frontière protocolaire.
- [USAGE.md](../USAGE.md) — monter des serveurs de langage (§ 8).
