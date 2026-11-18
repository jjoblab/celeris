# Migration — du dépôt de départ vers celeris

> Guide de migration pour les applications hôtes. Deux volets : la
> montée **1.x → 2.0** (couche d'intégration, ADR-0022 — en cours de
> déploiement, additive aujourd'hui), puis la correspondance historique
> depuis le dépôt de départ (`jo.codeeditor`, version finale M9).

## 0. La montée 1.x → 2.0 (ADR-0022, phase 2)

Deux racines nouvelles, **additives** : rien de ce qui suit ne casse un
hôte 1.x — les anciens poseurs vivent et délèguent. Les retraits
effectifs attendent le recensement des applications hôtes (§ 0.3).

### 0.1 Écouteurs : plusieurs abonnés (T3)

Les huit poseurs à emplacement unique de `EditorView` s'écrasaient
l'un l'autre (point 13 de la revue du 2026-11-17) : le
repositionneur des popups du kit occupait `setSurViewportChange`, tout
abonnement d'hôte l'écrasait en silence. La voie 2.0 est la paire
`add…Listener` / `remove…Listener` :

| 1.x (emplacement unique) | 2.0 (multi-abonnés) |
| --- | --- |
| `setSurViewportChange(r)` | `addViewportChangeListener(r)` / `removeViewportChangeListener(r)` |
| `setOnFontSizeChanged(c)` | `addFontSizeChangeListener(c)` / `removeFontSizeChangeListener(c)` |
| `setSurPlan(c)` | `addPlanListener(c)` / `removePlanListener(c)` |
| `setSurFichierDepose(c)` | `addFileDropListener(c)` / `removeFileDropListener(c)` |
| `setSurDoubleTap(v)` | `addDoubleTapListener(v)` / `removeDoubleTapListener(v)` |
| `setSurBasculeArret(c)` | `addBreakpointToggleListener(c)` / `removeBreakpointToggleListener(c)` |
| `setSurTouchePopup(v)` | `addPopupKeyListener(v)` / `removePopupKeyListener(v)` |
| `setSurGesteFantome(c)` | `addGhostGestureListener(c)` / `removeGhostGestureListener(c)` |

```java
// Avant (1.x) — l'hôte écrase le repositionneur du kit :
editorView.setSurViewportChange(this::repositionner);

// Après (2.0) — le kit et l'hôte vivent ensemble :
editorView.addViewportChangeListener(this::repositionner);
// … et le retrait explicite :
editorView.removeViewportChangeListener(this::repositionner);
```

Sémantique des événements à verdict (`doubleTap`, `touchePopup`) :
chaque abonné rend son verdict ; la cascade s'arrête dès qu'un abonné
consomme. Les événements d'information (`viewport`, `fontSize`,
`plan`, `fichierDéposé`, `basculeArrêt`, `gesteFantôme`) atteignent
CHAQUE abonné, dans l'ordre d'abonnement.

### 0.2 Réglages : une source unique (T2)

Le thème, la police, la taille, le retour à la ligne, les ligatures
et les portes de fonctionnalités vivent dans `EditorView.settings()`
(`EditorSettings`, point 14 de la revue) — la vue applique,
l'orchestrateur des popups suit par abonnement, plus de double pose :

```java
// Avant (1.x) — deux appels à la main, le désync en prime :
editorView.setTheme(SOMBRE);
popups.surTheme(SOMBRE);
editorView.setFont(new FontSpec("mono", 16f));
popups.surTailleTexte(16f);

// Après (2.0) — UN endroit, vue ET popups suivent :
editorView.settings().setTheme(SOMBRE);
editorView.settings().setFontSizeSp(16f);
// retour de ligne, ligatures, portes :
editorView.settings().setSoftWrap(true);
editorView.settings().setLigatures(true);
editorView.settings().toggleGate(ReglagesEditeur.Porte.MINIMAP);
```

Les poseurs 1.x (`setTheme`, `setFont`, `setSoftWrap`,
`setLigatures`) continuent de marcher : ils écrivent dans la source.
La table des portes reste `vue.reglages()` (clés stables, § 19
d'USAGE) — `EditorSettings.gates()` la rend aussi. La persistance
reste à l'hôte : abonnez-vous
(`settings().addListener(source -> …)`) et relisez ce qui vous
intéresse ; plusieurs abonnés vivent ensemble.

La façade `CelerisEditor.settings()` est une vue mince sur cette
source : `editor.settings().setTheme(...)` et
`editorView.settings().setTheme(...)` écrivent au même endroit.

### 0.3 Retraits prévus en 2.0.0 — recensement à valider par le propriétaire

Les symboles suivants vivent en compatibilité et sont **candidats au
retrait** en 2.0.0. Le retrait effectif attend le recensement des
applications hôtes connues du propriétaire (garde-fou du mandat
B0 : demander la liste, ne pas la deviner) :

| Symbole | Remplaçant | Remarque |
| --- | --- | --- |
| `EditorView.setSurViewportChange` | `add/removeViewportChangeListener` | le plus urgent : l'écrasement silencieux du kit |
| `EditorView.setOnFontSizeChanged` | `add/removeFontSizeChangeListener` | |
| `EditorView.setSurPlan` | `add/removePlanListener` | |
| `EditorView.setSurFichierDepose` | `add/removeFileDropListener` | |
| `EditorView.setSurDoubleTap` | `add/removeDoubleTapListener` | |
| `EditorView.setSurBasculeArret` | `add/removeBreakpointToggleListener` | |
| `EditorView.setSurTouchePopup` | `add/removePopupKeyListener` | |
| `EditorView.setSurGesteFantome` | `add/removeGhostGestureListener` | |
| `ReglagesEditeur.setSurBascule` | `EditorSettings.addListener` | l'écouteur unique des portes devient l'abonné des réglages |
| `PopupsEditeur.surTheme` / `surTailleTexte` | abonnement automatique à `vue.settings()` | l'appel direct reste utile hors vue (montage sans vue) |
| `EditorView.setTheme/setFont/setSoftWrap/setLigatures` | `settings().set…` | les poseurs 1.x délèguent déjà |

La dépréciation `@Deprecated` de ces symboles suivra le feu vert du
propriétaire sur ce recensement : le dépôt compile en
`-Xlint:all -Werror`, une dépréciation posée avant l'accord casserait
la démonstration (intouchable, § 0.1 du mandat) qui les appelle
encore.

---


## 1. Correspondance des modules

| Dépôt de départ | celeris | Nature du changement |
| --- | --- | --- |
| `cel-core` (moteur, Android) | `core-text`, `core-syntax`, `core-language`, `core-intelligence` | éclatement en modules JVM purs ; le moteur ne dépend plus du SDK |
| `cel-ui` (vues) | `android-view` + `android-widgets` | refonte complète de la vue (plafond 600 lignes) ; kit de popups porté |
| `cel-lsp` (client LSP) | `core-lsp` + `lsp-protocole` | client réécrit : domaine immuable d'un côté (ADR-005), frontière protocolaire de l'autre (ADR-015, LSP4J derrière) |
| `cel-lsp-api` (SPI langage) | `core-language` | contrats portés, enrichis (plateforme de langages, ADR-017) |
| `cel-textmate` (grammaires) | `core-syntax-grammars` | moteur conservé, réellement TextMate (joni, ADR-014) ; les grammaires deviennent des données JSON de paquets, plus d'archive binaire versionnée |
| `demo` (démo) | — | sortie du dépôt (ADR-0024, F.7) : une démonstration repensée vivra dans un module neuf |

## 2. Changements majeurs à prévoir

1. **Sélection** : le type mono-intervalle du dépôt de départ devient une
   liste de plages (multi-curseur) ; les appels de code qui construisaient
   une sélection à deux `int` migrent vers `SelRange.caretAt` /
   `SelRange` + `MultiSelection.of` (`core-text`).
2. **Positions** : les positions restent des offsets UTF-16 en `int`, mais
   les plages deviennent des types porteurs (`SelRange`, `TextEdit`,
   `EditSpan`) et les plages destinées à survivre aux éditions deviennent
   des `AnchoredRange` projetées par la session (`AnchoredRanges`,
   `PositionProjector`). La confusion ligne/rangée/pixel disparaît côté
   vue : les rangées visuelles sont servies par l'arbre d'accessibilité
   et la projection, jamais exposées comme des lignes.
3. **Mutation** : les mutations directes du document (hors transaction)
   disparaissent ; tout passe par la `Transaction`, annulation comprise
   (`TextSession`, ADR-003).
4. **Tokéniseurs** : les fournisseurs de langage qui implémentaient un
   tokéniseur manuel (`GenericTokenizer`, `TokenizerTemplate`) migrent
   soit vers un analyseur écrit (`LanguageSyntax`, ADR-004 — les six P0
   vivent en spécialisations factorisées), soit vers la voie de
   grammaires (paquet `grammar.json`, ADR-014) pour la longue traîne.
5. **Artefacts** : coordonnées Maven `jo.celeris:*` en lieu et place de
   `jo.codeeditor:*` ; niveaux d'API minimum relevés de 24 à 26 ;
   `lsp-protocole` est le seul module qui expose la bibliothèque LSP4J
   (ADR-015).

## 3. Ce qui ne change pas

- Le modèle mental : une vue Canvas unique, un document immuable par
  instantané, une voie de mutation unique.
- Les contrats de langage, portés et enrichis (la plateforme de langages
  les assemble, ADR-017).
- Le comportement des popups unifiées (kit porté tel quel, jetons et
  rendus compris — M8.5).
- La licence Apache-2.0 et l'absence de dépendance au runtime Kotlin.

## 4. Pas à pas de migration (final, M9)

### 4.1 Table de correspondance API publique

La source de vérité est le manifeste d'API (`api/<module>.api`,
régénéré par `./gradlew apiManifest`, contrôlé par le validateur de
compatibilité — ADR-009). Les points d'entrée usuels :

| Dépôt de départ | celeris | Note de migration |
| --- | --- | --- |
| `session.EditorSession` (la façade, 1 694 lignes) | `core-text` `TextSession` (+ `Document`) | la session refondue : instantanés immuables, auditeurs d'édition et de sélection explicites |
| `EditorSession.doReplaceRange` / `replaceRangeWithCaret` | `TextSession.replaceRange` / `Transaction` | la voie de mutation unique devient un type ; l'annulation vit dans la session (`undo`, `redo`, `undoDepth`) |
| sélection `int`×`int` | `SelRange` / `MultiSelection` | multi-curseur natif ; l'ancrage survit aux éditions par `AnchoredRanges` |
| `highlight.tokenizer.GenericTokenizer` / `TokenizerTemplate` | `core-syntax` `LanguageSyntax` (`JavaSyntax`, `KotlinSyntax`, `KtsSyntax`, `GroovySyntax`, `XmlSyntax`, `JsonSyntax`) | l'arbre tolérant incrémental remplace les jetons plats (ADR-004) ; coloration, plis, plan, crochets et indentation consomment le même arbre |
| grammaires TextMate embarquées (`cel-textmate`) | paquets `grammar.json` + `core-syntax-grammars` | le format réel des extensions VS Code, compilé par joni (ADR-014) ; injections comprises (H-05) |
| `cel-lsp-api` fournisseurs de langage | `core-language` `LanguageContributor` / `LanguageCapability` | capacités déclarées par paquets et contributeurs (ADR-005/017) |
| `cel-lsp` client (session, transport) | `core-lsp` (domaine immuable) + `lsp-protocole` (`LspSession`, montages) | ADR-015 : la bibliothèque vit derrière la frontière, jamais dans le code hôte |
| `view.EditorView` (4 579 lignes) | `android-view` `EditorView` (≤ 600 lignes) + `android-widgets` + `android-host` | la vue délègue : peintres, gouttière, popups, SPI hôte |
| kit de popups (`view.popup.*`) | `android-widgets` (jetons `PopupTokens`, carte verre, rangées deux lignes) | porté tel quel (M8.5) ; mêmes valeurs, mêmes rendus |
| thèmes de palettes | `ScopeThemes` (clair, sombre, contraste élevé) | la couleur se résout par PILE DE PORTÉES (H-07), la palette par sorte n'est plus qu'un repli |

### 4.2 Procédure de reprise des thèmes et gabarits personnalisés

**Thèmes.** La voie officielle est l'import du format `tokenColors` de
VS Code (`TokenColorsTheme`, `core-language`) : un fichier JSON portant
`name`, `colors` (`editor.background`, `editor.foreground`) et
`tokenColors` (sélecteurs de portées TextMate — `scope`, éventuellement
une liste — et `settings.foreground`, `settings.fontStyle`). La palette
dérive du fichier seul : exemple vivant dans la démonstration
(`themes/atelier.json`, le quatrième thème « Atelier », M7.6 H-07).
Les réglages d'encre par sorte de jeton migrent vers des règles de
portées ; les trois thèmes livrés (`ScopeThemes`) servent de gabarit
de contraste (banc WCAG ≥ 4,5:1).

**Gabarits.** Les gabarits vivent dans les paquets de langage
(`snippets.json`, `PackSnippet`) : préfixe, trous tabulables, corps
multiligne — la complétion de gabarit (B-06) les sert par l'index. Les
squelettes de méthode (M6) restent côté intelligence
(`SquelettesMethode`) : ils se déduisent de l'arbre, sans migration.

### 4.3 Procédure de reprise des extensions de langage

Un langage entier est un **paquet de données** (`langages/<id>/`,
M7.6) — aucune recompilation :

1. `pack.json` — identité : `id`, `version`, `schemaVersion` (1),
   `license`, `provenance` ;
2. `language.json` — le descripteur (noms exacts, extensions, motifs,
   première ligne, badge) et la configuration (commentaires,
   `brackets`, fermeture automatique, indentation, plis) — le
   sous-ensemble du `language-configuration.json` de VS Code ;
3. `grammar.json` — la grammaire TextMate (format réel des extensions
   de VS Code, inclusions et injections comprises) ;
4. `outline.json` — le plan par règles (sélecteur → catégorie → groupe
   de capture) ;
5. `snippets.json` — les gabarits ;
6. `servers.json` — les serveurs de langage déclarés (transport,
   classe, interface, nom annoncé) : **l'hôte seul décide de monter**
   (revue de sécurité M9).

Chargement : `Packs.folder(dossier)` (un sous-dossier par paquet) ou
`Packs.archive(zip)` — entrées plates, liens symboliques et remontées
refusés, tailles et profondeur bornées (`PackLoader`) ; assemblage
par `LanguagePlatform` (paquets intégrés, paquets propres, capacités
génériques). Un langage à analyseur écrit reste possible
(`LanguageSyntax`, ADR-004) et se déclare attaché par préférence dans
le `language.json` du paquet.

### 4.4 Checklist de vérification des performances après migration

La discipline de mesure est le minimum de trois tours (PIEGE-13) ;
chaque banc imprime sa mesure.

- [ ] Édition : `./gradlew :core-text:test` — le banc d'édition
      (mille éditions sur 50 000 lignes) tient le mandat d'1 ms par
      édition (ADR-006) ; le cliquet interne de M9 exige 0,5 ms.
- [ ] Remplacement global et annulation : même banc — la passe et son
      annulation tiennent 150 ms chacune (cliquet M9, ~×4 de marge).
- [ ] Ré-analyse : `./gradlew :core-syntax:test` — 10 ms sur
      50 000 lignes par analyseur écrit (ADR-004), la grammaire pareil
      (`core-syntax-grammars`, `GrammairesBudgetTest`).
- [ ] Première image : `PremiereImageAnalyseBudgetTest` — l'instantané
      de fenêtre sur 100 000 lignes tient l'enveloppe de 500 ms (G-01).
- [ ] Rendu de vue : `./gradlew :android-view:test` — le fantôme se
      trace sous 16 ms (image à 60 Hz), les gros fichiers s'ouvrent
      progressivement (`GrosFichiersTest`).
- [ ] Sur appareil : les budgets de rendu d'ADR-006 restent à
      calibrer sur l'appareil de référence
      (`docs/qualite/VERIFICATION-APPAREIL.md`) — le bac JVM ne
      remplace pas la vitre.
