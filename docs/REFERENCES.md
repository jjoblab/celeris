# REFERENCES — projets et normes cités

> Depuis M7.6, les projets, IDE, éditeurs, bibliothèques, normes et
> formats tiers se nomment librement dans le dépôt : une mention est
> factuelle et sourcée. Deux projets restent interdits sans exception
> (règle 7 du jalon) — leurs noms ne paraissent ni ici ni nulle part
> dans le dépôt ; le contrôle `scripts/check-forbidden-refs` et le
> contrôle de provenance `scripts/check-dependency-provenance`
> s'appuient sur le secret de CI `CELERIS_FORBIDDEN_REFS`.
>
> Licences vérifiées en ligne le 2026-11-08 (fichiers LICENSE des
> dépôts cités). L'outillage de build et de test est consigné au
> `NOTICE` avec ses licences — il n'est pas répété ici.

## Dépendances et candidates

| Projet | Rôle dans celeris | Licence | Vérifié le |
| --- | --- | --- | --- |
| [LSP4J](https://github.com/eclipse-lsp4j/lsp4j) | dépendance du runtime publié `lsp-protocole` (M7.6, ADR-015) : protocole LSP et JSON-RPC derrière la frontière | EPL-2.0 OU BSD-3-Clause (consommable sous BSD-3-Clause, compatible Apache-2.0) | 2026-11-09 |
| [joni](https://github.com/jruby/joni) | dépendance du runtime publié `core-syntax-grammars` (M7.6, ADR-014) : portage Java d'Oniguruma, moteur des expressions des grammaires TextMate | MIT | 2026-11-08 |
| [jcodings](https://github.com/jruby/jcodings) | rouage d'encodages de joni (UTF-8 seul sert), même module publié | MIT | 2026-11-08 |
| [tree-sitter](https://github.com/tree-sitter/tree-sitter) | candidate (M7.6, spike) : moteur d'analyse incrémental en C, niveau 2 de l'analyse structurelle par liaison JNI écrite ici | MIT | 2026-11-08 |
| [Gson](https://github.com/google/gson) | rouage JSON de la bibliothèque LSP derrière la frontière `lsp-protocole` (M7.6, ADR-015), épinglée STRICTEMENT : épinglée pour la maîtrise de l'octet du runtime | Apache-2.0 | 2026-11-09 |

## Points de comparaison

| Projet | Rôle dans celeris | Licence | Vérifié le |
| --- | --- | --- | --- |
| Android Studio | l'étalon de la matrice de parité, en toutes lettres | propriétaire (aucun code repris) | 2026-11-08 |
| [VS Code](https://github.com/microsoft/vscode) | formats suivis : `language-configuration.json`, thèmes `tokenColors`, gabarits de fragments ; référence d'éditeur | MIT (aucun code repris) | 2026-11-08 |
| Plateforme IntelliJ | point de comparaison d'architecture : points d'extension et langages déclarés par données | termes open source JetBrains (aucun code repris) | 2026-11-08 |

## Normes et formats

| Norme | Rôle dans celeris | Licence de la spécification | Vérifié le |
| --- | --- | --- | --- |
| [Language Server Protocol](https://microsoft.github.io/language-server-protocol/) | le protocole des serveurs de langage (3.17/3.18) | CC-BY-4.0 | 2026-11-08 |
| Format de grammaires TextMate | le format chargé par le moteur de `core-syntax-grammars` (M7.6 : compatibilité réelle — joni, captures en objets, inclusions résolues) ; chaque grammaire re porte la sienne, consignée au paquet et au `NOTICE` | par grammaire | 2026-11-08 |
| [Oniguruma](https://github.com/kkos/oniguruma) | la sémantique d'expressions rationnelles que suppose le format TextMate (joni en est le portage Java) | BSD-2-Clause | 2026-11-08 |
| Model Context Protocol (MCP) | la compatibilité de l'API d'outils de l'IA native (ADR-012) | spécification ouverte | 2026-11-08 |
| Unicode (UAX #29, UAX #9) | groupes de graphèmes et algorithme bidirectionnel du cœur | spécification ouverte | 2026-11-08 |
| UTF-16, XML, JSON, TOML, YAML | encodage interne et formats édités | normes ouvertes | 2026-11-08 |

## Règle de mise à jour

Toute nouvelle dépendance ou donnée reprise (grammaire, thème) ajoute
sa ligne ici, au `NOTICE` et, le cas échéant, au registre de portage —
avec sa licence vérifiée en ligne à la date du jour et sa provenance
contrôlée par `scripts/check-dependency-provenance`.
