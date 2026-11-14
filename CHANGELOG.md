# CHANGELOG

Tous les changements notables de ce projet sont consignés ici, par version,
en français. Format calqué sur « Keep a Changelog » ; versions suivant
SemVer. **Court par construction** : une version = une entrée ; les détails
vivent dans les ADR et le registre de portage, pas ici.

## [1.2.0] — en cours

### CI — le rouge de la politique de langue se referme (2026-11-14)
- **Typographie française corrigée** : 44 espaces sécables vivaient devant les « : » et « ; » de `README.md`, `CHANGELOG.md` et `docs/simulateur-web/README.md` (les commits du simulateur et de la parité, poussés sans le « check » racine) — `:policyLanguageCheck` refusait le build. Le mode `--fix` du script pose les espaces qu'il faut (U+00A0 avant « : », U+202F avant « ; ») : 33 lignes, aucun autre caractère ne bouge (preuve au banc : les textes hors espaces demeurent identiques). Le vert des travaux de la CI se rejoue au complet (« check » racine, publication locale, fumée de la cli, APK de démonstration) ; la consigne se retient : tout document Markdown passe au « check » racine avant chaque poussage.

### Popups — la parité au simulateur web : l'ampoule, les indices en ligne, la jauge de l'annonce (2026-11-14)
- **Le simulateur web ENTRE AU DÉPÔT** : `docs/simulateur-web/` porte l'application Next.js validée par le propriétaire — la référence de design fait foi (« une divergence entre une carte réelle et sa carte du simulateur est un défaut à corriger »). L'AMPOULE JAUNE d'y naît dans sa gouttière (à la ligne du caret qui porte un diagnostic elle remplace le marqueur, comme dans l'IDE — son tap ouvre le menu d'intentions) et les INLAY HINTS s'y lisent en fin de ligne utile (`var resultat = 0;` porte `: int` — jamais dans le flux, le texte ne se décale pas) ; le readme du dossier dit le rôle, l'installation et la table de correspondance simulateur → éditeur.
- **La jaUGE de l'annonce** : la notification porte sa BARRE DE PROGRECTION — l'horloge qui coule se voit (piste muette pleine largeur, remplissage à l'encre d'accent au prorata du reste, le tick de 100 ms vit sur le même fil que l'expiration — armer arme les deux, desarmer coupe les deux) et la légende « l'horloge la referme — X,X s » compte le rebours ; sans horloge armée, la jauge ne se trace pas (la carte ne promet pas un temps qu'elle ne tient pas). Bancs : `NotificationViewTest` +2 (la jauge coule de un à zéro, la légende au dixième — pure).
- **L'AMPOULE D'INTENTIONS de l'éditeur** : le petit icône jaune (`#FFCB6B`) se trace dans la coulisse de la gouttière — le disque du verre et son culot — et PRIME sur le point d'erreur de la même ligne (au caret en défaut, l'ampoule remplace le marqueur, le comportement de l'IDE) ; `MarquesGouttiere.setAmpoules` pose les lignes, `setSurAmpoule` rend la ligne cliquée, `GutterPainter` trace, le satellite `AmpouleIntentions` décide (la ligne du caret qui porte un diagnostic — jamais devinée) et le tap ouvre le menu. Bancs : `GutterPainterTest` +1 (l'ampoule prime), `AmpouleGouttiereTest` (pose + clic délégué, 2), `AmpouleIntentionsTest` (la décision pure, 1).
- **Les INLAY HINTS de l'éditeur** : la couche `InlayHintsPainter` trace les indices en fin de ligne UTILE (respiration de deux cellules, encre de gouttière, ITALIQUE — la même ligne de base que le code), l'API `EditorView.setIndicesInlay` publie, le moteur `MoteurInlayHints` tranche le type des `var` LITTÉRAUX (entier, décimal, suffixes L/f/d, booléen, chaîne, caractère — JAMAIS une main-d'œuvre inconnue ne se devine) et le satellite `IndicesInlay` repose à chaque édition ; le fichier Java de la démonstration gagne sa méthode `rapport()` (deux `var` littéraux — la fonctionnalité se voit sur appareil). Bancs : `MoteurInlayHintsTest` (5), le constructeur de l'orchestrateur délèste le lien des genres pour rester sous les plafonds (50 lignes, complexité 79).
- **Les ICÔNES des cartes** : le patron `TextRowsPopup` gagne la rangée ICONISÉE (`Row.icone` — le glyphe vectoriel précède le texte, la cellule respire) et l'AMPOULE d'`IconeCarte` y vit à l'encre d'avertissement — le menu d'intentions porte l'icône de chacune de ses actions (le menu Alt+Enter de l'IDE iconise ses rangées). Bancs : `PopupViewsTest` +1 (l'ampoule précède chaque action, la section reste nue, le glyphe élargit la carte).

### Popups — le simulateur interactif : chaque popup s'essaie sans appareil (2026-11-14)
- **`docs/simulateur/popups.html` naît** : un fichier AUTONOME (aucune dépendance, aucun réseau) qui reproduit la couche entière dans un navigateur. La machine à états réelle tourne en JavaScript — `PopupController` mot pour mot : régimes frappe / défilement / tap hors cartes / échappement, famille des surfaces d'actions, cascades (la documentation meurt avec sa liste, le débordement avec sa barre) ; le patron `TextRowsPopup` rend les rangées typées (paliers 0.90/0.85/0.75/0.70, filets, sections en petites capitales, encres de la palette verre). Les 26 genres vivent avec leurs sections exactes (l'en-tête des usages compte, la racine de la hiérarchie vit UNE fois, la fiche de diagnostic empile, « × java.lang.Exception » à l'encre d'erreur) et les feuilles modales servent leurs ateliers (recherche vivante, plan, aller à, renommage, réglages, revue du diff, correctif d'import, revue d'agent bloc par bloc, prédiction et consentement IA). Chaque surface DÉTAILLE son utilisation dans la fiche accompagnante (quand, comment, sections affichées, régimes, API hôte) ; les boucles complètes tournent sur le scénario de la démonstration (le masquage pose le `noinspection` et le lint relaie, la suppression sûre barre puis efface, l'exception relit la trace du document réel, l'annulation défait chaque écriture).

### Popups — l'audit Android Studio s'étend au reste des cartes (2026-11-14)
- **La fiche de diagnostic empile** : l'IDE de référence ne sert jamais une bulle par problème — la fiche porte TOUTES les signatures du lieu, chacune son marqueur « × » / « ! » à son encre, un filet les sépare ; l'INDICE D'ACTIONS clôt (« Plus d'actions · menu d'intentions » — l'équivalent du lien « More actions… » de l'IDE, posé seulement si des intentions existent : l'IDE ne promet pas de correctifs sans correctifs). Bancs : `PopupViewsTest` +2.
- **La bulle de notification gagne ses LIENS** : les actions de la bulle de l'IDE vivent en hyperliens derrière le message (à l'encre d'accent, derrière un filet) — la prise d'un lien referme la bulle AVANT de faire courir l'action (une action qui re-pose n'est jamais tuée par sa propre fermeture) ; `PopupsEditeur.notifier(titre, message, actions)` publie. Bancs : `NotificationViewTest` +1.
- **L'ENCRE EXPLICITE se pose enfin** : les entiers négatifs des couleurs opaques étaient confondus avec les sentinelles de rangée — le marqueur d'erreur, l'introuvable muet de la hiérarchie et le cadre étranger de l'exception rendaient tous à l'encre du texte ; la résolution `encreDe` se lit au banc sans passer par le rendu. Banc : `PopupViewsTest.explicitInkRendersAsItself`.

### Popups — le port : les usages vivent chez l'orchestrateur (2026-11-14)
- **La seconde surface portée suit le patron complet** : le satellite `Usages` naît dans `android-host` (le serveur de langage sert les lieux par le fil `references`, la transpose pure devient offset de saut — l'extrait se rogne), l'audit Android Studio y compte comme l'IDE : l'en-tête porte le COMPTE DU SERVEUR, tous fichiers ; l'occurrence ÉTRANGÈRE vit muette (le nom du fichier du serveur se lit, le lieu ne se visite pas, jamais il ne se devine — l'éditeur embarqué n'ouvre qu'un document), ni le doigt ni la flèche ne la prennent. `PopupsEditeur.ouvrirUsages` publie, le serveur se branche au satellite, l'atelier d'usages quitte la démonstration (le pont y perd sa ligne, le catalogue route à l'orchestrateur), la ressource `titre_usages` déménage (EN/FR, la parité ADR-008 suit). Banc : `UsagesTransposeTest` (android-host, 5).

### Popups — le port : la documentation externe vit chez l'orchestrateur (2026-11-14)
- **La troisième surface portée** : le satellite `DocExterne` naît dans `android-host` (la carte DÉRIVÉE, sa propre instance — jamais celle du survol ; le serveur par le fil `survol`, la table du coach en repli, le même chemin de données que le survol), `PopupsEditeur.ouvrirDocExterne` publie — et le câblage manquant au pont se répare : la consultation se REPOSITIONNE au défilement (l'orchestrateur porte le devoir du régime, les ateliers du pont ne l'avaient jamais câblé). L'atelier de documentation externe quitte la démonstration (le pont n'écoute plus le serveur, le catalogue route à l'orchestrateur).

### Popups — le port : les hiérarchies vivent chez l'orchestrateur, l'arbre s'aligne sur l'IDE (2026-11-14)
- **La quatrième surface portée suit le patron complet** : le satellite `CarteHierarchies` naît dans `android-host` (le moteur `Hierarchies` du cœur sert l'arbre sur le projet embarqué, la transpose pure déménage avec lui), `PopupsEditeur.ouvrirHierarchie` publie — le langage du fichier se retient au branchement du serveur, le plan au `setPlan` vif. **L'audit Android Studio corrige l'arbre** : la RACINE vit UNE fois — l'en-tête de la carte la nomme (l'arbre de l'IDE porte le type interrogé à sa racine, jamais il ne se répète dans les sections supertypes ET sous-types comme avant) ; l'introuvable vit muet à la FRONTIÈRE seule (plus jamais deux fois) ; la section VIDE ne se pose pas (l'arbre de l'IDE ne montre que les nœuds qui existent). L'atelier quitte la démonstration (le pont y perd sa ligne, le catalogue route à l'orchestrateur), les sept ressources de hiérarchie déménagent (EN/FR, ADR-008). Banc : `HierarchiesTransposeTest` (android-host, 3 — le connu et la frontière, les appelants sans la racine, la section vide élidée).

### Popups — le port : les gabarits vivent chez l'orchestrateur (2026-11-14)
- **La cinquième surface portée** : le satellite `Gabarits` naît dans `android-host` (constructeur, accesseurs, equals/hashCode/toString des champs réels de la classe englobante, entourage de la sélection, gabarit de méthode — le plan d'insertion pur déménage avec lui), `PopupsEditeur.ouvrirGabarits` publie et `configurerGabaritMethode` pose le geste A-22 de l'hôte (un seul canal — la carte n'est qu'une peau de plus) ; la carte se REPOSITIONNE au défilement (le câblage manquant au pont se répare). La fiche de diagnostic gagne aussi son satellite `InfoBulle` — la pose quitte le corps de l'orchestrateur, la complexité de classe redescend sous le plafond. L'atelier quitte la démonstration (le pont y perd sa ligne et son paramètre, le catalogue route à l'orchestrateur), six ressources déménagent et `gabarits_methode` naît (EN/FR, ADR-008). Banc : `GabaritsPlanTest` (android-host, 2 — l'insertion avant la fermante, la classe inconnue ne génère rien).

### Popups — le port : la sûreté de la suppression et du déplacement vit chez l'orchestrateur (2026-11-14)
- **La sixième surface portée (les deux cartes d'un coup)** : le satellite `Surete` naît dans `android-host` — la carte des usages qui barrent (`SAFE_DELETE` : le verdict du moteur s'affiche, chaque usage saute à son lieu, la suppression attend son heure, la voie libre efface en transaction annulable) et la carte des cibles du déménagement (`SAFE_MOVE` : les types du fichier hors la classe englobante se proposent, les conditions de sûreté du moteur seules décident, le refus s'annonce sans raison inventée). `PopupsEditeur.supprimerSur` et `deplacerDans` publient ; la carte des cibles se REPOSITIONNE au défilement (le câblage manquant au pont se répare). L'atelier quitte la démonstration (le pont y perd sa ligne, le catalogue route à l'orchestrateur), huit ressources déménagent (la copie de `titre_notification` suit, EN/FR, ADR-008). Banc : `SureteTransposeTest` (android-host, 2 — les cibles excluent la classe englobante, les usages barants deviennent des occurrences).

### Popups — le port : le masquage d'inspection vit chez l'orchestrateur (2026-11-14)
- **La septième surface portée** : le satellite `Masquage` naît dans `android-host` — les diagnostics réglés de la ligne du caret (l'identifiant entre crochets du lint, jamais une règle devinée) se proposent, le choix pose le commentaire « noinspection » en fin de ligne (transaction annulable), l'analyse se relance : la règle masquée ne se rapporte plus, les autres demeurent. `PopupsEditeur.masquerInspection` publie et `configurerRelanceLint` pose le geste de relance de l'hôte (null abrège le geste — jamais de relance devinée) ; la carte se REPOSITIONNE au défilement. L'atelier quitte la démonstration (le pont n'y garde que l'exception), quatre ressources déménagent (EN/FR, ADR-008). La boucle complète du masquage reste prouvée chez la démonstration (`SuppressionInspectionBoucleTest` — le faux build de lint y est le harnais de test).

### Popups — le port s'achève : l'exception vit chez l'orchestrateur, le pont meurt (2026-11-14)
- **La huitième surface portée — le port est ACHEVÉ** : le satellite `ExceptionCarte` naît dans `android-host` (l'échec simulé porte la trace du document réel — le type englobant du caret porte la classe, la méthode englobante porte le sommet à la ligne du caret, jamais deviné ; la trace se rend au format des piles puis se relit par l'analyseur E-06), `TraceExecution` déménage avec lui ainsi que son banc. `PopupsEditeur.executerFichier` publie ; la carte se REPOSITIONNE au défilement (régime de la consultation).
- **Le pont de test `PontSurfaces` MEURT** : les huit surfaces du feu vert vivent désormais chez l'éditeur — la démonstration ne teste plus qu'elle-même, son rôle exact (la directive du propriétaire du 2026-11-14 s'achève : les fonctionnalités vivent dans les modules de l'éditeur, l'application hôte n'a presque rien à faire — deux gestes se posent par configuration : `configurerGabaritMethode`, `configurerRelanceLint`).
- **Deux poses quittent le corps de l'orchestrateur** : l'info-bulle de diagnostic gagnait déjà son satellite `InfoBulle` ; le menu d'intentions gagne le sien (`CarteIntentions`) — la complexité de classe de l'orchestrateur redescend sous le plafond, chaque surface vit son satellite.

### Popups — la réconciliation des deux lignées du soir (2026-11-14)
- **L'info de type vit chez l'orchestrateur — la première surface portée suit le patron complet** : le satellite `TypeInfo` naît dans `android-host` (le symbole se lit chez `ContenuPopups`, le type chez `LecteurTypes` du cœur, la carte se pose sous le curseur, bornée — sans type lisible elle ne se pose jamais), `PopupsEditeur.ouvrirTypeInfo` publie, l'atelier d'info de type quitte la démonstration (le pont y perd sa ligne, le catalogue route à l'orchestrateur), le banc du lecteur déménage renommé `LecteurTypesTest`, la nomination du plafond suit le renommage (`AtelierPopups` devient `PopupsEditeur` dans les suppressions — 613 lignes, l'orchestrateur seul porte les câbles), le module gagne `api(core-intelligence)`.
- **Les deux lignées du 2026-11-13 se réconcilient** : la lignée poussée au soir (les vingt-six genres, la passe des six surfaces dormantes puis celle des cinq du feu vert) et la lignée locale du déménagement (l'orchestrateur `PopupsEditeur` dans `android-host`) vivent désormais ensemble — `AtelierPopups` s'absorbe dans l'orchestrateur par le renommage de Git, la carte de notification s'unifie sur le régime du quart (l'annonce vit son quart, seule son heure la conclut, le tap qui la prend la referme, le voile la coupe — les régimes éprouvés par la lignée distante l'emportent, les bancs contradictoires tranchés), la ligne de base de langue descend (353 → 344). Le pont provisoire `PontSurfaces` : la démonstration teste les surfaces réveillées sur le manager de l'éditeur — son rôle exact, le temps du port complet.
- **L'audit de conformité Android Studio (le message du propriétaire du 2026-11-14)** : la documentation des popups LSP y gagne les sections standard des serveurs — le modèle lit les en-têtes Markdown (« ```java », gras de ligne entière « **Parameters:** », « **Returns:** », « **Throws:** », titres « # ») comme les clôtures typées du dépôt de départ, chaque section au filet comme la doc rapide d'Android Studio ; les listes à tirets (« - nom — description ») s'encrent à l'encre des mots-clés comme les `@param`, le gras en ligne reste au corps. Bancs : `CompletionDocModelTest` +3 (sections standard, titres, épargne du gras en ligne). Le javadoc du modèle signature dit enfin « en gras » — le point courant de l'info de paramètres y est en gras, la convention d'Android Studio, le verbe « souligné » mentait.
- **Le readme règne sur les vingt-sept popups** : la table des genres gagne les neuf surfaces du feu vert (usages, info de type, hiérarchie, documentation externe, génération, suppression sûre, déplacement sûr, masquage d'inspection, détails d'exception), les feuilles citent l'aller à, le lien de l'inventaire suit son déménagement vers `docs/`.
- **Les moteurs purs des surfaces réveillées quittent la démo pour `core-intelligence`** : `LecteurTypes` (le type à la déclaration, autonome du coach — son symbole-à-l'offset s'inline), `Entourer` (l'entourage de sélection), `SuppressionInspection` (la marque « noinspection »), `CorrectifImport` (la résolution d'import au classpath) et `ResultatsAller` (la recherche floue de l'aller à) vivent désormais dans le cœur — l'intégrateur y lit ses moteurs sans la démonstration. L'API publique gagne dix-sept entrées (manifeste régénéré, croissance purement additive — ADR-009) ; le banc de la boucle complète du masquage reste chez la démo (`SuppressionInspectionBoucleTest` — le faux build de lint y est le harnais), le banc de l'entourage se nomme `EntourerSurSelectionTest` (le banc A-08 de `SmartTyping` garde son nom).

### Popups — la notification vit : la carte éphémère du coin bas-droit (2026-11-14)
- **Le genre `NOTIFICATION` naît au contrôleur (dix-sept genres)** : une carte éphémère au coin bas-droit — ni la frappe ni le défilement ne la ferment (le texte ne la regarde pas), le tap hors cartes la chasse, la feuille modale l'emporte, l'échappement la referme. L'ancre `PopupAnchor.bottomRightCorner` borne la taille aux murs AVANT de positionner (la carte trop grande ne dépasse jamais le bas ni la droite). Bancs : `PopupControllerTest` +1 (régimes), `PopupAnchorTest` +1 (coin + murs), `PopupManagerTest` +1 (échappement, voile).
- **`NotificationView` vit dans le kit** : le titre au palier titre, le message à l'encre du texte — l'horloge (4 000 ms) referme seule, la RÉ-POSE ré-arme sans mourir du retard de l'ancienne (`desarmer` retire l'horloge en vol, la course du retrait ne se paie jamais deux fois), le détachement de la scène la retire aussi. Banc : `NotificationViewTest` (3 — expiration au terme, ré-pose ré-armée sans double mort, désarmement).
- **`PopupsEditeur.notifier(titre, message)` publie** (android-host, satellite `Notifications`) : l'hôte y montre ce qui ne touche pas le texte — sauvegarde, serveur, agent. Le README (table des popups), l'USAGE §9.1 et `POPUP_INVENTORY` suivent.

### Popups — le déménagement : les fonctionnalités vivent chez l'éditeur (décision du propriétaire, 2026-11-14)
- **L'orchestration des popups déménage de la démo vers `android-host`** : `PopupsEditeur` (ex-`AtelierPopups`) orchestre depuis le nouveau paquet `jo.celeris.android.host.popups` — `DocAncre` (survol, aperçu de définition, `@Preview`), `BordDroit` (bande, minimap, pouce), `SelecteurCouleur` (le double-tap d'un littéral), `FilAriane` (né de l'extraction au plafond ADR-002), `LanguageSession` (la surface commune des serveurs — les modèles du kit y arrivent déjà convertis), `ContenuPopups` (les algorithmes purs : préfixe, complétion des mots, badges du protocole, chemin du plan, issues de la bande), `CoachPopups` (l'interface des tables de l'hôte, MUET par défaut — la complétion des mots vit sans serveur) et `PalettesPopups` (le verre dérivé du thème). Le module gagne `api(android-view)`, `api(android-widgets)` et `api(core-lsp)` : l'étage de composition de l'hôte — l'application hôte n'a presque rien à écrire. Banc : `ContenuPopupsTest` (android-host, 5 — complétion filtrée par le préfixe, régime de frappe au premier caractère, symbole et préfixe au caret, chemin du fil d'Ariane externe vers interne, candidats et documentation du protocole).
- **Le kit gagne le débordement et la barre (`android-widgets`)** : `MenuDebordement` (la carte du débordement de la barre de sélection — section NavMenu, le choix referme la carte AVANT d'exécuter l'action) et `BarreToolbar` (la carte de débordement de la toolbar pendue sous la puce + la barre de touches au-dessus du clavier, l'observateur de layout RETIRABLE) déménagent de la démo au kit, leurs dépendances nées avec eux. Banc : `MenuDebordementTest` (3 — genre lié, actions vides, le choix referme avant d'exécuter), `BarreToolbarTest` (3 — genres liés, la puce pose la carte, la touchbar vit posée).
- **La démonstration ne teste plus qu'elle-même** : `AtelierPopups` et ses satellites quittent le terrain ; `PopupCoach` ne garde que les tables factices (mots réservés Java, signatures et documentation du serveur de référence) servies par `CoachDemo` à l'orchestrateur ; `ServeurLangage`, `DeclaredServerSession` et `TablesCouverture` consomment la `LanguageSession` déménagée ; les tests déménagés avec leur code. Le README racine gagne la section « Les popups de l'éditeur » (le montage, les seize popups, les feuilles), l'USAGE §9 gagne l'orchestrateur prêt à l'emploi AVANT la composition à la main, ARCHITECTURE consigne le déménagement sous son diagramme, `POPUP_INVENTORY` §1.6 réécrit son orchestration, et les README d'android-host et d'android-widgets suivent.

### Popups — le chantier d'unification ferme : le manager règne, le clavier se délègue (D-10)
- **Le coordinateur des popups vit en `android-widgets` (`PopupManager`)** : l'UNIQUE écrivain de l'état ET des vues — `lier` (UNE vue par genre, le partage d'instances se refuse dès le câblage — l'audit P1), `montrer` (les exclus quittent la couche AVANT la pose, la carte se borne aux murs), `fermer` (les cascades n'y vivent qu'une fois : la documentation suit sa liste, le débordement suit sa barre), `surFrappe`, `surDefilement`, `fermerSousVoile`, `detacher` au cycle — l'état et la couche ne divergent JAMAIS, les zombies d'état deviennent impossibles ; les six copies de mesure (P4) meurent dans `taille`, LA mesure unique des ancres. La démonstration y migre : `AtelierPopups` construit et possède le manager, ses `fermer`/`retirer`/`taille` privés meurent (régression de volume), DEUX cartes de documentation (plus de carte partagée entre la complétion et le survol). Banc : `PopupManagerTest` (Robolectric, 13 — liaisons refusées, cascades isochrones, régimes, tap hors cartes, clavier, bornage, détachement).
- **Le clavier se délègue (PIEGE-27 préservé)** : l'éditeur garde le focus, le crochet public `EditorView.setSurTouchePopup` se consulte en tête de `onKeyDown` — le verdict consommé rend la main sans bouger le curseur ; UP/DOWN déplacent la surbrillance de la première carte interactive (contrat `PopupClavier`, deux méthodes — complétion et menus d'intentions l'implémentent), Enter et Tab valident, Escape referme (cascades comprises) ; la touche inconnue retourne à l'éditeur, la table de raccourcis décide sa vie normale. Banc : `TouchePopupVueTest` (android-view), `PopupManagerTest.leClavierDelegueSertLaCompletion`.
- **Le tap hors cartes referme les genres qui cèdent** : la couche AVERTIT sans consommer (le geste poursuit sa vie vers l'éditeur, le caret bouge) — les menus et les bulles cèdent, la complétion suit le curseur, le chrome vit avec le document ; le refus de la carte partagée entre genres, la course du retrait différé corrigée (P6 : une carte re-prise ne se retire jamais d'elle-même) et `TOOLBAR_OVERFLOW` en genre propre (P3 : deux menus ne tiennent jamais — il détournait le menu d'intentions) complètent l'audit. Banc : `PopupLayerReposeTest`, `PopupManagerTest.leTapHorsCartesRefermeLesGenresQuiCedent`.
- **Le pas étendu du clavier délégué (PAGE_UP/PAGE_DOWN, le signal du propriétaire du 2026-11-13)** : la complétion saute sa surbrillance d'une PAGE de candidats visibles — huit, son plafond —, le saut borné aux murs de la liste ; le contrat `PopupClavier` gagne `surPage(pas)` (une page par unité, la carte qui ne pagine pas DÉCLINE — la touche retourne à l'éditeur, le document y défile), le manager route les deux codes dans le même ordre de délégation. Le menu d'intentions décline (ses actions tiennent en peu de rangées). Banc : `PopupManagerTest.laPageEtendueSertLaCompletion` (+1, 14 tests).
- **La carte des usages du symbole vit — le genre 17e `REFERENCES`, la première surface réveillée du signal du propriétaire** : `ReferencesView` (`android-widgets`, patron `TextRowsPopup` — l'en-tête compte « N usages de « symbole » », les rangées cliquables « L12 · extrait », le choix SAUTE et referme) ; deux surfaces d'actions ne tiennent jamais (elle chasse et est chassée par le menu d'intentions et le débordement de la toolbar), la frappe, le défilement et le tap hors cartes la referment, le voile la coupe, le routage clavier la sert en dernier (flèches, Enter — la page décline : les rangées vivent à découvert). La démonstration gagne `AtelierUsages` : le serveur de langage sert les lieux par le fil (`references`), l'atelier transpose (le début de plage devient l'offset du saut, le lieu étranger au document ne se devine jamais — limite consignée), l'action vit au menu d'intentions, à la puce de débordement et à la carte (`action_usages`, chaînes en deux langues) ; `AtelierPopups` (612 lignes) se nomme au plafond relevé — la première nomination du chantier par la décision du propriétaire du 2026-11-13 (ADR-019, justification au `config/checkstyle/suppressions.xml`). Bancs : `PopupControllerTest` +2 (exclusions réciproques, trois régimes), `PopupManagerTest.leClavierDelegueSertLesUsages`, `ReferencesViewTest` (3 — en-tête intouchable, pick, bornage des flèches, validation, carte vide), `UsagesReferencesTest` (démo — les lieux du mot entier par le fil, la transpose du saut).
- **La spécification D-10 de `BancSpecs` rejouée par `BancSpecsTest`** : la frappe ouvre (le prédicat du coach, les mots du code par le fournisseur), la liste survit à la frappe suivante (elle filtre — le régime rend la liste vivante), Enter accepte (le candidat remplace le mot au curseur, sa documentation suit à la fermeture), Escape referme (la cascade encore) — les flèches déplacent la surbrillance par la même délégation, prouvées au banc du kit. L'USAGE §9 suit : le montage tient en trois temps (lier, déclencher, déléguer) ; les trois documents du chantier (`POPUP_SYSTEM_AUDIT`, `POPUP_SYSTEM_ARCHITECTURE`, `POPUP_INVENTORY`) consignent l'audit, l'architecture cible et la grille de classification. La matrice relit D-10.
- **La phase 9 de la mission ferme : les six livrables s'achèvent et se rangent dans `docs/`** : le rapport de migration (`docs/POPUP_MIGRATION_REPORT.md` — composant par composant, les dix problèmes de l'audit et leur traitement, les fichiers créés et modifiés, AUCUN supprimé, AUCUNE dépendance nouvelle, les limitations), le rapport de tests (`docs/POPUP_TEST_REPORT.md` — les bancs du chantier, la non-régression, le journal des quatre échecs tranchés, le verdict de la passe) et le guide du développeur (`docs/POPUP_DEVELOPER_GUIDE.md` — la recette des huit pas d'un genre nouveau sans toucher au manager, l'exemple complet de l'atelier du sélecteur, l'intégration aux moteurs) complètent l'audit, l'architecture et l'inventaire (déplacés à `docs/`) ; la feuille de route de la mission (`docs/feuille-de-route-popups.md`) tient le journal des neuf phases, les restes et les décisions du propriétaire — dont le plafond relevable à 1 200 lignes par nomination ; la feuille de route restante du projet gagne l'étape de la mission.

### Popups — la passe des six surfaces clôt le signal : type, hiérarchie, notification, valeurs de débogage, aller à, documentation externe et import local
- **L'info de type vit — le genre 18e `TYPE_INFO` et sa carte `TypeInfoView`** (le fil rouge du guide du développeur devenu réel) : une rangée titre, la carte passive ne règne pas sur le clavier (elle décrit, elle ne choisit pas) ; le lecteur local `LecteurTypes` lit la DÉCLARATION (« Type symbole », hors mots de conduite — jamais un type deviné), l'atelier `AtelierTypeInfo` la pose sous le curseur. Bancs : `TypeInfoViewTest` (2), `TypeInfoTest` (3).
- **La hiérarchie de types et d'appels vit — le genre 19e `HIERARCHY` et sa carte `HierarchiesView`** : l'arbre se rend en rangées indentées par PROFONDEUR (le patron `Row` gagne le niveau d'indentation), les moteurs `Hierarchies` (core-intelligence) servent le projet embarqué, le sommet connu SAUTE à sa déclaration, l'introuvable vit muet, le doute se porte en légende ; les surfaces d'actions ne tiennent jamais ensemble (elle chasse et est chassée par le menu, le débordement ET la carte des usages). Bancs : `HierarchiesViewTest` (3), `HierarchiesTransposeTest` (2 — sur le moteur réel).
- **La notification vit — le genre 20e `NOTIFICATION` et sa carte `NotificationView`** : la forme se décide, la bulle éphémère du coin bas-droit (l'ancre `PopupAnchor.bottomRightCorner` naît) ; le régime UNIQUE de l'annonce — frappe, défilement et geste extérieur l'épargnent, seule son heure la conclut (l'horloge de la carte, la re-pose réarme — jamais deux courses) ; le tap qui la prend la referme. Les correctifs appliqués et la pause de débogage s'y annoncent. Bancs : `NotificationViewTest` (3), `PopupAnchorTest` +1.
- **Les valeurs de débogage en ligne vivent (E-03 — la SPI était servie, la surface dormait)** : la couche `ValeursPainter` (`android-view`) rend les valeurs en fin de ligne UTILE (les blancs de queue ne comptent pas, encre de gouttière, ligne de base du code — PIEGE-11), le crochet public `EditorView.setValeursEnLigne` les pose ; la pause simulée lit les affectations ÉCRITES de sa ligne (déterministe, jamais deviné) — la SPI efface les valeurs d'une pause d'avant. Bancs : `ValeursPainterTest` (2), `FauxDebogueurTest` +1, `ValeursPauseTest` (2).
- **L'aller à fichier/symbole vit — la feuille `FeuilleAller` et le modèle pur `ResultatsAller`** (catégorie C) : la recherche floue B-13 mélange les fichiers du catalogue et les symboles du plan par score (fichiers d'abord à égalité), le choix SAUTE au symbole ou ouvre le fichier, la requête muette sert le catalogue en raccourci. Bancs : `ResultatsAllerTest` (2).
- **La documentation externe vit — le genre 21e `EXTERNAL_DOC` et l'atelier `AtelierDocExterne`** : le régime de la CONSULTATION (la frappe ne la chasse pas, le défilement la REPOSITIONNE, le geste extérieur la referme) ; la carte dérivée prend SA PROPRE instance de `CompletionDocView` (jamais celle du survol). **La résolution d'import local vit (catégorie B)** : `CorrectifImport` résout l'identifiant au curseur par le classpath du terrain, la feuille d'aperçu de l'atelier du correctif la sert en repli (B-18, B-17 — l'édition annulable d'un geste). Bancs : `PopupControllerTest` +5 (les régimes des genres nouveaux), `CorrectifImportTest` (2).
- **Les plafonds suivent la décision du propriétaire** : trois nominations ciblées rejoignent `AtelierPopups` — `EditorActivity` (le hub de scène, les événements réels de la notification) et `EditorView` (le pipeline de couches et les crochets publics, la couche des valeurs y vit), justifications au `config/checkstyle/suppressions.xml` (ADR-019). La passe de référence se rafraîchit : **2 420 tests, 0 échec, 14 modules**.

### Popups — le feu vert clôt l'inventaire : génération, sûreté, masquage d'inspection, détails d'exception (2 442 tests)
- **La génération et l'entourage vivent — le genre 22e `GABARITS`** : la carte des actions de génération au caret sert constructeur, accesseurs et equals/hashCode/toString lus des CHAMPS RÉELS de la classe englobante (B-24 — `Generateurs.champs`), l'entourage de la sélection try/catch et if (le moteur pur `Entourer` : le bloc ceint s'indente d'un cran par ligne portante, la sélection se reporte sur le bloc ceint, un geste défait l'entourage) et le gabarit de méthode à trous liés (A-22 — le canal unique de la refactorisation, la carte n'est qu'une peau de plus). Bancs : `PopupControllerTest` +1, `PopupManagerTest` +1, `EntourerTest` (3), `AtelierGabaritsTest` (2).
- **La suppression sûre et le déplacement vivent — les genres 23e `SAFE_DELETE` et 24e `SAFE_MOVE`** : les usages qui barrent s'affichent en carte d'usages (l'en-tête compte, chacun saute à son lieu, la suppression attend son heure), la voie libre efface la déclaration en transaction annulable et l'annonce ; les types candidats au déménagement se proposent en carte de cibles, le choix tente le plan — les conditions de sûreté du moteur B-23 seules décident, le refus s'annonce sans raison inventée. Les exclusions de la famille des surfaces d'actions deviennent la table exhaustive `FAMILLE_ACTIONS` du contrôleur ; la lecture du diagnostic à la ligne déménage chez l'analyseur de lint (`AnalyseurLint.aLaLigne`). Bancs : `PopupControllerTest` +2, `AtelierSureteTest` (2).
- **Le masquage d'inspection vit — le genre 25e `SUPPRESS_INSPECTION`** : les diagnostics réglés de la ligne du caret (l'identifiant entre crochets du lint, jamais une règle devinée) se proposent en carte, le choix pose la marque « noinspection » en fin de ligne — le moteur pur `SuppressionInspection` la calcule en transaction annulable, la ligne déjà masquée se refuse ; le faux build de lint HONORE la marque à sa relance : la règle masquée ne se rapporte plus, les autres demeurent. Bancs : `PopupControllerTest` +1, `SuppressionInspectionTest` (3 — la boucle complète), `FauxLintTest` +1.
- **Les détails d'exception vivent — le genre 26e `EXCEPTION_DETAILS`** : l'exécution simulée du terrain (comme le faux build de lint simule l'analyse — la trace naît du document), l'échec porte la trace du document réel (le type englobant du caret porte la classe, la méthode englobante porte le sommet à la ligne du caret — jamais deviné : le plan seul sait), la trace se rend au format des piles puis se relit par l'analyseur (E-06 se réveille) ; la carte `ExceptionView` du kit rend le type à l'encre d'erreur, le message, la pile — le cadre du fichier ouvert SAUTE à sa ligne, l'étranger vit muet ; le fichier sans méthode ne peut échouer (l'exécution s'annonce terminée). Bancs : `PopupControllerTest` +1, `ExceptionViewTest` (2), `TraceExecutionTest` (3).
- **Les exclusions deviennent une table, la nomination suit** : la famille des surfaces d'actions vit dans la table exhaustive `FAMILLE_ACTIONS` (huit genres, une surface d'actions à la fois — les menus cèdent à la complétion, les cartes qui décrivent cohabitent) ; le banc de la machine à états bute à 626 lignes de tables exhaustives — nomination du 2026-11-13 (ADR-019, justification au `config/checkstyle/suppressions.xml`). La passe de référence se rafraîchit : **2 442 tests, 0 échec, 14 modules** — l'inventaire ne compte plus de manquant (les catégories A–E closes, la catégorie F ressort de l'hôte).

### Structure — le niveau 2 natif vit, borné (H-09 rouvert, ADR-020)
- **La voie native du niveau 2 vit en `core-syntax`, BORNÉE aux fichiers modestes (≤ 200 Ko)** — le second signal du propriétaire ouvre la voie différée de l'ADR-016 : liaison JNI maison (`core-syntax/natif/liaison.c` + `natif/construire` — runtime v0.27.2 et grammaires json/python/javascript épinglés, jamais versionnés, construction OPTATIVE hors de `check`) ; `MoteurNatif` sert le re-parse incrémental depuis l'arbre précédent (une seule édition en attente — la deuxième abandonne l'arbre), `MaterialisationNatif` lit l'arbre entier APLATI — les feuilles deviennent les jetons du lexique (soudés à la convention du socle, rognés à leur ligne), les nœuds de premier niveau qui traversent les lignes deviennent les chunks du pli ; l'arbre ne survit pas au-delà de la borne (la discipline mémoire de la voie). Les paquets json, python et javascript portent `preferred: tree-sitter` avec leur repli d'origine — la bibliothèque absente ne change RIEN au consommateur. Banc : `NatifBancTest` (jetons exacts, pli exact, incrémental à l'édition, saut sans bibliothèque). L'ADR-020 révisionne l'ADR-016 ; la matrice rouvre et couvre H-09 (126 lignes, toutes couvertes).

### Rendu — le pli replié affiche son « {…} », les guides suivent les commentaires
- **La ligne d'en-tête repliée se compose en `android-view`** (`FoldedRowSource` + la couche `FoldPlaceholder`) : préfixe coloré jusqu'à la colonne du crochet ouvrant, la chip « {…} » (ou « […] », le commentaire de bloc pour une javadoc — une région fusionnée ferme au crochet fermant de sa dernière ligne), le suffixe de la dernière ligne cachée rapporté à la couleur de SA ligne ; la géométrie vit dans `pieceDe`, LA source unique, lue par la couche de fond — collée à la révision (PIEGE-20) ; le respan recompose les rangées de pli entières (PIEGE-26). Banc : `FoldedRowSourceTest` (6), `FoldPlaceholderTest` (2).
- **Les guides verticaux suivent aussi les commentaires et javadocs** : chaque suite de lignes dont la tête parle en commentaire ouvre SON trait à la colonne de sa première ligne, de la ligne après l'ouverture à la ligne avant la fermeture — même géométrie que les blocs d'accolades (H-09) ; le guide englobant continue de traverser, le commentaire de fin de ligne n'ouvre rien. Banc : `GuidesStructurelsTest` +2.

### Tactile — le mot sélectionné se voit, la pilule du dépôt de départ revient
- **La sélection se peint en voile** (`SelectionPainter`) : la teinte du thème pose son voile (alpha 0x99) au lieu de son plein — le texte tracé par-dessus reste lisible, le mot sélectionné se voit ENTIÈREMENT (le redressement du constat du propriétaire). Banc : `SelectionPainterTest` +1.
- **La barre de sélection reprend la pilule du dépôt de départ** (`android-widgets`) : libellés respirés de 12 dp, filet séparateur, icônes « i » de documentation (`setOnDocs`) et trois points de débordement, capsule au rayon de sa hauteur, pastille pressée à l'accent, cascade d'entrée (pop 160 ms, montée 5 dp, décal 24 ms) — les MÊMES actions ne font jamais re-naître la barre (le drag la déplace d'un seul tenant). Les options suivent : Couper, Copier, Coller, Tout sélectionner en principales, Dupliquer la ligne au débordement. Banc : `PopupSurfacesTest` +2.
- **Le double-tap rend son offset à l'hôte** (`setSurDoubleTap`) : son verdict consomme le geste (la cascade du tap simple ne suit pas — le curseur ne bouge pas) ou le laisse passer ; la détection vit dans `TouchesEditeur` (300 ms, seuil de tap). Banc : `DoubleTapViewTest` (3).

### Couleurs — le littéral s'encadre, le double-clic ouvre le sélecteur
- **Chaque littéral de couleur de la fenêtre se rend ENCADRÉ de sa couleur** (`PastillesTexte`, `android-view`) : fond en voile au quart de l'alpha, trait plein d'un pixel, respiré de 2 dp horizontaux et 1 dp vertical — TOUS les littéraux d'une ligne, le cadre rogné à la tranche de sa rangée (PIEGE-04). `PastillesCouleur` gagne `tous()` et `formater()` (la largeur du littéral tient au remplacement). Banc : `PastillesTexteTest` (4), `PastillesCouleurTest` +2.
- **Le sélecteur de couleur du dépôt de départ vit en `android-widgets`** (`ColorPickerView`) : en-tête des deux pastilles (l'originale et la courante, leurs littéraux hex vivants), champ saturation-valeur, bande de teinte spectrale, palette Material de seize teintes — l'application IMPLICITE (le tap de cellule ou le relâchement du drag), jamais de boutons. La démonstration y branche `AtelierCouleur` : l'alpha d'origine survit, le littéral reformaté remplace l'ancien EN PLACE. Banc : `ColorPickerViewTest` (5).

### Structure — les guides verticaux suivent les blocs, le chantier de fond se ferme (H-09)
- **Les guides d'indentation deviennent structurels en `android-view`** : `GuidesStructurels` (la table du pont de syntaxe) porte un guide par BLOC de l'analyse, jamais un trait par cran de blancs — les accolades appariées des jetons servent les langages à crochets (une accolade dans une chaîne ou un commentaire n'ouvre rien), les régions d'indentation servent les langages à décalage ; la ligne qui porte l'accolade ouvrante donne SA colonne au trait, le trait part de la ligne APRÈS l'ouvrante et meurt à la ligne AVANT la fermante — les lignes vides du corps se joignent à lui, un seul segment continu par bloc. La table vit en lignes du document (le pli replié se traverse, PIEGE-04), porte SA révision (jamais la structure d'une autre révision ne s'aligne sur les rangées, PIEGE-20) et se remappe à l'édition comme les plis (le guide survit au vol de publication, la ligne qui ouvre meurt, le guide meurt avec elle, PIEGE-26) ; la projection règle les rangées à l'image, une ouvrante coupée par le retour à la ligne descend le départ du trait sous la rangée qui porte le crochet. Le natif reste écarté — les mesures du spike tiennent (ADR-016 : incrémental 9,6–44,5 ms de p50 contre le budget de 10 ms, arbres 26–31× le texte contre 3×) : la structure niveau 2 se sert par les niveaux 1 et 3. Banc : `IndentGuidesTest` (10 tests — guide par bloc à la colonne ouvrante, bornes sous l'ouvrante et au-dessus de la fermante, lignes vides traversées, pli traversé, accolades de chaîne et de commentaire muettes, sans langage muet, tabulation au taquet, édition sans publication) et `GuidesStructurelsTest` (6 tests — table des accolades appariées, chaîne et commentaire muets, paires sans corps, régions d'indentation, remappage à l'édition, mort avec l'ouverture). La matrice ferme la ligne H-09 et relit A-17 : sa porte compte 126 lignes, toutes couvertes.

### Ouverture — le binaire se détecte, se prévoit, se refuse (A-26)
- **Les fichiers binaires vivent en `android-host`** : `DetectionBinaire` décide sur les octets BRUTS, dans le préfixe borné (8 Kio) — l'octet nul et les contrôles hors texte refusent, jamais une extension de nom (elle ment), jamais un décodage (il détruirait ce qu'il lit : l'octet nul devenu remplacement, la perte se confondrait). `HexadecimalPreview` sert la lecture sûre — chaque ligne porte son décalage, seize octets en hexadécimal, la gouttière des imprimables ; PLAFOND de 256 lignes (4 Kio), drapeau de coupe : la mémoire de l'hôte ne porte jamais le binaire entier pour le montrer. L'arbre de documents gagne `lireOctets` (octets bruts, jamais l'aller-retour du décodage) ; le système de fichiers virtuel gagne `estBinaire` et `apercuBinaire` — défauts honnêtes : les systèmes à texte seul disent non. L'adaptateur de production REFUSE l'écriture par la voie texte sur un fichier binaire (`FichierBinaireException`) — jamais l'aller-retour destructeur ; la session qui montre l'aperçu se pose en lecture seule (la brique vit au cœur depuis M2). Banc : `FichiersBinairesTest`, 7 tests — octet nul, contrôles et blancs, préfixe borné (limite documentée), lignes de vidage exactes, plafond et drapeau, détection-aperçu-refus au système virtuel, mémoire de référence. La matrice gagne la ligne A-26 : 124 lignes couvertes sur 125, la dernière est le chantier de fond (H-09).

### Correctifs — l'aperçu se prévoit avant l'acceptation (B-18)
- **L'aperçu d'un correctif vit en `android-widgets`** : `PatchPreview` porte la prévision — les lignes du document avant et après l'édition proposée se lisent en préfixe « - » et « + », chaque bloc changé encadré de contexte (deux par défaut, le contexte nul ne montre que les changées) ; les blocs proches partagent leurs lignes intactes, le hors-bloc ne se montre jamais, les textes identiques ne se prévisent pas — l'aperçu n'engage à rien. L'atelier de la démo sert la feuille du kit (ADR-018) : le correctif proposé sur la sélection arrive par le fil du protocole, l'édition se joue sur une COPIE pour se prévoir — jamais sur la session ouverte —, l'acceptation applique par la voie de transaction annulable d'un geste (B-17), le refus ne touche à rien ; le terrain du serveur de référence enveloppe la sélection d'accolades (la sélection vide n'engage à rien). Banc : `PatchPreviewTest` (10 tests — préfixes, contexte, hors-bloc, contexte nul, blocs proches, ajout pur ancré et de fin, suppression pure, identiques, refus d'entrée) et la spécification B-18 de `BancSpecs` rejouée par `BancSpecsTest` — le correctif se voit, s'applique, se défait. La matrice gagne la ligne B-18 : 123 lignes couvertes sur 125.

### Survols — la documentation des modificateurs se sert au fil (C-10)
- **La documentation des modificateurs d'agencement vit en `core-lsp`** : `DocModificateurs` porte la table — padding, background, size, fillMaxWidth, weight, align, offset, clickable, alpha, border, clip, shadow, aspectRatio, wrapContentSize… et le récepteur `Modifier` lui-même, dix-neuf entrées, textes français, signature vivante en clôture Kotlin (PIEGE-16) ; le NOM connu seul décide (ADR-005) — l'identifiant qui porte le curseur se lit, la forme se vérifie au passage (le modificateur se nomme en position d'appel, blanc intercalé toléré, le récepteur se survole en toute position), les refus rendent un survol nul, jamais un texte faux ; la lecture est TEXTUELLE, jamais lexicale — limite documentée. Le serveur de référence de la démo sert la table au fil du protocole (B-09 inchangé par ailleurs), l'échantillon Kotlin gagne sa chaîne `Modifier.padding(16.dp)`. Banc : `DocModificateursTest` (8 tests — documentation servie, plage aux bornes, récepteur, forme d'appel, refus, bornes du curseur, lecture textuelle) et la spécification C-10 de `BancSpecs` rejouée par `BancSpecsTest` — l'appui long du doigt sélectionne le nom, la documentation s'ancre au curseur. La matrice gagne la ligne C-10 : 122 lignes couvertes sur 125.

### Hiérarchies — les types et les appels se parcourent en arbres (B-12)
- **Les hiérarchies de types et d'appels vivent en `core-intelligence`** : `Hierarchies.types` rend les ancêtres par générations et les descendants en profondeur — les clauses `extends`/`implements` se lisent à l'en-tête, chevron de généricité porté à part (le `T extends Nombre` d'une borne n'est pas un parent), le nom qualifié prend son dernier segment ; `Hierarchies.appelants` rend les appelants en arbre — chaque site d'appel nu ou `this.nom(` compte, la signature de la méthode homonyme s'épargne, la récursion directe se montre une fois (profondeur un) ; le doute se signale SANS arête, jamais tranché (récepteur étranger `x.nom(`, référence de méthode `nom::`, appel hors méthode) ; les parents hors du projet embarqué se signalent introuvables, la frontière s'arrête là. L'index reste nominal (ADR-005) : les surcharges homonymes fusionnent, limite documentée. Banc : `HierarchiesTest`, 12 tests — profondeur trois d'ancêtres et quatre d'appels sur le corpus multi-fichiers, descendants en profondeur, bornes de généricité, nom qualifié, introuvables, doute (récepteur étranger, appel hors méthode), récursion, construction exclue, nom blanc. La matrice gagne la ligne B-12.

### Refactorisation — la chaîne se ressource, le geste reste annulable (C-13)
- **L'extraction d'une chaîne vers une ressource vit en `core-intelligence`** : `ExtractionRessource.plan` porte le curseur au littéral double-guillemeté, le toucher suffit — le lexique dit les bornes, jamais l'arbre ; le littéral devient `getString(R.string.nom)`, la ressource naît dans le fichier de valeurs par défaut avant la fermeture de `<resources>`, au retrait des entrées existantes (un cran sous la fermeture quand le fichier est vierge) ; les cinq échappements de base se décodent, les entités du XML se protègent ; le nom se valide (minuscule d'abord, tiret bas, chiffres) et un nom déjà défini dans le projet REFUSE — jamais un plan faux. Le plan porte ses éditions par fichier et s'applique par la voie de transaction, annulable d'un geste par fichier (le patron multi-fichiers du renommage). Banc : `ExtractionRessourceTest`, 13 tests — ressource créée, référence insérée, annulation, refus (nom pris, camelCase, chaîne vide, fichier de valeurs absent ou brisé, hors littéral, apostrophe). La matrice gagne la ligne C-13.

### Reprise du propriétaire — cinq écarts redressés, la revue ouverte
- **La reprise du 2026-11-12 a couvert les cinq écarts de la revue comparative** : l'icône de pli répond au doigt (`PliAuTapTest`, PIEGE-02 tenu par vidage du cache) ; la minimap se rend COLORÉE (`MinimapModelTest` — segments encrés, code, chaînes, commentaires, nombres, mots-clés) ; la toolbar porte son débordement dans la CARTE du kit et sa barre de touches au-dessus du clavier (`TouchBarViewTest`, ADR-018) ; l'Entrée entre accolades rend le repli textuel (`EntreeRepliTextuelTest`) ; le serveur Java déclaré s'éprouve entier — la signature joint le survol au POINT D'USAGE, les diagnostics se poussent, les popups vivent aux bons moments (`JavaSignatureTest`, `JavaHoverTest`, `ServeurDeclareMomentsTest`, H-13). La revue comparative s'ouvre contre Android Studio :
  [`ANALYSE-COMPARATIVE-2.md`](docs/audit/ANALYSE-COMPARATIVE-2.md) — 119 lignes de la matrice couvertes sur 125, AUCUNE P0 ouverte, six P2 restantes. La porte verte se referme au passage{NB}: parité des ressources mesurée aux noms (ADR-008), plafonds ADR-002 repris (le serveur, la minimap, la démo redescendent sous les plafonds), manifeste d'API du serveur committé (ajouts purs, SemVer tenu).

### Langages par paquets — Go et Rust, la longue traîne reprend (C5)
- **Go et Rust vivent en paquets intégrés** : la longue traîne reprend — `langages/go` et `langages/rust` reprennent les grammaires du dépôt VS Code SANS MODIFICATION (main vérifié le 2026-11-12 ; go convertie du projet worlpaker/go-syntax, rust du projet dustypomerleau/rust-syntax, l’amont consigné au cgmanifest du dépôt vscode) ; `PaquetGoIntegreTest` et `PaquetRustIntegreTest` verrouillent l’égalité octet pour octet, les acceptations (go 85,3 % ≥ 85 %, rust 100 % ≥ 98 %) et le rejeu déterministe des échantillons ; le banc du commerce compte quinze grammaires, la matrice compte vingt-quatre langages intégrés. Le reste de la traîne (vingt-sept du dépôt de départ) dort sur le signal du propriétaire.

### Publication distante — les douze artefacts paraissent (C1)
- **La publication distante vit enfin** (2026-11-12) : les DOUZE artefacts
  `jo.celeris:*` — les huit modules purs, les trois bibliothèques Android
  en AAR, le nouveau `serveur-lsp-java` — paraissent au registre de
  paquets de l’hébergeur (`maven.pkg.github.com/jjoblab/celeris`, ADR-011) ;
  un projet vierge consomme `jo.celeris:core-text:1.1.0` (compile, exécute,
  édite) et l’AAR `android-view` s’y vérifie : les sommes SHA-256 des
  artefacts consommés depuis le registre correspondent octet pour octet
  aux artefacts construits — la sortie est reproductible, la publication
  est la preuve. Les tags `v1.0.0` et `v1.1.0` se posent (le premier sur
  le commit de la 1.0.0, le second sur la clôture).

### Étage appareil de vérification — le second étage passe vert (C11)

- **Les quatre bancs d'instrumentation vivent dans `demo/src/androidTest`**
  et PASSENT VERT sur l'appareil (premier passage vert le 2026-11-12,
  déclenchement manuel) : `PinchZoomOnDeviceTest` (les mêmes
  `MotionEvent` que le banc JVM, envergures calibrées au-dessus de
  l'envergure minimale de mise à l'échelle du détecteur — 14 puis 28
  puis 14 sp, les paliers de PIEGE-24), `GutterStabilityOnDeviceTest`
  (le RENDU RÉEL de la vue en bitmap — le vrai Skia de l'appareil :
  l'abscisse de la première encre et le décalage minimal restent sous
  le pixel à la publication d'une analyse, PIEGE-25),
  `FlickerOnDeviceTest` (quatre rendus pendant une frappe à analyse
  retenue par le crochet de débogage et après publication — les mots
  non touchés gardent leurs pixels, PIEGE-26) et
  `CompletionPopupOnDeviceTest` (le premier popup réel de complétion
  par le serveur de langage Java — le focus ne quitte jamais
  l'éditeur, le blanc referme la liste, PIEGE-27).
- **La frappe vit par la voie exacte de l'IME** : la connexion d'entrée
  de l'éditeur porte les textes, le curseur se borne en fin de document —
  les bandes mesurées (les premières lignes) restent jamais touchées ;
  l'appareil géré `pixel2api34` (image ATD, API 34) les exécute, les
  dépendances d'instrumentation vivent au catalogue (`androidx.test`,
  UiAutomator 2.3.0 — jamais dans les artefacts publiés).
- **La CI exécute l'étage** : le travail `appareil.yml` court au
  nocturne, à chaque fusion de demande et au déclenchement manuel —
  rapports en artefact `device-tests-reports`, la règle udev ouvre
  `/dev/kvm` au runner, le journal des échecs imprime les XML (messages
  d'assertion, aperçu ASCII, mesures du détecteur). Le plafond
  Checkstyle couvre `src/androidTest` comme les bancs JVM (plafonds
  600/800, ADR-019) ; les leçons de l'image ATD (l'arbre muet, l'écran
  qui s'éveille muet, le seuil d'envergure, le compositeur sans vitrine)
  se consignent au document de vérification — la recette manuelle du
  propriétaire (`docs/recette/RECETTE-M7.5.md`) garde la vitre de
  référence.

## [1.1.0] — 2026-11-12

### Mini serveur de langage Java — le compagnon de banc de l'éditeur

- **Le module `serveur-lsp-java` (JVM pure, douzième artefact publié)** :
  un serveur LSP Java complet mais volontairement petit, déterministe,
  sans réseau ni système de fichiers — pour éprouver l'éditeur de bout
  en bout contre un serveur réel, sans archive tierce.
- **Les capacités servies** : poignée de main et négociation (complétion,
  survol, symboles, plis, formatage, synchronisation complète),
  diagnostics POUSSÉS à chaque ouverture et changement (équilibre des
  paires, littéraux non fermés, point-virgule probable), complétion par
  mots-clés et symboles du document, survol documenté, symboles
  hiérarchiques, plis d'accolades et de commentaires, formatage
  d'indentation idempotent.
- **Le montage** : la convention des serveurs embarqués (constructeur
  vide, `setClient`) — montable par objet, PAR DÉSIGNATION (la voie des
  déclarations `servers.json`), ou exécutable seul sur l'entrée-sortie
  standard (`JavaLanguageServerMain`) pour les bancs et l'intégration
  continue qui veulent un processus séparé. Il ne surveille jamais la
  mort d'un processus parent (le piège fatal aux hôtes embarqués,
  constat M8.6).
- **La preuve** : `JavaLanguageServerTest` monte le serveur sur des
  tubes à file et le rejoint par la session cliente de `lsp-protocole`
  — la poignée de main, les diagnostics poussés, la complétion
  documentée, le survol, les symboles, les plis et le formatage
  traversent les deux bouts de la même bibliothèque, et le montage par
  désignation s'y éprouve comme la démo le vivra ; les analyses pures
  ont chacune leur banc (`JavaSourceTest`, `JavaDiagnosticsTest`,
  `JavaSymbolsTest`, `JavaFoldsTest`, `JavaCompletionTest`,
  `JavaHoverTest`, `JavaFormatterTest`) et l'architecture son
  `ArchitectureTest` (aucune dépendance montante, la bibliothèque
  exceptée). La démonstration déclare le serveur en donnée
  (`serveurs/servers.json`, langage Java) et le monte par la
  désignation : `DeclaredServersTest` le prouve.
- **Ruptures pré-1.0 (ADR-009)** : aucune — module nouveau, manifeste
  `api/serveur-lsp-java.api` commité.

### Chantier C2 — hygiène du dépôt

- **Dependabot** (`.github/dependabot.yml`) : veille hebdomadaire sur
  le catalogue de versions gradué et sur les actions épinglées par
  somme SHA ; correctifs et mineures groupés, majeures isolées —
  chaque montée de version traverse les portes qualité.
- **Politique de sécurité** (`SECURITY.md`) : versions prises en
  charge, signalement privé par courriel (jamais d'issue publique),
  délais annoncés, périmètre des huit artefacts publiés — la
  procédure du dépôt de départ, adaptée à l'état réel du dépôt
  (SBOM, provenance, revue M9).

### Chantier C3 — cohérence documentaire

- **Erratum du résumé M9** : huit modules publiés (le `cli` n'a pas
  `maven-publish`), 16 lignes P0 au statut Noyau (la dette consignée
  ligne à ligne par la matrice), parité en/fr mesurée à 99/99 clés.
- **cli** : la bannière affiche 1.0.0 ; un test épingle la version
  annoncée — plus de version fantôme.

### Interface — maquettes ADR-018

- **Maquettes réactives des popups** (`docs/maquettes/popups.html`) :
  les trois formes de l'ADR-018 — feuille du bas (réglages, palette de
  commandes, recherche/remplacement), feuille du haut (revue d'agent),
  popup ancrée (complétion, diagnostic, aide aux paramètres) — en un
  fichier HTML autonome, sans dépendance ; trois thèmes (clair, sombre,
  contraste élevé) et trois gabarits (téléphone, tablette, bureau).

### Interface — chantier C12, tout en popups

- **Feuilles du kit** (`android-widgets`) : la feuille du bas et la
  feuille du haut de l'ADR-018 — poignée de glissement, rangée de
  titre, bouton de fermeture, corps défilant, voile, quatre
  fermetures (bouton, voile, glissement au-delà du seuil, retour) —
  aux jetons de la maquette ; onze tests de vue (Robolectric).
- **Douze surfaces migrées** : l'écran de réglages devient feuille du
  bas (l'activité disparaît du manifeste) ; le plan latéral, la
  recherche et le renommage la rejoignent ; les revues par blocs
  (diff D-12, agent F-04), le rappel de consentement IA (F-10) et la
  décision de prédiction (F-02) ouvrent la feuille du haut ; l'aperçu
  de définition (D-11) et le rendu `@Preview` (C-11) passent en
  popups ancrées ; les verdicts de prédiction rejoignent la barre
  d'état. Plus aucun dialogue système, plus aucun écran plein cadre
  hors des conteneurs hôtes — l'inventaire est consigné
  (`docs/audit/INVENTAIRE-SURFACES.md`).

### Charge et limites — chantier C10

- **Banc à 480 000 lignes** (`Charge480kLignesTest`, tas dédié de 1 Go
  sur les tâches de test des modules de bancs) : la fenêtre d'abord
  1441 ms (budget 2500 ms), l'incrément après édition au milieu 36 ms
  (budget 100 ms), le tas de l'ouverture +90 Mo (budget 192 Mo) — le
  calibrage est consigné dans le banc, le défilement ne coûte rien.
- **Mode gros fichiers tri-état** (`LargeFileMode`, à la manière du
  dépôt de départ) : AUTO (défaut) suit les seuils « gros document »
  (2,5 M caractères, 50 000 lignes), ON force les coupures, OFF les
  ferme — la complétion au curseur coupe, l'édition reste entière
  (coloration, frappe, annulation).
- **Limites au README** (nouvelle section « Charge et limites ») : les
  budgets mesurés et les seuils du mode — repris au `USAGE.md`
  (chantier C4).

### Chantier C4 — guide d'intégration hôte

- **`USAGE.md` à la racine** (719 lignes) : le guide d'intégration
  qui manquait à tout hôte — coordonnées des huit artefacts et leur
  disponibilité (mavenLocal aujourd'hui, registre de l'hébergeur à C1,
  modules Android par inclusion de source), déclaration des
  dépendances, montage de la vue, modèle de texte, registre des
  langages, paquets de données, serveurs de langage déclarés, popups
  et feuilles (ADR-018), SPI hôte (VCS, exécution, débogueur,
  fichiers), contrats IA et consentement, thèmes, budgets de
  performance attendus, grands documents et coupures, cli, intégrité
  et provenance, vie privée, dettes restantes, pièges d'intégration.
- **Vérifié contre les manifestes** : chaque coordonnée d'artefact
  et chaque nom d'API cité est vérifié contre `api/*.api` (ADR-009)
  et les sources publiques des modules Android — 175 identifiants
  balayés, tous présents ; le squelette suit `MIGRATION.md` et
  l'USAGE.md du dépôt de départ (audit § 9, recommandation 4 ; § 6,
  gap 3).

### Langages par paquets — chantier C5

- **HTML, première grammaire du commerce livrée en paquet intégré** : `html` rejoint les paquets intégrés (`langages/html/`) — la grammaire TextMate HTML des extensions de VS Code (MIT, main épinglé du 2026-11-08), reprise SANS MODIFICATION de la copie épinglée du banc du commerce ; `PaquetHtmlIntegreTest` verrouille l'égalité octet pour octet, l'acceptation (98,7 % ≥ seuil 97 %) et le rejeu déterministe de l'échantillon. Licence MIT au NOTICE, somme SHA-256 épinglée au NOTICE et au registre de portage.
- **La famille web du commerce entière** : `css`, `javascript` et `typescript` rejoignent HTML — les grammaires TextMate des extensions de VS Code (MIT, main épinglé du 2026-11-08), reprises SANS MODIFICATION des copies épinglées du banc ; les bancs `PaquetCssIntegreTest`, `PaquetJavascriptIntegreTest` et `PaquetTypescriptIntegreTest` verrouillent l'égalité octet pour octet, les acceptations (css 96,6 % ≥ 95 %, javascript 90,3 % ≥ 88 %, typescript 90,2 % ≥ 88 %) et le rejeu déterministe des échantillons. Licences MIT au NOTICE, sommes SHA-256 épinglées au NOTICE et au registre de portage ; la matrice compte vingt-deux langages intégrés. Le chantier poursuit : Go, Rust… (mêmes dépôt et épingle que le banc).

- **Le nom racine des grammaires devient facultatif** : le format TextMate l'autorise et la grammaire PHP des extensions de VS Code s'en passe — le chargeur tolère l'absence (la portée racine sert de repli affichable), le banc de conformité suit le nouveau contrat ; une grammaire réelle du commerce ne se réécrit plus pour charger.
- **PHP et Ruby vivent en paquets intégrés** : la longue traîne annoncée commence — `langages/php` et `langages/ruby` reprendent les grammaires du dépôt VS Code SANS MODIFICATION (main épinglé du 2026-11-08, vérifié inchangé le 2026-11-10) ; `PaquetPhpIntegreTest` et `PaquetRubyIntegreTest` verrouillent l'égalité octet pour octet, les acceptations (php 96,4 % ≥ 95 %, ruby 98,9 % ≥ 98 %) et le rejeu déterministe des échantillons ; le banc du commerce compte treize grammaires. Le signal du propriénaire arrive le 2026-11-12 — la traîne poursuit dessous.

### Deuxième vague — chantier C13 (parité Android Studio d'abord)

- **Mots à bosses de chameau (A-04)** : `CamelWord` raffine les frontières
  de mots UAX #29 — minuscule vers majuscule (« monCompteur » coupe avant
  le C), fin de course de majuscules (« HTTPServer » coupe avant le S
  final), lettre et chiffre du script commun (« abc123def » se coupe en
  trois) ; les écritures sans casse (CJK, arabe, hébreu) ne changent d'un
  cran, une bosse ne coupe jamais deux mots UAX. Le déplacement et la
  sélection par mots (`CaretsMotion`) suivent les bossages — la fenêtre du
  recul embarque un caractère au-delà du curseur pour lire la fin de
  course. Le critère de la matrice se vérifie mot pour mot :
  « monCompteurDeLignes » se coupe en 4 mots (`CamelWordTest`,
  `CaretsMotionTest`).
- **Extraire — les cinq variantes (B-20)** : `Extraction` — l'expression
  sélectionnée devient une déclaration et la sélection se remplace par
  son nom. La variable locale se pose avant la ligne de la sélection
  (`var nom = expression ;`, méthode porteuse exigée) ; la constante et
  le champ se posent avant l'accolade fermante de la classe porteuse, le
  type lu sur la forme du littéral (`literalType` : int, long, double,
  float, boolean, char, String), le nom de constante crié aux joints de
  casse (« maLimite » devient « MA_LIMITE ») ; la méthode naît après
  la porteuse — l'analyse des flux (`FlowAnalysis`, `MethodStructure`,
  `TypeRun`, `ExpressionTypes`) lit les références libres : les
  paramètres et locaux de la porteuse deviennent ceux de la nouvelle
  méthode (dans l'ordre de leur première lecture, `var` lu sur son
  initialisateur littéral), les champs passent, le type de retour se lit
  sur la forme (promotion numérique, concaténation à « + » seuls) ; le
  paramètre réécrit la signature et passe l'expression à chaque appel du
  document (constructions comprises), l'expression n'y lit que littéraux
  et champs — l'appel sur récepteur hors `this`, la référence `::` et la
  surcharge homonyme refusent. Refus par plan nul, jamais par plan
  faux ; les éditions descendent — le plan s'applique en une
  transaction annulable (`ExtractionTest`).

- **Aligner en ligne (B-21)** : `Inline.declaration` — la déclaration
  désignée disparaît : ses usages reçoivent le littéral de son
  initialisateur, la déclaration se supprime si elle est locale ou
  privée, le champ visible hors de la classe reste déclaré. Le membre
  de `this` se remplace en entier (`this.champ` devient le littéral).
  Le refus mène, jamais le faux : initialisateur littéral seul (un
  littéral est atomique, aucune précédence ne se brise), réaffectation
  refusée (composée, incrément, décrément), ombre homonyme refusée
  (redéclaration, paramètre de lambda, champ ombré par une locale),
  récepteur étranger refusé (`InlineTest`).
- **Changer la signature (B-22)** : `SignatureChange.reorder` — les paramètres
  de la méthode désignée se réordonnent, la signature et chaque appel du
  document (constructions et appels récursifs compris) suivent le nouvel
  ordre en une transaction annulable. Le refus mène, jamais le faux :
  surcharge homonyme, référence de méthode, appel d'arité différente,
  varargs, chevrons non appariés (comparaison ou type explicite —
  l'ambiguïté ne se tranche pas) ; l'ordre qui n'est pas une permutation
  se refuse d'exception (`SignatureChangeTest`).

- **Déplacement et suppression sûre (B-23)** : `SafeDelete.delete` — la
  déclaration désignée par la sélection (méthode, type ou champ — la
  plus intime) ne disparaît que si le document ne l'utilise nulle part
  ailleurs : le verdict rend les usages barants (le client les montre
  avant de renoncer, à la manière de l'étalon) ou le plan d'effacement
  quand la voie est libre. Les appels et leurs suspects barrent une
  méthode (récepteur étranger ambigu, référence `::`), les occurrences
  du nom barrent un type ou un champ (l'appel homonyme d'une méthode
  déclarée ne lit pas le champ), la récursion seule ne barre pas, le
  champ qui partage sa ligne ou sa déclaration refuse ; l'effacement
  prend les lignes entières, la ligne blanche voisine part avec lui.
  `SafeMove.move` — la méthode désignée déménage dans un autre type du
  document, annotations contiguës comprises, si et seulement si rien ne
  pend : usage du nom hors déclaration, lecture d'un champ d'origine,
  appel d'une méthode sœur, conflit homonyme dans la cible,
  constructeur, surcharge, cible d'une ligne refusent ; le corps se
  réindente au niveau des membres de la cible, en une transaction
  annulable (`SafeDeleteTest`, `SafeMoveTest`).

- **Historique local par fichier (A-14)** : `HistoriqueLocal` enveloppe le
  système de fichiers virtuel (E-01) — chaque écriture archive le
  contenu qui s'en va (l'écriture qui ne change rien n'archive rien),
  l'horloge injectée horodate chaque `RevisionLocale`. `revisions`
  sert l'histoire chronologique du fichier, vide pour un fichier sans
  passé ; `restaurer` réécrit le contenu d'une révision passée par la
  porte commune — la restauration archive le contenu remplacé, elle
  se défait comme elle s'est faite. La révision d'un autre fichier,
  ou forgée, refuse d'exception. L'isolement tient par construction :
  l'histoire vit par chemin, celle d'un fichier ne fuit jamais dans
  celle d'un autre (`HistoriqueLocalTest`).

- **La cli outillée (G-03)** : l'exécutable sans Android gagne les trois
  commandes qui lui manquaient — `diagnostiquer` (les plages malformées
  de l'arbre, ligne et colonne, code 3 quand un diagnostic existe),
  `formater` (le texte formaté seul sur la sortie, quatre espaces par
  niveau) et `refactorer renommer` (le fichier renommé seul sur la
  sortie, appliqué par la transaction ; les conflits signalés sans être
  tranchés — ambiguïté et collision disent code 4). Les codes de sortie
  se documentent : 0 succès, 1 fichier illisible ou langage sans
  analyse, 2 usage inconnu, 3 diagnostics trouvés, 4 conflit de
  renommage ; la sortie est déterministe (`CelerisOutilsTest`).

- **Complétion postfixe (A-23)** : `PostfixCompletion.expand` — l'expression
  devant le point se consomme dans son gabarit, en une seule édition :
  `expr.if` devient `if (expr) {}`, jamais le préfixe ne se matérialise
  (le piège hérité du lot L0). Sept motifs à la syntaxe Java (`for`,
  `if`, `not`, `notnull`, `null`, `return`, `while`), le curseur entre
  les accolades pour les blocs et à la fin pour les fins
  d'instruction ; l'expression se lit en remontant identifiants,
  points et appels appariés ; le mot incomplet, sans point,
  l'expression vide, littérale ou mal terminée refusent
  (`PostfixCompletionTest`).

- **Recherche dans le projet (A-21)** : `RechercheProjet.chercher` — la requête
  se pose sur le projet embarqué, hors ligne. La requête qui nomme un
  identifiant entier suit la voie de l'index (les occurrences déjà comptées
  servent, jamais les chaînes ni les commentaires) ; la requête partielle
  suit la voie du balayage (chaque fichier se lit de bout en bout, chaînes
  comprises). Ordre déterministe : uris du dictionnaire puis offsets
  croissants ; motif vide refusé d'exception ; latence consignée au banc
  (60 fichiers de 200 lignes : index 0 ms / budget 200 ms, balayage 1 ms /
  budget 400 ms) (`RechercheProjetTest`).

- **Raffinements de complétion (C6)** : les quatre raffinements du dépôt de
  départ, l'ancien servant d'oracle. `CompletionMatcher` — l'apparieur unifié
  à six paliers (exact, casse, préfixe, casse, bosses de chameau,
  sous-séquence), groupes de départage (résolu, mot-clé, gabarit, mot du
  tampon), tri stable, identifiant de tête (la signature ne participe jamais
  au flou), positions en gras exactes. `AcceptanceHistory` — l'apprentissage
  du classement : fréquence puis fraîcheur, compartiments étanches par
  langage, capacité 200 identifiants, bascule, purge, persistance par
  photographie versionnée. `ContextualCompletion` — les constantes
  d'énumération après le point, les attributs d'annotation dans la
  parenthèse, lus sur les jetons. `FusedFloor` — le plancher fusionné :
  brancher un fournisseur ne retire ni les mots-clés ni les gabarits
  (`RaffinementsCompletionTest`).

- **Renommage lié des balises XML (C8)** : `XmlLinkedTags` — l'édition du nom
  d'une balise ouvrante (frappe ou retour arrière, caret au milieu ou en fin
  de nom) se reflète à la position alignée du nom fermant apparié, en un
  seul lot d'annulation. L'appariement est un empilement par nom : les
  homonymes imbriqués se respectent, les auto-fermantes ne comptent pas,
  les commentaires, CDATA et processing instructions se sautent ; la
  liaison est à sens unique (éditer la fermante ne touche jamais
  l'ouvrante) (`XmlLinkedTagsTest`).
- **La frappe fine Java dédiée (C8)** : `JavaSmartTyping` — deux gestes que le
  moteur générique ne connaît pas. Le découpage des chaînes à l'Entrée :
  le caret dans un littéral ferme la chaîne, pose la concaténation et la
  rouvre à la ligne un cran sous la sienne (`"abc⏎def"` devient
  `"abc" +⏎    "def"`) — blocs de texte, échappée pendante et chaîne non
  fermée refusent. Le cran des labels de cas : l'Entrée qui suit
  « case … : », « case … -> » ou « default: » gagne un cran — le
  corps du cas vit sous son étiquette ; le mot de tête décide, le
  ternaire et la ligne de commentaire ne trompent pas. Le geste dédié se
  pose sur le moteur générique plage par plage (multi-curseurs mêlés
  compris), la lecture des chaînes est textuelle — elle survit à l'arbre
  périmé d'une édition à peine frappée (`JavaSmartTypingTest`) ; câblé dans
  la démo par la langue du fichier — les citations d'un attribut XML ne
  se découpent jamais (`DemoIntelligenceTest`).

- **L'auto-import adossé au classpath (C8)** : `ClasspathIndex` porte
  l'index de l'hôte — nom simple vers noms qualifiés, ordre de préférence
  conservé, doublons retirés, noms mal formés refusés d'exception.
  `ClasspathImports.candidats` propose les noms qualifiés du nom simple
  sous le curseur, dans l'ordre de l'index ; l'import déjà présent ne
  se re-propose pas, les autres candidats du même nom restent proposés —
  le choix reste à l'utilisateur. L'insertion vit chez `ImportAuto` :
  une édition de position unique à la tête du bloc
  (`ClasspathImportsTest`) ; câblé dans l'atelier de la démo sur le
  classpath du terrain — le geste d'import suit le symbole au curseur.

- **La signature riche (C7)** : `SignatureAideModel` porte les surcharges —
  TOUTES les entrées candidates avec leur documentation. Le stepper navigue :
  `precedente` et `suivante` rendent le modèle voisin, le cycle revient —
  jamais bloqué. La fenêtre ±1 borne ce que la vue montre autour de
  l'active ; le peek sert la documentation de la signature active —
  l'appui long bascule, le panneau grandit. `SignatureHelpView` rend les
  voisines atténuées au palier méta, le stepper ‹ i/n › au palier micro,
  l'active garde ses trois paliers (frappé accent, actif gras, à venir
  atténué) ; le pas au toucher prévient l'hôte par le rappel. La
  conversion du protocole porte TOUTES les entrées
  (`ServeurLangage.enModele`) ; le coach sert le repli local sur
  réponse vide (`AtelierPopups`). Bancs : `SignatureAideModelTest`
  (stepper, fenêtre, peek, bornes), `PopupViewsTest` (le pas au toucher,
  le peek à l'appui long, une seule signature ne passe pas).

- **Les trous riches du gabarit (C7)** : le trou à choix
  `${genre|int,long|}` porte sa liste fermée — les doublons se retirent,
  le choix vide refuse d'exception, le premier rend par défaut, le choix
  se cycle (`Trou.choixSuivant` — le courant inconnu rend le premier, le
  trou libre rend le courant tel quel). Les macros de l'hôte se résolvent
  au rendu (`Gabarit.of(corps, macros)`) — `${FICHIER}` devient sa
  valeur SANS trou : elle n'attend pas la frappe ; l'inconnue
  de la table reste un trou libre, jamais d'invention. La syntaxe celeris
  reste `${NOM}`, les macros de l'ancien vivaient en MAJUSCULES
  (`$FILE$`, `$DATE$`, `$USER$`) — la table de l'hôte porte les valeurs.
  Les miroirs étaient livrés (M5, `Gabarits.miroirs`). Câblé dans la
  démo : le gabarit de méthode gagne la macro FICHIER (résolue du
  nom du fichier ouvert) et le trou à choix du genre du retour
  (`AtelierRefactorTest`).


- **La minimap (C9)** : `MinimapModel` porte la silhouette du document —
  chaque rangée de la carte charge son bouquet de lignes (la longueur de
  la plus longue, plafonnée à 120), le cran d'une ligne se calcule au
  prorata, la ligne d'un cran est la première de son bouquet — le saut
  retombe toujours sur une vraie ligne. `MinimapView` rend chaque rangée
  en un trait à sa charge, la fenêtre visible à l'encre d'accent :
  l'appui ET le glissement sautent, le doigt tient la carte ; aucune
  allocation par image, 200 crans au plus — le budget de rendu tient
  (ADR-006). Câblée dans la démo au bord droit, à gauche du pouce et de
  la bande d'erreurs (`MinimapModelTest`, `PopupSurfacesTest`).

- **Le défilement rapide (C9)** : `FastScrollerView` — le pouce au bord
  droit suit la fraction du défilement, sa hauteur la part visible :
  le glissement rend la fraction visée (le doigt porte le HAUT du
  pouce), le pouce GROSSIT sous le doigt — le retour du geste à la
  manière du kit de la plateforme ; tout tient : le pouce se
  tait. `EditorView` gagne la voie publique : `scrollFractionY`,
  `visibleFractionY`, `scrollToFractionY`, `premiereLigneVisible`,
  `lignesVisibles` — le corps vit chez `ViewportFractions`
  (l'`EditorView` tient son plafond ADR-002 : le corps d'`onDraw`
  vit chez `FrameAssembler.dessiner`, le montage des couches chez
  `ScrollFrame.assemble`, la fabrique IME chez
  `EditorInputConnection.creer`, la réinitialisation de scène chez
  `FrameAssembler.reinitialiserScene`), le défilement se borne à
  l'image suivante, l'avis de fenêtre y part (`ScrollFractionTest`,
  `PopupSurfacesTest`).

- **Les guides d’indentation (A-17)** : `IndentGuides`, couche de chrome —
  un trait vertical par cran d’indentation (`UNIT` = 4 colonnes), tracé à
  mi-chemin entre les colonnes de bord, de la première à la dernière rangée
  de chaque niveau ; les rangées contiguës d’un même niveau se joignent en
  une ligne continue. Le niveau se lit par rangée, la projection décide de
  la ligne — un pli replié couvre ses lignes, le guide le traverse au
  niveau le plus faible de son bouquet ; les blancs de tête se comptent en
  primitives (aucune allocation dans la boucle), la tabulation compte à son
  cran ; l’alpha discret (0x1A) vit dans le canal alpha de l’encre de
  base — le guide aligne, il ne lit pas. Tracés sous la sélection et le
  texte : la couche s’insère entre la gouttière et la sélection
  (`IndentGuidesTest` — le canvas du banc capture les lignes tracées :
  guides alignés aux niveaux, le guide traverse le pli, la tabulation
  ouvre son niveau, sans indentation rien ne se trace).

- **Le glisser-déposer (D-20)** : `GlisserDeposer`, collaborateur de la vue —
  le TEXTE déposé s’insère au point de chute : l’offset se résout par le
  viseur, l’insertion passe par la voie de transaction (ADR-003), une
  annulation la défait ; l’URI de FICHIER déposée va à l’HÔTE par le
  rappel (`setSurFichierDepose`) — la vue ne décide jamais ce qu’ouvrir
  (le scénario de la matrice : dépôt de fichier → ouverture) ; le texte
  a la précédence : un dépôt qui porte les deux insère le texte, l’uri
  suit le rappel ; le geste vide refuse — la vue n’accepte que ce
  qu’elle sait déposer ; la lecture seule refuse le texte. Le décodage
  du presse-papiers assemble les tranches TEXT et ramasse les uris des
  tranches porteuses d’uri. L’`EditorView` tient son plafond ADR-002 :
  le corps d’`onKeyDown` déménage chez `KeyActions.action` (`GlisserDeposerTest`
  — la vue vit dans la fenêtre d’une activité, les gestes passent par le
  vrai dispatch).

- **Les ligatures (D-14)** : `setLigatures` — « liga » ouvre les
  ligatures de la police (la police à ligatures posée par l’hôte livre
  ses glyphes liés), « -liga » les ferme ; toutes les peintures du
  texte suivent, fermées par défaut. `poserPolice(famille, police)` —
  la police libre vit sous son nom de famille dans la table de la
  règle : la mesure et le tracé la lisent ensemble (PIEGE-22), la
  famille active se redérive — métriques, viseur, cache. La
  démonstration embarque Fira Code 6.2 (SIL OFL 1.1, attribuée au
  `NOTICE`, somme SHA-256 épinglée) : l’interrupteur « Ligatures »
  de la feuille des réglages l’ouvre, la famille suit. L’`EditorView`
  tient son plafond ADR-002 : le montage de la gouttière déménage
  chez `GutterPainter.monter` (`LigaturesTest` — test de capture :
  fermées par défaut, la bascule ouvre puis referme, la police posée
  vit au tracé comme à la mesure, la famille active se redérive au
  repos).

- **Le volume de tests — première traîne (C13)** : six collaborateurs
  vivaient sans aucun banc, leurs bancs éponymes naissent (31 tests,
  C13 volume) — `EditorGutterTest` (le niveau d’eau croît et ne
  rétrécit jamais, la coulisse découle de la configuration seule,
  PIEGE-25), `ViewFontsTest` (le changement redérive métriques et
  viseur, la police posée sur la famille active redérive — la
  dormante attend, la table de la règle, PIEGE-22), `SceneDiagnosticsTest`
  (l’exécuteur injecté jamais détourné, les lectures de vérité),
  `FantomeEditeurTest` (la suggestion se dépose, Tab et l’appui
  partent à l’hôte, F-01), `CurseurClavierTest` (le clignotement au
  rythme de la plateforme — la vue vit dans la fenêtre d’une
  activité, la perte de focus arrête), `PopupPanelTest` (les
  respirations dérivent de la densité, la mesure les réserve des
  deux côtés). Volume courant : 2 028 méthodes `@Test` contre
  3 130 à l’audit — la traîne continue.

- **Le volume de tests — deuxième traîne (C13)** : trois
  collaborateurs purs de la géométrie de la vue gagnent leurs
  bancs (19 tests) — `CaretGeometryTest` (A-18 : sous
  coupures, la rangée, l’abscisse et l’ordonnée d’un curseur
  vivent sur SA tranche ; le miroir exact de la visée —
  PIEGE-06, la visée du quart gauche retombe sur sa colonne ;
  la rangée fantôme de la fin de ligne documentée),
  `GutterLanesTest` (la coulisse découle de la configuration
  seule, PIEGE-25 : la syntaxe réserve deux caractères, le
  maximum de la respiration et des deux caractères, chaque
  intégration partage la coulisse — le maximum jamais la
  somme), `RowLayoutTest` (la tranche vide est légale, la fin
  inversée refuse, ligne négative et débuts négatifs refusent,
  les listes nulles refusent — les segments et les plages
  vidés signalent les chemins rapides du peintre). Volume
  courant : 2 047 méthodes `@Test` contre 3 130 à l’audit —
  la traîne continue.

### Qualité — le rempart de langue et le plafond relevé (ADR-019)

- **Le rempart de langue tient de nouveau** : les identifiants
  français nouveaux de la vague revenaient à l’anglais — `FrappeJava`
  devient `JavaSmartTyping` (le nom dit sa famille : le moteur générique
  est `SmartTyping`), la rangée de la minimap devient `MapRow(slot,
  load)`, le montage du pipeline prend la couche (`PainterLayer`) au
  lieu de l’éditeur fantôme — la ligne de base redescend à l’identique
  (344 figés), la CI redevient verte sur les trois travaux.
- **Plafond relevé des bancs volumineux (ADR-019)** : 600 lignes par
  fichier restent la règle ; les bancs nommés — première nomination :
  `GutterPainterTest` (602 lignes, dix-neuf tests d’un seul
  collaborateur) — vivent sous un plafond relevé à 800, nominatif et
  consigné (`config/checkstyle/suppressions.xml`) ; le cliquet demeure,
  plus haut. Les portes Checkstyle des modules Android couvrent
  désormais les bancs (`src/test/java`) comme le source principal —
  mêmes plafonds que les modules purs ; les violations des bancs
  (imports inutilisés, import étoile, lignes trop longues, méthodes de
  banc au-delà de 50 lignes) sont corrigées au passage — les méthodes
  longues se découpent, jamais le plafond de méthode ne se relève.

### Publication — les modules Android vivent en artefacts Maven

- **Les trois bibliothèques Android se publient comme les modules purs**
  (décision du propriétaire du 2026-11-11, ADR-011) :
  `android-view`, `android-widgets` et `android-host` portent la
  convention de publication — AAR de la variante release, sources,
  Javadoc, POM, métadonnées Gradle, SBOM CycloneDX et sommes de
  contrôle SHA-256, aux mêmes coordonnées `jo.celeris:*` que les huit
  modules purs. **Onze artefacts publiés** au total ; la démo et la cli
  restent des applications, jamais publiées.
- **La variante release se déclare une fois** dans la convention des
  bibliothèques Android (`singleVariant("release")`, sources et
  Javadoc comprises) ; la convention de publication existante servait
  déjà la variante Android quand le composant `release` existe — elle
  entre enfin en service, le commentaire de report (M2/M3) disparaît.
- **La CI publie aussi sous SDK** : le travail Android de la CI vérifie
  la publication locale des onze artefacts (`publishToMavenLocal
  publicationChecksums`), comme le travail des modules purs — vérifié
  en mavenLocal : AAR, sources, Javadoc, POM et `.module` présents,
  POM résolvant les dépendances du cœur aux mêmes coordonnées.
- **Les documents suivent** : `USAGE.md` (§ 2 et § 3 — onze
  coordonnées, l'AAR d'abord, l'inclusion de source en voie de secours
  pour lire et adapter), `SECURITY.md` (périmètre des onze artefacts),
  ADR-011 (décision consignée), et la § 18 se corrige au passage — les
  chantiers C7 et C9 y figuraient encore « non démarrés » alors qu'ils
  sont livrés depuis le 2026-11-11.

### Documentation — un README par module, le racine honnête

- **Treize README de module** : chaque module porte désormais son
  README — rôle, nature, coordonnées, dépendances, bancs (chiffres
  relevés au 2026-11-11) et portes propres ; les modules purs citent
  leur manifeste d'API, les modules Android en sont exempts (ADR-009 ne
  couvre que les modules purs) et déclarent leur AAR publié depuis le
  2026-11-11 ; la cli et la démo se déclarent applications, jamais
  publiées.
- **Le README racine dit l'état réel** : 1.0.0 livrée le 2026-11-10 en
  publication locale, 1.1.0 en chantier, dix chantiers clos, un qui
  dort sur le signal du propriétaire (C5), trois bloqués sur un moyen
  extérieur (C1 secrets, C11 appareil, C14 décision) ; nouvelle
  section « Publication » (les onze artefacts), nouvelle section
  « Bancs et cliquets » (326 bancs, 2 048 méthodes `@Test` relevés au
  2026-11-11 — le volume n'est pas un but en soi, décision du
  propriétaire : les cliquets font foi) ; la table des modules relie
  chaque README et corrige les rôles (la gouttière vit dans
  `android-view`, le kit de popups et les ornements dans
  `android-widgets`) ; la documentation référence ADR-019 et la
  feuille de route restante.
- **L'index des ADR rattrape ADR-019** (plafond relevé des bancs
  volumineux, décision du propriétaire du 2026-11-11) — plus aucun ADR
  accepté absent de l'index.

### Feuille de route — C13 se clôt, les étapes restantes se consignent

- **C13 (deuxième vague) se clôt le 2026-11-11** : le périmètre est
  livré — refactorings (A-04, B-20 à B-23), historique local (A-14),
  guides d'indentation (A-17), ligatures (D-14), glisser-déposer
  (D-20), complétion postfixe (A-23), recherche projet (A-21), CLI
  complète (G-03). Sur le volume de tests, décision du propriétaire du
  2026-11-11 : la parité de volume avec l'audit (3 130 méthodes
  `@Test`) n'est pas un but — le relevé courant (2 048 méthodes
  `@Test`, 326 bancs, les treize modules compris) fait foi, les bancs
  continuent de naître au fil des chantiers sans quota ; les cliquets
  (couverture, mutation, plafonds, manifestes) restent les seuls
  juges.
- **`docs/feuille-de-route-restante.md` naît** (décision du
  propriétaire du 2026-11-11) : l'inventaire honnête de tout ce qui
  reste — publication distante des onze artefacts (C1, secrets du
  registre), clôture de la 1.1.0 et tag `v1.1.0`, langages suivants
  (C5, dort sur le signal du propriétaire), vérification sur vitre
  (C11, appareil de référence), archivage du dépôt de départ (C14,
  décision), entretien continu (dependabot, montées de version). Le
  volume de tests n'y figure pas comme étape. La feuille de route
  historique garde les chantiers C1 à C14, les jalons M0 à M9 et le
  journal des clôtures ; C13 y passe à terminé avec sa preuve au
  journal.

### Corrections — la chasse aux bugs de l'audit paie : trois critiques et sept majeurs, régressions comprises

- **Les trois défauts critiques prouvés tombent**, chacun avec son test
  de régression : le modificateur Kotlin orphelin en fin de fichier
  (l'accès hors bornes du dialecte — `KotlinArbreTest` garde
  `class A {\npublic`), la suppression du chunk ancre d'une ligne
  unique (le second plantage du parseur incrémental — la tranche de
  marche vidée rend un chunk en attente,
  `ArbreIncrementalTest` garde le scénario) et le tiret seul en fin de
  ligne JSON (le jeton débordait sa ligne — `JsonLexeurTest` garde la
  frappe en cours digérée).
- **Sept défauts majeurs au correctif isolé suivent** : la sonde de
  présence des paquets ferme son flux (`Packs`), la plage inversée
  d'un serveur généreux s'ordonne avant la transaction
  (`EditsEspaceLsp`), le motif vide de la recherche en ligne de
  commande se refuse au lieu de tourner sans fin (`Celeris`), le
  chemin Windows en attribut XML se ferme — l'antislash n'échappe rien
  en XML (`XmlLexeur`), le décommentage d'une ligne `//x` sans espace
  retire le préfixe réellement présent — la transaction ne lève plus
  sur un document légal (`Commenter`), le garde catastrophique rapporte
  le quantificateur non borné à tous les groupes ouverts — `((a+))+`
  ne passe plus par le sommet seul (`GardeCatastrophique`, aucune règle
  des grammaires livrées écartée, le seuil de conformité le prouve), et
  la fermeture d'un document purge ses diagnostics — une session longue
  n'accumule plus au `didClose` (`LspSession`, `RecepteurServeur`).
- **Les défauts majeurs restants** (amputation de paires de drapeaux
  au-delà de la fenêtre, garde de temps des regex tierces, identité de
  fusion du renommage, atomicité de la transaction de texte) exigent
  des bancs dédiés : ils restent consignés au rapport d'audit pour
  traitement ultérieur, jamais corrigés en aveugle.

### La démonstration vit sans serveur embarqué

- **Le repli de référence reprend la main** : plus aucune dépendance
  tierce ne vit dans le dépôt — la déclaration de l'hôte se vide, le
  serveur de référence (M7.5, §8) répond pour tous les langages ouverts,
  NOTICE, REFERENCES, résumés, audits, recette et matrice ne consignent
  plus qu'une dépendance tierce au runtime : la frontière protocolaire
  et ses sommes. La dernière règle d'exception du binaire retiré quitte
  `.gitignore`.
- **L'histoire du dépôt se recoud** : les mentions du tiers retiré
  s'effacent de toute la lignée des commits — l'identité, les
  coordonnées et les archives nommées ne s'y trouvent plus, le dépôt se
  relit sans eux de la première à la dernière ligne.

### Hygiène — la CI redevient verte

- **Quatre imports post-réflow partent** (`android-widgets`) : la
  découpe en sous-paquets avait laissé deux imports vers `Corps` —
  devenu interne au paquet `clike` — et deux imports `SyntaxIssue`
  inemployés ; compilation, Checkstyle et bancs de vue repassent
  (contrôles complets avec SDK, tests Robolectric).

## [1.0.0] — 2026-11-10

### M9 — durcissement et 1.0 : audit d'accessibilité, revue de sécurité, cliquet calibré, guide final, paquet jouet

- **Audit d'accessibilité complet (M9-a)** : les six décisions de
  l'ADR-008 passées en revue, DEUX défauts réels corrigés (cinq
  interrupteurs et un curseur de réglage sans nom, étiquetés par
  `labelFor`) ; l'auditeur automatique promis dès M3 vit enfin en CI
  (`scripts/check-accessibilite`, cinq règles, dix auto-tests,
  bloquant).
- **Revue de sécurité (M9-b)** : une faille fermée — la déclaration
  d'un serveur embarqué par un PAQUET ne monte plus : seul l'hôte
  décide de l'instanciation (`DeclaredServers`, rouge puis vert) ;
  `allowBackup` interdit au manifeste, exigé par test ; dix surfaces
  passées en revue (`docs/audit/REVUE-SECURITE-M9.md`).
- **Performance finale, cliquet resserré (M9-c)** : les bancs
  calibrés (calibrage consigné : date, machine, mesures, marge) —
  édition 0,5 ms par édition (mandat 1 ms, mesure 0,145), passe de
  remplacement 150 ms, annulation 100 ms ; les budgets produit ne
  bougent pas.
- **Documentation exhaustive (M9-d)** : `MIGRATION.md` final (table
  de correspondance API, reprise des thèmes et gabarits, extensions
  de langage, checklist de performances) ; le README rattrape l'état
  (treize modules, ADR 001-017, catégories A à H).
- **Démonstration complète (M9-e)** : le paquet jouet de
  l'utilisateur se joue à chaud depuis les réglages (H-04) — un
  langage ENTIER en données seules (Logo), copié au stockage, chargé
  sans recompiler ; la matrice passe la ligne à DÉMONTRÉ.
- **Publication 1.0.0** : artefacts à `jo.celeris:*:1.0.0`, SBOM et
  sommes de contrôle vérifiés.


### M7.6 (H-05) — les injections vivent, les grammaires du commerce chargent sans modification (2026-11-10)

- **Les injections TextMate s'appliquent (H-05)** : la clé de chaque
  injection porte son sélecteur de portées et sa priorité —
  `L:` devant la fermeture et le contexte, `R:` et sans
  préfixe derrière (l'hôte reste maître chez lui). Le lexeur
  évalue les sélecteurs contre la PILE courante à chaque position ;
  la règle injectée porte sa pile, empilée sur le contexte
  accueil. Les grammaires SANS injection gardent le chemin rapide
  d'avant. Le compteur « injections ignorées » disparaît du
  rapport : elles ne s'ignorent plus.
- **La forme catastrophique écarte SA règle, jamais le paquet** :
  le garde de l'étape 5 refusait le paquet entier — or les
  grammaires du commerce portent des formes que l'analyse textuelle
  refuse à tort (`(?x)` étendu de python, attributs HTML) et
  le critère du jalon exige leur chargement SANS MODIFICATION. La
  règle écartée se compte au rapport comme un motif invalide :
  elle ne colore rien, le budget d'exécution garde le reste, jamais
  un gel. Même justice pour les inclusions de référentiel
  inconnues (sautées, comptées — le shell du commerce s'en porte
  auteur) ; le banc des PAQUETS INTÉGRÉS exige leur absence,
  les paquets écrits ici restent stricts.
- **Le banc de conformité du commerce (H-05)** : les ONZE grammaires
  des extensions de VS Code (c, cpp, css, html, javascript, json,
  markdown, python, shell, typescript, yaml — MIT, épinglées,
  provenance au registre de portage et au NOTICE, hors artefacts
  publiés) chargent TELLES QUE PUBLIÉES, rejouent leur échantillon
  avec déterminisme, jetons à piles de portées, aucun incident de
  budget. L'acceptation se mesure et se seuille par grammaire — le
  cliquet contre la dérive d'une montée de version : json 100 %,
  python 98,8 %, html 98,7 %, shell 99,4 %, css 96,6 %, c 95,8 %,
  markdown 93,9 %, javascript 90,3 %, typescript 90,2 %, yaml
  87,5 %, cpp 71,5 % (les refus : garde catastrophique
  conservateur, comptés, jamais silencieux).
- **La preuve** : `InjectionsLexeurTest` (L: gagne les ex æquo,
  R: les cède, le sélecteur ne s'applique que sur sa pile,
  l'exclusion écarte) ; `GrammairesCommerceTest` (chargement,
  seuils, rejeu, déterminisme, injections html présentes) ;
  l'inclusion inconnue et la forme catastrophique se comptent au
  rapport dans `GrammairesTest` et `GrammairesTextMateTest` ; les
  paquets intégrés restent à 100 % d'acceptation et sans
  inclusion inconnue (`GrammairesConformiteTest`).

### M7.6 (H-07) — les thèmes se résolvent par portées : sélecteurs TextMate, import tokenColors, la couleur quitte la sorte (2026-11-10)

- **La pile de portées vit dans le jeton** : le lexeur de la voie
  des grammaires empile la racine du langage, les blocs ouverts
  (contenu, sinon construction) et la portée du jeton — internée
  une fois dans un arbre de préfixes (`ScopeStacks`), désignée
  par un ENTIER que le jeton porte. La profondeur et la table se
  bornent (une grammaire hostile y perd de la précision, jamais
  l'éditeur sa mémoire). `Token` gagne son champ de pile ;
  `Portees.de` disparaît : la sorte de jeton devient la
  catégorie DÉRIVÉE de la portée de tête (les consommateurs hérités
  — crochets, plis — ne changent pas), la COULEUR se résout depuis
  la pile.
- **Le thème par portées (`ScopeTheme`, core-syntax)** :
  sélecteurs au format TextMate — virgules, descendance, exclusion
  `-`, préfixe hiérarchique — scorés par profondeur couplée,
  nombre d'éléments et précision du dernier (`keyword.operator`
  l'emporte sur `keyword` sur la même portée). La résolution
  FUSIONNE les règles qui matchent — l'encre de la meilleure, les
  attributs (graisse, italique, souligné) cumulés — et se GARDE EN
  CACHE par identifiant de pile : le tracé ne manipule aucune
  chaîne et n'alloue rien par image (PIEGE-15). Les thèmes clair,
  sombre et contraste élevé sont écrits ici — mêmes teintes que les
  palettes par sorte (le banc WCAG de la vue s'y appuie), mécanisme
  de portées.
- **Le tracè suit (`TextPainter`)** : la plage qui porte sa
  pile se peint par le thème — pinceau dédié, encre et attributs
  posés à chaque tracé, aucun ne fuit vers la plage suivante
  (PIEGE-01), `PaintBrush` porte les trois attributs — ; la
  plage sans pile garde la palette par sorte (`EditorTheme`
  porte son `ScopeTheme`, `ColoredSpan` sa pile,
  `SyntaxRowSource` les relie).
- **L'importeur tokenColors de VS Code (`TokenColorsTheme`,
  core-language)** : nom, couleurs d'éditeur (`editor.background`,
  `editor.foreground`) et règles au sélecteur chaîne ou liste
  se lisent ; l'encre exige l'opacité, la première faute nomme sa
  position. La démonstration s'en sert : le thème « Atelier »
  (écrit ici, au format du commerce) rejoint la rotation des thèmes
  — son chrome vient du fichier, sa palette par sorte se DÉRIVE des
  résolutions du thème : une seule table de couleurs vit dans le
  fichier.
- **La preuve** : `ScopeStacksTest` (l'internement partage, la
  dérivation de la sorte, les bornes), `ScopeSelectorTest`
  (descendance, exclusion, préfixe, score), `ScopeThemeTest`
  (la meilleure encre, les attributs cumulés, le cache rend la même
  instance), `PilesPorteesTest` (le jeton porte sa pile de la
  racine à sa portée, l'écart vit dans son bloc, déterminisme),
  `TextPainterScopesTest` (la plage à pile se peint à l'encre
  du thème, les attributs ne fuient pas), `ScopeThemeContrastTest`
  (chaque encre des trois thèmes livrés contraste ≥ 4,5:1 sur son
  fond), `TokenColorsThemeTest` et `ImportedThemesTest`
  (l'import lit, résout, dérive la palette et contraste).

### M7.6 (H-13) — les serveurs de langage se déclarent en données : `servers.json`, l'éditeur n'en connaît aucun (2026-11-10)

- **La déclaration de serveur devient une donnée** : un
  `LanguageServerDeclaration` — transport (`embedded`, `stdio` ou
  `tcp`), classe du serveur, interface cliente, nom annoncé, options
  d'initialisation — lu du `servers.json` d'un paquet ou du fichier
  de l'hôte qui assemble son registre. Chaque transport exige ses
  champs, la première faute nomme le sien, un paquet ne déclare QUE
  son langage — et les SEIZE paquets intégrés n'en déclarent AUCUN :
  l'éditeur ne connaît pas les serveurs de ses hôtes, il porte la
  déclaration jusqu'à eux (règle du propriétaire : le module de
  démonstration n'est pas l'utilisateur final de l'éditeur).
- **La démonstration déclare, le montage exécute** : la ressource `serveurs/servers.json` du module démo porte les déclarations de l'hôte ; `DeclaredServers` lit la donnée, vérifie la DISPONIBILITÉ (la classe vit au chemin de classes) et monte par la désignation de la frontière — l'aiguillage codé en dur de `EditorActivity` disparaît, l'exemption `LangagesDemo` du contrôle d'enregistrement se retire (plus aucun littéral à exempter). Indisponible, le serveur déclaré cède la place au
  serveur de référence (M7.5, §8) — l'index répond vite et hors
  ligne, le serveur répond juste, la vue ne sait toujours pas qui a
  répondu (ADR-005).
- **La session générique des serveurs déclarés** :
  `DeclaredServerSession` porte la session déclarée — les CAPACITÉS NÉGOCIÉES à la
  poignée de main décident des demandes : le serveur qui n'annonce
  pas l'aide aux paramètres n'y est jamais sollicité, la complétion
  se sert de la négociation, plus jamais d'une constante. La poignée
  de main emporte les options d'initialisation de la déclaration,
  telles quelles (`LspSession.initialiser(options)`).
- **La preuve** : `LanguageServerDeclarationTest` (lecture entière et
  refus précis des trois transports, paquet qui déclare le langage
  d'un autre refusé, paquets intégrés sans serveur, plateforme
  assemble les déclarations du paquet ET de l'hôte) ;
  `DeclaredServersTest` (la déclaration de la démo décrit le serveur
  XML, le langage déclaré monte SON serveur, sans déclaration ni
  serveur indisponible le repli répond) ; les SEPT tests de parité et
  les HUIT de couverture rejoués INTACTS par le chemin de la
  déclaration — aucun nom de serveur dans le code de la démo.

### M7.6 (étape 8) — la couverture LSP nouvelle : treize méthodes derrière la frontière, le monteur embarqué les sert (2026-11-10)

- **Treize méthodes du protocole raccordées, chacune à une interface
  (H-12, ADR-015)** : `references`, `documentSymbol`,
  `documentHighlight`, `prepareRename`, `rename`, `codeAction`,
  `formatting`, `rangeFormatting`, `foldingRange`, `selectionRange`,
  `semanticTokens` (entier et plage) et `inlayHint` — la session de la
  frontière les émet au-dessus de l'ordonnanceur du domaine
  (interactives devant, travaux de fond derrière), les réponses
  redeviennent les types immuables de `core-lsp` : cinq types nouveaux
  (`SymboleLsp`, `PliLsp` fin EXCLUSIVE — la convention de l'arbre
  commun, `SurlignageLsp`, `OptionsFormatageLsp`,
  `PreparationRennomageLsp`) et `CapacitesServeur` porte ses quatre
  capacités nouvelles. La porte de session de la démonstration
  (`LanguageSession`) les déclare toutes : le serveur réel, le serveur
  de référence et le terrain muet y répondent — la vue ne voit que le
  domaine.
- **Le montage du serveur de référence suit la couverture** : le SPI
  `ServeurEmbarque.Documents` gagne ses replis (le service qui n'en
  sert pas rend ses vides du protocole, JAMAIS l'exception de la
  bibliothèque — `rangeFormatting`, `selectionRange` et
  `semanticTokensRange` entraient en défaut muet), et l'adaptateur rend
  la forme moderne `documentChanges` des éditions d'espace — la
  VERSION du document survit au aller-retour, les édits de gabarit
  voyagent en `SnippetTextEdit`.
- **La preuve en deux étages** : `ServeurDocumentsCouvertureTest` (le aller-retour complet au monteur — le domaine part du terrain, devient protocole sur le fil, redevient domaine à la session, les treize méthodes et les replis vides) ; le banc `BancSpecs`
  porte son scénario H-12 (plan, pli et formatage par le fil du
  serveur de référence). Les sept tests de parité restent INTACTS, le
  corpus de traces en sert SEPT — les traces d'indices et d'édition
  d'espace, qui attendaient leur correspondance depuis l'étape 7, la
  trouvent.
- **La scission des plafonds (ADR-002)** : le module de la frontière
  tient ses 600 lignes par fichier et sa complexité de classe à 80 —
  l'adaptateur du serveur de référence (`ServeurDocuments`), la maison
  des demandes de couverture (`RequetesCouverture`) et les
  correspondances (`CorrespondanceCouverture`, `CorrespondanceEspace`)
  naissent de la scission, l'API publique ne bouge pas d'une signature.
- **Ruptures pré-1.0 (ADR-009)** : `CapacitesServeur.de` gagne quatre
  paramètres, `ServeurEmbarque.Documents` gagne trois replis
  (`formatagePlage`, `plagesSelection`, `jetonsPlage`), `LanguageSession`
  de la démo gagne treize méthodes, `JetonSemantique`/`JetonsSemantiques`
  deviennent `SemanticTokenLsp`/`SemanticTokensLsp` (la règle de langue,
  la ligne de base de `core-lsp` descend à zéro) — manifestes
  régénérés.

### M7.6 (étape 7) — le client LSP passe par la bibliothèque derrière la frontière, le code maison disparaît, la démo réduite par R8 est prouvée (2026-11-10)

- **La frontière protocolaire `lsp-protocole` (ADR-015) porte désormais
  le protocole SEULE** : la parité était prononcée — les bancs de la démonstration passaient SANS MODIFICATION par la frontière —, le code maison est coupé : décodages JSON, cadrage
  RPC, `ClientLsp`, transports — 334 lignes d'API publique rendues au
  manifeste, la couverture du domaine se reconstruit par construction
  directe (`CapacitesServeurTest`, `TypesDomaineLspTest`).
- **Le corpus de traces du jalon lit par le Gson de la bibliothèque**
  (`CorpusTracesTest` derrière la frontière) : la conclusion de l'enquête
  Gson — les quatre traces sans alternative ambiguë se lisent
  directement par `MessageJsonHandler.getGson()` ; la définition se lit
  en `List<Location>` posée en `Either.forLeft`, l'`Either` de deux
  listes étant ambigu sans le résolveur de requêtes (« ambiguous Either
  type ») ; les traces indices et édition d'espace attendent la
  couverture nouvelle (H-12) — l'édit de gabarit `celeris/templateEdit`
  reste une forme nôtre que les types standard refusent.
- **Les règles R8 consommateur sont prouvées sur la démo réduite**
  (ADR-015) : `lsp-protocole/regles-conservation.pro` garde le fil
  réflexif (rouage JSON, proxys, TypeToken), `demo/regles-r8.pro`
  garde le serveur embarqué monté par désignation et son archive
  découpée au octet près. `assembleRelease` vert : 6,3 Mo et
  6 383 classes contre 10,3 Mo et 14 005 au débogage (−39 % /
  −54 %) ; le dex vérifié porte la bibliothèque (568 classes) et
  Gson (217, copie unique) à leurs noms d'origine, les 671 classes du
  serveur entières, les services ServiceLoader rendus ; cinq scénarios
  — poignée de main, complétion documentée, diagnostics, survol,
  édition d'espace — rejoués sur le binaire réduit (R8 classfile,
  mêmes règles). La réduction y révèle un défaut latent : le lexeur
  de grammaires journalisait par `System.Logger`, absente de la
  plateforme avant le niveau 33 — corrigé vers `java.util.logging`,
  présente des deux côtés.
- **Ruptures pré-1.0 (ADR-009)** : les fabriques JSON de `core-lsp`
  (`CapacitesServeur.de(ValeurJson)` et sa famille) disparaissent — les
  types du domaine se construisent par leurs fabriques typées, la
  lecture du fil vit derrière la frontière ; manifestes d'API
  régénérés.

### M7.6 (étape 6) — l'analyse structurelle niveaux 1 et 3 : plis et plan par les données, analyseurs écrits factorisés (2026-11-09)

- **Niveau 1 (H-08, ADR-016)** : les plis par indentation
  (`IndentationFolds` — réglage `offSide` de la configuration, lignes
  vides et de commentaire seul jamais clôturantes, tabulations au
  taquet) et le plan par règles (`outline.json` des paquets,
  `OutlineRule`/`RuleOutline` — la première règle qui matche nomme la
  ligne, l'entrée couvre sa région de pli) ; Python, YAML et SQL ont
  plis et plan sans une ligne de Java dédiée — la vue, le cli et la
  démo les servent par la plateforme (`DataStructureTest`,
  `DataStructureDemoTest`), deux fichiers d'exemple les rendent
  visibles à la démonstration (`dependances.yml`, `inventaire.sql`).
- **Niveau 3 (H-10, ADR-016)** : Java, Kotlin, KTS et Groovy deviennent
  des spécialisations d'au plus 150 lignes (134, 123 et 147) du socle
  `CLikeLexer`/`CLikeParser` — les douze classes par langage disparaissent,
  les instantanés des corpus figés AVANT le remaniement prouvent
  l'identité (états, jetons, arbre) : `ClikeSnapshotsTest`. La ligne de
  base de langue descend de 29 identifiants français.
- **Ruptures pré-1.0 (ADR-009)** : `LanguagePack` gagne ses règles de
  plan, `LanguageSupport` les expose, `core-syntax` gagne
  `IndentationFolds`, `OutlineRule`, `RuleOutline` et la fusion des
  plis de `TreeFolds` — manifestes d'API régénérés.

### M7.6 (étape 5) — le moteur de grammaires devient réellement TextMate (joni), la plateforme gagne l'option tree-sitter (2026-11-08)

- **Le moteur de la voie de grammaires charge le format TextMate RÉEL,
  telles que les extensions de VS Code les publient** (ADR-004 révisé,
  ADR-014) : captures en objets portant un nom (plus de captures en
  chaînes), inclusions du référentiel résolues en RÉFÉRENCES parcourues
  à la lexicographie (les cycles se traversent, plus d'aplatissement),
  `$self` et `$base` pointent la racine, les fermetures à RÉFÉRENCES de
  l'ouverture (`\1`, `\2` — ici-documents shell, clôtures de blocs
  Markdown) se substituent à chaque ouverture, échappées littéralement.
  Les DIX grammaires des paquets de langage sont réécrites au format
  réel. Les expressions passent toutes par joni (2.2.9 + jcodings
  1.0.64, MIT, sémantique Oniguruma) — première dépendance tierce du
  RUNTIME d'un artefact publié : catalogue, SBOM, section runtime
  du `NOTICE` avec sommes de contrôle SHA-256, `REFERENCES.md`
  (règle 5). Le transcodage UTF-8 par ligne (`Utf8Line` et sa table
  octets↔UTF-16, ASCII sans table) se teste sur l'accentué et l'emoji
  (ADR-003, ADR-014).
- **Le bornage exigé par le prompt tient, jamais un gel de
  l'interface** : le garde catastrophique REJETTE au chargement les
  quantificateurs non bornés imbriqués — la forme exponentielle
  classique (`(a+)+`, `(?:\w+\s*)*`) — quand la forme DÉLIMITÉE passe
  (`(?:\.\w+)+`, le point des noms qualifiés : chaque itération
  commence par un atome que la suite ne peut pas avaler, le retour
  arrière reste polynomial) ; le lexeur dépense un BUDGET d'essais
  par ligne — au dépassement, la fin de ligne garde la portée de son
  contexte, l'incident se journalise. Ce qui s'écarte se COMPTE au
  rapport de chargement (`RapportGrammaire`) : motifs refusés,
  règles `while` dégradées, sous-motifs de capture réduits au nom,
  injections ignorées, inclusions externes sans registre — jamais en
  silence.
- **Le banc de conformité devient un seuil bloquant** : chaque
  grammaire de paquet se rejoue sur son corpus (jetons produits,
  déterminisme vérifié, aucun incident de budget) et son taux
  d'acceptation doit rester 100 % — une montée de version de joni
  qui rejetterait un motif que les paquets emploient échoue la CI
  avant jamais d'atteindre l'utilisateur.
- **La plateforme gagne l'option tree-sitter, à côté de TextMate** (la
  demande du propriétaire : « en plus de TextMate je souhaite
  aussi ajouter l'option tree-sitter ») : le niveau
  `tree-sitter` rejoint `handwritten` et `grammar` dans la préférence
  syntaxique des paquets (`"syntax": {"preferred": "tree-sitter",
  "fallback": "grammar"}`), résolu par `LanguagePlatform` comme les
  autres, et l'enregistrement `TreeSitterLanguages` porte la case —
  VIDE à M7.6 (ADR-016 : l'arrière natif ne rentre pas au dépôt,
  les budgets mesurés ne tiennent pas). Un hôte qui embarquerait un
  jour l'arrière natif enregistre ses sources : la préférence se
  met à servir, le repli cesse de servir, aucun consommateur ne
  change ; sans arrière, la déclaration retombe sur son repli,
  jamais d'improvisation — le tout testé (`TreeSitterOptionTest`).
### M7.6 — le spike tree-sitter écarte le niveau 2 natif : les budgets ne tiennent pas (2026-11-08)

- **ADR-016 : tree-sitter (v0.27.2, MIT) écarté à M7.6 — ce n'est pas un échec,
  c'est la réponse chiffrée que le spike devait donner**. Liaison JNI minimale
  écrite ici (règle 7 : la liaison répandue de l'organisation interdite exclue),
  trois grammaires (json, python, javascript, MIT), corpus VALIDES à l'échelle du
  mandat : l'incrémental après un caractère mesure 9,6 à 44,5 ms de p50 (budget
  ADR-004 : 10 ms) — le plancher croît linéairement avec le document (≈ 6-10 ms/Mo,
  0,5 ms à 190 Ko) — et les arbres pèsent 26 à 31 fois le texte (budget : 3×).
  Tailles (hôte, élagué) : runtime 223 Ko, grammaires +8 à +464 Ko — la taille
  n'était pas le bloqueur. Les niveaux 1 (grammaire générique, plan par données)
  et 3 (analyseurs factorisés) portent le jalon ; la case du niveau 2 reste
  déclarée dans les paquets, révisable par ADR avec mesures sur appareil.

### M7.6 — le spike du client LSP prouve LSP4J 1.0.0 (2026-11-08)

- **ADR-015 : LSP4J 1.0.0 derrière la frontière `lsp-lsp4j`**. Le spike lance un serveur de langage par le `LSPLauncher` de la 1.0.0 et rejoue poignée de main, diagnostics poussés, complétion documentée, survol et extinction — tout passe (identique sous 0.24.0 et 0.23.1). L'arbre des dépendances tient en gson seul (intervalle [2.9.1, 3.0) : **copie unique** épinglée au catalogue). Côté Android : 728 Ko de dex, 1 636 méthodes, zéro référence interne (d8 + dexdump). Restent à l'intégration : règles R8 sur la démo réduite, appareil API 26 — avant toute suppression du code maison, comme le prompt l'exige.

### M7.6 — le spike du moteur d'expressions rationnelles tranche pour joni (2026-11-08)

- **ADR-014 : joni (2.2.9 + jcodings 1.0.64, MIT) entre comme moteur des
  grammaires TextMate**. Le spike (`spikes/regex/`, onze grammaires réelles des
  extensions de VS Code, 3 700 motifs) mesure : la traduction vers
  `java.util.regex` compile 91 % des motifs mais en **réécrit 122 en silence**
  (quantificateurs possessifs changés sans échec — divergence invisible,
  pire qu'un refus) et rejette 234 motifs sur le seul mode étendu (?x) ;
  joni compile 99 % des motifs du commerce avec la sémantique Oniguruma que
  le format suppose, à vitesse comparable (rapport 0,46× à 2,56× selon la
  grammaire). Côté Android : 615 Ko de dex, 546 méthodes, zéro référence à
  sun.misc.Unsafe ou jdk.internal (d8 + dexdump). Le bornage exigé par le
  prompt (jamais un gel de l'interface) reste à la charge du moteur :
  validation des paquets et budget par ligne.

### M7.6 (étape 1) — la règle des références externes change de visage (2026-11-08)

- **Les projets tiers se nomment librement, deux restent interdits** :
  le contrôle `check-external-refs` devient `check-forbidden-refs` (les
  cinq heuristiques génériques disparaissent — « comportement de », « port
  de », « mieux que », « équivalent de », « inspiré de » étaient du
  français courant), ses tests couvrent motif trouvé, motif absent,
  variable vide et motif invalide ; un nouveau contrôle de provenance,
  `check-dependency-provenance`, vérifie que catalogue de versions,
  métadonnées de vérification et URL de dépôts ne mènent à aucun artefact
  interdit (règle 7). Les désignations neutres `[EXT-n]` disparaissent —
  tree-sitter et LSP4J se nomment ; la parité se formule contre
  Android Studio en toutes lettres (matrice comprise) ; les ADR 004 et
  005 reçoivent leur révision du 2026-11-08 ; `docs/REFERENCES.md`
  consigne projets et normes cités, leur rôle, leur licence vérifiée en
  ligne et la date de vérification. Action du propriétaire exécutée : le
  secret de CI `CELERIS_FORBIDDEN_REFS` porte les motifs prescrits par le
  prompt (mise à jour par l'API le 2026-11-08, confirmée).

### Le CI ne tombe plus sur le téléchargement d'android-all (2026-11-08)

- **Le jar android-all de Robolectric arrive par résolution Gradle, plus
  par HTTP au démarrage du test** : le résolveur intégré de Robolectric
  téléchargeait son jar (199 Mo) sans reprise ni cache partagé — un
  bridage transitoire de Maven Central (429) faisait tomber TOUTE la
  suite de tests de vue au premier démarrage (initializationError sur
  chaque classe, constaté en CI et en local). La convention Android
  résout désormais le jar par Gradle (configuration `sdkRobolectric`,
  reprises configurées et cache) et la tâche `robolectricDeps` écrit la
  correspondance `robolectric-deps.properties` que Robolectric lit au
  démarrage : plus aucun réseau au moment du test. La version vit au
  catalogue (`robolectricAndroidAll`), épinglée sur robolectric.properties
  (sdk 35) et Robolectric 4.15.1.

### M8.6 — la barre au menu de débordement et la complétion raisonnable (2026-11-08)

- **La carte de complétion garde une taille raisonnable** (le
  redressement du propriétaire : « la largeur du popup de complétion
  trop petite, il n'a pas une taille raisonnable ») : le plancher
  `LARGEUR_MIN_LISTE_DP` (240 dp) s'ajoute au plafond du dépôt de
  départ (280 dp) — les libellés courts ne rétrécissent plus la
  fenêtre, la rangée deux lignes y respire, les libellés longs ne
  mangent toujours pas l'écran.
- **La barre de sélection porte son MENU DE DÉBORDEMENT, au design du
  kit** (la demande du propriétaire : « le toolbar de sélection avec le
  menu overflow même design ») : les actions principales — couper,
  copier, coller — tiennent les pastilles ; la PUCE DE DÉBORDEMENT
  (carré arrondi au rayon du kit, teinté à vingt pour cent, trois
  points à l'encre du texte, teinte de sélection à l'appui) ouvre le
  menu des actions repliées — la même carte que le menu d'intentions,
  ancrée sous la puce. La machine à états porte le régime : le menu
  n'ouvre pas sans sa barre, ne lui survit pas, cède à la frappe avec
  elle et se repositionne au défilement.
- **Les serveurs de langage vivent EN PROCESSUS sur des TUBES MÉMOIRE** : le client du cœur rejoint tout serveur monté par le transport à flux — le cadrage Content-Length circule pour de vrai. Pourquoi des tubes propres : ceux du JDK couplent la lecture à la vivacité du fil écrivain — les réponses du serveur partent de fils de piscine éphémères, et leur mort brisait la liaison (un fil de lecture tué silencieusement, des réponses en attente pour toujours). Les diagnostics POUSSÉS suivent — gouttière, bande d'erreurs et barre d'état. La fermeture ne lui envoie jamais `exit` : une implémentation réelle y éteint la machine virtuelle, fatale au processus hôte. Le remplacement en vol des frappes rapides (PIEGE-18) est couvert au banc de la frontière.
- **La documentation de complétion lit ses trois formes du protocole**
  (le protocole sert aussi du CONTENU BALISÉ `{kind, value}`, le lecteur n'en tirait rien) : `ElementCompletion`
  décode la chaîne nue, l'objet balisé et le tableau de contenus — la
  documentation arrive enfin du monde réel.
- **L'exemple XML porte une DTD interne documentée** : déclarations
  commentées, attributs requis — la matière du test à fond sur
  appareil, sans ressource externe ni réseau.

### M8.5 — redressement : le design des popups du dépôt de départ et le clavier du défilement (2026-11-08)

- **Le design des popups revient au kit du dépôt de départ, toutes
  surfaces comprises.** `PopupTokens` porte les jetons d'origine (rayons
  14/8/6 dp, respirations 12/8/6 dp, échelle typographique
  0,90/0,85/0,75/0,70 × la taille de texte de l'éditeur) ;
  `PopupPalette` dérive le VERRE du thème hôte (fond de l'éditeur à
  alpha 0xDB sombre / 0xE0 clair, liseré hairline blanc à 10 % / noir à
  8 %) et porte les encres sémantiques (atténué, teinte de sélection,
  accent, mot-clé, les trois gravités) ; `PopupPanel` redessine la
  carte (rayon 14 dp, liseré d'un pixel, clip arrondi, filets et bandes
  teintées). La complétion porte ses RANGÉES DEUX LIGNES — badge de
  genre en puce teintée à 20 % (glyphe à l'encre du genre, palette
  calibrée, assombrie sur thème clair), runs de match du préfixe à
  l'accent et au gras, identité qualifiée en atténué dessous, largeur
  plafonnée à 280 dp ; la documentation aligne sa bande signature
  teintée, ses libellés de genre en micro-typographie, ses lignes
  PARAM indentées et RETOUR en gras atténué ; la signature se rend en
  TROIS PALIERS (frappés à l'accent, actif à l'accent et au gras, à
  venir en atténué — la ligne devient sa propre barre de progression) ;
  l'info-bulle pose son marqueur de gravité (× erreur, ! avertissement)
  au-dessus d'un message à l'encre du texte ; le fil d'Ariane passe au
  palier méta atténué ; la bande d'erreurs redevient le TIRET de 4 dp
  au bord droit (10 dp tactiles) et colore ses marques à la gravité
  (le modèle porte désormais la sienne) ; la barre de sélection
  surligne sa pastille pressée de la teinte de sélection. La
  démonstration dérive la palette du thème d'éditeur courant — le
  verre suit la rotation des thèmes et la taille de texte de l'éditeur
  (le coach sert les candidats typés : genre de badge, signature en
  identité qualifiée, préfixe de match).
- **Le défilement au doigt n'ouvre plus le clavier** (le constat du
  propriétaire : « je ne peux pas défiler horizontal et vertical sans
  que le clavier ne devienne visible »). Le doigt posé n'arme ni IME ni
  curseur : le geste qui dépasse le seuil de tap (24 px) devient un
  défilement à un doigt sur les DEUX axes, borné par la géométrie de
  défilement ; le relâchement sans mouvement seul résout le tap
  (pilule, pli, marques, pose du curseur et ouverture du clavier) ;
  l'appui long et son dragage ne rouvrent pas le clavier au
  relâchement, l'armement long ne survit jamais à un nouvel appui.

### M7.5 — reprise : le branchement de l'intelligence (2026-11-07)

- Le SPI IntelligenceHub.Provider vit sur la vue : les moteurs purs
  répondent enfin aux événements réels — la frappe validée pose ses
  paires (A-05, A-08), l'Entrée porte l'indentation de l'arbre et
  cesse d'être muette (A-06), le retour arrière mange les paires
  vides (A-07), le collage multiligne s'aligne à l'accueil (A-09) ;
  sans fournisseur la frappe passe telle quelle, l'Entrée reste un
  saut nu. La complétion de la démonstration lit les mots du CODE par
  l'analyse publiée — jamais les chaînes (B-03) —, le coach ne garde
  que le repli. L'atelier de refactorisation porte gabarit (A-22),
  formatage (B-25), renommage des usages entiers (B-19) et import
  (B-27) au menu, chaque geste une transaction annulable. Le serveur
  de langage de référence vit EN PROCESSUS : l'aide aux paramètres
  (B-07) et le survol (B-09) arrivent par le fil du protocole,
  l'ordonnanceur annule et remplace (PIEGE-18). La démonstration
  vérifie ses scénarios : douze spécifications rejouées par le fil
  réel au banc de l'accueil, douze lignes de la matrice montent à
  « Démontré ». La recette s'enrichit des étapes 12 à 27.

### M7.5 — reprise : les dix popups (2026-11-07)

- Les dix popups se posent sur la couche : PopupController porte leurs
  régiments d'exclusion (la documentation de complétion n'existe pas
  sans sa liste, la barre de sélection cède à la frappe mais survit au
  défilement, la bande d'erreurs ne cède à rien), PopupAnchor borne
  chaque ancre à la couche, PopupPanel et TextRowsPopup partagent fond
  arrondi et rangées — complétion, documentation, signature,
  intentions, info-bulle, fil d'Ariane, bande d'erreurs et barre de
  sélection ont leurs vues, PIEGE-27 prouvé à chacune. L'éditeur
  expose l'ancre (`caretBounds`, `selectionBounds`) et prévient du
  changement de fenêtre (`setSurViewportChange` — un avis par état
  réel, jamais au clignotement) ; la session notifie chaque
  changement effectif de sélection (`SelectionListener`, toute source
  comprise). La démonstration branche les dix : PopupCoach décide (le
  filtrage par préfixe, la signature comptée à plat, la documentation
  du serveur factice), la barre de recherche coordonne son état — son
  champ garde le focus, la couche l'interdit.

### M7.5 — jalon de redressement (2026-11-07)

- Le zoom au pincement ne faisait rien : le facteur du détecteur est
  incrémental (rapport à l'événement précédent), la vue le lisait
  cumulé — le palier ne bougeait jamais. Le facteur cumulé se
  reconstitue depuis l'écartement d'engagement, la taille marche
  l'échelle par paliers avec bande d'hystérésis de 15 % de l'intervalle
  (PIEGE-24) ; le point sous le doigt tient (gouttière lue avant le
  palier, posée après) ; le changement de police re-mesure un document
  jamais édité (PIEGE-28) ; Ctrl + plus/moins/zéro et la molette Ctrl
  marchent d'un palier, l'écouteur onFontSizeChanged prévient l'hôte.
- La largeur de gouttière n'était pas stable : elle se calculait sur
  les rangées visuelles, bouclait avec le budget de colonnes, n'avait
  ni plancher ni hystérésis, s'élargissait à l'arrivée des marques et
  vivait en pixels bruts. GutterLayout calcule sur les lignes du
  document, plancher de trois chiffres, marges en dp converties une
  fois, coulisse de configuration (GutterLanes) réservée dès
  l'attachement du langage, niveau d'eau monotone (PIEGE-25).
- La ligne en cours de frappe scintillait : la ligne touchée perdait
  toute sa couleur jusqu'à la publication, et la publication rejetait
  toutes les mesures. TokenCatchUp sert la ligne touchée par ses
  jetons publiés recalés sur l'édition en attente (l'inséré hérite du
  jeton de gauche, celui du point d'insertion s'étend), relit la ligne
  depuis l'état publié quand la structure change ; la publication
  re-spanne le cache en place, la mesure survit (PIEGE-26, PIEGE-20
  amendé).
- La couche porte-popups s'installe : PopupController (machine à états
  pure — exclusions, cohabitations, fermetures à la frappe et au
  défilement) et PopupLayer (superposition dans la même fenêtre, sans
  jeton de fenêtre) ; aucun popup ne prend jamais le focus d'entrée de
  l'éditeur (PIEGE-27). Les dix popups restent à livrer.
- Le banc de test de vue (ADR-013) : Robolectric 4.15.1 sur JVM (rendu
  natif), EditorTestRig (exécuteur d'analyse manuel, canvas
  enregistreur, gestes multi-pointeurs), travail CI view-tests à
  chaque commit.
- La vérité des tableaux de bord : cadre de statut à cinq niveaux,
  audit mécanique de la matrice (63 « livrés » affirmés ; 39 Câblé,
  54 Noyau, 17 Non démarré, 0 Démontré prouvés), cinq contrôles
  bloquants dans check (matrice, pièges, démo, rempart de langue —
  408 identifiants français figés —, dates contre l'historique git).
  RemapLignes se retire (remplacé par TokenCatchUp) ;
  MarquesGouttiere.coulisse(coulisseSyntaxe, charWidth) devient
  coulisses(syntaxeActivee) de configuration.

### Corrigé

- Les mots colorés s'éparpillaient, séparés d'un grand espace dès que la
  coloration s'affichait : les pinceaux par sorte de jeton ne recevaient
  jamais la police de l'image — seuls les tracés non colorés la posaient
  (PIEGE-22) — chaque plage se traçait à la police par défaut de la
  plateforme pendant que l'avance se calculait à la police de l'éditeur ;
  chaque pinceau par sorte reçoit désormais la police de l'image avant
  tout tracé, la règle qui mesure reste la police qui trace.
- La coloration disparaissait puis revenait à chaque caractère tapé, la
  suppression comprise : l'image qui affiche la frappe courait plus vite
  que la publication d'analyse, se traçait sans couleur, et rien ne la
  ramenait avant l'édition suivante — la source se réhabille à chaque
  publication (le cache de rangées suit), et pendant le vol, la
  publication périmée rattrape par remappage : les lignes épargnées par
  l'édition gardent leurs couleurs (contenu démontré identique,
  `RemapLignes`), la seule ligne touchée attend son encre le temps du
  vol (PIEGE-20 : jamais une ligne au contenu non prouvé identique ne
  se colore, une rafale au-delà du plafond retombe sans couleur).
- Le retour à la ligne souple fermait l'application à l'activation : la
  fenêtre de rangées visibles n'était jamais bornée au contenu, et les
  rangées sous la dernière ligne projetaient la dernière ligne sur des
  tranches croissantes — hors document sous coupure (hors bornes à
  l'image), répétée sous la fenêtre sans coupure (le contenu
  « éparpillé ») ; l'état d'image borne désormais sa fenêtre au contenu
  (ScrollGeometry.borneesAuContenu), test de reproduction sur les
  contenus et langages réels de la démonstration, plis compris.
- Le texte de l'éditeur se traçait à la police par défaut de la
  plateforme pendant que les positions se calculaient à la police de
  l'éditeur : colonnes dérivées, plages décalées, glyphe hors métrique
  ; chaque couche repose maintenant la police de l'image sur son
  pinceau (TextBrush.setFont, PIEGE-22) — la règle qui mesure est la
  police qui trace.
- Les tailles de police se lisaient en pixels bruts — minuscules sur
  écran dense : elles vivent en sp, converties à la densité de
  l'appareil (PIEGE-23) ; le pincement et ses paliers suivent la
  police logique.
- Le clavier logiciel n'apparaissait jamais : l'appui qui prend le
  focus dévoile l'IME explicitement — une vue focusable au toucher ne
  l'ouvre pas d'elle-même.
- Le curseur d'écriture n'était jamais tracé : une barre par plage
  vide — chaque curseur d'une sélection multiple trace le sien (A-01)
  — pleine hauteur de rangée, clignotante tant que la vue tient le
  focus ; les poignées de sélection s'élargissent à la police.

- La démonstration s'ouvrait en arrêt forcé sur l'appareil : les
  barres d'application de l'accueil et des réglages ne portaient ni
  `layout_width` ni `layout_height` — l'inflation mourait au premier
  écran, et ni la compilation ni l'assemblage ne le disaient ; un
  test de cohérence des mises en page (chaque vue doit porter ses
  dimensions) ferme la porte.
- L'accueil de la démonstration affichait une liste vide : le
  catalogue des cartes n'était jamais soumis à l'adaptateur — les
  fichiers d'exemple n'étaient pas ouvrables depuis l'accueil.
- L'APK de la démonstration porte désormais un numéro de version
  (`versionCode` 1, `versionName` lu au catalogue) : le manifeste
  assemblé en était dépourvu, et certains installateurs refusent un
  paquet sans version.

### Ajouté

- Tranche 6 (M8) — le consentement et la clôture (F-10) : le garde du
  contexte consulte le consentement à chaque départ — sans lui, la
  requête reste au sol, fantôme et prédiction compris, le journal du
  faux modèle en témoigne ; l'interrupteur des réglages de la
  démonstration reste coupé par défaut, l'atelier IA refuse poliment
  sans lui. La neutralité est structurelle : le manifeste de la
  démonstration ne déclare aucune permission — pas même INTERNET — et
  le test CI DemoNeutreTest le vérifie ; le scénario d'exclusions y
  passe (le chemin exclu ne part jamais, même consentant). Le budget
  de latence d'affichage du fantôme se consigne : mille tracés au
  minimum de trois tours, chaque tracé sous le budget d'une image à
  soixante hertz. La catégorie F complète entre au catalogue de la
  démonstration ; aucun paquet celeris n'embarque de télémétrie,
  l'option reste à l'hôte, documentée au README.
- Tranche 5 (M8) — le rendu du fantôme et le menu d'intentions
  (F-01 interface, F-02, F-03) : la couche FantomePainter dessine la
  suggestion au chemin des indices en ligne — une cellule d'encre
  pleine par caractère sur la ligne de base du code (PIEGE-11 couvert,
  registre à jour), encre fanée, première ligne à l'ancre et suivantes
  au bord gauche du texte ; l'acceptation se prend au clavier (Tab,
  remappable) et au geste (appui sur le fantôme), GesteFantome porte
  le mot, la ligne, le bloc et le refus ; PiloteFantome, pur et testé,
  applique à l'ancre décalée et ré-ancre le reste ; les actions IA
  (suggestion, prédiction) vivent au menu de l'atelier au même titre
  que les intentions classiques, la prédiction s'applique
  explicitement — transaction annulable, refus qui se lit.
- Tranche 4 (M8) — les exclusions de contexte (F-07) : le fichier
  d'exclusion du dépôt déclare les chemins jamais envoyés à un modèle
  (chemins exacts, dossiers, jokers — une règle sans barre vaut pour
  tout suffixe de composante) ; le garde du contexte se glisse entre
  la session et le modèle — le chemin exclu ne produit aucun départ,
  le journal du faux modèle en témoigne ; le fichier d'instructions
  d'agent se sert par la voie des outils (outil celeris.instructions,
  sixième du catalogue MCP), jamais inventé.
- Tranche 3 (M8) — l'API d'outils pour agents et sa surface MCP
  (F-05, F-06) : lire des plages et l'instantané — la réponse porte la
  révision servie —, lister les symboles et les diagnostics des
  sources injectées par l'hôte ; appliquer une transaction avec
  détection de conflit — l'instantané de référence doit correspondre,
  refus explicite au message lisible (« relire, recalculer,
  recommencer »), jamais de fusion silencieuse — par la voie de
  transaction ordinaire, annulable d'un geste ; les éditions multiples
  vivent en coordonnées de référence, ordonnées et disjointes. Le
  connecteur MCP expose les mêmes outils sous leurs noms de serveur
  (catalogue, réponses JSON), le transport reste à l'hôte.
- Tranche 2 (M8) — la revue des modifications d'agent (F-04) : les
  propositions d'agent arrivent étiquetées (auteur, intention) et datées
  de leur instantané de référence, éditions ordonnées et disjointes
  validées dès la construction ; la revue découpe en blocs contigus,
  décide bloc par bloc — le refus réaligne les suivants, les plages de
  référence ne bougent jamais —, les points de contrôle reviennent en
  arrière, la lecture en ligne rend les retraits et insertions par bloc
  (« - » et « + ») ; l'application se prend en une transaction
  annulable et refuse explicitement le document qui a bougé (jamais de
  fusion silencieuse). La démonstration révise son agent factice bloc
  par bloc, retour au point de contrôle compris.
- Tranche 1 (M8) — les contrats IA (ADR-012) : le texte fantôme
  (F-01, noyau — suggestion multiligne datée par révision, jamais
  ré-ancrée ; acceptation par mot, par ligne ou en bloc, le reste
  survit ancré pareil ; refus à la frappe suivante) ; la prédiction
  d'édition (F-02 — le modèle prédit une opération, l'application
  explicite passe par la voie de transaction, donc annulable, jamais
  silencieuse : refus de révision, de protection, d'éditions qui ne
  collent pas au document) ; les faux modèles de référence (F-08 —
  local synchrone, distant par exécuteur injecté, même contrat,
  journal des requêtes) ; l'annulation au jeton et la latence non
  bloquante (F-09 — la frappe suivante annule le vol, mille demandes
  au modèle aveugle mesurées au minimum de trois tours). La mutation
  s'étend à core-ai (86,4 %, seuil 80).
- Tranche 9 (M7) — les trois surfaces d'interface restantes :
  l'aperçu rapide d'une définition (D-11 — le plan du fichier sert
  d'index, l'entrée la plus intime contenant le curseur se survole,
  l'extrait reste borné à douze lignes) ; la vue de différences
  (D-12 — DiffBlocs découpe par la plus longue sous-suite commune,
  lectures en ligne et côte à côte, RevueDiff accepte et refuse par
  bloc avec réalignement et points de contrôle) ; les grands écrans
  (D-19 — ClasseLargeur suit les seuils des classes de taille, la
  gouttière resserre ou respire, le pliable déplié change de classe
  d'un geste). La démonstration relit son diff bloc par bloc et
  applique la fusion en une transaction annulable.
- Tranche 8 (M7) — les pastilles de couleur et les repères `@Preview`
  : les quatre formes de littéraux Android se résolvent (C-05 — la
  forme courte étend ses composantes, le carré se peint dans la
  respiration droite de la gouttière à la valeur résolue) ; chaque
  annotation `@Preview` ancre son losange dans la coulisse (C-11 — le
  nom du composable se lit sur place ou dessous, le clic délègue le
  rendu à l'hôte). couleurs.xml et Apercus.kt entrent au catalogue de
  la démonstration.
- Tranche 7 (M7) — les langages P1 restants par la voie de grammaires
  (C-15) : SQL (mots insensibles à la casse, blocs repliés), C
  (préprocesseur, types natifs, corps repliés) et C++ (classes, espaces
  de noms, `::`), budgets de ré-analyse tenus ; les règles R8/ProGuard
  (C-09) entrent avec leur grammaire et leur garde-fou — règles
  inconnues, accolades orphelines, membres sans point-virgule — sans
  faux positif sur le corpus sain.
- Tranche 6 (M7) — l'adaptateur de stockage et le lint importé :
  l'adaptateur de production du système de fichiers virtuel (E-01 —
  AdaptateurStockageDocuments sur la porte ArbreDocuments,
  ArbreDocumentsSaf seul à toucher au framework, parents implicites à
  l'écriture, même contrat que l'implémentation de référence) ; la
  sortie d'Android Lint se convertit en diagnostics aux lignes exactes
  (C-12 — AnalyseurLint), les lignes porteuses se peignent dans la
  gouttière fondues aux erreurs de la syntaxe, les correctifs
  mécaniques (ellipse, cadratin) s'appliquent en transactions
  annulables — AtelierLint et FauxLint servent la démonstration.
- La bascule du retour à la ligne souple entre dans la barre de
  l'atelier — icône directe, état dans le libellé, persistance
  partagée avec les réglages ; plus besoin de quitter l'éditeur pour
  l'essayer.
- Le routage tactile (TouchesEditeur) et le curseur avec son clavier
  (CurseurClavier) quittent la vue pour leurs collaborateurs — même
  famille qu'AccessibilityHost ; la vue reste sous le plafond de
  600 lignes.
- L'interface de la démonstration devient application : vrais layouts
  Material 3 (accueil en cartes à monogrammes de langage, atelier
  d'édition à barre d'application et menu, réglages décrits), identité
  sombre coordonnée à la palette de l'éditeur, marque en icône
  adaptative, bord à bord unifié clavier compris ; CarteFichier pur
  et testé porte les cartes.
- Tranche 5 (M7) — la peinture de gouttière des intégrations : barre
  de modification VCS au bord gauche de la coulisse (D-03, modifiée
  et ajoutée distinguées), point d'arrêt au centre (D-04, carré plein
  posé, creux désactivé), repère d'exécution par-dessus (D-05, flèche
  de ligne courante, chevrons de pile) ; la coulisse s'élargit de
  deux caractères, la fusion vit dans GutterMarks.avec, MarqueLigne /
  EtatArret / MarquesIntegration purs et testés ; la démonstration
  devient terrain de débogage — clic des numéros pour poser et
  retirer les arrêts, bouton de débogage (pause simulée au premier
  arrêt actif avec pile de deux cadres, reprise), diff ligne à ligne
  vivant face au contenu d'origine ; EtatLignesVcs.marques() énumère
  les lignes marquées.
- Tranche 4 (M7) — le bloc XML, manifeste et catalogues : XmlValidation
  (C-03 — fermeture étrangère à sa plage exacte, traversés d'ancêtre
  signalés à leur ouverture, ouverts restants à la fin) ; SchemaAndroid
  et CompletionsXml (C-02 — schéma embarqué, le contexte du curseur
  décide : balise, attribut, valeur ; unités dp/sp, préfixes
  @string/) ; ReferencesRessources (C-04 — définition par balise de
  valeurs ou fichier propre, usages à l'offset exact) ;
  CompletionsManifeste (C-06 — attributs des balises et permissions
  par préfixe) ; CatalogueVersions (C-08 — sections lues, accès
  suggérés, tirets en points, bundles dans leur espace de noms) ;
  manifestes d'API régénérés (soixante-dix-huit ajouts, aucun
  retrait).
- Tranche 3 (M7) — l'éditeur en ligne de commande : `celeris analyse
  <fichier>` (langage détecté par extension — dédiés et voie de
  grammaires —, lignes visibles, caractères, plan par TreeOutline) et
  `celeris recherche <fichier> <motif>` (occurrences aux lignes et
  colonnes exactes) ; codes de sortie distincts (0, 1 illisible, 2
  usage) — le « editor-cli exécuté en CI » de la feuille de route ;
  la bannière sans argument reste la fumée M1.
- Tranche 2 (M7) — les contrats SPI de l'hôte (E-01 à E-06) avec
  implémentations de référence déterministes testées sans appareil
  (36 tests) : système de fichiers virtuel (chemins validés contre la
  traversée à la construction), VCS (marques par ligne, blame,
  historique), débogueur (arrêts avec état désactivé, ligne courante,
  survol borné), exécution (lancements signalés, états visibles),
  analyseur de problèmes de build (plages exactes), analyseur de
  piles (fichier:ligne exact).
- Tranche 1 (M7) — la démonstration devient terrain d'essai : écran
  d'éditeur complet (vue Canvas, barre de recherche vivante, panneau
  de plan cliquable, barre d'état, zoom de police, thèmes cyclés,
  annuler/rétablir), huit fichiers d'exemple — un par langage P0
  (Kotlin, Java, XML, KTS, TOML, JSON, Markdown, Python) —, les
  réglages en direct câblés au moteur (numéros masquables, retour
  souple, thème) ; NavigationCommands (pose de curseur et sélection
  de plage, purs et testés), HitTest.offsetAt extrait de la vue,
  GestesFabrique (EditorView reste sous le plafond 600 lignes).
- Clôture M6 — catégorie B P0 couverte (B-03, B-04, B-05, B-07,
  B-09, B-10, B-19, B-25), critères testés sur le corpus Android
  embarqué (index, usages, complétion, renommage, génération,
  formatage, éditeurconfig), traces de contrat en corpus (la forme
  celeris des édits de gabarit y vit), dix-sept scénarios au
  catalogue — soixante au total ; 1169 tests à la passe de clôture
  (179 neufs du jalon), mutation 85.2 % sur 1807
  mutations, publication locale et APK assemblés.
- Tranche 8 (M6) — les requêtes interactives restantes : survol B-09
  (résolution à la demande, deux formes bornées), définition B-10
  (lieux du serveur, le retour par l'historique M5-t4), références
  B-11 (le raffinement serveur, contexte à déclarations comprises),
  aide aux paramètres B-07 (signatures décodées, paramètre courant
  BORNÉ à l'arité, la plage soulignée cherchée entre parenthèses —
  le nom de méthode jamais confondu —, placement chez PopupPlacer),
  indices en ligne B-08 (partie visible, priorité de fond devancée
  par l'interactif, parties cliquables vers leur cible) ; 21 tests
  neufs.
- Tranche 7 (M6) — formatage B-25 (le retrait se règle sur les
  niveaux de l'arbre, idempotent par construction — Java, Kotlin,
  XML, tabulations comprises), respect de .editorconfig B-26
  (sections par extension, la plus spécifique gagne, racine à
  défauts, fins de ligne converties, clés inconnues ignorées),
  génération de code B-24 (constructeur, accesseurs aux
  conventions — booléen en is —, equals/hashCode/toString, position
  d'insertion avant l'accolade fermante) ; 19 tests neufs.
- Tranche 6 (M6) — les éditions d'espace de travail et le bloqué
  historique débloqué : modèle à deux sortes (éditions de document
  et édits de gabarit celeris, la forme que la liaison tierce 1.0.0
  du dépôt de départ imposait sans pouvoir être absorbée),
  transposition en transaction annulable à ordre descendant,
  renommage multi-fichiers B-19 sur l'index hors ligne aux conflits
  signalés (ambiguïté, collision), correctifs B-17 par transaction,
  squelettes de méthode B-06 aux trous tabulables, composition
  démontrée de bout en bout chez le consommateur (ADR-002 : les
  modules purs restent latéraux) ; 16 tests neufs.
- Tranche 5 (M6) — complétion de base B-03 (les mots du fichier au
  préfixe près, hors chaînes et commentaires, le mot tapé jamais
  candidat de lui-même), import automatique B-05 (en tête du bloc
  par édition de position unique, jamais re-inséré), optimisation
  des imports B-27 (utilisés ou non signalés — hors lignes
  d'import, chaînes et commentaires — jamais supprimés en silence) ;
  19 tests neufs.
- Tranche 4 (M6) — l'index de symboles hors ligne (ADR-005) : le
  plan du même arbre que la coloration alimente l'index, remplacé
  fichier par fichier ; usages B-11 en occurrences entières à casse
  exacte — jamais dans les chaînes ni les commentaires, les jetons
  décident — ; recherche floue B-13 (identique, préfixe, début de
  mot, sous-chaîne, sous-suite) ordonnée par score ; budget mémoire
  borné et saturation signée ; persistance compacte identique ;
  16 tests neufs.
- Tranche 3 (M6) — l'ordonnancement des requêtes LSP : fusion des
  demandes identiques (une requête sur le fil, deux appelants
  servis), annulation au remplacement ($/cancelRequest, la réponse
  tardive tombe sans casse), priorités (l'interactif devance le
  fond du même document, jamais l'inverse), resynchronisation (la
  réponse périmée par un changement de version échoue, l'appelant
  re-émet) ; complétion typée B-04 par la clé du document, la
  coupure du serveur honorée (PIEGE-17) ; transport à flux —
  cadrage Content-Length sur tubes et prises, fil de lecture dédié ;
  10 tests neufs.
- Tranche 2 (M6) — le client LSP : poignée de main initialize/
  initialized aux capacités analysées, documents versionnés à
  synchronisation complète, diagnostics poussés décodés et stockés,
  jetons sémantiques TIRÉS par requête — un flux indépendant des
  diagnostics (PIEGE-18 couvert : le tir marche sans aucun diagnostic,
  la tempête n'y change rien) —, transposition des positions UTF-16
  aux bornes clampées, fil de travail propre et fermeture shutdown
  puis exit ; 18 tests neufs.
- Tranche 1 (M6) — le socle du client LSP (ADR-005, zéro dépendance
  tierce) : analyseur et sérialiseur JSON stricts (RFC 8259),
  enveloppes JSON-RPC 2.0 aux codes du protocole compris (annulations
  serveur distinguées), transport injectable avec cadrage
  Content-Length à réassemblage par morceaux et paire en processus
  à livraison synchrone ; liste de complétion (B-04) à la coupure du
  serveur honorée — jamais réordonnée ni complétée localement
  (PIEGE-17 couvert) —, jetons sémantiques (B-02) décodés du delta
  en positions absolues, modèle de documentation de complétion
  (B-09) aux blocs typés distincts (PIEGE-16 couvert) ; 45 tests
  neufs (39 core-lsp, 6 android-widgets).
- Clôture M5 — les critères de sortie du jalon sont tous couverts :
  catégorie A P0 (A-05, A-06, A-11, A-20 et sa barre) et
  D-08/D-09/D-10, chaque ligne de matrice M5 porte son scénario au
  catalogue — quarante-trois scénarios ; 146 tests neufs (990
  à la passe de clôture), mutation 85,2 % (seuil 80),
  publication locale et APK de démonstration assemblés ; le
  catalogue restructuré en sous-listes sous le plafond Checkstyle.
- Tranche 6 (M5) — lignes collantes, caractères invisibles, stylet
  et plugins isolés (A-16, A-19, D-18, G-04) : StickyLines colle
  les en-têtes englobants du même arbre, budget d'image borné ;
  InvisibleChars matérialise finale, tabulation et fine — toujours —,
  interne en mode « tout », fin de ligne sur demande ;
  PointerGestures zoome à la molette avec Ctrl (borné 0,5–3,0),
  info-bulle après station ; PluginHost isole les extensions —
  celle qui lève est journalisée, les suivantes courent ; 18
  tests neufs.
- Tranche 5 (M5) — bande d'erreurs, fil d'Ariane et popups unifiées
  (D-08, D-09, D-10) : ErrorBandModel borne la densité des
  diagnostics à un cran de bande et le clic saute au diagnostic ;
  BreadcrumbModel tire le chemin du symbole du même arbre et tronque
  par la largeur, ellipse comprise ; PopupPlacer coordonne
  complétion, documentation et signature autour du curseur — jamais
  superposées, jamais hors écran ; 15 tests neufs.
- Tranche 4 (M5) — barre de recherche, historique et signets (A-20
  facette interface, B-14, B-15/D-06) : SearchBarModel porte
  l'état de la barre au-dessus du moteur M2 — options, occurrences,
  navigation circulaire, ouverture depuis le curseur ;
  NavigationHistory — pile bornée, retour et avance, la fermeture de
  fichier vide ; BookmarkModel — sauts circulaires, persistance
  par projection des offsets sur la voie unique ; 19 tests
  neufs.
- Tranche 3 (M5) — sélection syntaxique et colonne (A-02, A-03) :
  TreeSelection expose la chaîne des étendues englobantes — mot,
  instruction, bloc, type, document — étendre monte d'un cran, réduire
  descend ; ColumnSelection transforme le glissement rectangulaire
  en une plage par rangée aux bornes visuelles exactes, tranche du
  retour à la ligne souple comprise ; 11 tests neufs (6 syntaxe,
  5 vue).
- Tranche 2 (M5) — actions de lignes, commentaires, tri et gabarits
  (A-10 à A-12, A-22) : LineActions opère le bloc de lignes
  touchées (déplacer, dupliquer — les curseurs descendent dans la
  copie —, joindre, supprimer, multi-sélection par blocs) ;
  Commenter bascule selon le langage (famille C « // », dièse,
  bloc XML, JSON masqué) tout ou rien, la ligne vide ne compte
  pas ; LineSorter trie naturellement (« item2 » avant
  « item10 »), casse au choix ; les gabarits dynamiques portent
  leurs trous dans l'ordre de tabulation et leurs variables liées —
  les miroirs propagent l'édition aux occurrences sœurs au même
  décalage relatif ; 32 tests neufs.
- Tranche 1 (M5) — la frappe intelligente par plans (A-05 à A-09) : le
  nouveau moteur de décision de core-intelligence rend des plans
  immuables (transaction + sélection d'après), la session les applique
  par la voie unique ; TextSession gagne apply(Transaction,
  MultiSelection) — le refaire restitue la sélection explicite (A-13),
  la transaction vide déplace la sélection sans étape :
  fermeture automatique des paires et citations (A-05), Entrée indentée
  depuis l'arbre avec descente du fermant (A-06), retour arrière
  intelligent avec repli documenté sur les clusters (A-07), entourage
  en ligne ou par bloc (A-08), collage réindenté à la profondeur
  d'accueil (A-09) ; 46 tests neufs (40 intelligence + 6
  session), mutation 85,2 % (seuil 80).
- Clôture M4 — les critères de sortie du jalon sont tous couverts :
  ré-analyse ≤ 10 ms sur 50 000 lignes par langage (six analyseurs
  dédiés de 0,19 à 1,18 ms, six langages à grammaires de 0,47 à
  2,59 ms), corpus embarqués, scénarios B-01, B-16, A-15, C-01,
  C-07, C-14 à C-16 et G-01 au catalogue (vingt-deux scénarios) ;
  851 tests verts, manifestes d'API à jour (api/core-syntax-grammars
  posé), publication locale et APK de démonstration assemblés.

- Tranche 9 (M4) — la voie de grammaires pour la longue traîne
  (C-15/C-16) : nouveau module pur core-syntax-grammars — grammaires
  déclaratives au format TextMate (lecteur JSON strict, inclusions du
  référentiel résolues en références, cycles à travers un bloc permis),
  mapping portées → sortes de jetons (voie héritée du dépôt de départ),
  lexeur à pile de blocs ouverts immuable et à égalité de valeur — la
  cascade s'arrête exactement à la reconvergence (PIEGE-21) —, parseur
  générique aux plis par paires de crochets multi-lignes ; six
  grammaires embarquées : TOML, YAML, properties, Markdown (reste des
  P1) et Python, Shell (P2), coloration et plis sans analyseur dédié ;
  la cli annonce le septième module du cœur ; 54 tests neufs.

- Tranche 8 (M4) — le plan consommateur (B-16) : le pont session ↔
  vue pousse les entrées du plan de l'analyse habillée — le MÊME arbre
  que la coloration, jamais une autre révision — le rattachement et le
  retrait du langage vident le plan ; le modèle (android-widgets)
  déduit la profondeur de l'imbrication des bornes, conserve le
  préfixe intact d'un plan à l'autre (mise à jour incrémentale) et
  résout la rangée visée ; le peintre trace glyphe de catégorie en
  primitives et nom en retrait par niveau, sans allocation par image
  (PIEGE-15) ; 11 tests neufs.

- Tranche 7 (M4) — analyseur Gradle Groovy (C-07) : dialecte de
  script de construction — chaque appel de premier niveau en tête de
  ligne ouvre son chunk, gabarits ${…} où les guillemets internes ne
  clôturent pas, chaînes triples en crochets appariés, commentaires
  nichés en profondeur, états transportés ligne à ligne (PIEGE-19) ;
  budget de ré-analyse tenu sur machine lente (PIEGE-13) : JSON
  vivait en un chunk unique — chaque valeur de premier niveau ouvre
  désormais son chunk — et l'assemblage du lexique copie de masse le
  préfixe et le suffixe intacts (plancher abaissé pour tous les
  langages, 0,19 à 1,18 ms mesurés sur 50 000 lignes) ; scénario
  C-07 au catalogue de la démonstration ; 791 tests verts.

- Tranche 6 (M4) — l'analyse quitte le fil d'appel : jeton d'annulation
  sous moniteur (ADR-007 — ni volatile ni atomique au cœur, règles
  greppables au test d'architecture), exécuteur injecté au moteur
  `MoteurAnalyse` — fenêtre visible d'abord (G-01), complétion par lots
  publiés, l'édition supplante le travail en vol et la tâche périmée ne
  publie jamais (PIEGE-20) ; après publication, l'incrément suit (un
  seul site, une publication) et le rejeu d'annulation passe par la
  même voie (PIEGE-07) ; le pont `EditorSyntax` n'habille qu'à la bonne
  révision — la première image ne paie jamais l'arbre (G-01, facette
  analyse : instantané de fenêtre à 224 ms sur 100 000 lignes,
  enveloppe 500 ms, annuler la lexicographie coûte moins cher que la
  terminer) ; 770 tests verts.

- Tranche 5 (M4) — les consommateurs du rendu : coloration par plages
  du même arbre (B-01 — `SyntaxRowSource` habille chaque rangée des
  jetons soudés, rognés à la tranche, sans trou à peindre), palettes
  par sorte vérifiées WCAG sur les trois thèmes (D-13), plis calculés
  depuis l'arbre avec survie du pli manuel à l'éditon voisine (A-15 —
  `FoldTracker`, remappage monotone, rejeu d'annulation compris),
  coulisse de marqueurs en gouttière (D-07 — chevrons et bascule au
  clic ; D-02 — marque d'erreur par ligne) ; le pont pur
  `EditorSyntax` relie session, analyse et vue sur la voie de
  transaction unique ; le changement de source vide tout le cache
  (PIEGE-02) et le changement de session à révision 0 reconstruit
  désormais sa source (angle mort latent) ; 744 tests verts.
- Tranche 4bis (M4) — budget de ré-analyse à seuil : les cinq
  langages P0 ré-analysent 50 000 lignes après édition en moins de
  10 ms, minimum de trois tours retenu (PIEGE-13) ; le script Gradle
  KTS, mesuré à 29,2 ms en un chunk unique, gagne le découpage par
  appel de premier niveau — identificateur en tête de ligne à
  profondeur nulle — et retombe local ; 704 tests verts.
- Tranche 4 (M4) — analyseurs Kotlin et Gradle KTS : gabarits de
  chaînes (guillemets internes aux accolades qui ne clôturent pas),
  chaînes brutes par état, commentaires de bloc nichés à profondeur
  (la KDoc continuation reste de la documentation et s'attache au
  chunk), opérateurs propres ; le prédicat de découpe accepte le
  changement de ligne — Kotlin vit sans point-virgule — et les
  membres s'arrêtent à la fin de leur ligne ; le dialecte KTS
  partage lexeur et parseur sous son identifiant ; JavaCorps devient
  Corps paramétré par langage ; 699 tests verts — les cinq
  analyseurs P0 de la feuille de route sont livrés.
- Tranche 3 (M4) — analyseurs XML et JSON : lexeur XML à états
  transportés (fermeture nommée et auto-fermeture appariées à
  l'ouverte, CDATA en crochet apparié, balises à cheval sur les
  lignes, reprise après fermeture, noms non ASCII), parseur
  d'éléments tolérant (TAG repliables, fermeture désassortie en
  nœud d'erreur) ; JSON sans état transporté (nombres à exposant,
  paires nommées par leur clé au plan, contenants repliables, un
  chunk par document) ; l'extension de trivia n'attache plus que la
  documentation — le prologue XML cesse d'engloutir la racine ; 680
  tests verts.
- Tranche 2 (M4) — analyseur Java et registre des langages : lexeur
  à états transportés (textes en bloc ouvrant un crochet apparié,
  reprise du lexique après fermeture en milieu de ligne, casse —
  segment qualifié = identificateur, chaîne non fermée = ERROR) ;
  jetons soudés — les blancs appartiennent au jeton suivant, le rendu
  n'a pas de trou à peindre ; parseur tolérant (paquet et imports
  groupés, documentation entraînée, constructeurs nommés, corps en
  instructions et blocs, jamais d'exception), plan, plis, diagnostics
  et indentation (`TreeIndent`) du même arbre ; registre par
  extension la plus longue (`build.gradle.kts` contre
  `build.gradle`) ; correctifs du socle — l'extension de trivia
  atteint le début de plage (frontière parasite à l'ancre), le pli
  d'une déclaration avale celui de sa documentation — extraits
  `JavaNombres`, `JavaSignes`, `Jetons`, `JavaCorps` (complexité de
  classe sous le plafond PMD) ; 646 tests verts.
- Tranche 1 (M4) — socle d'analyse incrémentale : jetons à offsets
  relatifs par ligne, états transportés (cascade de commentaires bornée
  à la reconvergence, PIEGE-19), profondeurs de crochets suivies ;
  arbre tolérant ancré par chunks — sous-arbres relatifs, ré-analyse
  du seul chunk propriétaire, ancres décalées, jamais d'exception sur
  source malformé ; crochets appariés, plis, plan et diagnostics
  consommateurs du même arbre ; coordinateur session (annulation
  suivie par la voie unique, PIEGE-07) ; 44 tests, jqwik à graine
  déterministe, frontières ArchUnit.
- Tranche 6 (M3) — accessibilité, BiDi au rendu, scénarios et
  ressources : granularités de mouvement (clusters, mots, rangées,
  pages — une seule sémantique avec le clavier matériel), arbre
  virtuel de rangées visibles (lecture par lignes et plages, G-05,
  jamais le document entier), colle d'accessibilité (nœud éditable,
  enfants virtuels, annonces de sélection et de contenu) ; segments
  visuels bidirectionnels (règle L2, détection RTL sans allocation,
  tracé segment par segment, chemin rapide inchangé — PIEGE-15) ;
  Checkstyle et PMD deviennent bloquants sous AGP (écart tranche 1
  résolu, deux violations corrigées) ; scénarios M3 au catalogue
  (quatorze) et ressources en/fr complètes ; 562 tests verts, l'APK
  debug s'assemble.
- Tranche 5 (M3) — IME par niveau d'API 26 à 36 : machine de
  composition pure (frappe composée remplace la plage, jamais un
  append ; curseur relatif à la fin moins un ou au début,
  sémantique des sources de la plateforme ; région alignée aux
  clusters de graphèmes par fenêtres croissantes ; retour arrière
  dans la composition par cluster entier) ; fenêtres et
  suppressions d'entourage jamais une demi-paire de substitution ;
  défilement minimum de dévoilement du curseur ; soulignement de
  composition borné aux tranches de rangée (PIEGE-04) ; les 38
  méthodes de la surface 36 implantées sans repli (biseauté sur
  les archives 26/31/33/34/35/36 : rien après le 34) ; toute
  édition passe par la voie de transaction unique, capacités
  absentes assumées et documentées (fenêtres plutôt que texte
  entier, G-01) ; 125 tests verts.
- Tranche 4 (M3) — tactile complet et clavier matériel : pincement
  zoom par paliers bornés, point sous le doigt stable (D-15) ; pilule
  « ajouter un curseur » — l'appui long arme, l'appui ajoute, les
  existants tiennent (A-01) ; poignées de plage dont le glissement ne
  déplace qu'une extrémité, alignées aux tranches de rangée (A-18) ;
  table de raccourcis complète, remappage sans mutation, conflit
  refusé en nommant l'action, encodage dictionnaire pour la
  persistance hôte, mouvements par clusters, mots entiers et rangées,
  sélection l'ancre figée (D-17) ; suppression avant par clusters de
  graphèmes au cœur, symétrique de l'arrière (`ClusterDeletes`,
  manifeste d'API régénéré, mutation 85,2 %) ; géométrie de loupe
  bornée à la fenêtre ; 87 tests verts.
- Tranche 3 (M3) — retour à la ligne souple par offsets, très gros
  fichiers : la projection signe plis ET coupures (sommes préfixées en
  un passage, budget d'au moins une colonne — PIEGE-04/08), la clé de
  cache signe le budget (PIEGE-02), la rangée porte sa tranche (texte,
  offsets, largeur) ; gouttière muette sur les continuations,
  sélection bornée à la tranche ; vue : `setSoftWrap`,
  panoramique à deux axes, défilement horizontal borné ; correctif du
  peintre de texte — il consulte la source courante par
  `RebindingRowSource`, jamais une capture périmée ; première image de
  100 000 lignes mesurée à 154 ms (budget 500 ms, G-01), 480 000 lignes
  ouvrables ; 65 tests verts.
- Tranche 2 (M3) — vue Canvas réelle et chrome : thèmes clair, sombre,
  contraste élevé à contraste WCAG vérifié automatiquement (D-13) ;
  gouttière à numéros absolus alignés à droite, étiquettes mémoïsées
  (D-01, facette numéros) ; géométrie de défilement borné ; `EditorView`
  — une seule vue, trois couches, cache par révision, curseur posé au
  DOWN et sélection à ancre figée, panoramique à deux doigts — et ses
  adaptateurs Android (surface par image, pinceaux possédés, règle de
  police réelle) ; lint Android sans aucun problème, 51 tests verts.
- Tranche 1 (M3) — pièges de la vue écrits avant le code (PIEGE-01 à 06,
  14, 15, registre à jour) : socle de rendu JVM pur dans `android-view`,
  testable sans SDK — métriques à règle de police injectée, cache de
  rangées à clés immuables et consultation sans allocation, couches à
  pinceau possédé et couleur de base reposée à chaque image, projection
  rangées ↔ lignes par le modèle de plis, gestuelle à ancre figée au
  DOWN, conversion position ← pixel au plancher, gras mémoïsé jusqu'au
  changement de police, tracé d'image à l'indice (26 tests verts).
- Tranche 5 — multi-sélection de première classe (ADR-003) : plages
  ancre-curseur `SelRange`, liste normalisée `MultiSelection` (fusion aux
  recouvrements, projection à gravité droite), la session porte la
  sélection et la restitue à l'annulation (A-13) ; frappe multi-curseurs
  par la voie de transaction, coalescence étendue ; propriétés jqwik
  contre oracle naïf.
- Tranche 6 — UAX #29/#9, encodages et BOM : clusters de graphèmes
  (`Grapheme`, GB3-GB999 révision 15) et segmentation de mots (`Word`,
  WB3-WB999) ; résolution bidirectionnelle (`Bidi`, P2-P3, X1-X8, W1-W7,
  N1-N2, I1-I2, L1, L2 — N0 non implémentée, limite documentée) ;
  détection par BOM toutes familles, heuristique bornée, décodage strict
  à erreurs par offsets, ré-encodage refusant les substituants isolés,
  fins de ligne dominantes (`Encoding`, `EncodingCodec`, `Utf8Codec`,
  `WideCodecs`) ; suppression arrière par clusters entiers, tous
  curseurs, plages fusionnées au toucher, par la voie unique
  (PIEGE-07) ; scénarios de démo M2 au catalogue (A-13, A-18, A-20,
  A-24, A-25) avec test de fumée ; mutation 85,1 % (1513/1777, seuil
  80 %).
- Tranche 2 — pièges du texte, tests écrits avant le code (PIEGE-07/08/13,
  registre des pièges à jour) : moteur d'annulation transactionnel dans
  `core-text` — la `Transaction`, seule voie de mutation (ADR-003), le rejeu
  de l'annulation par cette même voie, coalescence des frappes portée à
  horloge injectée (ADR-007), instantané `Document` immuable, projecteurs de
  positions (plages ancrées, gravités du pipeline commun), propriété jqwik à
  oracle `StringBuilder` (défaire/refaire tout restaure les textes).
- Tranche 3 — index de lignes porté dans l'instantané `Document` (registre de
  portage) : débuts de ligne en tableau parallèle, recherche dichotomique,
  splice incrémental à chaque édition PAR la voie de mutation unique —
  annulation et refaire compris ; requêtes de ligne (numéro, débuts, fins,
  contenu), seuils « gros document », mémo du texte complet à référence
  stable ; invariants croisés document/index vérifiés au fuzz (oracle
  `StringBuilder` étendu, défaire/refaire compris).
- Tranche 4 — recherche/remplacement porté dans `core-text` (registre de
  portage, tests du dépôt de départ comme oracle)  moteur `FindReplace` à
  horloge injectée (ADR-007)  casse, mot entier, regex sous garde-fou
  (budget temps échantillonné, plafond de collecte, états exposés),
  occurrences entières du mot sous le caret ; passe de remplacement par la
  voie de transaction — éditions par occurrence ajustées en deltas cumulés,
  UNE étape d'annulation jamais coalescée avec les frappes, plafond miroir
  de la collecte ; remplacement global par la session ; propriété jqwik à
  oracle `String.replace` ; correctifs consignés (casse insensible
  exotique — repli régional, indices sur le texte original ;
  correspondances de remplacement non recouvrantes).
- Benchmark d'édition de référence : 50 000 lignes, mille éditions d'un
  caractère par tour par la voie complète de la session, minimum de trois
  tours (PIEGE-13) — mesure locale : 0,055 ms par édition (budget 1 ms).
- Modèle de retour à la ligne souple pur à mesure injectée : coupe par
  caractères d'un mot plus large que la ligne par recherche dichotomique,
  accumulation gloutonne mot à mot (PIEGE-08).
- Harnais de benchmark au minimum de trois tours par phase, horloge
  injectée (PIEGE-13) — mesure locale undo/redo × 100 sur 2 000 lignes :
  0,22 ms / 0,25 ms.

### Corrigé

- la démonstration n'affichait jamais le contenu du fichier ouvert :
  la session préparée n'était pas branchée sur EditorView, qui gardait
  sa session vide d'origine — le branchement se fait à la création de
  la scène.
- le contrôle Javadoc de la politique de langue confondait le
  contenu des blocs de texte Java avec des déclarations — le code
  factice des tests bloquait la porte à tort ; l'état d'ouverture
  des blocs se suit désormais, aucune ligne légitime n'échappe au
  contrôle.
- Segmentation de mots : le regard des séparateurs médians (WB7, WB12)
  comparait le caractère directement précédent au lieu du précédent
  effectif — une marque combinante collée au séparateur coupait le mot
  que la norme garde entier (« a.◌̱b ») ; le précédent effectif porte
  désormais sa position de début et la classe avant le séparateur se
  lit au bon endroit (WB4).
- La porte PMD tournait à vide depuis M1 : le jeu de règles référençait des
  règles retirées de PMD 7.29.0 (aucun fichier analysé, tâche verte) —
  références valides, seuil de complexité réel (15 par méthode inchangé,
  plafonds 600/50 portés par Checkstyle, seuil de classe à la valeur par
  défaut de la règle).
- La mutation ciblait aussi les classes internes et utilitaires de test :
  cibles limitées au code de production ; le seuil suit le score au sens
  canonique de PIT (un mutant mort par boucle ou par mémoire est détecté) —
  seuil de 80 % inchangé, la tranche passe aussi en KILLED seuls.

- Validation M1 par le propriétaire (2026-11-04) : « M1 validé », sans
  désignation d'appareil de référence (question 5 ouverte) — M2, moteur de
  texte, est déclenché.
- Portage de la rope équilibrée dans `core-text` (feuilles, branches, fusion
  d'épines, rééquilibrage Fibonacci — registre de portage) avec ses tests
  portés et étendus : frontières exactes de découpage, identité des
  coupes pleines, contrats de message hors bornes, fenêtres de visite
  alignées sur les frontières de feuilles.
- Propriétés jqwik à oracle `StringBuilder` (environ 10 300 séquences
  aléatoires par exécution — objectif ADR-003 atteint pour la rope) ;
  le fuzz Jupiter passe à graines fixes reproductibles (exigence de la
  mutation) ; le harnais de démonstration M1 est retiré, remplacé
  par les propriétés réelles.

### Corrigé

- `apiCheck` relisait le nom du projet à l'exécution : la divergence
  de manifeste échouait en erreur de cache de configuration au lieu de la
  consigne de régénération — le nom est capturé à la configuration.
- Premier passage CI (ajustement prédit par le résumé M1) : le travail
  « modules purs » voit le SDK préinstallé du runner — le refus explicite
  `CELERIS_SKIP_ANDROID=1` prime sur la détection ; `sdkmanager`
  s'appelle par chemin complet (absent du PATH) ; la tolérance
  Javadoc consulte les source sets à la réalisation de la tâche (AGP 9 ne
  les pose pas à l'application du plugin — la consultation hâtive cassait
  la configuration des modules Android).
- Deuxième passage CI : la tolérance Javadoc relisait le conteneur
  dans la lambda de tâche (extensions DE LA TÂCHE, jamais trouvées — retour
  au relevé hâtif, avec findByName null-sûr au lieu de named().get()
  jetant) ; les trois modules Android n'appliquent plus la
  convention de publication — différée à M2/M3 (écart n° 5 du résumé M1) —
  AGP 9 ne posant ni « release » ni « java » dans le conteneur de
  composants.

- Validation M0 par le propriétaire (2026-11-04) : les douze ADR passent à
  l'état « Acceptée » ; les réponses aux questions groupées sont consignées
  dans le résumé M0 (question 5 — appareil de référence des budgets —
  ouverte).
- Fondations M1 : `build-logic` à plugins de convention, catalogue unique,
  squelette exécuté des dix modules et de la démonstration, cli, workflows
  CI épinglés par somme SHA avec validation du wrapper.
- Portes qualité bloquantes dès le premier commit : Checkstyle, PMD, Error
  Prone + NullAway (JSpecify), JaCoCo, ArchUnit, Jupiter/jqwik, mutation
  PIT au seuil de 80 % , scripts de politique (langue, neutralité).
- Manifeste d'API publique par module pur (`api/*.api`) vérifié dans
  `check` ; publication Maven locale complète avec SBOM et sommes SHA-256.
- `AGENT.md` (contrats d'agent) ; `NOTICE` consigne l'outillage et ses
  licences.

## [0.0.1] — 2026-11-04

### Ajouté

- Cadrage M0 : audit mesuré du dépôt de départ (`docs/audit/`), matrice de
  parité A–G (`docs/parite/`), ADR 001 à 012 en proposition, feuille de
  route chiffrée, glossaire.
- Spikes de mesure (rope re-mesurée, lexicographie incrémentale, rendu par
  proxy JVM) avec résultats consignés.
- Contrôles de politique de langue et de références externes, exécutables
  dès M0.

[0.0.1]: https://github.com/jjoblab/celeris/releases/tag/v0.0.1

