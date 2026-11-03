# ADR-010 — Stratégie de test

- **Statut** : Acceptée (validée par le propriétaire le 2026-11-04)
- **Date** : 2026-11-04

## Contexte

Le dépôt de départ compte 3 130 annotations `@Test` mesurées (73 307 lignes
de test) — mais 187 fichiers Robolectric, **zéro** test sous
`src/androidTest/` (constat n° 7) : l'IME, les gestes et le rendu ne sont
jamais vérifiés sur appareil. Le mandat impose l'inverse cumulatif : tests
JVM rapides sans SDK, propriétés et fuzz, mutation, tests de vue sur
appareils gérés, benchmarks à seuils — et chaque piège hérité converti en
test nommé avant le code (`docs/audit/PIEGES-HERITES.md`).

## Décision proposée

1. **Pyramide inversée par la vitesse** : les modules purs (`core-*`)
   s'exécutent en quelques secondes sur JVM pure sans SDK Android
   (ADR-002) ; c'est la porte de qualité qui protège toute la suite.
2. **Propriétés et fuzz (jqwik)** sur le modèle de texte et l'analyse :
   séquences aléatoires d'éditions contre oracle naïf `StringBuilder`
   (généralisation du fuzz hérité, ADR-003) ; invariants croisés
   document ↔ index de lignes ↔ plis ↔ décalage des diagnostics ;
   corpus Unicode (mixte BiDi, CJK, emoji composés, combinants).
3. **Mutation (PIT)** sur `core-text` avec seuil de 80 % ; couverture de
   lignes ≥ 90 % sur les modules purs, ≥ 70 % sur les vues — jamais
   utilisées seules comme preuve (la mutation et les propriétés priment).
4. **Tests de vue en deux temps** : captures de référence (rendu déterministe
   avec mesure injectée) puis tests **instrumentés sur appareils gérés par
   Gradle** : IME à chaque niveau d'API supporté (26, 31, 33, 34, 36),
   gestes (poignées, loupe, pilule, pincement), clavier matériel,
   accessibilité (auditeur automatique). Un scénario de la démo = un test
   de fumée instrumenté (règle d'or du mandat).
5. **Benchmarks à seuils en CI** : budgets de l'ADR-006 en cliquet
   (regression = échec) ; protocole « minimum de trois tours » hérité
   (PIEGE-13) ; JMH sur JVM pour le modèle, macrobenchmarks sur appareil
   pour la frappe et le défilement.
6. **Traçabilité des pièges hérités** : chaque ligne de
   `PIEGES-HERITES.md` porte un identifiant `PIEGE-nn` cité par le test
   qui la couvre ; le tableau d'avancement est généré depuis le code
   (aucun chiffre saisi à la main).
7. **Aucune réflexion** dans les tests (PIEGE-05) : tout point d'injection
   est une interface (ADR-007) ; un test qui utilise la réflexion casse la
   revue, l'outillage la détecte.

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| A. Cumulatif : JVM + propriétés + mutation + instrumenté + benchmarks (proposition) | **retenue** | seul cumul qui couvre les échecs mesurés du dépôt de départ (appareil jamais testé, chiffres dérivants) |
| B. Doubler Robolectric | écartée | le constat n° 7 : 187 fichiers Robolectric n'ont jamais attrapé un bug d'IME |
| C. Tests manuels à la démo | écartée | invérifiable ; contredit la définition du « terminé » |

## Compromis

- Les appareils gérés en CI ont un coût d'infrastructure (service
  d'appareils éphémères) : décision d'infrastructure à confirmer par le
  propriétaire à M1 (voir questions groupées du résumé M0) ; à défaut, la
  batterie instrumentée tourne sur l'appareil de référence du propriétaire
  avant chaque fusion.
- La mutation ralentit la CI : exécution nocturne avec porte de fusion au
  matin, seuil bloquant.

## Conséquences

- Le nombre de tests, la couverture et le score de mutation sont **générés**
  dans un tableau d'avancement — mettant fin à la dérive documentaire
  mesurée (README 4.14.1, ARCHITECTURE « 2 610 tests », AGENT « 3 185
  tests » pour 3 130 mesurés).
- Un bug corrigé arrive toujours avec son test rouge-avant-vert (règle
  héritée, conservée).
- L'oracle naïf borne la complexité : tout comportement non exprimable en
  `StringBuilder` + invariants n'entre pas dans le fuzz.

## Actions

- [ ] M1 : harnais jqwik + PIT + couverture, seuils actifs dès le squelette.
- [x] M1 : décision appareils gérés contre appareil de référence local —
      décision du 2026-11-04 : appareil de référence local (question 4 du
      résumé M0).
- [ ] M2 : fuzz de l'éditeur complet (transactions, undo, index, plis).
