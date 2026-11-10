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

**M9 — durcissement et 1.0 (livré).** M0 à M8 sont livrés :
moteur de texte, vue Canvas, syntaxe, édition assistée, sémantique,
Android et SPI, IA native, plateforme de langages (M7.6 : seize
langages en données, grammaires réellement TextMate, LSP derrière la
frontière protocolaire). M9 referme le cycle : audit d'accessibilité
complet (auditeur automatique en CI), revue de sécurité (faille de
montage fermée), cliquet de performance calibré, guide de migration
final, puis publication 1.0.0. Les points d'entrée :
[`docs/RESUME-M9.md`](docs/RESUME-M9.md) et
[`docs/feuille-de-route.md`](docs/feuille-de-route.md).

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

## Modules

| Module | Nature | Rôle |
| --- | --- | --- |
| `core-text` | JVM pure | modèle de texte, instantanés, transactions, multi-sélection, annulation |
| `core-syntax` | JVM pure | analyse syntaxique incrémentale, structure (plis, plan, crochets) |
| `core-syntax-grammars` | JVM pure | voie de grammaires TextMate, longue traîne (ADR-014) |
| `core-language` | JVM pure | plateforme de langages : contrats, paquets de données, registre (ADR-017) |
| `core-intelligence` | JVM pure | index de symboles, complétion, refactorings, formatage |
| `core-lsp` | JVM pure | domaine LSP : types immuables, ordonnancement, UTF-16 (ADR-015) |
| `lsp-protocole` | JVM pure | frontière protocolaire du LSP : session, montages embarqués (ADR-015) |
| `core-ai` | JVM pure | contrats IA et API d'outils pour agents |
| `android-view` | bibliothèque Android | vue Canvas, rendu, entrées, IME, accessibilité |
| `android-widgets` | bibliothèque Android | popups, gouttière, barres |
| `android-host` | bibliothèque Android | adaptateurs hôte (système de fichiers, VCS, débogueur) |
| `cli` | application JVM | exécutable sans Android |
| `demo` | application Android | terrain de démonstration complet (non publiée) |

## Documentation

- [`ARCHITECTURE.md`](ARCHITECTURE.md) — vue d'ensemble technique et règles de dépendance.
- [`MIGRATION.md`](MIGRATION.md) — guide de migration du dépôt de départ vers celeris (final à M9).
- [`docs/adr/`](docs/adr/) — décisions d'architecture (ADR-001 à ADR-017).
- [`docs/parite/MATRICE-PARITE.md`](docs/parite/MATRICE-PARITE.md) — référentiel de parité, catégories A à H.
- [`docs/audit/`](docs/audit/) — état des lieux du dépôt de départ, registre de portage, pièges hérités, spikes, audit d'accessibilité et revue de sécurité (M9).
- [`docs/REFERENCES.md`](docs/REFERENCES.md) — projets et normes cités, licences vérifiées.
- [`docs/feuille-de-route.md`](docs/feuille-de-route.md) — jalons M1 à M9 et effort estimé.
- [`docs/GLOSSAIRE.md`](docs/GLOSSAIRE.md) — terminologie française de référence.

## Language / Langue

Code, identifiants et messages d'exception : anglais. Javadoc, commentaires
et documentation : français (typographie comprise). Voir
`scripts/check-language-policy`.

## Licence

Apache-2.0 — voir [`LICENSE`](LICENSE) et [`NOTICE`](NOTICE).
