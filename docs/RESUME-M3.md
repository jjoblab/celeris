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

## Prochaines tranches (prévues)

3. Retour à la ligne souple par offsets (A-18, variante code) et très
   gros fichiers à l'ouverture (A-25, G-01 : 100 000 lignes ≤ 500 ms) ;
   défilement horizontal borné.
4. Tactile : poignées, pilule multi-curseur (A-01), pincement zoom
   (D-15), loupe ; clavier matériel (D-17).
5. IME par niveau d'API (26→36), composition par clusters de graphèmes.
6. Accessibilité : arbre virtuel, événements, `en`/`fr` complets ;
   scénarios de démo M3 (A-01, A-18, D-01 à D-17, G-01 à G-05) ;
   clôture du jalon.
