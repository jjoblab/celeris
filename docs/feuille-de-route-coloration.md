# Feuille de route — fiabilisation de la coloration syntaxique

> Document vivant : **mis à jour à chaque étape** (statut, date, commit).
> Origine : audit interne de la coloration (2026-11-19, session 11) — les
> constats et preuves vivent dans le rapport d'audit, non versionné.
> Décision du propriétaire (2026-11-19) : **option A — corriger
> l'approche actuelle**, sans refonte ni dépendance externe.

## Principes de la feuille

- Une étape = un commit = un banc qui échoue avant et passe après.
- L'ordre suit la gravité décroissante ; chaque étape est indépendante et
  réversible.
- Le statut se met à jour au moment du commit : `☐ à faire` →
  `🟡 en cours` → `✅ fait (hash court, date)`. Les blocages se notent
  `🔴 bloqué` avec la raison et la décision attendue.
- Le socle (voies maison et grammaires) est sain — l'audit l'a prouvé au
  banc (zéro divergence incrémental/complet sur 1 150 éditions
  aléatoires). Les correctifs visent la voie native et des points
  périphériques ; aucun ne touche les invariants gagnants (états par
  ligne, reconvergence, fraîcheur par révision, rattrapage).

## Vue d'ensemble

| Étape | Objet | Gravité | Statut |
| --- | --- | --- | --- |
| A1.1 | La voie native reprogramme l'analyse à l'édition | critique | ✅ fait (e13428b, 2026-11-19) |
| A1.2 | Le rattrapage de coloration sert la voie native | critique | ✅ fait (daa62fc, 2026-11-19) |
| A1.3 | L'échec de passe native notifie (plus d'écran figé) | moyenne | ✅ fait (e13428b, 2026-11-19) |
| A2 | La BOM n'est plus colorée comme un opérateur | mineure | ✅ fait (444a064, 2026-11-19) |
| A3 | Statut du `\r` CRLF décidé et documenté | mineure | ✅ fait (7ebe335, 2026-11-19 — ADR-0026, voie (b) retenue) |
| A4 | Consolidations documentaires (bornes, internement, sémantique LSP) | mineure | ✅ fait (ADR-0027 acceptée le 2026-11-20 — voir B1) |
| A5 | Bancs structurels : multi-onglets, frappe native, critère incrémental en CI | — | ✅ fait (3 bancs, 5 tests verts) |
| A6 | Étude : la voie des grammaires par moteur externe (textmate) | — | 🔴 reportée par le propriétaire (2026-11-20, « une autre fois ») |
| B1 | Surcouche sémantique LSP (ADR-0027 acceptée) | fonctionnalité | ✅ fait (2026-11-20, le jour de la validation) |

## Détail des étapes

### A1.1 — La voie native reprogramme l'analyse à l'édition

- **Constat d'origine** : `MoteurNatif.surEdition` enregistre les bornes
  de l'édition mais ne reprogramme AUCUNE passe — contrairement à la voie
  du socle (`MoteurAnalyse.surEdition`), qui poste une tâche à chaque
  édition. Après la première publication, toute frappe laisse la
  publication périmée pour toujours : l'éditeur reste décoloré. Le banc
  du module appelait `reanalyser()` à la main et masquait le défaut.
- **Correctif** : à l'édition, le moteur poste la passe sur son exécuteur
  (voie synchrone : la passe se paie au `reanalyser()` suivant — le pont
  de vue la déclenche déjà). La coalescence reste simple : une passe en
  vol n'est pas annulable côté natif, mais la passe lit TOUJOURS le
  document courant — une rafale de frappes produit des passes en série,
  chacune servant la révision la plus récente au moment où elle tourne.
- **Bancs** : (1) l'édition suivi d'`analyseDeRevision(révision courante)`
  rend une analyse non nulle (sous `assumeTrue` de la liaison) ; (2) la
  rafale de frappes finit publiée à la dernière révision. *(Posés :
  `ReprogrammationEditionNatifTest` — sautés sur JVM sans la liaison,
  ils servent la CI Android où elle vit.)*
- **Statut** : ✅ fait (e13428b, 2026-11-19)

### A1.2 — Le rattrapage de coloration sert la voie native

- **Constat d'origine** : `EditorSyntax.habillerPerimee` ne gère que la
  voie hors fil du socle : quand la voie native sert, l'habillage
  périmé rend la source TELLE QUELLE — aucune couleur entre la frappe et
  la publication, et pour toujours tant que A1.1 n'est pas là.
- **Correctif** : l'habillage périmé lit la publication de la voie qui
  sert (le contrat `MoteurSyntaxe` expose déjà `analysePubliee()`) et le
  rattrapeur `TokenCatchUp` s'y applique tel quel — il ne lit qu'un
  lexique document, jamais le moteur qui l'a produit.
- **Bancs** : banc de vue avec une fausse fabrique de voie (enregistrée
  par `ServiceLoader` de test, armée sélectivement pour ne servir que
  ce banc) : la frappe garde les couleurs publiées recalées jusqu'à la
  publication suivante. *(Posés : `RattrapageVoieNatifTest` — rouge sans
  correctif, vert avec ; les 21 bancs coloration existants restent
  verts.)*
- **Statut** : ✅ fait (daa62fc, 2026-11-19)

### A1.3 — L'échec de passe native notifie

- **Constat d'origine** : quand la passe native échoue (arbre nul), la
  publication devient nulle SANS notification : la vue reste sur
  « couleur attendue » à jamais, écran figé sans diagnostic.
- **Correctif** : l'échec de passe publie l'analyse précédente telle
  quelle (ou nothing de neuf) ET prévient les auditeurs — la vue quitte
  l'attente ; l'incident se journalise (`java.util.logging`, comme le
  lexeur des grammaires).
- **Bancs** : un auditeur posé sur un moteur dont la passe échoue
  reçoit la notification. *(La notification part maintenant à CHAQUE
  passe — échec compris : la maquette du banc A1.2 la vérifie côté vue,
  le banc natif la couvrira sur CI.)*
- **Statut** : ✅ fait (e13428b, 2026-11-19)

### A2 — La BOM n'est plus colorée comme un opérateur

- **Constat d'origine** : la marque d'ordre U+FEFF en tête de fichier
  UTF-8 est lexicographiée comme un opérateur par le lexeur de la
  famille C — elle reçoit l'encre des opérateurs (preuve au banc de
  l'audit : `L0 OPERATOR[0..1] KEYWORD[1..4]…`).
- **Correctif** : le caractère U+FEFF en tête de PREMIÈRE ligne est
  ignoré (jeton vide), la voie des grammaires pareil si elle le colore.
- **Bancs** : fichier avec BOM → premier jeton = KEYWORD à l'offset 1,
  aucun jeton opérateur parasite. *(Posés : `BomEnteteTest` — rouge 2
  échecs, vert 3/3 ; la BOM au MILIEU reste un jeton, frontière figée.)*
- **Statut** : ✅ fait (444a064, 2026-11-19)

### A3 — Statut du `\r` CRLF décidé et documenté

- **Constat d'origine** : l'index de lignes ne compte que `\n` — le `\r`
  d'une fin CRLF reste dans la ligne rendue. Aucun effet sur les jetons
  (le lexeur le tient pour un blanc) mais les couches qui comptent des
  colonnes voient un caractère invisible de plus.
- **Décision à prendre** : (a) retirer le `\r` de la ligne rendue —
  cohérent avec « la ligne ne contient jamais le saut de ligne final »,
  ou (b) l'assumer et le documenter. L'audit recommande (a) : la
  convention du socle est déjà « sans le saut de ligne final », le
  `\r` en fait partie.
- **Bancs** : cohérence colonnes/offsets sur un document CRLF. *(La
  voie (b) est retenue — ADR-0026 ; banc `FinsCrlfTest` : convention du
  modèle, lexicographie exacte, édition avant fin CRLF : incrémental ==
  complet.)*
- **Statut** : ✅ fait (7ebe335, 2026-11-19)

### A4 — Consolidations documentaires

- La borne de 200 Ko de la voie native entre dans ARCHITECTURE.md (elle
  vit dans le code, pas dans la doc).
- Le garde-fou mémoire de l'internement des portées (table globale
  bornée à 50 000 nœuds, jamais vidée) se documente comme tel dans sa
  Javadoc.
- Statut de la coloration sémantique LSP (types présents, rendu non
  câblé) : note d'orientation — câbler en surcouche ou retirer de
  l'API. **Décision propriétaire.** *(Tranché par l'ADR-0027 —
  proposition : câbler EN SURCOUCHE, phase dédiée après validation
  terrain de A1-A3 ; le retrait n'est pas une option, le protocolaire
  est complet et testé.)*
- **Statut** : ✅ fait (borne dans ARCHITECTURE.md §2.2, garde-fou
  documenté dans ScopeStacks, ADR-0027 posée et indexée — reste la
  validation propriétaire de l'ADR)

### A5 — Bancs structurels (non-régression durable)

- Multi-onglets : 3 sessions, 3 langages, 10 bascules — zéro
  re-tokenisation par retour (compteur de passes).
- Frappe dans un document de la voie native : la publication suit chaque
  révision (verrouille A1.1/A1.2).
- Le critère « zéro divergence incrémental/complet sur N éditions
  aléatoires » (vrai à N=1 150 dans l'audit) se fige en banc CI.
  *(Fait : `DivergenceNulleBancTest` — 1 150 éditions à graine fixe
  20261119, gestes réalistes y compris états ouverts ; VÉRIFIÉ VERT :
  zéro divergence, jetons/états/profondeurs comparés à chaque étape.)*
- **Statut** : ✅ fait (MultiOngletsBancTest 2/2 — trois onglets
  Java/Kotlin/Groovy, dix bascules sans re-tokenisation, frappe active
  isolée ; PublicationChaqueRevisionBancTest 2/2 — maquette
  reprogrammante, la rafale publie à chaque révision, la couleur ne
  périmé plus d'une frappe ; DivergenceNulleBancTest 1/1)

### A6 — Étude : la voie des grammaires par moteur externe (textmate)

- **Question posée par le propriétaire** (2026-11-19) : utiliser un
  moteur de grammaires EXTERNE pour la voie des grammaires, comme le
  fait l'éditeur de référence sur Android.
- **État des lieux** : la voie maison (ADR-004 révisé, ADR-014) lit
  les grammaires TextMate RÉELLES (celles que publient les extensions
  de VS Code — format JSON/PList, captures, inclusions, `$self`),
  embarque 27 grammaires (1,8 Mo), et exécute les expressions par
  **joni** (portage Java d'Oniguruma — le MÊME moteur que la
  référence). L'audit du 2026-11-19 a validé sa conformité au banc.
- **Ce qu'un moteur externe apporterait** : un moteur natif (C via
  JNI, comme la référence) exécute les expressions plus vite et
  déporte la maintenance. Ce qu'il COÛTE : une dépendance JNI de plus
  (la voie tree-sitter en a déjà une), la perte du contrôle
  incrémental (les moteurs externes pensent « ligne par ligne », le
  socle pense « reconvergence par états »), et le doublement des
  chemins de repli si le moteur manque sur une architecture.
- **Statut** : 🔴 REPORTÉE par le propriétaire le 2026-11-20
  (« A6 une autre fois ») — les données restent posées, la
  décision reviendra à lui.

### B1 — Surcouche sémantique LSP (ADR-0027 acceptée)

- **Décision du propriétaire** (2026-11-20) : « Je valide
  l'adr 027 » — câbler en surcouche, exécuté le jour même.
- **Ce qui vit où** : `SemanticOverlay` (pure, android-view) porte
  les jetons transposés en offsets absolus, la légende et la révision
  servie ; `SemanticRowSource` enveloppe la source habillée ;
  `EditorSyntax.poseSemantique` compose à l'habillage (le même endroit
  que le rattrapage D-05) et RECALE à la frame (PIEGE-20 : une pose
  périmée se vide, jamais elle ne colore une révision plus récente) ;
  `EditorView.setSemanticTokens` est la porte publique ; côté hôte,
  `EditorPopups` demande au calme (300 ms après la synchronisation,
  une demande par rafale de frappe) et transpose par `LspPositions`
  (frontière ADR-005) ; `LanguageSession.tokenTypes()` (défaut vide)
  porte la légende convenue.
- **Les règles tenues** : le jeton RAFFINE — il ne colore que
  l'intersection avec la preuve syntaxique dessous, jamais un
  remplacement ; le jeton orphelin s'ignore ; le recouvrement
  de jetons se rogne (le premier l'emporte) ; le type inconnu de la
  légende reste muet ; l'effacement (débranchement du serveur)
  rend la coloration syntaxique seule.
- **Bancs** : `SemanticOverlayTest` 7/7 (fusion pure) et
  `ColorationSemantiqueVueTest` 3/3 (bout en bout sur le harnais :
  raffinement servi, périmé ignoré, effacement) — le banc
  d'intégration a attrapé une pose périmée qui colorait quand même :
  corrigée au recalage à l'habillage.

## Journal de suivi

| Date | Étape | Commit | Notes |
| --- | --- | --- | --- |
| 2026-11-19 | création | — | feuille posée après décision du propriétaire (option A) |
| 2026-11-20 | B1 | (ce commit) | ADR-0027 validée par le propriétaire puis exécutée le jour même : surcouche, fusion à l'habillage, fraîcheur PIEGE-20 ; SemanticOverlayTest 7/7, ColorationSemantiqueVueTest 3/3 (le banc a attrapé une pose périmée qui colorait — corrigée au recalage) |
| 2026-11-19 | A1.1 + A1.3 | e13428b | la voie native reprogramme à l'édition (coalescence, garde fermeture) ; l'échec de passe journalise et prévient — les bancs natifs sautent sur JVM (liaison Android), ils servent la CI |
| 2026-11-19 | A1.2 | daa62fc | le rattrapage lit la publication de la voie qui sert ; prouvé rouge→vert par fabrique de banc armée sélectivement (3/3 verts, 21 bancs coloration sans régression) |
| 2026-11-19 | A2 | 444a064 | la BOM de tête est un blanc de format (garde en position 0 de la ligne de tête) ; banc rouge 2 échecs → vert 3/3 |
| 2026-11-19 | A3 | 7ebe335 | ADR-0026 : le \r reste dans la ligne rendue (voie (b)) ; Javadoc du modèle + banc CRLF (convention, lexicographie, incrémental == complet) |
| 2026-11-19 | A4 | — | borne 200 Ko dans ARCHITECTURE.md §2.2 ; garde-fou ScopeStacks documenté (« garde-fou, pas cache ») ; ADR-0027 (proposition) : sémantique LSP en surcouche, phase dédiée |
| 2026-11-19 | A5 | — | MultiOngletsBancTest 2/2, PublicationChaqueRevisionBancTest 2/2, DivergenceNulleBancTest 1/1 (1 150 éditions, graine 20261119, zéro divergence) |
| 2026-11-19 | A6 | — | étude posée : moteur de grammaires externe — décision propriétaire |
