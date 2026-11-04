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
