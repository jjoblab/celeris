# Résumé M3 — vue minimale (en cours)

> **Déclenchement** : clôture du jalon M2 (2026-11-04, sortie vérifiée
> sur les critères de la feuille de route). Périmètre : ADR-006 (rendu et
> budgets), ADR-008 (accessibilité, i18n, bidirectionnel) et feuille de
> route M3 — PIEGE-01 à PIEGE-06, PIEGE-14 et PIEGE-15 écrits avant le
> code. Ce jalon couvre plusieurs livraisons ; ce document suit
> l'avancement tranche par tranche.

## Tranche 1 — pièges de la vue et socle de rendu (2026-11-04)

### Livré

- **Les huit pièges de rendu écrits avant le code** (règle de
  traçabilité du registre des pièges, motif établi en M2) : tests nommés
  commités d'abord — échec en compilation documenté — puis socle de
  rendu qui les couvre. Registre des pièges à jour : PIEGE-01 à 06, 14
  et 15 **couverts**.
- **Socle de rendu JVM pur dans `android-view`**, testable sans SDK ni
  appareil (mesure de police injectée — ADR-008 ; surface de dessin par
  interface — approche proxy du spike de rendu, ADR-006) :
  - `EditorMetrics`, `FontSpec`, `FontRuler` : métriques immuables
    dérivées d'une règle injectée ; un changement de taille redérive par
    reconsultation, jamais par mutation en place (PIEGE-05) ;
  - `RowLayoutCache` (avec `RowKey`, `RowLayout`, `RowSource`) : cache à
    adressage ouvert, consultation par valeurs brutes sans allocation,
    clés immuables — révision, rangée, thème, police — et invalidation
    par révision (PIEGE-02) ;
  - `PainterLayer` / `TextPainter` / `SelectionPainter` (avec
    `BrushFactory`, `TextBrush`, `DrawSurface`, `FrameState`) : chaque
    couche possède son pinceau et repose sa couleur de base à chaque
    image — prouvé même sous la fabrique hostile qui partage un pinceau
    mutable entre toutes les couches (PIEGE-01) ;
  - `ScrollFrame` : tracé d'image à l'indice, état d'image muté sur
    place — preuve d'identité : cent une images de défilement, aucun
    objet nouveau ne croise l'interface après l'échauffement (PIEGE-15) ;
  - `SelectionDrag` : ancre figée au DOWN, la plage s'inverse proprement
    quand le doigt franchit la borne (PIEGE-03) ;
  - `FoldModel`, `VisualProjection` : projection lignes ↔ rangées qui
    passe par le modèle de plis en direct — jamais l'identité naïve
    « rangée = ligne » (PIEGE-04) ;
  - `HitTest` : conversion position ← pixel au plancher, discipline de
    visée au quart gauche du glyphe (PIEGE-06) ;
  - `TypefaceCache` (avec `FontHandle`, `TypefaceFactory`) : couple
    (base, gras) mémoïsé par valeur, invalidé au changement de police
    (PIEGE-14).

### Vérifié localement (sans appareil)

- `./gradlew check` vert — politique de langue et neutralité comprises,
  Error Prone + NullAway, tests Jupiter ;
- `:android-view:testDebugUnitTest` : **26 tests, zéro échec** — les
  huit pièges par leur nom de registre plus les tests de comportement
  (modèle de plis, projection, cache, visée, gestuelle) ;
- `:android-view:lintDebug --rerun-tasks` vert — lint bloquant, zéro
  référence de baseline.

### Écarts et limites

1. **Checkstyle et PMD ne se matérialisent pas sur les modules AGP** :
   les conventions appliquent bien les plugins, mais aucune tâche
   `checkstyle*`/`pmd*` n'existe sous AGP — constat préexistant depuis
   M1 (le module `demo` est dans le même cas), les plafonds 600/50/15 y
   sont donc tenus par discipline et non par porte. Le câblage explicite
   de ces tâches sur les variantes AGP demande une décision du
   propriétaire (build-logic, tous les modules Android) — à trancher
   avant la clôture M3.
2. **Colonnes monospace uniquement** : la couche de sélection calcule
   les colonnes en largeur uniforme (`charWidth` des métriques) ; la
   mesure par rangette (hors monospace) et le retour à la ligne souple
   par offsets (variante code de A-18) viennent avec la vue réelle.
3. **Pas encore de vue Android réelle** : la surface `DrawSurface`
   n'a pas d'implémentation `android.graphics` — c'est l'objet de la
   tranche suivante (vue Canvas, défilement, gouttière, thèmes), puis
   IME, clavier matériel et accessibilité.
4. **Tests instrumentés non exécutables ici** : pas d'appareil ni
   d'émulateur dans l'environnement d'exécution ; les critères de sortie
   M3 qui exigent l'appareil réel (budgets de rendu, gestes, IME,
   accessibilité) restent à vérifier sur le matériel du propriétaire.

## Tranche 2 — vue Canvas réelle et chrome (2026-11-04)

### Livré

- **Thèmes complets à contraste vérifié (D-13)** : le thème gagne la
  couleur de fond ; les trois palettes du mandat entrent (`EditorThemes`
  — clair, sombre, contraste élevé) avec le rapport de contraste WCAG
  2.x en utilitaire pur (`WcagContrast`). Le test vérifie
  automatiquement les seuils : encre ≥ 4,5:1 sur le fond ET sur le fond
  de sélection, gouttière ≥ 4,5:1, curseur ≥ 3:1 — les trois palettes
  passent.
- **Gouttière à numéros absolus (D-01, facette numéros)** : couche du
  pipeline (`GutterPainter`), alignement à droite dans le champ
  (`GutterGeometry`), étiquettes mémoïsées — jamais une chaîne allouée
  par rangée et par image (`GutterNumbers`, PIEGE-15) ; la rangée d'un
  pli porte le numéro de son en-tête (PIEGE-04) ; insensible au
  défilement horizontal. La bascule numéros relatifs et les marques de
  diagnostics (D-02) suivent.
- **Géométrie de défilement** (`ScrollGeometry`, pure) : bornage du
  décalage au contenu — un contenu plus court que la fenêtre ne défile
  pas —, première rangée visible, nombre de rangées à tracer (entamées
  comprises, une de marge).
- **Vue Canvas réelle** (`EditorView`, ADR-006) : une seule vue, mince
  par construction — la logique vérifiable sans appareil vit dans les
  classes pures couvertes, la vue assemble (pipeline à trois couches,
  état d'image, cache), suit la révision du document (invalidation par
  révision, projection reconstruite à l'édition, plis conservés),
  défile verticalement au panoramique à deux doigts, pose le curseur au
  DOWN puis étend la sélection par la gestuelle à ancre figée
  (PIEGE-03), expose `performClick` pour l'accessibilité.
- **Adaptateurs Android** : `CanvasSurface` (surface attachée par
  image, jamais allouée — PIEGE-15), `PaintBrushes`/`PaintBrush`
  (pinceaux possédés par couche — PIEGE-01), `AndroidFontRuler` (mesure
  réelle, consultée aux dérivations de métriques seulement — PIEGE-05).
- **Source de rangées sur document** (`DocumentRowSource`, pure,
  testée) : la rangée porte la ligne que la projection lui donne, texte
  sans le saut de ligne.

### Vérifié localement (sans appareil)

- `./gradlew check` vert — politique de langue et neutralité comprises ;
- `:android-view:testDebugUnitTest` : **51 tests, zéro échec** (huit
  nouveaux : géométrie de gouttière, étiquettes mémoïsées, couche de
  gouttière, source de rangées) ;
- `:android-view:lintDebug --rerun-tasks` : **aucun problème** (zéro
  avertissement, zéro référence de baseline).

### Écarts et limites

1. **Checkstyle/PMD toujours inexistants sous AGP** (écart de la
   tranche 1, inchangé — décision de câblage à trancher avant la
   clôture M3).
2. **Défilement horizontal différé** : le bornage horizontal exige le
   suivi de la largeur maximale de ligne, qui vient avec la tranche
   retour à la ligne (A-18) ; les lignes longues rognent au bord.
3. **Édition hors périmètre de la vue** : la vue affiche et sélectionne ;
   la frappe, l'IME et le clavier matériel (tranches suivantes) passeront
   par la voie de transaction unique.
4. **Instrumenté absent** : pas de test sur appareil dans cet
   environnement (gestes réels, IME, budgets de rendu) — à exécuter sur
   le matériel du propriétaire.

## Tranche 3 — retour à la ligne par offsets et très gros fichiers (2026-11-04)

### Livré

- **Retour à la ligne souple par offsets (A-18, variante code)** : la
  projection `VisualProjection` signe désormais plis ET coupures — une
  ligne de longueur L occupe max(1, ceil(L / budget)) rangées, chaque
  rangée porte sa tranche d'offsets (`spanStartAtRow`, `spanEndAtRow`,
  `spanIndexAtRow`). Les sommes préfixées se bâtissent en un passage sur
  les longueurs, hors du chemin de dessin ; les conversions
  (rowCount, rowOfLine, firstLineOfRow, lastLineOfRow) restent sans
  allocation (PIEGE-15) et passent par la projection sous plis ET sous
  coupures (PIEGE-04).
- **Budget d'au moins une colonne (PIEGE-08)** : un contenu plus large
  que la fenêtre est coupé par caractère ; le budget nul coupe le
  retour à la ligne — chaque ligne garde sa rangée entière, la forme
  héritée du socle est intacte.
- **La clé de cache signe le budget (PIEGE-02)** : `RowKey` gagne le
  budget de colonnes — même révision, même rangée, autre budget : une
  autre entrée, jamais une tranche périmée servie. La consultation
  `find` gagne la forme à cinq valeurs, l'ancienne délègue à budget nul.
- **La rangée porte sa tranche** : `DocumentRowSource` sert texte (par
  `subText`), colonnes de début et de fin, largeur au caractère
  près ; le numéro de gouttière ne se pose que sur la première tranche
  d'une ligne coupée — les continuations restent muettes ; le fond de
  sélection se borne à la tranche de chaque rangée, positionné en
  colonnes DANS la tranche, aligné sur le texte tracé depuis le bord
  gauche de la rangée.
- **Vue** : `setSoftWrap(boolean)` dérive le budget de la fenêtre et
  de la gouttière, la projection se reconstruit quand le budget
  change ; le panoramique à deux doigts défile dans les DEUX axes, le
  défilement horizontal est borné à la plus large rangée (écart n° 2 de
  la tranche 2 résolu) ; `offsetAt` vise la colonne dans la tranche.
- **Correctif : le peintre de texte suit la source courante.** La vue
  reconstruisait sa source de rangées à chaque changement de session ou
  de révision, mais la couche de texte, construite une fois au pipeline,
  avait capturé la source du premier jour — le cache invalidé se
  recomblait du contenu d'avant. La parade : `RebindingRowSource`, une
  source déléguée réassignable (test écrit avant le correctif, échec en
  compilation documenté).

### Vérifié localement (sans appareil)

- `./gradlew check` vert — politique de langue et neutralité comprises ;
- `:android-view:testDebugUnitTest` : **65 tests, zéro échec**
  (quatorze nouveaux : projection sous coupures, budget d'une colonne,
  plis après lignes coupées, longueur maximale, clé signée, tranches de
  la source, sélection bornée, gouttière muette, gros fichiers) ;
- `:android-view:lintDebug --rerun-tasks` : **aucun problème** (zéro
  avertissement, zéro référence de baseline) ;
- `GrosFichiersTest` (G-01, PIEGE-13 — minimum de trois tours) :
  100 000 lignes de 80 caractères, première image complète (document,
  projection, soixante mises en page) mesurée à **154 ms** — budget
  500 ms ; 480 000 lignes ouvrables, comptes exacts (480 000 lignes,
  480 000 rangées, longueur maximale 40).

### Écarts et limites

1. **Checkstyle/PMD toujours inexistants sous AGP** (écart de la
   tranche 1, inchangé — décision de câblage à trancher avant la
   clôture M3).
2. **Coupes au budget de colonnes, pas aux frontières de mots** : la
   variante code coupe à la colonne exacte, blancs préservés — le choix
   du mandat pour l'éditeur de code ; la variante prose à frontières
   de mots (mesure injectée) vit dans `core-text` (`SoftWrap`) et
   servira les popups. La mesure par rangette (hors monospace) suit son
   jalon.
3. **Le budget dérive de la gouttière de l'image précédente** : la
   largeur de gouttière dépend du nombre de rangées, qui dépend du
   budget — la reconstruction à l'image suivante fait converger le
   couple en une à deux images ; sans effet sur la cohérence (le
   budget signé par la projection, la source et la clé de cache est
   toujours le même).
4. **Instrumenté absent** : pas de test sur appareil dans cet
   environnement (défilement fluide réel, budgets d'image sur matériel)
   — à exécuter sur le matériel du propriétaire.

## Prochaines tranches (prévues)

4. Tactile : poignées, pilule multi-curseur (A-01), pincement zoom
   (D-15), loupe ; clavier matériel (D-17).
5. IME par niveau d'API (26→36), composition par clusters de graphèmes.
6. Accessibilité : arbre virtuel, événements, `en`/`fr` complets ;
   scénarios de démo M3 (A-01, A-18, D-01 à D-17, G-01 à G-05) ;
   clôture du jalon.
