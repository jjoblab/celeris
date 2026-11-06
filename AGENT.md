# AGENT.md — guide pour les agents IA et contributeurs

Document court et factuel : uniquement ce qui a été vérifié dans ce dépôt.
Les décisions structurantes vivent dans les ADR (`docs/adr/`), validées par
le propriétaire le 2026-11-04 ; le point d'entrée du cadrage est
[`docs/RESUME-M0.md`](docs/RESUME-M0.md), l'avancement courant dans
[`docs/RESUME-M6.md`](docs/RESUME-M6.md).

## Vue d'ensemble

Réécriture complète de l'éditeur de code embarquable : **Java 17**
(artefacts) construit sur **JDK 21**, rendu Canvas, onze modules plus la
démonstration (ADR-002, la voie de grammaires s'y ajoute à M4) :

| Module | Nature | Rôle |
| --- | --- | --- |
| `core-text` | JVM pure | modèle de texte, instantanés, transactions (ADR-003) |
| `core-syntax` | JVM pure | arbre syntaxique incrémental (ADR-004) |
| `core-syntax-grammars` | JVM pure | voie de grammaires, longue traîne (ADR-004) |
| `core-language` | JVM pure | contrats de langage |
| `core-intelligence` | JVM pure | index, complétion, refactorisations |
| `core-lsp` | JVM pure | client LSP (ADR-005) |
| `core-ai` | JVM pure | contrats IA, outils d'agent (ADR-012) |
| `android-view` | Android | vue Canvas, rendu, IME, accessibilité |
| `android-widgets` | Android | popups, gouttière, barres |
| `android-host` | Android | adaptateurs hôte (SPI) |
| `cli` | application JVM | exécutable sans Android |
| `demo` | application Android | terrain de démonstration (non publiée) |

## Commandes (vérifiées)

```bash
export JAVA_HOME=<jdk-21>            # javac exigé
./gradlew check                      # politique + qualité de chaque module
./gradlew :core-text:check           # un module pur
./gradlew :core-text:mutationThreshold   # mutation (seuil 80 %)
./gradlew publishToMavenLocal publicationChecksums   # publication locale
./gradlew :cli:run                   # fumée de la cli
```

Sans SDK Android (ANDROID_HOME, ANDROID_SDK_ROOT ou local.properties),
les modules Android ne sont pas inclus : `check` reste vert sur les
modules purs — c'est le critère de sortie M1, pas une dégradation. La CI
construit l'ensemble avec SDK.

## Conventions

- **Commentaires, Javadoc et documentation en français** ; identifiants
  et messages d'exception en anglais, ASCII (contrôlé par
  `scripts/check-language-policy`).
- Typographie française des fichiers Markdown : espaces insécables
  devant « ; », « : », « ! », « ? » (le script propose `--fix`).
- Commits : `feat(scope):` / `fix(scope):` / `chore(scope):` / `docs:`,
  un commit par changement, sujet et corps explicatifs, en français.
- Tout correctif de bug arrive avec un test qui échoue avant et passe
  après.
- Toute version vit dans `gradle/libs.versions.toml` — une version en
  dur dans un module casse la CI.
- `CHANGELOG.md` : une entrée par version, court par construction.

## Portes qualité (toutes bloquantes)

`check` enchaîne : politique de langue et neutralité (scripts), Checkstyle
(plafonds 600 lignes par fichier, 50 par méthode), PMD (complexité
cyclomatique 15), Error Prone + NullAway (`@NullMarked` JSpecify), tests
(Jupiter + jqwik + ArchUnit), couverture JaCoCo (≥ 90 % de lignes sur les
modules purs), manifeste d'API (`api/<module>.api`, régénérer par
`./gradlew apiManifest` après toute évolution voulue de l'API).

## Pièges constatés dans ce dépôt

- La mutation (PIT) ne cible que le code de production : les classes de
  test sont exclues, le seuil ne se prononce pas sur le vide.
- Javadoc échoue « aucune classe à documenter » sur un module qui ne
  contient que des package-info : toléré tant qu'il n'y a pas de classe
  réelle, bloquant ensuite.
- Le wrapper Gradle est épinglé par somme de contrôle
  (`gradle/wrapper/gradle-wrapper.properties`) : ne jamais changer la
  distribution sans mettre à jour la somme.
- PMD 7 a retiré `ExcessiveClassLength` et `ExcessiveMethodLength` (les
  plafonds 600/50 vivent dans Checkstyle) : une référence de règle
  introuvable laisse la porte tourner À VIDE (« No files to analyze »,
  tâche verte) — le jeu de règles ne doit référencer que des règles
  existantes pour la version épinglée.
- La mutation ne cible que le code de production
  (`--mutableCodePaths` du source set principal) : le filtre d'exclusion
  par nom ne rattrape ni les classes internes de test ni les utilitaires
  de test.

## Neutralité (règle absolue)

Aucun projet, bibliothèque, éditeur ou dépôt tiers n'est nommé, cité ni
décrit où que ce soit (code, docs, commits, noms de branches). Les
identifiants neutres `[EXT-n]` les désignent dans les comptes rendus.
Seules exceptions : les dépendances réellement déclarées (fichiers de
build, `NOTICE`) et les normes (Android, Java, Gradle, LSP, MCP, Unicode).
Le contrôle `scripts/check-external-refs` tourne en CI avec des motifs
nominatifs lus dans le secret `CELERIS_FORBIDDEN_REFS`.

## Définition de « terminé »

`./gradlew check` vert (politique comprise) · mutation et publication
locales vertes sur les modules concernés · manifestes d'API à jour ·
`CHANGELOG.md` et résumé de jalon à jour · `git status` propre. Le
résultat validé est poussé systématiquement sur `main` (décision du
propriétaire) ; les branches de travail sont fusionnées puis supprimées.

## À ne pas faire

- Toucher à `local.properties`, aux secrets, aux keystores ; committer
  un jeton d'authentification.
- Committer `build/`, `.gradle/`, `*.iml`, `*.hprof`.
- Affaiblir un seuil (Checkstyle, PMD, couverture, mutation) sans ADR
  validé par le propriétaire : les seuils ne sont que des cliquets.
