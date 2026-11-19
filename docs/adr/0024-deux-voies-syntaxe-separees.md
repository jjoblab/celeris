# ADR-0024 — Deux voies de coloration séparées et choisissables

> **Statut : acceptée** (décision du propriétaire, F.4, 2026-11-18).
> Elle révisionne l'ADR-020 (voie native bornée au module publié) sur
> un point : la voie ne vit plus DANS le module publié.

## Contexte

Le dépôt porte deux moteurs de coloration :

- la voie **grammaires** (TextMate, `core-syntax-grammars`) : pure
  JVM, embarquée, elle sert par défaut ;
- la voie **native** (tree-sitter, ADR-016 révisée par l'ADR-020) :
  liaison JNI + runtime C, encore **dans `core-syntax`** — le module
  publié porte des classes qui, sans bibliothèque partagée, ne font
  rien (`SyntaxeNatif.json()` rend toujours null), et le pont de la
  vue les importe directement (`MoteurNatif.de` dans
  `EditorSyntax.rattacher`).

Le propriétaire décide le 2026-11-18 (F.4) : **séparer les deux voies
en modules distincts, chacune réellement ce qu'elle prétend être, et
laisser l'utilisateur choisir l'une, l'autre, ou les deux**. Le
modèle est celui des éditeurs qui exposent le choix « moteur
grammaires » contre « moteur tree-sitter » : deux artefacts, une
préférence.

Le périmètre du dépôt se resserre en même temps (F.7) : la
bibliothèque ne publie QUE les modules de l'éditeur — pas
d'application d'exemple, pas de serveur de langage de banc.
Un futur module d'exemple, neuf et plus solide, pourra naître
sur cette base ; il vivra hors des artefacts publiés (jamais sur
JitPack, directive du propriétaire du 2026-11-18).

## Décision

1. **`core-syntax-treesitter`** — nouveau module JVM opt-in : la
   liaison (`LiaisonArbre`), les sources (`SyntaxeNatif`), le moteur
   (`MoteurNatif`), l'état borné (`EtatNatif`,
   `MaterialisationNatif`), le code C (`natif/liaison.c`,
   `natif/construire`) et la tâche de build `natif`. Rien de tout
   cela ne vit plus dans `core-syntax` ni dans aucun artefact que
   l'hôte reçoit sans l'avoir demandé. La bibliothèque partagée ne se
   charge toujours QUE par la propriété `celeris.natif.lib`
   (ADR-0023 session, C4) — jamais empaquetée, jamais sondée.
2. **L'interface d'inversion `MoteurSyntaxe`** (dans `core-syntax`) :
   `analysePubliee()`, `analyseDeRevision(revision)`,
   `reanalyser()`, `fenetre(…)`, `ajouterAuditeur(…)`, `close()`.
   `MoteurAnalyse` et `MoteurNatif` l'implémentent. Le pont
   `EditorSyntax` ne connaît plus QUE cette interface : **la vue ne
   dépend d'aucune voie** — un seul champ `MoteurSyntaxe` sert les
   deux.
3. **Le point de branchement de la voie native** : le registre
   `TreeSitterLanguages` (core-language) est le point d'inscription.
   Le module séparé y enregistre ses sources
   (`TreeSitterLanguages.enregistrer`) quand l'hôte l'embarque ; la
   plateforme résout la préférence `tree-sitter` du paquet comme
   toujours (préférence, puis repli) — **la préférence du paquet
   choisit la voie**. Sans le module : la case reste vide, le repli
   déclaré sert, rien ne se charge, rien ne change pour personne.
   L'annonce se fait par `ServiceLoader` (`FabriqueMoteurVoie`) :
   c'est la voie qui se déclare, jamais la vue qui sonde.
4. **La vue reste agnostique** : `android-view` ne déclare pas la
   dépendance vers le nouveau module ; il reçoit l'interface et la
   source que la plateforme a choisies.
5. **Le périmètre publié se resserre** (F.7) : le dépôt ne porte que
   les modules de l'éditeur et l'exemple consommateur. Les symboles
   publics qui ne servaient qu'une application hôte interne partent :
   `PopupsEditeur.surTheme`/`surTailleTexte` deviennent privés
   (l'abonnement automatique aux réglages les remplace),
   `ReglagesEditeur.setSurBascule` est retiré
   (`EditorSettings.addListener` sert), et les poseurs
   `setTheme`/`setFont`/`setSoftWrap`/`setLigatures` d'`EditorView`
   sont dépréciés puis retirés à la 2.0.0 (la source unique
   `settings()` sert). MIGRATION.md § 0.3 se réécrit en conséquence.
6. **Choix explicite dans le kit** : la façade (`CelerisEditor`) et
   `LanguageKit` ne changent pas de surface — la préférence du paquet
   (déclarée dans `pack.json`) reste LE sélecteur de voie ; l'hôte
   qui veut la voie tree-sitter embarque le module et pose sa
   préférence, l'hôte qui ne veut pas n'embarque rien.

## Conséquences

- L'artefact publié par défaut ne contient **aucun chemin de
  chargement natif** : la surface trompeuse disparaît (fin du
  `SyntaxeNatif` importable-mais-inerte dans `core-syntax`).
- Le BOM et l'agrégateur n'exigent PAS le nouveau module : il
  s'ajoute à la main (`com.github.jjoblab.celeris:
  core-syntax-treesitter:<tag>`) — opt-in explicite.
- Les bancs du niveau 2 migrent avec lui (banc JVM, `assumeTrue`
  sans bibliothèque) ; `core-syntax` garde ses bancs purs.
- ADR-0020 reste lue ainsi : la voie bornée (200 Ko, arbre éphémère)
  VIT, mais dans son module ; ce qui change est le toit, pas la
  discipline.
- Le futur module d'exemple, s'il naît, reste HORS des
  artefacts publiés : jamais dans la BOM, jamais dans l'agrégateur,
  jamais sur JitPack (directive du propriétaire) ;
  `samples/consumer-smoke` reste le consommateur de référence.

## Le module d'exemple est né (2026-11-18, séance suivante)

Le futur module annoncé est arrivé : **`sample`** — l'application
d'exemple du dépôt, née VOLONTAIREMENT petite. Une activité unique
(`MainActivity`) pose la scène du § 4.1 d'USAGE en code (aucune
ressource de gabarit : tout ce que l'hôte écrit reste visible au
même endroit), monte le chargeur de langues partagé puis la façade
(`CelerisEditor.attach`), ouvre un document Java et referme tout à
la destruction de la scène. Sa seule dépendance est l'agrégateur
`celeris` — le LSP embarqué reste optionnel et l'exemple ne le
prend pas. La CI l'assemble (`:sample:assembleDebug`) ; il ne se
publie jamais (aucune convention de publication, hors BOM et
agrégateur, jamais sur JitPack). `samples/consumer-smoke` garde la
preuve mécanique (mavenLocal, R8, mapping vérifié) : `sample`
montre le code, `consumer-smoke` prouve l'octet.
