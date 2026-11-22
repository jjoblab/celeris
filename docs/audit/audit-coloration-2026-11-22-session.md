# Audit coloration — session du 2026-11-22 (prompt « retard de coloration et cohérence »)

> Dépôt : `jjoblab/celeris`, branche `main`, base `99a8c839` (tag `v2.2.14`).
> Référence de comparaison : `Rosemoe/sora-editor` (clone local, lecture seule).
> Mesures exécutées sur JVM 17.0.21.1 (Temurin), sandbox Linux 2 CPU / 4 Go.

## 1. État de départ vérifié (mesuré AVANT tout correctif)

Un audit complet (RAPPORT-celeris.md, H1–H15) avait déjà été intégré au dépôt
au 2026-11-21, suivi de la vague de correctifs 2.2.12–2.2.14 : largeur nulle
(H8), budget proportionnel (H9), première analyse sans retenue (H1), retenue
par moteur (H2), chargeur de voie méémorisé (H7), rechargement synchronisé
(H15), langue inchangée no-op (H10), lexeur du rattrapage méémorisé (H6).
Le présent prompt (2026-11-22) demande les chantiers restants A, B, C, D.

Mesures « avant » de cette session (banc JVM reproduit de zéro) :

| Mesure | Valeur (JVM 2 CPU) |
| --- | --- |
| Assemblage des 33 paquets (Grammars.read seul, froid) | 679–835 ms |
| Assemblage complet (tours chauds) | 273–358 ms |
| Paquets avec grammaire | 27/33 (6 par analyseur écrit) |
| Lexeur yaml | 4,3 µs/ligne |
| Lexeur markdown | 5,6 µs/ligne |
| Lexeur python | 27,1 µs/ligne |
| Lexeur html | 100,8 µs/ligne |
| Lexeur css | 65,3 µs/ligne |
| Lexeur csharp | 87,1 µs/ligne |
| Lexeur javascript | 296,1 µs/ligne (2 000 lignes : 593 ms) |

## 2. Chantier A — couleur sans attendre les 33 paquets : ENTIÈREMENT DÉFINI, CORRECTIF PARTIEL

**Banc rouge posé et commité (`b87f628b`)** : `LanguageKitPrioriteTest` —
4 tests, dont `laLangueDuFichierNePatientePasLesAutresPaquets` échoue à
l'état de base (« la résolution a lu 33 paquets ») : preuve que la langue
du fichier ouvert attend l'assemblage complet (constat C5/H3 du prompt, § 4
chantier A).

**Correctif partiel commité (commit suivant)** :
- `PackLoader.descriptorOf(PackFiles)` : lecture de `language.json` SEUL
  (descripteur léger — extensions, filenames, motifs — sans compiler la
  grammaire). C'est la couture « descripteurs d'abord » du chantier A.
- `LanguageDetectionRules` (nouvelle classe pure) : « ce descripteur
  peut-il porter ce nom » — extensions, noms exacts, motifs de nom ;
  décision volontairement LARGE (faux positif admis : le paquet passe en
  tête de file ; faux négatif interdit).
- `LanguageKit.prioriser(filename)` : le nom du fichier ouvert se pose avant
  la résolution ; l'assemblage trié (`sourcePrioritaire`) met les paquets
  candidats EN TÊTE de file, le registre reste complet (aucun paquet écarté).
- Au passage (P6 de l'audit) : suppression du double décodage UTF-8 du texte
  de grammaire dans `PackLoader.assemble`.

**Ce qui reste à faire pour finir le chantier A** : le banc est toujours
rouge parce que la résolution attend le registre COMPLET — réordonner ne
suffit pas. La fin exige le registre bâti des descripteurs seuls (le kit
publie le registre après lecture des 33 `language.json`, la grammaire d'un
langage ne se compilant qu'à la première demande via un `LanguageSupport`
paresseux thread-safe), puis le préchargement du reste en tâche de fond.
Toutes les coutures nécessaires existent maintenant (`descriptorOf`,
`LanguageDetectionRules`, tri prioritaire) — l'étape suivante est le
`LanguageSupport` à syntaxe paresseuse (le prompt l'appelle
`LanguageSupport.syntax()` paresseux et méémorisé).

## 3. Chantiers B, C, D — constats de cette session (non corrigés)

- **B (cache de recherche)** : confirmé par lecture — `GrammarLexer.chercher`
  alloue un `Matcher` joni par règle par position, sans cache « prochaine
  correspondance » (le modèle est `JoniOnigRegExp.search` de tm4e :
  mémoriser ligne/position/résultat, désactiver pour `\G`). L'audit
  existant (F8) chiffre l'attendu : python ×9 → ×2 vs lexeur écrit.
- **C1 (Option.CAPTURE_GROUP)** : confirmé — `Expressions.compiler` compile
  avec `Option.NONE`, tm4e/Sora compilent avec `Option.CAPTURE_GROUP` (+ `Syntax.DEFAULT`,
  `WarnCallback.NONE`).
- **C2 (\A)** : confirmé — 6 grammaires livrées portent `\A` (csharp, css,
  javascript, lua, swift, typescript) ; le lexeur ligne-indépendant le lit
  « début de ligne ». Le modèle tm4e : `buildAnchorCache` avec variantes
  allowA/allowG par ligne 0 / reste du document.
- **C3 (\n en fin de ligne)** : confirmé — 20 grammaires livrées portent des
  motifs à `\n` (lua 39, swift 18, ruby 19, html 7…) ; la ligne lexée ne le
  porte pas.
- **C4 (inclusions externes)** : confirmé — `PackGrammars.compile` appelle
  `Grammars.read(grammarJson)` SANS registre ; `Grammars.read(json, Map)`
  existe déjà et est public. La couture : la fabrique reçoit le registre des
  grammaires par portée racine (lazy), HTML colore `<script>`/`<style>`.
- **C7 (ScopeStacks)** : vérifié — table statique bornée à 50 000 nœuds,
  Javadoc « garde-fou, pas cache » documentée (dégradation silencieuse au
  plafond : le push rend le parent) ; l'audit actuel l'assume, le prompt
  demande un banc de reproductibilité (ordre d'ouverture de N fichiers).
- **D (tree-sitter)** : vérifié — l'API AndroidIDE 4.3.3 PUBLIE `TSQuery`,
  `TSQueryCursor` (itérable `TSQueryMatch`, `setByteRange`), prédicats
  (`TSQueryPredicateStep`, handlers `#match?`/`#set!` fournis). Le module
  `core-syntax-treesitter` ne les utilise pas : `MaterialisationNatif.KINDS`
  (table codée en dur) + heuristiques par nom de nœud. Les `.scm` de
  référence existent dans sora-editor (`app/src/main/assets/tree-sitter-queries/
  java/{highlights,locals,brackets,blocks}.scm`). Grammaires natives servies :
  json, python (javascript a été retiré : aucun artefact publié — sa déclaration
  `language.json` "preferred": "tree-sitter" reste à corriger, cf. incohérence
  déclarative du prompt).
- **P6** : corrigé au passage cette session (dédoublement du décodage).

## 4. Environnement reconstruit (sandbox neuve)

JDK 17.0.21.1 Temurin (`/home/z/my-project/tools/jdk-17`), Gradle 9.9.1
(wrapper téléchargé), dépôt cloné, sora-editor cloné. Tests de base verts
(`:core-syntax-grammars:test` : 149 tests ; `:core-language:test` : 30
tests dont le banc rouge volontaire).

## 5. Blocage : le token GitHub fourni est INVALIDE

Le token `ghp_…` transmis dans le message est refusé par GitHub (401 sur
l'API `GET /user` en `token`, `Bearer` et `basic` ; `git push` :
« Invalid username or token »). Les 2 commits locaux ne peuvent pas être
poussés. Identité git locale posée : `jjoblab / olson12jb@gmail.com`
(l'identité du dépôt). **Il faudra un token valide (ou l'accès SSH) pour
pousser les commits `b87f628b` et le suivant sur `origin/main`.**

## 6. Protocole de mesure appareil (à exécuter par l'humain)

1. Installer le sample (`sample/assembleDebug`) sur un appareil d'entrée de
   gamme, froid (premier lancement après installation, puis après kill).
2. Jalons `android.os.Trace`/logs horodatés déjà posés dans le dépôt :
   `kit.preload()` appelé → `estPret()` → langue posée
   (`setLanguageWhenRegistryReady` journalise) → première publication →
   premier frame coloré.
3. Après le chantier A complet, reprendre la mesure : le jalon « langue
   posée » doit tomber sous ~300 ms après le premier frame pour
   Java/Kotlin/XML/JSON/Python (critère du prompt § 4 chantier A).
4. C7 : ouvrir successivement N fichiers variés, journaliser
   `ScopeStacks` NODES.size() (sonde à ajouter), vérifier la stabilité
   des couleurs d'un même fichier ouvert en premier ou en dernier.

## 7. Livrables de cette session

- Commits locaux (non poussés, token invalide) :
  - `b87f628b` — banc rouge du chantier A (+ 3 contrôles verts).
  - commit suivant — correctif partiel : `descriptorOf`, tri prioritaire,
    `LanguageDetectionRules`, unique décodage grammaire.
- Bancs de mesure JVM persistés : `/home/z/my-project/scripts/
  MesuresChargementAvant.java`, `MesuresLexeurAvant.java` (exécutables :
  voir § 1).
- Ce rapport : `docs/audit/audit-coloration-2026-11-22-session.md` du dépôt.
