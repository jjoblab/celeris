# Inventaire des popups de Celeris

> Inventaire exhaustif (Phase 1) : chaque composant qui affiche une surface
> contextuelle au-dessus de l'éditeur, fiche par fiche, puis classification
> (catégorie fonctionnelle, forme visuelle, verdict de l'audit). Les chemins
> sont exacts. Les catégories A–G reprennent le plan de classification de la
> mission ; la colonne « état » reprend les six classes possibles :
> [1] fonctionnel et réutilisable, [2] fonctionnel mais à refactorer,
> [3] partiellement implémenté, [4] dupliqué, [5] obsolète/inutilisé,
> [6] manquant.

## 1. Fiches des composants existants

### 1.1 Infrastructure du kit (android-widgets)

| # | Classe | Fiche | État |
| --- | --- | --- | --- |
| I-1 | `PopupController` | Machine à états pure, 26 genres, table exhaustive `FAMILLE_ACTIONS` des exclusions de la famille des surfaces d’actions, cascades (doc suit la liste, débordement suit la barre), régimes frappe/défilement/`closeAll`. Aucune vue. | [1] |
| I-2 | `PopupLayer` | ViewGroup porteur, PIEGE-27, voile naissance/mort avec retrait différé, `show`/`reposition`/`remonter`/`dismiss`, invariant une-vue-un-genre. | [2] — `show` ne borne pas les coordonnées ; course du retrait différé (voir audit P6). |
| I-3 | `PopupPanel` (abstraite) | Carte de base : verre, liseré hairline, coins arrondis, clip, animations 120/100 ms, palette. | [1] |
| I-4 | `TextRowsPopup` (abstraite) | Carte à rangées typées + surbrillance + rappel tactile `setOnPick`. | [1] |
| I-5 | `PopupPlacer` | Placement coordonné du trio complétion/documentation/signature, borné, pur. | [1] |
| I-6 | `PopupAnchor` | Ancres simples bornées : `belowOrAboveCaret`, `aboveOrBelowCaret`, `atPoint`, `topLeftCorner`, `rightEdge`, `centeredAboveSelection`. | [2] — `rightEdge` mort (jamais appelé en production). |
| I-7 | `PopupPalette` / `PopupTokens` | Thème verre (dark/light/dérivé) + jetons dp/sp. | [1] |

### 1.2 Surfaces portées par la couche (android-widgets)

| # | Classe | Genre | Fonction | Déclencheur | Données | Positionnement | Fermeture | État |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| S-1 | `CompletionListView` | `COMPLETION_LIST` | Liste de complétion deux lignes, badge de genre, runs du préfixe. | Frappe (démo), sélection si ouverte, action de menu. | LSP `LanguageSession.completion` ou repli `IntelligenceHub` + `PopupCoach`. | `PopupPlacer` sous le curseur. | Frappe sur blanc/ponctuation, acceptation, action menu. | [2] — aucun clavier (flèches/Enter/Escape). |
| S-2 | `CompletionDocView` (+`CompletionDocModel`) | `COMPLETION_DOC` ET `HOVER` | Documentation flottante de complétion ET survol/peek/`@Preview` — MÊME instance. | Ouverture avec la liste ; double-tap ; « i » de la barre ; peek (menu) ; clic repère gouttière. | LSP `survol` ou table `PopupCoach` ; `ApercuDefinition` (plan local). | `PopupPlacer` (flanc) ou `PopupAnchor.atPoint`/`topLeftCorner`. | Suit la liste ; frappe/défilement ferment le `HOVER`. | [4]-risque — l'instance partagée entre deux genres fait mentir l'état (audit P1). |
| S-3 | `SignatureHelpView` (+`SignatureAideModel`) | `SIGNATURE_HELP` | Aide aux paramètres : surcharges ±1, paramètre actif à l'accent, stepper tactile, peek appui long. | Chaque édition/sélection. | LSP `signature` ou `PopupCoach.signatureFor`. | `PopupPlacer` (côté opposé). | Modèle null. | [2] — `Handler` sans nettoyage au détachement. |
| S-4 | `IntentionsMenuView` | `INTENTIONS_MENU` ET `SELECTION_OVERFLOW` | Menu d'actions au curseur ET carte de débordement — MÊME instance. | Action menu ; puce « ⋯ » barre ; puce toolbar. | `CatalogueActions` (activité). | `PopupAnchor.belowOrAboveCaret` / ad hoc coin haut-droit (toolbar). | Choix d'une action avant exécution ; exclusions. | [4]-risque — trois usages, deux genres, deux positionnements concurrents (audit P1). |
| S-5 | `DiagnosticTooltipView` | `DIAGNOSTIC_TOOLTIP` | Info-bulle de diagnostic (×/!, message). | Sélection (hors frappe) sur ligne diagnostiquée. | `DiagnosticHote` (SPI). | `PopupAnchor.atPoint` au caret. | Frappe, défilement. | [1] |
| S-6 | `BreadcrumbView` (+`BreadcrumbModel`) | `BREADCRUMB` | Fil d'Ariane (chemin du plan englobant). | Chaque édition/sélection. | `OutlineEntry` (plan). | `PopupAnchor.topLeftCorner`. | Chemin vide. | [1] |
| S-7 | `SelectionBarView` | `SELECTION_BAR` | Pilule d'actions de la sélection + « i » + « ⋯ ». | Sélection non vide ; dragage la déplace (`remonter`). | Actions de l'activité. | `PopupAnchor.centeredAboveSelection`. | Frappe, sélection vide. | [1] |
| S-8 | `ColorPickerView` | `COLOR_PICKER` | Sélecteur de couleur (champ SV + teinte + palette Material). | Double-tap sur littéral de couleur. | `PastillesCouleur` (vue) + modèle interne. | `PopupAnchor.belowOrAboveCaret`. | Pick implicite, frappe, défilement — le « tap hors carte » documenté N'EST PAS implémenté. | [2] |
| S-9 | `TouchBarView` | `TOUCH_BAR` | Barre de touches de code au-dessus du clavier. | Posée par l'hôte ; reposée au layout global. | Touches de l'activité. | Ad hoc bas centré (`reposerBarre`). | Jamais (vit avec le document). | [2] — listener global irrécupérable (audit P6). |
| S-10 | `ErrorBandView` (+`ErrorBandModel`) | `ERROR_BAND` | Bande d'erreurs au bord droit (tirets de gravité, saut tactile). | Diagnostics de l'hôte. | `DiagnosticHote`. | Ad hoc `AtelierBordDroit.bordDroit()`. | Sans diagnostics. | [2] — positionnement ad hoc hors `PopupAnchor`. |
| S-11 | `MinimapView` (+`MinimapModel`) | `MINIMAP` | Silhouette colorée + cartouche de fenêtre. | Édition/défilement. | Document + arbre syntaxique. | Ad hoc `bordDroit()`. | Jamais. | [2] — idem ; palette locale. |
| S-12 | `FastScrollerView` | `FAST_SCROLLER` | Pouce de défilement rapide qui grossit sous le doigt. | Défilement/doigt. | Fenêtre du document. | Ad hoc `bordDroit()`. | Jamais. | [2] — idem. |
| S-13 | `ReferencesView` | `REFERENCES` | Les usages du symbole au curseur : rangées cliquables « L12 · extrait », en-tête qui compte, saut au choix. | Action de menu, menu d'intentions, puce de débordement. | LSP `LanguageSession.references` (le lieu du document ouvert). | `PopupAnchor.belowOrAboveCaret`. | Choix d'une occurrence (saut), frappe, défilement, tap hors cartes. | [1] — le signal du propriétaire du 2026-11-13. |
| S-14 | `TypeInfoView` | `TYPE_INFO` | Le nom du type du symbole au curseur — une rangée titre, la carte passive ne règne pas sur le clavier. | Action de menu, menu d'intentions, puce de débordement. | `LecteurTypes` (lecture locale de déclaration, jamais deviné). | `PopupAnchor.belowOrAboveCaret`. | Frappe, défilement, tap hors cartes. | [1] — le fil rouge du guide. |
| S-15 | `HierarchiesView` | `HIERARCHY` | L'arbre de types ou d'appels du symbole au curseur — rangées indentées par PROFONDEUR, le sommet connu saute. | Action de menu, menu d'intentions, puce de débordement. | `Hierarchies` (core-intelligence, projet embarqué). | `PopupAnchor.belowOrAboveCaret`. | Choix d'un sommet (saut), frappe, défilement, tap hors cartes. | [1] |
| S-16 | `NotificationView` | `NOTIFICATION` | La bulle éphémère du coin bas-droit — l'annonce vit son quart, l'horloge la referme. | L'hôte (correctifs appliqués, pause de débogage, exécution). | L'activité. | `PopupAnchor.bottomRightCorner`. | Son heure (4 s) ou le tap qui la prend. | [1] |
| S-17 | `ExceptionView` | `EXCEPTION_DETAILS` | Les détails d'exception — le type à l'encre d'erreur, le message, la pile en cadres cliquables (l'étranger vit muet). | Action d'exécution (menu, intentions, puce). | `TraceExecution` (trace du document réel, l'analyseur de piles E-06 la relit). | `PopupAnchor.belowOrAboveCaret`. | Tap hors cartes, échappement — la consultation. | [1] — la passe du 2026-11-14. |

### 1.3 Feuilles modales (ADR-018) — android-widgets + demo

| # | Classe | Fonction | Focus | État |
| --- | --- | --- | --- | --- |
| F-1 | `Feuille` (abstraite) | Surface modale posée sur `android.R.id.content`, voile cliquable, poignée de glissement, retour système. | PREND le focus (assumé, hors PIEGE-27). | [2] — non coordonnée avec le contrôleur de couche (audit P8). |
| F-2 | `FeuilleBas` | Réglages (`FeuilleReglages`), plan (`FeuillePlan`), recherche (`AtelierRecherche`), renommage (`AtelierRefactor`). | Idem. | [2] |
| F-3 | `FeuilleHaut` | Revues de diff (`AtelierDiff`), correctifs LSP (`AtelierCorrectif`), IA (`AtelierIA`), agent (`AtelierAgent`). | Idem. | [2] |

### 1.4 Hors kit — surfaces contextuelles de `android-view` et `demo`

| # | Classe | Nature | État |
| --- | --- | --- | --- |
| H-1 | `Loupe` + `MagnifierGeometry` | Bulle d'agrandissement in-canvas pendant le dragage. | [1] — choix assumé (photographie de frame). |
| H-2 | `FantomePainter`/`FantomeEditeur`/`GesteFantome` | Texte fantôme IA in-canvas, acceptation Tab. | [1] |
| H-3 | `DiagnosticOverlay` (demo) + `surimpression` | HUD de diagnostic (TextView sur la scène). | [1] — hors périmètre carte. |
| H-4 | `PointerGestures.bulleVisible` | Info-bulle de survol stationnaire — PLUS AUCUN APPELANT en production (survol par appui long retiré le 2026-11-13). | [5] — code mort couvert par un banc. |
| H-5 | `ValeursPainter` + `ValeurLigne` | Valeurs de débogage en ligne (E-03) : le rendu en fin de ligne utile, encre de gouttière, crochet `setValeursEnLigne`. | [1] — la pause les pose, la reprise les retire. |

### 1.5 Modèles de support (android-widgets)

`CompletionDocModel` (Markdown typé), `SignatureAideModel`,
`BreadcrumbModel`, `ErrorBandModel`, `MinimapModel`, `SearchBarModel`,
`ApercuDefinition` (peek ≤ 12 lignes du plan), `BadgeGenre` (genres de
badges), `OutlineModel`/`OutlineView`/`OutlinePainter`/`OutlineSurface`
(plan), `DiffBlocs`/`RevueDiff`/`PatchPreview` (diffs et correctifs) —
tous [1] : purs, testés, sans vue.

### 1.6 Orchestration (demo)

| # | Classe | Rôle | État |
| --- | --- | --- | --- |
| O-1 | `AtelierPopups` (598 l.) | Chef d'orchestre de facto : possède contrôleur + vues, branche session/vue, délègue aux satellites. | [2] — au plafond ADR-002 ; six copies de mesure ; cascades en double. |
| O-2 | `DocAncre` | Survol/peek/`@Preview` sur la carte partagée. | [4]-risque (vue partagée). |
| O-3 | `MenuDebordement` | Débordement de la barre sur la carte partagée. | [4]-risque. |
| O-4 | `AtelierCouleur` | Color picker au double-tap. | [2]. |
| O-5 | `AtelierBordDroit` | Bande/minimap/pouce, positionnement ad hoc. | [2]. |
| O-6 | `AtelierBarreToolbar` | Carte de débordement toolbar (genre `INTENTIONS_MENU` !) + touchbar. | [4]-risque + fuite listener. |
| O-7 | `PopupCoach` | Décisions de contenu pur : candidats, tables de repli, `doitOuvrirALaFrappe`, path, issues. | [1] |
| O-8 | `CatalogueActions` | Table unique des actions, trois peaux (menu, carte, intentions). | [1] |

## 2. Classification par catégorie fonctionnelle (phase 3 de la mission)

### CATÉGORIE A — Intelligence du code

| Fonction | Composant | Forme | État |
| --- | --- | --- | --- |
| Code Completion | `CompletionListView` | popup flottant ancrée au caret | implémenté (tactile) |
| Quick Documentation (dans complétion) | `CompletionDocView` | popup flottante au flanc | implémenté |
| Hover Information | `CompletionDocView` sous genre `HOVER` | infobulle au point | implémenté (double-tap / « i ») |
| Parameter Info / Signature Help | `SignatureHelpView` | popup flottante | implémenté (serveur ou repli) |
| Type Information | `TypeInfoView` | carte ancrée au curseur | implémenté (lecteur local de déclaration) |
| Quick Definition (peek) | `DocAncre.apercu` + `ApercuDefinition` | popup flottante | implémenté (plan local) |
| External Documentation | `CompletionDocView` sous genre `EXTERNAL_DOC` (`AtelierDocExterne`) | popup flottante | implémenté (régime de la consultation) |
| Code Inspection Description | `DiagnosticTooltipView` | infobulle | implémenté |

### CATÉGORIE B — Diagnostics et corrections

| Fonction | Composant | Forme | État |
| --- | --- | --- | --- |
| Quick Fix / Correctifs | `AtelierCorrectif` | feuille haute | implémenté (codeAction du serveur de référence uniquement) |
| Intention Actions | `IntentionsMenuView` | menu au caret | implémenté |
| Error/Warning Details | `DiagnosticTooltipView` + `ErrorBandView` | infobulle + bande | implémenté |
| Import Resolution | `CorrectifImport` + feuille d'aperçu `AtelierCorrectif` | feuille haute | implémenté (classpath du terrain, repli local) |
| Suppress Inspection | `SuppressionInspection` + `AtelierSuppression` (carte du kit) | carte ancrée au curseur | implémenté (la marque « noinspection » en fin de ligne, le lint l'honore à sa relance — la passe du 2026-11-14) |

### CATÉGORIE C — Navigation

| Fonction | Composant | Forme | État |
| --- | --- | --- | --- |
| File Structure / Plan | `FeuillePlan` + `OutlineView` | feuille basse | implémenté |
| Breadcrumb | `BreadcrumbView` | bandeau coin haut-gauche | implémenté |
| Search (fichier) | `AtelierRecherche` | feuille basse + état `SEARCH_BAR` | implémenté |
| Go to Definition | peek `DocAncre.apercu` | popup flottante | implémenté (plan local) |
| Find Usages / References | `ReferencesView` | carte ancrée au curseur | implémenté (serveur de langage par le fil) |
| Go to File / Class / Symbol | `FeuilleAller` + `ResultatsAller` | feuille basse | implémenté (recherche floue B-13, fichiers et plan) |
| Related Symbols / Hiérarchies | `HierarchiesView` + `AtelierHierarchies` | carte ancrée au curseur | implémenté (types et appels, projet embarqué) |

### CATÉGORIE D — Refactoring et génération

| Fonction | Composant | Forme | État |
| --- | --- | --- | --- |
| Rename | `AtelierRefactor` (feuille) + `preparationRennomage`/`rennomage` LSP | feuille basse | implémenté |
| Diff de renommage | `AtelierDiff` + `DiffBlocs`/`RevueDiff` | feuille haute | implémenté |
| Safe Delete / Move | `AtelierSurete` (cartes du kit) + `SafeDelete`/`SafeMove` | cartes ancrées au curseur | implémenté (les usages qui barrent s'affichent, les cibles se proposent — le verdict du moteur décide, la passe du 2026-11-14) |
| Generate Code / Gabarits | `AtelierGabarits` (carte du kit) + `Generateurs`/`Gabarits` | carte ancrée au curseur | implémenté (champs réels de la classe englobante B-24, le gabarit à trous A-22 — la passe du 2026-11-14) |
| Surround With / Live Templates | `Entourer` + la carte de génération | carte ancrée au curseur | implémenté (try/catch, if — le bloc ceint s'indente d'un cran, la passe du 2026-11-14) |

### CATÉGORIE E — Débogage

| Fonction | Composant | Forme | État |
| --- | --- | --- | --- |
| Valeurs en ligne | `ValeursPainter` + `setValeursEnLigne` (SPI `ValeurEnLigne` servie) | inlay in-canvas | implémenté (fin de ligne utile, pause/reprise) |
| Points d'arrêt (pose) | `EtatArret` + gouttière (appui long) | marque inline | implémenté |
| Exception Details | `AtelierException` + `TraceExecution` (carte du kit `ExceptionView`) | carte ancrée au curseur | implémenté (l'exécution simulée E-04 porte la trace du document réel, l'analyseur de piles E-06 la relit — la passe du 2026-11-14) |

### CATÉGORIE F — Projet et environnement

| Fonction | Composant | Forme | État |
| --- | --- | --- | --- |
| Menu de la toolbar | `AtelierBarreToolbar` + `IntentionsMenuView` | carte de débordement | implémenté (genre détourné) |
| Barre de touches de code | `TouchBarView` | barre au-dessus du clavier | implémenté |
| Réglages | `FeuilleReglages` | feuille basse | implémenté |
| Révocations IA/agent | `AtelierIA`/`AtelierAgent` | feuilles hautes | implémenté |
| Git / Gradle / Configs d'exécution | — | — | manquant (hors mandat de l'éditeur, ressort de l'hôte) |

### CATÉGORIE G — UI contextuelle

| Fonction | Composant | Forme | État |
| --- | --- | --- | --- |
| Menu contextuel éditeur | `IntentionsMenuView` (intentions) | menu au caret | implémenté |
| Barre de sélection | `SelectionBarView` + `MenuDebordement` | pilule + carte | implémenté |
| Color Picker | `ColorPickerView` | popup flottante | implémenté |
| Rendu `@Preview` | `DocAncre.renduPreview` | carte au coin gouttière | implémenté |
| Notifications | `NotificationView` + `AtelierNotifications` | bulle éphémère coin bas-droit | implémenté (horloge 4 s, tap la prend) |
| Loupe | `Loupe` | bulle in-canvas | implémenté (hors couche) |
| Fantôme IA | `FantomeEditeur` | inlay éphémère in-canvas | implémenté (hors couche) |
| Minimap / Fast scroll / Bande d'erreurs | `MinimapView`/`FastScrollerView`/`ErrorBandView` | chrome bord droit | implémenté (chrome, pas cartes) |

## 3. Bilan chiffré

- 17 genres d'état au contrôleur ; 13 surfaces portées par la couche ;
  3 feuilles modales (+ 9 ateliers de feuilles en démo) ; 3 surfaces
  in-canvas assumées (loupe, fantôme, HUD) ; 2 surfaces interactives
  hors couche (gouttière, poignées).
- Fonctionnalités réellement opérationnelles (tactile) : complétion + doc
  + signature + intentions + survol + peek + info-bulle + fil d'Ariane +
  barre de sélection + débordements + color picker + touchbar + bande +
  minimap + pouce + plan + recherche + réglages + renommage + diff +
  correctifs + IA/agent + `@Preview`.
- Manquants prioritaires (au sens « la donnée existe, la surface manque ») :
  hiérarchies ; valeurs de débogage ; notifications ;
  recherche de symbole/fichier. (La navigation clavier, la fermeture au clic extérieur et les usages sont livrés — le signal du 2026-11-13.)
- Aucun doublon de MOTEUR réel : les trois voies de complétion
  (`CompletionMots`, `PopupCoach.completionFor`, capacité `Completion` de
  `core-language`) vivent à des étages différents (production, repli de
  repli, contrat non branché) — la capacité `Completion` enregistrée par
  `LangagesDemo` n'est LUE nulle part en production : [5] à décider en
  phase architecture.

## 4. Règles de coexistence constatées (aujourd'hui)

Écrites dans `PopupController` : complétion ⇄ intentions s'excluent ;
`COMPLETION_DOC` exige la liste, fermé avec elle ; `SELECTION_OVERFLOW`
exige la barre, fermé avec elle ; la frappe ferme survol, info-bulle,
barre de sélection, débordements, color picker, usages, info de type et
hiérarchie ; le défilement ferme survol, info-bulle, color picker,
usages, info de type et hiérarchie — il repositionne le reste ; le tap
hors cartes referme les cartes d'actions et les bulles (usages,
hiérarchie, info de type et documentation externe comprises) ;
bande/minimap/pouce/touchbar ne ferment jamais ; la notification vit
son quart — frappe, défilement et geste extérieur l'épargnent, seule
son heure la conclut ; la documentation externe vit le régime de la
consultation — la frappe ne la chasse pas, le défilement la
repositionne. La barre de recherche ferme sur demande seule. Les
feuilles ignorent le contrôleur (P8) ; le menu d'intentions, le
débordement de la toolbar, la carte des usages et la hiérarchie ne
tiennent JAMAIS ensemble (une surface d'actions à la fois) ; la
complétion cohabite avec usages, hiérarchie, info de type et
documentation externe (elles décrivent, la saisie les chasse).

