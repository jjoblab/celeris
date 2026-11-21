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
              │  (cli, application IDE hôte, agents, exemple)  │
              └───────┬───────────────────────────┬────────────┘
                      │ API publique (ADR-009)    │ SPI (VCS, débogueur…)
              ┌───────▼───────────┐       ┌───────▼───────────┐
              │   android-view    │       │   android-host    │
              │  vue, rendu, IME, │       │ SPI hôte + popups │
              │  gestes, a11y     │       └───────┬───────────┘
              └───────┬───────────┘               │
              ┌───────▼───────────┐
              │ android-widgets   │
              │ popups, gouttière │
              └───────┬───────────┘
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

Depuis le déménagement des popups du 2026-11-14 (décision du
propriétaire : les fonctionnalités vivent dans les modules de
l'éditeur, jamais chez l'hôte),
`android-host` compose au-dessus d'`android-view`, `android-widgets`
et `core-lsp` : il porte l'orchestration des popups
(`PopupsEditeur`, paquet `jo.celeris.android.host.popups`) — la
l'orchestration vit là, l'hôte reste mince.

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

La seconde voie — l'analyse native par arbre de syntaxe concrète (module
opt-in `core-syntax-treesitter`, ADR-020/0025) — est **bornée à 200 Ko** :
au-delà, l'arbre natif ne survit pas à la passe (la discipline mémoire de
l'ADR — la re-analyse s'y paie au plein, les budgets de l'ADR-004 ne
tiennent que sous la borne) et la publication précédente reste servie.
La borne vit dans le code (`NativeEngine.BORNE_CARACTERES`, en caractères du modèle) ; le dépassement
n'est pas une erreur, c'est un repli silencieux vers la publication
courante de la voie qui sert.

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

### Paquets internes (règle 1 de l'ADR-009, exécutée)

Tout paquet dont un segment se nomme `internal` est INTERNE : ses classes y sont publiques entre elles mais n'entrent
JAMAIS dans le manifeste d'API (le générateur filtre par segment
de paquet). Le mouvement P de la feuille des améliorations y
range les familles de collaboration serrée :

| Paquet | Rôle |
| --- | --- |
| `jo.celeris.android.view.internal` | les parcs d'état par session (moteurs d'analyse, mises en page, positions de défilement) et l'avis de fenêtre |
| `jo.celeris.android.view.internal.{paint,gutter,exec,model,viewport}` | peintres internes, collaborateur de gouttière, exécuteurs d'analyse, modèles de calcul, registre d'écouteurs |
| `jo.celeris.android.view.internal.{input,gestures,frame,caret,accessibility,diagnostics,fonts}` | les familles couplées à la vue (P7) : clavier, gestes, assemblage d'image, ancres, accessibilité, diagnostics, polices |
| `jo.celeris.android.view.internal.access` | le pont `EditorAccess` : la surface exacte que les familles touchent de la vue |
| `jo.celeris.android.widgets.internal` | les superclasses des popups (P8) : PopupPanel, TextRowsPopup, Feuille, NavigationEtages |
| `jo.celeris.core.intelligence.internal.{structure,flow,scan}` | vocabulaire des analyses  : structures, flot de données, balayage d'identifiants |
| `jo.celeris.core.text.internal.{bidi,segmentation}` | passes UAX#9 et UAX#29 |

La règle des symboles publics anglais s'applique AUX CLASSES
DÉPLACÉES : déplacer un interne le publicise dans son
paquet — les noms français ont été renommés d'abord (P4 :
ExtendsClauses, ProjectFile, IdentifierScan). Les familles qui
touchent des champs package-privés de la vue (clavier, gestes,
IME, diagnostics, fantômes) restent au paquet racine :
les déplacer exigerait de publiciser ces champs — décision du
propriétaire (feuille des améliorations, décision n°  1).


## 4. Ce que l'éditeur n'est pas

L'éditeur ne compile pas, n'exécute pas, ne débogue pas : il expose les
interfaces où l'hôte se branche (points d'arrêt, repères d'exécution,
diagnostics de build, traces liées). Cette frontière est la garantie que le
composant reste embarquable et que l'exemple consommateur suffit
à prouver l'API.
