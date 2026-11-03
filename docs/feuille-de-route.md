# Feuille de route — jalons M1 à M9

> Chiffres d'effort : estimations en jours-homme (j-h) pour un développeur
> senior familiarisé avec Android et les parseurs ; fourchettes, pas des
> promesses. Le chemin critique passe par M2 → M3 → M4 : les trois jalons
> partagent l'index, l'invalidation et la géométrie.

## Vue d'ensemble

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

## Détail par jalon

### M1 — Fondations (10–15 j-h)

Dépôt neuf exécuté : `build-logic` (conventions publication/lint/javac),
catalogue unique, squelette des dix modules, workflows CI (actions épinglées
par SHA, wrapper validé), Checkstyle/PMD/Error Prone + NullAway, ArchUnit,
validateur d'API, jqwik/PIT/couverture branchés, scripts de politique de
langue et de références externes **bloquants**, squelette de la démo
(catalogue de scénarios vide, écran de réglages en direct), premiers
`package-info.java`.
Sortie : CI verte sur le squelette ; `./gradlew check` sans SDK Android sur
les modules purs ; budgets calibrés sur l'appareil de référence (ADR-006).

### M2 — Moteur de texte, JVM pure (25–35 j-h)

Portage rope + index + undo coalescente (registre) ; instantanés immuables ;
`Transaction` unique (multi-sélection, projection des positions) ;
recherche/remplacement ; UAX #29/#9 ; encodages et BOM. PIEGE-07, PIEGE-08,
PIEGE-13 écrits **avant** le code. Fuzz jqwik à oracle `StringBuilder`.
Sortie : fuzz et oracle verts ; JMH ≤ 1 ms/édition sur 50 000 lignes ;
mutation ≥ 80 % ; pièges liés au texte couverts ; scénarios de démo A-13,
A-18, A-20, A-24, A-25.

### M3 — Vue minimale (30–40 j-h)

Rendu Canvas sans allocation par image ; cache par rangée indexé sur valeurs
immuables ; curseurs et sélections multiples tactiles ; défilement fluide ;
retour à la ligne souple ; gouttière (numéros, diagnostics, plis) ; thèmes
clair/sombre/contraste élevé ; IME par niveau d'API (26→36) ; clavier
matériel ; arbre d'accessibilité virtuel ; ressources `en`/`fr` complètes ;
BiDi au rendu. PIEGE-01 à PIEGE-06, PIEGE-14, PIEGE-15 écrits avant.
Sortie : budgets de rendu tenus sur appareil réel ; tests instrumentés IME,
gestes, accessibilité verts ; scénarios A-01, A-18, D-01 à D-17, G-01 à G-05.

### M4 — Syntaxe (35–50 j-h)

Socle d'analyse incrémental (jetons, états par ligne, arbre tolérant,
crochets, plis) ; analyseurs Java, Kotlin, XML, JSON, Gradle KTS ; voie de
grammaires pour la longue traîne ; coloration/plis/plan/indentation
consommateurs du même arbre ; ouverture progressive de très gros fichiers.
Test de cascade de commentaires borné. Sortie : ré-analyse ≤ 10 ms sur
50 000 lignes par langage ; corpus embarqués ; scénarios B-01, B-16, A-15,
C-01, C-07, C-14 à C-16, G-01.

### M5 — Édition assistée (20–30 j-h)

Frappe intelligente (paires, indentation, retour arrière, entourer,
collage) ; gabarits dynamiques ; interface de recherche/remplacement ;
historique de navigation ; fil d'Ariane ; lignes collantes ; popups unifiées
(portage du kit) ; plugins isolés. Sortie : catégorie A P0 couverte +
D-08/D-09/D-10 ; scénario par ligne de matrice référencé.

### M6 — Sémantique (35–50 j-h)

Client LSP propre (annulation, priorités, fusion, resynchronisation ;
transports stdio/réseau/in-process) ; indexeur hors ligne ; complétion,
aide aux paramètres, survol, définition, usages, renommage, correctifs,
formatage, indices en ligne, jetons sémantiques, diagnostics ; moteur de
refactorisation ; édits de gabarit (le blocage historique 1.0.0). PIEGE-16
à PIEGE-18 écrits avant. Sortie : catégorie B P0 couverte, critères testés
sur projets Android réels (corpus embarqué).

### M7 — Android et SPI (25–35 j-h)

XML ressources/manifeste/Gradle/catalogues/R8 ; Compose (délegué) ;
SPI VCS/débogueur/exécution avec implémentations de référence ;
`editor-cli` sans Android exécuté en CI. Sortie : catégories C et E P0
couvertes.

### M8 — IA native (20–30 j-h)

Contrats `core-ai` ; texte fantôme ; prédiction d'édition ; revue de
modifications d'agent par blocs avec points de contrôle ; API d'outils
compatible MCP avec détection de conflit ; exclusions de contexte ; faux
modèles déterministes. Sortie : catégorie F P0 couverte ; budgets de
latence tenus ; test « aucune requête sans consentement » vert.

### M9 — Durcissement et 1.0 (15–25 j-h)

Audit d'accessibilité complet ; revue de sécurité ; performance finale
(cliquet resserré) ; documentation exhaustive ; `MIGRATION.md` final du
dépôt de départ vers celeris ; démonstration complète ; publication 1.0.0
sur la cible validée. Sortie : tous budgets et portes verts ; aucun P0
ouvert ; guide de migration relu.

## Risques identifiés (M0)

| Risque | Jalon | Parade |
| --- | --- | --- |
| Effort des analyseurs P0 sous-estimé | M4 | fourchette haute + option B chiffrée en réserve (ADR-004) |
| Dérive des budgets de rendu sur appareils variés | M3+ | calibration dès M1, cliquet CI, appareil de référence désigné |
| IME : fragment réelle entre fabricants | M3 | tests instrumentés par niveau d'API, corpus de composition hérité |
| LSP : serveurs réels divergents | M6 | traces enregistrées en corpus de tests de contrat |
| Surcoût des tests instrumentés en CI | tous | décision appareils gérés contre appareil local à M1 |
| Dépendance unique au développeur | tous | documentation continue, ADR, registres — la boîte noire est interdite |
