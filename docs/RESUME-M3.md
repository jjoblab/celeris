# Résumé M3 — vue minimale (livrée côté développement, vérification appareil du propriétaire en attente)

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

## Tranche 4 — tactile complet et clavier matériel (2026-11-04)

### Livré

- **Pincement zoom (D-15)** : la taille monte par paliers (échelle
  bornée), jamais une valeur libre ; le défilement se réajuste pour
  que la colonne et la rangée sous le doigt y restent — le pincement
  passe par le détecteur dédié, la taille redérive par `setFont`
  (PIEGE-05).
- **Pilule « ajouter un curseur » (A-01)** : l'appui long arme la
  pilule au doigt, chaque appui ailleurs AJOUTE un curseur (les
  existants tiennent), l'appui sur la pilule la désarme, le
  panoramique la couche.
- **Poignées de plage et géométrie de curseur** : les extrémités
  suivent les tranches de rangée (A-18), la visée désigne le départ ou
  la fin à portée, le glissement ne bouge que son extrémité —
  l'extrémité tirée devient curseur, l'autre devient ancre.
- **Clavier matériel (D-17)** : `KeyBindings` / `KeyBinding` /
  `EditorAction` — table par défaut complète (chaque action liée),
  remappage sans mutation (l'ancienne touche se libère), conflit
  refusé en nommant l'action, encodage dictionnaire pour la
  persistance hôte ; la vue expose `setKeyBindings` et route
  `onKeyDown`.
- **Mouvements des curseurs (D-17)** : par clusters de graphèmes et
  par mots entiers (fenêtres croissantes, jamais le document entier),
  montée et descente par RANGÉES sous coupures (A-18), début et fin
  de rangée, page à la hauteur visible, sélection l'ancre figée, tout
  sélectionner.
- **Suppression avant par clusters (D-17, cœur)** : symétrique de
  l'arrière — un curseur retire le cluster qui le suit, une plage non
  vide est remplacée par le vide, les plages voisines fusionnent au
  toucher, le geste coalesce avec la frappe et s'annule avec
  restauration de la sélection (A-13). La construction des
  suppressions (arrière et avant) déménage dans `ClusterDeletes`
  (classe de paquet) : la complexité de `TextSession` repasse sous le
  seuil de classe PMD ; manifeste d'API régénéré.
- **Géométrie de loupe** : la loupe flotte au-dessus du doigt, bornée
  à la fenêtre — le contenu agrandi se trace sur l'appareil (écart).
- **Vue** : détecteurs d'appui long et de pincement, chrome (pilule
  et poignées) tracé après le pipeline au pinceau possédé par la vue.

### Vérifié localement (sans appareil)

- `./gradlew check` vert — politique de langue et neutralité comprises ;
- `:android-view:testDebugUnitTest` : **87 tests, zéro échec**
  (vingt-deux nouveaux : pincement, pilule, poignées, raccourcis,
  mouvements, loupe) ;
- `:android-view:lintDebug --rerun-tasks` : **aucun problème** (zéro
  avertissement, zéro référence de baseline) ;
- mutation `core-text` déclarée au commit de la suppression avant :
  85,2 % détectées (1533/1799), couverture ligne 97 % — rejou de
  sortie prévu à la clôture du jalon.

### Écarts et limites

1. **Checkstyle/PMD toujours inexistants sous AGP** (écart de la
   tranche 1, inchangé — décision de câblage à trancher avant la
   clôture M3).
2. **Loupe : géométrie seule** : le contenu agrandi se trace sur
   l'appareil (composant système) — à vérifier sur le matériel du
   propriétaire.
3. **Instrumenté absent** : pas de test sur appareil dans cet
   environnement (pincement réel, appui long, poignées, clavier
   matériel) — à exécuter sur le matériel du propriétaire.

## Tranche 5 — IME par niveau d'API, composition par clusters (2026-11-05)

### Livré

- **Machine de composition pure** (`CompositionState`) : la
  composition démarre sur la sélection primaire, chaque frappe
  composée remplace la plage composée — jamais un append —, le commit
  consomme la composition ou la sélection à défaut. La position du
  curseur suit la convention de la plateforme, vérifiée sur ses
  sources au niveau 36 : positive relative à la fin du texte moins un
  (la valeur 1 pose le curseur juste après), nulle ou négative
  relative au début, toujours bornée au document.
- **Alignement aux clusters de graphèmes** : la région de composition
  et le retour arrière dedans s'alignent aux frontières de clusters
  par fenêtres croissantes autour de la borne — une région posée au
  milieu d'un emoji l'absorbe entier, une famille liée par sélecteur
  forme UN cluster, le coût est borné par le cluster, jamais par le
  document. Jamais une demi-paire de substitution (piège Unicode du
  registre).
- **Entourage IME** (`SurroundingTexts`) : fenêtres
  `getSurroundingText` honorant les longueurs, bornées au document,
  sélection relative et position de document ; suppressions
  d'entourage en unités et en points de code, un cluster entamé par
  une borne part en entier.
- **Dévoilement du curseur** (`CaretReveal`) : après chaque édition
  IME, le défilement se réajuste au minimum pour rendre le curseur
  visible — rien ne bouge s'il l'est déjà, jamais négatif, bornage au
  contenu à l'image suivante (PIEGE-04).
- **Soulignement de composition** (`ComposingPainter`) : une bande
  par rangée dans la descente, bornée à la tranche — jamais le
  débordement d'une coupure (A-18, PIEGE-04).
- **Colle complète** (`EditorInputConnection`) : la surface ENTIÈRE
  de l'interface de plateforme au niveau de compilation 36 est
  implantée méthode par méthode, sans aucun repli par défaut — 26
  méthodes de base, 3 au niveau 31 (entourage étendu sous garde de
  version, consommation déclarée, vérification orthographique
  refusée), 5 au 33 (instantané refusé, attributs de texte délégués,
  mises à jour de curseur à deux arguments refusées), 4 au 34
  (remplacement direct, gestes de stylet répondus NON PRIS EN CHARGE,
  bornes de texte non répondues). Les niveaux 35 et 36 n'ajoutent
  rien — biseauté sur les archives de plateforme elles-mêmes, le test
  de surface verrouille le compte exact (38).
- **Vue** : focusable au toucher (l'appui prend le focus),
  `onCheckIsTextEditor` (session éditable), `onCreateInputConnection`
  — texte de code sans suggestions, jamais plein écran, sélection
  initiale annoncée ; lecture seule : TYPE_NULL, pas de connexion.
  Toute frappe matérielle clôt la composition d'abord ; le retour
  arrière pendant une composition retire le dernier cluster composé,
  entier ; les touches IME suivent la voie clavier (D-17). La
  composition est une interaction de plage primaire : la première
  frappe IME y ramène la sélection.

### Vérifié localement (sans appareil)

- `./gradlew check` vert — politique de langue et neutralité comprises ;
- `:android-view:testDebugUnitTest` : **125 tests, zéro échec**
  (trente-huit nouveaux : composition, entourage, dévoilement,
  soulignement, surface par niveau d'API) ;
- `:android-view:lintDebug --rerun-tasks` : **aucun problème** (zéro
  avertissement, zéro référence de baseline).

### Écarts et limites

1. **Checkstyle/PMD toujours inexistants sous AGP** (écart de la
   tranche 1, inchangé — décision de câblage à trancher avant la
   clôture M3).
2. **Instrumenté absent** : la composition réelle dépend des IME des
   fabricants (risque « fragment réelle » de la feuille de route) ;
   les tests instrumentés par niveau d'API et le corpus de composition
   hérité restent à exécuter sur le matériel du propriétaire.
3. **Capacités IME absentes assumées** : pas de texte extrait pleine
   page ni d'instantané entier (les très gros fichiers se servent par
   fenêtres, G-01), pas de moniteur immédiat de curseur, pas de
   correction orthographique embarquée, pas de contenu riche, pas de
   gestes de stylet — chaque refus est documenté dans la colle et
   répond poliment à l'IME.

## Tranche 6 — accessibilité, BiDi au rendu, scénarios et clôture (2026-11-05)

### Livré

- **Granularités de mouvement** (`MovementGranularity`, G-05) :
  CARACTÈRE par clusters de graphèmes, MOT par mots entiers, LIGNE par
  RANGÉES visuelles sous coupures (A-18 — la lecture suit l'écran),
  PAGE à la hauteur visible ; le calcul délègue au moteur de
  déplacement du clavier matériel (D-17) : une seule sémantique de
  mouvement, deux portes d'entrée.
- **Arbre d'accessibilité virtuel** (`VirtualRows`, G-05 et G-01) :
  l'éditeur s'expose comme un nœud éditable dont les enfants virtuels
  sont les RANGÉES VISIBLES — texte de la tranche, bornes à l'écran ;
  jamais le document entier ne se sert, la fenêtre seule.
- **Colle d'accessibilité** (`AccessibilityHost`) : nœud racine
  éditable (actions de mouvement à granularité, sélection posée,
  défilement d'une page), enfants virtuels par rangée (lecture, clic
  qui pose le curseur au début de la rangée) ; les nœuds se créent par
  `obtain()` — les constructeurs publics n'arrivent qu'au niveau 30,
  la dépréciation du niveau 36 recommande précisément ces
  constructeurs, `obtain()` reste la voie compatible niveau 26 ;
  `setTextSelectable` sous garde 33. La vue annonce la sélection
  changée (tacteur, IME, actions) et le contenu changé (écouteur
  d'édition de la session).
- **BiDi au rendu** (`VisualRuns`, ADR-008) : les segments visuels —
  plages de niveau homogène, réorganisées par la règle L2 (miroir du
  cœur UAX #9), détection RTL sans allocation ; `RowLayout` porte les
  segments, `TextPainter` trace segment par segment par la variante à
  bornes de `DrawSurface` (`canvas.drawText(CharSequence, start, end,
  ...)`) — le chemin rapide d'une rangée purement gauche-à-droite
  reste un seul tracé sans allocation (PIEGE-15). Le texte logique ne
  bouge jamais : seules les positions de tracé changent (ADR-003).
- **Vue assagie** : la préparation d'état d'image se partage entre
  tracé et accessibilité (`preparerEtatImage`), le chrome (pilule,
  poignées) déménage dans `ChromePainter`, le grand aiguillage clavier
  dans `KeyActions` — complexité de classe sous le seuil PMD.
- **Portes qualité réelles sous AGP** (écart n° 1 de la tranche 1,
  résolu) : Checkstyle et PMD se posent explicitement sur le source
  set principal des conventions Android — plafonds 600/50 et
  complexité 15/80 bloquants, comme sur les modules purs. Premier
  passage réel : deux violations corrigées (`CaretsMotion.caretVise`
  scindée, `KeyActions` extrait, `ScenarioCatalog` scindé par
  catégorie).
- **Scénarios et ressources** (demo) : quatorze scénarios au
  catalogue — A-01, A-13, A-18, A-20, A-24, A-25, D-01, D-13, D-15,
  D-16, D-17, G-01, G-02, G-05, facettes livrées seules ; ressources
  `en`/`fr` complètes (ADR-008) : les deux écrans migrent leurs
  libellés, le manifeste porte `@string/app_name` ; l'APK debug
  s'assemble.

### Vérifié localement (sans appareil)

- `./gradlew check` vert — politique de langue et neutralité comprises,
  Checkstyle/PMD bloquants sur TOUS les modules (purs et Android) ;
- 562 tests verts au total — 141 `android-view`, 418 `core-text`,
  3 `demo` — zéro échec ;
- `:android-view:lintDebug`, `:demo:lintDebug` : aucun problème ;
- `:demo:assembleDebug` : l'APK se construit.

## Clôture du jalon (côté développement)

Les six tranches sont livrées, les portes qualité vertes. Ce qui
reste vit chez le propriétaire ou aux jalons suivants :

1. **Sur le matériel du propriétaire** : les tests instrumentés IME,
   gestes et accessibilité ; les budgets de rendu sur appareil réel
   (le critère de sortie de la feuille de route) ; la loupe et le
   pincement réels ; la composition réelle des IME des fabricants
   (risque « fragment réelle » consigné dans la feuille de route).
2. **Aux jalons suivants** : D-02 (diagnostics en gouttière — moteur
   syntaxique M4), D-07 (plis en gouttière — M5), D-14 (ligatures —
   M5 avec la mesure par rangette) ; la variante prose du retour à la
   ligne (frontières de mots) ; la visée et les bandes de sélection
   en colonnes VISUELLES sur les rangées bidirectionnelles mixtes —
   aujourd'hui logiques, l'état reste exact (ADR-003), l'affichage des
   bandes sur rangée mixte est l'écart assumé (M5).
3. **Registre des pièges** : les huit pièges de rendu (PIEGE-01 à 06,
   14, 15) et les pièges Unicode du registre restent couverts par
   leurs tests nommés ; le registre de portage ne garde que « plis »
   comme reste explicite du cœur texte.

La vérification appareil du propriétaire clora le jalon au sens
strict de la feuille de route ; côté développement, tout est livré,
poussé sur `main` et documenté.
