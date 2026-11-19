# ADR-0025 — La voie tree-sitter consomme les artefacts Maven de la liaison Android

> **Statut : acceptée** (décision du propriétaire, 2026-11-18 :
> « pour tree sitter, je pense à utiliser
> `com.itsaky.androidide.treesitter`, regarde comment [l'éditeur de
> référence] a fait » — la méthode de l'éditeur de référence a été
> étudiée : module bibliothèque Android, dépendance `api` sur l'artefact
> cœur, règles de conservation R8 embarquées ; le nom de l'éditeur
> inspireur ne paraît jamais dans le dépôt, règle 7 du jalon M7.6).
> **Elle révisionne l'ADR-0024 (décision 1)** : le module ne porte
> plus sa liaison JNI maison.

## Contexte

La voie tree-sitter de l'ADR-0024 vit par une liaison JNI **écrite
ici** (`natif/liaison.c`) : la tâche optative `natif` télécharge le
runtime C (v0.27.2) et les grammaires épinglées, gcc assemble
`libliaison.so` — jamais empaquetée, chargée uniquement par la
propriété `celeris.natif.lib` au banc JVM (C4). Le résultat : une voie
qui ne se prouve **jamais** chez un consommateur réel — les fabriques
rendent toujours `null` dans tout artefact publié, la « voie native »
n'existe que pour le banc du dépôt qui compile sa bibliothèque.

Le projet AndroidIDE publie sur Maven Central
(`com.itsaky.androidide.treesitter`, licence **LGPL-2.1**) la liaison
Android de tree-sitter : l'artefact cœur `android-tree-sitter`
(`TSParser`, `TSLanguage`, `TSTree`, `TSNode`, `TSInputEdit`,
`TSPoint`, `TreeSitter.loadLibrary()`) et une grammaire par artefact
(`tree-sitter-json`, `tree-sitter-python`, …) — chaque AAR embarque
ses `.so` pour les quatre ABI courantes (arm64-v8a, armeabi-v7a, x86,
x86_64). Vérifié le 2026-11-18 sur `repo1.maven.org` (POM et AAR
ouverts) : dernière version **4.3.3**, packaging AAR, dépendance
transitive unique (`annotations`, runtime). **Il n'existe PAS
d'artefact `tree-sitter-javascript`** — les grammaires publiées sont
Java, JSON, Kotlin, Logs, Python, XML (et C, C++, AIDL, properties).

## Décision

1. **Le module `core-syntax-treesitter` devient une bibliothèque
   Android** (`com.android.library`, minSdk 26, publié en AAR comme
   les autres modules Android, ADR-011). Un AAR ne se résout pas
   depuis un module JVM pur : c'est la contrainte qui décide, pas une
   mode. Il consomme par Maven :
   `com.itsaky.androidide.treesitter:android-tree-sitter:4.3.3`
   (cœur, `implementation` — aucun type de la liaison ne paraît dans
   l'API du module), `tree-sitter-json:4.3.3` et
   `tree-sitter-python:4.3.3` (grammaires servies). Les `.so` suivent
   les AAR : **l'hôte qui embarque le module reçoit la voie native
   complète, toutes ABI** — le dépôt ne compile plus aucun C.
2. **La liaison maison part** : `natif/liaison.c`, `natif/construire`
   et la tâche `natif` (celle du module ET la résiduelle de
   `core-syntax`), la propriété `celeris.natif.lib` et la Javadoc
   « réservée au banc JVM » (C4) — le problème qu'elles bornaient
   (une `.so` jamais livrée) n'existe plus. `LiaisonArbre` garde son
   nom et son rôle de porteur package-private : il charge les
   bibliothèques de la liaison (`TreeSitter.loadLibrary()` puis les
   blocs statiques des classes de grammaire) ; **sans Android
   (banc JVM), le chargement échoue proprement, `presente()` rend
   false et les bancs natifs se sautent** (`assumeTrue`) — la voie se
   prouve sur appareil, les bancs JVM gardent la mécanique pure.
3. **Les positions passent au modèle du dépôt** : la liaison Android
   travaille en unités UTF-16×2 (« JS units ») ; `LiaisonArbre`
   divise par deux au moment d'aplatir — l'arbre aplati porte des
   offsets CARACTÈRES, la table octet→caractère de
   `MaterialisationNatif` disparaît, l'édition incrémentale
   (`TSInputEdit.create`) se pose directement en offsets du modèle.
4. **JavaScript quitte le registre de la voie** : aucune grammaire
   publiée ne le porte (vérifié ci-dessus) — `SyntaxeNatif.javascript()`
   est retiré (module opt-in, zéro consommateur, fenêtre 2.0.0) ; le
   paquet `javascript` garde son repli TextMate déclaré, la préférence
   `tree-sitter` retombe dessus. La liste servie : **json, python**.
5. **Règles de conservation R8 embarquées** (méthode de l'éditeur de
   référence) : `consumer-rules.pro` garde
   `-keep class com.itsaky.androidide.treesitter.** { *; }` — les
   méthodes natives sont appelées par nom depuis le code natif.
6. **Le désucrage suit la contrainte de l'AAR** : les métadonnées de
   `android-tree-sitter` exigent `coreLibraryDesugaringEnabled` — le
   module l'active et `NOTICE` consigne la dépendance LGPL-2.1 au
   runtime publié ; `docs/REFERENCES.md` porte la provenance.
7. **Le manifeste `api/core-syntax-treesitter.api` part** : les
   modules Android n'en portent pas (ADR-009) ; la vérification passe
   par tests, lint, Checkstyle et PMD, comme `android-view`.

## Conséquences

- La voie devient **réelle pour l'hôte** : embarquer
  `core-syntax-treesitter` suffit (les `.so` et la liaison suivent) ;
  la préférence `tree-sitter` d'un paquet json/python se met à
  servir sur appareil. USAGE § 3.1 bis documente le désucrage exigé.
- La charge de la preuve JVM disparaît (plus de tâche `natif`, plus
  de gcc) ; la preuve appareil reste à écrire (banc d'instrumentation
  ou observation sur le module `sample`) — ouverte, assumée.
- LGPL-2.1 au runtime d'un artefact publié : premier cas du dépôt
  (joni/lsp4j sont MIT/EPL) — `NOTICE` le consigne, l'hôte embarque
  la voie OPT-IN en connaissance de cause.
- L'ABImension : quatre ABI courantes ; pas de riscv64 (aucun AAR
  publié ne la porte — constat, pas un choix du dépôt).
