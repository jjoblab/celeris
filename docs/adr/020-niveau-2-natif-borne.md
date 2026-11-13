# ADR-020 — Niveau 2 natif : la révision de l'ADR-016 sur signal du propriétaire

- **Statut** : Accepté (2026-11-13 — le signal du propriétaire a ouvert le
  chantier : « tu dois lancer le chantier tree sitter » ; la voie différée de
  l'ADR-016 devient retenue, bornée aux conditions mesurées du spike)
- **Date** : 2026-11-13
- **Révise** : l'ADR-016 (2026-11-13, l'écart à M7.6) — ses conditions de
  révision y sont toutes notées ; la (a) reste ouverte : la mesure sur
  l'appareil de référence suivra le plan VERIFICATION-APPAREIL, la voie vit
  d'abord sur le profil JVM (bancs, cli) où elle se construit et se prouve.

## Contexte

L'ADR-016 a écarté le niveau 2 sur mesures : l'incrémental à l'échelle du
mandat (9,6 à 44,5 ms de p50 contre le budget de 10 ms), les arbres à 26-31×
le texte (budget 3×). Il laissait la voie « différée » : « un tree-sitter
pour fichiers modestes se défendrait » — re-parse incrémental mesuré à
0,5 ms à 190 Ko. Le propriétaire a donné le signal de l'ouvrir.

La voie diffère du socle par sa nature : les analyseurs écrits et les
grammaires lexicographient LIGNE À LIGNE (cascade d'états), l'arrière
natif analyse le document ENTIER en une passe. La liaison JNI vit dans
`core-syntax/natif/liaison.c` — écrite ici, fine, jamais celle de
l'organisation interdite (règle 7) ; le runtime (v0.27.2, MIT) et les
grammaires (json v0.24.8, python v0.25.0, javascript v0.25.0, MIT)
restent épinglés par `natif/construire`, jamais versionnés.

## Décision

**Le niveau 2 vit, BORNÉ aux fichiers modestes (≤ 200 Ko), comme voie
optionnelle du socle — la case déclarée de M7.6 se remplit.**

1. **La liaison** : `natif/construire` télécharge les sources épinglées
   puis gcc assemble `build/natif/libliaison.so`. La tâche est
   OPTATIVE (`./gradlew :core-syntax:natif`), jamais dans `check` — la
   chaîne de build du dépôt reste pure ; sans la bibliothèque, les
   fabriques rendent null et les paquets servent leur repli déclaré.
2. **Le moteur** (`MoteurNatif`) : l'arbre de la passe précédente sert
   le re-parse incrémental (l'édition s'y rapporte par points, une
   seule édition en attente — la deuxième abandonne l'arbre, la
   composition ne se devine jamais). La matérialisation Java
   (`MaterialisationNatif`) lit l'arbre entier APLATI : les feuilles y
   deviennent les jetons du lexique (soudées à la convention du socle,
   rognées à leur ligne), les nœuds de premier niveau qui traversent
   les lignes deviennent les chunks du pli. **Jamais de natif dans le
   chemin de rendu** — le jalon de l'ADR-004 tient.
3. **La borne mémoire** (condition c de l'ADR-016, révisée) : au-delà
   de 200 Ko, l'arbre natif ne SURVIT PAS à la passe — la re-analyse
   s'y paie au plein. Sous la borne, l'arbre survit entre deux éditions
   (le régime mesuré du spike : 0,5 ms incrémental à 190 Ko, ~6 Mo
   résidents pour un fichier à la borne — le budget PROPRE à la voie,
   documenté ici).
4. **L'enregistrement** : la démonstration déclare les sources natives
   dans la case du niveau 2 (`TreeSitterLanguages`) ; les paquets json,
   python et javascript portent `preferred: tree-sitter` avec leur
   repli d'origine. La bibliothèque absente ne change RIEN : le repli
   sert, aucun consommateur ne sait qui a répondu.
5. **Le pont** : `EditorSyntax` branche le moteur natif quand la source
   du langage est native — le lexeur et le parseur ligne à ligne ne
   s'y servent jamais ; la publication suit la voie du socle
   (synchrone ou exécutée, ADR-007), la fenêtre visible ne la
   concerne pas (la voie sert l'entier).

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| Niveau 2 pour tout langage, sans borne | écartée | les mesures de l'ADR-016 (incrémental 9,6-44,5 ms de p50, arbres 26-31×) |
| Niveau 2 BORNÉ aux fichiers modestes (≤ 200 Ko) | **retenue** | 0,5 ms incrémental à 190 Ko (spike) ; l'arbre ne survit pas au-delà — la mémoire reste la discipline de la voie |
| Niveaux 1 et 3 seuls (ADR-016) | révisée | le signal du propriétaire ouvre la voie différée ; la structure du socle ne change pas |

## Compromis

- La précision du niveau 2 (jetons exacts, plis exacts) ne sert que
  trois langages pour l'instant — chaque grammaire nouvelle est un
  épinglage de version et un banc (le patron de C5).
- L'assembleur natif (gcc) reste hors du chemin standard : la CI des
  contributeurs sans compilateur ne le rencontre jamais ; le banc du
  niveau 2 se saute d'un `assumeTrue` sans la bibliothèque.
- La mesure sur l'appareil de référence (condition a de l'ADR-016)
  reste due : la voie se prouve d'abord sur le profil JVM, le plan
  VERIFICATION-APPAREIL dira la suite.

## Conséquences

- `core-syntax/natif/` (liaison C + script de construction) entre au
  dépôt ; `core-syntax/natif/sources/` (runtime et grammaires
  téléchargés) ne s'y versionne jamais (le patron des spikes).
- Le manifeste d'API de `core-syntax` grandit de la voie publique
  (`MoteurNatif`, `SyntaxeNatif`, `TreeDiagnostics.lignesEnErreur`) —
  additions pures, aucune rupture (ADR-009).
- Les budgets de l'ADR-004 restent le seuil des niveaux 1 et 3 ; la
  voie native porte ses propres budgets, bornés, documentés ici.

## Actions

- [x] La liaison, le moteur, la matérialisation et le banc
      (`NatifBancTest` : jetons exacts, pli exact, incrémental, saut
      sans bibliothèque).
- [x] L'enregistrement de la démonstration et la préférence des
      paquets (json, python, javascript) avec leur repli.
- [ ] La mesure sur l'appareil de référence (plan VERIFICATION-APPAREIL)
      — la condition (a) de l'ADR-016 reste ouverte.
- [ ] Les grammaires suivantes, une par une, sur le même patron
      (épinglage, banc, enregistrement).
