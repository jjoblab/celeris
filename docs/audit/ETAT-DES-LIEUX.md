# État des lieux mesuré du dépôt de départ

> Document **généré** par `scripts/audit_etat_des_lieux.py` — les chiffres
> sont mesurés, jamais saisis à la main. Regénérer :
> `python3 scripts/audit_etat_des_lieux.py --repo <clone> --out docs/audit/ETAT-DES-LIEUX.md`.


Mesures prises le 2026-11-03 sur le commit `f310a39` (2026-10-30), branche `main` du dépôt de départ, cloné en lecture seule dans un répertoire distinct de ce dépôt.


## 1. Méthode

Toutes les mesures découlent de `git ls-files`, puis de comptages ligne à
ligne (équivalent `wc -l`) et d'expressions rationnelles sur le contenu des
fichiers. Le script n'écrit rien dans le dépôt audité.


## 2. Inventaire

| Mesure | Valeur |
| --- | --- |
| Fichiers versionnés | 846 |
| Fichiers Java | 709 |
| Annotations `@Test` | 3 130 dans 376 fichiers |
| Lignes de test (`src/test`) | 73 307 |
| Entrées de baseline lint figées | 48 |
| Archives binaires versionnées (hors wrapper) | 2 |
| Fichiers `build.gradle.kts` | 7 |
| Gradle (wrapper) / AGP / compileSdk / minSdk | 9.5.1 / 9.0.0 / 34 / 24 |
| Ressources de langues | values |
| Bibliothèque client LSP déclarée | version 0.22.0 |


### 2.1 Code par module (lignes physiques)

| Module | Fichiers prod. | Lignes prod. | Fichiers test | Lignes test |
| --- | --- | --- | --- | --- |
| `cel-core` | 88 | 21 175 | 112 | 21 004 |
| `cel-lsp-api` | 41 | 2 151 | 5 | 498 |
| `cel-lsp` | 45 | 8 283 | 52 | 8 877 |
| `cel-textmate` | 12 | 2 951 | 9 | 2 152 |
| `cel-ui` | 111 | 39 794 | 179 | 35 376 |
| `demo` | 26 | 13 311 | 29 | 5 400 |


### 2.2 Plus gros fichiers

| Fichier | Lignes |
| --- | --- |
| `editor/cel-ui/src/main/java/jo/codeeditor/view/EditorView.java` | 4 579 |
| `editor/cel-core/src/main/java/jo/codeeditor/highlight/tokenizer/TokenizerTemplate.java` | 2 046 |
| `demo/src/main/java/jo/codeeditor/demo/EditorActivity.java` | 1 842 |
| `editor/cel-core/src/main/java/jo/codeeditor/session/EditorSession.java` | 1 694 |
| `editor/cel-ui/src/main/java/jo/codeeditor/view/render/EditorAssistPopupPainter.java` | 1 659 |
| `editor/cel-core/src/main/java/jo/codeeditor/highlight/tokenizer/GenericTokenizer.java` | 1 587 |
| `editor/cel-ui/src/main/java/jo/codeeditor/view/input/EditorInputHandler.java` | 1 313 |
| `editor/cel-ui/src/main/java/jo/codeeditor/view/popup/EditorCompletionPopup.java` | 1 241 |
| `editor/cel-ui/src/main/java/jo/codeeditor/view/popup/EditorPopupAnchors.java` | 1 221 |
| `demo/src/main/java/jo/codeeditor/demo/DemoSamples.java` | 1 198 |
| `editor/cel-core/src/test/java/jo/codeeditor/highlight/tokenizer/GenericTokenizerTest.java` | 1 175 |
| `editor/cel-ui/src/main/java/jo/codeeditor/view/render/EditorChromePainter.java` | 1 167 |

Fichiers de plus de 1 000 lignes : **13** — `editor/cel-ui/src/main/java/jo/codeeditor/view/EditorView.java`, `editor/cel-core/src/main/java/jo/codeeditor/highlight/tokenizer/TokenizerTemplate.java`, `demo/src/main/java/jo/codeeditor/demo/EditorActivity.java`, `editor/cel-core/src/main/java/jo/codeeditor/session/EditorSession.java`, `editor/cel-ui/src/main/java/jo/codeeditor/view/render/EditorAssistPopupPainter.java`, `editor/cel-core/src/main/java/jo/codeeditor/highlight/tokenizer/GenericTokenizer.java`, `editor/cel-ui/src/main/java/jo/codeeditor/view/input/EditorInputHandler.java`, `editor/cel-ui/src/main/java/jo/codeeditor/view/popup/EditorCompletionPopup.java`, `editor/cel-ui/src/main/java/jo/codeeditor/view/popup/EditorPopupAnchors.java`, `demo/src/main/java/jo/codeeditor/demo/DemoSamples.java`, `editor/cel-core/src/test/java/jo/codeeditor/highlight/tokenizer/GenericTokenizerTest.java`, `editor/cel-ui/src/main/java/jo/codeeditor/view/render/EditorChromePainter.java`, `editor/cel-ui/src/main/java/jo/codeeditor/view/render/EditorRenderer.java`.


### 2.3 Baselines lint

| Fichier | Entrées figées |
| --- | --- |
| `demo/lint-baseline.xml` | 23 |
| `editor/cel-core/lint-baseline.xml` | 8 |
| `editor/cel-lsp-api/lint-baseline.xml` | 5 |
| `editor/cel-lsp/lint-baseline.xml` | 6 |
| `editor/cel-ui/lint-baseline.xml` | 6 |

Remarque : l'élément racine `<issues>` est exclu du comptage. Un comptage
naïf par ligne (le chiffre initialement annoncé) surestime le total d'une
unité par fichier.


### 2.4 Concurrence et scellés

| Motif | Occurrences | Fichiers |
| --- | --- | --- |
| `@RestrictTo` (bibliothèques) | 310 | 56 |
| `volatile` (bibliothèques) | 146 | 47 |
| `AtomicXxx` (bibliothèques) | 158 | 37 |
| `Executors.newXxx` (bibliothèques) | 34 | 32 |


## 3. Constats structurants vérifiés

| # | Constat | Verdict | Preuve mesurée |
| --- | --- | --- | --- |
| 1 | Modules « purs » exigeant le SDK Android | **confirmé** | 5 modules `com.android.library`, aucun `java-library` : aucun test du moteur sans SDK |
| 2 | Classe dieu `EditorView` | **confirmé** | 4 579 lignes, dont 494 déclarent un membre `public` ; 13 fichiers de plus de 1 000 lignes |
| 3 | Frontières internes faibles | **confirmé** | 310 annotations `@RestrictTo` dans 56 fichiers de bibliothèque |
| 4 | Aucun arbre syntaxique ; sémantique hors bibliothèque | **confirmé** | 19 fichiers de tokéniseurs en production ; l'intelligence Java (22 fichiers, 9 301 lignes) vit dans la démo, pas dans les bibliothèques |
| 5 | Sélection mono-curseur | **confirmé** | `Selection` : deux champs `int` (`start`, `end`), aucun modèle multi-curseur |
| 6 | Accessibilité absente de la vue principale | **confirmé** | `AccessibilityNodeInfo` : 0 fichier ; `contentDescription` : 2 fichiers (`BreadcrumbBar.java`, `EditorSearchBar.java`) — la vue principale n'expose rien au lecteur d'écran |
| 7 | Tests non représentatifs de l'appareil | **confirmé** | 187 fichiers Robolectric ; 0 fichier(s) sous `src/androidTest/` |
| 8 | Dérive documentaire | **confirmé** | README annonce la version 4.14.1 ; ARCHITECTURE annonce `EditorView` ≈ 3960 lignes (mesuré : 4 579) et 2610 tests ; AGENT.md annonce 3185 tests (mesuré : 3 130 `@Test`) ; CHANGELOG : 234 798 octets |
| 9 | Build dupliqué | **confirmé** | 7 `build.gradle.kts`, dont 6 blocs de publication dupliqués |
| 10 | Concurrence répartie sans modèle unique | **confirmé** | `volatile` : 47 fichiers ; `AtomicXxx` : 37 fichiers ; `Executors` : 32 fichiers |


## 4. Acquis à préserver (vérifiés)

| Acquis | État constaté |
| --- | --- |
| Rope du moteur | `395` lignes, imports uniquement `java.*` : portable en JVM pure sans retouche (re-mesurée, voir `docs/audit/SPIKES.md`)
| Voie de mutation unique | `EditorSession.doReplaceRange` / `replaceRangeWithCaret` : point d'entrée commun des mutations, undo compris
| Kit de popups purs | `TextLayoutKit`, `PopupPlacement`, `PopupCoordinator` : logique de placement sans dépendance Android
| Accessibilité | `AccessibilityNodeInfo` absent des fichiers Java ; `contentDescription` limité aux barres de recherche et de fil d'Ariane |
| Ponts IME | `ImeBridge` (cœur) et `EditorImeBridge` (vue), par niveau d'API |
| Pièges connus | sept pièges consignés dans `AGENT.md`, convertis en tests dans `docs/audit/PIEGES-HERITES.md`
| CI aux actions épinglées par SHA | workflows du dépôt de départ avec validation du wrapper : pratique reprise telle quelle (voir ADR-011)
| Catalogue de versions unique | `gradle/libs.versions.toml` fait autorité ; le refus délibéré d'une montée de version y est documenté (utile pour ADR-005)


## 5. Écarts avec les chiffres du mandat

Le mandat annonçait des chiffres à revérifier ; voici les mesures réelles.

| Mesure | Annoncé | Mesuré |
| --- | --- | --- |
| Annotations `@Test` | 2 873 | **3 130** |
| Fichiers de test | 357 | **376** |
| Lignes de test | ≈ 68 000 | **73 307** |
| Entrées de baseline | 53 | **48** (racine `<issues>` exclue) |
| CHANGELOG | ≈ 235 000 octets | **234 798 octets** |

Les écarts sur les tests s'expliquent par l'évolution du dépôt ; les lignes de
production par module mesurées correspondent en revanche exactement aux
chiffres du mandat. Le reste du présent document ne cite que des valeurs
mesurées.
