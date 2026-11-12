# cli — l'exécutable sans Android

La ligne de commande celeris : le même moteur que la vue Android, en
JVM pure, sans SDK — la fumée de la CI l'exécute à chaque poussée
(`./gradlew :cli:run`). Une application, jamais publiée.

## En bref

| | |
| --- | --- |
| Nature | application JVM (`java` distribution, ADR-011) |
| Publication | jamais publiée — l'entrée vit dans `jo.celeris.cli.Celeris` |
| Dépendances | `core-text`, `core-syntax`, `core-syntax-grammars`, `core-language`, `core-intelligence`, `core-lsp`, `core-ai` |
| Bancs | 5 bancs, 21 méthodes `@Test` (relevé du 2026-11-12) |

## Ce qu'elle porte

- **La bannière** — `Celeris` affiche la version courante ; un test
  épingle la version annoncée (chantier C3 — plus de version fantôme).
- **Les commandes (G-03, chantier C13)** — `version`, `analyse`
  (l'arbre et les jetons), `recherche` (les occurrences d'un motif),
  `diagnostiquer` (diagnostics de l'analyse, `TreeDiagnostics`),
  `formater` (`Formateur`) et `refactorer renommer` (`IndexSymboles`
  + `Renommage`) ; sortie déterministe, codes de sortie documentés.

## Exemple

```sh
# La fumée de la CI exécute la bannière à chaque poussée.
./gradlew :cli:run

# Le lanceur installé (./gradlew :cli:installDist), puis les commandes :
cli/build/install/cli/bin/cli version
cli/build/install/cli/bin/cli analyse src/main/java/…/Celeris.java
cli/build/install/cli/bin/cli diagnostiquer exemple/Corrompu.java
cli/build/install/cli/bin/cli formater exemple/Corrompu.java
cli/build/install/cli/bin/cli refactorer renommer Foo Bar
```

## Bancs et cliquets

Tests de fumée des commandes, test cliquet de la version, architecture
(module sans Android). Couverture JaCoCo ≥ 90 % de lignes, comme les
modules purs.

## Pour aller plus loin

- [ADR-011](../docs/adr/011-build-et-publication.md) — build et
  publication (la cli n'est pas un artefact).
- [USAGE.md](../USAGE.md) — la cli côté hôte (§ 15).
