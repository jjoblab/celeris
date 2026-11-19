# Registre des renommages — le code passe à l'anglais

> Registre vivant de la refonte 2.3.0 (session 16, 2026-11-19) : la
> règle du dépôt redevient ce qu'elle a toujours dû être — **le code
> (classes, méthodes, champs, constantes) en ANGLAIS, la Javadoc et
> les commentaires en FRANÇAIS**. La dette héritée (82 types, 551
> méthodes, 298 champs français recensés) se rembourse ici, module
> par module, avant les chantiers P de restructuration des packages
> (le pilote P1 l'avait prouvé : publiciser des noms français
> contrevient à la règle).

## Règles appliquées

1. **Le code seul** : les identifiants se renomment par mot entier
   (`\b`) dans le code HORS commentaires et HORS chaînes littérales.
2. **Les Javadoc suivent le code, pas le texte** : seules les
   références `{@link X}`, `{@value X}`, `@see X`, `{@code X}`,
   `@param x`, `@throws X` se réécrivent — le texte français des
   commentaires reste INTACT (c'est la règle du dépôt).
3. **Les chaînes ne se touchent jamais** : messages, JSON de test,
   noms de portées TextMate — un audit systématique
   (`scripts/audit-chaines.py`) compare les littéraux avant/après
   et tout dénaturé se restaure.
4. **Collisions avec les bibliothèques** : le suffixe `Lsp` du dépôt
   désambiguïse (`ServerCapsLsp`, `CompletionListLsp` contre les
   `org.eclipse.lsp4j.*` du même nom) ; `notify()` d'`Object` a
   donné `notifyListeners()` ; les collisions internes se dénouent
   par un nom distinct (`applyPalette`, `settingsPanel`).
5. **Les manifestes `api/*.api` se régénèrent** (`apiManifest
   --rerun-tasks`) — l'API publique du dépôt devient anglaise ;
   MIGRATION.md porte chaque avant/après côté hôte.
6. **La baseline langue descend** : `language-baseline.txt` tombe de
   « sample 17 » à « sample 13 » (le reste de la dette visible par
   le rempart est déjà remboursé ; le dictionnaire du rempart sera
   renforcé en P2 pour les noms composés qu'il ne voyait pas).

## La table des renommages, par module

### core-lsp (API publique — manifeste régénéré)

| Ancien | Nouveau |
| --- | --- |
| `CapacitesServeur` | `ServerCapsLsp` |
| `RequeteAnnuleeException` (+ `Raison` : `DEVANCEE`/`PERIMEE`) | `RequestCancelledException` (`Reason` : `SUPERSEDED`/`STALE`) |
| `PositionsLsp` | `LspPositions` |
| `EditsEspaceLsp` | `WorkspaceEditsLsp` |
| `AuditeurDiagnostics` | `DiagnosticsListener` |
| `SuiviVersions` | `VersionTracker` |
| `OrdonnanceurLsp` (+ `Cle`, `CleFusion`, `Priorite`) | `LspScheduler` (`Key`, `FusionKey`, `Priority`) |
| `DocModificateurs` | `ModifierDocs` |
| `CorrectifLsp` / `EditionTexteLsp` / `EditionDocumentLsp` / `EditionEspaceLsp` / `EditionGabaritLsp` | `CodeActionLsp` / `TextEditLsp` / `DocumentEditLsp` / `WorkspaceEditLsp` / `TemplateEditLsp` |
| `IndiceLsp` (+ `Partie`) / `LieuLsp` / `ListeCompletion` | `InlayHintLsp` (`Part`) / `LocationLsp` / `CompletionListLsp` |
| `ElementCompletion` / `OptionsFormatageLsp` / `PliLsp` / `PlageLsp` / `PositionLsp` | `CompletionItemLsp` / `FormatOptionsLsp` / `FoldLsp` / `RangeLsp` / `LspPosition` |
| `PreparationRennomageLsp` / `SurlignageLsp` / `SurvolLsp` / `SymboleLsp` | `RenamePrepareLsp` / `HighlightLsp` / `HoverLsp` / `SymbolLsp` |
| composants : `plage`, `debut`, `fin`, `edition(s)`, `nouveauTexte`, `texteTri/Filtre/Insertion`, `libelle`, `gravite`, `genre`, `ligne(Début/Fin)`, `colonne`, `longueur`, `largeurTab`, `jeton(s)` | `range`, `start`, `end`, `edit(s)`, `newText`, `sortText/filterText/insertText`, `label`, `severity`, `kind`, `startLine/endLine`, `column`, `length`, `tabWidth`, `token(s)` |
| méthodes : `vides()`, `sertXxx()`, `estIncomplete()`, `filtrer()`, `trier()`, `versionDe()`, `oublier()`, `lire()`, `sur()` | `empty()`, `supportsXxx()`, `isIncomplete()`, `filter()`, `sort()`, `versionOf()`, `forget()`, `read()`, `on()` |
| records imbriqués : `Entree`, `Parametre` | `Entry`, `Parameter` |

### lsp-protocole (API publique — manifeste régénéré)

| Ancien | Nouveau |
| --- | --- |
| `ServeurEmbarque` (+ `Documents`) / `ServeurExterne` | `EmbeddedServer` (`Docs`) / `ExternalServer` |
| `ServeurDocuments` (+ `ServiceDocuments`) / `ReceiverServeur` | `DocumentsServer` (`DocumentsService`) / `ServerReceiver` |
| `TubesMemoire` | `MemoryPipes` |
| `CorrespondanceLsp/Documents/Espace/Couverture` | `LspMapping` / `DocumentsMapping` / `WorkspaceMapping` / `CoverageMapping` |
| `RequetesCouverture` / `EcritureDocuments` | `CoverageRequests` / `DocumentsWriting` |
| `sur()` / `monter()` / `parStdio()` / `parSocket()` / `creer()` | `on()` / `start()` / `viaStdio()` / `viaSocket()` / `create()` |
| `ouvrir` / `changer` / `fermer` / `versionDe` / `diagnosticsDe` / `ajouterAuditeurDiagnostics` | `open` / `change` / `close` / `versionOf` / `diagnosticsOf` / `addDiagnosticsListener` |
| `capacites()` / `initialiser()` / `formatagePlage()` / `plis()` / `plagesSelection()` / `jetons(Plage)` / `rennomage()` / `surlignages()` / `symboles()` / `survol()` / `indices()` | `capabilities()` / `initialize()` / `rangeFormatting()` / `folds()` / `selectionRanges()` / `tokens(Range)` / `rename()` / `highlights()` / `symbols()` / `hover()` / `inlayHints()` |
| les soixantes `lireXxx`/`ecrireXxx` (mapping lsp4j ↔ domaine) | `readXxx` / `writeXxx` (`readCapabilities`, `writeRange`, `readDiagnostics`, …) |

### core-syntax (+ grammars, treesitter) (API publique — manifestes régénérés)

| Ancien | Nouveau |
| --- | --- |
| `MoteurAnalyse` (+ `Tache`) / `MoteurSyntaxe` | `AnalysisEngine` (`Task`) / `SyntaxEngine` |
| `MoteurNatif` / `EtatNatif` (treesitter) | `NativeEngine` / `NativeState` |
| `AnalyseAnnulee` | `AnalysisCancelled` |
| `Corps` (clike) | `BodyParser` |
| `Grammaire` / `Grammaires` / `GrammaireLexeur` / `GrammaireParseur` | `Grammar` / `Grammars` / `GrammarLexer` / `GrammarParser` |
| `GrammaireKind` / `GrammaireSyntax` / `RapportGrammaire` / `LecteurGrammaire` | `GrammarKind` / `GrammarSyntax` / `GrammarReport` / `GrammarReader` |
| `EtatGrammaire` / `GardeCatastrophique` / `MotsClesGrammaire` | `GrammarState` / `CatastrophicGuard` / `GrammarKeywords` |
| `Regle` / `RegleBloc` / `RegleListe` / `RegleSimple` / `BlocOuvert` | `Rule` / `BlockRule` / `ListRule` / `SimpleRule` / `OpenBlock` |
| `ajouterAuditeur` / `retirerAuditeur` / `fenetre` / `analyseFenetre` / `estAnnule` | `addListener` / `removeListener` / `window` / `analyzeWindow` / `isCancelled` |
| `notifier` (moteur natif) | `notifyListeners` (collision `Object.notify()` évitée) |
| `lignesEnErreur` / `chercherAccolade` / `texteCrochet` / `estTrivia` | `linesInError` / `findBrace` / `bracketText` / `isTrivia` |

### android-host (façade de l'hôte — la table de l'ADR-0023 appliquée)

| Ancien | Nouveau |
| --- | --- |
| `PopupsEditeur` | `EditorPopups` |
| `DiagnosticHote` / `PlageLignes` / `ValeurEnLigne` / `IndicesInlay` | `HostDiagnostic` / `LineRange` / `InlineValue` / `InlayHints` |
| `AnalyseurPile` / `CadrePile` / `BordDroit` / `EtatExecution` / `EtatLignesVcs` | `StackAnalyzer` / `StackFrame` / `RightEdge` / `RunState` / `VcsLinesState` |
| `MemoireSystemeFichiers` / `MoteurInlayHints` / `Reglages` / `SelecteurCouleur` | `InMemoryFileSystem` / `InlayHintsEngine` / `SettingsPanel` / `ColorPickerLogic` |
| `brancherServeur` / `detacher` / `setLangue` / `setPlan` / `setNomFichier` | `connectServer` / `detach` / `setLanguage` / `setOutline` / `setFileName` |
| `ouvrirRecherche` / `fermerRecherche` / `basculerCompletion` / `ouvrirIntentions` / `ouvrirSurvol(…AuDoubleTap)` | `openSearch` / `closeSearch` / `toggleCompletion` / `openIntentions` / `openHover(…OnDoubleTap)` |
| `ouvrirGabarits` / `ouvrirHierarchie` / `ouvrirReglages` / `ouvrirTypeInfo` / `ouvrirUsages` / `ouvrirDocExterne` / `ouvrirColorPicker` | `openTemplates` / `openHierarchy` / `openSettings` / `openTypeInfo` / `openUsages` / `openExternalDoc` / `openColorPicker` |
| `configurerIntentions` / `configurerActionsSelection(Vives)` / `configurerGabaritMethode` / `configurerRelanceLint` | `setIntentions` / `setSelectionActions(Live)` / `setMethodTemplateGesture` / `setLintRerun` |
| `surTouche` / `surFenetre` / `surTailleTexte` / `supprimerSur` / `deplacerDans` / `masquerInspection` / `executerFichier` | `onKey` / `onWindow` / `onTextSize` / `deleteIn` / `moveInto` / `hideInspection` / `runFile` |
| `rafraichir*(Montage/BarreSelection/Palette/Reglages/Signature)` | `refresh*(Mount/SelectionBar/Palette/Settings/Signature)` |
| `poserLsp` (LanguageSession) | `setDiagnosticsListener` (le pont sample suit) |

### android-widgets

| Ancien | Nouveau |
| --- | --- |
| `PopupClavier` / `ReglagesView` / `Touche` / `TypeLigne` / `EditListenerPont` | `KeyboardPopup` / `SettingsSheet` / `KeyCap` / `LineKind` / `EditListenerBridge` |
| `fermerSousVoile` / `fermerTout` / `fermerDebordement` / `fermerParExclusion` / `ouvrirDebordement` | `closeSubsheet` / `closeAll` / `closeOverflow` / `closeByExclusion` / `openOverflow` |
| `surTouche` / `surFrappe` / `surRetour` / `surDefilement` / `surToucheCarte` / `surToucheHorsCartes` / `setSurToucheHorsCartes` | `onKey` / `onType` / `onBack` / `onScroll` / `onCardTouch` / `onTouchOutsideCards` / `setOnTouchOutsideCards` |
| `montrer` / `cacher` / `retour` / `retourAuPointDeControle` / `glisser` | `show` / `hide` / `back` / `backToCheckpoint` / `drag` |
| les 25 `dessinerXxx` / `tracerXxx` / `mesurerPastilles` | `drawXxx` / `traceXxx` / `measureChips` |
| `poserBarreTouches` / `poserChrome` / `poserContour` / `poserCorps` / `poserEncarts` / `poserPalette` / `poserTaille` / `poserVueMesuree` | `setKeyRow` / `setChrome` / `setOutline` / `setBody` / `setInsets` / `setPalette` / `setSize` / `setMeasuredView` |
| ~60 constantes `HAUTEUR_DP`, `ECART_DP`, `ENCRE_*`, `LARGEUR_*`… | `HEIGHT_DP`, `GAP_DP`, `*_INK`, `WIDTH_*`… |

### android-view

| Ancien | Nouveau |
| --- | --- |
| `AccoladesMiroir` / `BandeLigneCourante` / `ClasseLargeur` / `CouchesDuTexte` | `BraceMirror` / `CurrentLineBand` / `WidthClass` / `TextLayers` |
| `EtatBreakpoint` / `ExecuteurAnalyse` / `GlisserDeposer` / `IndiceInlay` | `BreakpointState` / `AnalysisExecutor` / `DragDrop` / `InlayHintMark` |
| `Loupe` / `PaireAccolades` / `ParcMoteurs` / `ParcRangees` | `Magnifier` / `BracePair` / `EnginePark` / `RowPark` |
| `PastilleCouleur/Source` / `PastillesCouleur/Texte` | `ColorChip` / `SourceChip` / `ColorChips` / `TextChips` |
| `PlisParSession` / `PressePapiersClavier` / `RappelsHote` / `ReglagesEditeur` | `FoldsBySession` / `KeyboardClipboard` / `HostCallbacks` / `EditorSettingsPanel` |
| `ReperePreview/Source` / `ReperesPreview` / `ValeurLigne` / `ValeursPainter` | `PreviewMark` / `SourceMark` / `PreviewMarks` / `LineValue` / `ValuesPainter` |
| `setNumerosVisibles` / `setClasseLargeur` / `premiereLigneVisible` / `marquesGouttiere` / `poserPolice` | `setLineNumbersVisible` / `setWidthClass` / `firstVisibleLine` / `gutterMarks` / `setFont` |
| `setCoulisse*` / `setFantome` / `setIndicesInlay` / `setPastilles` / `setReperesPreview` / `setSurClicLigne` / `setValeursEnLigne` | `set*Lane` / `setGhost` / `setInlayHints` / `setChips` / `setPreviewMarks` / `setOnLineClick` / `setInlineValues` |
| `peindreRangee` / `peindrePlage` / `remplirRangee` / `soulignerRangee` / `dessinerImage` | `paintRow` / `paintRange` / `fillRow` / `underlineRow` / `drawImage` |
| `attacher` / `episser` / `habiller` / `recalage` / `bornerDefilement` / `chasserAuPlafond` | `attach` / `splice` / `wrap` / `realignment` / `clampScroll` / `evictAtCeiling` |
| ~50 champs et constantes (`gouttiere`, `coulisse(s)`, `pinceau(x)`, `police`, `curseur`, `numeros`, `arrets`, `PLAFOND`, `SOMBRE`…) | `gutter`, `lane(s)`, `brush(es)`, `font`, `cursor`, `numbers`, `breakpoints`, `CEILING`, `DARK`… |

### core-text, core-ai, core-language, core-intelligence, sample, cli

| Ancien | Nouveau |
| --- | --- |
| `notifierSelection` / `validerBornes` / `frappeSur` (core-text) | `notifySelection` / `validateBounds` / `keystrokeOn` |
| `MARQUES` / `ENCODINGS_DE_MARQUE` / `LIMITE_GROS_LIGNES` | `MARKS` / `MARK_ENCODINGS` / `BIG_LINE_LIMIT` |
| `accepterMot/Ligne/Jusqua/Tout` / `refuser` / `couperEnLignes` / `decouperEnBlocs` (core-ai) | `acceptWord/Line/Until/All` / `reject` / `cutIntoLines` / `splitIntoBlocks` |
| `fantome` / `frappe` / `requetes` / `retourAuPointDeControle` | `ghost` / `keystroke` / `requests` / `backToCheckpoint` |
| `PackFiles`, `compilerMotifs` / `pairesAuto` / `pairesCrochets` / `serveursDe` / `tailleIndentation` (core-language) | `compilePatterns` / `autoPairs` / `bracketPairs` / `serversOf` / `indentSize` |
| `EtatChaine` / `Geste` (records internes) | `StringState` / `TypingGesture` |
| `Echantillon(s)` / `Panier` / `SessionLspPoseuse` / `MiniJavaServeur` (sample) | `Sample(s)` / `Basket` / `LspSessionMount` / `MiniJavaServer` |
| `monterKit/Journal/Tiroir` / `poserLsp` / `rafraichirOnglets` / `fermerOnglet` / `sonderLangue` (sample) | `mountKit/Journal/Drawer` / `mountLsp` / `refreshTabs` / `closeTab` / `probeLanguage` |

## Ce qui reste (residuel mesuré : 6 types, 60 méthodes, 71 champs)

- **6 types français restants** : ce sont des classes de BANC
  (`MiniLangage` du banc core-syntax, `ServeurFactice` de
  lsp-protocole, `FabriqueVoieBanc` d'android-view…) et les
  internes déjà convergents — ils se renommeront avec le chantier P
  (les bancs suivent les modules qu'ils testent).
- **Le dictionnaire du rempart** (`check-language-baseline`) sera
  renforcé en P2 : il ne voyait pas les noms composés ni les
  minuscules — la baseline « sample 13 » est le VRAI reste de la
  dette telle que le rempart la mesure.

## Journal

- 2026-11-19 (session 16) : la passe complète — 8 manifestes api
  régénérés, tous les modules recompilés et testés (584 tests
  android-view, 74 lsp-protocole, tous les cores verts), chaînes
  littérales auditées et restaurées (textblocks de grammaires,
  JSON de bancs), collisions lsp4j dénouées par le suffixe `Lsp`,
  `Object.notify()` évité, suppressions checkstyle suivies
  (`EditorPopups`).
