# CHANGELOG

Tous les changements notables de ce projet sont consignés ici, par version,
en français. Format calqué sur « Keep a Changelog » ; versions suivant
SemVer. **Court par construction** : une version = une entrée ; les détails
vivent dans les ADR et le registre de portage, pas ici.

## [Non publié]

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
