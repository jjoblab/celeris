# ADR-0023 — Nommage de l'API publique (brouillon, non décidé)

> **Statut : brouillon** — chantier E du mandat d'intégration
> (2026-11-18). Aucun renommage n'est appliqué par cette mission ; ce
> document prépare la décision. La décision revient au propriétaire.

## Contexte

`AGENT.md` impose des identifiants anglais pour le code ; la
baseline `scripts/language-baseline.txt` recense pourtant **263
identifiants français dans les modules publiés** (android-host 31,
android-view 64, android-widgets 28, core-ai 49, core-syntax 91 —
la démonstration, hors périmètre, en compte 81). La surface publique
d'un hôte mêle ainsi deux langues : `setSoftWrap`, `setFont` à côté
de `setNumerosVisibles`, `setRetenueAnalyseMs`, `PopupsEditeur`,
`CoachPopups`, `detacher()`, `brancherServeur`.

Deux forces contraires :

- **cohérence pour l'hôte international** : le public JitPack est
  international (B3 a posé un README anglais) ; un hôte qui lit la
  javadoc en français mais doit deviner si la méthode s'appelle
  `setLineNumbers` ou `setNumerosVisibles` paie un coût réel à
  chaque premier contact ;
- **coût de renommage** : chaque renommage cassant doit être migré
  (démo intouchable, bancs, docs) et documenté (MIGRATION.md).

Le point de bascule : **le nombre de consommateurs**. Aujourd'hui le
recensement du propriétaire (validé le 2026-11-18) est **vide —
aucune application n'utilise celeris**. Renommer maintenant ne casse
personne ; après adoption large, chaque renommage devient une
version majeure de douleur.

## Inventaire (depuis les manifestes `api/*.api` et les surfaces Android)

### À garder tels quels (déjà anglais ou neutres)

`TextSession`, `Document`, `Analysis`, `LanguageKit`, `EditorView`,
`EditorSettings`, `EditorTheme`, `FontSpec`, `KeyBindings`,
`IntelligenceHub`, `OutlineEntry`, `SyntaxService`, `PackGrammars`,
`LanguageSupport`, `PopupLayer`, `PopupManager`, `PopupController`,
`CelerisEditor`, `setSession`, `setLanguage`, `setFont`,
`setSoftWrap`, `setLigatures`, `setKeyBindings`, `setTheme`,
`addPlanListener`, `addViewportChangeListener`, `addDoubleTapListener`,
`addPopupKeyListener`, `addBreakpointToggleListener`,
`addFileDropListener`, `addGhostGestureListener`,
`addFontSizeChangeListener` (les paires 2.0 de l'ADR-0022 sont
**toutes en anglais** — le garde-fou du mandat a tenu), `undo`,
`redo`, `selection`, `caret`, `apply`.

### Candidats au renommage (français → proposition anglaise)

Les ~25 symboles les plus visibles pour un hôte, par fréquence
d'usage attendue dans le code d'intégration :

| Actuel (français) | Proposition | Module | Visibilité hôte |
| --- | --- | --- | --- |
| `PopupsEditeur` | `EditorPopups` | android-host | constante — la façade l'expose |
| `CoachPopups` | `PopupCoach` | android-host | haute — le constructeur longue |
| `PopupsEditeur.detacher()` | `detach()` | android-host | haute — cycle de vie |
| `PopupsEditeur.notifier(titre, msg)` | `notify(title, msg)` | android-host | haute |
| `PopupsEditeur.brancherServeur(...)` | `connectServer(...)` | android-host | haute — § 8 d'USAGE |
| `PopupsEditeur.configurerIntentions(...)` | `setIntentions(...)` | android-host | moyenne |
| `PopupsEditeur.configurerActionsSelection(...)` | `setSelectionActions(...)` | android-host | moyenne |
| `PopupsEditeur.ouvrirRecherche()` / `fermerRecherche()` | `openSearch()` / `closeSearch()` | android-host | haute — raccourci clavier |
| `PopupsEditeur.basculerCompletion()` | `toggleCompletion()` | android-host | haute |
| `PopupsEditeur.ouvrirIntentions()` | `openIntentions()` | android-host | moyenne |
| `PopupsEditeur.ouvrirSurvol()` | `openHover()` | android-host | moyenne |
| `PopupsEditeur.ouvrirGabarits()` | `openTemplates()` | android-host | moyenne |
| `PopupsEditeur.ouvrirHierarchie()` | `openHierarchy()` | android-host | moyenne |
| `PopupsEditeur.ouvrirReglages(...)` | `openSettings(...)` | android-host | moyenne |
| `PopupsEditeur.surTheme(t)` / `surTailleTexte(s)` | `onTheme(t)` / `onTextSize(s)` | android-host | basse (voie 2.0 : abonnement) |
| `PopupsEditeur.setNomFichier(n)` | `setFileName(n)` | android-host | moyenne |
| `PopupsEditeur.setPlan(...)` / `setDiagnostics(...)` | `setOutline(...)` / `setDiagnostics(...)` | android-host | moyenne — `setDiagnostics` déjà neutre |
| `EditorView.setNumerosVisibles(b)` | `setLineNumbersVisible(b)` | android-view | haute |
| `EditorView.setRetenueAnalyseMs(ms)` | `setAnalysisDebounceMs(ms)` | android-view | moyenne |
| `EditorView.premiereLigneVisible()` | `firstVisibleLine()` | android-view | moyenne |
| `EditorView.marquesGouttiere()` | `gutterMarks()` | android-view | moyenne — § 10 d'USAGE |
| `EditorView.setClasseLargeur(c)` / `ClasseLargeur` | `setWidthClass(c)` / `WidthClass` | android-view | basse |
| `PopupManager.fermerTout()` | `closeAll()` | android-widgets | moyenne |
| `PopupManager.montrer(...)` | `show(...)` | android-widgets | moyenne |
| `BarreToolbar.poserBarreTouches(...)` | `setKeyRow(...)` | android-widgets | basse (démo) |

`lsp-protocole` (artefact) : le nom reste — le module est le
protocole LSP embarqué ; changer un identifiant d'artefact impose la
coordonnée JitPack complète et n'a rien d'un renommage de symbole.
Il se décide avec le groupe Maven (§ F.6), pas ici.

## Stratégie de migration proposée

1. **2.0.0 (fenêtre actuelle — zéro consommateur)** : renommage
   direct SANS alias. Le recensement validé vide rend la période
   d'alias inutile : personne n'a de code à faire migrer en douceur.
   MIGRATION.md « 1.x → 2.0 » porte chaque avant/après (le § 0.1 en
   montre déjà le modèle). C'est l'option la moins chère qui
   n'arrivera qu'une fois.
2. **Si un premier hôte apparaît avant le chantier** : passer au
   régime « alias déprécié une mineure, retrait à la majeure
   suivante » — nouveau nom en 2.0, `@Deprecated` sur l'ancien,
   retrait en 3.0.
3. Les **nouveaux symboles restent anglais** sans exception (déjà
   la règle du mandat B0 — les paires add/remove de l'ADR-0022 en
   sont la preuve).
4. La démo, intouchable, migre mécaniquement le jour du renommage
   (le dépôt compile en `-Werror` : la dépréciation casserait son
   build — d'où l'option 1 sans alias, qui migre tout d'un tenant).

## Argument central

**Peu de consommateurs aujourd'hui, renommer coûte peu ; après
adoption large, c'est cassant.** Le coût du renommage est fixe
(migration du dépôt : estimée à une session — la table ci-dessus
couvre les symboles à portée de main, les 263 de la baseline se
traitent par liste mécanique) ; le coût du statu quo croît avec
chaque hôte qui apprend la surface bilingue. La fenêtre « zéro
consommateur » est ouverte aujourd'hui ; le jour où JitPack sert le
premier hôte réel, elle se referme pour des années.

## Décision attendue du propriétaire

1. Renommer en 2.0.0 sans alias (recommandé — cf. argument central),
   ou poser les alias dépréciés ?
2. La table des ~25 propositions convient-elle ? (les noms anglais
   proposés se discutent un à un)
3. Calendrier : dans la 2.0.0 en préparation, ou chantier séparé
   après ? (§ F.5 du mandat : un seul cassement pour les hôtes —
   recommandé : DANS la 2.0.0)

## Conséquences

- Si décidé : un chantier de renommage mécanique (baseline en main),
  MIGRATION.md enrichi, manifestes `api/*.api` régénérés, baseline
  `language-baseline.txt` remise à zéro pour les modules publiés.
- Si refusé : la surface reste bilingue ; ce document sert de
  référence pour les symboles que tout NOUVEAU code doit nommer en
  anglais.
