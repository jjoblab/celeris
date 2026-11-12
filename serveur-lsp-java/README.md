# serveur-lsp-java — le mini serveur de langage Java

Le compagnon de banc de l'éditeur : un serveur LSP Java complet mais
volontairement petit — poignée de main, diagnostics poussés,
complétion documentée, survol (au nom déclaré comme au POINT D'USAGE
d'un symbole), aide aux paramètres (l'appel ouvert sous le curseur
rend la signature de SA déclaration), symboles hiérarchiques, plis et
formatage d'indentation. Aucun réseau, aucun système de fichiers, des
analyses déterministes : un même document rend toujours les mêmes
réponses. La démonstration le déclare en donnée (`servers.json`) et
le monte en processus par la désignation de la frontière
protocolaire ; il se lance aussi seul, sur l'entrée-sortie standard,
pour éprouver l'éditeur contre un serveur en processus séparé.

## En bref

| | |
| --- | --- |
| Nature | JVM pure — se construit et se teste sans SDK Android |
| Coordonnées | `jo.celeris:serveur-lsp-java:1.1.0` (jar, sources, Javadoc, SBOM, sommes — ADR-011) |
| Manifeste d'API | [`api/serveur-lsp-java.api`](../api/serveur-lsp-java.api), vérifié à chaque `check` (ADR-009) |
| Dépendances | la bibliothèque de protocole seule (NOTICE — détail interne, jamais dans l'API) |
| Bancs | 11 bancs, 83 méthodes `@Test` (relevé du 2026-11-12) |

## Ce qu'il porte

- **Le serveur** — `JavaLanguageServer` : la convention des serveurs
  embarqués (constructeur vide, `setClient`), les capacités
  négociées à la poignée de main (complétion, survol, AIDE AUX
  PARAMÈTRES, symboles, plis, formatage, synchronisation complète —
  la signature joint le survol depuis le 2026-11-12), les diagnostics
  POUSSÉS à chaque ouverture et changement, la liste vide à la
  fermeture.
- **L'analyse** — `JavaSource` (l'index des lignes, la transposition
  UTF-16, le code dépouillé de ses chaînes et commentaires),
  `JavaDiagnostics` (équilibre des paires, littéraux non fermés,
  point-virgule probable), `JavaSymbols` (types, méthodes,
  constructeurs, champs), `JavaFolds`, `JavaCompletion`,
  `JavaSignature` (le paramètre courant se compte aux virgules de
  premier niveau, « new » répond par son constructeur, l'en-tête
  d'une déclaration n'est pas un appel), `JavaHover` (le mot-clé du
  langage a sa table, le symbole sa ligne de déclaration), et
  `JavaFormatter`.
- **Le point d'entrée autonome** — `JavaLanguageServerMain` :
  l'entrée-sortie standard, pour les bancs et l'intégration continue
  qui veulent un serveur réel en processus séparé. Il ne surveille
  jamais la mort d'un processus parent (le piège fatal aux hôtes
  embarqués, constat M8.6).

## Exemple

La déclaration qui le monte en processus (la voie PAR
DÉSIGNATION, jamais de classe appelée en dur chez l'hôte) — telle
que la démonstration la porte dans
`demo/src/main/resources/serveurs/servers.json` :

```json
{
  "servers": [{
    "language": "java",
    "transport": "embedded",
    "serverClass": "jo.celeris.serveur.java.JavaLanguageServer",
    "clientInterface": "org.eclipse.lsp4j.services.LanguageClient",
    "clientName": "celeris-demo",
    "initializationOptions": { }
  }]
}
```

## Bancs et cliquets

Le banc de bout en bout monte le serveur sur des tubes à file et le
rejoint par la session cliente de `lsp-protocole` : les deux bouts de
la même bibliothèque se prouvent l'un à l'autre — poignée de main,
diagnostics poussés, complétion, survol, AIDE AUX PARAMÈTRES, symboles,
plis, formatage, et le montage PAR DÉSIGNATION (la voie des
déclarations `servers.json`). Couverture JaCoCo ≥ 90 % de lignes.

## Pour aller plus loin

- [ADR-015](../docs/adr/015-client-lsp-sur-lsp4j.md) — la frontière
  protocolaire que le serveur traverse.
- [USAGE.md](../USAGE.md) — serveurs de langage déclarés en données
  (§ 8) : l'exemple d'`servers.json` déclare ce serveur.
