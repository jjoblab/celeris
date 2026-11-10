# Registre de portage — du dépôt de départ vers celeris

> Règle du mandat : chaque fichier porté est consigné ici (source, décision,
> tests associés). À M0, ce registre contient le **pré-classement issu de
> l'audit** ; les colonnes « statut » et « commit » se remplissent à chaque
> portage réel (M2+). Un fichier non listé ici ne peut pas être porté.
>
> Le dépôt de départ est traité en lecture seule au commit `f310a39` ; le
> nouveau dépôt n'est pas un fork et ne reprend aucun historique.

## Légende

- **Porter** : réutiliser l'algorithme et ses tests après revue ligne à
  ligne ; l'API d'accueil est celle de celeris (types forts, transactions).
  Portage rope (2026-11-04), adaptations consignées  : paquet
  `jo.celeris.core.text`, contract `@NullMarked` (plus de tolérance
  à `null` en entrée), feuille et branche invisibles hors du paquet,
  `instanceof` à motifs (niveau 17), graines du fuzz fixées pour la
  mutation.
- **Spécifier** : réécrire ; l'ancien code sert de spécification et
  d'oracle de test (les sorties observées deviennent les assertions).
- **Abandonner** : ne pas reprendre.

## Cœur texte (`cel-core` → `core-text`)

| Élément source | Décision | Justification (audit/spike) | Tests associés |
| --- | --- | --- | --- |
| `rope/Rope.java` (395 l., imports `java.util` seuls) | Porter | re-mesuré : 0,002 ms/édition p50 sur 2 M car., mémoire 1,22× | portés et étendus (M2, 2026-11-04) : `RopeTest`, `RopeFuzzTest` (graines fixes reproductibles), `RopeVisitLeavesTest`, `RopeProprietesTest` (jqwik, ~10 300 séquences/oracle `StringBuilder`) |
| index de lignes de `document/` | Porter | voie de mutation unique déjà validée | portés (2026-11-04, tranche 3) : `DocumentIndexTest` (requêtes, splice, bornes clampées, seuils « gros document »), `DocumentIndexProprietesTest` (jqwik, invariants croisés document/index, oracle `StringBuilder` étendu, défaire/refaire compris), benchmark d'édition de référence (50 000 lignes, minimum de trois tours). Adaptations consignées : fabrique intolérante au nul (`@NullMarked`), `subText` strict conservé, tests de paresse par réflexion non repris, vue `CharSequence` différée aux helpers de frappe (M3/M5) |
| `session/EditorSession.java` (1 694 l.) | Spécifier | conforme au plafond 600 l. ; le concept (voie unique, undo par la même voie) est l'acquis, pas la classe | spécification entamée (2026-11-04) : `TextSession` — la voie unique et l'annulation par la même voie (PIEGE-07) ; l'index de lignes, la multi-sélection et les ponts restent à spécifier. Oracle : `UndoPipelineTest` → `undoReplay_goesThroughTransactionPipeline` |
| `session/UndoManager`/`UndoRecorder` (coalescence) | Porter | coalescence mesurée et éprouvée | portés (2026-11-04) : `UndoStack` (horloge injectée, ADR-007 — le moteur n'appelle jamais l'horloge système) et `UndoStep` (copie défensive) ; adaptation consignée : le lot multi-éditions d'une transaction remplace `begin/endBatch`, le dispatch vit dans la session ; tests de coalescence portés et étendus (frontière de délai sur horloge scriptée, coupes de chaîne par undo/lot/remplacement) |
| modèle de sélection (`Selection`, 2 `int`) | Abandonner (remplacer) | mono-curseur par construction ; multi-sélection exigée (A-01) | remplacée (2026-11-04, tranche 5) : `SelRange` (ancre et curseur, direction comprise), `MultiSelection` (tri, fusion aux recouvrements, projection à gravité droite ADR-003), la session porte la sélection et la restitue à l'annulation (A-13) ; tests `MultiSelectionTest`, `TextSessionSelectionTest`, propriétés jqwik `SelectionProprietesTest` (oracle naïf sur texte et sélections empilées) |
| segmentation Unicode (graphèmes, mots) | Spécifier (UAX #29 révision 15) | la norme vit dans le cœur, jamais dans la vue (ADR-003) ; rien à porter du dépôt de départ | spécifiés (2026-11-04, tranche 6) : `Grapheme` (règles GB3-GB999, classes des catégories générales complétées par les plages propres : préfixes, indicateurs régionaux, jamo hangûl, pictogrammes étendus, modificateurs d'emoji) et `Word` (règles WB3-WB999, `WbClassifier`/`WbClass`) ; tests `GraphemeTest` (bornes exactes des plages, ZWJ, ZWNJ, drapeaux, séquences à teinte, contrôles) et `WordTest` (élision française, séparateurs décimaux, kana, tirets bas). Correctif consigné : le regard des séparateurs médians traverse les attaches (WB4). Limites documentées : pictogrammes attribués après U+1FFFD, contractions hébraïques et segmentation par dictionnaire absentes |
| résolution bidirectionnelle | Spécifier (UAX #9, sous-ensemble documenté) | niveaux et réordonnancement exigés pour poser les curseurs et découper les lignes en runs d'affichage (ADR-008) | spécifiée (2026-11-04, tranche 6) : `Bidi` (P2-P3, X1-X8, W1-W7, N1-N2, I1-I2, L1, L2), `BidiClassifier`, `BidiEmbeddings` et `BidiRuns` visibles du paquet, `Bidi.BidiInfo` classe à accesseurs ; tests `BidiTest` (règles faibles W2/W4/W5/W7 et neutres N1 observées par les niveaux, enchâssements et isolats, fuzz à graine fixe : l'ordre visuel est toujours une permutation). Limites documentées : N0 (paires de crochets) non implémentée, contrôles explicites conservés comme bornes neutres, l'entrée est une ligne |
| encodages et BOM | Spécifier | A-24 : détection sans heuristique réseau, décodage strict, BOM préservé | spécifiés (2026-11-04, tranche 6) : `Encoding` (famille), `EncodingCodec` (détection par BOM toutes familles — l'UTF-32 gagne le préfixe partagé avec l'UTF-16 —, heuristique bornée pour l'UTF-16/32 sans BOM, Latin-1 jamais auto-détecté, décodage strict à erreurs par offsets, ré-encodage refusant les substituants isolés, mode de fin de ligne dominant), `Utf8Codec` (machine à états, survols et hors-plage rejetés aux bornes exactes), `WideCodecs` (UTF-16 et UTF-32 des deux boutismes) ; tests `EncodingCodecTest` (allers-retours des points de code extrêmes de chaque longueur de séquence, au-delà du plan multilingue de base) ; la suppression arrière par clusters entiers (`TextSession.deleteBackward`, coalescence des gestes) s'appuie sur `Grapheme` (`TextSessionEditingTest`) ; la suppression avant suit la même discipline (`TextSession.deleteForward`, `ClusterDeletes`, 2026-11-04, tranche 4 M3) |
| recherche/remplacement | Porter | acquis ; regex, casse, mot entier déjà couverts | portés (2026-11-04, tranche 4)  `FindReplaceTest`, `FindReplaceSafetyTest`, `WordOccurrencesTest` (oracle du dépôt de départ), `FindReplaceReplaceTest`, `TextSessionReplaceTest` et `RemplacementProprietesTest` (passe spécifiée du contrôleur, oracle `String.replace`). Adaptations consignées  horloge injectée en millisecondes (ADR-007 — le garde-fou du départ appelait `System.nanoTime` en production), correctif de la casse insensible exotique (minuscule qui change la longueur → repli par comparaison régionale, indices sur le texte original), correspondances de remplacement non recouvrantes, plafond miroir de la collecte sur la passe (la passe regex du départ était non bornée) |
| décalage des diagnostics et annotations | Porter | « pipeline commun » du dépôt de départ, cité par PIEGE-07 | porté (2026-11-04) : contrat `PositionProjector` + `AnchoredRanges` (gravités du pipeline commun, chute des plages vides) ; les types porteurs arrivent avec leurs modules |
| plis, retour à la ligne (modèles purs) | Porter | modèles purs documentés | retour à la ligne pur porté (2026-11-04) : `SoftWrap` à mesure injectée, coupe dichotomique par caractères (PIEGE-08 écrit avant) ; variante code livrée avec la vue (2026-11-04, tranche 3 M3) : coupes par offsets dans `VisualProjection` (budget de colonnes, tranche par rangée, PIEGE-04/08), plis et coupures confondus ; plis purs à venir |
| `ImeBridge` (cœur) | Porter | pont de composition éprouvé par niveau d'API | tests de composition IME |

## Syntaxe (`cel-core/highlight` → `core-syntax`)

| Élément source | Décision | Justification | Tests associés |
| --- | --- | --- | --- |
| 19 tokéniseurs manuels | Abandonner (remplacer par l'arbre, ADR-004) | constat n° 4 : voies séparées, pas d'arbre | corpus de coloration → tests de l'arbre |
| `TokenizerTemplate.java` (2 046 l.), `GenericTokenizer` (1 587 l.) | Abandonner | classes au-delà du plafond, remplies par le socle commun | — |
| voie de grammaires (`cel-textmate`) | Porter (en module `core-syntax-grammars`) | longue traîne (ADR-004) ; **les deux archives binaires versionnées ne sont pas reprises** (redéclarées en dépendances coordonnées, ADR-011) | tests de grammaires existants |
| mapping portées → types de jetons | Porter | contrat stable | tests de mapping |

## Interface (`cel-ui` → `android-view` / `android-widgets`)

| Élément source | Décision | Justification | Tests associés |
| --- | --- | --- | --- |
| `EditorView.java` (4 579 l., 494 membres publics) | Abandonner (refonte totale) | constat n° 2 ; plafond 600 l. ; composition imposée | captures de référence = oracle visuel |
| `render/EditorRenderer.java` | Spécifier | pipeline à couches conservé comme concept ; l'ordre des couches documenté devient la spécification | tests de capture + PIEGE-04 |
| caches de rendu par ligne (`LineRenderCache`) | Porter | acquis (révisions, invalidation) | PIEGE-02 d'abord |
| `EditorShapedLayoutCache` (ligatures) | Spécifier | chemin façonné contre chemin tissé : la dualité est documentée par PIEGE-12 | PIEGE-11/PIEGE-12 |
| `popup/kit/*` (`TextLayoutKit`, `PopupPlacement`, `PopupTokens`, `PopupCard`, `DocRenderModel`) | Porter | logique pure, mesure injectée — modèle du genre | leurs tests (y compris wrap/ellipses : PIEGE-09/10) |
| popups de complétion/signature (rendu) | Spécifier | s'appuient sur le kit porté | tests de placement coordonné |
| gestes (`EditorInputHandler`, `EditorSelectionGestures`, `EditorTapResolver`) | Spécifier | machine à états explicite exigée (PIEGE-03) ; l'actuel sert de spécification | tests gestuels instrumentés |
| `EditorWrapGeometry` (wrap/plis ↔ Y) | Porter | géométrie pure éprouvée | tests de conversion rangées ↔ lignes (PIEGE-04) |
| `BreadcrumbBar`, `EditorSearchBar` (a11y partielle) | Spécifier | seuls fichiers avec `contentDescription` — la vue principale doit rattraper (ADR-008) | auditeur d'accessibilité |

## Sémantique (`cel-lsp*` → `core-lsp` / `core-language`)

| Élément source | Décision | Justification | Tests associés |
| --- | --- | --- | --- |
| contrats `Language` et providers (`cel-lsp-api`) | Porter | acquis cité par le mandat | tests de contrat |
| client LSP sur LSP4J | Abandonner (client propre, ADR-005) | 0.22.0 figé par rupture de contrat ; 8 283 l. à remplacer par une surface bornée | traces de serveurs réels → corpus de tests de contrat |
| `LanguageServerWrapper`, `LspEditor` | Spécifier | cycle de vie documenté (déconnexion, resynchronisation) = spécification | `LspDisconnectLifecycleTest` → oracle |
| `MiniJava*` (démo, 22 fichiers, 9 301 l.) | Abandonner | hors bibliothèque ; remplacé par l'arbre (M4) puis l'index (M6) | — |

## Données de banc (M7.6, H-05)

| Élément | Décision | Justification | Tests associés |
| --- | --- | --- | --- |
| grammaires TextMate du commerce (13 fichiers `*.tmLanguage.json`, extensions VS Code, MIT) | Reprendre inchangées | le banc de conformité du commerce prouve le chargement SANS MODIFICATION (H-05) ; ressources de test uniquement, hors artefacts publiés ; provenance épinglée (main du 2026-11-08, sommes au spike) | `GrammairesCommerceTest` |
| grammaire HTML (`html.tmLanguage.json` du dépôt microsoft/vscode, MIT) | Livrer en paquet intégré (chantier C5 : la première grammaire du commerce livrée) | le paquet `langages/html` la reprend SANS MODIFICATION — le banc de conformité du paquet verrouille l'égalité octet pour octet avec la copie épinglée du banc, l'acceptation tient le seuil (98,7 %), l'échantillon se rejoue avec déterminisme ; licence MIT au NOTICE, somme SHA-256 épinglée au NOTICE et ici : `80dedf4fb27e88889ac8fb72763954a6d2660502c686f4415208d8c8d00352cd` | `PaquetHtmlIntegreTest` |
| grammaire CSS (`css.tmLanguage.json` du dépôt microsoft/vscode, MIT) | Livrer en paquet intégré (chantier C5 : la famille web du commerce) | le paquet `langages/css` la reprend SANS MODIFICATION — le banc de conformité du paquet verrouille l'égalité octet pour octet avec la copie épinglée du banc, l'acceptation tient le seuil (96,6 %), l'échantillon se rejoue avec déterminisme ; licence MIT au NOTICE, somme SHA-256 épinglée au NOTICE et ici : `55a3e184f702da6a5a64dc4d7ce82ab298a1bae8d35e2924483941eb223d54b1` | `PaquetCssIntegreTest` |
| grammaire JavaScript (`JavaScript.tmLanguage.json` du dépôt microsoft/vscode, MIT) | Livrer en paquet intégré (chantier C5 : la famille web du commerce) | le paquet `langages/javascript` la reprend SANS MODIFICATION — le banc de conformité du paquet verrouille l'égalité octet pour octet avec la copie épinglée du banc, l'acceptation tient le seuil (90,3 %), l'échantillon se rejoue avec déterminisme ; licence MIT au NOTICE, somme SHA-256 épinglée au NOTICE et ici : `db6f17f15bc4f5e860a3b8fa6055a69720a53df845c8d5121cdc4f128c16291f` | `PaquetJavascriptIntegreTest` |
| grammaire TypeScript (`TypeScript.tmLanguage.json` du dépôt microsoft/vscode, MIT) | Livrer en paquet intégré (chantier C5 : la famille web du commerce) | le paquet `langages/typescript` la reprend SANS MODIFICATION — le banc de conformité du paquet verrouille l'égalité octet pour octet avec la copie épinglée du banc, l'acceptation tient le seuil (90,2 %), l'échantillon se rejoue avec déterminisme ; licence MIT au NOTICE, somme SHA-256 épinglée au NOTICE et ici : `4e92e0d7de560217d6c8d3236d85e6e17a5d77825b15729a230c761743122661` | `PaquetTypescriptIntegreTest` |
| grammaire PHP (`php.tmLanguage.json` du dépôt microsoft/vscode, MIT) | Livrer en paquet intégré (chantier C5 : la longue traîne) | le paquet `langages/php` la reprend SANS MODIFICATION — le banc de conformité du paquet verrouille l'égalité octet pour octet avec la copie épinglée du banc, l'acceptation tient le seuil (96,4 %), l'échantillon se rejoue avec déterminisme ; licence MIT au NOTICE, somme SHA-256 épinglée au NOTICE et ici : `dea92689034f878009c556d59abef08a16e0995ad423ac8ffe93fad78d065c1c` | `PaquetPhpIntegreTest` |
| grammaire Ruby (`ruby.tmLanguage.json` du dépôt microsoft/vscode, MIT) | Livrer en paquet intégré (chantier C5 : la longue traîne) | le paquet `langages/ruby` la reprend SANS MODIFICATION — le banc de conformité du paquet verrouille l'égalité octet pour octet avec la copie épinglée du banc, l'acceptation tient le seuil (98,9 %), l'échantillon se rejoue avec déterminisme ; licence MIT au NOTICE, somme SHA-256 épinglée au NOTICE et ici : `050632ddb85d98e2fa9db029951334575e26f6fcca2083675398f6a241605f98` | `PaquetRubyIntegreTest` |

## Refactorisations — chantier C13 (travaux neufs, l'ancien ne porte pas la source)

| Élément | Décision | Justification | Tests associés |
| --- | --- | --- | --- |
| mots à bosses de chameau (A-04) | Spécifier | l'ancien ne la porte pas (« non vérifié » à l'audit) ; l'étalon est la matrice Android Studio — trois règles de bossage raffinent les frontières UAX #29, la base normative reste entière | `CamelWordTest` (critère de la matrice mot pour mot, BiDi et CJK), `CaretsMotionTest` (déplacement et sélection) |
| extraire — les cinq variantes (B-20) | Spécifier | l'ancien ne le porte pas (renommage et occurrences seuls) ; l'étalon est la matrice — le plan se construit sur l'arbre (méthode et classe porteuses), le type lexical sur la forme du littéral, refus par plan nul ; les variantes méthode et paramètre étendent la même règle par l'analyse des flux — les références libres se résolvent aux déclarations de la porteuse et aux champs (le lexeur soude les jetons, les textes se décollent), les types se lisent sur la forme (promotion numérique, concaténation), les appels du document s'apparient par parenthèses ; le lu, jamais le deviné, décide | `ExtractionTest` (cinq variantes sur corpus Java : application, texte résultant, annulation, refus — hors méthode, nom inconnu, `var` illisible, local aux appels, surcharge, récepteur ambigu) |

## Outillage

| Élément source | Décision | Justification |
| --- | --- | --- |
| workflows CI épinglés par SHA + validation du wrapper | Porter | acquis cité par le mandat |
| catalogue de versions unique | Porter | acquis (commentaires de cliquet inclus) |
| contrôles de langue / références externes | Réécrire en plus large | l'audit mesure 115 mentions externes dans la doc du dépôt de départ : preuve que le contrôle doit couvrir tous les fichiers |
| baselines lint (48 entrées) | Abandonner | zéro baseline dès le premier commit (ADR-011) |

## Avancement

| Catégorie | Fichiers portés | Restants |
| --- | --- | --- |
| Cœur texte | `rope/Rope.java` + 3 fichiers de tests (2026-11-04) ; `session/UndoManager`/`UndoRecorder` → `UndoStack`/`UndoStep`, session de texte spécifiée (`TextSession`, `Document`, `Transaction`, projecteurs de positions) et wrap pur (`SoftWrap`), avec leurs tests (2026-11-04, tranche des pièges) ; index de lignes porté dans `Document` avec mémo du texte, requêtes et splice incrémental (2026-11-04, tranche 3) ; recherche/remplacement porté (`FindReplace` à horloge injectée, `FindOptions`, `Match`, `WordOccurrences`, vue garde-fou, passe de remplacement et `TextSession.replaceAll`) avec ses tests (2026-11-04, tranche 4) ; multi-sélection (`SelRange`, `MultiSelection`, sélection portée par la session et restituée à l'annulation, A-13) avec ses tests (2026-11-04, tranche 5) ; segmentation UAX #29 (`Grapheme`, `Word`), résolution UAX #9 (`Bidi` et ses classes de paquet), encodages et BOM (`Encoding`, `EncodingCodec`, `Utf8Codec`, `WideCodecs`), suppression arrière par clusters, scénarios de démo M2 au catalogue (2026-11-04, tranche 6) ; jalon M2 clos ; IME réécrit en classes pures — `CompositionState` (machine de composition alignée aux clusters), `SurroundingTexts` (fenêtres et suppressions d'entourage), `CaretReveal`, colle `EditorInputConnection` sur la surface 26→36 — avec ses tests (2026-11-05, tranche 5 M3) | plis |
