# Résumé M8 — IA native (ouvert le 2026-11-08)

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
> **État** : jalon ouvert — tranches 1 et 2 livrées (les contrats du texte
> fantôme et de la prédiction, les faux modèles, l'annulation ; la
> revue des modifications d'agent). Les tranches suivantes restent à
> écrire (voir « Reste ouvert »).

## Avant la première tranche — l'hygiène inter-jalons (2026-11-08)

Le registre des pièges rattrape les deux pièges du rapport d'appareil
de M7 : la police qui trace devient **PIEGE-22** (couvert —
`lesPlagesSeTracentALaPoliceDeLEditeur`, `TextPainterSyntaxTest`), les
sp convertis à la densité deviennent **PIEGE-23** (couvert depuis le
2026-11-08 — `taillesSpConvertiesALaDensite`, `TaillesSpTest` : la
conversion quitte la vue pour `TaillesSp`, pure, échelle de police de
l'accessibilité comprise). Les citations suivaient dans
android-view, le changelog et le résumé M7 ; l'« État » de ce dernier,
resté à « NON clos », dit désormais clos.

## Tranche 1 — les contrats IA : texte fantôme, prédiction, faux modèles (2026-11-08)

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

## Tranche 2 — la revue des modifications d'agent (2026-11-08)

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

## Reste ouvert

- **Tranche 3 — l'API d'outils pour agents** (F-05, F-06) : lire des
  plages et des instantanés, lister les symboles, lire les
  diagnostics, appliquer une transaction avec détection de conflit ;
  surface compatible avec les serveurs d'outils du protocole MCP.
- **Tranche 4 — les exclusions de contexte** (F-07) : le fichier
  d'exclusion (chemins jamais envoyés à un modèle, assertion sur le
  journal des faux), le fichier d'instructions d'agent honoré par les
  outils exposés.
- **Tranche 5 — le rendu du fantôme et le menu d'intentions** (F-01
  interface, F-03) : le dessin éphémère au chemin des indices en ligne
  (PIEGE-11), l'acceptation au clavier et au geste, l'action IA au
  même titre qu'une intention classique.
- **Tranche 6 — le consentement et la clôture** (F-10) : aucun trafic
  sortant sans activation explicite, télémétrie en option anonyme
  documentée, test CI « aucune requête réseau en mode par défaut » ;
  budgets de latence IA consignés, matrice et changelog à jour,
  scénarios de démonstration citant les lignes F.
