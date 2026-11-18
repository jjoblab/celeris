# ADR-0024 — Deux voies de coloration séparées et choisissables

> **Statut : acceptée** (décision du propriétaire, F.4, 2026-11-18).
> Elle révisionne l'ADR-020 (voie native bornée au module publié) sur
> un point : la voie ne vit plus DANS le module publié.

## Contexte

Le dépôt porte deux moteurs de coloration :

- la voie **grammaires** (TextMate, `core-syntax-grammars`) : pure
  JVM, embarquée, elle sert par défaut ;
- la voie **native** (tree-sitter, ADR-016 révisée par l'ADR-020) :
  liaison JNI + runtime C, aujourd'hui **dans `core-syntax`** — le
  module publié porte des classes qui, sans bibliothèque partagée,
  ne font rien (`SyntaxeNatif.json()` rend toujours null), et le pont
  de la vue les importe directement (`MoteurNatif.de` dans
  `EditorSyntax.rattacher`).

Le propriétaire décide le 2026-11-18 (F.4) : **séparer les deux voies
en modules distincts, chacune réellement ce qu'elle prétend être, et
laisser l'utilisateur choisir l'une, l'autre, ou les deux**. Le
modèle est celui des éditeurs qui exposent le choix « moteur
grammaires » contre « moteur tree-sitter » : deux artefacts, une
préférence.

Par ailleurs (F.7) : la démonstration et `serveur-lsp-java` quittent
le dépôt — une démonstration repensée, sur une base plus solide,
remplacera l'actuelle dans un module neuf. Les retraits 1.x qui
étaient gelés derrière l'intouchabilité de la démo se débloquent.

## Décision

1. **`core-syntax-treesitter`** — nouveau module JVM opt-in : la
   liaison (`LiaisonArbre`), les sources (`SyntaxeNatif`), le moteur
   (`MoteurNatif`), l'état borné (`EtatNatif`,
   `MaterialisationNatif`), le code C (`natif/liaison.c`,
   `natif/construire`) et la tâche de build `natif`. Rien de tout
   cela ne vit plus dans `core-syntax` ni dans aucun artefact que
   l'hôte reçoit sans l'avoir demandé. La bibliothèque partagée ne se
   charge toujours QUE par la propriété `celeris.natif.lib`
   (ADR-0023 session, C4) — jamais empaquetée, jamais sondée.
2. **L'interface d'inversion `MoteurSyntaxe`** (dans `core-syntax`) :
   `analysePubliee()`, `analyseDeRevision(revision)`,
   `reanalyser()`, `fenetre(…)`, `ajouterAuditeur(…)`, `close()`.
   `MoteurAnalyse` et `MoteurNatif` l'implémentent. Le pont
   `EditorSyntax` ne connaît plus QUE cette interface : **la vue ne
   dépend d'aucune voie** — le champ `moteurNatif` disparaît du pont,
   un seul champ `MoteurSyntaxe` sert les deux.
3. **Le point de branchement de la voie native** : le registre
   `TreeSitterLanguages` (core-language, vide depuis M7.6) est le
   point d'inscription. Le module séparé y enregistre ses sources
   (`TreeSitterLanguages.enregistrer`) quand l'hôte l'embarque ; la
   plateforme résout la préférence `tree-sitter` du paquet comme
   aujourd'hui (préférence, puis repli) — **la préférence du paquet
   choisit la voie**. Sans le module : la case reste vide, le repli
   déclaré sert, rien ne se charge, rien ne change pour personne.
4. **La vue reste agnostique** : `android-view` ne déclare pas la
   dépendance vers le nouveau module ; il reçoit l'interface et la
   source que la plateforme a choisies. Le couplage au natif est
   inversé — c'est la voie qui se déclare, jamais la vue qui sonde.
5. **F.7** : `demo` et `serveur-lsp-java` sont supprimés (settings,
   CI, manifeste d'API, baseline de langue, mentions documentaires).
   Les retraits 1.x gelés derrière la démo deviennent possibles :
   `PopupsEditeur.surTheme`/`surTailleTexte` (l'abonnement automatique
   aux réglages les remplace), `ReglagesEditeur.setSurBascule`
   (`EditorSettings.addListener` sert), et les poseurs
   `setTheme`/`setFont`/`setSoftWrap`/`setLigatures` d'`EditorView`
   (la source unique `settings()` sert). MIGRATION.md § 0.3 se
   réécrit en conséquence.
6. **Choix explicite dans le kit** : la façade (`CelerisEditor`) et
   `LanguageKit` ne changent pas de surface — la préférence du paquet
   (déclarée dans `pack.json`) reste LE sélecteur de voie ; l'hôte
   qui veut la voie tree-sitter embarque le module et pose sa
   préférence, l'hôte qui ne veut pas n'embarque rien.

## Conséquences

- L'artefact publié par défaut ne contient **aucun chemin de
  chargement natif** : la surface trompeuse disparaît (fin du
  `SyntaxeNatif` importable-mais-inerte dans `core-syntax`).
- Le BOM et l'agrégateur n'exigent PAS le nouveau module : il
  s'ajoute à la main (`com.github.jjoblab.celeris:
  core-syntax-treesitter:<tag>`) — opt-in explicite.
- Les bancs du niveau 2 migrent avec lui (banc JVM, `assumeTrue`
  sans bibliothèque) ; `core-syntax` garde ses bancs purs.
- ADR-0020 reste lue ainsi : la voie bornée (200 Ko, arbre éphémère)
  VIT, mais dans son module ; ce qui change est le toit, pas la
  discipline.
- La suppression de la démo emporte son APK CI, la baseline langue
  de ses 81 identifiants, et la responsabilité d'un exemple complet
  revient au futur module de démonstration repensé (hors mission
  présente) ; `samples/consumer-smoke` reste le consommateur de
  référence tant qu'il vit.
