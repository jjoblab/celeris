# Résumé M2 — moteur de texte (clos)

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
  l'application (modules JVM), avec findByName null-sûr ; et les
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

## Tranche 3 — index de lignes (2026-11-04)

> L'ordre imposé en fin de tranche 2 est exécuté : `DocumentIndexTest` a été
> écrit et laissé en échec de compilation AVANT toute ligne d'implantation
> (82 erreurs constatées), puis le portage l'a rendu vert.

### Livré

- **Index de lignes porté dans `Document`** (registre de portage, décision
  « Porter ») : tableau parallèle des débuts de ligne (l'entrée 0 vaut 0,
  chaque suivante suit un saut de ligne), recherche dichotomique offset →
  ligne, requêtes de début, de fin de contenu et de contenu de ligne,
  bornes clampées, seuils « gros document » (2,5 M caractères / 50 k lignes).
  Le remplacement splices l'index incrémentalement : préfixe réutilisé,
  sauts de ligne du remplacement scannés, suffixe décalé du delta.
- **L'index est maintenu par la voie de mutation unique** (ADR-003) : la
  transaction, l'annulation et le refaire s'appliquent tous par
  `Document.replace` — aucun chemin de mutation ne contourne le splice
  (famille PIEGE-07).
- **Mémo du texte complet** porté avec sa documentation de sûreté : champs
  finaux (publication JMM §17.5), référence volatile à course bénigne,
  référence stable sur un même instantané (testée par identité).
- **Invariants croisés au fuzz** (jqwik, 250 suites par exécution) : oracle
  `StringBuilder` étendu — l'index de lignes de l'instantané doit coïncider,
  entrée par entrée, avec l'index recalculé par balayage du miroir, après
  chaque édition ET après l'annulation intégrale puis le refaire intégral.
- **Benchmark d'édition de référence branché** (budget ADR-006) : document
  de 50 000 lignes (≈ 2 M caractères), mille remplacements d'un caractère
  par tour PAR la voie complète de la session (lecture du texte retiré,
  splice de la rope, maintien de l'index, enregistrement d'étape), positions
  déterministes à graine fixe, minimum de trois tours (PIEGE-13).
  **Mesure locale : 0,055 ms par édition** — budget 1 ms, marge ≈ 18×.

### Vérifié localement (sans SDK Android)

- `./gradlew check` vert — politique de langue 0 problème, neutralité
  0 problème, Checkstyle/PMD/Error Prone + NullAway, JaCoCo ≥ 90 % sur
  `core-text`, manifeste d'API régénéré et conforme (7 méthodes publiques
  ajoutées à `Document`) ;
- `./gradlew :core-text:mutationThreshold` vert — **86,1 % détectées au sens
  de PIT** (407 KILLED + 11 TIMED_OUT + 3 MEMORY_ERROR sur 489) ; 83,2 % en
  KILLED seuls, au-dessus du seuil sous les deux sémantiques ;
- `publishToMavenLocal publicationChecksums` et `:cli:run` verts ;
- **CI verte au premier passage** (508b013, 2026-11-04) : les deux travaux
  passent toutes leurs étapes — le rejou complet en local avant poussée
  (discipline prise en tranche 1) a tenu sa promesse une fois de plus.

### Écarts et limites

1. **Survivants analysés** (10 sur le code neuf, tous équivalents
   structurels, même classe que les tranches 1 et 2) : la garde du document
   vide de `computeLineStarts` et le retour hâtif de plage vide de `subText`
   coïncident avec le chemin général ; les bornes clampées de
   `lineForOffset` coïncident avec la recherche dichotomique aux frontières
   exactes ; le retour hâtif de ligne vide de `lineText` coïncide avec
   `subText(x, x)` ; le ternaire `(end > start)` du splice est équivalent
   quand `end == start` (même offset) ; la garde sans effet de `replace`
   est doublement couverte par la session.
2. **Coût du splice** : la recopie du suffixe de l'index domine l'édition
   (0,055 ms mesuré contre 0,002 ms pour la rope seule — cohérent avec la
   mesure d'insertion de ligne du spike, 39 µs). C'est le compromis
   consenti par l'ADR-003 (index à côté, plat) ; un index arborescent ne
   se justifierait que si le budget était menacé — il ne l'est pas (marge
   ≈ 18×).
3. **Adaptations consignées au portage** : fabrique intolérante au nul
   (contract `@NullMarked`), `subText` strict conservé (le dépôt de départ
   clampe), tests de paresse par réflexion sur le mémo privé non repris
   (style maison déterministe, aucune réflexion), vue `CharSequence` non
   reprise à ce stade (aucun consommateur — retournera avec les helpers de
   frappe).
4. **Undo/redo × 100 (2 000 lignes)** : 0,79 ms / 0,78 ms mesurés (contre
   0,22 ms / 0,25 ms en tranche 2) — le rejeu d'annulation maintient
   désormais aussi l'index ; budget 2 s, marge massive.
5. **Restauration de sélection (A-13)** : attend la multi-sélection.
6. **Variante code du retour à la ligne** : vient avec la vue (M3).
7. **Types forts (`DocOffset`)** : toujours pas introduits — à trancher
   avant le gel 0.1.0 de `core-text`.
8. **Recherche/remplacement, UAX #29/#9, encodages et BOM** : tranches
   suivantes.

### Prochaine tranche (ordre imposé)

1. Portage de la recherche/remplacement dans `core-text` (registre : les
   tests du dépôt de départ deviennent l'oracle).
2. Multi-sélection de première classe (ADR-003) : transactions
   multi-plages, restauration des sélections à l'annulation (A-13).

## Tranche 4 — recherche/remplacement (2026-11-04)

> L'ordre imposé en fin de tranche 3 est exécuté : les tests du moteur de
> recherche (oracle du dépôt de départ) et ceux de la passe de
> remplacement ont été écrits et laissés en échec de compilation AVANT
> toute ligne d'implantation (100 erreurs constatées), puis le portage les
> a rendus verts.

### Livré

- **Moteur de recherche porté** (registre de portage, décision « Porter »)
  : `FindReplace` à horloge injectée en millisecondes (ADR-007 — le
  garde-fou du dépôt de départ appelait `System.nanoTime` en code de
  production, consigné), `FindOptions` et `Match` en enregistrements
  immuables, recherche littérale, mot entier et regex. Le garde-fou D-05
  est porté : budget temps de 250 ms par recherche regex (échéance
  échantillonnée tous les 1 024 accès du texte, interruption du fil
  honorée), plafond de collecte à 10 000 correspondances, états
  `truncated` / `tooCostly` exposés pour signal à l'interface. La vue
  garde-fou (`GuardedCharSequence`) est visible du paquet pour ses tests.
- **Occurrences du mot sous le caret portées** : `WordOccurrences` —
  `wordAt` renvoie un `Match` typé nullable (le `int[]` du départ devient
  une plage typée, contrat `@NullMarked`), éligibilité (identifiants,
  jamais les littéraux numériques ni la ponctuation), collecte des
  occurrences entières plafonnée (frontières identifiant : `_` et `$` ne
  sont pas des frontières, contrairement au mode « mot entier »).
- **Correctif porté, consigné** : la casse insensible du départ passait
  par la minuscule complète du texte — pour les caractères dont la
  minuscule change la longueur (U+0130), les indices hérités désignaient
  un texte qui n'était pas celui d'origine. Quand une longueur change
  (texte ou requête), le repli par comparaison régionale conserve les
  indices du texte original — tests dédiés (requête exotique, texte
  exotique, les deux).
- **Passe de remplacement spécifiée** (le contrôleur du dépôt de départ
  sert de spécification, registre) : segments d'origine + remplacements
  développés (groupes `$n` en mode regex par `appendReplacement` dans un
  tampon jetable), motif invalide ou remplacement mal formé ou budget
  dépassé → AUCUN remplacement (le texte n'est jamais muté à moitié).
  Adaptations consignées : les correspondances de remplacement ne se
  recouvrent jamais (la collecte peut recouvrir pour la surbrillance, la
  passe avance au-delà de chaque occurrence remplacée — le contrôleur du
  départ plantait sur les candidats recouvrants) ; la passe est plafonnée
  à 10 000 remplacements, miroir du plafond de collecte (la passe regex du
  départ était non bornée — état `truncated` exposé) ; une correspondance
  de largeur nulle au remplacement vide ne crée rien (une édition sans
  effet n'a rien à faire sur la voie de mutation) — le correctif sépare le
  curseur de copie de la position d'ajout du matcher, la queue n'est
  copiée que depuis le curseur (jamais par `appendTail`).
- **Remplacement global par la session** : `TextSession.replaceAll` —
  lecture du texte courant, passe du moteur à l'horloge de la session,
  puis mutation PAR LA VOIE UNIQUE (famille PIEGE-07) : les éditions, une
  par occurrence en ordre du document, sont ajustées en deltas cumulés
  pour la sémantique séquentielle de la `Transaction`. Tout le
  remplacement forme UNE étape d'annulation — empilement direct, jamais
  coalescée avec les frappes (une commande n'est pas une frappe, la
  frappe suivante ne fusionne pas non plus). Annulation et refaire
  rejouent par la même voie — index de lignes, projections et auditeurs
  compris ; la lecture seule refuse sans créer d'étape.
- **Propriétés jqwik à graine déterministe** : le remplacement littéral
  coïncide avec l'oracle `String.replace` du JDK ; les éditions ajustées
  en deltas cumulés reconstruisent le texte du résultat ; par la session,
  tout remplacement s'annule intégralement en une étape.
- **Benchmark de remplacement branché** (budget ADR-006, PIEGE-13) :
  document de 50 000 lignes (≈ 2 M caractères), 1 000 occurrences,
  remplacement global par la voie complète de la session puis annulation,
  chaque phase chronométrée séparément, minimum de trois tours.
  **Mesure locale : 0,076 ms par occurrence** (annulation : 32 ms) —
  budget de protection 1 ms par occurrence, marge ≈ 13×.

### Vérifié localement (sans SDK Android)

- `./gradlew check` vert — politique de langue 0 problème, neutralité
  0 problème, Checkstyle/PMD/Error Prone + NullAway, JaCoCo ≥ 90 % sur
  `core-text`, manifeste d'API régénéré et conforme (cinq classes publiques
  nouvelles : `Match`, `FindOptions`, `FindReplace` et ses enregistrements
  imbriqués, `WordOccurrences`) ;
- `./gradlew :core-text:mutationThreshold` vert — **87,5 % détectées au
  sens de PIT** (589 KILLED + 12 TIMED_OUT + 3 MEMORY_ERROR sur 690) ;
  renforcement ciblé après analyse des survivants (frontières des retours
  de troncature, branches non couvertes du garde-fou — l'échantillonnage
  tire aussi dans `length()` et `subSequence()` —, demi-conditions de la
  vue de recherche) : le score passe de 85,0 % à 87,5 % au fil de la
  tranche ;
- `publishToMavenLocal publicationChecksums` et `:cli:run` verts ;
- **CI verte au premier passage** (20371b1, 2026-11-04) : les deux travaux
  passent toutes leurs étapes — le rejou complet en local avant poussée
  (discipline prise en tranche 1) a tenu sa promesse une fois de plus.

### Écarts et limites

1. **Survivants analysés** (86 non détectés, même classe que les tranches
   précédentes) : bornes d'échantillonnage du garde-fou (un contrôle
   décalé d'un accès reste dans l'échelle de la milliseconde),
   conditions de sortie de boucle redondantes avec l'épuisement des
   candidates (la candidate suivante renvoyée hors bornes ne produit
   aucune correspondance), frontières de boucles équivalentes par
   construction — la précédente classe d'équivalence des tranches 1 à 3.
2. **Troncature de la passe de remplacement** : les 10 000 premiers
   remplacements sont appliqués, la queue d'origine est préservée et les
   états sont exposés par le moteur pur (`truncated`) — la session ne
   renvoie que le compte appliqué ; l'interface (M5) porte la charge du
   signal et du découpage en passes si besoin.
3. **Restauration de sélection (A-13)** : attend la multi-sélection —
   l'étape d'annulation du remplacement ne porte encore que des éditions.
4. **Types forts (`DocOffset`)** : toujours pas introduits — à trancher
   avant le gel 0.1.0 de `core-text`.
5. **UAX #29/#9, encodages et BOM** : tranches suivantes du chemin
   critique.
6. **Benchmark d'édition de référence** : la mesure du jour (0,109 ms par
   édition) reste sous le budget de 1 ms ; l'écart avec la mesure de la
   tranche 3 (0,055 ms) est le bruit du runner local — le minimum de
   trois tours écarte le bruit en CI (PIEGE-13), la protection reste
   structurelle.

### Prochaine tranche (ordre imposé)

1. Multi-sélection de première classe (ADR-003) : transactions
   multi-plages, restauration des sélections à l'annulation (A-13).
2. UAX #29/#9 (segmentation de texte et espaces) puis encodages et BOM.

## Tranche 5 — multi-sélection de première classe (2026-11-04)

> L'ordre imposé en fin de tranche 4 est exécuté : les tests de la
> multi-sélection et de la restauration A-13 ont été écrits et laissés en
> échec de compilation avant toute ligne d'implantation, puis le code les a
> rendus verts — quatre commits (test, plages, session, propriétés).

### Livré

- **`SelRange` et `MultiSelection`** : plages ancre-curseur (direction
  comprise), liste normalisée — triée par début, fusionnée aux
  recouvrements, dédupliquée —, projection à travers une édition à gravité
  droite (ADR-003) : la plage au point d'insertion suit le texte inséré,
  la plage couverte se rabat, les suivantes se décalent, puis
  re-normalisation. Le modèle mono-intervalle du dépôt de départ est
  remplacé (parité A-01), conformément au registre de portage qui le
  classait « abandonner (remplacer) ».
- **La session porte la sélection** : chaque étape d'historique porte les
  sélections d'avant et d'après ; annuler restitue le texte ET la
  sélection exacte d'avant (pluralité, direction et positions comprises),
  refaire celle d'après (A-13). La frappe multi-curseurs est une
  transaction ordinaire par la voie unique.
- **Coalescence étendue à la frappe multi-curseurs** : les curseurs
  frappés doivent être exactement ceux laissés par la frappe précédente —
  le cas mono-curseur retrouve la règle d'adjacence du dépôt de départ.
- **Propriétés jqwik à graine déterministe** (`SelectionProprietesTest`) :
  suites de frappes à un à trois curseurs aléatoires, horloge au-delà de
  la fenêtre de coalescence ; chaque annulation restitue le texte et la
  sélection exacts d'avant le geste contre oracle naïf ; la projection à
  travers une édition aléatoire est monotone, bornée par la longueur
  d'après et préserve la disjointure après normalisation.

### Vérifié localement (sans SDK Android)

- `./gradlew check` vert — manifeste d'API régénéré et conforme (cinq
  signatures nouvelles, additions seulement : `MultiSelection`, `SelRange`
  et les méthodes de sélection de la session) ;
- `./gradlew :core-text:mutationThreshold` vert.

### Écarts et limites

1. **UAX #29/#9, encodages et BOM** : tranche suivante du chemin critique.
2. **Types forts (`DocOffset`)** : toujours pas introduits — à trancher
   avant le gel 0.1.0 de `core-text`.

### Prochaine tranche (ordre imposé)

1. UAX #29 (clusters de graphèmes, segmentation de mots) — tests écrits
   avant le code.
2. UAX #9 (résolution bidirectionnelle) puis encodages et BOM.

## Tranche 6 — UAX #29/#9, encodages et BOM, suppression par clusters (2026-11-04)

> Dernière tranche du jalon : la charge Unicode du modèle de texte. Les
> tests de chaque brique ont été écrits et laissés en échec de compilation
> avant l'implantation (huit commits : deux par brique plus le geste de
> suppression), puis le renforcement de la mutation a clos l'écart au
> seuil avec un correctif de conformité UAX #29 consigné.

### Livré

- **Clusters de graphèmes (`Grapheme`, UAX #29 révision 15)** : règles
  GB3 à GB999 ; classes de rupture dérivées des catégories générales
  Unicode complétées par les plages propres à UAX #29 (préfixes,
  indicateurs régionaux, jamo hangûl, pictogrammes étendus, modificateurs
  d'emoji). Limite documentée : les pictogrammes attribués après U+1FFFD
  tombent en classe ANY et se segmentent caractère par caractère.
- **Segmentation de mots (`Word`, règles WB3-WB999)** : classes
  `WbClass`, classificateur `WbClassifier` ; l'élision française, les
  séparateurs décimaux, les tirets bas, les kana et les fins de ligne y
  retrouvent la sémantique de la norme. **Correctif de conformité
  consigné** : le regard des séparateurs médians (WB7, WB12) comparait le
  caractère directement précédent au lieu du précédent effectif — une
  marque combinante collée au séparateur coupait le mot que la norme
  garde entier ; le précédent effectif porte désormais sa position
  (enregistrement interne `Effective`) et les tests « a.◌̱b » /
  « 1,◌̱2 » échouaient avant le correctif et passent après.
- **Résolution bidirectionnelle (`Bidi`, UAX #9)** : niveau de paragraphe
  par premier caractère fort hors isolats (P2-P3), enchâssements et
  isolats explicites (X1-X8), types faibles (W1-W7), neutres (N1-N2),
  niveaux implicites (I1-I2), blancs de fin de ligne (L1),
  réordonnancement visuel (L2) ; `BidiInfo` est une classe à accesseurs
  (niveaux, ordres visuel et logique, niveau à l'unité, direction).
  Limites documentées : N0 (paires de crochets) non implémentée ;
  contrôles explicites conservés comme bornes neutres invisibles (X9,
  approche classique) ; l'entrée est une ligne, les séparateurs de
  paragraphe relèvent de l'appelant. La classification vit dans
  `BidiClassifier`, les enchâssements dans `BidiEmbeddings`, les courses
  dans `BidiRuns`, visibles du paquet.
- **Encodages et BOM (`Encoding`, `EncodingCodec`, `Utf8Codec`,
  `WideCodecs`)** : détection par BOM toutes familles — l'UTF-32 l'emporte
  sur le préfixe partagé avec l'UTF-16 —, heuristique bornée pour
  l'UTF-16 et l'UTF-32 sans BOM (le Latin-1 n'est jamais auto-détecté),
  décodage strict dont les erreurs portent offsets et remplacement U+FFFD,
  ré-encodage refusant les substituants isolés, BOM retiré au décodé et
  optionnel au ré-encodage, mode de fin de ligne dominant exposé (LF,
  CRLF, CR, mixte). `Utf8Codec` est la machine à états UTF-8 (survols et
  hors-plage rejetés aux bornes exactes de chaque longueur de séquence),
  `WideCodecs` les codecs UTF-16 et UTF-32 des deux boutismes (A-24).
- **Suppression arrière par clusters (`TextSession.deleteBackward`)** :
  les plages non vides deviennent vides, chaque curseur retire le cluster
  de graphèmes qui le précède — fenêtre croissante depuis l'offset
  jusqu'à une frontière certaine, un cluster plus long que la fenêtre
  l'élargit au lieu d'être coupé —, les plages de curseurs voisins
  fusionnent au toucher, les positions séquentielles s'ajustent en deltas
  cumulés et tout passe par la voie de transaction unique (famille
  PIEGE-07). La coalescence s'étend aux gestes : frappe ou suppression
  d'un caractère par curseur (A-13 : le geste s'annule en restituant la
  sélection).
- **Renforcement ciblé de la mutation** (après analyse des survivants,
  discipline des tranches précédentes) : les règles faibles W2, W4, W5 et
  W7 de UAX #9 et les neutres N1 n'étaient observées par aucun test ; les
  bornes exactes des plages UAX #29 (indicateurs régionaux,
  modificateurs d'emoji, syllabes et jamo hangûl), les octets canoniques
  des frontières UTF-8, le refus des survols et hors-plage un cran avant
  ou après la borne, les allers-retours au-delà du plan multilingue de
  base et la fenêtre croissante de la suppression sont désormais testés.
- **Scénarios de démo M2 au catalogue** (`demo`) : A-13, A-18 (facette
  cœur), A-20, A-24 et A-25 (facette modèle) dans `ScenarioCatalog`, avec
  test de fumée `ScenarioCatalogTest` (présence, ordre de la matrice,
  format, unicité, complétude des critères) — compilé et exécuté par la
  CI avec le SDK.

### Vérifié localement (sans SDK Android)

- `./gradlew check` vert — politique de langue 0 problème, neutralité
  0 problème, Checkstyle/PMD/Error Prone + NullAway, JaCoCo ≥ 90 % sur
  `core-text` (couverture ligne des classes mutées : 97 %), manifeste
  d'API régénéré et conforme (25 sections nouvelles au fil de la tranche,
  additions seulement) ;
- `./gradlew :core-text:mutationThreshold` vert — **85,1 % détectées au
  sens de PIT** (1513 tuées sur 1777, force de test 86 %) ; le premier
  passage de la tranche mesurait 79,2 % (1408/1777) : le renforcement
  ciblé a tué 105 mutants et réduit les mutants sans couverture de 53 à
  26 ;
- `publishToMavenLocal publicationChecksums` et `:cli:run` verts (rejou
  complet avant poussée, discipline prise en tranche 1).

### Écarts et limites

1. **N0 (paires de crochets, UAX #9)** : non implémentée — limite
   documentée dans la Javadoc de `Bidi` ; les textes qui en dépendent
   relèvent du sous-ensemble classique.
2. **Pictogrammes après U+1FFFD** : se segmentent caractère par
   caractère — les pictogrammes attribués ultérieurement exigent une mise
   à jour des plages (limite documentée).
3. **Types forts (`DocOffset`)** : toujours pas introduits — à trancher
   avant le gel 0.1.0 de `core-text` (reporté de tranche en tranche,
   jamais bloquant).
4. **Navigation par mots côté session** : `Word` expose les frontières ;
   les méthodes de saut (mot suivant, mot précédent, casse chameau pour
   A-04) viennent avec la vue (M3/M5).

## Passage de porte — sortie M2

Critères de sortie de la feuille de route, vérifiés :

- **fuzz et oracle verts** : rope (≈ 10 300 séquences à oracle
  `StringBuilder`), invariants croisés document/index, remplacement à
  oracle `String.replace`, sélection contre oracle naïf, permutation
  bidirectionnelle — tous à graines fixes (exigence de reproductibilité
  de la mutation) ;
- **JMH ≤ 1 ms/édition sur 50 000 lignes** : la mesure locale de la
  tranche 4 (0,109 ms par édition, minimum de trois tours) reste la
  référence ; la tranche 6 n'ajoute aucune voie d'édition nouvelle ;
- **mutation ≥ 80 %** : 85,1 % (1513/1777) après renforcement ciblé ;
- **pièges liés au texte couverts** : PIEGE-07, PIEGE-08 et PIEGE-13
  écrits avant le code (tranche 2) et couverts depuis ;
- **scénarios de démo A-13, A-18, A-20, A-24, A-25** : au catalogue avec
  test de fumée.

Le jalon M2 est clos ; la suite est M3 (vue minimale) — PIEGE-01 à
PIEGE-06, PIEGE-14 et PIEGE-15 à écrire avant le code, rendu Canvas sans
allocation par image, curseurs tactiles, IME par niveau d'API.
