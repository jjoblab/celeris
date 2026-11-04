# Résumé M3 — vue minimale (en cours)

> **Déclenchement** : clôture du jalon M2 (2026-11-04, sortie vérifiée
> sur les critères de la feuille de route). Périmètre : ADR-006 (rendu et
> budgets), ADR-008 (accessibilité, i18n, bidirectionnel) et feuille de
> route M3 — PIEGE-01 à PIEGE-06, PIEGE-14 et PIEGE-15 écrits avant le
> code. Ce jalon couvre plusieurs livraisons ; ce document suit
> l'avancement tranche par tranche.

## Tranche 1 — pièges de la vue et socle de rendu (2026-11-04)

### Livré

- **Les huit pièges de rendu écrits avant le code** (règle de
  traçabilité du registre des pièges, motif établi en M2) : tests nommés
  commités d'abord — échec en compilation documenté — puis socle de
  rendu qui les couvre. Registre des pièges à jour : PIEGE-01 à 06, 14
  et 15 **couverts**.
- **Socle de rendu JVM pur dans `android-view`**, testable sans SDK ni
  appareil (mesure de police injectée — ADR-008 ; surface de dessin par
  interface — approche proxy du spike de rendu, ADR-006) :
  - `EditorMetrics`, `FontSpec`, `FontRuler` : métriques immuables
    dérivées d'une règle injectée ; un changement de taille redérive par
    reconsultation, jamais par mutation en place (PIEGE-05) ;
  - `RowLayoutCache` (avec `RowKey`, `RowLayout`, `RowSource`) : cache à
    adressage ouvert, consultation par valeurs brutes sans allocation,
    clés immuables — révision, rangée, thème, police — et invalidation
    par révision (PIEGE-02) ;
  - `PainterLayer` / `TextPainter` / `SelectionPainter` (avec
    `BrushFactory`, `TextBrush`, `DrawSurface`, `FrameState`) : chaque
    couche possède son pinceau et repose sa couleur de base à chaque
    image — prouvé même sous la fabrique hostile qui partage un pinceau
    mutable entre toutes les couches (PIEGE-01) ;
  - `ScrollFrame` : tracé d'image à l'indice, état d'image muté sur
    place — preuve d'identité : cent une images de défilement, aucun
    objet nouveau ne croise l'interface après l'échauffement (PIEGE-15) ;
  - `SelectionDrag` : ancre figée au DOWN, la plage s'inverse proprement
    quand le doigt franchit la borne (PIEGE-03) ;
  - `FoldModel`, `VisualProjection` : projection lignes ↔ rangées qui
    passe par le modèle de plis en direct — jamais l'identité naïve
    « rangée = ligne » (PIEGE-04) ;
  - `HitTest` : conversion position ← pixel au plancher, discipline de
    visée au quart gauche du glyphe (PIEGE-06) ;
  - `TypefaceCache` (avec `FontHandle`, `TypefaceFactory`) : couple
    (base, gras) mémoïsé par valeur, invalidé au changement de police
    (PIEGE-14).

### Vérifié localement (sans appareil)

- `./gradlew check` vert — politique de langue et neutralité comprises,
  Error Prone + NullAway, tests Jupiter ;
- `:android-view:testDebugUnitTest` : **26 tests, zéro échec** — les
  huit pièges par leur nom de registre plus les tests de comportement
  (modèle de plis, projection, cache, visée, gestuelle) ;
- `:android-view:lintDebug --rerun-tasks` vert — lint bloquant, zéro
  référence de baseline.

### Écarts et limites

1. **Checkstyle et PMD ne se matérialisent pas sur les modules AGP** :
   les conventions appliquent bien les plugins, mais aucune tâche
   `checkstyle*`/`pmd*` n'existe sous AGP — constat préexistant depuis
   M1 (le module `demo` est dans le même cas), les plafonds 600/50/15 y
   sont donc tenus par discipline et non par porte. Le câblage explicite
   de ces tâches sur les variantes AGP demande une décision du
   propriétaire (build-logic, tous les modules Android) — à trancher
   avant la clôture M3.
2. **Colonnes monospace uniquement** : la couche de sélection calcule
   les colonnes en largeur uniforme (`charWidth` des métriques) ; la
   mesure par rangette (hors monospace) et le retour à la ligne souple
   par offsets (variante code de A-18) viennent avec la vue réelle.
3. **Pas encore de vue Android réelle** : la surface `DrawSurface`
   n'a pas d'implémentation `android.graphics` — c'est l'objet de la
   tranche suivante (vue Canvas, défilement, gouttière, thèmes), puis
   IME, clavier matériel et accessibilité.
4. **Tests instrumentés non exécutables ici** : pas d'appareil ni
   d'émulateur dans l'environnement d'exécution ; les critères de sortie
   M3 qui exigent l'appareil réel (budgets de rendu, gestes, IME,
   accessibilité) restent à vérifier sur le matériel du propriétaire.

## Prochaines tranches (prévues)

2. Vue Android réelle : `DrawSurface` sur Canvas, pinceaux réels,
   défilement fluide, gouttière (numéros), thèmes clair/sombre/contraste
   élevé (D-13), guides d'indentation (A-17).
3. Retour à la ligne souple par offsets (A-18, variante code) et très
   gros fichiers à l'ouverture (A-25, G-01 : 100 000 lignes ≤ 500 ms).
4. Tactile : poignées, pilule multi-curseur (A-01), pincement zoom
   (D-15), loupe ; clavier matériel (D-17).
5. IME par niveau d'API (26→36), composition par clusters de graphèmes.
6. Accessibilité : arbre virtuel, événements, `en`/`fr` complets ;
   scénarios de démo M3 (A-01, A-18, D-01 à D-17, G-01 à G-05) ;
   clôture du jalon.
