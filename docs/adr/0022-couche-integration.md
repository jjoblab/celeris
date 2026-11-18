# ADR-0022 — La couche d'intégration : façade d'abord, consolidation ensuite

- **Statut** : Acceptée (mandat du propriétaire, revue du 2026-11-17 § 0.1 point 4)
- **Date** : 2026-11-18
- **Amende** : aucune — nouvelle couche, le cœur et le rendu ne bougent pas

## Contexte

La revue du 2026-11-17 (§ 1.1) a décomposé les quinze démarches que
l'hôte doit écrire *dans son code* pour obtenir un éditeur complet.
Toutes vérifiées sur le code au fil de la mission (deux se sont
révélées inexactes, voir plus bas) :

| # | Constat vérifié | Preuve (fichier, ligne au 2026-11-18) |
| --- | --- | --- |
| 1 | Le chemin officiel (USAGE § 4) monte une vue sans popups ni complétion ; l'orchestrateur n'apparaît qu'au § 9.1 | `USAGE.md` § 4 vs § 9.1 |
| 2 | Saisie intelligente coupée par défaut : `provider` null à la construction | `IntelligenceHub.java` (~145) |
| 3 | Erreur de doc : `IntelligenceHub.Provider.builtins()` cité, la méthode est `IntelligenceHub.builtins()` | `USAGE.md` ~771 |
| 4 | Thème et taille déclarés deux fois (vue ET orchestrateur), resynchronisés à la main | `PopupsEditeur.java` ~178, `surTheme`/`surTailleTexte` ~256/~262 |
| 5 | Deux déclencheurs à brancher à la main (`setSurDoubleTap`, `setSurTouchePopup`) | `EditorView.java` ~476, ~490 ; branchés par `PopupsEditeur.java` ~251-253 |
| 6 | Cycle de vie manuel : `popups.detacher()` à appeler ; la vue sans libération — **corrigé depuis** (C1 : `EditorView.release()`, commit `1204c9d` ; `PopupsEditeur.detacher()` retire ses écouteurs, commit `2759bb6`) | `USAGE.md` § 19.10 |
| 7 | Résolution de langue en cinq temps, registre à assembler hors fil UI, aucun « langue de ce fichier » | `USAGE.md` § 6 ; `demo/.../LangagesDemo.java` (l'hôte écrit son propre chargeur) |
| 8 | Horloge obligatoire : `TextSession.of(contenu, System::nanoTime)` | `TextSession.java` ~64, ~75 |
| 9 | Argument opaque : `CoachPopups.muet()` au constructeur de l'orchestrateur | `PopupsEditeur.java` ~178 |
| 10 | ~10 dépendances Maven sans BOM ni agrégateur | `USAGE.md` § 3.1 (traité par B1, hors ADR) |
| 11 | Si LSP embarqué : règles R8 à copier à la main | `lsp-protocole/regles-conservation.pro` à la racine du module (traité par B2) |
| 12 | `repositories { … }` sans dire dans quel fichier (FAIL_ON_PROJECT_REPOS) | `USAGE.md` § 3 |
| 13 | Huit setters à abonné unique sur la vue ; `PopupsEditeur` en prend un (`setSurViewportChange`) — l'hôte qui s'y abonne écrase l'orchestrateur | `EditorView.java` ~296-551 ; `PopupsEditeur.java` ~251 |
| 14 | Réglages éparpillés : `ReglagesEditeur` ne porte que les portes ; thème, police et taille vivent ailleurs | `ReglagesEditeur.java` ; `EditorView.java` |
| 15 | `PopupsEditeur.detacher()` ne retirait pas ses écouteurs de session — **corrigé** (commit `2759bb6`) ; `TextSession` expose les retraits depuis toujours (la revue disait le contraire : grep négatif inexact) | `PopupsEditeur.java` ; `TextSession.java` ~158/~178 |

Le propriétaire mandate (§ 0.1 point 4) : la façade additive est
validée, une refonte plus large de la couche d'intégration est
autorisée si c'est la solution la plus propre. Hors périmètre : le
moteur de rendu, `core-*` (sauf chargeur asynchrone de langues), le
renommage global (§ E).

## Options comparées

### Option A — Façade seule

Un `CelerisEditor.attach(view, layer)` compose l'existant :
il appelle `setSession`, `setLanguage`, `PopupsEditeur`, pose
`IntelligenceHub.builtins()`, branche les deux déclencheurs, expose
`open(name, text)`, `settings()`, `close()`.

- Avantages : livrable vite, zéro risque pour l'existant, sert
  d'exemple consommateur (B5) dès la phase 1.
- Défauts : les points 4, 13 et 14 ne se résolvent pas — ils vivent
  dans les composants, pas dans leur assemblage. La façade devrait
  appeler `setTheme` PUIS `surTheme`, `setOnFontSizeChanged` PUIS
  `surTailleTexte` : la double déclaration demeure, encapsulée mais
  réelle ; deux setters à abonné unique restent un piège pour l'hôte
  qui veut SA notification de viewport (il écrase l'orchestrateur).
- Verdict : améliore 1, 2, 5, 6, 7, 8, 9, mais laisse la moitié du
  diagnostic en place.

### Option B — Façade + consolidation mesurée (phase 2)

La façade de la phase 1, puis les composants eux-mêmes apprennent ce
que la façade simule : réglages à source unique (T2), écouteurs
multiples (T3), valeurs par défaut sûres (T4), chargeur de langues
partagé (T5). La façade devient mince : elle assemble, elle ne
compense plus.

- Avantages : chaque défaut meurt à sa racine ; la façade ne cache
  rien ; l'API manuelle (§ 9.2) profite des mêmes correctifs.
- Défauts : toucher `EditorView`/`PopupsEditeur` est cassant pour un
  montage manuel existant (les setters à abonné unique disparaissent
  en 2.0.0) ; plus de travail.
- Verdict : la solution la plus propre — c'est le critère du mandat.

### Option C — Contrôleur propriétaire du cycle de vie (refonte complète)

Un `EditorDocument` non visuel possède session, langue, intelligence,
écouteurs ; `EditorView` devient une vue pure qui s'y abonne
(« séparation vue/état », T7). Le montage : on crée le document, on
crée la vue, on les lie.

- Avantages : testabilité maximale de l'état sans Android ; la vue
  devient mince.
- Défauts : refonte du plus gros objet du dépôt (le constructeur
  d'`EditorView` tisse session, pont de syntaxe, langue,
  intelligence, thème, polices, gouttière — ~70 lignes), risque élevé
  sur le moteur de rendu que le mandat exclut, et le bénéfice ne se
  matérialise que si T1-T3 sont faits d'abord. La revue elle-même
  demande d'« évaluer, pas forcer » (T7).
- Verdict : hors mandat pour la phase 2 ; T7 reste une étude
  optionnelle après.

## Décision

**Option B, en deux phases** — exactement la méthode imposée par le
PROMPT § B0 :

1. **Phase 1 (v1.2.0, additive)** : `CelerisEditor` dans `android-host`,
   symboles publics en anglais. Il compose l'existant et corrige ce
   qu'une façade peut corriger : valeurs par défaut utiles
   (`IntelligenceHub.builtins()` actif, coach muet, déclencheurs
   branchés), `open(fileName, text)` qui résout la langue (registre
   partagé asynchrone — le chargeur vit dans `core-language` comme
   l'autorise le mandat), surcharges de confort (`TextSession.of`
   sans horloge), libération automatique (`release()` de la vue au
   détachement définitif + `close()` explicite). Les points 4, 13, 14
   restent connus et documentés comme dette — la façade les compense
   en interne (appels enchaînés vue→orchestrateur), sans les cacher
   au point de les nier.
2. **Phase 2 (v2.0.0, cassante autorisée)** : les composants
   apprennent — T2 `EditorSettings` source unique (absorbe
   `ReglagesEditeur`, thème/taille inclus, plusieurs écouteurs) ;
   T3 paires `add…Listener`/`remove…Listener` sur la vue (l'ancien
   API disparaît, `MIGRATION.md` porte chaque avant/après) ; T4-T5
   consolidation des défauts et du chargeur ; T6 la façade devient LE
   chemin du § 4 d'USAGE. T7 (séparation vue/état) : évalué à la fin
   de la phase 2, exécuté seulement si la mesure le justifie.

**Ce que cette ADR démontre** (réponse à la question du PROMPT) : une
façade seule ne peut PAS résoudre les points 13 et 14 — le piège de
l'abonné unique vit dans `EditorView.setSurViewportChange` (un champ,
un setter) ; l'éclatement des réglages vit dans `ReglagesEditeur`
qui n'a jamais connu le thème. La façade peut encapsuler la double
synchronisation du point 4, mais au prix de répliquer chaque setter
deux fois — c'est une compensation, pas une correction. D'où la phase
2.

## Garde-fous (repris du mandat, non négociables)

- Portes qualité vertes à chaque étape ; JaCoCo ≥ 90 % sur modules
  purs ; Checkstyle/PMD/Error Prone/NullAway au plafond.
- Nouveaux symboles publics en anglais ; baseline langue jamais
  montante ; manifestes `api/*.api` régénérés à chaque évolution.
- Aucune nouvelle dépendance tierce, aucune AndroidX ; ADR-018 (tout
  en popups), ADR-007 (analyse hors fil UI), ADR-009 (API fermée)
  respectées.
- `MIGRATION.md` « 1.x → 2.0 » : chaque appel retiré ou changé y
  figure avec son avant/après.
- Recensement de l'impact sur les applications hôtes connues à
  présenter au propriétaire AVANT tout retrait de symbole.
- `demo` et `serveur-lsp-java` intouchables ; si la phase 2 casse
  leur compilation, le signaler au lieu de réparer.

## Plan de migration

1. Phase 1 : `CelerisEditor`, `EditorSettings` (lecture), chargeur
   `LanguageLoader` (core-language), surcharges `TextSession.of`.
   Bancs : montage complet < 10 lignes, ouverture `.java` pendant le
   chargement asynchrone, `close()` idempotent, zéro prolifération de
   fils (banc C2 étendu à la façade).
2. Exemple consommateur (B5) contre la façade seule — il prouve la
   promesse « éditeur complet en une dépendance et dix lignes ».
3. Phase 2 : T2 puis T3 (les deux racines), puis T4-T6 ; chaque
   étape : banc rouge d'abord, correctif ensuite, `MIGRATION.md` au
   fil de l'eau.
4. Version 2.0.0 : retraits effectifs, documentation alignée (§ 4
   réécrit autour du kit, § 9.2 en annexe « avancé »), tag unique
   après validation complète (la publication JitPack n'a lieu qu'à la
   fin des travaux, directive du propriétaire du 2026-11-18).

## Conséquences

- La façade phase 1 est la pièce maîtresse livrée d'abord ; elle
  porte seule la valeur jusqu'à la phase 2.
- Les points 13/14/4 ne sont « fermés » qu'en 2.0.0 ; d'ici là, la
  façade les compense proprement et la doc le dit.
- `PopupsEditeur` n'est pas remplacé : il est composé (la revue
  § 6 le demande) ; sa construction par la façade masque
  `CoachPopups.muet()` (point 9) sans le retirer — le retrait effectif
  juge en phase 2.
