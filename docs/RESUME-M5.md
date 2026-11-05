# Résumé M5 — édition assistée (en cours)

> **Sortie visée** : catégorie A P0 couverte (A-05, A-06, A-11,
> A-20) + D-08/D-09/D-10 ; scénario par ligne de matrice
> référencé. La vérification appareil du propriétaire reste sienne.

> **Déclenchement** : clôture du jalon M4 (2026-11-05, critères de
> sortie couverts — ré-analyse ≤ 10 ms, corpus, scénarios). Périmètre
> (feuille de route) : frappe intelligente (paires, indentation,
> retour arrière, entourer, collage) ; gabarits dynamiques ;
> interface de recherche/remplacement ; historique de
> navigation ; fil d'Ariane ; lignes collantes ; popups
> unifiées ; plugins isolés. Vingt-trois lignes de matrice M5 au
> total.

## Tranche 1 — la frappe intelligente par plans (2026-11-06)

### Livré

- **Le contrat du geste** (`EditPlan`) : transaction par la voie
  unique + sélection d'après explicite. Le moteur de décision ne mute
  jamais ; la session applique le plan, l'annulation restitue la
  sélection d'avant, le refaire la sélection explicite (A-13).
- **La surcharge de session**
  (`TextSession.apply(Transaction, MultiSelection)`) : le geste
  intelligent choisit ses curseurs — entre les deux caractères d'une
  paire, après l'indentation d'une Entrée — là où la projection
  automatique ne peut pas les deviner. La transaction vide déplace la
  sélection sans créer d'étape : le saut par-dessus un fermant
  n'édite pas.
- **Les règles par langage** (`TypingRules`) : paires appariées
  `(`, `[`, `{` et citations ; le JSON ne dédouble que le
  guillemet ; les identifiants inconnus retombent sur le défaut —
  la longue traîne (C-16) frappe aussi bien.
- **La frappe** (`SmartTyping.insert`, A-05) : l'ouvrant insère sa
  paire, curseur au milieu ; la citation se dédouble ;
  l'ouvrant ou la citation sur une sélection l'entoure, le contenu reste
  sélectionné ; le fermant déjà présent sous le curseur est
  sauté — pas de double fermeture. Multi-curseurs : chaque
  curseur reçoit son geste, deltas cumulés.
- **L'Entrée** (`SmartTyping.enter`, A-06) : le retrait attendu se
  lit dans l'arbre — le nombre de nœuds structurants qui englobent la
  coupe (`IndentEngine.depthAtCaret`). Le fermant qui reste sur la
  ligne descend seul sous le curseur : l'Entrée entre accolades
  produit les deux sauts. Style espaces ou tabulations
  (`IndentStyle`) ; sans arbre, la ligne courante prête son
  indentation.
- **Le retour arrière** (`SmartTyping.backspace`, A-07) : la paire
  vide disparaît entière, la citation vide aussi, l'indentation
  excédentaire se retire d'un cran. Tout autre cas rend la main :
  la session retombe sur sa suppression par clusters de graphèmes
  (ADR-003, livré M2) — le repli est global, geste partagé par tous les
  curseurs, documenté.
- **L'entourage** (`SmartTyping.surround`, A-08) : paire en ligne
  ou bloc multiligne (le contenu gagne un cran), curseur après le
  fermant, chaque plage de la sélection entourée.
- **Le collage réindenté** (`PasteReindenter`, A-09) : la première
  ligne ne bouge pas — elle complète la ligne d'accueil ; les
  suivantes s'alignent sur la profondeur d'accueil lue dans l'arbre, la
  structure relative du collage conservée.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt, modules purs ET avec
  SDK Android (politique de langue, Checkstyle, PMD, manifestes
  compris) ;
- `:core-text:test` : **424 tests, zéro échec** (la surcharge
  comprise — restauration au refaire, transaction vide, lecture seule,
  bornes) ;
- `:core-intelligence:test` : **40 tests, zéro échec** — paires,
  Entrée (corpus Java et XML vérifiés par sonde sur les bornes réelles
  des nœuds), retour arrière, entourage, collage ;
- mutation `:core-text:mutationThreshold` : **85,2 %** (seuil
  80) sur 1 807 mutations ;
- manifestes d'API régénérés (`core-text`, `core-intelligence`).

### Écarts et limites

1. **L'arbre peut être périmé d'une édition** : la profondeur se
   lit sur l'analyse publiée — une accolade à peine frappée peut
   décaler le retrait d'un cran jusqu'à la ré-analyse (≤ 10 ms par le
   moteur M4). Borné, jamais faux de plus d'un cran.
2. **Le repli du retour arrière est global** : en multi-curseurs
   mixte (une paire vide, un mot), tous les curseurs retombent sur la
   suppression par clusters — la paire se retire alors caractère par
   caractère sur ce curseur. Choix documenté, la règle locale par
   curseur n'était pas au périmètre.
3. **L'Entrée en repli (sans arbre) ne descend pas le fermant** :
   la double insertion suppose la profondeur ; sans arbre, seul
   le saut simple est produit.
4. **Les gabarits de A-06** (« Entrée, collage, gabarit ») : la
   facette gabarit d'indentation suit la tranche des gabarits
   dynamiques (A-22).

## Tranche 2 — actions de lignes, commentaires, tri et gabarits (2026-11-06)

### Livré

- **Les actions de lignes** (`LineActions`, A-10) : le bloc de
  lignes touchées par la sélection est l'unité du geste — chaque plage
  contribue ses lignes, les contiguës fusionnent. Monter et descendre
  échangent le bloc avec sa voisine ; les bords du document ne
  bougent pas (plan vide, la sélection tient). Dupliquer place la copie
  derrière le bloc ; les curseurs y descendent, colonnes
  conservées. Joindre colle une ligne à la suivante en retirant
  l'indentation de celle-ci ; un bloc fusionne sans toucher à la
  ligne qui le suit. Supprimer emporte le bloc et son saut — le saut
  d'avant pour le dernier bloc du document.
- **Le commentaire** (`CommentRules`, `Commenter`, A-11) : le
  préfixe suit le langage — la famille C au double barre, la famille au
  dièse (Python, Shell, YAML, properties, TOML), le balisage par bloc.
  JSON n'a pas de commentaire : l'action est masquée, les
  langages inconnus aussi — jamais de préfixe deviné. La bascule ligne
  à ligne est tout ou rien : une ligne non commentée commente
  tout le bloc, toutes commentées les retirent, la ligne vide ne compte
  pas. Le bloc XML entoure la sélection (le contenu reste
  sélectionné) ou se retire s'il l'entoure déjà.
- **Le tri** (`LineSorter`, A-12) : ordre naturel — les tranches
  numériques se comparent en nombres, le reste en chaînes — la
  sensibilité à la casse appartient à l'hôte. Le bloc trié remplace le
  bloc d'origine et reste sélectionné.
- **Les gabarits dynamiques** (`Gabarit`, `Trou`, `Gabarits`,
  A-22) : le corps porte ses variables `${nom}` et
  `${nom:defaut}` ; le rendu pose les défauts, les trous
  sortent dans l'ordre du rendu — l'ordre de tabulation — et les
  occurrences d'une même variable sont liées. L'insertion sélectionne
  le premier trou ; les miroirs propagent l'édition d'une
  occurrence à ses sœurs, au même décalage relatif, borné aux limites
  de chacune ; les positions restent dans le repère d'avant
  l'édition — l'appelant compose les deltas.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt avec SDK Android ;
- `:core-intelligence:test` : **72 tests, zéro échec** (32 neufs
  — actions de lignes 11, commentaires 9, tri 5, gabarits 7) ;
- manifeste d'API régénéré (`core-intelligence`).

### Écarts et limites

1. **Les actions de lignes se raccordent au clavier et à la pilule à la
   tranche vue** : le cœur est livré, les raccourcis D-17 et la
   pilule tactile suivent la tranche d'intégration.
2. **Le tri porte sur le bloc d'un bout à l'autre** : plusieurs
   plages disjointes trient l'étendue de la première à la dernière —
   le tri par blocs séparés n'était pas au périmètre.
3. **Les gabarits n'ont pas de trous à choix** (énumérations) ni
   d'imbrication : la facette à trous et variables liées du
   cahier des charges est couverte, les choix suivront le besoin réel.

## Tranche 3 — sélection syntaxique et colonne (2026-11-06)

### Livré

- **La sélection syntaxique** (`TreeSelection` dans core-syntax,
  A-03) : la chaîne des étendues englobantes d'une position — le
  jeton qui la porte (le mot courant), les nœuds structurants qui la
  contiennent du plus petit au plus grand, puis le document. La chaîne
  est strictement croissante, dédoublonnée ; `suivante` rend la
  plus petite étendue qui contient strictement la sélection,
  `precedente` la plus grande contenue — les extensions successives
  couvrent mot → instruction → bloc → type sur Java et Kotlin,
  l'imbrication des éléments sur XML (le mot XML est le jeton de
  texte). L'arbre tolérant n'expose pas de nœuds d'expression
  (ADR-004) : l'étape « expression » se réduit au plus petit nœud
  englobant — documenté.
- **La sélection en colonne** (`ColumnSelection` dans android-view,
  A-02) : le glissement rectangulaire devient une plage par
  rangée — les bornes visuelles exactes, colonne de départ à colonne
  d'arrivée, vivant dans la tranche de chaque rangée (le retour à la
  ligne souple décale la colonne écran du début de tranche) et bornées
  à ce que la rangée porte. La ligne courte s'arrête à sa fin, la
  rangée au-delà donne une plage vide, l'ordre du glissement n'y
  change rien.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt avec SDK Android ;
- `:core-syntax:test` : **174 tests, zéro échec** (6 neufs) ;
- `:android-view:test` : **194 tests, zéro échec** (5 neufs) ;
- les bornes des nœuds vérifiées par sonde avant l'écriture des
  assertions (Java STATEMENT, Kotlin STATEMENT — la première sonde à
  l'offset des espaces manquait l'instruction, corrigée —, XML STRING) ;
- manifeste d'API régénéré (`core-syntax`).

### Écarts et limites

1. **La chaîne parcourt l'arbre entier** : l'étendue suivante
   rassemble les candidats de tous les chunks — suffisant au geste
   (une frappe), l'index par ligne suivra si le profil l'exige.
2. **Le raccord du glissement colonne au tactile** (le passage en mode
   colonne par la pilule ou la touche Alt) vit à la tranche
   d'intégration vue — le cœur géométrique est livré.

## Tranche 4 — barre de recherche, historique et signets (2026-11-06)

### Livré

- **La barre de recherche** (`SearchBarModel` dans android-widgets,
  A-20 facette interface) : l'état de la barre au-dessus du
  moteur du cœur — la requête et ses options (casse, mot entier,
  regex), la liste des occurrences recalculée à chaque changement,
  l'occurrence courante et sa navigation circulaire, l'ouverture
  depuis le curseur par l'occurrence la plus proche. Le remplacement
  appartient à la session (replaceAll par la voie de transaction,
  livré M2). Les huit combinaisons d'options sont vérifiées contre le
  moteur : en mode regex, le mot entier appartient au motif —
  comportement du cœur documenté au M2.
- **L'historique de navigation** (`NavigationHistory` dans
  android-view, B-14) : la pile bornée des positions visitées —
  le retour et l'avance, la visite qui coupe l'avance, la répétition
  qui ne s'empile pas, la fermeture de fichier qui vide l'historique
  (les positions d'un fichier fermé ne se suivent plus).
- **Les signets** (`BookmarkModel` dans android-view, B-15/D-06) :
  des lignes marquées, du saut au suivant et au précédent en boucle.
  La persistance par session passe par la projection des offsets —
  le modèle implémente le projecteur de la voie unique (ADR-003) :
  chaque édition décale les débuts de lignes marquées, le signet dont
  la ligne disparaît tombe au point de fusion et se dédoublonne.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt avec SDK Android ;
- SearchBarModelTest **7 tests**, NavigationHistoryTest **6 tests**,
  BookmarkModelTest **6 tests** — zéro échec ;
- les huit combinaisons d'options vérifiées par sonde contre le moteur
  avant l'écriture des assertions.

### Écarts et limites

1. **Le dessin de la barre et de la gouttière des signets** vit à la
   tranche d'intégration vue : les modèles sont livrés, le
   raccord visuel (peintres, commande) suit.
2. **L'historique ne porte qu'un fichier** : la fermeture vide —
   un historique inter-fichiers n'était pas au périmètre.

## Tranche 5 — bande d'erreurs, fil d'Ariane et popups unifiées (2026-11-06)

### Livré

- **La bande d'erreurs annotée** (`ErrorBandModel`, D-08) : les
  diagnostics du même arbre que la coloration deviennent des marques
  le long du défilement. La densité est bornée — au plus une marque
  par cran de bande, la première l'emporte ; le clic saute au
  diagnostic du cran le plus proche ; les diagnostics périmés
  (au-delà du document) sont ignorés.
- **Le fil d'Ariane** (`BreadcrumbModel`, D-09) : le chemin du
  symbole courant sort du plan du même arbre — les entrées qui
  englobent le curseur, de la plus externe à la plus interne. La
  troncature par la largeur garde les niveaux profonds et élague la
  tête : l'ellipse signale ce qui tombe et compte dans la
  largeur. Le mesureur est injecté — le peintre fournit le sien.
- **Le placement coordonné des popups unifiées** (`PopupPlacer`,
  D-10) : complétion sous le curseur (au-dessus en bord bas),
  documentation au flanc de la complétion (à droite, à gauche en bord
  droit), signature du côté opposé du curseur — repliée sous la
  complétion en bord haut. Jamais superposées, jamais hors écran.
  L'accessibilité du contenu appartient au peintre, qui annonce chaque
  popup à l'explorateur (tranche vue).

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt avec SDK Android ;
- ErrorBandModelTest **5 tests**, BreadcrumbModelTest **5 tests**,
  PopupPlacerTest **5 tests** — zéro échec ;
- la géométrie du placement vérifiée à la main avant l'écriture des
  assertions (bords bas, droit, haut).

### Écarts et limites

1. **Les peintres de bande, de fil et de popups** vivent à la tranche
   d'intégration vue : les modèles sont livrés, le dessin suit.
2. **La popup d'actions** (la quatrième du kit unifié) se place comme
   la documentation — le placement spécifique au menu suivra le
   raccord réel.
