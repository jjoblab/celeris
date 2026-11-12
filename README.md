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

**1.0.0 livrée le 2026-11-10 (publication locale) ; 1.1.0 en chantier.**
Les jalons M0 à M9 sont bouclés : moteur de texte, vue Canvas, syntaxe,
édition assistée, sémantique, Android et SPI, IA native, plateforme de
vingt-quatre langages (grammaires réellement TextMate, LSP derrière la
frontière protocolaire), durcissement. Les chantiers post-1.0 suivent :
dix clos (C2, C3, C4, C6, C7, C8, C9, C10, C12, C13), un dort sur le
signal du propriétaire (C5 — langages suivants), trois attendent un moyen
extérieur (C1 — publication distante, C11 — appareil de référence,
C14 — archivage du dépôt de départ). Les étapes restantes vivent dans
[`docs/feuille-de-route-restante.md`](docs/feuille-de-route-restante.md) ;
l'historique des chantiers et le journal des clôtures dans
[`docs/feuille-de-route.md`](docs/feuille-de-route.md).

## Publication

Douze artefacts Maven, groupe `jo.celeris`, version 1.0.0 — chacun avec son
jar (ou son AAR pour les bibliothèques Android, depuis le 2026-11-11), ses
sources, sa Javadoc, son SBOM CycloneDX et sa somme SHA-256 (ADR-011) :
`core-text`, `core-syntax`, `core-syntax-grammars`, `core-language`,
`core-intelligence`, `core-lsp`, `core-ai`, `lsp-protocole`,
`serveur-lsp-java` (JVM pure) ;
`android-view`, `android-widgets`, `android-host` (AAR). La publication
distante (registre de l'hébergeur) attend les secrets du propriétaire —
chantier C1. Coordonnées et déclaration des dépendances :
[`USAGE.md`](USAGE.md) (§ 2, § 3).

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

## Charge et limites

Les budgets mesurés — bancs de la CI, tas dédié de 1 Go, minimum de
trois tours (calibrages consignés dans les bancs) :

| Charge | Budget | Mesuré |
| --- | --- | --- |
| Édition, 50 000 lignes | ≤ 1 ms | 0,145 ms |
| Première image, 100 000 lignes | ≤ 500 ms | 154 ms (texte), 224 ms (analyse par fenêtre) |
| Ouverture, 480 000 lignes (12,1 M caractères) — la fenêtre d'abord | ≤ 2 500 ms | 1214–1441 ms, tas +90 Mo |
| Édition au milieu, 480 000 lignes — l'incrément | ≤ 100 ms | 2–36 ms |

Le défilement ne coûte rien : la fenêtre visible se note, le travail
en vol ne s'annule pas ; l'analyse vit hors fil
([ADR-007](docs/adr/007-modele-de-threading.md)) — la fenêtre d'abord,
la complétion par lots ensuite. Au-delà des seuils « gros document »
du dépôt de départ (2,5 M caractères ou 50 000 lignes), le régime des
fonctions lourdes coupe la complétion au curseur — l'édition reste
entière : coloration, frappe et annulation ne se coupent jamais.
Tri-état à la manière du dépôt de départ :
`editorView.intelligence().setLargeFileMode(AUTO | ON | OFF)` — AUTO
(défaut) suit les seuils du document, ON force les coupures, OFF les
ferme même au-delà des seuils.

## Bancs et cliquets

326 bancs, 2 048 méthodes `@Test` (relevé du 2026-11-11, les treize
modules compris). Le volume n'est pas un but en soi (décision du
propriétaire du 2026-11-11 : la parité de volume avec l'audit du dépôt
de départ ne se poursuit pas) — les cliquets font foi : couverture
JaCoCo ≥ 90 % de lignes sur les modules purs, score de mutation ≥ 80 %
sur `core-text` (ADR-010), plafonds Checkstyle/PMD (600/800 lignes par
fichier, ADR-002/019 ; complexité 15), manifestes d'API vérifiés à
chaque construction (ADR-009).

## Modules

Chaque module porte son propre `README.md` (rôle, coordonnées, bancs,
portes) :

| Module | Nature | Rôle |
| --- | --- | --- |
| [`core-text`](core-text/README.md) | JVM pure | modèle de texte, instantanés, transactions, multi-sélection, annulation |
| [`core-syntax`](core-syntax/README.md) | JVM pure | analyse syntaxique incrémentale, structure (plis, plan, crochets) |
| [`core-syntax-grammars`](core-syntax-grammars/README.md) | JVM pure | voie de grammaires TextMate, longue traîne (ADR-014) |
| [`core-language`](core-language/README.md) | JVM pure | plateforme de langages : contrats, paquets de données, registre (ADR-017) |
| [`core-intelligence`](core-intelligence/README.md) | JVM pure | index de symboles, complétion, refactorings, formatage |
| [`core-lsp`](core-lsp/README.md) | JVM pure | domaine LSP : types immuables, ordonnancement, UTF-16 (ADR-015) |
| [`lsp-protocole`](lsp-protocole/README.md) | JVM pure | frontière protocolaire du LSP : session, montages embarqués (ADR-015) |
| [`serveur-lsp-java`](serveur-lsp-java/README.md) | JVM pure | mini serveur de langage Java : compagnon de banc de l'éditeur (ADR-015) |
| [`core-ai`](core-ai/README.md) | JVM pure | contrats IA et API d'outils pour agents |
| [`android-view`](android-view/README.md) | bibliothèque Android | vue Canvas, rendu, entrées, IME, accessibilité, gouttière |
| [`android-widgets`](android-widgets/README.md) | bibliothèque Android | kit de popups (ADR-018), complétion, recherche, plan, minimap, défilement rapide |
| [`android-host`](android-host/README.md) | bibliothèque Android | adaptateurs hôte (SPI) : fichiers, VCS, exécution, débogueur, historique local |
| [`cli`](cli/README.md) | application JVM | exécutable sans Android (diagnostiquer, formater, refactorer) |
| [`demo`](demo/README.md) | application Android | terrain de démonstration complet |

Douze des quatorze modules se publient en artefacts Maven
(`jo.celeris:*`, section « Publication ») ; la cli et la démo sont des
applications, jamais publiées.

## Documentation

- [`USAGE.md`](USAGE.md) — guide d'intégration hôte : dépendances, montage de la vue, langages, serveurs de langage, popups, SPI, contrats IA, budgets (chantier C4).
- [`ARCHITECTURE.md`](ARCHITECTURE.md) — vue d'ensemble technique et règles de dépendance.
- [`MIGRATION.md`](MIGRATION.md) — guide de migration du dépôt de départ vers celeris (final à M9).
- [`docs/adr/`](docs/adr/) — décisions d'architecture (ADR-001 à ADR-019).
- [`docs/feuille-de-route-restante.md`](docs/feuille-de-route-restante.md) — les étapes restantes (état vérifié au 2026-11-11) ; [`docs/feuille-de-route.md`](docs/feuille-de-route.md) — l'historique des chantiers post-1.0 et des jalons M1 à M9, avec le journal des clôtures.
- [`docs/parite/MATRICE-PARITE.md`](docs/parite/MATRICE-PARITE.md) — référentiel de parité, catégories A à H.
- [`docs/audit/`](docs/audit/) — état des lieux du dépôt de départ, registre de portage, pièges hérités, spikes, audit d'accessibilité et revue de sécurité (M9), analyse comparative avec le dépôt de départ.
- [`docs/REFERENCES.md`](docs/REFERENCES.md) — projets et normes cités, licences vérifiées.
- [`docs/maquettes/popups.html`](docs/maquettes/popups.html) — maquettes réactives des popups (ADR-018) : feuille du bas, feuille du haut, popup ancrée.
- [`docs/GLOSSAIRE.md`](docs/GLOSSAIRE.md) — terminologie française de référence.

## Language / Langue

Code, identifiants et messages d'exception : anglais. Javadoc, commentaires
et documentation : français (typographie comprise). Voir
`scripts/check-language-policy`.

## Licence

Apache-2.0 — voir [`LICENSE`](LICENSE) et [`NOTICE`](NOTICE).
