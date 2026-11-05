# Résumé M4 — syntaxe (livré, clôture vérifiée le 2026-11-05)

> **Sortie** : les critères de la feuille de route sont couverts —
> ré-analyse ≤ 10 ms sur 50 000 lignes par langage, corpus embarqués,
> scénarios B-01, B-16, A-15, C-01, C-07, C-14 à C-16 et G-01 (la
> vérification appareil du propriétaire reste sienne).

> **Déclenchement** : clôture du jalon M3 côté développement (2026-11-05,
> sortie vérifiée sur les critères de la feuille de route — la vérification
> appareil du propriétaire reste sienne). Périmètre : ADR-004 (analyse
> syntaxique incrémentale) et feuille de route M4 — socle commun, puis
> analyseurs Java, Kotlin, XML, JSON, Gradle KTS, voie de grammaires,
> corpus et budgets. PIEGE-19 (cascade de commentaires) inscrit au registre
> et écrit avant le code.

## Tranche 1 — socle commun d'analyse incrémentale (2026-11-05)

### Livré

- **Jetons et états par ligne** (`Token`, `TokenKind`, `LexState`,
  `LexedLine`, `LanguageLexer`) : quatorze sortes de jetons communes
  (mots réservés, types, chaînes, nombres, commentaires, documentation,
  annotations, opérateurs, ponctuation, identificateurs, constantes,
  crochets ouvrants/fermants, erreurs) ; les offsets sont RELATIFS à la
  ligne — une ligne au contenu inchangé garde ses jetons quand le
  document bouge ailleurs.
- **Lexicographie incrémentale** (`IncrementalLexer`, `LexDocument`,
  `LexResult`, `EditSite`) : une édition relit les lignes touchées puis
  continue tant que l'état de sortie diverge de l'ancien — la cascade
  s'arrête à la reconvergence, jamais au-delà (PIEGE-19) ; les paniers
  au-delà sont partagés à l'identité avec la passe précédente. La
  profondeur de crochets au début de chaque ligne accompagne les paniers :
  recalculée sur les lignes relues, décalée du différentiel net sur la
  suite.
- **Arbre tolérant ancré par chunks** (`SynNode`, `NodeKind`,
  `NodeCategory`, `Chunk`, `SyntaxTree`, `TokenSlice`, `LanguageParser`,
  `IncrementalParser`, `SyntaxService`, `Analysis`) : les chunks de haut
  niveau pavent le document sans s'imbriquer ; les sous-arbres portent
  des offsets relatifs au début du chunk, l'ancre (ligne, colonne) se
  résout par le document — une édition ne ré-analyse que les chunks
  propriétaires des lignes touchées, les autres gardent leur sous-arbre
  à l'identité (preuve par identité des instances) et ne portent que
  leurs ancres décalées. Le parseur du langage ne lève jamais : la
  malformation devient un nœud d'erreur.
- **Consommateurs du même arbre** : `BracketMatcher` (visée du crochet
  adjacent, partenaire par balayage local), `TreeFolds` (régions de pli
  des nœuds structurants et des commentaires multi-lignes, dédoublonnées,
  triées — A-15), `TreeOutline` (plan du même arbre, bornes absolues —
  B-16), `TreeDiagnostics` (plages malformées, nœud d'erreur le plus
  haut seul rapporté — D-02).
- **Coordination session ↔ analyse** (`SyntaxCoordinator`) : écoute la
  voie de transaction unique, analyse paresseusement à la première
  demande puis par incrément à chaque édition — le rejeu de l'annulation
  compris (PIEGE-07). Fermeture = détachement explicite (l'auditeur
  d'édition de `TextSession` gagne `removeEditListener`, manifeste
  régénéré).
- **Frontières d'architecture** (`ArchitectureTest`) : aucun import
  Android, dépendances vers le bas uniquement (core-text).

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt (modules purs et
  Android, politique de langue et neutralité comprises) ;
- `:core-syntax:test` : **44 tests, zéro échec** — paniers et états,
  réutilisation à l'identité, alignement par delta de lignes,
  profondeurs décalées, cascade exactement bornée (50 000 lignes :
  re-lexicographie de [100..40 000] à la suppression de l'ouverture,
  suite réutilisée ; ouverture sans fermeture jusqu'à la dernière ligne
  et termine), propriétés jqwik à graine déterministe (l'incrémental
  égale le complet, panier par panier, après des chaînes d'éditions),
  chunks conservés à l'identité, tolérance (crochet non fermé → nœud
  d'erreur), visée de crochets, plis, plan, diagnostics, coordination ;
- Checkstyle (600/50) et PMD (complexité 15) verts, sources et tests ;
- couverture JaCoCo ≥ 90 % de lignes sur `core-syntax` ;
- manifestes d'API régénérés (`core-syntax`, `core-text` pour
  `removeEditListener`).

### Écarts et limites

1. **Le langage d'essai du socle n'est pas un vrai langage** : les
   analyseurs P0 (Java d'abord) suivent aux tranches 2 à 4 — le socle
   est prouvé sur un langage miniature à commentaires de bloc, la
   réalité des grammaires viendra avec leurs corpus.
2. **La profondeur des plis par colonnes visuelles** (rangées mixtes
   bidirectionnelles) reste l'écart assumé de M3 — les plis du socle
   sont en lignes du document.
3. **Budgets de ré-analyse** : tenus — le benchmark à seuil (10 ms sur
   50 000 lignes par langage, minimum de trois tours — PIEGE-13) est
   livré avec les corpus répliqués : les cinq langages P0 passent
   (ReanalyseBudgetTest, tranche 4bis).

## Tranche 2 — analyseur Java et registre des langages (2026-11-05)

### Livré

- **Lexeur Java** (`JavaSyntax`, `JavaLexeur`, `JavaNombres`,
  `JavaSignes`) : mots réservés, littéraux de toutes bases (soulignés,
  virgule, exposant signé, suffixe), chaînes et caractères échappés,
  textes en bloc multi-lignes par état transporté — l'ouverture triple
  guillemet est un crochet ouvrant apparié à sa fermeture —
  commentaires de ligne, de bloc et de documentation (le DOC se
  distingue du bloc), annotations, opérateurs contre ponctuation,
  convention de casse (majuscule initiale = type, tout en majuscules =
  constante, un segment qualifié reste un identificateur) ; le lexique
  REPREND après une fermeture en milieu de ligne (l'essai
  «commentaireDeBlocMultiLignes» l'exigeait) ; une chaîne non fermée
  est un jeton ERROR, jamais une exception.
- **Jetons soudés** : dans une ligne, chaque jeton commence à la fin du
  précédent — les blancs appartiennent au jeton qui les suit, le rendu
  n'a jamais de trou à peindre ; `Jetons.mot` épure le texte pour les
  comparaisons du parseur.
- **Parseur Java tolérant** (`JavaParseur`, `JavaCorps`, `Jetons`,
  `JavaKind`) : chunks de haut niveau — paquet et imports groupés, puis
  chaque type avec sa documentation entraînée par l'extension
  arrière — membres (méthodes, constructeurs — un type suivi de
  parenthèse porte son nom — champs, types imbriqués, initialiseurs) et
  corps en instructions et blocs ; jamais d'exception, la malformation
  devient un nœud d'erreur ; plan, plis, diagnostics et niveaux
  d'indentation (`TreeIndent` — les lignes qui ferment portent un cran
  de plus) lus du même arbre.
- **Registre des langages** (`core-language` : `Language`,
  `LanguageRegistry`) : identification par nom de fichier, extensions
  composées comprises — la plus longue gagne, `build.gradle.kts` est du
  KTS contre `build.gradle` du Groovy ; donnée pure, c'est l'hôte qui
  peuple le registre.
- **Correctifs du socle révélés par les tests Java** :
  `IncrementalParser.etendreTrivia` atteint désormais le début de
  plage — l'ancre incrémentale d'un chunk documenté engendrait une
  frontière parasite à la déclaration (3 chunks contre 2 au complet) ;
  `TreeFolds` ne garde, à ligne d'en-tête identique, que la plus grande
  région — la documentation d'une déclaration se replie avec elle, pas
  deux fois.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt — 646 tests, zéro
  échec (25 tests Java neufs, 5 de registre) ;
- `:core-syntax:test` : 67 tests verts — lexeur (littéraux, états,
  reprise après fermeture, casse), arbre (plan, chunks, plis,
  tolérance, incrémental égal au complet sur le corpus, coordination
  session), propriété jqwik à graine déterministe (douze éditions
  chaînées du corpus, panier par panier et état par état) ;
- Checkstyle (600/50) et PMD verts — la complexité de classe des
  nouveaux fichiers repasse sous le plafond (80) par extraction de
  `JavaNombres`, `JavaSignes`, `Jetons` et `JavaCorps` (lexeur 70,
  parseur 73), sans affaiblir le seuil ;
- couverture JaCoCo ≥ 90 % de lignes sur `core-syntax` et
  `core-language` ;
- manifestes d'API régénérés : `core-syntax` (+82 lignes),
  `core-language` (+19).

### Écarts et limites

1. **Un seul analyseur P0 sur cinq** : Kotlin, XML, JSON et Gradle KTS
   suivent aux tranches 3 et 4 — le socle et le langage le plus exigeant
   sont prouvés, les autres grammaires réutilisent `Jetons` et le motif
   état-par-ligne.
2. **Les retouches d'essai** (comptage 1-based du pli corrigé en base 0,
   `public` superflu retiré des blocs de texte, `exige()` NullAway)
   sont documentées dans le commit — les tests de la tranche n'avaient
   jamais tourné avant l'implémentation, par construction.

## Tranche 3 — analyseurs XML et JSON (2026-11-05)

### Livré

- **Lexeur XML** (`XmlSyntax`, `XmlLexeur`) : la balise ouvrante porte
  son nom puis ses attributs jusqu'au chevron de ponctuation ; la
  fermeture nommée et l'auto-fermeture sont des crochets fermants
  appariés à l'ouverte — appariement et profondeur suivent ; prologue
  en documentation, texte en un jeton (entités comprises), section
  CDATA ouvrant un crochet apparié à ]]>, commentaires,
  balises à cheval sur les lignes et CDATA multi-lignes par état
  transporté, lexique qui reprend après fermeture en milieu de
  ligne, valeur d'attribut non fermée = ERROR, noms de balises non
  ASCII.
- **Parseur XML** (`XmlParseur`, `XmlKind`) : chaque élément est un
  nœud TAG nommé et repliable — attributs couverts par l'envergure,
  enfants imbriqués, texte et CDATA couverts sans nœud propre ; la
  fermeture manquante ou désassortie devient un nœud d'erreur,
  jamais d'exception ; la fermeture ]]> d'une section CDATA
  ne clôt pas l'élément qui l'entoure.
- **Lexeur et parseur JSON** (`JsonSyntax`, `JsonLexeur`, `JsonParseur`,
  `JsonKind`) : aucune état transporté — une chaîne ne traverse
  jamais les lignes ; nombres signés à virgule et exposant,
  true/false/null réservés ; le document entier est une valeur en
  un chunk, objets et tableaux repliables, chaque paire un nœud FIELD
  nommé par sa clé sans guillemets — le plan liste les clés.
- **Correctif du socle** : l'extension arrière des trivia n'attache
  plus que la documentation (DOC) — les commentaires ordinaires ne
  suivent pas la déclaration ; le prologue XML cessait d'être un
  chunk à part (l'élément racine l'engloutissait).

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt — 680 tests, zéro
  échec (34 neufs, jqwik à 120 essais compris) ;
- Checkstyle (600/50) et PMD verts — `JsonLexeur.lexNombre` découpé
  en fraction et exposant (complexité de méthode sous le plafond 15) ;
- couverture JaCoCo ≥ 90 % de lignes ; manifeste d'API
  `core-syntax` régénéré (+96 lignes).

### Écarts et limites

1. **Trois analyseurs P0 sur cinq** : Kotlin et Gradle KTS suivent à
   la tranche 4 — le motif état-par-ligne et les utilitaires Jetons
   sont prouvés sur trois grammaires différentes (C, balisage,
   sérialisation).
2. **Le JSON vit en un chunk unique** : le lexique reste incrémental
   (paniers par ligne partagés), mais le parseur relit le chunk
   entier à chaque édition — le benchmark de clôture mesurera le
   coût sur 50 000 lignes.

## Tranche 4 — analyseurs Kotlin et Gradle KTS (2026-11-05)

### Livré

- **Lexeur Kotlin** (`KotlinSyntax`, `KotlinLexeur`, `KotlinSignes`) :
  gabarits de chaînes — les guillemets internes aux accolades
  ${…} ne clôturent pas —, chaînes brutes multi-lignes par
  état transporté (ouverture triple guillemet en crochet ouvrant
  apparié), commentaires de bloc NICHÉS par état à profondeur —
  DansBloc porte la profondeur ET le drapeau documentation, la
  KDoc continuation reste de la documentation et s'attache au chunk —,
  opérateurs propres (?: ?. :: -> .. ..<), convention de casse.
- **Parseur Kotlin** (`KotlinParseur`, `KotlinKind`) : une
  déclaration Kotlin vit sans point-virgule — le prédicat de
  découpe accepte le changement de ligne, les membres s'arrêtent à la
  fin de la leur ; fonctions et propriétés de premier niveau en
  chunks racines, classes, objets (compagnons et anonymes compris) et
  interfaces en membres nommés, corps délégués au Corps
  paramétré ; l'accolade d'un objet anonyme se cherche dès le
  mot-clé.
- **Dialecte Gradle KTS** (`KtsSyntax`) : même lexeur, même parseur,
  identifiant propre — le registre des langages distingue
  build.gradle.kts de build.gradle par l'extension
  composée la plus longue.
- **Corps partagé** : `JavaCorps` devient `Corps`, paramétré par les
  sortes de nœuds du langage appelant — Java et Kotlin partagent le
  même découpage tolérant des corps.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt — 699 tests, zéro
  échec (19 neufs) ;
- Checkstyle (600/50) et PMD verts — `membre` découpé en scan
  (`scannerMembre`), `KotlinSignes.lex` allégé de ses signes
  composés ;
- couverture JaCoCo ≥ 90 % de lignes ; manifeste `core-syntax`
  régénéré (+110 lignes).

### Écarts et limites

1. **Les cinq analyseurs P0 sont livrés** (Java, Kotlin, XML, JSON,
   Gradle KTS) : restent la voie de grammaires pour la longue traîne,
   les consommateurs de la vue, l'ouverture progressive et les
   budgets de ré-analyse (10 ms sur 50 000 lignes, PIEGE-13).
2. **Le gabarit multi-lignes reste une approximation** : les
   accolades ${…} à cheval sur les lignes ne sont pas
   suivies — le contenu continue de couleur chaîne, sans clôture
   prématurée.
3. **Les déclarations à cheval sur les lignes** (corps d'accolades
   ouvert sur la ligne suivante de la signature) restent tolérées en
   plages, sans nœud dédié — le corpus et le style usuel ouvrent
   l'accolade sur la ligne de signature.

## Tranche 4bis — budget de ré-analyse à seuil (2026-11-05)

### Livré

- **Budget de sortie mesuré** (`ReanalyseBudgetTest`) : les cinq
  langages P0 ré-analysent 50 000 lignes après édition en moins de
  10 ms, minimum de trois tours retenu (PIEGE-13). Le script Gradle
  KTS vivait en un chunk unique (29,2 ms mesurés) : le prédicat de
  découpe Kotlin accepte désormais l'identificateur en tête de ligne
  à profondeur nulle — chaque appel de premier niveau ouvre son chunk,
  la ré-analyse retombe locale.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble — 704 tests, zéro échec ;
- le découpage par appel ne change rien aux fichiers Kotlin
  ordinaires (aucune déclaration n'est un identificateur de premier
  niveau) — les essais d'arbre Kotlin restent verts sans retouche.

## Tranche 5 — les consommateurs du rendu : coloration, plis et diagnostics (2026-11-05)

### Livré

- **Palettes syntaxiques par thème** (`SyntaxPalette`, `EditorTheme`,
  `EditorThemes`) : une encre par sorte de jeton du socle, résolue une
  fois pour toutes (ADR-008) et indexée par ordinal — la consultation
  ne lit qu'un tableau, sans allocation dans le chemin de dessin
  (PIEGE-15). Les trois thèmes complets (clair, sombre, contraste
  élevé) gagnent leur palette, chaque encre contrastant ≥ 4,5:1 sur le
  fond — vérifié par test (D-13) ; le thème hérité à six champs
  reçoit la palette monochrome à l'encre de base, un thème sans
  coloration déclarée reste un thème valide.
- **Plages colorées couvrantes** (`ColoredSpan`, `RowLayout.spans`,
  `SyntaxRowSource`) : le décorateur greffe sur chaque mise en page
  les plages des paniers de l'analyse — les jetons soudés du socle
  couvrent la ligne, tout écart résiduel se comble à l'identificateur,
  la rangée n'a jamais de trou à peindre ; sous retour à la ligne
  (A-18) les plages sont rognées à la tranche, une coupe au milieu
  d'un jeton le suit, l'état transporté colore la suite d'un
  commentaire de bloc, la rangée vide reste sans plage.
- **Le peintre de texte coloré** (`TextPainter`) : un pinceau par
  sorte pris à sa fabrique, la couleur de chaque plage posée à
  l'instant précis du tracé — même sous la pire fabrique (un pinceau
  mutable unique), aucune couleur résiduelle ne se propage (PIEGE-01) ;
  sous segments bidirectionnels, la couleur suit l'intersection
  plage-segment dans l'ordre visuel (ADR-008) ; une rangée sans plage
  reste sur le chemin rapide d'un seul tracé.
- **Le suivi des plis à l'édition** (`FoldTracker`, A-15) : le pli
  manuel survit à l'édition voisine — l'en-tête détruit tue le pli,
  les lignes d'après se décalent du différentiel net de sauts de
  ligne, le corps chevauchant la zone touchée s'étend ou se rétracte,
  le corps vidé tue le pli ; l'insertion en fin de contenu de ligne
  n'y déplace pas l'en-tête, la coupure au milieu de l'en-tête fait
  suivre le contenu. Le remappage passe par la monotonie de
  l'indexation des lignes, l'état replié ne bouge jamais, le rejeu de
  l'annulation traverse la même voie (PIEGE-07) — annuler l'édition
  qui avait décalé le pli le ramène à sa place.
- **La coulisse de marqueurs** (`GutterMarks`, `GutterPainter`,
  `GutterGeometry`, `DrawSurface.drawLine`) : les chevrons de pli
  (D-07) — replié pointe à droite, pliable pointe vers le bas — en
  deux traits de primitives, la marque d'erreur (D-02) à l'encre
  d'erreur du thème ; l'état replié s'interroge au vivant sur le
  modèle de plis, basculer un pli se voit à l'image suivante sans
  reconstruire les marques ; la coulisse s'ajoute à gauche du champ de
  numéros, la gouttière sans marques reste muette.
- **Le pont session ↔ vue** (`EditorSyntax`) : pur, testable sans SDK
  — le coordinateur d'analyse suit la session sur la voie de
  transaction unique, la source de rangées s'habille des plages,
  les régions pliables (`TreeFolds`) et les lignes en erreur
  (`TreeDiagnostics`) se rafraîchissent par révision d'analyse,
  l'appui dans la coulisse bascule le pli de la ligne visée avec
  garde de chevauchement, l'éditon remappe les plis sur l'instantané
  d'avant.
- **Le câblage de la vue** (`EditorView.setLanguage`) : le changement
  de source vide TOUT le cache — la langue ne signe aucune composante
  de clé (PIEGE-02) — et la sentinel `Long.MIN_VALUE` force la
  reconstruction ; au passage, le changement de session à révision 0
  reconstruit désormais sa source : `Document.of` démarre à révision
  0, l'angle mort était latent depuis M3.
- **Scénarios M4 au catalogue** : A-15, B-01, C-01 et C-14, dans
  l'ordre de la matrice de parité (18 scénarios au total).

### Vérifié localement

- `./gradlew check` vert sur l'ensemble — **744 tests, zéro échec**
  (40 neufs : palettes et contrastes WCAG, plages couvrantes et
  rognées, coloration sous BiDi et fabrique hostile, suivi de plis —
  onze éditions types dont le rejeu d'annulation —, marques de
  gouttière, chevrons et coulisse, vidage complet du cache) ;
- Checkstyle (600/50) et PMD (classe 80, méthode 15) verts : la
  complexité d'`EditorView` tient sous le plafond par délégation au
  pont `EditorSyntax`, la palette indexe par ordinal (complexité 1) ;
- couverture JaCoCo ≥ 90 % de lignes maintenue sur les modules purs.

### Écarts et limites

1. **L'analyse vit sur le fil d'appel** : la première analyse d'un
   grand document se paie à la première image — l'incrémental la
   suit (µs à l'édition). Le fil dédié et l'annulation (ADR-007) et
   l'ouverture progressive (G-01) suivent leur tranche.
2. **D-02 à un seul niveau** : le socle ne rapporte que des erreurs
   (nœuds malformés) ; la priorité erreur > avertissement attend les
   diagnostics de sémantique (M6). L'aperçu de pli au maintien (D-07)
   attend les popups (M5).
3. **Restent à livrer pour M4** : la voie de grammaires pour la
   longue traîne (C-15/C-16), le plan consommateur (B-16), le
   scénario C-07 (Groovy et KTS — l'analyseur Groovy n'existe pas),
   l'ouverture progressive et les corpus embarqués.

## Tranche 6 — l'analyse quitte le fil d'appel (2026-11-05)

### Livré

- **Le jeton d'annulation** (`JetonAnnulation`, `AnalyseAnnulee`) :
  drapeau sous moniteur intrinsèque — annuler coûte moins cher que
  terminer, la vérification est un point de contrôle explicite entre
  deux phases longues, jamais dans la boucle chaude. Ni volatile ni
  atomique au cœur : la règle ADR-007 devient greppable et vérifiée
  par le test d'architecture du module (aucun champ volatile, aucune
  classe atomique, la fabrique Executors interdite — l'exécuteur
  s'injecte).
- **La lexicographie à point de contrôle** (`IncrementalLexer.lexFull`
  à jeton) : tous les 256 lignes le jeton s'interroge — la passe
  annulée s'arrête au premier point de contrôle, mesuré moins cher
  que la passe complète sur 100 000 lignes.
- **L'arbre par fenêtre** (`IncrementalParser.parseFenetre`,
  `completer`, `Chunk.estEnAttente`, `SyntaxTree.estComplet`) : la
  marche des jetons calcule la géométrie de TOUS les chunks — c'est
  bon marché — et `parseChunk` se paie séparément ; la fenêtre visible
  construit ses chunks, les autres se posent en attente : marqueur
  neutre, inerte pour plis, plan et diagnostics (deux chunks peuvent
  partager une ligne, seuls leurs jetons entrent dans la tranche à la
  complétion) ; la complétion construit par lots dans l'ordre du
  document sans retoucher les sous-arbres déjà là — preuve par
  l'identité des instances ; l'édition incrémentale part d'un arbre
  partiel comme d'un complet : le chunk propriétaire se ré-analyse,
  les chunks en attente se décalent et restent en attente.
- **Le moteur hors fil** (`MoteurAnalyse`) : l'exécuteur s'injecte,
  tout état mutable vit sous le moniteur, le travail se capture hors
  verrou ; la fenêtre visible se paie d'abord, la complétion publie
  par lots de 32 sur le fil de travail ; l'édition supprime le
  travail en vol par son jeton et reprogramme sur l'instantané le
  plus récent — une tâche supplantée ne publie jamais, la publication
  vérifie son jeton sous moniteur (PIEGE-20) ; un seul site d'édition
  en attente suit la voie incrémentale (une publication), une chaîne
  de sites ne se plie pas et repart par la fenêtre ; le rejeu de
  l'annulation suit la même voie que toute édition (PIEGE-07) ;
  close() détache l'auditeur de session et annule tout.
- **Le pont à deux voies** (`EditorSyntax` à exécuteur,
  `ExecuteurAnalyse`) : sans exécuteur, la voie synchrone d'avant ;
  avec, la vue n'habille JAMAIS un document avec le lexique d'une
  autre révision — sans publication à jour, la source passe sans
  couleur, la première image ne paie pas l'arbre (PIEGE-20, facette
  vue) ; la fenêtre visible se pousse à chaque image préparée — en
  rangées, la projection résout —, le défilement ne coûte rien ; le
  rappel d'invalidation part à chaque publication sur le fil de
  travail, l'hôte y poste `postInvalidateOnAnimation` (appelable de
  tout fil) ; le fil d'analyse naît à la première tâche, une vue sans
  langage n'ouvre aucun fil ; EditorView reste sous les plafonds par
  délégation au pont.
- **Scénario G-01 au catalogue** : le texte dit la coloration
  progressive — fenêtre d'abord (224 ms mesurés sur cent mille
  lignes), complétion par lots ensuite.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble — **770 tests, zéro échec**
  (26 neufs : jeton, fenêtre et complétion, moteur hors fil — dont la
  tâche supplantée en plein vol, lexeur retenu derrière une porte —,
  budget d'instantané de fenêtre, pont à deux voies) ;
- budget G-01 facette analyse : instantané de fenêtre mesuré à
  **224 ms sur 100 041 lignes Java** (minimum de trois tours,
  enveloppe 500 ms — celle de la première image de la matrice) ;
  l'annulation de la lexicographie, mesurée, coûte moins cher que la
  terminer ;
- règles ADR-007 greppables vertes sur core-syntax ; manifeste d'API
  régénéré : additions seulement, aucune rupture (ADR-009).

### Écarts et limites

1. **La lexicographie reste pleine à l'ouverture** : les états se
   cascadent de la première ligne, la fenêtre seule ne peut pas s'y
   servir — l'instantané de fenêtre paie donc le lexique complet
   (mesuré : 224 ms à 100 000 lignes, dans l'enveloppe de la première
   image). Le lexique paresseux par fenêtre suivra si les corpus le
   demandent.
2. **Un fil démon par vue** : le moteur vit le temps de la vue, le
   partage de pool est l'affaire de l'hôte (injection au pont).
3. **Rafale d'éditions pendant l'ouverture** : la chaîne de sites ne
   se plie pas — la voie de fenêtre repart à chaque édition, bornée
   par l'annulation (le travail supplanté rend son jeton au premier
   point de contrôle).
4. **Restaient alors à livrer pour M4** : la voie de grammaires pour
   la longue traîne (C-15/C-16), le plan consommateur (B-16) et le
   scénario C-07 (Groovy) — ce dernier est livré en tranche 7.

## Tranche 7 — analyseur Gradle Groovy, budget tenu sur machine lente (2026-11-05)

### Livré

- **Lexeur Groovy** (`GroovySyntax`, `GroovyLexeur`, `GroovySignes`,
  `GroovyKind`) sur le dialecte Gradle (C-07) : mots réservés et
  modificateurs, nombres (relatifs, décimaux, suffixes), chaînes
  délimitées avec échappements, gabarits `${…}` où les guillemets
  internes ne clôturent jamais, chaînes triples en crochets appariés
  ouvrant à fermeture, commentaires de ligne, blocs nichés en profondeur
  (le DOC se distingue du bloc), annotations ; états transportés —
  bloc ouvert et triple ouverte traversent les lignes, l'état de sortie
  reprend à la profondeur exacte (PIEGE-19).
- **Parseur Groovy tolérant** (`GroovyParseur`) : dans un script,
  chaque appel de premier niveau en tête de ligne ouvre son chunk
  (plugins, task, dependencies…) — la ré-analyse reste locale —, les
  types (class, interface, enum, trait) s'analysent en membres,
  méthodes `def` à parenthèses et champs `def` se distinguent, les
  corps passent par `Corps` partagé ; la malformation devient un
  nœud d'erreur, jamais une exception.
- **Corpus embarqué** (`corpus/exemple.groovy`) et budget de ré-analyse
  étendu au sixième langage (ReanalyseBudgetTest) : 0,48 ms mesurés
  sur 50 000 lignes.
- **Correctif CI — budget sur machine lente (PIEGE-13)** : le budget
  de 10 ms tenait localement mais cassait sur les exécuteurs GitHub
  (json à 8 ms local, deux à quatre fois plus lent là-bas). Deux
  causes, deux remèdes : JSON vivait en un chunk unique — chaque
  valeur de premier niveau ouvre désormais son chunk (précédent KTS de
  la tranche 4bis, 8,02 → 0,43 ms mesurés) ; l'assemblage du lexique
  relisait les 50 000 paniers ligne à ligne à chaque édition — le
  préfixe et le suffixe intacts se copient de masse
  (`System.arraycopy`), différentiel nul ou pas (plancher de tous les
  langages abaissé, 0,19 à 1,18 ms mesurés).
- **Scénario C-07 au catalogue** de la démonstration, avec son test de
  fumée (dix-neuf scénarios au total).

### Vérifié localement

- `./gradlew check` vert sur l'ensemble — **791 tests, zéro échec** ;
- budget de ré-analyse, minimum de trois tours sur 50 000 lignes :
  Java 0,19 ms, Kotlin 0,72 ms, XML 1,04 ms, JSON 0,43 ms, Gradle KTS
  1,18 ms, Gradle Groovy 0,48 ms (budget 10 ms) ;
- mutation core-text 85,2 % détectées (seuil 80) ; publication locale,
  sommes de contrôle et fumée de la cli verts ; APK démonstration
  assemblé ; manifeste d'API régénéré : additions seulement, aucune
  rupture (ADR-009).

### Écarts et limites

1. **Gradle Groovy sans résolution dynamique** : le dialecte couvre
   les scripts de construction, pas le Groovy général — les fermetures
   `it`, les builders et la métaprogrammation restent hors périmètre
   (C-07 parle de scripts Gradle).

## Tranche 8 — le plan consommateur (2026-11-05)

### Livré

- **Le pont pousse le plan** (`EditorSyntax.setSurPlan`) : les entrées
  sortent de l'analyse habillée — le MÊME arbre que la coloration, à
  la révision près — poussées à chaque changement de révision
  analytique ; le rattachement d'une session et le retrait du langage
  poussent un plan vide : le consommateur n'affiche jamais les entrées
  d'un document parti. Le rappel part sur le fil qui habille.
- **Le modèle du plan** (`OutlineModel`, android-widgets, pur) : la
  profondeur d'affichage se déduit de l'imbrication des bornes — une
  entrée vit dans la première entrée précédente qui la contient —, la
  mise à jour conserve le préfixe intact d'un plan à l'autre (le
  changement rapporte préfixe, remplacées, ajoutées) et le test au
  point résout la rangée visée, null hors bornes.
- **Le peintre du plan** (`OutlinePainter` + `OutlineSurface`) : un
  glyphe de catégorie en primitives — le carré des types, le cercle
  des fonctions, le tiret des champs — et le nom en retrait d'un pas
  par niveau, sans allocation dans la boucle de dessin (PIEGE-15) ;
  la vue (`OutlineView`) est fine : modèle, peintre et adaptateur
  canvas, l'appui part au consommateur déclaré.
- **Scénario B-16 au catalogue** de la démonstration (vingt
  scénarios).

### Vérifié localement

- `./gradlew check` vert sur l'ensemble — **802 tests, zéro échec**
  (11 neufs : plan du même arbre que la coloration — une analyse
  indépendante produit les mêmes entrées —, mise à jour à l'édition
  voie synchrone et hors fil, retrait du langage, profondeurs,
  préfixe intact sur le vrai plan du socle après édition
  incrémentale, test au point, glyphes et retraits, plan vide) ;
- Checkstyle (600/50) et PMD (15) verts sur les modules touchés.

### Écarts et limites

1. **Le plan des langages à grammaires** : la voie de grammaires ne
   produit pas d'arbre de déclarations (ADR-004) — le plan
   consommateur ne s'applique qu'aux langages à analyseur dédié.
2. **Le défilement du plan** : la hauteur suit le nombre d'entrées,
   l'hôte enveloppe la vue d'une vue défilante quand le plan dépasse
   l'écran.

## Tranche 9 — la voie de grammaires pour la longue traîne (2026-11-05)

### Livré

- **Le module pur `core-syntax-grammars`** (ADR-002, la voie héritée
  du dépôt de départ selon le registre de portage) : les langages
  sans analyseur dédié se colorent et se replient par grammaire
  déclarative au format TextMate, portés par le socle incrémental
  commun — jetons, états transportés, crochets appariés, plis.
- **Le lecteur et le chargeur** (`LecteurJson`, `Grammaires`) : JSON
  strict — objets, tableaux, chaînes échappées unicode comprises,
  nombres — tout refus est net, position en cause ; les inclusions du
  référentiel (`#cle`) se résolvent en références — la liste de chaque
  clé existe avant sa propre résolution, les inclusions cycliques à
  travers un bloc (un tableau de valeurs qui contient des tableaux) se
  référencent sans fin d'aplatissement ; `$self` est refusé.
- **Le mapping portées → sortes de jetons** (`Portees`, voie héritée)
  : les portées pointées se rabattent sur les quatorze sortes communes
  — la coloration d'une grammaire vit dans les mêmes paniers que
  celle des analyseurs dédiés ; `punctuation.section.*.begin/.end`
  porte les crochets appariés, quelque soit le suffixe de langage.
- **Le lexeur à pile de blocs ouverts** (`GrammaireLexeur`,
  `EtatGrammaire`, PIEGE-21) : la pile transportée est immuable et à
  égalité de valeur — la cascade de re-lexicographie s'arrête
  exactement à la reconvergence, l'édition au cœur d'une chaîne
  ouverte ne relit QUE sa ligne ; le candidat le plus à gauche gagne,
  les ex æquo se départagent par l'ordre des règles, la fermeture du
  bloc ouvert prime ; les écarts non couverts portent la portée de
  contenu du bloc ouvert, rognés de leurs blancs d'escorte.
- **Le parseur générique** (`GrammaireParseur`) : chunks au premier
  jeton de chaque ligne de premier niveau (la ré-analyse reste locale
  à la ligne éditée), une région de pli par paire de crochets
  multi-lignes — chaînes triples, tableaux, clôtures de code,
  séquences en flux. Jamais d'arbre de déclarations : le plan reste
  l'affaire des analyseurs dédiés.
- **Six grammaires embarquées** (`LangagesGrammaires`, corpus
  compris) : TOML, YAML, properties et Markdown — le reste des P1
  (C-15) ; Python et Shell — la longue traîne P2 (C-16). Le registre
  des langages de l'hôte les identifie par nom de fichier.
- **Scénarios C-15 et C-16 au catalogue** (vingt-deux scénarios) ; la
  cli annonce le septième module du cœur.

### Vérifié localement

- `./gradlew :core-syntax-grammars:check` vert, puis l'ensemble —
  **851 tests, zéro échec** (54 neufs : lecteur JSON et chargeur,
  mapping, lexeur — pile à égalité de valeur, cascade bornée par la
  reconvergence (PIEGE-21), l'ouverture qui ne ferme plus cascade
  jusqu'au bout et termine (PIEGE-19), chaîne triple en crochets
  appariés —, parseur — chunks par ligne, plis des paires
  multi-lignes, crochet non fermé toléré —, six langages coloration +
  plis (critère C-15/C-16), propriété jqwik à graine déterministe
  (l'incrémental égale le complet, panier par panier et état par
  état), budget de ré-analyse) ;
- budget de ré-analyse, minimum de trois tours sur 50 000 lignes :
  TOML 1,67 ms, YAML 1,15 ms, properties 2,30 ms, Markdown 0,59 ms,
  Python 2,59 ms, Shell 0,47 ms (budget 10 ms, PIEGE-13) ;
- couverture JaCoCo ≥ 90 % de lignes ; Checkstyle (600/50) et PMD
  (classe 80, méthode 15) verts ; manifeste d'API posé
  (`api/core-syntax-grammars.api`) ; règles ArchUnit vertes (aucune
  dépendance Android, dépendances vers le bas uniquement).

### Écarts et limites

1. **Le format des expressions** : les grammaires embarquées usent du
   sous-ensemble d'expressions compatible avec la plateforme — les
   récursions arrière et possessives restent hors périmètre ; une
   grammaire tierce exigeant le moteur d'expressions historique en
   entier ne se chargera pas.
2. **SQL, C et C++** (C-15) : suivent M7 par la même voie — les
   grammaires livrées démontrent le chemin.
3. **Le pli des langages à indentation** (Python, YAML) : la voie
   replie les paires délimitées (chaînes triples, tableaux, flux),
   pas les blocs d'indentation — un pli par niveau d'indentation
   exigera un consommateur dédié de la gouttière.
4. **Les contextes invisibles** : la pile ne transporte que les blocs
   ouverts — les grammaires à héritage de contexte implicite ne sont
   pas couvertes.

## Clôture M4 — critères de sortie (2026-11-05)

| Critère (feuille de route M4) | État |
| --- | --- |
| Ré-analyse ≤ 10 ms sur 50 000 lignes par langage | tenu — six analyseurs dédiés de 0,19 à 1,18 ms (tranche 7), six langages à grammaires de 0,47 à 2,59 ms (tranche 9), minimum de trois tours |
| Corpus embarqués | par langage : Java, Kotlin, XML, JSON, Gradle KTS, Gradle Groovy (core-syntax) ; TOML, YAML, properties, Markdown, Python, Shell (core-syntax-grammars) |
| Scénarios B-01, B-16, A-15, C-01, C-07, C-14 à C-16, G-01 | au catalogue de la démonstration, vingt-deux scénarios, ordre de la matrice |
| Test de cascade de commentaires borné | couvert dès la tranche 1 (PIEGE-19), repris par la voie de grammaires (PIEGE-21) |
| Ouverture progressive de très gros fichiers | livré en tranche 6 (G-01 — fenêtre d'abord, complétion par lots) |

Vérifications de clôture : `./gradlew check` vert sur l'ensemble —
**851 tests, zéro échec** ; couverture JaCoCo ≥ 90 % de lignes sur les
modules purs ; Checkstyle et PMD verts partout ; manifestes d'API à
jour (additions seulement, `api/core-syntax-grammars.api` posé) ;
mutation core-text au seuil (85,2 % détectées, seuil 80) ; publication
locale et sommes de contrôle vertes ; fumée de la cli verte (sept
modules du cœur annoncés) ; APK de démonstration assemblé.
