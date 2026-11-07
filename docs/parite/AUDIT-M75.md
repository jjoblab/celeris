# Audit de la matrice de parité — M7.5 (2026-11-07)

Le nouveau cadre de statut (M7.5, §11) : **Non démarré** (rien dans
le code), **Noyau** (modèle pur testé, aucune vue), **Câblé** (vue et
événements en place, au moins un test de vue cité), **Démontré**
(scénario de démo jouable avec auto-vérification — la surcharge de la
démo n'existe pas encore, aucune ligne ne l'atteint), **Vérifié
appareil** (test d'instrumentation vert — aucun n'existe encore).

L'audit est MÉCANIQUE : le statut se déduit des citations vérifiables
(test de vue citant la ligne, scénario de démo citant la ligne), pas
des affirmations des jalons précédents. La baisse de « livré » est le
but de l'exercice : un modèle pur sans vue n'a jamais été livré.

## Décomptes

| Statut | Avant (affirmé) | Après (prouvé) |
| --- | --- | --- |
| Livré / couvert (affirmé) | 63 | 0 |
| Autres (planifié, partiel…) | 47 | 0 |
| Non démarré (prouvé) | — | 17 |
| Noyau (prouvé) | — | 54 |
| Câblé (prouvé) | — | 39 |
| Démontré (prouvé) | — | 0 |
| Vérifié appareil (prouvé) | — | 0 |
| **Total** | 110 | 110 |

## Le détail, ligne à ligne

| Ligne | Statut affirmé avant | Statut prouvé | Test cité | Scénario de démo |
| --- | --- | --- | --- | --- |
| A-01 | absent (au départ ; celeris : pilule tactile livrée M3-t4 — … | Câblé | ChromePainterTest | oui |
| A-02 | absent (celeris : livré M5-t3 — plage par rangée aux bornes … | Câblé | ColumnSelectionTest | oui |
| A-03 | partiel (mot courant) (celeris : livré M5-t3 — chaîne mot → … | Noyau | TreeSelectionTest | oui |
| A-04 | non vérifié | Non démarré | — | non |
| A-05 | présent (celeris : moteur de plans livré M5-t1 — paire insér… | Noyau | FrappePairesTest | oui |
| A-06 | présent (celeris : livré M5-t1 — retrait lu dans l'arbre au … | Noyau | EntreeIndentationTest | oui |
| A-07 | non vérifié (celeris : livré M5-t1 — paire et citations vide… | Noyau | RetourArriereTest | oui |
| A-08 | non vérifié (celeris : livré M5-t1 — paire en ligne et bloc … | Noyau | EntourerTest | oui |
| A-09 | non vérifié (celeris : livré M5-t1 — première ligne inchangé… | Noyau | CollageReindentationTest | oui |
| A-10 | non vérifié (celeris : livré M5-t2 — bloc de lignes touchées… | Noyau | ActionsLignesTest | oui |
| A-11 | présent (celeris : livré M5-t2 — préfixe par langage, tout o… | Noyau | CommenterTest | oui |
| A-12 | absent (celeris : livré M5-t2 — tri naturel « item2 » < « it… | Noyau | TriLignesTest | oui |
| A-13 | présent (coalescence) | Câblé | RetourSoupleCrashTest | oui |
| A-14 | absent | Non démarré | — | non |
| A-15 | présent (au départ ; celeris : régions calculées depuis l'ar… | Câblé | GutterMarksTest | oui |
| A-16 | absent (celeris : livré M5-t6 — en-têtes englobants du même … | Câblé | StickyLinesTest | oui |
| A-17 | présent | Non démarré | — | non |
| A-18 | présent | Câblé | GutterPainterTest | oui |
| A-19 | non vérifié (celeris : livré M5-t6 — finale, tabulation et f… | Câblé | InvisibleCharsTest | oui |
| A-20 | présent (celeris : moteur livré M2 ; barre — état, options, … | Câblé | SearchBarModelTest | oui |
| A-21 | absent | Non démarré | — | non |
| A-22 | présent, gabarits riches (celeris : livré M5-t2 — trous tabu… | Noyau | GabaritTest | oui |
| A-23 | présent (bancs démo) | Non démarré | — | non |
| A-24 | partiel | Noyau | EncodingCodecTest | oui |
| A-25 | partiel (première image JVM : 154 ms pour 100 000 lignes ; a… | Câblé | GrosFichiersTest | oui |
| A-26 | absent | Non démarré | — | non |
| B-01 | présent (19 tokéniseurs) (au départ ; celeris : coloration p… | Câblé | SyntaxPaletteTest | oui |
| B-02 | présent (tir sémantique) | Noyau | ScenarioCatalogTest | oui |
| B-03 | présent | Câblé | RetourSoupleCrashTest | oui |
| B-04 | présent | Noyau | ScenarioCatalogTest | oui |
| B-05 | présent | Noyau | ImportAutoTest | oui |
| B-06 | absent | Noyau | SquelettesMethodeTest | oui |
| B-07 | présent | Câblé | SignatureAideModelTest | oui |
| B-08 | présent (inlay hints réparés) | Noyau | ScenarioCatalogTest | oui |
| B-09 | présent | Câblé | CompletionDocModelTest | oui |
| B-10 | partiel (LSP) | Noyau | IndexSymbolesTest | oui |
| B-11 | absent | Noyau | IndexSymbolesTest | oui |
| B-12 | absent | Non démarré | — | non |
| B-13 | partiel | Noyau | IndexSymbolesTest | oui |
| B-14 | absent (celeris : livré M5-t4 — pile bornée, la visite coupe… | Câblé | NavigationHistoryTest | oui |
| B-15 | absent (celeris : livré M5-t4 — bascule, sauts circulaires, … | Câblé | BookmarkModelTest | oui |
| B-16 | absent (au départ ; celeris : livré M4-t8 — le pont session … | Câblé | EditorSyntaxPlanTest | oui |
| B-17 | partiel (quick fixes) | Noyau | EditsEspaceDemoTest | oui |
| B-18 | absent | Non démarré | — | non |
| B-19 | absent | Noyau | RenommageTest | oui |
| B-20 | absent | Non démarré | — | non |
| B-21 | absent | Non démarré | — | non |
| B-22 | absent | Non démarré | — | non |
| B-23 | absent | Non démarré | — | non |
| B-24 | présent (démo) | Noyau | GenerateursTest | oui |
| B-25 | partiel | Noyau | FormateurTest | oui |
| B-26 | absent | Noyau | EditorConfigTest | oui |
| B-27 | présent | Noyau | OptimiseurImportsTest | oui |
| C-01 | présent (au départ ; celeris : arbre, plis et coloration XML… | Noyau | ScenarioCatalogTest | oui |
| C-02 | livré (celeris : M7-t4 — schéma embarqué, le contexte du cur… | Noyau | CompletionsXmlTest | oui |
| C-03 | livré (celeris : M7-t4 — la fermeture étrangère porte sa pla… | Noyau | XmlValidationTest | oui |
| C-04 | livré (celeris : M7-t4 — définition par balise de valeurs ou… | Noyau | ReferencesRessourcesTest | oui |
| C-05 | livré (celeris : M7-t8 — les quatre formes Android résolues,… | Câblé | GutterPainterTest | oui |
| C-06 | livré (celeris : M7-t4 — attributs des balises du manifeste … | Noyau | CompletionsManifesteTest | oui |
| C-07 | livré (KTS tranche 4, Groovy tranche 7 — chunks par appel de… | Noyau | GroovyLexeurTest | oui |
| C-08 | livré (celeris : M7-t4 — sections lues, accès suggérés : tir… | Noyau | CatalogueVersionsTest | oui |
| C-09 | livré (celeris : M7-t7 — grammaire des règles, jokers et blo… | Noyau | ValidationReglesTest | non |
| C-10 | absent | Non démarré | — | non |
| C-11 | livré (celeris : M7-t8 — chaque annotation ancre son losange… | Câblé | GutterPainterTest | oui |
| C-12 | livré (celeris : M7-t6 — la sortie texte de lint se converti… | Câblé | GutterMarksTest | oui |
| C-13 | absent | Non démarré | — | non |
| C-14 | Java partiel (démo) (au départ ; celeris : les trois analyse… | Noyau | FichiersExempleTest | oui |
| C-15 | livré (Gradle KTS et Groovy tranches 4 et 7, JSON tranche 3 … | Noyau | FichiersExempleTest | oui |
| C-16 | partiel (tokéniseurs au départ ; celeris : la voie de gramma… | Noyau | FichiersExempleTest | oui |
| D-01 | présent | Câblé | GutterPainterTest | oui |
| D-02 | présent (au départ ; celeris : marque d'erreur par ligne dan… | Câblé | GutterPainterTest | non |
| D-03 | livré (celeris : peinture M7-t5 — barre au bord gauche de la… | Câblé | MarquesIntegrationTest | oui |
| D-04 | livré (celeris : peinture M7-t5 — carré plein posé, carré cr… | Câblé | MarquesIntegrationTest | oui |
| D-05 | livré (celeris : peinture M7-t5 — flèche de ligne courante à… | Câblé | MarquesIntegrationTest | oui |
| D-06 | absent (couvert par B-15) | Noyau | ScenarioCatalogTest | oui |
| D-07 | présent (au départ ; celeris : chevrons replié/déplié et bas… | Câblé | GutterPainterTest | non |
| D-08 | absent (celeris : livré M5-t5 — densité bornée à un cran de … | Câblé | ErrorBandModelTest | oui |
| D-09 | présent (celeris : livré M5-t5 — chemin du symbole depuis le… | Câblé | BreadcrumbModelTest | oui |
| D-10 | présent (kit unifié) (celeris : placement coordonné livré M5… | Câblé | SignatureAideModelTest | oui |
| D-11 | livré (celeris : M7-t9 — le plan du fichier sert d'index, l'… | Câblé | ApercuDefinitionTest | oui |
| D-12 | livré (celeris : M7-t9 — DiffBlocs découpe par la plus longu… | Câblé | DiffBlocsTest | oui |
| D-13 | partiel | Câblé | SyntaxPaletteTest | oui |
| D-14 | présent | Non démarré | — | oui |
| D-15 | présent (celeris M3-t4 : paliers bornés, point sous le doigt… | Câblé | KeyboardZoomTest | oui |
| D-16 | présent | Noyau | ScenarioCatalogTest | oui |
| D-17 | partiel (celeris M3-t4 : table complète, remappage, encodage… | Câblé | CaretsMotionTest | oui |
| D-18 | partiel (celeris : livré M5-t6 — molette Ctrl = zoom borné, … | Câblé | PointerGesturesTest | oui |
| D-19 | livré (celeris : M7-t9 — ClasseLargeur suit les seuils des c… | Câblé | ClasseLargeurTest | oui |
| D-20 | absent | Non démarré | — | non |
| E-01 | livré (celeris : contrat SPI et implémentation de référence … | Noyau | MemoireSystemeFichiersTest | oui |
| E-02 | couvert (celeris : contrat SPI et faux VCS de référence livr… | Noyau | FauxVcsTest | oui |
| E-03 | couvert (celeris : contrat SPI et faux débogueur livrés M7-t… | Noyau | FauxDebogueurTest | oui |
| E-04 | couvert (celeris : contrat SPI et fausse exécution livrés M7… | Noyau | FauxExecutionTest | oui |
| E-05 | livré (celeris : analyseur de sortie M7-t2, formes complète … | Noyau | AnalyseurProblemesTest | oui |
| E-06 | absent (celeris : analyseur de piles livré M7-t2 — cadres co… | Noyau | AnalyseurPileTest | oui |
| F-01 | livré (celeris : noyau M8-t1 — SessionTexteFantome, suggesti… | Câblé | LatenceFantomeVueTest | oui |
| F-02 | livré (celeris : noyau M8-t1 — ModelePrediction prédit une o… | Noyau | PredictionTest | oui |
| F-03 | livré (celeris : M8-t5 — les actions IA (suggestion, prédict… | Noyau | ScenarioCatalogTest | oui |
| F-04 | livré (celeris : M8-t2 — PropositionAgent étiquetée et datée… | Noyau | RevueAgentTest | oui |
| F-05 | livré (celeris : M8-t3 — OutilsEditeur : lire des plages et … | Noyau | OutilsEditeurTest | oui |
| F-06 | livré (celeris : M8-t3 — ConnecteurMcp : le catalogue des ci… | Noyau | ExclusionsContexteTest | oui |
| F-07 | livré (celeris : M8-t4 — ExclusionsContexte lit le fichier d… | Noyau | ExclusionsContexteTest | oui |
| F-08 | livré (celeris : M8-t1 — FauxModeleLocal synchrone, FauxMode… | Noyau | FauxModelesTest | oui |
| F-09 | livré (celeris : noyau M8-t1 — jeton annulé à la frappe suiv… | Noyau | LatenceFantomeTest | oui |
| F-10 | livré (celeris : M8-t6 — le garde du contexte consulte le co… | Noyau | DemoNeutreTest | oui |
| G-01 | partiel (texte : 154 ms / 500 ms, 480 000 lignes ouvrables ;… | Câblé | GrosFichiersTest | oui |
| G-02 | partiel | Noyau | ScenarioCatalogTest | oui |
| G-03 | absent | Non démarré | — | non |
| G-04 | absent (celeris : livré M5-t6 — l'extension qui lève est jou… | Câblé | PluginHostTest | oui |
| G-05 | absent (au départ ; celeris M3-t6 : arbre virtuel de rangées… | Câblé | MovementGranularityTest | oui |

## Écarts notables

- Les lignes « livrées » des jalons M2 à M8 retombent au statut que
  leurs PREUVES soutiennent : presque toutes « Noyau » ou « Câblé » —
  la démonstration ne démontrait pas (catalogue décoratif, ateliers
  sans auto-vérification, aucun test d'appareil).
- Les correctifs de M7.5 (D-15 pincement, D-01 gouttière, B-01
  coloration stable) montent à « Câblé » : test de vue cité, test
  d'appareil encore absent.
- La catégorie F (IA native) retombe massivement à « Noyau » : les
  contrats sont testés en isolation, la vue et les scénarios
  auto-vérifiés manquent (l'intelligence n'est pas branchée, §8 du
  prompt M7.5 — non livré dans cette fenêtre, consigné au résumé).

Le contrôle continu vit dans `scripts/check-parity-matrix` (CI) :
statuts valides, tests cités existants, scénarios cités existants.
