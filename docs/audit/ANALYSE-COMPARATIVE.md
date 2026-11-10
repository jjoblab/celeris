# Analyse comparative — celeris face au dépôt de départ

> **Question posée** : le nouveau projet (celeris, 1.0.0) est-il
> supérieur à l'ancien (le dépôt de départ, `jo.codeeditor:*`, 4.14.1) ? Quels gaps et
> quelles limites restent ?
>
> **Méthode** : les deux dépôts ont été audités localement le 2026-11-10 —
> inventaires complets (architecture, fonctionnalités, tests, sécurité,
> accessibilité, performance, docs, publication), comptages indépendants par
> `git ls-files` et `rg` sur les fichiers suivis uniquement, recoupement avec
> les documents internes des deux dépôts (`docs/audit/ETAT-DES-LIEUX.md`,
> `docs/RESUME-M9.md`, `docs/parite/MATRICE-PARITE.md`,
> `docs/audit/REGISTRE-PORTAGE.md`, `MIGRATION.md`, `FEATURES.md`,
> `ARCHITECTURE.md` de chacun). Chaque chiffre cité ci-dessous a été mesuré
> ou recoupé au moins deux fois ; les points non vérifiables sont signalés.
>
> **Auteur des deux dépôts** : jjoblab (contributeur unique).

---

## 1. Résumé exécutif

**Réponse courte : oui, celeris est supérieur — mais sur les fondations,
pas encore sur le produit.** La plateforme de celeris (architecture modulaire,
moteur de texte, accessibilité, internationalisation, IA, qualité outillée,
sécurité outillée, gouvernance) dépasse nettement celle du dépôt de départ. En
revanche, la parité fonctionnelle effective n'est **pas** atteinte :
couverture de langages (16 contre 29), richesse de l'interface (minimap, fil
d'Ariane, error stripe… absents), raffinements d'intelligence locale
(complétion contextuelle, signature riche, snippets avancés, postfixe),
volume de tests (1 761 contre 3 130 `@Test`) et publication consommable
(zéro tag contre 68 ; `publishToMavenLocal` contre un dépôt distant actif).

Le résultat est cohérent avec l'histoire des deux projets : celeris est une
**réécriture de 7 jours** (2026-11-03 → 2026-11-10, 305 commits, 1.0.0) dont
l'ossature a été portée de l'acquis du dépôt de départ — 45 jours de
développement, 529 commits, 68 versions publiées, figé au commit `f310a39`
(2026-10-30) par le registre de portage de celeris. Le pari de celeris est
que ce qui est difficile à rattraper (la plateforme) est fait, et que ce qui
reste (le contenu, le chrome) est à coût marginal faible grâce aux paquets
de langage en données seules et au kit de popups déjà porté.

### Verdict par dimension

| Dimension | Verdict |
| --- | --- |
| Architecture, modularité (13 modules purs/Android vs 5) | **Supérieur** |
| Frontière LSP (LSP4J 1.0.0 encapsulée vs 0.22.0 figé) | **Supérieur** |
| Compatibilité binaire (manifestes d'API contrôlés en CI) | **Supérieur** |
| Moteur de texte (multi-curseur, UAX #9/#29, transactions) | **Supérieur** |
| Accessibilité, i18n, bidirectionnel | **Très supérieur** |
| IA embarquée (fantôme, agents, MCP, consentement) | **Sans équivalent** |
| Qualité outillée (mutation 85 %, couverture ≥ 90 % bloquante) | **Supérieur** |
| Sécurité outillée (SBOM, sommes SHA-256, R8 vérifié) | **Supérieur** |
| Gouvernance d'ingénierie (17 ADR, audits, matrice contrôlée) | **Supérieur** |
| Hygiène des dépendances (dependabot, SECURITY.md) | **Inférieur** |
| Couverture de langages (16 vs 29) | **Inférieur** |
| Richesse de la vue et du chrome | **Inférieur** |
| Raffinements d'intelligence locale | **Inférieur** |
| Volume de tests (1 761 vs 3 130 `@Test`) | **Inférieur** |
| Preuve de charge (100 000 lignes vs 480 000 lignes) | **Inférieur** |
| Publication consommable (0 tag vs 68) | **Inférieur** |
| Documentation produit (pas d'équivalent USAGE.md) | **Inférieur** |

---

## 2. Les deux projets en un coup d'œil

### 2.1 Le lignage

Le dépôt de départ (`jo.codeeditor:*`) est une bibliothèque Java d'éditeur
de code pour Android : moteur pur, rendu Canvas dans une vue unique,
coloration incrémentale par tokéniseurs, grammaires optionnelles
TextMate-like, client LSP4J. Publiée sur un dépôt distant de paquets,
consommée par un projet hôte réel (cité dans ses scripts et son code),
elle a connu 68 versions publiées de v3.35.0 à v4.14.1.

celeris (`jo.celeris:*`) n'est **pas un fork** : « le nouveau dépôt n'est pas
un fork et ne reprend aucun historique » (`docs/audit/REGISTRE-PORTAGE.md`).
L'ancien dépôt y est nommé « dépôt de départ », traité **en lecture seule au
commit `f310a39`** — précisément le dernier commit du dépôt de départ. Chaque
élément repris y est consigné avec une décision : **porter** (rope, index de
lignes, coalescence d'annulation, recherche/remplacement, kit de popups,
moteur de grammaires), **spécifier** (la session, le rendu, les gestes, le
client LSP — l'ancien code devient spécification et oracle de test), ou
**abandonner** (la sélection mono-curseur, les 19 tokéniseurs manuels, la
`EditorView` de 4 579 lignes, le client LSP4J 0.22.0, les baselines lint).
Le référentiel de parité change aussi : le dépôt de départ se mesurait
à lui-même ;
celeris vise **Android Studio** (matrice de 123 lignes, catégories A à H).

Concrètement : l'ancien est de facto gelé (plus aucun commit depuis le
2026-10-30, figé par contrat dans le registre de portage), et le nouveau en
est l'héritier revendiqué — la dette utile de l'ancien n'est pas perdue,
elle est spécifiée.

### 2.2 Chiffres clés côte à côte

| Mesure | dépôt de départ (ancien) | celeris (nouveau) |
| --- | --- | --- |
| Période de développement | 2026-09-16 → 2026-10-30 (45 j) | 2026-11-03 → 2026-11-10 (7 j) |
| Commits | 529 | 305 |
| Tags | 68 (v3.35.0 → v4.14.1) | 0 |
| Versions au CHANGELOG | 103 historisées | 2 (0.0.1, 1.0.0) |
| Langage produit | Java 100 % (zéro Kotlin) | Java 100 % (Kotlin cantonné au build, ADR-001) |
| Rendu | Canvas, une seule vue | Canvas refondu en peintres |
| Modules Gradle | 5 bibliothèques + démo | 13 modules (8 purs publiés, 3 Android, cli, démo) |
| Fichiers produit / lignes | 323 / 87 665 | 465 / 52 263 |
| Fichiers test / lignes | 376 / 73 307 | 256 / 39 445 |
| Méthodes `@Test` | 3 130 (+5 répétées → 3 185 exécutées) | 1 761 (+21 propriétés jqwik ; check complet : 1 819 verts, politique comprise) |
| Plus gros fichier produit | `EditorView.java`, 4 579 l. (13 fichiers > 1 000 l.) | plafond 600 l. (Checkstyle) |
| Langages intégrés | 29 (14 tokéniseurs) + 23 grammaires | 16 (6 analyseurs écrits + 10 grammaires TextMate) |
| Client LSP | LSP4J 0.22.0 direct, 14 providers | LSP4J 1.0.0 derrière frontière (ADR-015), 13 familles |
| SDK / build | compileSdk 34, minSdk 24, AGP 9.0.0, Gradle 9.5.1, JDK 17 | compileSdk 36, minSdk 26, AGP 9.5.0, Gradle 9.8.2, JDK 21 (release 17) |
| Dépendances embarquées | LSP4J + Gson ; joni/jcodings (jars versionnés) | lsp4j 1.0.0 + Gson 2.10.1 (frontière) ; joni/jcodings (coordonnées) ; serveur-retire (démo) |
| Multi-curseur | absent (planifié, non livré) | natif (A-01) |
| Bidirectionnel (UAX #9) | non supporté (décision explicite) | cœur + rendu |
| Graphèmes (UAX #29) | non (suppression par caractère) | partout (clusters) |
| Couverture de code | Jacoco en rapport, jamais bloquant | JaCoCo ≥ 90 % bloquant (purs) ; PIT mutation 85 % (seuil 80) |
| Baselines lint | 48 entrées figées (536 lignes) | zéro baseline |
| CI | 1 workflow ; chiffres contrôlés ±5 % ; budgets de classes | 1 workflow ; 12 scripts de politique bloquants ; mutation ; publication locale vérifiée |
| Dependabot | oui (hebdomadaire) | **non** |
| SECURITY.md | oui | **non** |
| Manifestes d'API | non (revue manuelle des signatures) | `api/*.api` committés, `apiCheck` bloquant (ADR-009) |
| Publication | dépôt distant de paquets, workflow protégé par tag | `publishToMavenLocal` vert (8 artefacts, SBOM, SHA-256) ; distant configuré non exécuté |
| ADR | 3 | 17 |
| Doc produit | USAGE.md 1 319 l., FEATURES.md 402 l. | MIGRATION.md 143 l. ; pas d'USAGE/FEATURES |
| Dette TODO/FIXME réelle | nulle (66 occurrences toutes volontaires : échantillons, fixtures) | nulle (1 occurrence : grammaire tierce de test) |

---

## 3. Là où le nouveau est supérieur

### 3.1 Architecture

Les deux projets partagent les mêmes principes fondateurs — Java pur, Canvas,
cœur sans Android, un seul point de mutation — mais celeris va sensiblement
plus loin :

- **Topologie** : 13 modules à frontières nettes (8 modules purs JVM publiés,
  3 modules Android, un exécutable `cli`, une démo) contre 5 bibliothèques.
  Le moteur du dépôt de départ vivait dans une bibliothèque Android (`cel-core`) ;
  chez celeris, tout ce qui pense vit en JVM pure, publiable et testable
  sans SDK (le `check` des modules purs tourne en quelques secondes).
- **Plafonds structurels** : Checkstyle 600 lignes/fichier, 50/méthode,
  PMD complexité 15, ArchUnit dans 9 modules — contre 13 fichiers de plus
  de 1 000 lignes chez l'ancien, dont la `EditorView` de 4 579 lignes
  (494 membres publics) que le propre plan de l'ancien voulait démonter.
- **Frontière LSP** (ADR-015) : LSP4J **1.0.0** vit dans un seul module
  (`lsp-protocole`) et n'apparaît jamais dans l'API publique ; le domaine
  LSP (`core-lsp`) est immuable et maison. L'ancien est figé sur LSP4J
  **0.22.0** — la migration vers 1.0.0 (rupture WorkspaceEdit/Hover) y était
  qualifiée de « lot à part entière » jamais livré ; celeris l'a faite.
- **Compatibilité binaire** : manifestes d'API committés (`api/*.api`,
  8 fichiers) contrôlés par un validateur bloquant (ADR-009). L'ancien
  n'avait qu'une revue manuelle des signatures publiques — dette assumée
  dans ses documents.
- **Zéro baseline lint** dès le premier commit (ADR-011), contre 48 entrées
  figées chez l'ancien, doctrine « jamais régénérée ».

### 3.2 Moteur de texte

- **Multi-curseur natif** (A-01) : `MultiSelection`, tri, fusion aux
  recouvrements, projection à gravité droite ; la recherche/remplacement
  et la transaction sont multi-plages. L'ancien l'avait explicitement
  **non livré** (une seule `Selection` par session, plan conservé en
  perspectives).
- **Transactions** : toute mutation passe par la `Transaction`, annulation
  comprise, avec restauration de la sélection (A-13, PIEGE-07). L'ancien
  avait une voie unique équivalente (`replaceRange`) mais mono-plage, avec
  `begin/endBatch`.
- **Unicode normatif au cœur** : segmentation UAX #29 (graphèmes, mots,
  suppression par clusters), résolution bidirectionnelle UAX #9 (niveaux,
  runs, embeddings), encodages stricts (UTF-8/16/32, Latin-1, BOM préservé,
  détection bornée). L'ancien supprimait par caractère et documentait le
  RTL comme **non garanti** (USAGE.md, ADR-001 n° 7 de l'ancien).
- **L'acquis est repris, pas perdu** : rope (re-mesurée à 0,002 ms p50 sur
  2 M caractères), index de lignes à splice incrémental, coalescence
  d'annulation (100 frappes → 1 opération), recherche/remplacement — tous
  portés avec leurs tests et étendus (fuzz à graines fixes, ~10 300
  séquences jqwik contre `StringBuilder` en oracle).

### 3.3 Accessibilité et internationalisation

C'est l'écart le plus net. Le dépôt de départ en est au minimum déclaré : 9 appels
`setContentDescription`, un nœud d'accessibilité minimal sur la barre de
symboles, un plan (`docs/plans/05-accessibilite-i18n-rtl.md`) mesurant
lui-même le constat — jamais livré.

celeris a livré l'équivalent du plan (ADR-008, six décisions) :
**arbre d'accessibilité virtuel** (rangées visibles en nœuds, navigation
par caractère/mot/ligne/rangée, actions, annonces), **parité en/fr
contrôlée en CI** (99/99 clés mesurées dans la démo), **UAX #9 et #29 au
cœur et au rendu**, **contraste WCAG ≥ 4,5:1** vérifié au banc pour les
trois thèmes, **tailles en sp** et mesure de police injectée, et un
**auditeur automatique en CI** (`scripts/check-accessibilite`, cinq règles,
dix auto-tests) qui a déjà fait tomber deux défauts réels en M9.

### 3.4 IA embarquée

Sans équivalent chez l'ancien, qui n'a aucune capacité IA. `core-ai` de
celeris apporte : texte fantôme multiligne (acceptation par mot/ligne/bloc,
jeton d'annulation), prédiction d'édition **comme transaction annulable**
(jamais d'application silencieuse), revue d'agent par blocs avec points de
contrôle, API d'outils éditeur pour agents (lecture, symboles, diagnostics,
transaction avec détection de conflit), connecteur compatible **MCP**
(transport à l'hôte), et des garde-fous : consentement explicite
(`GardeContexte`), exclusions de contexte (un chemin exclu n'est jamais
transmis), aucun transport réseau dans le cœur.

### 3.5 Qualité outillée

L'ancien est supérieur en volume de tests (voir § 4.4), mais celeris est
supérieur en **profondeur** :

| Levier | dépôt de départ | celeris |
| --- | --- | --- |
| Couverture | Jacoco en **rapport**, aucun seuil | JaCoCo **≥ 90 % lignes bloquant** sur modules purs |
| Tests de mutation | aucun | PIT, seuil 80, mesuré **85,0 %** sur `core-text` |
| Nullicité | — | NullAway + JSpecify `@NullMarked` |
| Architecture | budgets de classes (cliquet lignes/membres) | ArchUnit (9 modules) + plafonds Checkstyle/PMD |
| Warnings | `-Xlint:all` sans `-Werror` | `-Xlint:all -Werror` + Error Prone |
| Propriétés | fuzz `@RepeatedTest` | jqwik (21 propriétés, oracles naïfs) |
| Chiffres documentés | README contrôlé ±5 % | 12 scripts de politique auto-testés (langue, références interdites, provenance, enregistrement des langages, accessibilité, matrice, pièges, démo, dates) |

### 3.6 Sécurité outillée

Les deux sont sérieux (zéro permission dans la démo, actions CI épinglées
par SHA, validation des wrappers, NOTICE). celeris ajoute des garanties que
l'ancien n'a pas : **SBOM CycloneDX** et **sommes SHA-256** écrites à côté de
chaque artefact publié, **provenance scriptée et bloquante**, APK release
**réduit par R8 et vérifié** (dex contrôlé, 5 scénarios rejoués sur le
binaire réduit, 6,3 Mo contre 10,3 Mo), `allowBackup="false"` **exigé par
test**, chargeur de paquets durci (zip-slip, liens symboliques, tailles,
profondeur, schéma), et une **revue de sécurité M9** de dix surfaces qui a
fermé une faille réelle rouge puis vert (un paquet ne peut plus faire monter
un serveur par réflexion — seul l'hôte décide).

### 3.7 Gouvernance d'ingénierie

17 ADR (l'ancien : 3), registre de portage ligne à ligne, pièges hérités
traqués, matrice de parité de 123 lignes **contrôlée par script** (chaque
statut cite un test qui existe), feuille de route jalonnée, audits
d'accessibilité et de sécurité consignés, glossaire et références. Chez
l'ancien, la gouvernance était réelle mais plus légère (plans, ADR,
audit 3.69.0, cliquets de budgets).

---

## 4. Là où l'ancien reste devant

### 4.1 Couverture de langages — 29 contre 16

L'ancien colore **29 langages** (14 tokéniseurs intégrés + 23 grammaires
TextMate-like embarquées) : java, kotlin, javascript, typescript, c, cpp, go,
rust, swift, dart, php, ruby, scala, groovy, csharp, python, lua, xml/html,
css, json, yaml, sql, shell, properties, toml, smali, log, markdown, text.

celeris en livre **16** en paquets de données : java, kotlin, kts, groovy,
xml, json (6 analyseurs écrits) + c, cpp, markdown, proguard, properties,
python, shell, sql, toml, yaml (10 grammaires TextMate réelles).

**Manquent donc** : JavaScript, TypeScript, CSS, Go, Rust, Swift, Dart, PHP,
Ruby, Scala, C#, Lua, smali, log, HTML (grammaire prouvée au banc du
commerce à 98,7 % mais pas encore livrée en paquet), text.

Le mécanisme de celeris est néanmoins supérieur : un langage entier est un
**paquet de six fichiers de données** (identité, descripteur, grammaire,
plan, gabarits, serveurs), ajoutable **à chaud** (démontré en M9 : le paquet
jouet Logo se copie au stockage et le registre se RECHARGE, H-04) — chez
l'ancien, « enregistrer un langage = une entrée de table » côté intégrés,
mais la voie grammaires exigeait des archives binaires versionnées. Les 11
grammaires VS Code du banc du commerce se chargent d'ailleurs **sans
modification** (json 100 %, python 98,8 %, html 98,7 %, cpp 71,5 % — garde
conservateur). Le gap est donc du **contenu, pas du moteur** : chaque
langage manquant est un paquet de données à assembler, pas du code à
écrire.

### 4.2 Richesse de la vue et du chrome

L'ancien a livré une interface remarquablement riche ; celeris en a porté le
kit de popups **tel quel** (M8.5) mais n'a pas (encore) les éléments
suivants — vérifiés absents du code et de la matrice celeris :

| Élément livré chez l'ancien | État celeris |
| --- | --- |
| Minimap | absent |
| Fil d'Ariane (breadcrumb) | « spécifier » au registre de portage ; pas dans le produit |
| Fast-scroller de gouttière + error stripe | absent |
| Édition par blocs (`BlockEditor`) | absent |
| Keymap rebindable + accords (Ctrl+K Ctrl+C) | raccourcis câblés (D-17), pas de remappage |
| Barre de touches de code | absent |
| Polices à ligatures (D-14) | non démarré (matrice) |
| Glisser-déposer de blocs (D-20) | non démarré (matrice) |
| Loupe native API 28+ (α=0,55/image) et loupe réglable ×1,5–×3 | loupe présente, mode natif non documenté |
| 4ᵉ tap paragraphe, cascade multi-tap | non documenté |
| Aperçu flottant « goutte d'eau » (B27, placement anti-recouvrement) | absent |
| Aperçu de correctif avant application (B-18) | non démarré (matrice) |

En sens inverse, celeris a déjà : gouttière multi-couloirs, sticky lines,
zoom, popups unifiées (kit porté, mêmes valeurs/rendus), pilule
multi-curseur, toolbar de sélection (M8.6), IME 26→36 par clusters
graphèmes, plugins de décoration isolés, thèmes résolus par **pile de
portées** avec import `tokenColors` VS Code — voie plus ouverte que le
schéma de 42 attributs de l'ancien.

### 4.3 Raffinements d'intelligence locale

L'ancien est allé bien plus loin dans le poli des fonctionnalités :

- **Complétion** : correspondance unifiée à 6 paliers (exact, casse,
  préfixe, camel humps, sous-séquence), **apprentissage du classement**
  (historique 200/langage, persistant `snapshot()/restore()`),
  **complétion contextuelle** (constantes d'enum après `.`, attributs
  d'annotation dans `@Anno(`, arguments nommés), documentation résolue au
  dwell 200 ms avec cache client, **plancher local fusionné** (brancher un
  fournisseur ne retire pas les mots-clés/snippets).
- **Signature** : trois paliers visuels (frappé accent / actif gras / à
  venir atténué), stepper ‹i/n› tactile, fenêtrage ±1, peek appui long,
  repli local sur réponse vide.
- **Snippets** : choix `${1\|a,b,c\|}`, macros `$FILE$/$DATE$/$USER$`,
  mirroring, ré-ancrage, expansion à l'acceptation de complétion
  (`insertTextFormat=Snippet`), plage `textEdit`, `filterText/sortText`.
- **Frappe assistée** : Smart Enter Java (case labels, découpage de
  chaînes, raw strings) et XML (expansion de paires), renommage lié des
  balises XML, **complétion postfixe** par table de profil (`.sout`),
  auto-import servi par le classpath, documentation par réflexion du
  runtime, indices Lambda.
- **Recherche** : occurrences du mot sous le caret plafonnées à 10 000,
  barre vivante « 3/17 ».

celeris a le socle (index de symboles, complétion par l'index, renommage
**multi-fichiers** B-19, formatage, squelettes de méthode, `.editorconfig`,
imports, correctifs rapides B-17 **prouvés contre un serveur réel**,
indices en ligne B-08) mais les raffinements ci-dessus sont absents, et la
matrice le dit : complétion postfixe **A-23 non démarré**, historique local
A-14 non démarré, extraire B-20, aligner B-21, changer la signature B-22,
déplacement sûr B-23 non démarrés. Verdict : **socle supérieur, surface
inférieure** — l'écart est du travail réel, pas du contenu.

### 4.4 Volume de tests

3 130 `@Test` chez l'ancien (3 185 exécutions, chiffre contrôlé en CI à
±5 %) contre 1 761 chez celeris (1 783 méthodes annotées avec les
propriétés ; le `check` complet revendique 1 819 tests verts, politique
comprise). Le ratio de lignes de test suit (73 307 contre 39 445). La
profondeur outillée (mutation, couverture bloquante) compense partiellement,
mais pour un produit qui revendique la parité avec Android Studio, le volume
restera un angle mort tant que la vue et les widgets n'auront pas rattrapé
les 1 066 tests d'interface de l'ancien (celeris en a ~460 sur
`android-view` + `android-widgets`).

### 4.5 Publication consommable et maturité

L'ancien est **livrable et consommé** : 68 tags, workflow de publication
déclenché par tag strict avec garde-fou version, `assembleRelease test`
avant publication (« un tag rouge ne publie jamais »), dépôt distant
actif, jar de sources, POM Apache-2.0, un hôte réel

celeris s'est arrêté à la publication **locale** : `publishToMavenLocal`
vert (8 artefacts `jo.celeris:*:1.0.0` — pas 9, `cli` n'est pas un module
publié — avec SBOM et SHA-256), dépôt distant **configuré mais jamais
exécuté**, **zéro tag**, aucun workflow de publication. Un hôte ne peut pas
consommer celeris 1.0.0 aujourd'hui hors mavenLocal. La feuille de route
assume : la publication distante « demande des décisions du propriétaire ».

### 4.6 Documentation produit

L'ancien est un produit documenté pour ses consommateurs : USAGE.md
(1 319 lignes, guide d'intégration complet), FEATURES.md (402 lignes),
BUILD.md, CONTRIBUTING.md, CHANGELOG de 103 versions. celeris a une
documentation d'ingénierie remarquable (ADR, RESUME, audits, matrice,
registre, MIGRATION.md final relu contre les manifestes d'API) mais
**aucun guide d'intégration** comparable — le README fait 76 lignes, le
CHANGELOG 2 versions. Pour un hôte qui migre, MIGRATION.md est excellent
mais bref face à USAGE.md.

### 4.7 Preuve de charge

L'ancien a des tests de charge à **480 000 lignes (~14 Mo)** en tas dédié de
1 Go, des limites documentées (`CHAR_LIMIT` 2,5 M caractères, `LINE_LIMIT`
50 000 lignes) et le mode gros fichiers tri-état. celeris tient des
budgets mesurés jusqu'à **100 000 lignes** (première image 154 ms pour 500
ms d'enveloppe, G-01) ; l'équivalent des tests de charge et des limites
dures de l'ancien n'existe pas encore.

---

## 5. Équivalences notables

Les domaines suivants sont **à parité réelle**, ce qui est une réussite vu
l'âge de celeris :

- **Client LSP** : 14 providers (ancien) contre 13 familles câblées
  (celeris) — complétion, diagnostics, survol, définition, références,
  renommage, correctifs, symboles, signature, plis, formatage, indices en
  ligne, surlignage. celeris y ajoute un **ordonnanceur** (fusion,
  annulation, priorités, resynchronisation) et prouve la frontière contre
  un **serveur réel embarqué** (serveur-retire 0.31.2), là où l'ancien se testait
  contre son mini-serveur de démo. L'ancien garde l'avantage **opératoire**
  (5 transports, multi-serveurs avec shutdown borné, debouncer, timeouts)
  que celeris délègue à LSP4J et à l'hôte.
- **Performance du cœur** : les deux sont excellents. L'ancien : < 1 ms par
  frappe sur 1 000 lignes, < 2,5 ms/op sur 1 M caractères (base corrigée),
  splice mesuré 0,11 ms sur 50 000 lignes, caches LRU 512/64, prefetch
  idle. celeris : cliquet resserré M9 — 0,5 ms par édition (mesuré
  **0,145 ms**, ×3,4 de marge), remplacement global 150 ms (mesurés
  39,8/37,0 ms), annulation/redo 100 ms (mesurés 0,85/1,06 ms),
  ré-analyse ≤ 10 ms/50 000 lignes, fantôme ≤ 16 ms. La discipline de
  mesure de celeris est supérieure (minimum trois tours PIEGE-13, horloge
  injectée ADR-007, calibrage consigné dans la Javadoc).
- **Rendu Canvas** : refonte celeris (peintres, plafond 600 lignes) contre
  pipeline à 16 couches de l'ancien — concepts équivalents, implémentation
  plus saine et testable (Robolectric, ADR-013) chez le nouveau.
- **Sécurité de base** : zéro permission, actions épinglées, NOTICE —
  les deux.

---

## 6. Les gaps restants du nouveau, par priorité

Classement proposé (P0 = à faire avant toute consommation par un hôte ;
P1 = parité de produit ; P2 = différenciation) :

| # | Gap | Réf. ancien | État celeris | Priorité |
| --- | --- | --- | --- | --- |
| 1 | Publication distante réelle (tag v1.0.0, workflow, dépôt distant) | workflow publish + 68 tags | configuré, non exécuté, 0 tag | **P0** |
| 2 | dependabot + SECURITY.md | Dependabot hebdo, SECURITY.md 38 l. | absents (vérifiés) | **P0** |
| 3 | Doc d'intégration (USAGE équivalent) | USAGE.md 1 319 l. | rien d'équivalent | **P0** |
| 4 | Langages manquants (JS, TS, CSS, Go, Rust, PHP, Ruby, C#, Lua, Swift, Dart, Scala, HTML…) | 29 intégrés | 16 ; voie paquets prouvée (H-04) | P1 |
| 5 | Chrome UI : minimap, fil d'Ariane, error stripe, fast-scroller, barre de touches | livrés | absents du produit | P1 |
| 6 | Raffinements de complétion (apprentissage, contexte enum/annotation, 6 paliers, plancher fusionné) | livrés | socle seul | P1 |
| 7 | Signature riche (3 paliers, stepper, peek) ; snippets riches (choix, macros, mirroring) | livrés | gabarits à trous simples | P1 |
| 8 | Smart typing Java/XML, postfixe, renommage XML lié, auto-import classpath | livrés | non démarrés (A-23…) | P1 |
| 9 | Tests de charge ≥ 480 000 lignes + limites documentées | ProgressiveLoadTest | 100 000 lignes max (G-01) | P1 |
| 10 | Volume de tests vers 3 000+ (interface surtout) | 1 066 tests UI | ~460 tests de vue | P2 |
| 11 | Ligatures (D-14), glisser-déposer (D-20), guides d'indentation (A-17) | ligatures et drag livrés | non démarrés | P2 |
| 12 | Refactorings (extraire B-20, aligner B-21, signature B-22, déplacement sûr B-23), historique local A-14, mots casse-chameau A-04 | partiellement (renommage XML, occurrences) | non démarrés | P2 |
| 13 | CLI complète (diagnostiquer, formater, refactorer — G-03) | inexistant chez l'ancien | `analyse`/`recherche` seuls ; `VERSION` encore `0.1.0-SNAPSHOT` | P2 |

Hors périmètre « ancien » mais à rappeler : la colonne « Vérifié appareil »
de la matrice celeris est **à zéro sur 123 lignes** — aucun des deux dépôts
n'a de test sur appareil réel ; celeris est celui qui l'assume et le
documente (`docs/qualite/VERIFICATION-APPAREIL.md`, second étage spécifié,
non exécuté faute d'émulateur).

---

## 7. Limites et risques

### 7.1 Côté celeris

- **Jeunesse radicale** : 7 jours, un contributeur, zéro appareil, zéro
  hôte consommateur réel. Le 1.0.0 est une plateforme, pas un produit fini.
- **Périmètre non livré consigné par sa propre matrice** : 47 lignes au
  noyau, 18 non démarrées (P1/P2), cpp au banc du commerce à 71,5 %
  (garde conservateur), jar du serveur tiers non vérifié par somme à la
  construction (risque faible, consigné).
- **Cinq incohérences documentaires internes vérifiées** pendant cette
  analyse — à réconcilier :
  1. RESUME-M9 annonce « neuf modules publiés » (dont `cli`) ; la
     configuration et le maven local en montrent **huit** (le `cli` n'a pas
     `maven-publish`) ;
  2. RESUME-M9 affirme « aucun “Non démarré” ni “Noyau” en P0 » ; la
     matrice compte **16 lignes P0 au statut Noyau** (fonctionnalités
     livrées au cœur, sans test de vue cité) ;
  3. RESUME-M9 cite une parité en/fr « 116/116 » ; la mesure directe des
     `strings.xml` donne **99/99** ;
  4. l'index `docs/adr/README.md` s'arrête à l'ADR-016 (017 manque) ;
  5. `cli/Celeris.java` affiche encore `VERSION = "0.1.0-SNAPSHOT"`.
- **Hygiène des dépendances en retrait** : ni dependabot ni SECURITY.md,
  alors que l'ancien a les deux.

### 7.2 Côté dépôt de départ

- **Gel de facto** : plus aucun commit depuis le 2026-10-30 ; le registre de
  portage de celeris le fige en lecture seule au commit `f310a39`. Tant
  qu'un statut n'est pas annoncé, un hôte peut légitimement hésiter à
  bâtir dessus.
- **Dettes assumées jamais closes** : multi-curseur et RTL planifiés non
  livrés, LSP4J figé en 0.22.0, accessibilité minimale (l'angle mort
  structurel pour tout hôte visant TalkBack), baselines lint gelées,
  `-Werror` différé, Jacoco jamais bloquant, incohérence de licence
  résiduelle (CONTRIBUTING dit MIT, le reste Apache-2.0), chiffres de
  FEATURES.md en léger retard sur l'arbre.
- **Architecture au plafond** : 13 fichiers > 1 000 lignes, moteur dans une
  bibliothèque Android — les plans de démantèlement existaient mais
  n'ont jamais été terminés (les « orchestrateurs regonflent
  légitimement », note leur propre ARCHITECTURE.md).

### 7.3 Limites communes

Aucun test sur appareil réel (IME par fabricant, gestes sur vitre, TalkBack
vivant, budgets de rendu mesurés sur vitre) dans aucun des deux dépôts ;
un seul contributeur ; pas de fuzzing continue ni de JMH ; la charge est
prouvée sur JVM, jamais sur matériel bas de gamme.

---

## 8. Verdict final

**Le nouveau est supérieur sur ce qui compte pour les cinq prochaines
années, inférieur sur ce qui se rattrape en quelques mois.**

- celeris gagne **structurellement** : modularité, frontière LSP à jour,
  multi-curseur, Unicode normatif (UAX #9/#29), accessibilité digne de ce
  nom, IA sans équivalent, qualité et sécurité outillées (mutation 85 %,
  couverture bloquante, SBOM, faille fermée), gouvernance d'ingénierie
  (17 ADR, audits, matrice contrôlée). Aucun de ces acquis ne se
  rattrape en patchant l'ancien — l'ancien en avait trois (ADR) et avait
  renoncé au reste (RTL, multi-curseur, LSP4J 1.0).
- Le dépôt de départ garde l'avantage **produit** : 29 langages, chrome riche,
  raffinements d'intelligence éprouvés, 3 130 tests, 68 versions publiées,
  un artefact consommable et consommé. Ce sont des mois de poli, pas de la
  structure.

**Donc : oui, le nouveau est supérieur — à condition de finir la sortie.**
Au moment où ce rapport est écrit, celeris 1.0.0 n'est consommable par
aucun hôte externe (pas de tag, pas de publication distante, pas de guide
d'intégration). Tant que les trois gaps P0 du § 6 ne sont pas fermés, un
hôte pressé doit encore choisir l'ancien ; un hôte qui regarde à trois ans
doit choisir le nouveau et contribuer à sa parité de produit. Le registre
de portage rend ce chemin peu risqué : l'ancien, spécifié en oracle, peut
servir de spécification pour chacun des raffinements manquants, comme il
l'a déjà été pour la rope, l'annulation et le kit de popups.

---

## 9. Recommandations (10 actions, par ordre)

1. **Publier réellement 1.0.0** : tag `v1.0.0`, exécution du workflow de
   publication vers le dépôt distant (déjà configuré), vérification de la somme
   d'un artefact consommé depuis un projet vierge. Un jour de travail.
2. **Rattraper l'hygiène de l'ancien** : dependabot (hebdo, gradle +
   actions), SECURITY.md (reprendre la procédure existante de l'ancien).
   Une heure de travail.
3. **Réconcilier les cinq incohérences documentaires** (§ 7.1) : 8 modules
   publiés, 16 P0 noyau, 99/99 clés, index ADR-017, `cli` VERSION.
   Une matinée.
4. **Écrire USAGE.md** (guide d'intégration hôte) — s'appuyer sur
   MIGRATION.md et sur l'USAGE.md de l'ancien comme plan.
5. **Langages par paquets de données** : priorité aux grammaires VS Code
   existantes (JS, TS, CSS, HTML, Go, Rust, PHP, Ruby…) — le moteur est
   prouvé au banc du commerce, c'est du contenu. Livrer HTML d'abord
   (98,7 % au banc).
6. **Porter les raffinements de complétion** de l'ancien (apprentissage du
   classement, contexte enum/annotation, 6 paliers) en les spécifiant
   comme au registre de portage — l'ancien sert d'oracle.
7. **Chrome à coût faible** : minimap, fil d'Ariane, error stripe,
   fast-scroller — le kit de popups porté tel quel prouve que la voie est
   praticable.
8. **Charge et limites** : test à 480 000 lignes à la manière de l'ancien
   (tas dédié), limites documentées équivalentes, mode gros fichiers.
9. **Poser le second étage appareil** de `VERIFICATION-APPAREIL.md`
   (pincement, gouttière, flicker, popup) dès qu'un émulateur est
   disponible — c'est le seul étage qui manque aux deux dépôts.
10. **Statuer sur le dépôt de départ** : archivage avec bannière de migration
    vers celeris (son gel au commit `f310a39` est déjà acté par le
    registre de portage) — pour que la communauté des deux dépôts ne
    sème pas un hôte sur une impasse.

---

## Annexe A — Chiffres par module

### Dépôt de départ (lignes physiques, mesuré le 2026-11-10 sur `f310a39`)

| Module | Fichiers prod. | Lignes prod. | `@Test` |
| --- | --- | --- | --- |
| `cel-core` | 88 | 21 175 | 1 441 |
| `cel-lsp-api` | 41 | 2 151 | 32 |
| `cel-lsp` | 45 | 8 283 | 238 |
| `cel-textmate` | 12 | 2 951 | 96 |
| `cel-ui` | 111 | 39 794 | 1 066 |
| `demo` | 26 | 13 311 | 257 |
| **Total** | **323** | **87 665** | **3 130** |

### celeris (Java, mesuré le 2026-11-10 sur `14a300f`)

| Module | Fichiers prod. | Lignes prod. | `@Test` | Publié |
| --- | --- | --- | --- | --- |
| `core-text` | 36 | 5 449 | 417 | oui |
| `core-syntax` | 69 | 6 619 | 210 | oui |
| `core-syntax-grammars` | 23 | 2 057 | 82 | oui |
| `core-language` | 26 (+45 JSON) | 3 349 | 110 | oui |
| `core-intelligence` | 32 | 3 298 | 166 | oui |
| `core-lsp` | 29 | 1 225 | 36 | oui |
| `lsp-protocole` | 11 | 2 887 | 62 | oui |
| `core-ai` | 22 | 1 939 | 60 | oui |
| `cli` | 2 | 236 | 11 | non |
| `android-view` | 102 | 11 748 | 366 | différé |
| `android-widgets` | 31 | 3 827 | 92 | différé |
| `android-host` | 36 | 1 716 | 54 | différé |
| `demo` | 46 | 7 913 | 95 | non |
| **Total** | **465** | **52 263** | **1 761** | **8 artefacts 1.0.0** |

(En plus : 21 méthodes de propriété jqwik, 1 test paramétré, 12 scripts de
politique auto-testés, tests de mutation PIT.)

## Annexe B — Méthode, sources et commandes

- **Dépôts** : le dépôt de départ au commit `f310a39` (dernier commit du
  2026-10-30, clone frais du 2026-11-10) ; celeris au commit `14a300f`
  (dernier commit du 2026-11-10, branche `main`).
- **Comptages** : `git rev-list --count HEAD`, `git ls-files '*.java' | xargs
  rg -c "@Test"`, `rg -c "<string name="`, comptages par module recoupés
  avec `docs/audit/ETAT-DES-LIEUX.md` de celeris (généré par
  `scripts/audit_etat_des_lieux.py`, qui mesure l'ancien indépendamment de
  cette analyse et confirme chacun des chiffres reprises ici).
- **Documents internes mobilisés** : côté celeris — `README.md`,
  `MIGRATION.md`, `CHANGELOG.md`, `docs/RESUME-M9.md`,
  `docs/parite/MATRICE-PARITE.md` (123 lignes, statuts comptés),
  `docs/audit/REGISTRE-PORTAGE.md`, `docs/audit/ETAT-DES-LIEUX.md`,
  ADR-001 à 017 ; côté dépôt de départ — `README.md`, `FEATURES.md` (402 l.),
  `USAGE.md`, `ARCHITECTURE.md`, `BUILD.md`, `SECURITY.md`, `NOTICE`,
  `docs/adr/` (3), `docs/plans/` (11), CHANGELOG + archive.
- **Vérifications d'absence** (§ 4.2) : recherche `rg -il` sur les fichiers
  suivis de celeris pour « minimap », « fil d'Ariane », « breadcrumb »,
  « error stripe », « fast-scroller », « keymap », « BlockEditor » —
  Concordances trouvées uniquement dans les documents d'audit de l'ancien
  dépôt ou nulles ; le produit n'en contient pas.
- **Limites de l'analyse** : statique — aucun des deux dépôts n'a été
  reconstruit ni rejoué pendant cette comparaison ; les chiffres de tests
  exécutés (3 185 chez l'ancien, 1 819 chez celeris, politique comprise)
  sont ceux que chaque dépôt revendique et contrôle en CI. Les chiffres de
  celeris ont par ailleurs été vérifiés verts lors du jalon M9
  (`./gradlew check` complet au 2026-11-10).
