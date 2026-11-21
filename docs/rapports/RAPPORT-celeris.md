# RAPPORT-celeris — analyse du retard de coloration et des incohérences du dépôt `jjoblab/celeris`

> Mission : rapport d'analyse (prompt « rapport-celeris », 2026-11-21).
> Dépôt audité au commit `f5985ef` (« la 2.2.11 », tag `v2.2.11`).
> Chaque constat cite un fichier et une ligne, une mesure ou la sortie d'un banc.
> Les verdicts distinguent **mesuré** (banc ou chronomètre JVM), **lu dans le code**
> (lecture vérifiée fichier:ligne) et **supposé** (extrapolation annoncée comme telle).

---

## 1. Résumé exécutif

Le retard de coloration a **deux causes dominantes qui se cumulent par contexte**.
Au démarrage à froid, c'est l'**assemblage séquentiel des 33 paquets de langues sur
un seul fil démon** qui retient la langue du fichier ouvert (mesuré 338–1 050 ms sur
JVM de bureau à 2 CPU ; la langue n'arrive qu'après TOUT l'assemblage, quel que soit
le fichier ouvert) ; ensuite s'ajoute la **retenue fixe de 250 ms** appliquée à TOUTE
soumission, y compris la première analyse complète d'une ouverture. À chaud (registre
prêt), la retenue de 250 ms devient la part dominante (85–98 % du retard sur un
fichier petit) ; sur un gros fichier à grammaire s'y ajoute le **lexique complet du
document** exigé avant la première publication (274 ms mesurés pour 20 000 lignes
Python sur JVM bureau) — la priorité « fenêtre visible d'abord » ne concerne que le
parseur, jamais le lexeur. Trois constats graves dépassent le retard : **les
fermetures de largeur nulle ne ferment jamais leurs blocs** (csharp et au moins neuf
autres grammaires réelles — l'état ouvert contamine tout le reste du fichier) ; **le
budget de 20 000 essais par ligne déborde dès 310 caractères de JavaScript** (la
moitié d'une ligne de 630 caractères reste monochrome, sans aucun signalement) ; **la
retenue est un créneau unique par vue** : ouvrir deux onglets à moins de 250 ms
d'intervalle détruit la tâche d'analyse du premier, qui reste sans couleur jusqu'à
son retour. Les correctifs prioritaires (F1–F7, § 7) sont tous prouvés par des bancs
rouges déjà posés ; le gain attendu est une première couleur **immédiate à chaud**
(retenue retirée de l'ouverture) et une coloration **fidèle** sur csharp, go, swift,
lua, ruby, php, dart, javascript, css et html (largeur nulle admise).

---

## 2. Méthode et environnement

| Élément | Valeur |
| --- | --- |
| Commit audité | `f5985ef` (branche `main`, tag `v2.2.11`) |
| Environnement d'exécution | sandbox Linux 2 CPU / 4 Go, JDK 17 Temurin 17.0.21.1+2, Gradle 9.9.1, SDK Android 36 |
| Builds exécutés | `:core-syntax-grammars:test` (149 tests verts, baseline), `:core-syntax:test`, `:core-language:test`, `:android-view:testDebugUnitTest` (bancs d'audit) |
| Mesures | lanceurs JVM autonomes (`scripts/MesuresCeleris.java`, `scripts/MesuresFenetre.java` du rapport, hors dépôt) sur les jars 2.2.11 ; `ThreadMXBean.getThreadAllocatedBytes` pour les allocations ; min de 5 tours après échauffement |
| Bancs d'audit posés | `LargeurNulleFinTest` (4), `BudgetLigneDenseTest` (3), `RetenuePremiereAnalyseTest` (4), `SetLangueInchangeeTest` (2), `ChargeurVoieRechercheTest` (1), `RechargementCourseTest` (1) — tous rouges à l'état actuel du dépôt, sauf les contrôles |
| **Non exécuté** | **aucun appareil Android ni émulateur** : pas de build release R8 sur appareil, pas de `FrameMetrics`, pas de mesures p95 réelles. Les facteurs « appareil » du rapport sont des **extrapolations annoncées** (×5 à ×15 entre JVM bureau 2 CPU et appareil milieu de gamme, ordre de grandeur usuel) ; la retenue de 250 ms, elle, est **structurelle** (minuteur) et vaut exactement 250 ms partout. La voie native (`core-syntax-treesitter`) n'a pas de banc exécutable sans la bibliothèque C : H13 est prouvé par lecture seule. |

Les bancs d'audit sont livrés avec ce rapport (§ Annexe B) sous forme de fichiers
de test complets, à poser dans le dépôt au moment des correctifs — conformément au
mandat, **aucun fichier du dépôt n'a été modifié** pour produire ce rapport : les
bancs ont été exécutés puis retirés de l'arbre (leurs résultats sont dans l'annexe).

---

## 3. Chronologie reconstituée de la première couleur

### 3.1 Démarrage à froid, premier onglet (`Salut.java`, ~35 lignes)

La chaîne vécue par `sample` (fichiers petits : le lexique est négligeable, le
retard visible est l'assemblage puis la retenue) :

| # | Étape | Fichier:ligne | Durée |
| --- | --- | --- | --- |
| 1 | `MainActivity.onCreate` → `mountKit()` → `kit.preload()` — l'assemblage des 33 paquets part sur le fil démon unique `celeris-langues` | `sample/.../MainActivity.java:81,96-109` ; `core-language/.../LanguageKit.java:167-207` | rend la main immédiatement |
| 2 | `open(Samples.tous().get(0))` — **immédiatement après** le préchargement : la langue est absente tant que le registre ne vit pas | `MainActivity.java:90` | — |
| 3 | `CelerisEditor.open` → `documentAAfficher` → `languageForSiPret` rend **null** (registre en assemblage) ; session créée, document affiché **sans couleur** (éditable) | `android-host/.../CelerisEditor.java:226-270,294-310` | — |
| 4 | `setLanguageWhenRegistryReady` : un fil démon `celeris-langue-ouverte` naît et **attend la fin de l'assemblage des 33 paquets** — y compris les 32 qui ne concernent pas le fichier ouvert | `CelerisEditor.java:426-449` | **mesuré JVM : 338 ms (chaud), 1 050 ms (froid, 1er tour)** ; appareil : **supposé 2–15 s** |
| 5 | Registre posé → `languageFor` rend java → `vue.post` → `vue.setLanguage` → `EditorSyntax.setLanguage` → `rattacher` | `CelerisEditor.java:430-445` ; `android-view/.../EditorSyntax.java:241-252,696-731` | ~0 (post UI) |
| 6 | `EnginePathFactoryLoader.create` — module opt-in absent (cas de `sample`) : `ServiceLoader` relit le META-INF à **chaque** rattachement (résultat nul jamais méémorisé) | `core-syntax/.../EnginePathFactoryLoader.java:40-51` | **mesuré : 7,2 µs par appel** |
| 7 | `AnalysisEngine.on(...)` puis `reanalyser()` → `executor.execute(tache)` : l'exécuteur est la **retenue de 250 ms** de la vue — la première analyse complète attend le minuteur | `EditorSyntax.java:725-730` ; `core-syntax/.../AnalysisEngine.java:121-134` ; `android-view/.../internal/exec/RetainedExecutor.java:127-134` | **250 ms exacts (structurel)** |
| 8 | Au terme : `analyzeWindow` → **`lexFull` du document ENTIER** (la fenêtre ne priorise que le parseur), puis `parseFenetre` | `core-syntax/.../SyntaxService.java:64-70` ; `IncrementalLexer.java:39-57` | mesuré JVM : java écrit 1,1 ms (100 lignes) à 58 ms (20 000) ; python grammaire 9,9 → 262 ms ; **l'exemple du dépôt (petits fichiers) : ~1–10 ms** |
| 9 | `publierSiCourant` → auditeur → `notifyAnalysisReady` → `surAnalysePrete` → invalidation de la vue | `AnalysisEngine.java:273-288` ; `EditorSyntax.java:776-781` | ~0 |
| 10 | Frame suivante : `FrameAssembler.suivreRevision` → `wrap` → `SyntaxRowSource` : **première rangée colorée** | `android-view/.../internal/frame/FrameAssembler.java:176-211` ; `EditorSyntax.java:386-411` | 1 frame |

**Total sur petits fichiers (l'exemple du dépôt)** : assemblage (dominant à froid)
+ 250 ms fixes + ~1–10 ms de lexique + 1 frame. À froid, l'assemblage représente
**75–95 %** du retard (extrapolé appareil) ; la retenue 250 ms en représente
**5–25 %**.

### 3.2 Démarrage à chaud : nouvel onglet jamais ouvert (registre prêt)

Même chaîne à partir de l'étape 5 : **0 ms d'assemblage**, puis 250 ms de retenue,
puis lexique. Pour un petit fichier, la retenue est alors **85–98 % du retard** ;
pour un fichier de 2 000 lignes à grammaire, retenue 250 ms + lexique mesuré
36,7 ms (python, JVM) → la retenue reste dominante (~85 %).

### 3.3 Retour sur un onglet déjà ouvert

Corrigé le 2026-11-19 pour la voie des grammaires : le moteur se **parque** par
session (`EnginePark`), le retour retrouve sa publication — zéro re-tokenisation si
le document n'a pas bougé (`EditorSyntax.attach` l.205-227, `EnginePark.java`).
Si des éditions ont eu lieu pendant l'absence : `catchUpParkedEngine` →
`reanalyser()` → **250 ms de retenue à nouveau** (H1 s'applique aussi au rattrapage)
puis passe incrémentale (µs–ms).

### 3.4 Deux onglets ouverts à moins de 250 ms d'intervalle (H2, prouvé au banc)

L'ouverture de B soumet sa propre tâche : `RetainedExecutor.execute` fait
`cancelPending()` puis `schedule(250)` — **la tâche en attente de A est supprimée**
(le minuteur ne porte qu'un seul créneau pour toute la vue). B publie à son terme ;
**A n'a aucune publication** tant que l'utilisateur ne revient pas à son onglet
(ce retour redéclenche `reanalyser`, avec encore 250 ms de retenue). Banc :
`RetenuePremiereAnalyseTest.deuxMoteursEnMoinsDeLaRetenueChacunGardeSaTache`
(rouge — moteur et vue).

---

## 4. Tableau des constats

Sévérités : **critique** (coloration fausse ou perte de fonction), **majeur**
(retard perceptible ou dette de robustesse), **mineur** (hygiène, contrat interne).

| ID | Titre | Sévérité | Statut | Preuve (fichier:ligne / mesure / banc) | Correctif proposé | Test associé | Effort |
|---|---|---|---|---|---|---|---|
| C1 | Fermetures de largeur nulle jamais admises : un bloc à fin `(?=…)`/`$` ne se ferme jamais, l'état contamine tout le fichier (H8) | **critique** | confirmé (banc ×4) | `GrammarLexer.java:190-192` ; csharp réel : sonde et banc | admettre la largeur nulle avec garde anti-boucle (avancer d'un caractère sans progression) | `LargeurNulleFinTest` (4 rouges) | M |
| C2 | Budget de 20 000 essais/ligne déborde dès 310 caractères de JavaScript ; dégradation silencieuse (H9) | **critique** | confirmé (banc + mesure) | `GrammarLexer.java:50,185-188` ; mesure : 20 appels JS = débordement, 630 chars → 119 jetons | budget proportionnel à la ligne (p.ex. 20 000 + 40×longueur) et/ou cache de prochaine correspondance (C7) qui réduit les essais | `BudgetLigneDenseTest` (2 rouges, 1 témoin vert) | S (borne) / M (cache) |
| C3 | La retenue de 250 ms s'applique à la première analyse complète d'une ouverture (H1) | majeur | confirmé (banc) | `RetainedExecutor.java:127-134` ; `AnalysisEngine.java:121-134` ; `SceneDiagnostics.java:57-73` | première soumission d'un moteur sans retenue (la retenue sert la frappe) | `RetenuePremiereAnalyseTest.laPremiereAnalyseNePatientePasLaRetenue` (rouge) | S |
| C4 | Créneau de retenue unique par vue : l'ouverture rapide d'un onglet détruit la tâche d'un autre (H2) | majeur | confirmé (banc ×2) | `RetainedExecutor.execute` l.127-134 ; `SceneDiagnostics.java:64-66` ; `EnginePark.java` | retenue par moteur (handle d'annulation par planification, minuteur partagé) | `deuxMoteursEnMoinsDeLaRetenue…` + `lePremierOngletGardeSaPublication…` (rouges) | M |
| C5 | La langue du fichier ouvert attend l'assemblage séquentiel des 33 paquets sur un fil unique (H3) | majeur | confirmé (code + mesure) | `MainActivity.java:81,90` ; `LanguageKit.java:98-99,167-207,354-363` ; mesure 338–1 050 ms JVM | assemblage paresseux par paquet : résoudre d'abord le paquet du fichier ouvert ; à terme cache disque des grammaires compilées | banc d'attente max (langue du 1er fichier < assemblage total) | M-L |
| C6 | La première publication exige le lexique du document entier ; la fenêtre ne priorise que le parseur (H4) | majeur | confirmé (code + mesure) | `SyntaxService.java:64-70` ; `IncrementalLexer.java:39-57` ; mesure : python 20 000 lignes = 274 ms de lexique avant la 1re publication | publier un lexique partiel (fenêtre + marge) puis compléter ; ou prioriser le lexique de fenêtre avec états d'entrée persistés | banc de chronologie (publication partielle < complète) | M |
| C7 | Lexeur de grammaires : recherche règle par règle à chaque jeton, `Matcher` alloué par recherche, `IdentityHashMap` par position (H5) | majeur | confirmé (code + mesure) | `GrammarLexer.java:122,164-195` ; mesure : 13,7 µs/ligne python vs 1,5 java ; 399 Mo alloués / 20 000 lignes ; ligne dense 3 290 chars = **234 ms** | cache de prochaine correspondance par règle (recherche depuis la position cachée, invalider sous la borne) | micro-banc de coût par ligne + allocation (annexe) | M-L |
| C8 | `wrapStale` appelle `syntaxeDuLangage.lexer()` qui rend une instance neuve à chaque rattrapage ; compteur `incidents()` perdu (H6) | mineur | confirmé (lecture) | `EditorSyntax.java:478-481` ; `GrammarSyntax.java:43-46` | mémoriser le lexeur du langage dans le pont (champ) | banc : même instance au rattrapage | S |
| C9 | Chargeur de voie : le résultat nul n'est pas méémorisé, champ non `volatile` — la Javadoc promet l'unicité (H7) | mineur | confirmé (banc + mesure) | `EnginePathFactoryLoader.java:24-25,40-51` ; banc : 2 lectures META-INF pour 2 appels ; 7,2 µs/appel | drapeau « chargé » méémorisé (NullObject) + `volatile`/méthode synchronisée | `ChargeurVoieRechercheTest.leResultatNulSeMemorise` (rouge) | S |
| C10 | `setLanguage` documenté « la langue inchangée ne re-rattache rien » mais vide le rattrapage inconditionnellement (flash de texte brut) (H10) | majeur | confirmé (banc) | `EditorSyntax.java:229-252` (Javadoc l.234-237 vs code l.241-252) | garde d'égalité en tête de méthode (retour immédiat si même langue) | `SetLangueInchangeeTest.laLangueInchangeeNeVidePasLeRattrapage` (rouge) | S |
| C11 | Voie native non parquée : refermée et recréée à chaque changement d'onglet (re-analyse complète), repli de complétion muet (H13) | majeur | confirmé (lecture — banc impossible sans liaison C) | `EditorSyntax.java:292-298,550-553,696-774` (seul `engine` est parqué/géré) ; `NativeEngine.java` | parquer `SyntaxEngine` (contrat commun) comme `AnalysisEngine` ; alimentation de `analyseDeRepli` | banc CI Android (liaison présente) | M |
| C12 | Pose différée de langue : un fil démon par ouverture sans langue, non borné ; ouverture dépassée jamais servie jusqu'à réouverture (H14) | mineur | confirmé (lecture) | `CelerisEditor.java:426-449` (garde l.431 ; `documents.put` l.438 dans la garde ; fil l.428-448) | exécuteur partagé borné pour la pose différée ; réessai enregistré hors garde | banc de fils (Robolectric) | S |
| C13 | `reload()` non synchronisé, `finAssemblage` non réinitialisé : le registre périmé d'avant rechargement gagne la course (H15) | majeur | confirmé (banc) | `LanguageKit.java:256-259` vs `167-175,262-292` ; banc : génération d'avant servie | `reload()` synchronisé, génération d'assemblage (le résultat trop vieux se jette) | `RechargementCourseTest` (rouge) | S-M |
| C14 | Égalité d'état de la voie grammaires trop lâche ? (H11) | — | **réfuté** | `GrammarState` (record) → `OpenBlock.equals` (règle + fermeture substituée, `OpenBlock.java:77-83`) → `BlockRule` (record, `Regex` par identité — même instance de grammaire) ; corroboré par `DivergenceNulleBancTest` (1 150 éditions, zéro divergence) et `GrammaireProprietesTest` | rien à corriger | existants | — |
| C15 | Surcouche sémantique : jetons périmés colorant une révision plus récente ? (H12) | — | **réfuté** | `SemanticOverlay.recaler` l.270-278 vide toute révision différente ; effacement au détachement d'onglet (`EditorPopups.java:927`) ; banc B1 existant | rien à corriger (noter la Javadoc de classe qui évoque un « recalage de ligne » inexistant — D17) | existants | — |
| D1 | `.jqwik-database` versionnés dans 8 modules alors que `.gitignore` les exclut | mineur | confirmé | `git ls-files` : 8 fichiers ; `.gitignore:22-24` | retirer de l'index (`git rm --cached`), le bannir | vérif `git ls-files` | XS |
| D2 | Miroirs Aliyun committés sous un commentaire « PATCH LOCAL SANDBOX (jamais commité) » | majeur (reproductibilité/provenance) | confirmé | `settings.gradle.kts:5-22` ; `build-logic/settings.gradle.kts:5-10` | retirer du dépôt ; miroir par init-script local non versionné (pratique déjà suivie pour Robolectric) | `check-dependency-provenance` étendu aux URL de dépôts ? | S |
| D3 | Constante dupliquée `DELAU_RETENU_PAR_DEFAUT_MS = 250L` | mineur | confirmé | `EditorView.java:101` et `EditorAccess.java:243` | une seule source (la vue expose, le pont lit) | grep | XS |
| D4 | AGENT.md annonce « treize modules » et omet `core-syntax-treesitter`, `celeris`, `celeris-bom` ; `settings.gradle.kts` en inclut 16 ; `core-lsp` décrit « client LSP » (ARCHITECTURE) contre « API du domaine LSP » (AGENT) | mineur | confirmé | `AGENT.md:11-29` ; `settings.gradle.kts` (16 `include`) ; `ARCHITECTURE.md:41` | régénérer la table des modules | lecture croisée | S |
| D5 | Commentaires évoquant encore « le démo » supprimé | mineur | confirmé | `CelerisEditor.java:283,285` ; `Samples.java:11` ; bancs `FusionDelColoréTest.java:21,74` | passer à « l'exemple » | grep « démo » | XS |
| D6 | Résidus de refactoring : doubles espaces `class X  implements`/`class X  {` (≥ 12 fichiers), indentation cassée de `public default void shutdown()` (×2), Javadoc dupliquée au-dessus de `LanguageKit.attendre` et de `EditorAccess.POLICE_PAR_DEFAUT`, Javadoc de `SyntaxService.of` séparée de sa méthode par `language()` | mineur | confirmé | `RetainedExecutor.java:26,48,82` ; `SceneDiagnostics.java:28` ; `LanguageKit.java:294-302` ; `EditorAccess.java:236-239` ; `SyntaxService.java:22-34` | nettoyage mécanique | Checkstyle (WhitespaceAfter?) ou relecture | S |
| D7 | Noms qualifiés complets (`java.util.List`, `java.util.function.Consumer`…) dans `EditorSyntax` malgré les imports (23 occurrences) | mineur | confirmé | `EditorSyntax.java` (l.108,114,124,396…) | importer et abréger | Checkstyle | XS |
| D8 | `CHANGELOG.md` pèse 232 Ko pour 22 versions — AGENT.md exige « une entrée par version, court par construction » | mineur | confirmé | `wc -c CHANGELOG.md` = 232 253 ; 22 titres `##` | régime : les détails vivent dans les ADR/rapports, le changelog résume | contrôle de taille en CI | S |
| D9 | Rempart de langue : la baseline ne porte que « sample 13 » ; le cliquet ne voit pas les identifiants français en minuscule/milieu (`rattacher`, `surEdition`, `editionsEnAttente`, `GuidesStructurels`…) | mineur | confirmé | `scripts/language-baseline.txt` ; `scripts/check-language-baseline` (motif à initiale capitale) ; 24 occurrences dans le seul `EditorSyntax` | élargir le motif (casse) et figer le décompte réel, ou assumer l'exemption interne | `check-language-baseline --maj` | S |
| D10 | La documentation dit « trente-deux langages livrés » ; le dépôt en embarque 33 | mineur | confirmé | `LanguageKit.java:94` ; `ls core-language/src/main/resources/langages` = 33 | corriger le décompte (ou le générer) | lecture | XS |
| D11 | `sample` ne ferme jamais la session d'un onglet fermé : sessions, moteurs parqués et lexiques vivent jusqu'à `close()` de la façade | majeur (mémoire) | confirmé (lecture) | `MainActivity.closeTab:298-310` (aucune fermeture de document) ; `CelerisEditor` (aucune API de fermeture par document) ; `EnginePark` (vide seulement à `closeAll`) | API `close(fileName)` sur la façade + banc mémoire | banc de parc (taille après fermeture) | M |
| D12 | `BORNE_OCTETS` (200 Ko) compare des **caractères** (`text.length()`), le nom ment | mineur | confirmé | `NativeEngine.java:53,271` | renommer (`BORNE_CARACTERES`) ou mesurer en octets | lecture | XS |
| D13 | Avertissements `java.util.regex` (« nested repeat operator … replaced », « ']' without escape ») à CHAQUE assemblage — le glob de noms de fichiers (`LanguageRegistry`) compile des motifs dérivés des paquets avec le regex du JDK | mineur | confirmé | `LanguageRegistry.java:361,367` ; sortie d'assemblage (annexe A) | filtrer/assainir les globs ; l'ADR-014 interdit le moteur du JDK pour les EXPRESSIONS, le glob y échappe mais pollue la console | banc silencieux d'assemblage | S |
| D14 | Aucune `gradle/dependency-verification-metadata.xml` malgré « sommes de contrôle » annoncées (ADR-014, M7.6) ; avec les miroirs Aliyun (D2), la provenance ne se vérifie nulle part | majeur (chaîne d'approvisionnement) | confirmé | `ls gradle/` (catalogue + wrapper seulement) | activer la vérification de dépendances Gradle | build avec vérification | M |
| D15 | `EditorView.java` 1 069 lignes et `EditorSyntax.java` 854 lignes, toutes deux au-dessus du plafond de 600 — suppressions Checkstyle sans plafond cible documenté | mineur (gouvernance) | confirmé | `config/checkstyle/suppressions.xml:56-57,65-66` ; ADR-019 ne couvre que les bancs | documenter les nominations relevées et leurs cibles | lecture | S |
| D16 | Javadoc de `SemanticOverlay` promet un « recalage de ligne » qui n'existe pas (le recalage = vidage ou identité) | mineur | confirmé | `SemanticOverlay.java:25-27` vs `recaler:270-278` | corriger la Javadoc | lecture | XS |
| D17 | Le JSON n'a pas de grammaire embarquée (analyseur écrit) : la « longue traîne » documentée omet ce cas et le corpus du prompt (§ B.1) ne peut pas comparer json par grammaire | mineur | confirmé | `core-language/src/main/resources/langages/json/` (pas de `grammar.json`) ; `JsonSyntax` (core-syntax) | le dire dans la doc des paquets | lecture | XS |

---

## 5. Détail des constats critiques et majeurs

### C1 — Les fermetures de largeur nulle ne ferment jamais (H8) — CRITIQUE

**Description.** `GrammarLexer.chercher` rejette toute correspondance dont
l'extrémité égale le début (`m.getEnd() == m.getBegin()`, `GrammarLexer.java:190-192`).
Or le format TextMate utilise légitimement des fermetures de largeur nulle :
anticipations `(?=…)`, ancres `$`, `^`. Un bloc dont la fermeture est ainsi large de
zéro **ne se ferme jamais** : l'état ouvert traverse toutes les lignes suivantes et
leur applique la portée de contenu du bloc.

**Reproduction minimale (banc posé, rouge).**
Grammaire construite au banc : bloc ouvert par `«`, fermé par `(?=»)` —
`LargeurNulleFinTest.fermetureAnticipationGrammaireMinimale` échoue :
`expected: <false> but was: <true>` (le bloc reste ouvert après `« contenu »`).
Cas réel : grammaire **csharp** livrée, ligne `class Z { int a = 1; }` — la
déclaration ouvre un bloc à fermeture `(?<=\})|(?=;)` ; la sonde de l'audit mesure :
`KEYWORD[0..5] STRING[5..26]` — **deux jetons pour toute la ligne**, état ouvert en
fin de ligne. La ligne suivante hérite de la pile du bloc : tout le fichier après la
première déclaration est colorié dans la portée du bloc. Au moins dix grammaires
livrées portent de telles fermetures (csharp, css, dart, go, html, javascript, lua,
php, ruby, swift — extraction de l'annexe A).

**Cause racine.** Le rejet protège d'une boucle infinie (une correspondance qui
n'avance pas ferait tourner `lex` sans fin) ; il a été étendu à tort à TOUTE
correspondance de largeur nulle, fermetures comprises.

**Correctif.** Admettre la largeur nulle **pour la fermeture du bloc ouvert**
(l'emettre FIN ne déplace pas `pos` au-delà du match : après `depiler`, `pos`
reçoit `candidat.end()` — égal au début — puis **avance d'au moins un caractère**
si la correspondance n'a pas progressé : la garde anti-boucle devient « avancer
d'un caractère quand la fermeture est de largeur nulle », exactement comme les
moteurs TextMate de référence). Les ouvertures de largeur nulle (`begin` en
anticipation, p.ex. csharp `(?=\benum\b)`) suivent la même règle : le jeton
d'ouverture émis est vide, l'état empile, la position avance d'un caractère.
**Diff proposé** (pseudo) :

```java
// GrammarLexer.lex — boucle principale
Candidat candidat = premier(donnees, pos, etat);
if (candidat == null) { gap(...); return figer(tokens, etat); }
gap(donnees, tokens, pos, candidat.start(), etat);
etat = emettre(tokens, donnees, candidat, etat);
int suivant = Math.max(candidat.end(), candidat.start() + 1); // garde anti-boucle
pos = suivant;
```

et dans `chercher` : ne rejeter la correspondance de largeur nulle **que** pour les
motifs simples (elle ne produirait rien), l'admettre pour `FIN` et `DEBUT`.

**Test.** `LargeurNulleFinTest` (4 bancs) : grammaire minimale `(?=»)`, ancre `$`,
csharp réel ×2 — tous rouges aujourd'hui, verts attendus après correctif. Le banc
`GrammaireProprietesTest` (propriété jqwik incrémental == complet, python) doit
rester vert : la convergence d'état ne change pas.

**Risque.** Les instantanés de conformité (`GrammairesConformiteTest`,
`Paquet*IntegreTest`) peuvent bouger sur les grammaires concernées — ils décrivent
l'état ACTUEL (bogué) : à recaler fichier par fichier après vérification visuelle
des portées attendues.

### C2 — Le budget de 20 000 essais par ligne déborde dès 310 caractères (H9) — CRITIQUE

**Description.** Chaque recherche de règle consomme un essai
(`GrammarLexer.java:185-188`), le plafond par défaut est 20 000 (l.50). À chaque
jeton émis, `premier` recherche la fermeture PUIS toutes les règles du contexte :
le coût en essais est ~`jetons × règles du contexte`. La grammaire javascript
livrée consomme ses 20 000 essais en ~120 jetons.

**Mesures (banc + sonde, annexe A).** Ligne de 20 appels `foo(42, "x");`
(310 caractères) : **débordement** (1 incident). Ligne de 40 appels (630
caractères) : **119 jetons émis au budget par défaut contre 280 à budget large**
— la seconde moitié de la ligne reste en portée de contexte. Le contenu réparti
(1 appel par ligne) ne déborde jamais : c'est la densité par ligne qui paie. Le
bundle minifié réel (des dizaines de kilo-octets sur une ligne) ne colore donc que
ses ~119 premiers jetons.

**Silence.** Aucun jeton ne signale la truncature ; un `WARNING` de journal seul
(`deborder`, l.198-207). L'utilisateur voit une fin de ligne monochrome.

**Correctif.** Deux voies cumulables : (a) **borne proportionnelle** :
`budget = max(20_000, 40 × longueurLigne)` — le budget doit borner le travail
catastrophique, pas la coloration d'une ligne dense ordinaire ; (b) le cache de
prochaine correspondance (C7) réduit mécaniquement les essais (une règle relue
depuis son cache ne consomme pas d'essai). Recommandation : (a) immédiatement
(S), (b) ensuite.

**Test.** `BudgetLigneDenseTest` : ligne de 310 caractères → aucun incident, aucun
jeton perdu (rouge aujourd'hui) ; témoin « répartie » vert (ne doit pas régresser).

### C3 — La retenue de 250 ms s'applique à l'ouverture (H1) — MAJEUR

**Preuve.** `AnalysisEngine.reanalyser` (l.121-134) soumet par
`executor.execute(tache)` ; l'exécuteur de la vue est le `RetainedExecutor`
(`SceneDiagnostics.java:57-73`, délai `EditorAccess.DELAI_RETENU_PAR_DEFAUT_MS =
250L`) ; `RetainedExecutor.execute` (l.127-134) programme TOUTE soumission au
minuteur. La première analyse complète — celle qui apporte la première couleur —
patiente donc 250 ms après la pose de la langue, alors qu'aucune frappe ne la
précède. Banc rouge : `laPremiereAnalyseNePatientePasLaRetenue`.

**Correctif.** La retenue sert la coalescence de la frappe : la **première**
soumission d'un moteur part direct à la base (voir C4 pour le siège de la logique).
Le retour d'onglet avec éditions manquées peut rester retenu (rafale possible) ou
partir direct — recommandation : direct aussi (c'est une ouverture).

**Test.** `RetenuePremiereAnalyseTest` : première soumission → 0 programmation au
minuteur, tâche à la base, publication servie ; l'édition suivante se retient
(1 programmation) — la coalescence garde son rôle.

### C4 — Un seul créneau de retenue pour toute la vue (H2) — MAJEUR

**Preuve.** Le `RetainedExecutor` vit une fois par vue
(`SceneDiagnostics.java:64-66`) et son minuteur ne porte qu'une tâche en attente
(`daemonDelayer` : une `AtomicReference`, l.58-86 ; `execute` : `cancelPending`
puis `schedule`). Deux moteurs d'une même vue (deux onglets) se partagent le
créneau : la soumission de B **supprime** la tâche de A. Bancs rouges (moteur et
vue) : `deuxMoteursEnMoinsDeLaRetenueChacunGardeSaTache`,
`lePremierOngletGardeSaPublicationMalgreLouvertureRapide` — dans ce dernier,
l'onglet A rouvert n'a **aucune** couleur : sa publication n'a jamais existé.

**Correctif.** Retenue **par moteur** : le créneau d'attente devient un état du
`RetainedExecutor` lui-même (une instance par moteur), le minuteur reste partagé.
Redesign minimal du contrat `Delayer` (interne, hors manifeste) :
`schedule(delayMs, task)` rend une **commande d'annulation** (le créneau vit chez
l'appelant) ; `SceneDiagnostics` fabrique base + minuteur, `EditorSyntax.rattacher`
enveloppe par moteur. La première soumission directe (C3) se posant dans la même
classe, les deux correctifs se livrent ensemble.

**Test.** Les deux bancs rouges ci-dessus ; les bancs existants
(`MultiOngletsBancTest`, `PublicationChaqueRevisionBancTest`) doivent rester verts.

### C5 — La langue du premier fichier attend les 33 paquets (H3) — MAJEUR

**Preuve.** `MainActivity.onCreate` lance `preload()` (l.81, via `mountKit`
l.96-109) puis `open(...)` (l.90) immédiatement : `languageForSiPret` rend null
tant que le registre n'est pas posé, et le registre n'est posé qu'à la fin de
l'assemblage **complet** des paquets (`LanguageKit.partirAssemblage` →
`assembleOnThread` → `plateforme.build()`, séquentiel sur le fil démon unique
`celeris-langues`). **Mesuré** : 1 050 ms à froid, 338–444 ms en chaud (JVM
sandbox 2 CPU, annexe A) ; la Javadoc du kit dit elle-même « jusqu'à une minute
sur un appareil lent » (`LanguageKit.java:297-301`). Le fichier ouvert attend
l'assemblage des 32 paquets qui ne le concernent pas ; s'ajoutent ensuite le fil
`celeris-langue-ouverte` (C12) puis la retenue (C3).

**Correctif.** Assemblage **paresseux par paquet** : le résolveur d'attente
(`registre()`) rend dès que le paquet DU FICHIER demandé est assemblé — l'ordre
d'assemblage place les paquets demandés en tête de file. Variante plus légère :
l'assemblage embarque d'abord les paquets des langages « courants » (java, kotlin,
xml, json, markdown) — arbitraire, moins propre. À terme : **cache disque** des
grammaires compilées (sérialisation des `Regex` joni impossible — mais le paquet
compilé en représentation intermédiaire l'est) ; l'étude dit si l'assemblage
domine sur appareil (hypothèse oui : 338–1 050 ms JVM → secondes appareil).

**Test.** Banc : kit sur exécuteur direct, source à N paquets ; `languageFor("x.java)`
rend AVANT la fin de l'assemblage des N paquets (horloge injectable : compter les
paquets assemblés au moment de la résolution).

### C6 — Le lexique complet avant la première publication (H4) — MAJEUR

**Preuve.** `SyntaxService.analyzeWindow` (l.64-70) appelle
`lexer.lexFull(doc, …)` sur TOUT le document avant `parseFenetre` ; le commentaire
l'assume (« lexicographie complète — les états se cascadent de la première ligne »).
La coloration ne dépend que du lexique : l'écran attend les lignes invisibles.
**Mesuré** (annexe A) : lexique python 20 000 lignes = 274 ms (JVM) avant la
première publication ; java écrit 30 ms. Sur appareil (supposé ×5–15) : 0,15–4 s.

**Correctif.** Publier un **lexique partiel** (fenêtre visible + marge) dès qu'il
est prêt, puis compléter hors fenêtre — exige de savoir reprendre le lexique
au-delà de la fenêtre : soit persistér l'état d'entrée de la première ligne hors
fenêtre (l'état de sortie de la dernière ligne lexée suffit — il est déjà produit
par la passe), soit assumer le plein pour le lexique et ne prioriser que le
parseur (statu quo documenté). La voie « états d'entrée persistés » est propre :
`LexDocument` connaît déjà `stateBefore(0)` ; la reprise depuis la ligne N+1 avec
l'état de sortie de la ligne N est exactement le mécanisme de `lexIncremental`.

**Test.** Banc : publication partielle rendue avant la fin du lexique complet
(horloge injectable : la fenêtre 0..40 publiée alors que `lexFull` continue).

### C7 — Le coût par jeton du lexeur de grammaires (H5) — MAJEUR

**Preuve (code).** À chaque position : `premier` alloue un
`IdentityHashMap`-backed `Set` (l.122), parcourt TOUTES les règles du contexte
(`parcourir`), et `chercher` alloue un `Matcher` joni **par règle par position**
(l.189) sans aucun cache de « prochaine correspondance » — coût
O(jetons × règles × reste de ligne).

**Preuve (mesures, annexe A).** Par ligne (20 000 lignes, min de 5) :
python 13,7 µs, yaml 4,7 µs, markdown 3,2 µs — contre **1,5 µs** pour le lexeur
Java écrit (rapport ×9 pour python). Allocations d'une passe complète : 399 Mo
(python 20 000 lignes) — ~500 octets par caractère de source. **Ligne dense
python de 3 290 caractères : 234 ms pour UNE ligne** (et débordement de budget,
C2). À l'échelle appareil, cette ligne coûte des secondes.

**Correctif.** Cache de prochaine correspondance **par règle** : mémoriser, par
règle, la prochaine position où elle matche (et son match) ; tant que la position
courante précède cette borne, la règle ne se relit pas — invalidation quand une
correspondance consommée franchit la borne. C'est l'optimisation classique des
moteurs TextMate ; elle réduit aussi les essais (C2). Complément : allouer le
`Set` de déduplication des listes une fois par ligne (pas par position) et
réutiliser les `Matcher` (joni le permet : `matcher.setTarget`? — à l'étude ; le
gain principal est le cache).

**Test.** Micro-banc de non-régression de coût : lexFull d'un corpus fixé sous
un plafond mesuré au commit de base (JVM, marge ×3) — le dépôt a déjà l'habitude
des seuils de benchmark (ADR-004/006).

### C10 — `setLanguage` même langue : le rattrapage se vide (H10) — MAJEUR

**Preuve.** La Javadoc (l.234-237) promet « La LANGUE inchangée ne re-rattache
rien » ; le code (l.241-252) vide `editionsEnAttente`, `chaineRompue` et `guides`
**sans condition**, puis `rattacher` (dont la garde `catchUpParkedEngine` évite
la re-création du moteur — mais les états vidés restent vidés). Avec une
publication périmée et des éditions en attente, `wrapStale` (l.462-482) ne trouve
plus rien à rattraper : la source repart **sans couleur** jusqu'à la publication
suivante — un flash de texte brut. Banc rouge :
`SetLangueInchangeeTest.laLangueInchangeeNeVidePasLeRattrapage`.

**Correctif.** Garde d'égalité en tête : si `nouvelleLangue == language` (et
session inchangée), retour immédiat — la méthode devient exactement ce que sa
Javadoc dit.

**Test.** Le banc rouge ci-dessus (le contrôle « langue différente re-rattache »
est vert et doit le rester).

### C11 — La voie native n'est pas parquée (H13) — MAJEUR (hôtes tree-sitter)

**Preuve (lecture — pas de banc possible sans la bibliothèque C).** Le parc
(`EnginePark`), la fenêtre (`window` l.292-298), le repli de complétion
(`analyseDeRepli` l.550-553) et le rattrapage (`catchUpParkedEngine` l.740-759)
ne connaissent QUE `AnalysisEngine`. Le moteur natif (`nativePathEngine`) est
refermé à chaque changement d'onglet (`closeEngine` l.761-774) et recréé
(`rattacher` l.712-718) : re-analyse complète à chaque retour d'onglet — le bug
« la coloration se refait à chaque onglet », corrigé le 2026-11-19 pour la voie
des grammaires, **reste ouvert pour la voie native** ; le repli de complétion y
est muet (`analyseDeRepli` rend null).

**Correctif.** Parquer par le **contrat** (`SyntaxEngine`) : le parc devient
`Map<TextSession, SyntaxEngine>`, `window`/`analyseDeRepli`/`catchUpParkedEngine`
passent par le champ unique. La validation exige la CI Android (liaison C) — les
bancs JVM sautent (`assumeTrue`, comme les bancs natifs existants).

**Test.** Banc CI Android : 3 onglets, 10 bascules, compteur de passes natives
(l'API `NativeEngine` n'expose pas encore de compteur : l'ajouter ou compter par
auditeur de publication).

### C13 — `reload()` en course avec l'assemblage (H15) — MAJEUR

**Preuve.** `reload()` (l.256-259) n'est pas synchronisé et ne réinitialise pas
`finAssemblage` ; l'assemblage en vol pose son registre sous
`if (registre == null)` (l.192-196). Banc rouge
(`RechargementCourseTest`) : un rechargement demandé PENDANT l'assemblage laisse
l'assemblage d'avant gagner — **le registre périmé est servi, le nouveau est
jeté en silence**. Scénario connexe documenté dans le code : l'assemblage
>120 s rend un registre vide en silence (l.297-302, `attendre` l.303-313).

**Correctif.** `reload()` sous moniteur ; génération d'assemblage incrémentée à
chaque `reload`/`preload` : un résultat de génération dépassée se jette ; la
résolution en attente relit la génération courante.

**Test.** `RechargementCourseTest` (rouge) : la génération posée après reload
doit être servie.

---

## 6. Incohérences de documentation et de configuration

Reprise et complément de la liste du prompt (§ 5) — toutes confirmées par commande
ou lecture ; détail dans le tableau D1–D17 (§ 4). Ajouts propres à cet audit :

1. **D2 (miroirs Aliyun)** : l'impact concret est double — (a) tout build, CI
   comprise, résout depuis un miroir tiers non déclaré dans la politique de
   provenance (`scripts/check-dependency-provenance` ne vérifie que les motifs
   interdits, pas les URL de dépôts autorisées) ; (b) le commentaire
   « jamais commité » est faux — le patch l'a été. Le contournement propre
   existait déjà dans le dépôt pour Robolectric : init-script local non versionné.
2. **D14** : sans `dependency-verification-metadata.xml` ni vérification de
   sommes, la règle 5/7 du jalon (« SBOM, sommes de contrôle ») n'est tenue par
   AUCUN mécanisme de build — c'est une case vide de la chaîne d'approvisionnement.
3. **D6** : les Javadoc **dupliquées** (deux blocs successifs au-dessus de
   `LanguageKit.attendre` et de `EditorAccess.POLICE_PAR_DEFAUT`) sont des
   cicatrices de déplacement de code — invisibles à la lecture rapide, elles
   documentent deux fois des choses légèrement différentes (le contenu diverge).
4. **D17** : le paquet `json` livré n'a **pas** de `grammar.json` (l'analyseur
   écrit sert) : la matrice de parité et la doc des paquets gagnent à le dire —
   le lecteur qui cherche « la grammaire JSON » dans `langages/json/` ne trouve
   rien.
5. La sonde d'assemblage (annexe A) émet **>20 avertissements java.util.regex**
   à chaque montage des paquets (D13) : bruit de console à chaque démarrage
   d'application hôte, trompeur pour qui cherche un vrai problème de grammaire.

---

## 7. Feuille de route des correctifs

Ordre proposé : d'abord la **fidélité** (coloration fausse), ensuite la
**latence**, ensuite la robustesse ; chaque étape est un commit avec son banc
(rouge avant, vert après), indépendante et réversible — le régime de la feuille
de route du dépôt.

| # | Correctif | Constats | Effort | Dépend | Risque de régression | Critère de sortie mesurable |
| --- | --- | --- | --- | --- | --- | --- |
| F1 | Largeur nulle admise (fermeture/ouverture) + garde anti-boucle | C1 | M | — | instantanés de conformité à recaler (csharp, css, …) | `LargeurNulleFinTest` 4/4 vert ; conformité recalée ; propriété jqwik verte |
| F2 | Budget par ligne proportionnel (`max(20 000, 40×longueur)`) | C2 | S | — | aucun (la borne monte, jamais ne descend) | `BudgetLigneDenseTest` 3/3 vert ; zéro incident sur toute ligne < 10 000 caractères |
| F3 | Première analyse sans retenue | C3 | S | F4 (même classe) | nul (l'ouverture ne coalescence rien) | banc `laPremiereAnalyseNePatientePasLaRetenue` vert |
| F4 | Retenue par moteur (créneau par `RetainedExecutor`, minuteur partagé) | C4 | M | — | bancs multi-onglets à rejouer | 2 bancs rouges verts ; `MultiOngletsBancTest` reste vert |
| F5 | `setLanguage` même langue : no-op | C10 | S | — | nul | banc vert ; Javadoc enfin vraie |
| F6 | Chargeur de voie : nul méémorisé + `volatile` | C9 | S | — | nul | `ChargeurVoieRechercheTest` vert |
| F7 | `reload()` synchronisé + génération d'assemblage | C13 | S-M | — | nul (le chemin râché devient correct) | `RechargementCourseTest` vert |
| F8 | Cache de prochaine correspondance par règle | C7, C2 | M-L | F2 (mesures) | fidélité des jetons (bancs de conformité + propriétés) | coût/ligne python ≤ ×2 du lexeur écrit (mesure annexe A reprise) ; essais/ligne ≤ 20 % |
| F9 | Assemblage : le paquet du fichier ouvert d'abord | C5 | M-L | — | ordre des erreurs de paquets (journalisation) | langue du 1er fichier rendue avant la fin de l'assemblage total (banc horloge) |
| F10 | Publication par fenêtre du LEXIQUE (états d'entrée persistés) | C6 | M | F8 souhaitable | fusion avec la complétion par lots existante | 1re publication d'un 20 000 lignes < lexique complet (banc) |
| F11 | Voie native parquée (contrat `SyntaxEngine`) | C11 | M | — | CI Android (liaison) obligatoire | 10 bascules d'onglets = zéro re-analyse native (CI) |
| F12 | Pose différée : exécuteur borné ; réessai hors garde | C12 | S | — | nul | banc : N ouvertures = ≤1 fil de pose ; langue servie au retour |
| F13 | Lexeur du rattrapage méémorisé | C8 | S | — | nul | même instance entre deux `wrapStale` |
| F14 | Hygiène D1–D17 (jqwik-db, Aliyun, constante, AGENT 16 modules, « démo », doubles espaces, Javadocs, noms qualifiés, CHANGELOG, baseline, 33 langages, fermeture d'onglet, BORNE_, globs, vérification de dépendances, nominations Checkstyle, Javadoc surcouche, json sans grammaire) | D1–D17 | S (D2, D11, D14 : M) | — | D2 exige un miroir local documenté pour la sandbox | `./gradlew check` vert ; `git ls-files` sans `.jqwik-database` ; politique langue/dates verte |

**Gains attendus, quantifiés sur les mesures de l'annexe A** (JVM sandbox ; les
parts appareil sont des extrapolations annoncées) :

- À chaud, petit fichier : retard ~250 ms → **~0 ms** (F3+F4) — la couleur suit la
  pose de la langue.
- À froid : le facteur dominant devient l'assemblage ; F9 le réduit au paquet du
  fichier ouvert (mesure à reprendre après F9 ; hypothèse : de 338–1 050 ms à
  <100 ms JVM pour un paquet).
- Gros fichier à grammaire : F10 publie la fenêtre sans attendre 274 ms de lexique
  (20 000 lignes python, JVM) ; F8 réduit le coût par ligne (~×9 → cible ×2).
- Fidélité : 10 grammaires livrées recouvrent leurs fermetures (csharp en tête) ;
  les lignes denses colorent entières (F1+F2).

**Critère global proposé** (à calibrer sur l'appareil de référence du mandat,
ADR-006) : « première image colorée < 100 ms au p95 sur un fichier de 2 000 lignes
après registre prêt ; zéro ligne dégradée par budget sur toute ligne < 10 000
caractères ; zéro re-tokenisation au retour d'onglet (les deux voies) ».

---

## 8. Annexes

### Annexe A — Données brutes des mesures (JVM sandbox 2 CPU, JDK 17.0.21, min de 5 après échauffement)

**Assemblage du registre (33 paquets, fil direct — le travail réel du fil démon)** :

| Tour | Durée | Extensions résolues |
| --- | --- | --- |
| 0 (froid, JIT compris) | 1 050 ms | 20/20 |
| 1 | 444 ms | 20/20 |
| 2 | 338 ms | 20/20 |

**`lexFull` par taille (lexique seul, le prérequis de la première publication)** :

| Langue (voie) | 100 lignes | 2 000 lignes | 20 000 lignes | µs/ligne (20 000) | Mo alloués (20 000) |
| --- | --- | --- | --- | --- | --- |
| java (écrit) | 0,43 ms | 4,67 ms | 29,98 ms | 1,5 | 34 |
| python (grammaire) | 6,76 ms | 36,05 ms | 274,13 ms | 13,7 | 399 |
| markdown (grammaire) | 1,78 ms | 11,61 ms | 64,53 ms | 3,2 | 140 |
| yaml (grammaire) | 2,73 ms | 12,63 ms | 93,84 ms | 4,7 | 247 |

**`analyzeWindow(0..40)` (première publication complète : lexique + fenêtre)** :

| Langue | 100 lignes | 2 000 lignes | 20 000 lignes |
| --- | --- | --- | --- |
| java (écrit) | 1,13 ms | 9,96 ms | 58,18 ms |
| python (grammaire) | 9,89 ms | 36,67 ms | 261,84 ms |

**Lignes denses** : python, 3 290 caractères : **233,76 ms pour une ligne**, 1 478
jetons, 5,41 Mo alloués, débordement de budget (avertissement journalisé) ;
markdown, 3 290 caractères : 0,10 ms (règles peu nombreuses). JavaScript, ligne de
20 appels (310 caractères) : débordement au budget par défaut ; 40 appels
(630 caractères) : 119 jetons au budget par défaut contre 280 à budget large.

**Chargeur de voie (H7)** : 10 000 appels `create()` sans module opt-in :
71 ms, **7,2 µs par appel** (chaque appel relit le META-INF/services).

**Sortie brute complète** : `mesures-jvm-brutes.txt` livré avec ce rapport.

### Annexe B — Bancs d'audit livrés

Fichiers de test complets (exécutés puis retirés de l'arbre pour le rapport ;
résultats dans le corps du rapport) :

| Banc | Module cible | Tests | Résultat à `f5985ef` |
| --- | --- | --- | --- |
| `LargeurNulleFinTest` | core-syntax-grammars | 4 | 4 rouges (H8 confirmé) |
| `BudgetLigneDenseTest` | core-syntax-grammars | 3 | 2 rouges + 1 témoin vert (H9) |
| `ChargeurVoieRechercheTest` | core-syntax | 1 | rouge (H7) |
| `RechargementCourseTest` | core-language | 1 | rouge (H15) |
| `RetenuePremiereAnalyseTest` | android-view | 4 | 3 rouges (H1, H2×2) + témoin vert |
| `SetLangueInchangeeTest` | android-view | 2 | 1 rouge (H10) + témoin vert |

### Annexe C — Évaluation des bancs existants (demande § B.3)

- `DivergenceNulleBancTest` (core-syntax) : 1 150 éditions à graine fixe sur le
  **lexeur Java écrit** — compare jetons, états de sortie et profondeurs à chaque
  geste. Ce qu'il ne couvre pas : **la voie des grammaires** (le critère « zéro
  divergence » n'est bancé que pour un analyseur écrit), les gros documents (le
  corpus de départ fait ~8 lignes), et les cascades longues (les gestes sont
  biaisés vers le début du document).
- `GrammaireProprietesTest` (core-syntax-grammars, jqwik) : propriété
  `lexIncremental(edit(doc)) == lexFull(edit(doc))` sur le paquet **python seul**,
  120 tirages × 12 éditions, alphabet couvrant chaînes triples et commentaires.
  Ne couvre pas : les 32 autres paquets (heredocs `\1` de markdown/shell,
  imbrications html/php…), les documents longs (corpus de 10 lignes), les
  constructions de largeur nulle (H8 — l'alphabet ne peut pas les produire), ni
  le **parseur** (fenêtre vs complet).
- `MultiOngletsBancTest` / `PublicationChaqueRevisionBancTest` (android-view) :
  verrouillent le parquage et la publication par révision — ne couvrent pas la
  **retenue** (horloge non injectée dans ces bancs) ni la voie native.

### Annexe D — Commandes utilisées

```bash
export JAVA_HOME=<jdk-17> ANDROID_HOME=<sdk-36>
./gradlew :core-syntax-grammars:test            # baseline : 149 verts
./gradlew :core-syntax-grammars:test --tests "jo.celeris.core.syntax.grammars.LargeurNulleFinTest" ...
./gradlew :android-view:testDebugUnitTest --tests "jo.celeris.android.view.RetenuePremiereAnalyseTest" ...
./gradlew :core-text:jar :core-syntax:jar :core-syntax-grammars:jar :core-language:jar
java -cp <jars + joni + jcodings> MesuresCeleris.java   # mesures de l'annexe A
python3 scripts/check-language-baseline && python3 scripts/check-language-policy
git ls-files | grep jqwik ; grep -rn aliyun settings.gradle.kts build-logic/
```

### Annexe E — Fichiers lus (principaux)

`AGENT.md`, `ARCHITECTURE.md`, `USAGE.md` (extraits), `README.md` (extraits),
`docs/feuille-de-route-coloration.md`, `docs/adr/004,006,007,014,0024,0027`,
`CHANGELOG.md` (structure), `gradle/libs.versions.toml`, `settings.gradle.kts`,
`jitpack.yml`, `config/checkstyle/suppressions.xml` ;
`sample/.../MainActivity.java`, `Samples.java` ;
`android-host/.../CelerisEditor.java`, `EditorPopups.java` (extraits) ;
`android-view/.../EditorView.java` (extraits), `EditorSyntax.java`,
`SemanticOverlay.java`, `internal/EnginePark.java`, `internal/access/EditorAccess.java`,
`internal/diagnostics/SceneDiagnostics.java`, `internal/exec/RetainedExecutor.java`,
`internal/frame/FrameAssembler.java` (extraits) ;
`core-language/.../LanguageKit.java`, `LanguageRegistry.java` (extraits),
`packs/PackSource.java` ;
`core-syntax/.../AnalysisEngine.java`, `SyntaxService.java`,
`IncrementalLexer.java`, `EnginePathFactoryLoader.java`, `Token.java`,
`clike/KotlinSyntax.java` (signature) ;
`core-syntax-grammars/.../GrammarLexer.java`, `GrammarSyntax.java`,
`GrammarState.java`, `OpenBlock.java`, `BlockRule.java`, `Grammar.java`,
`Expressions.java` (signatures), `GrammarReport.java` (signature) ;
`core-syntax-treesitter/.../NativeEngine.java` ;
bancs : `EditorSyntaxAsyncTest`, `DivergenceNulleBancTest`,
`GrammaireProprietesTest`, `GrammaireBornageTest`, `LanguageKitTest`,
`PackLoaderTest`, `CelerisEditorTest` (harnais).
