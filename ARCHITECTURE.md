# Architecture — celeris

> Document de cadrage (M0) : il décrit l'architecture **imposée par le
> mandat** et raffinée par les ADR correspondants, qui restent seuls maîtres
> des décisions tant qu'ils ne sont pas validés. Ce fichier sera tenu à jour
> à chaque jalon ; les chiffres (tests, tailles) restent générés, jamais saisis.

## 1. Vue d'ensemble

L'éditeur est un composant embarquable : il fournit le moteur de texte,
l'analyse syntaxique, l'intelligence du langage, la vue et les contrats
d'intégration. La synchronisation Gradle, la compilation, l'exécution,
l'émulateur, le profileur, le débogage et l'inspection de disposition restent
du ressort de l'application hôte, qui se branche par des interfaces de
service (SPI).

Le principe directeur hérité de l'audit du dépôt de départ : **le cœur ne
connaît pas Android**. Tout ce qui est testable sur JVM pure vit sous la
ligne de flottaison ; la vue n'est qu'une des consommatrices du cœur.

```
              ┌────────────────────────────────────────────────┐
              │              applications hôtes                │
              │   (démo, application IDE hôte, cli, agents)    │
              └───────┬───────────────────────────┬────────────┘
                      │ API publique (ADR-009)    │ SPI (VCS, débogueur…)
              ┌───────▼───────────┐       ┌───────▼───────────┐
              │   android-view    │       │   android-host    │
              │  vue, rendu, IME, │       │ adaptateurs hôte  │
              │  gestes, a11y     │       └───────┬───────────┘
              └───────┬───────────┘               │
              ┌───────▼───────────┐       ┌───────▼───────────┐
              │ android-widgets   │       │      demo         │
              │ popups, gouttière │       │  (application)    │
              └───────┬───────────┘       └───────────────────┘
                      │        ┌──────────────────────────────┐
              ┌───────▼────────▼──────────┐                   │
              │ core-intelligence         │  core-lsp         │
              │ index, complétion,        │  client LSP       │
              │ refactorings, formatage   │                   │
              └───────┬───────────────────┘        ┌──────────▼────────┐
              ┌───────▼───────────┐                │     core-ai       │
              │  core-language    │                │ contrats IA,      │
              │ contrats provider │                │ outils d'agent    │
              └───────┬───────────┘                └──────────┬────────┘
              ┌───────▼───────────┐                           │
              │   core-syntax     │◄──────────────────────────┘
              │ arbre incrémental │
              └───────┬───────────┘
              ┌───────▼───────────┐
              │    core-text      │  ← tout repose sur le modèle de texte
              └───────────────────┘
```

Les dépendances ne vont que vers le bas. Des tests d'architecture
(ArchUnit) figent la règle en CI dès M1 (ADR-002) : plus aucun scellé
`@RestrictTo` de partage interne — les frontières sont celles du compilateur.

## 2. Les quatre chaînes

### 2.1 Chaîne d'édition

Toute mutation est une `Transaction` immuable, appliquée par **un seul point
d'entrée** sur le fil UI : instantanés immuables produits, multi-sélection et
annulation de première classe, projection des positions pour les couches
supérieures (sélections, diagnostics, plis). Les lecteurs d'arrière-plan
travaillent sur des instantanés sans verrou. L'audit du dépôt de départ a
mesuré sa rope à 0,002 ms par édition d'un caractère sur 2 M de caractères
(`docs/audit/SPIKES.md`) : la marge au budget (1 ms) valide le portage.

### 2.2 Chaîne d'analyse

Un arbre syntaxique concret, sans perte, tolérant aux erreurs, incrémental,
calculé hors fil UI sur un instantané, annulable. La coloration, les plis,
les crochets, l'indentation, la sélection structurelle, le fil d'Ariane et le
plan lisent **le même arbre** — là où le dépôt de départ entretenait
dix-neuf tokéniseurs manuels indépendants. La voie des grammaires (format
TextMate) couvre la longue traîne de langages.

### 2.3 Chaîne de rendu

Une seule vue Canvas ; chaîne de rendu sans allocation par image ;
invalidation par plages de rangées ; caches indexés par des valeurs
immuables ; un pinceau par couche (le piège n° 1 du dépôt de départ) ;
pré-calcul asynchrone des mises en page de lignes ; cible 60 et 120 Hz ;
instrumentation `FrameMetrics`. Le spike de rendu (proxy JVM) mesure un
rapport de ~10^5 entre créer une mise en page et la consulter : le cache par
rangée est une condition d'existence, pas une optimisation.

### 2.4 Chaîne sémantique et IA

Un client LSP moderne (annulation, priorités, fusion, resynchronisation) et
un indexeur léger sur l'appareil ; au-dessus, des contrats IA : texte
fantôme, prédiction d'édition, revue des modifications d'agent, API d'outils
compatible avec les serveurs d'outils. Aucune requête réseau sans
consentement explicite ; les serveurs de langage tournent dans des processus
isolés.

## 3. Règles transverses

| Règle | Détail | ADR |
| --- | --- | --- |
| Threading | écrivain unique (fil UI), lecteurs sur instantanés ; horloge et exécuteurs injectables | ADR-007 |
| Types forts | `DocOffset`, `LineIndex`, `VisualRow`, `Pixel` — jamais des `int` interchangeables | ADR-003 |
| API publique | interne par défaut, nullité JSpecify, compatibilité binaire vérifiée en CI | ADR-009 |
| Taille des classes | 600 lignes, 50 par méthode, complexité 15 — sans cliquet relevable | ADR-002 |
| Accessibilité | arbre virtuel, `en`/`fr`, BiDi et Unicode dès la première version | ADR-008 |
| Qualité | `-Werror`, zéro baseline lint, mutation, fuzz à oracle | ADR-010, ADR-011 |
| Références externes | projets tiers nommés librement et sourcés, deux interdits (règle 7) ; provenance des dépendances vérifiée | ADR-011, M7.6 |

## 4. Ce que l'éditeur n'est pas

L'éditeur ne compile pas, n'exécute pas, ne débogue pas : il expose les
interfaces où l'hôte se branche (points d'arrêt, repères d'exécution,
diagnostics de build, traces liées). Cette frontière est la garantie que le
composant reste embarquable et que la démo suffit à prouver l'API.
