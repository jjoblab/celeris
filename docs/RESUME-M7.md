# Résumé M7 — Android et SPI (clos le 2026-11-07, tranches 1 à 9)

> **Déclenchement** : clôture du jalon M6 (2026-11-06, critères de
> sortie couverts — catégorie B P0, soixante scénarios). Périmètre
> (feuille de route) : XML ressources/manifeste/Gradle/catalogues/R8 ;
> Compose (délegué) ; SPI VCS/débogueur/exécution avec implémentations
> de référence ; `editor-cli` sans Android exécuté en CI. Sortie
> visée : catégories C et E P0 couvertes (C P0 déjà couverte M4 ;
> E-01 reste à fermer par l'adaptateur du framework d'accès au
> stockage).
>
> **État** : jalon clos le 2026-11-07 — les neuf tranches et les deux
> corrections hors tranche sont consignées ci-dessous, critères de
> sortie couverts (catégories C et E P0, SPI de référence, cli en
> CI). Les tranches 1 à 3 ont répondu à la reprise après
> interruption et à l'amélioration de la démonstration ; les tranches 4 et 5 livrent le bloc XML/manifeste/catalogues
> et la peinture de gouttière des intégrations — la démonstration
> devient terrain de débogage.

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
   PIEGE-22 — la règle qui mesure est la police qui trace).
3. **Le clavier n'apparaissait jamais** : une vue focusable au
   toucher n'ouvre pas l'IME d'elle-même — l'appui qui prend le focus
   dévoile le clavier explicitement.
4. **Le curseur était absent** — jamais tracé : une barre par plage
   vide (chaque curseur d'une sélection multiple, A-01), clignotante
   tant que la vue tient le focus, pleine hauteur de rangée.
5. **Le texte par défaut était minuscule** : les tailles se lisaient
   en pixels bruts ; elles vivent en sp, converties à la densité de
   l'appareil (PIEGE-23), le pincement suit la police logique.

La bascule du retour à la ligne entre dans la barre de l'atelier
(icône directe, état dans le libellé) — le correctif se vérifie sans
quitter l'éditeur. Le routage tactile (`TouchesEditeur`) et le
curseur avec son clavier (`CurseurClavier`) quittent la vue pour des
collaborateurs de la famille d'`AccessibilityHost` — la vue reste
mince et sous le plafond de 600 lignes.

### Preuve

`./gradlew check` vert sur l'ensemble (1 297 tests, politique de
langue comprise), `:demo:assembleDebug` assemblé et vérifié (apksigner).

## Tranche 6 — l'adaptateur de stockage et le lint importé (2026-11-07)

### Livré

- **E-01 — `AdaptateurStockageDocuments`** : les chemins validés par
  construction deviennent des identifiants de documents — la
  résolution descend l'arbre segment par segment depuis le dossier
  racine accordé. La porte `ArbreDocuments` garde le framework hors
  de la logique ; `ArbreDocumentsSaf` est la seule classe du module à
  toucher `ContentResolver` et `DocumentsContract` — mince, ligne à
  ligne documentée. L'écriture crée les parents implicites (« mkdir
  -p »), le listage rend l'ordre lexicographique : le même contrat que
  l'implémentation de référence, testé sur un arbre factice.
- **C-12 — `AnalyseurLint` et `CorrectifsLint`** : la sortie texte
  d'Android Lint — « chemin:ligne(:colonne): Gravite: message
  [IdRegle] » — se convertit en diagnostics aux lignes exactes ; les
  lignes porteuses se peignent dans la gouttière, fondues aux erreurs
  de la syntaxe (`GutterMarks.avecErreurs`,
  `MarquesGouttiere.setErreursImportees`). Les correctifs mécaniques —
  l'ellipse typographique, le tiret cadratin — s'appliquent en
  transactions de remplacement annulables ; le catalogue est fermé et
  honnête, le mécanisme est le livrable. La démonstration analyse et
  corrige d'un geste (`FauxLint`, `AtelierLint`).

### Preuve

`:android-host:check` et `:demo:check` verts (lint zéro erreur),
`:demo:assembleDebug` assemblé. La ligne E-01 de la matrice passe à
livrée — le P0 de la catégorie E est couvert.

## Tranche 7 — SQL, C, C++ et le garde-fou R8 (2026-11-07)

### Livré

- **C-15** : les trois langages P1 restants entrent par la voie de
  grammaires — SQL (mots réservés insensibles à la casse, types,
  blocs repliés), C (préprocesseur, types natifs, corps repliés) et
  C++ (classes, espaces de noms, gabarits, `::`). Registre, cli et
  catalogue de la démonstration suivent ; les budgets de ré-analyse de
  50 000 lignes tiennent pour les trois.
- **C-09** : la grammaire ProGuard colore règles, jokers et blocs de
  membres repliés ; `ValidationRegles` garde le fichier — règles
  inconnues hors catalogue, accolades fermantes orphelines, membres
  sans point-virgule dans les blocs — à la plage exacte, sans faux
  positif sur le corpus sain. `proguard-rules.pro` entre au catalogue
  de la démonstration.

### Preuve

`:core-syntax:check` et `:core-syntax-grammars:check` verts (couverture
≥ 90 %, budgets compris), manifestes d'API régénérés (SemVer mineure,
ADR-009).

## Tranche 8 — les pastilles de couleur et les repères @Preview (2026-11-07)

### Livré

- **C-05 — `PastillesCouleur`** : les quatre formes de littéraux
  Android — `#RGB`, `#ARGB`, `#RRGGBB`, `#AARRGGBB` — se résolvent
  (la forme courte étend ses composantes, la forme sans alpha prend
  l'alpha plein) ; le carré se peint dans la respiration à droite du
  champ de numéros, à la valeur résolue — test de capture à l'appui.
- **C-11 — `ReperesPreview`** : chaque annotation `@Preview` ancre un
  repère à sa ligne, le nom du composable se lit sur la ligne même ou
  sur les deux suivantes ; le losange se trace dans la coulisse, le
  clic sur le repère délègue le rendu à l'hôte
  (`setSurRenduPreview`) — jamais embarqué. La démonstration répond
  par sa propre scène.

### Preuve

`:android-view:check` et `:demo:check` verts ; `couleurs.xml` et
`Apercus.kt` entrent au catalogue de la démonstration.

## Tranche 9 — l'aperçu de définition, la revue de diff et les grands écrans (2026-11-07)

### Livré

- **D-11 — `ApercuDefinition`** : le plan du fichier (B-16) sert
  d'index ; le symbole au curseur — les caractères d'identifiant qui
  l'entourent — rend sa déclaration, à défaut l'entrée la plus intime
  qui contient le curseur ; l'extrait reste borné à douze lignes. La
  déclaration se lit dans une fenêtre flottante, sans quitter le
  fichier.
- **D-12 — `DiffBlocs` et `RevueDiff`** : la plus longue sous-suite
  commune découpe les changements en blocs indépendants, plages
  exactes des deux côtés ; les lectures en ligne (préfixes « - » et
  « + ») et côte à côte (colonnes alignées) rendent le même diff. La
  revue décide bloc par bloc — accepté, refusé, en attente —, le
  refus réaligne les suivants, les points de contrôle reviennent en
  arrière : la matière de la revue d'agent de M8 (F-04). La
  démonstration relit son diff face au contenu d'origine et applique
  la fusion en une transaction annulable (`AtelierDiff`).
- **D-19 — `ClasseLargeur`** : les seuils des classes de taille
  Material — compact sous 600 dp, étendu au-delà de 840 dp — décident
  de la respiration de la gouttière : le compact resserre le champ de
  numéros, l'étendu le respire, le pliable déplié change de classe
  d'un geste (`EditorView.setClasseLargeur`).

### Preuve

`./gradlew check` vert sur l'ensemble, `:demo:assembleDebug` assemblé.
Au menu de l'atelier : aperçu de définition et revue du diff.

## Clôture (2026-11-07)

Critères de sortie du jalon couverts : catégories C et E P0 couvertes
(C P0 depuis M4 ; E-01 par la tranche 6), SPI VCS/débogueur/exécution
avec implémentations de référence (tranche 2), `editor-cli` exécuté en
CI (tranche 3, la fumée M1 le vérifie à chaque poussée). Matrice de
parité mise à jour — C-05, C-09, C-11, C-12, C-15, D-11, D-12, D-19,
E-01 et E-05 passent à livrés. `git status` propre, poussée sur
`main`.