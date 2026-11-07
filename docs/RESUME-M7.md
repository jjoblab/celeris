# Résumé M7 — Android et SPI (en cours, tranches 1 à 5 livrées le 2026-11-06)

> **Déclenchement** : clôture du jalon M6 (2026-11-06, critères de
> sortie couverts — catégorie B P0, soixante scénarios). Périmètre
> (feuille de route) : XML ressources/manifeste/Gradle/catalogues/R8 ;
> Compose (délegué) ; SPI VCS/débogueur/exécution avec implémentations
> de référence ; `editor-cli` sans Android exécuté en CI. Sortie
> visée : catégories C et E P0 couvertes (C P0 déjà couverte M4 ;
> E-01 reste à fermer par l'adaptateur du framework d'accès au
> stockage).
>
> **État** : jalon NON clos — les tranches 6 et suivantes restent à
> écrire (voir « Reste ouvert »). Les tranches 1 à 3 ont répondu à la
> reprise après interruption et à l'amélioration de la
démonstration ; les tranches 4 et 5 livrent le bloc
> XML/manifeste/catalogues et la peinture de gouttière des
> intégrations — la démonstration devient terrain de débogage.

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

## Tranche 4 — le bloc XML, manifeste et catalogues (2026-11-06)

### Livré

- **C-03 — `XmlValidation`** : la validation relit le document par le
  même lexeur XML (commentaires, CDATA et prologues traversent par
  l'état transporté, aucune logique dupliquée). La fermeture
  étrangère porte sa plage exacte et nomme l'attendu comme le
  trouvé ; la fermeture qui referme un ancêtre signale chaque niveau
  traversé à la plage de son ouverture ; l'élément resté ouvert à la
  fin porte la plage de sa balise d'ouverture.
- **C-02 — `SchemaAndroid` et `CompletionsXml`** : le schéma embarqué
  (quinze balises, leurs attributs, les communs, les constantes de
  taille, les unités dp/sp, les préfixes de référence) répond au
  contexte du curseur — nom de balise après le chevron, attribut
  dans l'en-tête, valeur entre guillemets ; hors balise, rien ne se
  suggère.
- **C-04 — `ReferencesRessources`** : `@string/x` mène au
  `<string name="x">` du fichier de valeurs, `@layout/x` au fichier
  de mise en page propre ; les usages du projet se recensent à
  l'offset exact, la référence prolongée (`@string/app_name2`) ne
  compte pas.
- **C-06 — `CompletionsManifeste`** : les attributs de chaque balise
  du manifeste (manifeste, application, activité, service,
  récepteur, permission) et les permissions usuelles, par préfixe.
- **C-08 — `CatalogueVersions`** : `libs.versions.toml` se lit section
  par section, les alias se suggèrent dans les builds selon l'accès
  Gradle — tirets en points, `bundles` dans son espace de noms.

### Preuve

`:core-syntax:check` et `:core-intelligence:check` verts (couverture
≥ 90 %, Checkstyle, PMD, Error Prone, NullAway compris) ; manifestes
d'API régénérés — soixante-dix-huit ajouts, aucun retrait (SemVer
mineure, ADR-009). Cinq scénarios C au catalogue de la démonstration
(soixante-treize au total).

## Tranche 5 — la peinture de gouttière des intégrations (2026-11-06)

### Livré

- **Le modèle pur de la vue** (`MarquesIntegration`, `MarqueLigne`,
  `EtatArret`) : marques VCS par ligne, arrêts posés et désactivés,
  ligne courante et lignes de pile — lignes numérotées à partir de
  zéro, convention de la vue ; la fusion se fait par
  `GutterMarks.avec`, les marques vides ne coûtent rien.
- **La peinture** (`GutterPainter`) : la barre de modification au
  bord gauche de la coulisse — modifiée à l'encre de sélection,
  ajoutée à l'encre de chaîne (D-03) ; le point d'arrêt au centre —
  carré plein posé, carré creux désactivé (D-04) ; le repère
  d'exécution par-dessus — flèche de ligne courante à l'encre du
  curseur, chevron discret des lignes de pile (D-05). La coulisse
  s'élargit de deux caractères quand les intégrations parlent
  (`MarquesGouttiere`).
- **Le terrain de débogage** (`demo/EditorActivity`) : le clic dans
  le champ des numéros pose et retire les points d'arrêt (D-04) ; le
  bouton de débogage simule la pause au premier arrêt actif — à la
  cinquième ligne sinon — avec une pile de deux cadres, puis la
  reprise (D-05) ; le diff ligne à ligne face au contenu d'origine
  (`DiffLignes`) rafraîchit les marques VCS à la frappe (D-03).
- **L'adaptateur** (`demo/AdaptateurMarques`) : l'état des contrats
  de l'hôte — lignes à partir de un — devient les marques de la vue
  — lignes à partir de zéro —, arrêts et pile filtrés au fichier
  ouvert. `EtatLignesVcs.marques()` énumère désormais les lignes
  marquées : la matière première de la peinture.
- **Le câblage de la vue** : `EditorView` expose `marquesGouttiere()`
  et route la coulisse, la peinture et le clic du champ des numéros
  par le collaborateur — la vue reste sous le plafond de 600 lignes.

### Preuve

`./gradlew check` vert sur l'ensemble (SDK présent), `:demo:assembleDebug`
assemblé (1,27 Mo). Trois scénarios D au catalogue de la
démonstration, les lignes E-02/E-03/E-04 de la matrice passent à
couvert.

## Hors tranche — la démonstration devient application (2026-11-06)

Sur demande du propriétaire, l'interface de la démonstration passe du
code programmatique nu à de vrais layouts Material 3 : identité visuelle
sombre coordonnée à la palette de l'éditeur (marque «&#160;</>&#160;» en
icône adaptative, dix-neuf icônes vectorielles, écran de démarrage),
accueil en cartes (compteurs réels, monogrammes de langage teintés des
encres syntaxiques, `CarteFichier` pur et testé), atelier d'édition
remis en scène (barre d'application au menu d'actions, recherche en
champ garni, barre d'état à chasse fixe — position, langage, état du
débogueur), réglages en interrupteurs Material décrits, bord à bord
unifié (`EdgeToEdge`, clavier compris). Correctif au passage :
l'éditeur n'affichait jamais le contenu du fichier ouvert — la session
n'était pas branchée sur la vue.

### Preuve

`:demo:check` vert (lint zéro erreur, Checkstyle, PMD, tests Jupiter —
`CarteFichierTest` compris), `:demo:assembleDebug` assemblé.

## Hors tranche — l'atelier corrigé sur rapport d'appareil (2026-11-07)

Cinq défauts signalés sur l'appareil, cinq racines trouvées et closes
— chacun avec son test qui échouait avant :

1. **Le retour à la ligne souple fermait l'application** : la fenêtre
   de rangées visibles n'était jamais bornée au contenu — les rangées
   sous la dernière ligne projetaient des tranches hors document
   (`IndexOutOfBoundsException` à l'image sous coupure). L'état
   d'image borne sa fenêtre (`ScrollGeometry.borneesAuContenu`),
   reproduction testée sur les huit contenus de la démonstration,
   plis compris, budget d'une colonne compris (PIEGE-08).
2. **Le contenu s'affichait éparpillé** — deux causes cumulées : la
   dernière ligne se répétait sous la fenêtre (même racine que le
   plantage, sans coupure), et les pinceaux traçaient à la police par
   défaut de la plateforme (12 px à chasse variable) pendant que les
   positions se calculaient à la police de l'éditeur. Chaque couche
   repose la police de l'image sur son pinceau (`TextBrush.setFont`,
   PIEGE-21 — la règle qui mesure est la police qui trace).
3. **Le clavier n'apparaissait jamais** : une vue focusable au
   toucher n'ouvre pas l'IME d'elle-même — l'appui qui prend le focus
   dévoile le clavier explicitement.
4. **Le curseur était absent** — jamais tracé : une barre par plage
   vide (chaque curseur d'une sélection multiple, A-01), clignotante
   tant que la vue tient le focus, pleine hauteur de rangée.
5. **Le texte par défaut était minuscule** : les tailles se lisaient
   en pixels bruts ; elles vivent en sp, converties à la densité de
   l'appareil (PIEGE-22), le pincement suit la police logique.

La bascule du retour à la ligne entre dans la barre de l'atelier
(icône directe, état dans le libellé) — le correctif se vérifie sans
quitter l'éditeur. Le routage tactile (`TouchesEditeur`) et le
curseur avec son clavier (`CurseurClavier`) quittent la vue pour des
collaborateurs de la famille d'`AccessibilityHost` — la vue reste
mince et sous le plafond de 600 lignes.

### Preuve

`./gradlew check` vert sur l'ensemble (1 297 tests, politique de
langue comprise), `:demo:assembleDebug` assemblé et vérifié (apksigner).

## Reste ouvert (tranches suivantes de M7)

1. **E-01 — l'adaptateur de production** : le framework d'accès au
   stockage (arbres de documents, `ContentResolver`), mince et
   documenté — le contrat et l'implémentation de référence sont
   livrés, l'adaptateur reste à écrire.
2. **C-11** (repères `@Preview` Compose, rendu délégué à l'hôte) et
   **C-12** (diagnostics de linte importés — l'analyseur E-05 existe,
   le branchement reste).
3. **C-15** (SQL, C, C++ par la voie de grammaires), **C-05**
   (pastilles de couleur), **D-11** (aperçu rapide de définition),
   **D-12** (vue de différences), **D-19** (pliables, grands
   écrans), **C-09** (règles R8, « M7+ »).
4. Clôture : critères de sortie (catégories C et E P0 couvertes),
   CHANGELOG, ce résumé mis à jour, `git status` propre, poussée.
