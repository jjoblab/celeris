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
| I-1 | `PopupController` | Machine à états pure, 15 genres, exclusions (complétion ⇄ intentions), cascades (doc suit la liste, débordement suit la barre), régimes frappe/défilement/`closeAll`. Aucune vue. | [1] |
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

### 1.5 Modèles de support (android-widgets)

`CompletionDocModel` (Markdown typé), `SignatureAideModel`,
`BreadcrumbModel`, `ErrorBandModel`, `MinimapModel`, `SearchBarModel`,
`ApercuDefinition` (peek ≤ 12 lignes du plan), `BadgeGenre` (genres de
badges), `OutlineModel`/`OutlineView`/`OutlinePainter`/`OutlineSurface`
(plan), `DiffBlocs`/`RevueDiff`/`PatchPreview` (diffs et correctifs) —
tous [1] : purs, testés, sans vue.

### 1.6 Orchestration (android-host, le déménagement du 2026-11-14)

| # | Classe | Rôle | État |
| --- | --- | --- | --- |
| O-1 | `PopupsEditeur` (596 l.) | L'orchestrateur officiel : possède manager + vues, branche session/vue, délègue aux satellites — le montage complet de l'hôte tient en sa construction. | [1] — vit dans `jo.celeris.android.host.popups`. |
| O-2 | `DocAncre` | Survol/peek/`@Preview` sur la carte du genre survol. | [1] — déménagé. |
| O-3 | `MenuDebordement` | Débordement de la barre de sélection (carte du kit). | [1] — déménagé dans `android-widgets`. |
| O-4 | `SelecteurCouleur` | Color picker au double-tap. | [1] — déménagé. |
| O-5 | `BordDroit` | Bande/minimap/pouce, positionnement ad hoc. | [1] — déménagé. |
| O-6 | `BarreToolbar` | Carte de débordement toolbar (genre propre) + touchbar. | [1] — déménagé dans `android-widgets`. |
| O-7 | `FilAriane` | Le chemin du plan qui englobe le curseur. | [1] — né de l'extraction du plafond ADR-002. |
| O-8 | `ContenuPopups` / `CoachPopups` | Les algorithmes de contenu purs / l'interface des tables de l'hôte (le coach MUET par défaut). | [1] — déménagés. |
| O-9 | `LanguageSession` | La surface commune des serveurs de langage (modèles convertis). | [1] — déménagée. |
| O-10 | `CatalogueActions` (demo) | Table unique des actions, trois peaux (menu, carte, intentions). | [1] — reste au terrain. |
| O-11 | `CoachDemo` / `PopupCoach` (demo) | Les tables du terrain de test (mots Java, signatures, documentation factices). | [1] — reste au terrain. |

## 2. Classification par catégorie fonctionnelle (phase 3 de la mission)

### CATÉGORIE A — Intelligence du code

| Fonction | Composant | Forme | État |
| --- | --- | --- | --- |
| Code Completion | `CompletionListView` | popup flottant ancrée au caret | implémenté (tactile) |
| Quick Documentation (dans complétion) | `CompletionDocView` | popup flottante au flanc | implémenté |
| Hover Information | `CompletionDocView` sous genre `HOVER` | infobulle au point | implémenté (double-tap / « i ») |
| Parameter Info / Signature Help | `SignatureHelpView` | popup flottante | implémenté (serveur ou repli) |
| Type Information | — | — | manquant |
| Quick Definition (peek) | `DocAncre.apercu` + `ApercuDefinition` | popup flottante | implémenté (plan local) |
| External Documentation | — | — | manquant |
| Code Inspection Description | `DiagnosticTooltipView` | infobulle | implémenté |

### CATÉGORIE B — Diagnostics et corrections

| Fonction | Composant | Forme | État |
| --- | --- | --- | --- |
| Quick Fix / Correctifs | `AtelierCorrectif` | feuille haute | implémenté (codeAction du serveur de référence uniquement) |
| Intention Actions | `IntentionsMenuView` | menu au caret | implémenté |
| Error/Warning Details | `DiagnosticTooltipView` + `ErrorBandView` | infobulle + bande | implémenté |
| Import Resolution | — | — | manquant |
| Suppress Inspection | — | — | manquant |

### CATÉGORIE C — Navigation

| Fonction | Composant | Forme | État |
| --- | --- | --- | --- |
| File Structure / Plan | `FeuillePlan` + `OutlineView` | feuille basse | implémenté |
| Breadcrumb | `BreadcrumbView` | bandeau coin haut-gauche | implémenté |
| Search (fichier) | `AtelierRecherche` | feuille basse + état `SEARCH_BAR` | implémenté |
| Go to Definition | peek `DocAncre.apercu` | popup flottante | implémenté (plan local) |
| Find Usages / References | `LanguageSession.references` (données servies) | — | données prêtes, surface manquante |
| Go to File / Class / Symbol / Recent | — | — | manquant |
| Related Symbols | `Hierarchies` (core-intelligence, données) | — | données prêtes, surface manquante |

### CATÉGORIE D — Refactoring et génération

| Fonction | Composant | Forme | État |
| --- | --- | --- | --- |
| Rename | `AtelierRefactor` (feuille) + `preparationRennomage`/`rennomage` LSP | feuille basse | implémenté |
| Diff de renommage | `AtelierDiff` + `DiffBlocs`/`RevueDiff` | feuille haute | implémenté |
| Safe Delete / Move | `SafeDelete`/`SafeMove` (core-intelligence) | — | données prêtes, surface manquante |
| Generate Code / Gabarits | `Gabarits`/`SquelettesMethode` | — | données prêtes, surface manquante |
| Surround With / Live Templates | — | — | manquant |

### CATÉGORIE E — Débogage

| Fonction | Composant | Forme | État |
| --- | --- | --- | --- |
| Valeurs en ligne | `ValeurEnLigne` (SPI servie, `FauxDebogueur`) | — | données prêtes, surface manquante |
| Points d'arrêt (pose) | `EtatArret` + gouttière (appui long) | marque inline | implémenté |
| Exception Details | — | — | manquant |

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
| Notifications | `NotificationView` + genre `NOTIFICATION` | carte éphémère au coin bas-droit | implémenté (2026-11-14) |
| Loupe | `Loupe` | bulle in-canvas | implémenté (hors couche) |
| Fantôme IA | `FantomeEditeur` | inlay éphémère in-canvas | implémenté (hors couche) |
| Minimap / Fast scroll / Bande d'erreurs | `MinimapView`/`FastScrollerView`/`ErrorBandView` | chrome bord droit | implémenté (chrome, pas cartes) |

## 3. Bilan chiffré

- 15 genres d'état au contrôleur ; 12 surfaces portées par la couche ;
  3 feuilles modales (+ 9 ateliers de feuilles en démo) ; 3 surfaces
  in-canvas assumées (loupe, fantôme, HUD) ; 2 surfaces interactives
  hors couche (gouttière, poignées).
- Fonctionnalités réellement opérationnelles (tactile) : complétion + doc
  + signature + intentions + survol + peek + info-bulle + fil d'Ariane +
  barre de sélection + débordements + color picker + touchbar + bande +
  minimap + pouce + plan + recherche + réglages + renommage + diff +
  correctifs + IA/agent + `@Preview`.
- Manquants prioritaires (au sens « la donnée existe, la surface manque ») :
  navigation clavier des popups ; fermeture au clic extérieur ;
  usages/references ; hiérarchies ; valeurs de débogage ;
  recherche de symbole/fichier — la notification vit depuis le
  2026-11-14.
- Aucun doublon de MOTEUR réel : les trois voies de complétion
  (`CompletionMots`, `PopupCoach.completionFor`, capacité `Completion` de
  `core-language`) vivent à des étages différents (production, repli de
  repli, contrat non branché) — la capacité `Completion` enregistrée par
  `LangagesDemo` n'est LUE nulle part en production : [5] à décider en
  phase architecture.

## 4. Règles de coexistence constatées (aujourd'hui)

Écrites dans `PopupController` : complétion ⇄ intentions s'excluent ;
`COMPLETION_DOC` exige la liste, fermé avec elle ; `SELECTION_OVERFLOW`
exige la barre, fermé avec elle ; frappe ferme survol + info-bulle +
barre + débordement + color picker ; défilement ferme survol + info-bulle +
color picker, repositionne le reste ; bande/minimap/pouce/touchbar ne
ferment jamais. La barre de recherche ferme sur demande seule. Les feuilles
ignorent le contrôleur (P8). Aucune priorité explicite entre
`INTENTIONS_MENU` caret et débordement toolbar : même genre, même carte —
conflit silencieux (P1).
