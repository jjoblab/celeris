# Résumé M4 — syntaxe (livraison en cours, tranche par tranche)

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
3. **Budgets de ré-analyse** : le benchmark à seuil (10 ms sur 50 000
   lignes par langage, minimum de trois tours — PIEGE-13) arrive avec
   les corpus embarqués de la tranche de clôture ; la cascade est déjà
   prouvée bornée en lignes, pas encore en temps.

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
