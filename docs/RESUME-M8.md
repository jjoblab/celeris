# Résumé M8 — IA native (clos le 2026-11-07, tranches 1 à 6)

> **Déclenchement** : clôture du jalon M7 (2026-11-07, critères de
> sortie couverts — catégories C et E P0, SPI de référence, cli en CI).
> Périmètre (feuille de route) : contrats `core-ai` ; texte fantôme ;
> prédiction d'édition ; revue de modifications d'agent par blocs avec
> points de contrôle ; API d'outils compatible MCP avec détection de
> conflit ; exclusions de contexte ; faux modèles déterministes. Sortie
> visée : catégorie F P0 couverte (F-01, F-04, F-05, F-09, F-10) ;
> budgets de latence tenus ; test « aucune requête sans consentement »
> vert.
>
> **État** : jalon clos le 2026-11-07 — les six tranches sont
> consignées ci-dessous, critères de sortie couverts (catégorie F P0,
> budgets de latence, test « aucune requête sans consentement »
> vert). La catégorie F de la matrice est livrée entière.

## Avant la première tranche — l'hygiène inter-jalons (2026-11-07)

Le registre des pièges rattrape les deux pièges du rapport d'appareil
de M7 : la police qui trace devient **PIEGE-22** (couvert —
`lesPlagesSeTracentALaPoliceDeLEditeur`, `TextPainterSyntaxTest`), les
sp convertis à la densité deviennent **PIEGE-23** (couvert depuis le
2026-11-07 — `taillesSpConvertiesALaDensite`, `TaillesSpTest` : la
conversion quitte la vue pour `TaillesSp`, pure, échelle de police de
l'accessibilité comprise). Les citations suivaient dans
android-view, le changelog et le résumé M7 ; l'« État » de ce dernier,
resté à « NON clos », dit désormais clos.

## Tranche 1 — les contrats IA : texte fantôme, prédiction, faux modèles (2026-11-07)

### Livré

- **F-01, noyau — `SessionTexteFantome`** : la machine à états pure du
  fantôme. La suggestion (`SuggestionFantome` : texte multiligne,
  ancre, révision) ne s'affiche que fraîche — la livraison porte la
  révision de son instantané, celle du document au départ du vol ;
  périmée, ancrée hors document ou portée par un jeton mort, elle se
  jette : jamais ré-ancrée, jamais fusionnée à la hâte (ADR-007).
  L'acceptation se prend par mot (le mot, ses espaces meneuses, celle
  qui le sépare du suivant), par ligne (jusqu'au saut compris) ou en
  bloc ; le reste survit ancré pareil — un geste de l'utilisateur sur
  une suggestion visible, pas une livraison de modèle. La frappe
  suivante refuse le fantôme et annule le vol ; le refus explicite
  (clavier, geste) ferme pareil.
- **F-02 — `ModelePrediction`, `PredictionEdition`,
  `AppliquerPrediction`** : le modèle prédit une opération (des
  éditions), pas un texte ; le cœur ne lui donne aucune session à
  toucher — l'application est le geste explicite de l'hôte, par la voie
  de transaction ordinaire : une étape d'annulation, diffable. Le refus
  est un résultat, jamais une exception : révision périmée (jamais de
  fusion silencieuse — la discipline de l'API d'outils appliquée à la
  prédiction), protection en écriture, éditions qui ne collent pas au
  document. Les éditions multiples vivent dans l'espace séquentiel —
  chacune contre le document que les précédentes ont laissé — et se
  prévalident par simulation sur les instantanés immuables : la session
  fait confiance à son appelant, une prédiction ne la mérite pas, et
  une exception en cours d'application y laisserait le document muté
  sans étape d'annulation.
- **F-08 — `FauxModeleLocal` et `FauxModeleDistant`** : les modèles de
  référence déterministes (ADR-012, option A). Le local livre
  synchronement, sur le fil de l'appelant ; le distant ne livre que par
  l'exécuteur injecté — qui joue le réseau et décide quand la réponse
  arrive, le cœur n'en crée aucun (ADR-007). Même contrat pour l'un et
  l'autre : l'avis servi, l'aveu sans avis, le jeton relu au moment de
  livrer (un vol mort en route ne livre rien). Chaque requête reçue
  entre au journal — le jour des exclusions de contexte (F-07), c'est
  lui qui prouvera qu'un chemin exclu ne part jamais.
- **F-09, noyau — `RequeteModele` et les jetons** : la requête porte
  l'instantané, le curseur et le jeton (ADR-007) ; la demande suivante
  supplante la précédente — son jeton meurt —, l'acceptation clôt le
  vol. L'attente ne bloque jamais la frappe : mille demandes vers un
  modèle aveugle (réseau qui ne répond jamais) rendent la main à
  chaque fois, mesure au minimum de trois tours (PIEGE-13) — le
  chemin est fait d'appels purs, le budget d'une seconde est une
  protection structurelle. Le budget de latence d'affichage sera
  consigné avec le rendu (F-01, tranche interface).
- **Le module** : `core-ai` dépend explicitement de `core-text`
  (l'api transitait par `core-syntax`) ; la mutation s'y étend
  (convention extensible à tout module pur, seuil conservé).

### Preuve

`:core-ai:check` vert — 43 tests (Checkstyle, PMD, Error Prone,
NullAway, couverture ≥ 90 % compris), mutation 86,4 % (76/88, seuil
80), `publishToMavenLocal` et `publicationChecksums` verts. Manifeste
d'API régénéré — onze types publics ajoutés, aucun retrait (SemVer
mineure, ADR-009). `./gradlew check` vert sur l'ensemble (SDK présent).
Les lignes F-01, F-02, F-08 et F-09 de la matrice passent à partiel
(noyau livré, interface et scénarios de démonstration à venir).

## Tranche 2 — la revue des modifications d'agent (2026-11-07)

### Livré

- **F-04, noyau — `EtiquetteAgent`, `PropositionAgent`** : les éditions
  d'agent arrivent comme propositions étiquetées (auteur, intention) et
  datées de l'instantané de référence. Les éditions vivent en
  coordonnées du document de référence — la discipline des éditions
  d'espace de travail : ordonnées, disjointes, chaque texte retiré
  exact, validé dès la construction. Une proposition infidèle ne s'ouvre
  pas, la revue ne répare pas ce qu'elle reçoit.
- **F-04 — `RevueAgent`** : la proposition se découpe en blocs d'éditions
  contiguës, chaque bloc se décide seul — accepté, refusé, en attente —,
  le refus réaligne les suivants : les plages de référence ne bougent
  jamais, le bloc refusé se saute. La lecture en ligne rend le retiré
  (« - ») et l'inséré (« + ») par bloc. Les points de contrôle
  capturent l'état complet des décisions et reviennent en arrière. La
  matière de `RevueDiff` de M7-t9, adaptée aux propositions d'agent ;
  l'application se prend par la voie de transaction ordinaire — les
  éditions acceptées deviennent une seule transaction, une étape
  d'annulation — et le document qui a bougé depuis la proposition refuse
  le transfert, explicitement (jamais de fusion silencieuse).
- **La démonstration** : l'agent factice (`FauxAgent`, déterministe,
  trois insertions éparses — tête, milieu, pied) propose sur le
  document ouvert ; l'atelier (`AtelierAgent`) montre l'intention
  déclarée et les lignes du bloc, décide bloc par bloc, revient au
  point de contrôle d'ouverture d'un geste, applique la fusion en une
  transaction annulable. Les dialogues de revue quittent l'activité —
  l'atelier de diff suit le même patron, `EditorActivity` repasse sous
  le plafond de 600 lignes.

### Preuve

`:core-ai:check` vert — 49 tests (Checkstyle, PMD, Error Prone,
NullAway, couverture ≥ 90 % compris), mutation 83,1 % (118/142, seuil
80). Manifeste d'API régénéré — `EtiquetteAgent`, `PropositionAgent`,
`RevueAgent` ajoutés, aucun retrait (SemVer mineure, ADR-009).
`:demo:check` vert, la ligne F-04 de la matrice passe à livrée.

## Tranche 3 — l'API d'outils pour agents (2026-11-07)

### Livré

- **F-05 — `OutilsEditeur` et `OutilsEditeurSurSession`** : lire des
  plages et l'instantané entier — la réponse porte la révision servie,
  c'est elle que l'agent rend en référence de sa transaction —, lister
  les symboles (le plan B-16, la vue de l'index de l'ADR-005) et les
  diagnostics, tous deux servis par les sources injectées par l'hôte.
  L'application détecte le conflit d'abord : l'instantané de
  référence doit correspondre, sinon refus explicite au message lisible
  (« relire, recalculer, recommencer ») — jamais de fusion
  silencieuse, la discipline de la prédiction (M8-t1) appliquée aux
  agents. Les autres refus portent leur message aussi : session
  protégée, éditions qui ne collent pas, transaction vide. La voie
  d'application est la transaction ordinaire — une étape
  d'annulation ; les éditions multiples vivent en coordonnées de
  référence, ordonnées et disjointes, la prévalidation partagée avec
  `PropositionAgent` (la simulation sur l'instantané immuable).
- **F-06 — `ConnecteurMcp`** : la surface compatible avec les
  serveurs d'outils du protocole MCP — le catalogue se liste (noms
  `celeris.*`, descriptions, arguments), chaque appel traverse la
  même méthode que l'appel direct, les réponses se rendent en JSON
  avec échappement. Le cœur ne parle aucun transport (ADR-012) :
  l'hôte qui sert JSON-RPC branche ces méthodes derrière les siennes.
- **Les types** : `PlageLue` (le texte et sa révision),
  `TransactionOutil` (les éditions et la révision lue),
  `TransactionOutilReponse` (le sort, le message, la révision
  courante).

### Preuve

`:core-ai:check` vert — 58 tests, mutation 81,1 % (154/190, seuil 80).
Manifeste d'API régénéré — six types publics ajoutés, aucun retrait
(SemVer mineure, ADR-009). Les lignes F-05 et F-06 de la matrice
passent à livrées.

## Tranche 4 — les exclusions de contexte (2026-11-07)

### Livré

- **F-07 — `ExclusionsContexte`** : le fichier du dépôt déclare les
  chemins jamais envoyés à un modèle — secrets, clés, données du
  propriétaire. Une règle par ligne : chemin exact, dossier (barre
  finale ou ancêtre), ou joker ; une règle sans barre oblique vaut
  pour le chemin entier et pour chacun de ses suffixes de composantes
  — `*.jks` prend `certs/debug.jks` comme `debug.jks`.
- **F-07 — `GardeContexte`** : le garde se glisse entre la session et
  le modèle branché par l'hôte — fantôme et prédiction gardés
  ensemble. Le chemin exclu ne produit aucun départ : pas de
  requête, donc pas de réponse — le journal du faux modèle en
  témoigne, le fantôme reste absent. Le chemin admis traverse tel
  quel.
- **F-07 — `InstructionsAgent`** : le fichier d'instructions du dépôt
  se sert par la voie des outils (`celeris.instructions`, sixième
  outil du connecteur MCP) — telles quelles, jamais inventées ; le
  dépôt sans fichier répond honnêtement qu'il n'y a rien.

### Preuve

`:core-ai:check` vert — 64 tests, mutation 81,1 % (189/233, seuil 80).
Manifeste d'API régénéré — quatre types publics ajoutés, aucun
retrait (SemVer mineure, ADR-009). La ligne F-07 de la matrice passe à
livrée.

## Tranche 5 — le rendu du fantôme et le menu d'intentions (2026-11-07)

### Livré

- **F-01, interface — `FantomePainter`** : le dessin éphémère au
  chemin des indices en ligne — une cellule d'encre pleine par
  caractère, sur la même ligne de base que le code (PIEGE-11, couvert
  par le test nommé `inlayOccupiesExactInkCells_onCodeBaseline`). La
  première ligne se pose à l'ancre, les suivantes au bord gauche du
  texte, une rangée chacune — par-dessus le texte en place, l'encre
  fanée (soixante pour cent de l'encre de base, la teinte du thème
  reste) les distingue. Le découpage en lignes se prend au dépôt de la
  suggestion : aucune allocation par image (PIEGE-15).
- **F-01, gestes — `FantomeEditeur` et la vue** : l'acceptation au
  clavier (Tab, action `ACCEPT_FANTOME` remappable dans la table
  D-17) et au geste (l'appui sur la première ligne du fantôme) accepte
  le premier mot — l'hôte exécute, la vue ne touche à aucune session
  de modèle ; `GesteFantome` porte aussi la ligne, le bloc et le
  refus.
- **F-01, la démonstration — `PiloteFantome` et `AtelierIA`** : le
  pilote, pur et testé, applique le fragment accepté à l'ancre décalée
  — chaque fragment accepté avance l'ancre du suivant — et ré-ancre le
  reste à la révision courante pour l'affichage ; la frappe suivante
  ferme le fantôme et annule le vol (F-09). L'atelier IA demande au
  faux modèle local (déterministe au contenu), affiche, applique.
- **F-02 — la prédiction à l'atelier** : le modèle local prédit une
  opération, la boîte de dialogue montre la description, l'application
  reste explicite — transaction annulable, le refus se lit.
- **F-03 — les actions IA au menu d'intentions** : suggestion et
  prédiction vivent au menu de l'atelier, au même titre que les
  intentions classiques — lint, aperçu de définition, revue du diff,
  revue d'agent — même barre, même gestuelle.

### Preuve

`:android-view:check` vert — `FantomePainterTest` compris (PIEGE-11
couvert, le registre suit) ; `:demo:check` vert — `PiloteFantomeTest`
(la boucle complète sans appareil). `EditorView` reste sous le plafond
de 600 lignes. Les lignes F-01, F-02 et F-03 de la matrice passent à
livrées.

## Tranche 6 — le consentement et la clôture (2026-11-07)

### Livré

- **F-10 — le consentement dans le garde** : `GardeContexte` consulte
  l'état de consentement à chaque départ — relu, jamais figé à la
  construction. Sans consentement, la requête reste au sol, fantôme et
  prédiction compris : pas de requête, donc pas de réponse, le
  journal du faux modèle en témoigne. L'interrupteur des réglages de
  la démonstration (`ia.consentement`, coupé par défaut) l'alimente ;
  l'atelier IA refuse poliment sans lui et propose les réglages.
- **F-10, la neutralité structurelle** : le manifeste de la
  démonstration ne déclare AUCUNE permission — pas même INTERNET. Le
  test CI `DemoNeutreTest` le vérifie (la preuve structurelle : un
  paquet sans prise ne peut pas en ouvrir) et le scénario d'exclusions
  y passe (le chemin exclu ne part jamais, même consentant).
- **F-01 — le budget de latence d'affichage consigné** :
  `LatenceFantomeVueTest` mesure mille tracés du fantôme au minimum de
  trois tours (PIEGE-13), chaque tracé sous le budget d'une image à
  soixante hertz (16 ms) — la protection est structurelle, le chemin
  est fait d'appels purs sur l'état d'image.
- **F-10 — la télémétrie documentée** : aucun paquet celeris
  n'embarque de télémétrie — l'option reste à l'hôte, derrière un
  consentement explicite, documentée au README (section « Vie
  privée »).
- **Les scénarios de démonstration** : la catégorie F complète entre
  au catalogue — F-01 à F-10, chacun citant sa ligne de matrice ;
  l'atelier IA branche le faux local derrière le garde du contexte
  (consentement relu, exclusions embarquées).

### Preuve

`:core-ai:check` vert — 67 tests, mutation 80,5 % (190/236, seuil 80).
`:android-view:check` et `:demo:check` verts (`DemoNeutreTest` et
`PiloteFantomeTest` compris), `:demo:assembleDebug` assemblé (7,3 Mo).
`./gradlew check` vert sur l'ensemble (SDK présent). Les lignes F-08,
F-09 et F-10 de la matrice passent à livrées.

## Clôture (2026-11-07)

Critères de sortie du jalon couverts : catégorie F P0 couverte
(F-01, F-04, F-05, F-09, F-10 livrées, les P1 avec — F-02, F-03,
F-06, F-07, F-08) ; budgets de latence tenus (la frappe ne bloque
jamais — mille demandes au modèle aveugle ; l'affichage sous le
budget d'une image, minimum de trois tours) ; test « aucune
requête sans consentement » vert en CI (`DemoNeutreTest`, la preuve
structurelle en tête : le manifeste de la démonstration ne déclare
aucune prise réseau). Matrice de parité mise à jour — la catégorie F
entière passe à livrée. PIEGE-11 et PIEGE-23 couverts au registre en
cours de jalon. `git status` propre, poussée sur `main`.
