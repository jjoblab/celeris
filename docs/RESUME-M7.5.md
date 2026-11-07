# Résumé M7.5 — jalon de redressement (2026-11-07)

> **Déclenchement** : le constat du propriétaire dans la démonstration —
> quatre défauts visibles (pincement muet, gouttière instable,
> scintillement de coloration, popups absents) — et l'injonction du
> prompt de redressement : « c'est ce qu'on voit et manipule sur
> l'écran qui fait foi ». Périmètre : corriger par la boucle
> test-rouge → correctif → test-vert, brancher pour de vrai, prouver
> sur appareil.
>
> **État** : les trois défauts de rendu sont CORRIGÉS, prouvés par des
> tests de vue rouges puis verts (Robolectric, ADR-013) ; la couche
> porte-popups est posée (contrôleur pur + superposition sans focus) ;
> les dix popups, le branchement de l'intelligence et la démo qui
> démontre NE SONT PAS livrés — l'audit mécanique de la matrice le
> dit sans détour. La vérification appareil n'a pas pu s'exécuter
> (aucun émulateur dans l'environnement) : le plan exact vit dans
> `docs/qualite/VERIFICATION-APPAREIL.md`, la recette manuelle dans
> `docs/recette/RECETTE-M7.5.md`.

## Le banc de test (§10) — ADR-013, la première pierre

Robolectric 4.15.1 entre comme dépendance de test de vue (lanceur
JUnit 4 porté sur la plateforme Jupiter par le moteur vintage 6.1.3 —
Robolectric n'a pas d'extension Jupiter officielle, l'écart avec la
première rédaction de l'ADR est consigné au commit). Rendu NATIF
(Skia) : les métriques de police sont réelles, la mesure qui décide
du tracé est celle de l'appareil (PIEGE-22). `EditorTestRig` assemble
le banc partagé : vue réelle mesurée et posée, exécuteur d'analyse
MANUEL (la publication ne part que sur ordre), canvas enregistreur
(chaque `drawText` avec sa couleur), générateur de gestes
multi-pointeurs, vraie `InputConnection`. Le SDK d'exécution est
épinglé à 35 (Robolectric 4.15 n'exécute pas au-delà ; le module
compile contre 36) — `android-view/src/test/resources/robolectric.properties`.

Preuve exécutée : `:android-view:testDebugUnitTest` et
`:android-widgets:testDebugUnitTest` verts — 311 et 57 tests,
Robolectric compris ; Checkstyle et PMD verts ; le travail CI
`view-tests` (ADR-013) les rejoue à chaque commit.

## Bug 1 — le pincement ne fait rien : CAUSE CONFIRMÉE, corrigé

**Cause confirmée** : `ScaleGestureDetector.getScaleFactor()` est
incrémental (rapport à l'événement précédent, voisin de 1,00) ; la vue
le lisait comme cumulé — `PinchZoom.steppedSize(14, 1,02)` restait au
palier 14, pour toujours. Deux défauts voisins confirmés aussi :
`setFont` posait `revisionSuivie = 0L` (un document jamais édité ne se
re-mesurait pas — PIEGE-28), et l'ajustement du défilement lisait la
gouttière de la mauvaise image.

**Écarts avec le prompt, consignés** :
- la trajectoire documentée (facteur cumulé 1,25 depuis 14 → palier
  16) contredit la lecture littérale de la règle d'hystérésis
  (« milieu de l'intervalle + 15 % », qui donnerait 18) : la bande
  retenue est « le palier s'entre à 15 % de l'intervalle près » — la
  seule compatible avec l'exemple ; les deux empêchent le tremblement
  de ±3 % ;
- le geste du banc part d'un écartement plus bas que l'arrivée visée
  (120 → 520 px) : le détecteur n'engage qu'après son seuil de slop
  (≈ 40 px mesuré sous Robolectric), le facteur cumulé se mesure
  depuis l'ENGAGEMENT, pas depuis le premier contact.

**Correctif** (commit `bc4ac3b`, rouges au `77e16ac`) :
`PinchZoomHandler` porte l'état du geste et reconstitue le facteur
cumulé (écartement courant / écartement d'engagement, division
protégée) ; `PinchZoom.targetSize` borne le facteur
[0,25 ; 4] et `steppedSize` marche l'échelle palier par palier avec la
bande d'hystérésis ; la colonne sous le point focal se lit à la
gouttière d'AVANT et se pose à celle d'APRÈS (la gouttière suit la
police — bug 2 compris) ; les défilements se bornent au nouveau
contenu ; l'arbitrage est net : à l'engagement du pincement, le
panoramique s'arrête (`couperPanoramique`) — jamais de double
application. `setFont` passe à `Long.MIN_VALUE` et vide le cache
(PIEGE-28). Les raccourcis de bureau arrivent : Ctrl +
plus/moins/zéro (marche d'un palier exact), molette Ctrl (un palier
par cran, point sous le curseur stable — le raccourci plus porte le
code plateforme 81, pas 70 : le banc l'a prouvé en échec), et
l'écouteur `onFontSizeChanged` prévient l'hôte à chaque changement.

**Preuve exécutée** : rouges vus (`pinchOutIncreasesFontSize` :
28 attendu, 14 rendu ; `zoomOnUneditedDocumentRemeasuresText` :
85 attendu, 44 rendu), puis verts — `PinchZoomGestureTest` (4 tests),
`KeyboardZoomTest` (5), `PinchZoomTest` réécrit au nouveau contrat (5).
Appareil : NON EXÉCUTÉ (voir VERIFICATION-APPAREIL).

## Bug 2 — la gouttière instable : CAUSES CONFIRMÉES, corrigées

**Causes confirmées** (les cinq du prompt, lues puis prouvées en
échec) : la largeur se calculait sur les RANGÉES visuelles (retour à
la ligne, plis) au lieu des lignes ; le budget de colonnes lisait la
gouttière de l'image précédente (boucle de rétroaction au seuil des
1 000 rangées) ; aucun plancher ni hystérésis (la dixième ligne
décalait tout) ; la coulisse s'élargissait à l'ARRIVÉE des marques
d'intégration ; les marges vivaient en pixels bruts (4/8/12 px
écrits en dur).

**Correctif** (commit `c8c6ad0`, rouges au `86eb0c6`) :
`GutterLayout`, pur, calcule la largeur sur les LIGNES du document
avec plancher de trois chiffres, marges en dp converties une fois en
pixels entiers, arrondi au pixel ; `EditorGutter` porte le niveau
d'eau monotone (croît immédiatement quand un chiffre s'ajoute, ne
retombe qu'au changement de session/langage ou sur
`resetGutterWidth()`) ; `GutterLanes` porte la coulisse de
CONFIGURATION (les intégrations ACTIVÉES, jamais celles qui parlent —
la marque qui arrive se dessine dans la coulisse déjà réservée) et la
syntaxe se réserve dès l'attachement du langage, avant la première
publication ; la coulisse mémoïsée n'alloue plus rien par image en
régime établi (PIEGE-15) ; l'ordre de calcul est sans cycle : gouttière
AVANT le budget de colonnes, sur des grandeurs qui ne dépendent pas du
retour à la ligne. `ClasseLargeur` rend des dp, `MARGE_RETOUR` vit en
dp — l'écart restant : `GutterPainter.MARGE_PAR_DEFAUT` reste un
repli en pixels pour ses constructions nues hors vue (les tests) — la
vue passe toujours sa marge convertie ; consigné comme dette mineure.

**Preuve exécutée** : rouges vus (`gutterWidthDoesNotJumpWhenTypingTenthLine`
: 24 attendu stable, 32 rendu ; l'arrivée des marques : 24 → 40 ; le
repli : 48 → 24), puis verts — `GutterStabilityTest` (5, dont deux
images consécutives identiques), `GutterLayoutTest` (5 : plancher,
999 → 1000 un cran exact, trois densités, coulisse de configuration
seule). Les tests du peigne de l'ancien contrat
(`MarquesIntegrationTest`, `RetourSoupleCrashTest`) suivent le
nouveau — l'ancienne attente « les marques qui arrivent élargissent »
était le défaut, pas la garantie. Appareil : NON EXÉCUTÉ.

## Bug 3 — le scintillement de coloration : CAUSE CONFIRMÉE, corrigée

**Cause confirmée** : la ligne touchée par une édition en attente ne
remappait à RIEN (`RemapLignes` rendait « inconnu ») — toute la ligne
retombait à l'encre de base jusqu'à la publication, deux états
visibles par frappe ; et chaque publication rejetait TOUTES les
mises en page de la révision (`cache.invalidate`), mesure perdue pour
un simple changement de couleur.

**Correctif** (commit `e125bce`, rouges au `44da60a`) :
`TokenCatchUp`, pur, sert la ligne touchée par ses jetons PUBLIÉS
RECALÉS (stale-while-revalidate, la couche 1 obligatoire du prompt) :
les jetons avant l'édition restent, ceux après se décalent, celui qui
contient le point d'insertion s'étend — taper dans un identificateur
ne change pas sa couleur — et le texte inséré hérite de la catégorie
du jeton de gauche, à défaut de l'identificateur ; les éditions en
attente se composent, décalées en coordonnées PUBLIÉES dès leur
enregistrement (transformation contre les précédentes) ; quand la
structure change (saut de ligne), la ligne se RELIT depuis l'état
publié qui la précède (couche 2 — le lexeur par ligne du langage) :
le provisoire peut être imparfait, il n'est jamais vide. Les lignes
épargnées gardent le panier publié DÉMONTRÉ IDENTIQUE — la garantie
de l'ancien `RemapLignes`, qui se retire (remplacé, tests compris).
La publication ne rejette plus les mesures : `RowLayoutCache.respan`
re-spanne en place, seule la rangée aux plages changée se remplace —
`SyntaxRowSource.respan` rend la même instance quand rien n'a bougé.

**Invariant établi et prouvé** (PIEGE-26) : aucune image n'affiche en
encre de base un caractère qui avait une couleur à l'image précédente,
sauf si l'édition elle-même l'a changé. Prouvé par
`twoHundredKeystrokesNeverLoseColor` : 200 frappes consécutives avec
l'analyse retenue puis publiée par vagues, image dessinée à CHAQUE
frappe, aucune décolorée — au clavier virtuel comme en composition
IME. PIEGE-20 s'amende au registre : la retombée sans couleur était
le défaut, pas la garantie.

**Preuve exécutée** : rouges vus (les quatre tests de
`FlickerStabilityTest` en échec sur le code d'avant), puis verts —
plus `TokenCatchUpTest` (5, dont 50 éditions aléatoires seedées : le
préfixe non édité garde ses plages à l'identique, couverture exacte)
et les tests de remappage réécrits au nouveau contrat. Appareil :
NON EXÉCUTÉ.

## Bug 4 — la reprise (2026-11-07) : les DIX popups se posent sur la couche

Le premier chantier de la reprise est réglé : les dix popups ont leurs
vues, leur branchement et leur ancrage. `PopupController` porte les dix
RÉGIMES — la documentation de complétion n'ouvre pas sans sa liste et
ferme avec elle, la barre de sélection cède à la frappe (la frappe
remplace la sélection) mais survit au défilement, la bande d'erreurs
ne cède ni à la frappe ni au défilement (elle vit avec les
diagnostics, l'hôte l'ouvre et la ferme). `PopupAnchor`, pur, borne
chaque ancre à la couche — sous/sur le curseur selon la place, près du
point sans le recouvrir, bandeau haut, bord droit pleine hauteur,
centrée sur la sélection. `PopupPanel` et `TextRowsPopup` partagent
fond arrondi, liseré et rangées (texte, encre par rangée, surlignage,
tronçon souligné) : CompletionListView (le filtrage vit chez l'hôte,
la requête ne repart pas), CompletionDocView (titre et sections typés
DISTINCTS, PIEGE-16), SignatureHelpView (le paramètre courant souligné
par le tronçon exact du modèle), IntentionsMenuView,
DiagnosticTooltipView, BreadcrumbView, ErrorBandView (l'appui saute au
diagnostic du cran le plus proche), SelectionBarView — PIEGE-27 prouvé
à chaque vue (`PopupViewsTest`, `PopupSurfacesTest`, `PopupRegimesTest`,
`PopupAnchorTest`). L'ancre vient de la vue : `caretBounds()` et
`selectionBounds()` (PopupAnchors, quatre tests au banc),
`setSurViewportChange()` prévient au changement réel — jamais deux fois
le même état (ViewportWatch, le clignotement ne notifie pas). La
session notifie chaque changement effectif de sélection
(`SelectionListener`, toute source comprise — pose, projection,
annulation, refaire : `SelectionListenerTest`). La démonstration
branche les dix : `PopupCoach` décide (pur, testé), `AtelierPopups`
câble, la barre de recherche coordonne son ÉTAT — son champ garde le
focus, la couche l'interdit (PIEGE-27 assumé et consigné).

## Bug 4 — état au jalon (2026-11-07) : la COUCHE était posée, les popups non

La cause du prompt est confirmée : il n'y avait que des modèles.
`PopupController` (pur, `android-widgets`) porte la machine à états —
complétion et menu d'intentions s'excluent, l'aide aux paramètres
cohabite, le survol et l'info-bulle cèdent à la frappe et au
défilement, la complétion filtre sans se fermer — testé exhaustivement
(`PopupControllerTest`, 5 tests). `PopupLayer` (ViewGroup superposé à
l'éditeur dans la même fenêtre, jamais de `PopupWindow`) porte les
popups : PIEGE-27 y est prouvé (`PopupLayerFocusTest` — la couche et
ses popups ne prennent jamais le focus, l'éditeur dessous garde le
sien et sa connexion d'entrée). **Les dix popups eux-mêmes — barre de
sélection, complétion, aide aux paramètres, info-bulle, intentions,
survol, fil d'Ariane, bande d'erreurs, recherche — ne sont PAS
livrés** : ni leurs vues, ni le branchement aux modèles
(`SignatureAideModel`, `CompletionDocModel`, `ErrorBandModel`…), ni
l'ancrage (`caretBounds`). C'est le premier chantier de la reprise ;
la matrice le dit.

## Le branchement de l'intelligence (§8) : NON LIVRÉ

`SmartTyping`, `IndentEngine`, `Gabarits`, `CompletionMots`,
`Formateur`, `Renommage`, `ImportAuto`, `ClientLsp` ne sont toujours
pas branchés à la vue : le constat du prompt reste vrai. La fenêtre
du jalon a été dépensée sur les trois défauts et le banc ; ce
chantier entier — le SPI de fournisseurs, `IntelligenceHub`, le
serveur de référence en processus — reste à faire.

## La démo qui démontre (§9) : NON LIVRÉE

Le catalogue reste décoratif (aucune auto-vérification, pas de
`ScenarioSpec`, pas de surimpression de diagnostic, pas de réglage de
lenteur d'analyse). L'audit de la matrice en tient compte : **aucune
ligne n'est « Démontré »**.

## Vérité des tableaux de bord (§11) : LIVRÉ

- **Nouveau cadre de statut** à cinq niveaux (Non démarré, Noyau,
  Câblé, Démontré, Vérifié appareil) avec critères objectifs ; la
  matrice porte les colonnes Statut / Test de vue / Scénario /
  Test d'appareil, remplie par AUDIT MÉCANIQUE (les statuts se
  déduisent des citations vérifiables, pas des affirmations) :
  `docs/parite/AUDIT-M75.md`.
- **Décomptes** : avant — 63 lignes affirmaient « livré » ; après —
  39 Câblé, 54 Noyau, 17 Non démarré, **0 Démontré, 0 Vérifié
  appareil**. La baisse est le but : un modèle pur n'a jamais été
  livré. Les correctifs M7.5 (D-15, D-01, B-01) sont Câblé ; la
  catégorie F tombe à 9 Noyau + 1 Câblé.
- **Cinq contrôles bloquants** passent dans `./gradlew check` :
  `check-parity-matrix` (statuts valides, tests cités existants,
  scénarios cités connus, « Noyau » sans test de vue cité interdit),
  `check-pieges` (chaque test cité au registre existe — PIEGE-23
  cite des tests réels à présent), `check-demo-coverage`,
  `check-language-baseline` (le rempart : 408 identifiants français
  figés, module par module — la ligne ne monte jamais), et
  `check-docs-dates` (aucun document daté au-delà du dernier commit —
  il a attrapé l'ADR-013 lui-même en cours de route, preuve qu'il
  mord). Les dates de `RESUME-M8` rejoignent l'historique git
  (2026-11-07).
- **Renommage mécanique en anglais** : NON FAIT — mesuré (408
  identifiants), rempart posé, planning à la reprise (un commit par
  module, `check` vert à chaque pas). Le code NOUVEAU du jalon est en
  anglais dès le premier commit (`GutterLayout`, `PopupLayer`,
  `PinchZoomHandler`, `TokenCatchUp`, `EditorGutter`, `RecordingCanvas`).

## Définition de terminé — ligne à ligne

- [x] Un pincement réel fait passer la police de 14 à 28 sp par
      paliers, sans vibration, point focal en place — prouvé sur banc
      (Robolectric), PAS sur appareil ; PIEGE-24 couvert.
- [x] La gouttière garde la même largeur aux dixième ligne, marques,
      replis et retour à la ligne, trois densités au banc — PAS sur
      appareil ; PIEGE-25 couvert.
- [x] 200 frappes sans AUCUNE image décolorée, clavier et composition
      IME, au banc ; PIEGE-26 couvert. (Pas de crochet de lenteur à
      300 ms dans la démo : la retenue de la publication par
      l'exécuteur manuel du banc joue ce rôle dans la preuve.)
- [ ] Les dix popups s'ouvrent dans la démo : NON — la couche et
      l'invariant de focus sont posés (PIEGE-27 couvert), les popups
      non.
- [ ] Copier/couper/coller et la barre de sélection : NON.
- [ ] Frappe intelligente, indentation, gabarits, complétion,
      formatage, renommage, imports, serveur de référence : NON.
- [x] Chaque ligne de la matrice a un statut du cadre M7.5, vérifié
      par script en CI ; « livré » n'existe plus.
- [x] Le travail CI `view-tests` existe (chaque commit) ; les tests
      de vue y sont verts dans cette fenêtre. Le travail `device-tests`
      N'EXISTE PAS : aucun test d'instrumentation n'est écrit — un
      travail vide mentirait ; le plan exact vit dans
      VERIFICATION-APPAREIL. Le plancher de couverture de android-view
      n'est PAS consigné (la mesure JaCoCo sous AGP reste à brancher).
- [x] Le rempart de langue est actif (408 figés, ratchet bloquant) ;
      le renommage est planifié avec sa mesure, non exécuté.
- [ ] `./gradlew check`, mutation, `publishToMavenLocal`,
      `publicationChecksums`, `:demo:assembleDebug` : check complet
      et APK exécutés dans cette fenêtre (voir « Preuves finales ») ;
      la mutation et la publication locale n'ont PAS été relancées
      dans cette fenêtre (aucune ligne des modules purs n'a changé).
      Le manifeste d'API et le changelog sont à jour pour android-view
      et android-widgets (non publiés) ; les modules purs n'ont pas
      bougé.
- [x] Aucun secret dans le dépôt ni son historique : vérifié par
      balayage (motifs de jetons GitHub, clés AWS, clés privées PEM)
      sur l'arbre de travail, `.git/config` et `git log --all -p` —
      zéro occurrence. Le jeton du propriétaire n'a jamais été écrit
      dans un fichier : le push passe par une URL à usage unique, la
      configuration du dépôt reste propre.

## Preuves finales exécutées dans cette fenêtre

- `:android-view:testDebugUnitTest` — 311 tests verts (Robolectric
  compris) ; `:android-widgets:testDebugUnitTest` — 57 verts ;
  Checkstyle/PMD verts sur les deux modules.
- Les contrôles de politique : matrice (110 lignes : 39 Câblé,
  54 Noyau, 17 Non démarré), pièges (25 couverts, 3 à écrire — les
  ellipses M5 PIEGE-09/10/12, plus le nouveau), démo (cohérente),
  langue (408 figés), dates (aucune future) — tous verts.
- Le `./gradlew check` complet et `:demo:assembleDebug` : exécutés en
  fin de jalon (commit de clôture) — sortie consignée au commit.

## Risques restants

1. **Les popups sans vues** : la couche est posée mais n'a jamais
   porté un vrai popup — l'ancrage, le repositionnement au clavier et
   la fusion des sources resteront à prouver au premier branchement.
2. **Le rattrapage approximatif aux découpes** : la ligne qui suit un
   saut de ligne inséré se relit depuis l'état de la ligne QUI LA
   PRÉCÈDE au publié — imparfait si la ligne précédente a elle-même
   changé d'état (chaîne non vide) ; l'invariant « jamais encre de
   base » tient, la couleur peut être provisoirement fausse.
3. **Le clic de numéro de ligne** (`surClicLigne`) reçoit la largeur
   de gouttière de l'image courante — inchangé par le jalon, à
   re-vérifier au branchement des popups.
4. **La dette de langue** (408 identifiants) : gelée, pas payée.
5. **Aucune preuve appareil** : tout ce qui précède est prouvé sur
   JVM ; le premier appareil dira le reste.
