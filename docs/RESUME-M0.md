# Résumé M0 — cadrage de celeris

> **Validé par le propriétaire le 2026-11-04** (« c'est oui pour tes
> recommandations ») : M1 — Fondations est déclenché. Les réponses aux
> questions groupées sont consignées ci-dessous ; les douze ADR passent à
> l'état « Acceptée ».

## Validation du propriétaire — 2026-11-04

M0 est validé et M1 — Fondations démarre. Les questions groupées reçoivent
les réponses recommandées (mandat : sans réponse explicite, la
recommandation s'applique et est signalée en tête du compte rendu M1) :

1. **DSL de build** — DSL Kotlin confiné à `build-logic` et aux fichiers de
   build ; aucun Kotlin dans les artefacts publiés.
2. **Cible de publication** — registre de l'hébergeur du dépôt, privé en
   pré-1.0 ; le registre central public reste réexaminé après validation du
   domaine de l'espace de noms.
3. **Analyse syntaxique** — option A (analyseurs écrits ici) ; l'option B
   reste chiffrée en réserve.
4. **Appareils de test** — appareil de référence local à M1 ; décision
   d'infrastructure à revoir à M3.
5. **Appareil de référence des budgets** — **non désigné** : point ouvert,
   signalé en tête du compte rendu M1 (ADR-006).
6. **`minSdk` 26** — confirmé.

## Livré

- **Audit mesuré** du dépôt de départ (`docs/audit/ETAT-DES-LIEUX.md`),
  généré par script sur le commit `f310a39` : les dix constats du mandat
  sont **confirmés** ; chiffres notables — 709 fichiers Java, 3 130 `@Test`
  mesurés (le mandat annonçait 2 873), 48 entrées réelles de baseline lint
  (le « 53 » comptait les éléments racines), 310 `@RestrictTo`,
  `EditorView` à 4 579 lignes dont 494 membres publics, accessibilité
  absente de la vue, 115 mentions de projets externes dans la documentation
  malgré la règle du dépôt de départ.
- **Pièges hérités** : 18 pièges convertis en tests nommés
  (`docs/audit/PIEGES-HERITES.md`), à écrire avant le code concerné.
- **Spikes chiffrés** (`docs/audit/SPIKES.md`) : rope re-mesurée à
  **0,002 ms/édition** (p50) sur 2 M de caractères, mémoire 1,22× le texte —
  portage validé avec un facteur ~500 de marge ; lexicographie incrémentale
  **1,6 µs** (p50) contre 8,05 ms pour le parcours complet — la voie des
  analyseurs écrits ici est crédible ; rendu (proxy JVM) : rapport
  création/consultation de mise en page ~10⁵ — le cache par rangée est une
  condition d'existence.
- **Matrice de parité A–G** (`docs/parite/MATRICE-PARITE.md`) : 116
  capacités, priorités P0/P1/P2, module cible, jalon et critère
  d'acceptation testable. Étalon vérifié en ligne le 2026-11-04 : la ligne
  stable d'Android Studio est **2026.2.2** — le mandat citait la ligne
  2025.3, l'étalon a avancé de deux lignes.
- **12 ADR en proposition** (`docs/adr/`) et **feuille de route chiffrée**
  (`docs/feuille-de-route.md`) : 220 à 315 j-h au total, chemin critique
  M2→M4.
- **Outillage dès M0** : contrôles de politique de langue et de références
  externes, exécutés et verts sur ce dépôt ; audit régénérable par script.

## Écarts et limites

1. Les budgets de rendu restent des **proxy JVM** : calibration sur
   l'appareil de référence exigée à M1 (l'appareil n'est pas désigné à ce
   jour — voir question 5).
2. Les mesures « sur appareil » (IME, gestes) n'ont pas pu être rejouées
   ici : aucun SDK Android dans l'environnement de cadrage ; elles
   démarrent à M1 avec l'instrumentation.
3. Le chiffre « 0,11 ms/édition » du mandat correspond visiblement au
   pipeline de session complet ; la rope seule mesure 0,002 ms. La chaîne
   complète sera re-mesurée à M2.

## Décisions proposées (détail dans les ADR)

Java seul, niveau 17, build DSL Kotlin confiné au build ; topologie à dix
modules purs forcée par ArchUnit ; rope portée ; analyseurs incrémentaux
écrits ici pour les langages P0, grammaires pour la longue traîne ; client
LSP propre sans dépendance tierce ; écrivain unique et instantanés ;
accessibilité de première classe ; API interne par défaut avec validateur
de compatibilité ; publication vers le registre de l'hébergeur en pré-1.0.

## Questions groupées pour le propriétaire

1. **DSL de build** — Kotlin pour `build-logic` seul (recommandé : outillage
   robuste, zéro Kotlin dans les artefacts) ou Groovy partout (zéro Kotlin
   dans le dépôt, outillage plus fragile) ?
2. **Cible de publication** — registre de l'hébergeur en pré-1.0 puis
   registre central public après validation du domaine (recommandé), ou
   registre central public dès M1 (exige la preuve de propriété du domaine
   de l'espace de noms) ?
3. **Analyse syntaxique** — validez-vous l'option A (analyseurs écrits ici,
   recommandée) avec l'option B chiffrée en réserve, ou exigez-vous le
   prototype de l'option B avant tout engage de M4 ?
4. **Appareils de test** — service d'appareils gérés en CI (coût
   d'infrastructure, couverture large) ou appareil de référence local
   (recommandé à M1, décision d'infrastructure à revoir à M3) ?
5. **Appareil de référence des budgets** — quel appareil milieu de gamme
   désignez-vous pour calibrer les budgets de rendu (ADR-006) ?
6. **`minSdk` 26** — confirmé par les parts d'appareils mesurées (≈ 99 % de
   portée cumulée) ; un impératif en dessous (24, comme le dépôt de départ)
   change le plan Unicode et mémoire : à confirmer.

## Passage de porte

Acquis : la validation est intervenue le 2026-11-04 (section ci-dessus) ;
M1 — Fondations est engagé et son compte rendu signale les recommandations
appliquées.
