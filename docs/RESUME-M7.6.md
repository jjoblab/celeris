# Résumé M7.6 — plateforme de langages, LSP sur bibliothèque, règle des références (clos le 2026-11-10)

> **Déclenchement** : le prompt du propriétaire (analyse du 2026-11-08,
> cinq objectifs) — rétablir une règle des références qui ne bloque
> plus le projet, remplacer le support des langages codé en dur par
> une plateforme pilotée par des données, rendre le moteur de
> grammaires réellement compatible TextMate, factoriser l'analyse
> structurelle en trois niveaux, adopter LSP4J derrière une frontière.
> Le but : ajouter un langage en une journée de données et de tests,
> pas trois semaines de Java.
>
> **État** : les neuf étapes du jalon sont livrées, y compris les
> ajouts de la relecture du 2026-11-08. `./gradlew check` vert sur
> l'ensemble (politique comprise, 1 814 tests, 0 échec), APK de
> démonstration assemblé (debug et release réduite par R8),
> publication locale et sommes vertes. Poussée sur `main` à chaque
> étape (décision du propriétaire), branches de travail fusionnées
> puis supprimées.

## Les cinq objectifs, un par un

### 1. La règle des références externes (étape 1)

**Fait.** `scripts/check-external-refs` renaît en
`scripts/check-forbidden-refs` : seuls les motifs du secret de CI
`CELERIS_FORBIDDEN_REFS` décident, les cinq heuristiques du français
courant disparaissent ; `scripts/check-dependency-provenance` veille
sur le catalogue, les métadonnées de vérification et les URL de
dépôts ; les deux sont testés et tournent dans `check`. Les
désignations neutres s'effacent — tree-sitter et LSP4J se nomment enfin, ADR 004 et 005 reçoivent leur révision du 2026-11-08
sans réécrire l'histoire, la parité se formule contre Android Studio
en toutes lettres (matrice comprise), `docs/REFERENCES.md` consigne
projets et normes avec licences vérifiées. L'action du propriétaire
(mise à jour du secret de CI) a été exécutée par API le
2026-11-08T16:43:50Z.

**Commits** : `19f1248`, `2a5a162`, `beb04d6`. **Tests** :
`test-check-forbidden-refs`, `test-check-dependency-provenance`
(motif trouvé, absent, variable vide, motif invalide).

**Rouge puis vert** : un motif inconnu du secret faisait tomber les
deux scripts (rouge), les corrections les ont rendus verts ; le
fichier tout neuf `REFERENCES.md` échappait au contrôle avant son
premier commit (`git ls-files` ne le voyait pas) — corrigé dans
`2a5a162`, douze espaces sécables rattrapés.

### 2. La plateforme de langages (étape 4)

**Fait.** `LanguageDescriptor` (noms exacts, motifs, première ligne,
parent, badge), `LanguageConfiguration` (le sous-ensemble du
`language-configuration.json` de VS Code, `CommentRules` et
`TypingRules` devenus consommateurs), `LanguageSupport` (l'agrégat
que la vue, l'intelligence, la démo et le cli reçoivent), registre de
capacités avec famille et repli générique, détection à précédence
complète (réglage, nom exact, motif, extension composée, première
ligne, contenu). Les SEIZE langages vivent en paquets de données
(`langages/<id>/`) : six analyseurs écrits attachés par préférence,
dix grammaires ; les `switch` sur identifiants disparaissent
(`CommentRules`, `TypingRules`, `CarteFichier`, `FichiersExemple`,
`Celeris`), le contrôle d'architecture `check-language-registration`
le PROUVE (aucun littéral hors des paquets et de l'enregistrement).

**Commits** : `ca6622f`, `b7f5bf6`. **Tests** :
`DetectionPaquetsIntegresTest` (plus de trente cas par rang de
précédence), `PaquetsIntegresParametresTest` (UN test paramétré sur
tous les langages), `PackGrammarsTest`, `LanguageRegistryProprietesTest`.

**Écarts avec le prompt** : le prompt demandait vingt-cinq langages,
la matrice du propriétaire en exige seize (H-03) — seize livrés ; le
paquet jouet de l'utilisateur (H-04) est livré au noyau (chargeur de
dossiers et d'archives, sécurité zip-slip, liens symboliques, tailles,
profondeur — `PackLoaderTest`), pas encore jouable dans la
démonstration.

### 3. Le moteur de grammaires réellement TextMate (étapes 5 et 8 — H-05, H-07)

**Fait.** Le format réel des extensions de VS Code se charge : captures
en objets, inclusions `#dépôt`/`$self`/`$base`/`source.autre` résolues
en références parcourues (les cycles se traversent), fermetures à
références de l'ouverture (`\1`), expressions par joni (ADR-014). Les
injections TextMate s'appliquent désormais (H-05, `dfa8319`) : la clé
porte sélecteur et priorité (`L:` devant l'hôte, `R:` derrière), le
lexeur évalue les sélecteurs contre la pile de portées courante, la
règle injectée porte sa pile. Les ONZE grammaires du commerce des
extensions VS Code chargent SANS MODIFICATION (banc
`GrammairesCommerceTest`, licences MIT et provenance au registre de
portage et au NOTICE, ressources de test hors artefacts publiés),
avec seuil d'acceptation par grammaire : json 100 %, shell 99,4 %,
python 98,8 %, html 98,7 %, css 96,6 %, c 95,8 %, markdown 93,9 %,
javascript 90,3 %, typescript 90,2 %, yaml 87,5 %, cpp 71,5 %. La
forme catastrophique et l'inclusion de référentiel inconnue écartent
désormais SA règle — comptées au rapport, jamais le paquet entier
(l'étape 5 refusait le paquet : les grammaires du commerce portent des
formes que l'analyse textuelle refuse à tort, le critère du jalon
exige leur chargement). Le bornage d'exécution tient : budget d'essais
par ligne, au dépassement la fin de ligne garde la portée de son
contexte, l'incident se journalise (`GrammaireBornageTest`).

Les thèmes se résolvent par portées (H-07, `a04120e`) : la pile de
portées vit dans le jeton, internée dans un arbre de préfixes borné
(`ScopeStacks`) ; `Portees.de` disparaît, la sorte de jeton devient
catégorie dérivée ; `ScopeTheme` résout par sélecteurs TextMate
(descendance, exclusion, spécificité), fusionne les règles qui
matchent (l'encre de la meilleure, les attributs cumulés) et garde le
résultat en cache par pile — le tracé n'alloue rien par image. Les
thèmes clair, sombre et contraste élevé sont écrits ici (mêmes teintes
que les palettes, banc WCAG `ScopeThemeContrastTest`) ; l'importeur
`TokenColorsTheme` lit le format VS Code et la démonstration y bâtit
son quatrième thème « Atelier », sa palette dérivée du fichier seul.

**Commits** : `3cc5501`, `aae41bc`, `a04120e`, `dfa8319`. **Tests** :
`GrammairesConformiteTest` (les dix grammaires de paquets à 100 %
d'acceptation, déterminisme, aucun incident), `GrammairesTextMateTest`,
`InjectionsLexeurTest`, `GrammairesCommerceTest`, `ScopeStacksTest`,
`ScopeSelectorTest`, `ScopeThemeTest`, `PilesPorteesTest`,
`TextPainterScopesTest`, `ScopeThemeContrastTest`,
`TokenColorsThemeTest`, `ImportedThemesTest`.

**Rouge puis vert** : la forme catastrophique du commerce (`(?x)`
étendu de python, attributs HTML) refusait le paquet entier (rouge au
banc du commerce) ; l'écart de SA règle, comptée, l'a rendu vert. Un
candidat d'injection à gauche était écrasé par la fermeture du bloc
ouvert (rouge : l'injection ne s'appliquait jamais dans un bloc) —
la fusion par `garder` l'a rendu vert. La ligne de base de langue a
baissé : 408 identifiants français figés à M7.5, 344 à la clôture
(les renommages mécaniques de `core-syntax`, `core-lsp`,
`core-language` — exigés par le prompt — sont faits, ces modules
comptent zéro).

### 4. L'analyse structurelle en trois niveaux (étape 6)

**Fait.** Niveau 1 — le plan par données (`outline.json` des paquets :
sélecteur → catégorie → groupe de capture) et les plis par
indentation de la configuration (`offSide`) : Python, YAML et SQL ont
plis et plan sans une ligne de Java dédiée (`DataStructureTest`).
Niveau 2 — écarté par l'ADR-016 (voir les spikes), la case reste
déclarée dans les paquets, révisable. Niveau 3 — `CLikeLexer` et
`CLikeParser` paramétrables : Java, Kotlin, KTS et Groovy en
spécialisations d'au plus 150 lignes, instantanés identiques octet
pour octet (`ClikeSnapshotsTest`), budgets de l'ADR-004 tenus.

**Commits** : `d66970e`, `e0c10ee`. **Tests** : `DataStructureTest`,
`ClikeSnapshotsTest`, `TreeSitterOptionTest`, les bancs de budget
(`GrammairesBudgetTest`, `ReanalyseBudgetTest`).

### 5. LSP sur LSP4J derrière la frontière (étapes 7 et 8 — H-11, H-12)

**Fait.** Le module `lsp-protocole` porte le protocole : `LspSession`
sur des flux (tubes à file en mémoire pour la démo, jamais les tubes
du JDK dont la lecture couplait à la vivacité du fil écrivain),
correspondance vers les types immuables de `core-lsp`, montage
embarqué par désignation (`ServeurEmbarque.monter`), règles R8
consommatrices. La parité est prononcée : les bancs de la démonstration passent SANS MODIFICATION par la frontière ; le code maison (40 des 47 classes) est supprimé le même jour, le corpus de traces lit par le Gson de la bibliothèque ; la démo réduite par R8 est prouvée (assembleRelease vert, le dex vérifié porte la bibliothèque et sa copie unique de Gson).
La couverture nouvelle (H-12) raccorde TREIZE méthodes du protocole,
chacune à une interface : `references`, `documentSymbol`,
`documentHighlight`, `prepareRename`, `rename`, `codeAction`,
`formatting`, `rangeFormatting`, `foldingRange`, `selectionRange`,
`semanticTokens` (entier et plage) et `inlayHint` — servies par le monteur embarqué (`ServeurDocumentsCouvertureTest`), la vue ne voit que le domaine.

Les serveurs se déclarent en données (H-13, `cde46de`) :
`LanguageServerDeclaration` — transport (`embedded`, `stdio`, `tcp`),
classe du serveur, interface cliente, nom annoncé, options
d'initialisation — de la donnée pure lue du `servers.json` du paquet
ou du fichier de l'hôte qui assemble. Les seize paquets intégrés
n'en déclarent AUCUN : l'éditeur ne connaît pas les serveurs de ses
hôtes (règle du propriétaire), il porte la déclaration jusqu'à eux.
La démonstration porte ses déclarations (`serveurs/servers.json`), l'aiguillage `instanceof` de `EditorActivity` disparaît, la session générique
`DeclaredServerSession` ne demande que ce que la négociation annonce.

**Commits** : `c380e91`, `5f99a75`, `7c52f02`, `0d5fb87`, `21cb6c9`,
`236042c`, `0068cad`, `cde46de`. **Tests** : `LspSessionTest`,
`TubesMemoireTest`, `CorpusTracesTest`, `ServeurEmbarqueTest`,
`ServeurDocumentsCouvertureTest`, `LanguageServerDeclarationTest`,
`DeclaredServersTest`.

## Les trois spikes et leurs décisions

| Spike | Chiffres mesurés | Décision |
| --- | --- | --- |
| Moteur d'expressions (ADR-014) | onze grammaires réelles, 3 700 motifs : la traduction vers `java.util.regex` compile 91 % mais RÉÉCRIT 122 quantificateurs possessifs en silence et rejette 234 motifs sur le seul mode étendu `(?x)` ; joni 2.2.9 en compile 99 % avec la sémantique Oniguruma, à vitesse comparable (0,46× à 2,56×) ; Android : 615 Ko de dex, 546 méthodes, zéro `sun.misc.Unsafe` | **joni** (`ef47310`) |
| Client LSP (ADR-015) | un serveur de langage lancé par le `LSPLauncher` de la 1.0.0 : poignée de main, diagnostics poussés, complétion documentée, survol — tout passe, identiquement sous 0.24.0 et 0.23.1 ; l'arbre des dépendances tient en Gson seul (la 2.10.1 convient : copie unique) ; Android : 728 Ko de dex, 1 636 méthodes ; poignée de main 133-163 ms à froid | **LSP4J 1.0.0** (acceptée par le propriétaire) |
| tree-sitter niveau 2 (ADR-016) | liaison JNI minimale écrite ici (la liaison répandue de l'organisation interdite exclue, règle 7) ; incrémental après un caractère : 9,6 à 44,5 ms de p50 (budget ADR-004 : 10 ms), plancher croissant ≈ 6-10 ms/Mo ; arbres 26 à 31 fois le texte (budget : 3 fois) ; tailles hors cause (runtime 223 Ko) | **écarté à M7.6** — la case reste déclarée, révisable par ADR avec mesures sur appareil (`ae307dd`) |

## L'état sur appareil

**Non exécuté** — l'environnement de travail n'offre ni émulateur ni
appareil. La preuve disponible : l'APK de démonstration s'assemble
(debug et release réduite par R8, `assembleRelease` vert), les bancs JVM exécutent les serveurs de bout en bout, la réduction est vérifiée au dex. La vérification sur appareil reste au plan du
propriétaire (`docs/qualite/VERIFICATION-APPAREIL.md`).

## Écarts avec les constats du prompt

- **Une seule copie de lsp4j et Gson** : le chemin de classes fournit les deux bouts, la version de Gson épinglée au catalogue (étape 7).
- **L'APK de démo ne contient qu'une copie** : prouvé au dex.
- **`[EXT-1]`** : la matrice voyait l'IDE de référence, le RESUME-M8.5
  le dépôt de départ — la règle nouvelle tranche : la parité se
  formule contre Android Studio en toutes lettres, le portage du kit
  de popups du dépôt de départ du propriétaire reste permis et consigné
  au registre.
- **Dette de langue** : 408 → 344 identifiants figés (les modules
  refondus — `core-syntax`, `core-lsp`, `core-language` — comptent
  zéro).

## La matrice avant et après

| | Avant M7.6 | Après M7.6 |
| --- | --- | --- |
| Lignes | 110 | 123 |
| Noyau | 45 | 48 |
| Câblé | 36 | 45 |
| Non démarré | 17 | 18 |
| Démontré | 12 | 12 |
| Vérifié appareil | 0 | 0 |

La catégorie H (treize lignes) naît : H-01 à H-03, H-05, H-07, H-08,
H-10, H-12, H-13 câblées ; H-04, H-06, H-11 au noyau ; H-09 écartée
(ADR-016). `scripts/check-parity-matrix` et `scripts/check-demo-coverage`
couvrent la catégorie H sans exception.

## Ce qui n'est pas fait, et les risques restants

- **H-04 au noyau** : le paquet jouet de l'utilisateur ne se charge
  pas encore à chaud DEPUIS LA DÉMONSTRATION (le chargeur de dossiers
  et d'archives vit avec sa sécurité, testée) — le scénario de démo
  attend un écran hôte.
- **H-06 au noyau** : les langages incorporés (script dans HTML, code
  dans Markdown) — le mécanisme existe (inclusions externes par
  registre, injections H-05), aucun paquet livré ne le joue encore.
- **H-09 écartée** (ADR-016) : les budgets mesurés ne tiennent pas ;
  révisable avec mesures sur appareil.
- **H-11 au noyau** : la vérification sur appareil (API 26) du client
  reste au plan du propriétaire.
- **L'acceptation du commerce** : cpp à 71,5 % (le garde catastrophique
  reste délibérément conservateur — les règles écartées se comptent,
  elles ne colorent rien) ; monter le taux exige d'affiner l'analyse
  de délimitation, workitem séparé.
- **Risque** : la table d'internement des piles de portées est bornée
  (50 000 nœuds, profondeur 64) — au-delà, les piles nouvelles
  partagent leur dessous (perte de précision de coloration, jamais de
  mémoire).

## Clôture

Critères du prompt relus ligne à ligne : la règle des références
remplacée ; ajouter un langage ne demande que des données (les seize
paquets le prouvent, le contrôle d'architecture avec) ; dix grammaires
du commerce chargent sans modification (ONZE, banc à seuils) ; les
thèmes s'appliquent par portées et un `tokenColors` s'importe ; Java,
Kotlin, KTS et Groovy tiennent en spécialisations bornées à 150
lignes ; le spike tree-sitter a son ADR chiffré (écarté) ; le client
LSP passe par la bibliothèque derrière la frontière, la démo réduite
par R8 passe son auto-vérification, treize méthodes nouvelles sont
raccordées, aucune connexion sortante sans consentement ; la catégorie
H existe, les deux scripts de contrôle passent ; `check`, la
publication locale et l'APK verts ; aucun secret ni nom interdit dans
le dépôt (contrôlé par `check-forbidden-refs` et
`check-dependency-provenance` en CI). Les étapes sont poussées sur
`main` une à une, `git status` propre.
