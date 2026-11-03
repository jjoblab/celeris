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
