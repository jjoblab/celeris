# ADR-002 — Topologie des modules et règles de dépendance

- **Statut** : Acceptée (validée par le propriétaire le 2026-11-04)
- **Date** : 2026-11-04

## Contexte

L'audit du dépôt de départ (`docs/audit/ETAT-DES-LIEUX.md`, constats 1, 3 et
9) mesure la racine du problème : cinq modules « purs » déclarés
`com.android.library` (aucun test du moteur sans SDK Android), 310
annotations `@RestrictTo` partageant l'état interne entre sous-paquets, et
sept `build.gradle.kts` dupliqués. Les frontières y étaient conventionnelles
; elles doivent devenir mécaniques.

## Décision proposée

Adopter la topologie imposée par le mandat, à dix modules plus la
démonstration :

```
core-text          java-library        modèle de texte, instantanés, transactions,
                                        multi-sélection, annulation
core-syntax        java-library        arbre syntaxique incrémental, structure
core-syntax-grammars java-library      voie de grammaires pour la longue traîne
                                        (ajouté à M4, ADR-004)
core-language      java-library        contrats de langage, modèles, registre
core-intelligence  java-library        index de symboles, complétion, refactorings
core-lsp           java-library        client LSP (protocole et transport)
core-ai            java-library        contrats IA et API d'outils pour agents
android-view       com.android.library vue Canvas, rendu, entrées, IME, a11y
android-widgets    com.android.library popups, gouttière, barres
android-host       com.android.library adaptateurs hôte (FS virtuel, VCS, débogueur)
cli                application JVM     exécutable sans Android
demo               application Android terrain de démonstration (non publiée)
```

Règles :

1. **Les dépendances ne vont que vers le bas.** `core-*` ne déclarent aucune
   dépendance Android. Vérifié mécaniquement :
   - frontières de modules Gradle (`java-library` contre `android.library`) ;
   - tests ArchUnit en CI sur chaque module pur : interdiction des imports
   `android.*` et des dépendances montantes ;
   - le module `demo` ne voit que l'API publique.
2. **Plus aucun `@RestrictTo` de partage** : les classes sont soit publiques
   (API, ADR-009), soit de visibilité paquet. L'outil de compatibilité
   binaire (ADR-009) est le seul garde-fou de l'API exposée.
3. **Plafonds de taille vérifiés en CI** : 600 lignes par classe, 50 par
   méthode, complexité cyclomatique 15 (Checkstyle et PMD). Aucun cliquet
   relevable : toute exception exige un ADR dédié. L'audit mesure
   `EditorView` à 4 579 lignes dans le dépôt de départ — la refonte se fait
   par composition (un orchestrateur et des collaborateurs), jamais par
   héritage de comportement.
4. Les plugins de convention de `build-logic` définissent **une seule fois**
   la publication, le lint et les options `javac` (fin du copier-coller).

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| A. Dix modules séparés (proposition) | **retenue** | frontières mécaniques, testabilité JVM, CLI et IA dès l'origine |
| B. Regrouper les `core-*` en un seul module | écartée | refusionne les frontières que l'audit montre insuffisantes ; l'index, le LSP et l'IA n'ont pas le même rythme ni les mêmes dépendances |
| C. Conserver la topologie du dépôt de départ | écartée | constats 1, 3 et 9 de l'audit |

## Compromis

- Dix modules ont un coût d'outillage initial (conventions, catalogue,
  CI matricielle) payé à M1, amorti ensuite.
- Les frontières strictes imposent des interfaces explicites là où le dépôt
  de départ lisait les champs d'un voisin : plus de code colle, moins de
  couplage caché.

## Conséquences

- `./gradlew check` doit passer sur les modules purs **sans SDK Android**
  installé (critère de sortie M1).
- Toute dépendance montante détectée par ArchUnit casse la CI.
- Le nombre de fichiers augmente mécaniquement (plafond 600 lignes) ;
  l'audit du dépôt de départ montre le prix inverse : un fichier de 4 579
  lignes dont 494 membres publics.

## Actions

- [x] M1 : squelette des dix modules + `build-logic` + tests ArchUnit.
- [x] M1 : règles Checkstyle/PMD des plafonds, actives dès le premier commit.
