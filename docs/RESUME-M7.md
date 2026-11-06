# Résumé M7 — Android et SPI (en cours, tranches 1 à 3 livrées le 2026-11-06)

> **Déclenchement** : clôture du jalon M6 (2026-11-06, critères de
> sortie couverts — catégorie B P0, soixante scénarios). Périmètre
> (feuille de route) : XML ressources/manifeste/Gradle/catalogues/R8 ;
> Compose (délegué) ; SPI VCS/débogueur/exécution avec implémentations
> de référence ; `editor-cli` sans Android exécuté en CI. Sortie
> visée : catégories C et E P0 couvertes (C P0 déjà couverte M4 ;
> E-01 reste à fermer par l'adaptateur du framework d'accès au
> stockage).
>
> **État** : jalon NON clos — les tranches 4 et suivantes restent à
> écrire (voir « Reste ouvert »). Le propriétaire a demandé la
> reprise après une interruption et l'amélioration de la
> démonstration ; les trois premières tranches y répondent.

## Tranche 1 — la démonstration devient terrain d'essai (2026-11-06)

### Livré

- **L'écran d'éditeur complet** (`demo/EditorActivity`) : la vue
  Canvas pleine — barre d'outils (plan, recherche, réglages), barre
  de recherche vivante (occurrences, préc./suiv., saut avec la plage
  sélectionnée), panneau de plan cliquable (saut à la déclaration par
  le rappel B-16), barre d'état (ligne:colonne, langage), zoom de
  police borné (8 à 40 px, persisté), annuler/rétablir, thème cyclé
  clair/sombre/contraste élevé. Les réglages en direct de M1 sont
  enfin câblés au moteur : numéros masquables (D-01 — la gouttière
  réduite à la coulisse des marques), retour souple (A-18), thème
  sombre ; ils se réappliquent au retour de l'écran de réglages.
- **Huit fichiers d'exemple** (`demo/FichiersExemple`) : un par
  langage P0 — Kotlin, Java, XML, KTS, TOML, JSON, Markdown, Python
  (C-14 à C-16) — réalistes, avec l'analyseur dédié ou la grammaire
  déclarative associée à chaque extension.
- **Trois ajouts à `android-view`**, logique extraite en classes
  pures testées (`NavigationCommands` : pose de curseur et sélection
  de plage, bornage et ordonnancement ; `HitTest.offsetAt` :
  conversion point → offset extraite de la vue ; `GestesFabrique` :
  les détecteurs de gestes quittent le constructeur — `EditorView`
  reste sous le plafond de 600 lignes), plus
  `GutterGeometry.laneOnly` et `GutterPainter.setNumerosVisibles`.
- **Correctif CI au passage** (`fix(politique)`) : le strippage des
  commentaires du contrôle de langue ignorait les blocs de texte
  Java — le corpus M6 était traité comme du code, la CI était rouge
  sur b4a564b. Réécriture de l'historique : tous les commits portent
  désormais `jjoblab <olson12jb@gmail.com>` (demande du
  propriétaire), force-push des trois branches.

### Preuve

`./gradlew check` vert sur l'ensemble (SDK présent), `:demo:assembleDebug`
assemblé (1,2 Mo), trois activités déclarées, badging vérifié.

## Tranche 2 — les contrats SPI de l'hôte, E-01 à E-06 (2026-11-06)

### Livré

- **E-01** : `CheminDoc` (chemin normalisé, « .. » borné résolu,
  sortant refusé — la traversée ne passe pas à la construction),
  `SystemeFichiersVirtuel` (ouvrir, écrire, lister, estFichier) et
  `MemoireSystemeFichiers` (implémentation de référence en mémoire).
- **E-02/D-03** : `IntegrationVcs` — `EtatLignesVcs` (marque par
  ligne : inchangée, modifiée, ajoutée ; plages supprimées aux bornes
  inclusives), `AnnotationBlame`, `RevisionVcs` ; `FauxVcs` sert les
  données déclarées.
- **E-03/D-04/D-05** : `IntegrationDebogueur` — `Arret` (posé,
  retiré, basculé : l'état désactivé reste en place), `LigneArret` et
  pile en pause, `evaluerAuSurvol` borné (refus hors pause et hors
  pile, `IllegalStateException`), `ValeurEnLigne` ; `FauxDebogueur`
  simule pause, reprise et valeurs.
- **E-04** : `IntegrationExecution` — `lancer` signalé aux
  `AuditeurExecution`, `EtatExecution` par fichier (prêt, en cours,
  terminé, échoué) ; `FauxExecution` simule les fins.
- **E-05** : `AnalyseurProblemes` — la sortie des compilateurs
  (forme « chemin:ligne:colonne: gravite: message » et forme courte)
  convertie en `DiagnosticHote` aux plages exactes, chemins racinés
  dans le système virtuel, bruit ignoré.
- **E-06** : `AnalyseurPile` — les cadres « at » convertis en
  `CadrePile` (classe, méthode, fichier, ligne exacte), causes et
  ellipses ignorées, ligne inconnue à zéro (jamais devinée).

### Preuve

`:android-host:check` vert — 36 tests (Checkstyle, PMD, Error Prone,
NullAway, lint compris). Six scénarios E-01 à E-06 au catalogue de la
démonstration (soixante-cinq au total).

## Tranche 3 — l'éditeur en ligne de commande (2026-11-06)

### Livré

- **`celeris analyse <fichier>`** : langage détecté par extension —
  les six analyseurs dédiés (Kotlin, Java, XML, KTS, Groovy, JSON)
  et la voie de grammaires (TOML, YAML, properties, Markdown, Python,
  Shell) —, lignes visibles (le saut final ne crée pas de ligne
  fantôme), caractères, et le plan du fichier par `TreeOutline`
  (B-16) : chaque déclaration avec sa catégorie et sa ligne.
- **`celeris recherche <fichier> <motif>`** : occurrences aux lignes
  et colonnes exactes.
- **Codes de sortie distincts** : 0 succès, 1 fichier illisible
  (message, jamais de trace), 2 usage inconnu. La bannière sans
  argument reste la fumée de la CI (M1).
- Le point d'entrée délègue à `executer(args, sortie)` — testable
  sans tuer le JVM de test (le test historique qui appelait `main`
  et mourait sur `System.exit` est corrigé au passage) ; la table
  des langages remplace un switch qui cassait le plafond PMD de
  complexité.

### Preuve

`:cli:check` vert (couverture ≥ 90 %, ArchUnit — la règle des
paquets autorisés évolue avec la dépendance `core-syntax-grammars`,
documentée au commit), `:cli:run` vert, sortie vérifiée à la main
sur Java (plan complet) et TOML (grammaires).

## Reste ouvert (tranches suivantes de M7)

1. **Le bloc XML/manifeste/catalogues** : C-02 (complétion guidée
   par le schéma des ressources), C-03 (diagnostics de validation
   XML), C-04 (navigation des références `@string/`), C-06
   (complétion et diagnostics du manifeste), C-08 (catalogues de
   versions liés) — `core-syntax`/`core-intelligence`.
2. **La peinture de gouttière des intégrations** : D-03 (marques
   VCS), D-04 (points d'arrêt), D-05 (ligne courante et repères
   d'exécution) — consommer les SPI E-02/E-03/E-04 dans
   `GutterMarks`/`GutterPainter`, la coulisse s'élargit.
3. **E-01 — l'adaptateur de production** : le framework d'accès au
   stockage (arbres de documents, `ContentResolver`), mince et
   documenté — le contrat et l'implémentation de référence sont
   livrés, l'adaptateur reste à écrire.
4. **C-11** (repères `@Preview` Compose, rendu délegué à l'hôte) et
   **C-12** (diagnostics de linte importés — l'analyseur E-05 existe,
   le branchement reste).
5. **C-15** (SQL, C, C++ par la voie de grammaires), **C-05**
   (pastilles de couleur), **D-11** (aperçu rapide de définition),
   **D-12** (vue de différences), **D-19** (pliables, grands
   écrans), **C-09** (règles R8, « M7+ »).
6. Clôture : critères de sortie (catégories C et E P0 couvertes),
   CHANGELOG, ce résumé mis à jour, `git status` propre, poussée.
