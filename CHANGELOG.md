# CHANGELOG

Tous les changements notables de ce projet sont consignés ici, par version,
en français. Format calqué sur « Keep a Changelog » ; versions suivant
SemVer. **Court par construction** : une version = une entrée ; les détails
vivent dans les ADR et le registre de portage, pas ici.

## [Non publié]

### Ajouté

- Tranche 6 (M3) — accessibilité, BiDi au rendu, scénarios et
  ressources : granularités de mouvement (clusters, mots, rangées,
  pages — une seule sémantique avec le clavier matériel), arbre
  virtuel de rangées visibles (lecture par lignes et plages, G-05,
  jamais le document entier), colle d'accessibilité (nœud éditable,
  enfants virtuels, annonces de sélection et de contenu) ; segments
  visuels bidirectionnels (règle L2, détection RTL sans allocation,
  tracé segment par segment, chemin rapide inchangé — PIEGE-15) ;
  Checkstyle et PMD deviennent bloquants sous AGP (écart tranche 1
  résolu, deux violations corrigées) ; scénarios M3 au catalogue
  (quatorze) et ressources en/fr complètes ; 562 tests verts, l'APK
  debug s'assemble.
- Tranche 5 (M3) — IME par niveau d'API 26 à 36 : machine de
  composition pure (frappe composée remplace la plage, jamais un
  append ; curseur relatif à la fin moins un ou au début,
  sémantique des sources de la plateforme ; région alignée aux
  clusters de graphèmes par fenêtres croissantes ; retour arrière
  dans la composition par cluster entier) ; fenêtres et
  suppressions d'entourage jamais une demi-paire de substitution ;
  défilement minimum de dévoilement du curseur ; soulignement de
  composition borné aux tranches de rangée (PIEGE-04) ; les 38
  méthodes de la surface 36 implantées sans repli (biseauté sur
  les archives 26/31/33/34/35/36 : rien après le 34) ; toute
  édition passe par la voie de transaction unique, capacités
  absentes assumées et documentées (fenêtres plutôt que texte
  entier, G-01) ; 125 tests verts.
- Tranche 4 (M3) — tactile complet et clavier matériel : pincement
  zoom par paliers bornés, point sous le doigt stable (D-15) ; pilule
  « ajouter un curseur » — l'appui long arme, l'appui ajoute, les
  existants tiennent (A-01) ; poignées de plage dont le glissement ne
  déplace qu'une extrémité, alignées aux tranches de rangée (A-18) ;
  table de raccourcis complète, remappage sans mutation, conflit
  refusé en nommant l'action, encodage dictionnaire pour la
  persistance hôte, mouvements par clusters, mots entiers et rangées,
  sélection l'ancre figée (D-17) ; suppression avant par clusters de
  graphèmes au cœur, symétrique de l'arrière (`ClusterDeletes`,
  manifeste d'API régénéré, mutation 85,2 %) ; géométrie de loupe
  bornée à la fenêtre ; 87 tests verts.
- Tranche 3 (M3) — retour à la ligne souple par offsets, très gros
  fichiers : la projection signe plis ET coupures (sommes préfixées en
  un passage, budget d'au moins une colonne — PIEGE-04/08), la clé de
  cache signe le budget (PIEGE-02), la rangée porte sa tranche (texte,
  offsets, largeur) ; gouttière muette sur les continuations,
  sélection bornée à la tranche ; vue : `setSoftWrap`,
  panoramique à deux axes, défilement horizontal borné ; correctif du
  peintre de texte — il consulte la source courante par
  `RebindingRowSource`, jamais une capture périmée ; première image de
  100 000 lignes mesurée à 154 ms (budget 500 ms, G-01), 480 000 lignes
  ouvrables ; 65 tests verts.
- Tranche 2 (M3) — vue Canvas réelle et chrome : thèmes clair, sombre,
  contraste élevé à contraste WCAG vérifié automatiquement (D-13) ;
  gouttière à numéros absolus alignés à droite, étiquettes mémoïsées
  (D-01, facette numéros) ; géométrie de défilement borné ; `EditorView`
  — une seule vue, trois couches, cache par révision, curseur posé au
  DOWN et sélection à ancre figée, panoramique à deux doigts — et ses
  adaptateurs Android (surface par image, pinceaux possédés, règle de
  police réelle) ; lint Android sans aucun problème, 51 tests verts.
- Tranche 1 (M3) — pièges de la vue écrits avant le code (PIEGE-01 à 06,
  14, 15, registre à jour) : socle de rendu JVM pur dans `android-view`,
  testable sans SDK — métriques à règle de police injectée, cache de
  rangées à clés immuables et consultation sans allocation, couches à
  pinceau possédé et couleur de base reposée à chaque image, projection
  rangées ↔ lignes par le modèle de plis, gestuelle à ancre figée au
  DOWN, conversion position ← pixel au plancher, gras mémoïsé jusqu'au
  changement de police, tracé d'image à l'indice (26 tests verts).
- Tranche 5 — multi-sélection de première classe (ADR-003) : plages
  ancre-curseur `SelRange`, liste normalisée `MultiSelection` (fusion aux
  recouvrements, projection à gravité droite), la session porte la
  sélection et la restitue à l'annulation (A-13) ; frappe multi-curseurs
  par la voie de transaction, coalescence étendue ; propriétés jqwik
  contre oracle naïf.
- Tranche 6 — UAX #29/#9, encodages et BOM : clusters de graphèmes
  (`Grapheme`, GB3-GB999 révision 15) et segmentation de mots (`Word`,
  WB3-WB999) ; résolution bidirectionnelle (`Bidi`, P2-P3, X1-X8, W1-W7,
  N1-N2, I1-I2, L1, L2 — N0 non implémentée, limite documentée) ;
  détection par BOM toutes familles, heuristique bornée, décodage strict
  à erreurs par offsets, ré-encodage refusant les substituants isolés,
  fins de ligne dominantes (`Encoding`, `EncodingCodec`, `Utf8Codec`,
  `WideCodecs`) ; suppression arrière par clusters entiers, tous
  curseurs, plages fusionnées au toucher, par la voie unique
  (PIEGE-07) ; scénarios de démo M2 au catalogue (A-13, A-18, A-20,
  A-24, A-25) avec test de fumée ; mutation 85,1 % (1513/1777, seuil
  80 %).
- Tranche 2 — pièges du texte, tests écrits avant le code (PIEGE-07/08/13,
  registre des pièges à jour) : moteur d'annulation transactionnel dans
  `core-text` — la `Transaction`, seule voie de mutation (ADR-003), le rejeu
  de l'annulation par cette même voie, coalescence des frappes portée à
  horloge injectée (ADR-007), instantané `Document` immuable, projecteurs de
  positions (plages ancrées, gravités du pipeline commun), propriété jqwik à
  oracle `StringBuilder` (défaire/refaire tout restaure les textes).
- Tranche 3 — index de lignes porté dans l'instantané `Document` (registre de
  portage) : débuts de ligne en tableau parallèle, recherche dichotomique,
  splice incrémental à chaque édition PAR la voie de mutation unique —
  annulation et refaire compris ; requêtes de ligne (numéro, débuts, fins,
  contenu), seuils « gros document », mémo du texte complet à référence
  stable ; invariants croisés document/index vérifiés au fuzz (oracle
  `StringBuilder` étendu, défaire/refaire compris).
- Tranche 4 — recherche/remplacement porté dans `core-text` (registre de
  portage, tests du dépôt de départ comme oracle)  moteur `FindReplace` à
  horloge injectée (ADR-007)  casse, mot entier, regex sous garde-fou
  (budget temps échantillonné, plafond de collecte, états exposés),
  occurrences entières du mot sous le caret ; passe de remplacement par la
  voie de transaction — éditions par occurrence ajustées en deltas cumulés,
  UNE étape d'annulation jamais coalescée avec les frappes, plafond miroir
  de la collecte ; remplacement global par la session ; propriété jqwik à
  oracle `String.replace` ; correctifs consignés (casse insensible
  exotique — repli régional, indices sur le texte original ;
  correspondances de remplacement non recouvrantes).
- Benchmark d'édition de référence : 50 000 lignes, mille éditions d'un
  caractère par tour par la voie complète de la session, minimum de trois
  tours (PIEGE-13) — mesure locale : 0,055 ms par édition (budget 1 ms).
- Modèle de retour à la ligne souple pur à mesure injectée : coupe par
  caractères d'un mot plus large que la ligne par recherche dichotomique,
  accumulation gloutonne mot à mot (PIEGE-08).
- Harnais de benchmark au minimum de trois tours par phase, horloge
  injectée (PIEGE-13) — mesure locale undo/redo × 100 sur 2 000 lignes :
  0,22 ms / 0,25 ms.

### Corrigé

- Segmentation de mots : le regard des séparateurs médians (WB7, WB12)
  comparait le caractère directement précédent au lieu du précédent
  effectif — une marque combinante collée au séparateur coupait le mot
  que la norme garde entier (« a.◌̱b ») ; le précédent effectif porte
  désormais sa position de début et la classe avant le séparateur se
  lit au bon endroit (WB4).
- La porte PMD tournait à vide depuis M1 : le jeu de règles référençait des
  règles retirées de PMD 7.29.0 (aucun fichier analysé, tâche verte) —
  références valides, seuil de complexité réel (15 par méthode inchangé,
  plafonds 600/50 portés par Checkstyle, seuil de classe à la valeur par
  défaut de la règle).
- La mutation ciblait aussi les classes internes et utilitaires de test :
  cibles limitées au code de production ; le seuil suit le score au sens
  canonique de PIT (un mutant mort par boucle ou par mémoire est détecté) —
  seuil de 80 % inchangé, la tranche passe aussi en KILLED seuls.

- Validation M1 par le propriétaire (2026-11-04) : « M1 validé », sans
  désignation d'appareil de référence (question 5 ouverte) — M2, moteur de
  texte, est déclenché.
- Portage de la rope équilibrée dans `core-text` (feuilles, branches, fusion
  d'épines, rééquilibrage Fibonacci — registre de portage) avec ses tests
  portés et étendus : frontières exactes de découpage, identité des
  coupes pleines, contrats de message hors bornes, fenêtres de visite
  alignées sur les frontières de feuilles.
- Propriétés jqwik à oracle `StringBuilder` (environ 10 300 séquences
  aléatoires par exécution — objectif ADR-003 atteint pour la rope) ;
  le fuzz Jupiter passe à graines fixes reproductibles (exigence de la
  mutation) ; le harnais de démonstration M1 est retiré, remplacé
  par les propriétés réelles.

### Corrigé

- `apiCheck` relisait le nom du projet à l'exécution : la divergence
  de manifeste échouait en erreur de cache de configuration au lieu de la
  consigne de régénération — le nom est capturé à la configuration.
- Premier passage CI (ajustement prédit par le résumé M1) : le travail
  « modules purs » voit le SDK préinstallé du runner — le refus explicite
  `CELERIS_SKIP_ANDROID=1` prime sur la détection ; `sdkmanager`
  s'appelle par chemin complet (absent du PATH) ; la tolérance
  Javadoc consulte les source sets à la réalisation de la tâche (AGP 9 ne
  les pose pas à l'application du plugin — la consultation hâtive cassait
  la configuration des modules Android).
- Deuxième passage CI : la tolérance Javadoc relisait le conteneur
  dans la lambda de tâche (extensions DE LA TÂCHE, jamais trouvées — retour
  au relevé hâtif, avec findByName null-sûr au lieu de named().get()
  jetant) ; les trois modules Android n'appliquent plus la
  convention de publication — différée à M2/M3 (écart n° 5 du résumé M1) —
  AGP 9 ne posant ni « release » ni « java » dans le conteneur de
  composants.

- Validation M0 par le propriétaire (2026-11-04) : les douze ADR passent à
  l'état « Acceptée » ; les réponses aux questions groupées sont consignées
  dans le résumé M0 (question 5 — appareil de référence des budgets —
  ouverte).
- Fondations M1 : `build-logic` à plugins de convention, catalogue unique,
  squelette exécuté des dix modules et de la démonstration, cli, workflows
  CI épinglés par somme SHA avec validation du wrapper.
- Portes qualité bloquantes dès le premier commit : Checkstyle, PMD, Error
  Prone + NullAway (JSpecify), JaCoCo, ArchUnit, Jupiter/jqwik, mutation
  PIT au seuil de 80 % , scripts de politique (langue, neutralité).
- Manifeste d'API publique par module pur (`api/*.api`) vérifié dans
  `check` ; publication Maven locale complète avec SBOM et sommes SHA-256.
- `AGENT.md` (contrats d'agent) ; `NOTICE` consigne l'outillage et ses
  licences.

## [0.0.1] — 2026-11-04

### Ajouté

- Cadrage M0 : audit mesuré du dépôt de départ (`docs/audit/`), matrice de
  parité A–G (`docs/parite/`), ADR 001 à 012 en proposition, feuille de
  route chiffrée, glossaire.
- Spikes de mesure (rope re-mesurée, lexicographie incrémentale, rendu par
  proxy JVM) avec résultats consignés.
- Contrôles de politique de langue et de références externes, exécutables
  dès M0.

[0.0.1]: https://github.com/jjoblab/celeris/releases/tag/v0.0.1
