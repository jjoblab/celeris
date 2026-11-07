# celeris

**celeris** (latin : « rapide, vif ») est un éditeur de code embarquable pour
Android : une bibliothèque que toute application hôte intègre pour obtenir
une surface d'édition comparable à celle d'un environnement de développement
de référence — édition multi-curseur, coloration syntaxique, plis,
recherche/remplacement, complétion, refactorings, texte fantôme — pensée
**tactile d'abord**, hors ligne d'abord, et sobre en mémoire comme en batterie.

Le cœur du moteur ne dépend **pas du SDK Android** : il se construit et se
teste sur JVM pure, en quelques secondes. Le même cœur alimente la vue Canvas
Android, l'application de démonstration et un exécutable en ligne de commande.

## État du projet

**M8 — IA native (livrée, en attente de validation du propriétaire).**
M0 à M7 sont livrés : moteur de texte, vue Canvas, syntaxe, édition
assistée, sémantique, Android et SPI. Le jalon M8 clos la catégorie F
de la matrice de parité — texte fantôme, prédiction, revue d'agent,
API d'outils pour agents (surface MCP), exclusions de contexte,
consentement. Les points d'entrée : [`docs/RESUME-M8.md`](docs/RESUME-M8.md)
et [`docs/feuille-de-route.md`](docs/feuille-de-route.md).

## Vie privée

Aucun paquet celeris n'embarque de télémétrie — ni collecte, ni
déclaration d'usage. Les contrats IA du cœur ne connaissent aucun
transport : les modèles se branchent par l'hôte, et rien ne part sans
le consentement explicite de l'utilisateur (`GardeContexte`,
[`docs/adr/012-contrats-ia-agents.md`](docs/adr/012-contrats-ia-agents.md)).
L'hôte qui veut une télémétrie l'ajoute derrière son propre
consentement, anonyme et documentée — jamais dans le cœur. La
démonstration ne déclare d'ailleurs aucune permission réseau : sa
neutralité par défaut est structurelle, vérifiée en CI.

## Modules prévus

| Module | Nature | Rôle |
| --- | --- | --- |
| `core-text` | JVM pure | modèle de texte, instantanés, transactions, multi-sélection, annulation |
| `core-syntax` | JVM pure | analyse syntaxique incrémentale, structure (plis, plan, crochets) |
| `core-language` | JVM pure | contrats de langage (providers), modèles de données, registre |
| `core-intelligence` | JVM pure | index de symboles, complétion, refactorings, formatage |
| `core-lsp` | JVM pure | client LSP (protocole et transport) |
| `core-ai` | JVM pure | contrats IA et API d'outils pour agents |
| `android-view` | bibliothèque Android | vue Canvas, rendu, entrées, IME, accessibilité |
| `android-widgets` | bibliothèque Android | popups, gouttière, barres |
| `android-host` | bibliothèque Android | adaptateurs hôte (système de fichiers, VCS, débogueur) |
| `cli` | application JVM | exécutable sans Android |
| `demo` | application Android | terrain de démonstration complet (non publiée) |

## Documentation

- [`ARCHITECTURE.md`](ARCHITECTURE.md) — vue d'ensemble technique et règles de dépendance.
- [`docs/adr/`](docs/adr/) — décisions d'architecture (ADR-001 à ADR-012, en proposition).
- [`docs/parite/MATRICE-PARITE.md`](docs/parite/MATRICE-PARITE.md) — référentiel de parité, catégories A à G.
- [`docs/audit/`](docs/audit/) — état des lieux mesuré du dépôt de départ, registre de portage, pièges hérités, spikes.
- [`docs/feuille-de-route.md`](docs/feuille-de-route.md) — jalons M1 à M9 et effort estimé.
- [`docs/GLOSSAIRE.md`](docs/GLOSSAIRE.md) — terminologie française de référence.

## Language / Langue

Code, identifiants et messages d'exception : anglais. Javadoc, commentaires
et documentation : français (typographie comprise). Voir
`scripts/check-language-policy`.

## Licence

Apache-2.0 — voir [`LICENSE`](LICENSE) et [`NOTICE`](NOTICE).
