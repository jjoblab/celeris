# ADR-003 — Modèle de texte et transactions

- **Statut** : Proposition (M0)
- **Date** : 2026-11-04

## Contexte

Le modèle de texte est le socle de tout l'éditeur. L'audit du dépôt de
départ valide la structure persistante choisie (rope équilibrée par poids,
fusion d'épines) et sa voie de mutation unique (`doReplaceRange`) ; le spike
de re-mesure (`docs/audit/SPIKES.md`) confirme sur JVM pure :

| Mesure (50 000 lignes, 2 M caractères) | Valeur |
| --- | --- |
| Édition d'un caractère, p50 / p95 | 0,0020 ms / 0,0046 ms |
| Minimum de trois tours | 0,0019 ms par édition |
| Mémoire du modèle | ≈ 1,22 × la taille du texte |
| Budget du mandat | 1 ms par édition |

`Rope.java` (395 lignes) n'importe que `java.util` : le portage en JVM pure
ne demande aucune retouche. En revanche, le modèle hérité est mono-curseur
(`Selection` : deux `int`), sans types forts (confusion documentée entre
lignes du document, rangées visuelles et pixels) et sans transposition LSP
explicite.

## Décision proposée

1. **Porter la rope** du dépôt de départ après revue fichier par fichier
   (`docs/audit/REGISTRE-PORTAGE.md`), avec ses tests et son fuzz à oracle.
   Une structure d'arbre de pièces neuve n'apporterait rien de mesurable
   contre un coût de réécriture réel.
2. **Instantanés immuables** : chaque état du document est un instantané
   complet, lisible sans verrou depuis tout fil ; l'annulation n'est qu'une
   collection d'instantanés.
3. **La `Transaction` est la seule voie de mutation** : ensemble immuable,
   composable, inversible de changements, avec projection des positions
   (décalage des diagnostics, plis, sélections, composition IME).
   L'annulation et les éditions distantes (agents, hôtes) passent par là —
   jamais par une mutation directe (PIEGE-07).
4. **Multi-sélection de première classe** : une sélection est une liste de
   plages ordonnées, disjointes après normalisation ; les transactions
   s'appliquent à toutes les plages d'un coup, et l'annulation restaure les
   sélections (exigence de parité A-13).
5. **Types forts** dès `core-text` : `DocOffset` (UTF-16), `LineIndex`,
   `VisualRow` (défini dans `android-view` mais projeté depuis le cœur),
   `Pixel` (vue). Aucun `int` de position nu ne traverse une API publique.
6. **UTF-16 en interne**, transposition explicite vers les positions LSP
   (UTF-16 aussi, mais bornes différentes) ; groupes de graphèmes (UAX #29)
   et algorithmme bidirectionnel (UAX #9) implantés dans le cœur, testés sur
   textes CJK, emoji et combinants dès M2.
7. Le fuzz à oracle `StringBuilder` du dépôt de départ est **généralisé**
   (jqwik, ADR-010) : invariants entre document, index de lignes, plis et
   décalage des diagnostics.

## Options

| Option | Verdict | Raison mesurée |
| --- | --- | --- |
| A. Porter la rope existante | **retenue** | 0,002 ms/édition mesuré ; mémoire 1,22× ; zéro retouche JVM ; tests et fuzz réutilisables |
| B. Arbre de pièces neuf | écartée | mêmes propriétés asymptotiques ; coût de réécriture et de revalidation sans bénéfice mesuré |
| C. Tampon à trou + copies | écartée | édition économique mais instantanés immuables coûteux (copie entière) ; inadapté à la lecture multi-fils |

## Compromis

- La rope héritée est concise mais sans indexation par feuille externalisée :
  l'index de lignes vivra à côté (comme dans le dépôt de départ) et sera
  maintenu par la transaction — l'invariant est vérifié par le fuzz.
- L'immuabilité généralisée consomme plus de mémoire qu'un tampon mutable :
  mesuré à 1,22× ici, budgété à 3× modèle + index dans l'ADR-006.

## Conséquences

- `core-text` n'a aucune dépendance : il se teste en quelques secondes sur
  JVM pure (critère de sortie M2).
- La confusion ligne/rangée/pixel devient une erreur de **compilation**,
  pas un bug de production (PIEGE-04).
- Toute fonctionnalité d'édition (recherche, gabarits, refactorings)
  s'exprime comme producteur de transactions : un seul chemin à tester.

## Actions

- [ ] M2 : porter rope + index de lignes + annulation avec coalescence
      (registre de portage, colonne « tests associés » obligatoire).
- [ ] M2 : écrire les pièges PIEGE-07 et PIEGE-08 **avant** le code.
- [ ] M2 : propriétés jqwik : 10 000 séquences aléatoires par exécution CI.
