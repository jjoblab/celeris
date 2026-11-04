# Résumé M2 — moteur de texte (en cours)

> **Déclenchement** : validation M1 du propriétaire (2026-11-04, « M1 validé »,
> sans désignation d'appareil de référence — question 5 de M1 toujours ouverte).
> Périmètre : ADR-003 et feuille de route M2. Ce jalon couvre plusieurs
> livraisons ; ce document suit l'avancement tranche par tranche.

## Tranche 1 — portage de la rope (2026-11-04)

### Livré

- **`Rope` portée dans `core-text`** (registre de portage, décision « Porter »)
  : arbre équilibré par poids, feuilles de 512 caractères, fusion d'épines à
  la concaténation, rééquilibrage par invariant de Fibonacci, visite des
  feuilles avant et arrière avec arrêt anticipé. Adaptations consignées :
  paquet `jo.celeris.core.text`, contract `@NullMarked` (la tolérance à
  `null` de la fabrique disparaît), feuille et branche invisibles hors du
  paquet, `instanceof` à motifs (niveau 17).
- **Tests portés et étendus** : `RopeTest` (frontières exactes de MAX_LEAF,
  fusion d'épine aux bornes 512/513, identité des coupes pleines et du
  singleton vide, contrat de message hors bornes, frontières de feuilles à
  511/512/513 et 1023/1024/1025, rééquilibrage d'une chaîne déséquilibrée),
  `RopeVisitLeavesTest` (couverture exacte, miroir avant/arrière, clip des
  bornes, fenêtres alignées sur les frontières de feuilles),
  `RopeFuzzTest` (graines fixes — l'exigence de reproductibilité de la
  mutation remplace `System.nanoTime()`).
- **Propriétés jqwik à oracle `StringBuilder`** (`RopeProprietesTest`) :
  environ 10 300 séquences aléatoires par exécution, couvrant l'objectif
  ADR-003 (10 000) pour la rope — séquences d'éditions complètes,
  `charAt` au fil des insertions, sous-plages quelconques, immuabilité,
  survie des paires de substituts. Le harnais de démonstration M1 est
  retiré, remplacé par ces propriétés réelles.
- **Correctif de construction** : `apiCheck` capturait le nom de projet à
  l'exécution — toute divergence de manifeste échouait en erreur de cache
  de configuration au lieu de la consigne de régénération. Le nom est
  désormais une entrée de tâche posée à la configuration.
- **Ajustement du premier passage CI** (écart n° 1 du résumé M1, exaucé) :
  le travail « modules purs » voyait le SDK préinstallé du runner et
  entrait en configuration Android sans composants — le refus explicite
  `CELERIS_SKIP_ANDROID=1` prime désormais sur la détection de SDK ;
  `sdkmanager` s'appelle par chemin complet ; la tolérance Javadoc
  résout les source sets à la réalisation de la tâche (AGP 9 ne les pose
  pas à l'application du plugin — consultation hâtive fatale aux modules
  Android, jamais observée localement puisque sans SDK).
- **Deuxième ajustement CI** : le premier correctif de tolérance
  Javadoc relisait le conteneur de source sets DANS la lambda de tâche —
  `extensions` y désigne celles de la tâche, jamais trouvées : la
  publication locale échouait en CI (« No public or protected classes
  found to document » sur les squelettes). Retour au relevé à
  l'application (modules JVM), avec findByName null-sûr ; et les
  trois modules Android n'appliquent plus la convention de publication —
  AGP 9 ne pose ni « release » ni « java » dans le conteneur de composants,
  leur publication est différée à M2/M3 (écart n° 5 du résumé M1).
  Le travail « modules purs » est désormais rejoué intégralement en
  local avant chaque poussée (check, mutation, publication, fumée cli).
- **CI verte au cinquième passage** (2b7baa2, 2026-11-04) : les deux
  travaux passent toutes leurs étapes — critère de sortie M1 « CI verte
  sur le squelette » désormais satisfait pour de bon.

### Vérifié localement (sans SDK Android)

- `./gradlew check` vert (politique de langue 0 problème, neutralité 0
  problème, Checkstyle/PMD/Error Prone + NullAway, JaCoCo ≥ 90 % sur
  `core-text`, manifeste d'API régénéré et conforme) ;
- `./gradlew :core-text:mutationThreshold` vert — **score 82,2 %**
  (139 mutations tuées sur 169 ; survivants analysés : équivalents
  structurels — bornes de clip sans effet observable, branches de
  coalescence inatteignables par l'API publique) ;
- `publishToMavenLocal publicationChecksums` verts.

### Écarts et limites

1. **Les pièges PIEGE-07, PIEGE-08 et PIEGE-13 ne sont pas encore écrits** :
   ils précèdent leurs fonctionnalités (annulation transactionnelle,
   retour à la ligne souple, protocole de benchmark) conformément à la
   règle de traçabilité — tranches suivantes, tests d'abord.
2. **Index de lignes, undo coalescente, transactions multi-sélection,
   recherche/remplacement, UAX #29/#9, encodages et BOM** : à porter ou
   à spécifier — chemin critique de M2.
3. **JMH ≤ 1 ms/édition sur 50 000 lignes** : le harnais de mesure existe
   (spike M0) ; l'intégration benchmark de CI reste à brancher.
4. **Gel de l'API `core-text` en 0.1.0** : attend la fin du périmètre M2 —
   le manifeste suit chaque évolution d'ici là.

### Prochaine tranche (ordre imposé)

1. `PIEGE-07` — test `undoReplay_goesThroughTransactionPipeline` AVANT le
   moteur d'annulation (registre, coalescence portée du dépôt de départ).
2. `PIEGE-08` — test `softWrap_breaksOversizedWordByCharacter` AVANT le
   modèle de retour à la ligne pur.
3. `PIEGE-13` — protocole « minimum de trois tours » dans le harnais de
   benchmark.

## Tranche 2 — pièges du texte : PIEGE-07, PIEGE-08, PIEGE-13 (2026-11-04)

> L'ordre imposé en fin de tranche 1 est exécuté : les trois tests nommés ont
> été écrits et laissés en échec de compilation AVANT toute ligne
> d'implantation (règle de traçabilité du registre des pièges), puis le code
> les a rendus verts.

### Livré

- **PIEGE-07 couvert** — test `undoReplay_goesThroughTransactionPipeline`
  (Javadoc PIEGE-07) : le rejeu de l'annulation passe par la voie de
  transaction unique — plage ancrée revenue à sa position d'origine, auditeur
  d'édition notifié de l'édition inverse, garde lecture seule sans
  consommation d'étape, redo symétrique sans pollution de la pile.
- **Moteur d'annulation transactionnel** (ADR-003) : `Document`
  (instantané immuable : rope + révision), `TextEdit` (édition
  auto-inversible portant son texte retiré), `Transaction` (immuable,
  composable, inversible — la seule voie de mutation), `EditSpan` (plage
  appliquée aux projecteurs), `TextSession` (validation des bornes par
  arithmétique séquentielle strictement AVANT toute mutation, texte retiré
  relu sur l'instantané et toute divergence refusée avant de muter,
  écrivain unique sans verrou interne, ADR-007).
- **Coalescence portée** (registre : `UndoManager`/`UndoRecorder`) :
  `UndoStack` à horloge injectée (première consommatrice de l'action M2 de
  l'ADR-007 — le moteur n'appelle jamais l'horloge système), `UndoStep` à
  copie défensive, amorçage de chaîne porté avec son correctif (la condition
  d'adjacence était inatteignable sans lui). Adaptation consignée : le lot
  multi-éditions remplace `begin/endBatch` — une transaction entière est une
  étape d'annulation.
- **Projecteurs de positions** : contrat `PositionProjector` +
  `AnchoredRanges` — portage du décalage du pipeline commun (gravité droite
  des débuts, gauche des fins, chute des plages réduites à rien), cité par
  PIEGE-07 ; les types porteurs (diagnostics, plis, composition IME)
  arrivent avec leurs modules.
- **Propriété jqwik à oracle `StringBuilder`** : 250 suites aléatoires
  d'éditions par exécution — défaire toutes les étapes restaure le texte
  initial, les refaire toutes restaure le texte final (coalescence comprise).
- **PIEGE-08 couvert** — test `softWrap_breaksOversizedWordByCharacter`
  (Javadoc PIEGE-08), écrit avant le modèle : `SoftWrap`, modèle pur à
  mesure injectée (`Measurer`) — accumulation gloutonne mot à mot (une
  mesure par mot), coupe par caractères d'un mot plus large que la ligne par
  recherche dichotomique (le test COMPTE les mesures : une dizaine par coupe
  contre une par caractère), queue du mot coupé ouvrant la rangée au mot
  suivant, sémantique héritée du kit (sauts explicites, paragraphe tout
  blanc rendu tel quel, aucune espace en tête de rangée coupée, jamais moins
  d'un caractère par rangée).
- **PIEGE-13 couvert** — harnais `BenchmarkHarness` (outillage de test,
  ADR-007) : phases chronométrées séparément à chaque tour, trois tours par
  phase, minimum conservé par phase (le redo réarme la pile d'annulation
  pour le tour suivant, hors mesure). Test `undoBenchmarkUsesMinOfThreeRuns`
  sur horloge scriptée : trois tours exactement, tour bruité écarté.
  Mesure réelle locale : undo × 100 / redo × 100 sur 2 000 lignes —
  **0,22 ms / 0,25 ms** (budget 2 s, protection structurelle).

### Vérifié localement (sans SDK Android)

- `./gradlew check` vert — politique de langue 0 problème, neutralité
  0 problème, Checkstyle/PMD/Error Prone + NullAway, JaCoCo ≥ 90 % sur
  `core-text`, manifeste d'API régénéré et conforme ;
- `./gradlew :core-text:mutationThreshold` vert — **85,5 % détectés au sens
  de PIT** (343 KILLED + 8 TIMED_OUT + 3 MEMORY_ERROR sur 414) ; 82,9 % en
  KILLED seuls, au-dessus du seuil sous les deux sémantiques ;
- `publishToMavenLocal publicationChecksums` et `:cli:run` verts ;
- **CI verte au premier passage** (635a818, 2026-11-04) : les deux travaux
  passent — le rejou complet en local avant poussée (discipline prise en
  tranche 1) a tenu sa promesse.

### Écarts et limites

1. **Deux écarts d'instrument découverts et corrigés** : la porte PMD
   tournait à vide depuis M1 (le jeu de règles référençait des règles
   retirées de PMD 7.29.0 — aucun fichier analysé, tâche verte ; plafonds
   600/50 toujours portés par Checkstyle, seuil par méthode 15 inchangé et
   devenu effectif, seuil de classe à la valeur par défaut de la règle) ;
   la mutation ciblait aussi les classes internes et utilitaires de test
   (cibles désormais limitées au source set principal, score au sens
   canonique de PIT — le seuil de 80 % est inchangé et la tranche passe
   aussi en KILLED seuls).
2. **Survivants analysés** : équivalents structurels (frontières masquées
   par les gardes internes, doubles validations rope/document) — même classe
   d'équivalence que l'analyse de la rope en tranche 1.
3. **Restauration de sélection (A-13)** : attend la multi-sélection —
   l'étape d'annulation ne porte encore que des éditions.
4. **Variante code du retour à la ligne** (blancs préservés, coupes par
   offsets dans la ligne du document) : vient avec la vue (M3).
5. **Types forts (`DocOffset`)** : toujours pas introduits — les API ajoutées
   restent en `int`, cohérentes avec la rope portée ; à trancher avant le gel
   0.1.0 de `core-text`.
6. **Index de lignes, recherche/remplacement, UAX #29/#9, encodages et BOM,
   benchmark ≤ 1 ms/édition sur 50 000 lignes en CI** : tranches suivantes
   (le harnais de mesure existe, la tranche suivante branche la mesure).

### Prochaine tranche (ordre imposé)

1. Portage de l'index de lignes dans `Document` — la voie de mutation unique
   est en place, la transaction maintiendra l'index (ADR-003).
2. Invariants croisés au fuzz : document ↔ index de lignes (oracle
   `StringBuilder` étendu).
3. Benchmark de référence branché : ≤ 1 ms/édition sur 50 000 lignes,
   minimum de trois tours par phase (PIEGE-13 appliqué).
