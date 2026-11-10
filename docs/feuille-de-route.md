# Feuille de route — celeris

> **Document vivant** : ce fichier se met à jour à la clôture de chaque
> chantier — statut, date, preuve au journal des clôtures (décision du
> propriétaire du 2026-11-10). Les jalons M0 à M9 sont bouclés : la
> version 1.0.0 est publiée en local le 2026-11-10 (huit artefacts
> `jo.celeris:*`, SBOM et sommes de contrôle vérifiés) ; leur
> planification historique reste consultable en fin de fichier.
>
> Les chantiers descendent de l'audit comparatif avec le dépôt de
> départ ([`docs/audit/ANALYSE-COMPARATIVE.md`](audit/ANALYSE-COMPARATIVE.md),
> recommandations § 9, gaps § 6, incohérences § 7.1) et de la matrice de
> parité ([`docs/parite/MATRICE-PARITE.md`](parite/MATRICE-PARITE.md)) ;
> chaque chantier cite sa source. Les efforts sont des fourchettes en
> jours-homme (j-h) pour un développeur familier du dépôt — des
> estimations, pas des promesses.

## Statuts

| Statut | Signification |
| --- | --- |
| Non démarré | rien de livré |
| En cours | travail engagé, critère de sortie pas encore atteint |
| Terminé | critère de sortie atteint, preuve au journal des clôtures |
| Bloqué | attend un moyen extérieur : secret, appareil, décision |

## Directive transversale — tout en popups (ADR-018)

Décision du propriétaire du 2026-11-10 : toute surface fonctionnelle —
des chantiers ci-dessous, des chantiers futurs comme des
fonctionnalités existantes — se présente **exclusivement en popups** :
feuille du bas (bottom sheet), feuille du haut (top sheet), popup
ancrée au point d'intérêt. Jamais d'écran plein cadre, jamais de
dialogue système pour une fonctionnalité : l'hôte reste maître de son
écran. La décision, ses options et ses compromis vivent dans
[`docs/adr/018-interface-tout-en-popups.md`](adr/018-interface-tout-en-popups.md) ;
le chantier C12 inventorie les surfaces existantes et les ramène à la
conformité. La directive s'applique dès maintenant à tout nouveau
développement, quel que soit le statut de C12.

## Vue d'ensemble

| # | Chantier | Priorité | Effort | Statut |
| --- | --- | --- | --- | --- |
| C1 | Publication réelle de 1.0.0 : tag, workflow distant, artefact vérifié | P0 | ~1 j | Bloqué |
| C2 | Hygiène du dépôt : dependabot + politique de sécurité | P0 | ~1 h | Terminé |
| C3 | Cohérence documentaire : les incohérences de l'audit | P0 | ~0,5 j | Terminé |
| C4 | `USAGE.md` : guide d'intégration hôte | P0 | 2–3 j | Non démarré |
| C5 | Langages par paquets : la famille web et la longue traîne livrées, restent Go, Rust… | P1 | 1–2 j / langue | En cours |
| C6 | Raffinements de complétion | P1 | 3–5 j | Non démarré |
| C7 | Signature riche et snippets riches | P1 | 3–5 j | Non démarré |
| C8 | Frappe fine : smart typing, postfixe, renommage XML, auto-import | P1 | 3–5 j | Non démarré |
| C9 | Chrome de l'éditeur : minimap, fil d'Ariane, barre d'erreurs, défilement rapide | P1 | 1–2 j / ornement | Non démarré |
| C10 | Charge et limites : 480 000 lignes, mode gros fichiers | P1 | 2–3 j | Terminé |
| C11 | Second étage appareil de la vérification sur vitre | P1 | 1–2 j | Bloqué |
| C12 | Harmonisation tout en popups des surfaces existantes (ADR-018) | P1 | 2–4 j | Terminé |
| C13 | Deuxième vague : refactorings, ornement, volume de tests, CLI complète | P2 | 10–15 j | En cours |
| C14 | Statut du dépôt de départ : archivage et bannière de migration | P2 | ~1 h | Bloqué |

## Détail par chantier

### C1 — Publication réelle de 1.0.0

**Statut** : bloqué (secrets du propriétaire requis).

**Périmètre** : tag `v1.0.0` sur le commit de publication (aucun tag
n'existe encore) ; workflow de publication vers le dépôt distant — la
CI ne publie aujourd'hui qu'en local (`publishToMavenLocal`, SBOM et
sommes de contrôle, aucun travail distant) ; vérification de la somme
d'un artefact consommé depuis un projet vierge.

**Sortie** : le tag existe ; la publication distante a réussi en CI ;
un projet vierge consomme `jo.celeris:core-text:1.0.0` avec la somme
vérifiée qui correspond.

**Blocage** : exige les secrets du registre cible (Maven Central, dépôt
GitHub…) et l'arbitrage du propriétaire sur la cible — aucun secret de
publication ne sort du dépôt.

**Source** : audit § 9, recommandation 1 ; § 6, gap 1.

### C2 — Hygiène du dépôt

**Statut** : terminé le 2026-11-10 (dependabot, `SECURITY.md`, entrée `[1.1.0]` du changelog).

**Périmètre** : dependabot hebdomadaire — dépendances graduées du
catalogue `gradle/libs.versions.toml` et actions GitHub épinglées par
somme SHA ; `SECURITY.md` reprenant la procédure du dépôt de départ :
versions prises en charge, signalement privé par courriel (jamais
d'issue publique), délais annoncés, périmètre des artefacts publiés.

**Sortie** : `.github/dependabot.yml` versionné ; `SECURITY.md` à la
racine ; le changelog en rend compte.

**Source** : audit § 9, recommandation 2 ; § 6, gap 2.

### C3 — Cohérence documentaire

**Statut** : terminé le 2026-11-10 (erratum du résumé M9, bannière du cli en 1.0.0).

**Périmètre** : réconcilier les incohérences vérifiées par l'audit
(§ 7.1) —

1. RESUME-M9 annonce « neuf modules publiés » ; la configuration et le
   maven local en montrent huit (le `cli` n'a pas `maven-publish`) ;
2. RESUME-M9 affirme « aucun “Non démarré” ni “Noyau” en P0 » ; la
   matrice compte 16 lignes P0 au statut Noyau ;
3. RESUME-M9 cite une parité en/fr « 116/116 » ; la mesure directe des
   `strings.xml` donne 99/99 ;
4. l'index des ADR s'arrêtait à 016 — **rattrapé le 2026-11-10** avec
   l'ADR-018 ;
5. `cli/Celeris.java` affiche encore `VERSION = "0.1.0-SNAPSHOT"`.

**Sortie** : chaque affirmation contredite par la mesure directe est
corrigée ou porte un erratum daté ; la VERSION du `cli` est alignée ou
son statut SNAPSHOT est assumé par décision consignée.

**Source** : audit § 9, recommandation 3 ; § 7.1.

### C4 — USAGE.md : guide d'intégration hôte

**Statut** : non démarré.

**Périmètre** : le guide d'intégration qui manque à tout hôte —
déclaration des dépendances, montage de la vue, registre des langages,
popups et feuilles (ADR-018), SPI VCS/exécution/débogueur, contrats IA
et consentement, budgets de performance attendus. Plan : `MIGRATION.md`
et l'USAGE.md du dépôt de départ (1 319 lignes) comme squelette.

**Sortie** : `USAGE.md` à la racine ; les coordonnées d'artefacts et
les noms d'API du guide vérifiés contre les manifestes d'API (`api/`).

**Source** : audit § 9, recommandation 4 ; § 6, gap 3.

### C5 — Langages par paquets

**Statut** : en cours — la famille web du commerce (HTML, CSS,
JavaScript, TypeScript) puis la longue traîne (PHP, Ruby) livrées en
paquets intégrés le 2026-11-10.

Le propriétaire décide le 2026-11-10 : « on ajoutera d'autres langues
après » — la suite de la longue traîne (Go, Rust…) attend son signal,
le chantier dort jusqu'alors.

**Périmètre** : rapprocher les seize langages livrés des vingt-neuf du
dépôt de départ par la voie des paquets de données (ADR-017, prouvée
par le paquet jouet H-04) : grammaires du commerce d'abord — la
famille web (HTML 98,7 %, CSS 96,6 %, JavaScript 90,3 %, TypeScript
90,2 % d'acceptation au banc), puis la longue traîne — PHP (96,4 %)
et Ruby (98,9 %) livrés ; restent Go, Rust… (mêmes dépôt et
épingle que le banc du commerce).

**Sortie** : par langue — paquet épinglé, licence au NOTICE et au
registre de portage, échantillon rejoué avec déterminisme, ligne de
matrice à jour, banc de conformité étendu. HTML, CSS, JavaScript,
TypeScript puis PHP et Ruby (2026-11-10) : les paquets
`langages/html`, `langages/css`, `langages/javascript`,
`langages/typescript`, `langages/php` et `langages/ruby` reprendent
les grammaires du dépôt de VS Code SANS MODIFICATION (sommes
SHA-256 épinglées au NOTICE et au registre), les bancs de
conformité (`PaquetHtmlIntegreTest` à `PaquetRubyIntegreTest`)
verrouillent l'égalité octet pour octet, les acceptations et le
rejeu déterministe ; le chargeur tolère désormais le nom racine
absent — le format TextMate le rend facultatif, la grammaire PHP du
commerce s'en passe ; la matrice compte vingt-deux langages
intégrés. Restent : Go, Rust…

**Source** : audit § 9, recommandation 5 ; § 6, gap 4.

### C6 — Raffinements de complétion

**Statut** : non démarré.

**Périmètre** : l'apprentissage du classement, le contexte
enum/annotation, les six paliers et le plancher fusionné du dépôt de
départ — spécifiés au registre de portage, l'ancien servant d'oracle.

**Sortie** : par raffinement — spécification au registre, banc
comparatif contre l'oracle, tests de caractère.

**Source** : audit § 9, recommandation 6 ; § 6, gap 6.

### C7 — Signature riche et snippets riches

**Statut** : non démarré.

**Périmètre** : la signature sur trois paliers avec stepper et
peek ; les snippets à trous riches (choix, macros, mirroring) — les
gabarits livrés n'ont que des trous simples.

**Sortie** : les deux surfaces livrées en popups conformes à
l'ADR-018, avec tests de vue et lignes de matrice.

**Source** : audit § 6, gap 7.

### C8 — Frappe fine

**Statut** : non démarré.

**Périmètre** : smart typing Java/XML, complétion postfixe, renommage
XML lié, auto-import classpath — non démarrés (ligne A-23 en tête).

**Sortie** : par raffinement — spécification au registre de portage,
tests, ligne de matrice.

**Source** : audit § 6, gap 8.

### C9 — Chrome de l'éditeur

**Statut** : non démarré.

**Périmètre** : minimap, fil d'Ariane, barre d'erreurs (error stripe),
défilement rapide (fast-scroller) — le kit de popups porté à M5 prouve
que la voie est praticable ; toute surface interactive reste conforme à
l'ADR-018.

**Sortie** : par ornement — livré avec son test de vue, sa ligne de
matrice et son budget de rendu tenu (ADR-006).

**Source** : audit § 9, recommandation 7 ; § 6, gap 5.

### C10 — Charge et limites

**Statut** : terminé le 2026-11-10 (banc 480 000 lignes verrouillé, mode
gros fichiers livré, limites au README).

**Périmètre** : banc à 480 000 lignes à la manière du dépôt de départ
(tas dédié), limites documentées équivalentes, mode gros fichiers — la
charge n'est prouvée aujourd'hui qu'à 100 000 lignes (G-01).

**Sortie** : le banc est verrouillé — `Charge480kLignesTest` (core-syntax,
tas dédié de 1 Go : fenêtre d'abord 1441 ms / budget 2500 ms, incrément au
milieu 36 ms / budget 100 ms, tas de l'ouverture +90 Mo / budget 192 Mo,
calibrage consigné dans le banc) ; le mode gros fichiers tri-état
AUTO/ON/OFF vit au concentrateur d'intelligence (`LargeFileMode`,
`LargeFileModeTest`) — la complétion coupe au-delà des seuils du dépôt de
départ, l'édition reste entière ; les limites sont écrites au README
(section « Charge et limites » — le futur `USAGE.md` les reprendra, chantier
C4) ; G-01 étendu à la matrice.

**Source** : audit § 9, recommandation 8 ; § 6, gap 9.

### C11 — Second étage appareil

**Statut** : bloqué (aucun appareil ni émulateur dans le bac à sable).

**Périmètre** : exécuter le second étage de
[`docs/qualite/VERIFICATION-APPAREIL.md`](qualite/VERIFICATION-APPAREIL.md)
— pincement, gouttière, flicker, popup sur vitre. C'est le seul étage
qui manque aux deux dépôts : la colonne « Vérifié appareil » de la
matrice est à zéro sur 123 lignes.

**Sortie** : rapport d'exécution consigné au document de vérification.

**Blocage** : le propriétaire exécute sur son appareil de référence ou
fournit l'émulateur.

**Source** : audit § 9, recommandation 9.

### C12 — Harmonisation tout en popups

**Statut** : terminé le 2026-11-10 (feuilles du kit, douze surfaces
migrées, inventaire consigné).

**Périmètre** : inventorier les surfaces fonctionnelles existantes — la
démonstration en tête (écran de réglages, catalogue de scénarios), puis
les popups et barres livrées ; statuer surface par surface ; ramener
les non-conformes aux trois formes de l'ADR-018 ; livrer la feuille du
haut à son premier besoin si aucun chantier ne l'a encore fait naître.
Maquettes de référence : [`docs/maquettes/popups.html`](maquettes/popups.html).

**Sortie** : l'inventaire est consigné
([`docs/audit/INVENTAIRE-SURFACES.md`](audit/INVENTAIRE-SURFACES.md)) ;
les surfaces non conformes sont migrées — l'écran de réglages, le plan
latéral, la barre de recherche, six dialogues système et trois verdicts ;
aucune surface fonctionnelle plein cadre ne reste dans la démonstration.
La feuille du haut vit sa première vie (revues par blocs, consentement,
prédiction) et la feuille du bas sa première famille (réglages, plan,
recherche, renommage, verdicts du banc).

**Source** : ADR-018 ; décision du propriétaire du 2026-11-10.

### C13 — Deuxième vague : refactorings, ornement, tests, CLI

**Statut** : en cours — première vague
livrée le 2026-11-10 (A-04 mots à bosses de chameau, B-20 extraire en
variable, constante et champ).

Directrice du propriétaire du 2026-11-10 : la parité Android Studio
(lignes de la matrice) passe AVANT les gaps de l'analyse comparative
code-editor — les refactorings, l'historique local et les mots
casse-chameau précèdent les ligatures et le chrome ; les autres
langages (suite de C5) attendront, le propriétaire ajoutera les autres
langues après.

**Périmètre** : les gaps P2 de l'audit — refactorings (extraire B-20,
aligner B-21, signature B-22, déplacement sûr B-23), historique local
(A-14), mots casse-chameau (A-04) ; ligatures (D-14),
glisser-déposer (D-20), guides d'indentation (A-17) ; volume de tests
vers 3 000+ (l'écart mesuré : 1 761 contre 3 130) ; CLI complète
(diagnostiquer, formater, refactorer — G-03).

**Sortie** : par item — spécification au registre de portage, tests,
ligne de matrice ; les trois commandes de la CLI avec leurs tests de
fumée.

**Avancement (2026-11-10)** : A-04 clos — `CamelWord` (core-text)
raffine les frontières UAX #29 par les bossages de casse et de nature,
`CaretsMotion` (android-view) y branche le déplacement et la sélection
par mots, la matrice passe à Câblé (`CaretsMotionTest`) ; B-20 entamé
et câblé sur ses trois variantes déclaratives — variable, constante,
champ (`Extraction`, `ExtractionTest`, matrice à Câblé) — restent les
variantes méthode et paramètre (analyse des flux), puis B-21 à B-23,
A-14, A-17, D-14, D-20, G-03 et le volume de tests.

**Source** : audit § 6, gaps 10 à 13.

### C14 — Statut du dépôt de départ

**Statut** : bloqué (décision du propriétaire).

**Périmètre** : archiver le dépôt gelé (commit `f310a39`, gel déjà
acté par le registre de portage) avec bannière de migration vers
celeris — pour qu'aucun hôte ne sème sur une impasse.

**Sortie** : bannière visible sur le dépôt de départ ; le registre de
portage le consigne.

**Blocage** : décision du propriétaire ; la poussée se fait dans le
dépôt de départ, hors de ce bac à sable.

**Source** : audit § 9, recommandation 10 ; § 7.2.

## Journal des clôtures

| Date | Chantier | Preuve |
| --- | --- | --- |
| 2026-11-10 | C2 — hygiène du dépôt | dependabot (`.github/dependabot.yml`, commit e84ba50) et `SECURITY.md` (commit 8f20df7) ; entrée `[1.1.0]` du changelog |
| 2026-11-10 | C3 — cohérence documentaire | erratum du résumé M9 (commit 4f580e9) ; bannière du cli en 1.0.0 et test cliquet (commit 7acce1f) |
| 2026-11-10 | C12 — harmonisation tout en popups | feuilles du kit (`FeuilleBas`, `FeuilleHaut`, 11 tests de vue), douze surfaces migrées, écran de réglages disparu du manifeste ; inventaire consigné (`docs/audit/INVENTAIRE-SURFACES.md`) |
| 2026-11-10 | C10 — charge et limites | banc `Charge480kLignesTest` (480 000 lignes, tas dédié 1 Go), mode gros fichiers tri-état (`LargeFileMode` + `LargeFileModeTest`), limites au README (« Charge et limites ») ; G-01 étendu à la matrice |

## Historique — jalons M0 à M9 (bouclés, 1.0.0 publiée le 2026-11-10)

> Planification d'origine rédigée à M0, conservée telle quelle : les
> chiffres d'effort sont ceux estimés à l'époque, ils ne s'appliquent
> pas aux chantiers ci-dessus. Les comptes rendus vivent dans les
> `RESUME-M*.md`.

### Vue d'ensemble des jalons

| Jalon | Objet | Effort estimé | Cumul |
| --- | --- | --- | --- |
| M0 Cadrage | audit, parité, ADR, spikes | 5 j-h | 5 |
| M1 Fondations | dépôt neuf, build-logic, CI, portes qualité | 10–15 j-h | 15–20 |
| M2 Moteur de texte | instantanés, transactions, multi-sélection, undo, recherche, Unicode/BiDi | 25–35 j-h | 40–55 |
| M3 Vue minimale | rendu Canvas, curseurs, défilement, wrap, gouttière, thèmes, IME, a11y | 30–40 j-h | 70–95 |
| M4 Syntaxe | socle d'analyse + Java, Kotlin, XML, JSON, KTS + grammaires | 35–50 j-h | 105–145 |
| M5 Édition assistée | frappe intelligente, gabarits, recherche UI, fil d'Ariane, popups | 20–30 j-h | 125–175 |
| M6 Sémantique | client LSP, index, complétion, refactorings, formatage | 35–50 j-h | 160–225 |
| M7 Android et SPI | XML/manifeste/Gradle, SPI VCS/débogueur/exécution, cli | 25–35 j-h | 185–260 |
| M8 IA native | texte fantôme, revue d'agent, API d'outils, exclusions | 20–30 j-h | 205–290 |
| M9 Durcissement et 1.0 | audits, sécurité, perfs finales, migration, publication | 15–25 j-h | 220–315 |

Soit **10 à 15 mois-homme** ; environ **4 à 6 mois** à trois développeurs,
le chemin critique restant M2→M4.

### Détail par jalon

#### M1 — Fondations (10–15 j-h)

Dépôt neuf exécuté : `build-logic` (conventions publication/lint/javac),
catalogue unique, squelette des dix modules, workflows CI (actions épinglées
par SHA, wrapper validé), Checkstyle/PMD/Error Prone + NullAway, ArchUnit,
validateur d'API, jqwik/PIT/couverture branchés, scripts de politique de
langue et de références externes **bloquants**, squelette de la démo
(catalogue de scénarios vide, écran de réglages en direct), premiers
`package-info.java`.
Sortie : CI verte sur le squelette ; `./gradlew check` sans SDK Android sur
les modules purs ; budgets calibrés sur l'appareil de référence (ADR-006).

#### M2 — Moteur de texte, JVM pure (25–35 j-h)

Portage rope + index + undo coalescente (registre) ; instantanés immuables ;
`Transaction` unique (multi-sélection, projection des positions) ;
recherche/remplacement ; UAX #29/#9 ; encodages et BOM. PIEGE-07, PIEGE-08,
PIEGE-13 écrits **avant** le code. Fuzz jqwik à oracle `StringBuilder`.
Sortie : fuzz et oracle verts ; JMH ≤ 1 ms/édition sur 50 000 lignes ;
mutation ≥ 80 % ; pièges liés au texte couverts ; scénarios de démo A-13,
A-18, A-20, A-24, A-25.

#### M3 — Vue minimale (30–40 j-h)

Rendu Canvas sans allocation par image ; cache par rangée indexé sur valeurs
immuables ; curseurs et sélections multiples tactiles ; défilement fluide ;
retour à la ligne souple ; gouttière (numéros, diagnostics, plis) ; thèmes
clair/sombre/contraste élevé ; IME par niveau d'API (26→36) ; clavier
matériel ; arbre d'accessibilité virtuel ; ressources `en`/`fr` complètes ;
BiDi au rendu. PIEGE-01 à PIEGE-06, PIEGE-14, PIEGE-15 écrits avant.
Sortie : budgets de rendu tenus sur appareil réel ; tests instrumentés IME,
gestes, accessibilité verts ; scénarios A-01, A-18, D-01 à D-17, G-01 à G-05.

#### M4 — Syntaxe (35–50 j-h)

Socle d'analyse incrémental (jetons, états par ligne, arbre tolérant,
crochets, plis) ; analyseurs Java, Kotlin, XML, JSON, Gradle KTS ; voie de
grammaires pour la longue traîne ; coloration/plis/plan/indentation
consommateurs du même arbre ; ouverture progressive de très gros fichiers.
Test de cascade de commentaires borné. Sortie : ré-analyse ≤ 10 ms sur
50 000 lignes par langage ; corpus embarqués ; scénarios B-01, B-16, A-15,
C-01, C-07, C-14 à C-16, G-01.

#### M5 — Édition assistée (20–30 j-h)

Frappe intelligente (paires, indentation, retour arrière, entourer,
collage) ; gabarits dynamiques ; interface de recherche/remplacement ;
historique de navigation ; fil d'Ariane ; lignes collantes ; popups unifiées
(portage du kit) ; plugins isolés. Sortie : catégorie A P0 couverte +
D-08/D-09/D-10 ; scénario par ligne de matrice référencé.

#### M6 — Sémantique (35–50 j-h)

Client LSP propre (annulation, priorités, fusion, resynchronisation ;
transports stdio/réseau/in-process) ; indexeur hors ligne ; complétion,
aide aux paramètres, survol, définition, usages, renommage, correctifs,
formatage, indices en ligne, jetons sémantiques, diagnostics ; moteur de
refactorisation ; édits de gabarit (le blocage historique 1.0.0). PIEGE-16
à PIEGE-18 écrits avant. Sortie : catégorie B P0 couverte, critères testés
sur projets Android réels (corpus embarqué).

#### M7 — Android et SPI (25–35 j-h)

XML ressources/manifeste/Gradle/catalogues/R8 ; Compose (délégué) ;
SPI VCS/débogueur/exécution avec implémentations de référence ;
`editor-cli` sans Android exécuté en CI. Sortie : catégories C et E P0
couvertes.

#### M8 — IA native (20–30 j-h)

Contrats `core-ai` ; texte fantôme ; prédiction d'édition ; revue de
modifications d'agent par blocs avec points de contrôle ; API d'outils
compatible MCP avec détection de conflit ; exclusions de contexte ; faux
modèles déterministes. Sortie : catégorie F P0 couverte ; budgets de
latence tenus ; test « aucune requête sans consentement » vert.

#### M9 — Durcissement et 1.0 (15–25 j-h)

Audit d'accessibilité complet ; revue de sécurité ; performance finale
(cliquet resserré) ; documentation exhaustive ; `MIGRATION.md` final du
dépôt de départ vers celeris ; démonstration complète ; publication 1.0.0
sur la cible validée. Sortie : tous budgets et portes verts ; aucun P0
ouvert ; guide de migration relu.

### Risques identifiés (M0)

| Risque | Jalon | Parade |
| --- | --- | --- |
| Effort des analyseurs P0 sous-estimé | M4 | fourchette haute + option B chiffrée en réserve (ADR-004) |
| Dérive des budgets de rendu sur appareils variés | M3+ | calibration dès M1, cliquet CI, appareil de référence désigné |
| IME : fragment réelle entre fabricants | M3 | tests instrumentés par niveau d'API, corpus de composition hérité |
| LSP : serveurs réels divergents | M6 | traces enregistrées en corpus de tests de contrat |
| Surcoût des tests instrumentés en CI | tous | décision appareils gérés contre appareil local à M1 |
| Dépendance unique au développeur | tous | documentation continue, ADR, registres — la boîte noire est interdite |
