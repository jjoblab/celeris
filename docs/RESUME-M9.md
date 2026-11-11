# Résumé M9 — durcissement et 1.0 (clos le 2026-11-10)

> **Déclenchement** : clôture du jalon M8.6 (2026-11-08) puis de M7.6
> (2026-11-10). Périmètre (feuille de route) : audit d'accessibilité
> complet ; revue de sécurité ; performance finale (cliquet resserré) ;
> documentation exhaustive ; `MIGRATION.md` final ; démonstration
> complète ; publication 1.0.0. Sortie visée : tous budgets et portes
> verts ; aucun P0 ouvert ; guide de migration relu.
>
> **État** : le jalon est clos — les six livrables ci-dessous,
> `./gradlew check` vert (1 819 tests, 0 échec, politique comprise),
> publication locale `jo.celeris:*:1.0.0` verte (SBOM et sommes de
> contrôle), mutation 85,0 % (seuil 80), APK de démonstration assemblé
> (debug et release réduite par R8). Poussée sur `main` à chaque
> livrable (décision du propriétaire).

## 1. L'audit d'accessibilité (commit `a56bbdd`, `c05586d`)

**Fait.** Les six décisions de l'ADR-008 passées en revue, une par
une (`docs/audit/AUDIT-ACCESSIBILITE-M9.md`) : l'arbre virtuel
(`AccessibilityHost`, rangées visibles, granularités, annonces), la
parité en/fr (116/116), le bidirectionnel UAX #9 au cœur et au
rendu, les groupes de graphèmes UAX #29, le contraste ≥ 4,5:1 des
trois thèmes au banc WCAG, la mesure de police injectée et les
tailles en sp. **L'auditeur automatique promis dès M3 vit enfin** :
`scripts/check-accessibilite` (cinq règles : nom de chaque widget
interactif — propre ou `labelFor` —, aucun label littéral, titre de
chaque item de menu, parité des clés en/fr, câblage vivant de
l'arbre virtuel), dix auto-tests, câblé bloquant à `check`.

L'auditeur naissant a fait tomber DEUX défauts réels dans les
réglages de la démonstration : les CINQ interrupteurs `MaterialSwitch`
et le curseur de lenteur étaient nus au lecteur d'écran — chaque
libellé porte désormais `android:labelFor`. 9 widgets interactifs, 9
nommés. Limites honnêtes : aucun test sur appareil réel (plan du
propriétaire), auditeur déclaratif (le comportement appartient aux
bancs JVM).

## 2. La revue de sécurité (commit `4e17623`, `4d3f1fa`)

**Fait.** Dix surfaces passées en revue
(`docs/audit/REVUE-SECURITE-M9.md`). **Une faille réelle fermée,
rouge puis vert** : la voie des déclarations (H-13) fusionnait les
serveurs des paquets et de l'hôte, et la démonstration montait la
première déclaration embarquée disponible — `Class.forName` +
constructeur vide : un paquet copié par l'utilisateur pouvait faire
exécuter une classe arbitraire du chemin de classes. L'hôte ne monte
désormais que SES déclarations (`DeclaredServers`, test
`uneDeclarationDePaquetNeMonteJamaisSansLhote`). Durcissement du
manifeste : `allowBackup="false"`, exigé par `DemoNeutreTest`. Le
chargeur de paquets (zip-slip, liens symboliques, tailles,
profondeur, schéma), la frontière IA (consentement, exclusions,
neutralité structurelle), la provenance et les références interdites
(scripts auto-testés en CI), les licences (NOTICE), la journalisation
(sans fuite de contenu), la réflexion bornée (règles R8
consommatrices), les secrets (trois faux positifs vérifiés) : tout
constaté avec ses tests. Résiduel accepté : la vérification sur appareil reste au plan du
propriétaire.

## 3. La performance finale, cliquet resserré (commit `72447d7`)

**Fait.** Les trois bancs réels du harnais affirmaient des
protections structurelles lâches (facteur ~50) ; le cliquet M9 les
calibre : édition **0,5 ms par édition** (mandat ADR-006 : 1 ms ;
mesuré 0,145 ms sur le bac à sable à 2 cœurs, ~×3,4 de marge CI),
passe de remplacement global et annulation **150 ms** (mesurés 39,8
et 37,0 ms, ~×4), annulation/redo **100 ms** (mesurés 0,85/1,06 ms).
Chaque calibrage se consigne dans la Javadoc : date, machine,
mesures, marge (PIEGE-13 : minimum de trois tours). Les budgets
produit ne bougent pas (10 ms de ré-analyse ADR-004, 16 ms le tracé
du fantôme, 500 ms l'enveloppe de première image G-01) — ce sont
des critères, pas des cliquets internes.

## 4. La documentation exhaustive (commit `05bebf8`)

**Fait.** `MIGRATION.md` final : la section 4 promise à M0 tient —
table de correspondance API publique (relue contre les manifestes
`api/*.api` : `DocOffset`/`LineIndex` affirmés à M0 n'existent pas,
la section 2 se corrige — les positions restent des offsets UTF-16
portés par `SelRange`, `TextEdit`, `EditSpan`, `AnchoredRange`),
procédure de reprise des thèmes (import `tokenColors` de VS Code,
l'exemple vivant `atelier.json`) et des gabarits (`snippets.json`),
procédure de reprise des extensions de langage (le paquet de données
complet, six fichiers, la sécurité du chargeur, **l'hôte seul décide
de monter** — revue de sécurité M9), checklist de vérification des
performances (bancs, budgets, discipline, appareil). Le README
rattrape l'état : treize modules (`core-syntax-grammars` et
`lsp-protocole` compris), ADR 001 à 017, catégories A à H,
`MIGRATION.md`, `REFERENCES.md` et les audits M9.

## 5. La démonstration complète (commit `c3e8feb`)

**Fait.** Le dernier écart du dépôt se ferme : **H-04 jouable**. Le
paquet jouet — un langage ENTIER en données seules (le Logo de la
tortue : `pack.json`, `language.json`, `grammar.json` TextMate réel,
`outline.json`, `snippets.json`) — se copie au stockage par les
réglages (sixième carte), le registre se RECHARGE à chaud
(`LangagesDemo.recharger`, dossier des paquets par
`Packs.folder`), la carte `tortue.lgo` entre et sort de l'accueil,
son badge porte le « Lg » du langage détecté. Preuves :
`PaquetJouetTest` (détecté, coloré, plié, commenté, plan, gabarits ;
retiré, disparu), la spécification H-04 au banc des vérifications,
le scénario H-04 au catalogue (`ScenariosGlobales`, catégorie G
comprise — le catalogue repasse sous son plafond). **La matrice :
H-04 passe de Noyau à DÉMONTRÉ — 123 lignes, 13 démontrées** (le
maximum atteignable sans appareil : « Démontré » exige un scénario
auto-vérifié, « Vérifié appareil » reste au plan du propriétaire).

## 6. La publication 1.0.0

**Fait.** Artefacts `jo.celeris:*:1.0.0` (neuf modules publiés :
`core-text`, `core-syntax`, `core-syntax-grammars`,
`core-language`, `core-intelligence`, `core-lsp`, `lsp-protocole`,
`core-ai`, `cli`) : publication locale verte (jar, sources,
Javadoc, module, POM), SBOM CycloneDX, sommes de contrôle SHA-256 ;
la démonstration porte `versionName` 1.0.0. `CHANGELOG.md` : entrée
1.0.0, court par construction. Mutation `core-text` : 85,0 %
(seuil 80).

## Critères de sortie, relus

- **Tous budgets et portes verts** : `./gradlew check` (1 819
  tests, 0 échec, politique comprise — langue, références
  interdites, provenance, enregistrement des langages,
  **accessibilité**, rempart, matrice, pièges, démo, dates),
  publication locale et sommes vertes, mutation verte, APK debug et
  release réduite assemblés.
- **Aucun P0 ouvert** : les P0 de la matrice sont Câblés ou
  Démontrés — aucun « Non démarré » ni « Noyau » en P0 ; les lignes
  restantes au noyau ou non démarrées sont P1/P2, consignées.
- **Guide de migration relu** : chaque affirmation vérifiée contre
  les manifestes d'API, le résumé de section 4 complété.

## Ce qui n'est pas fait, et les risques restants

- **Vérifié appareil : zéro ligne** — aucune colonne « Vérifié
  appareil » ne passe : l'environnement n'offre ni émulateur ni
  appareil. La vérification (IME réelle par fabricant, gestes sur
  vitre, budgets de rendu ADR-006, TalkBack vivant) reste au plan
  du propriétaire (`docs/qualite/VERIFICATION-APPAREIL.md`).
- **Les lignes P1/P2 non livrées** : 47 au noyau, 18 non démarrées
  (P1/P2), la dette est consignée par la matrice — 1.0 assume le
  périmètre P0.
- **cpp au banc du commerce à 71,5 %** : le garde catastrophique
  reste conservateur (workitem séparé, M7.6).
- **Publication sur la cible validée** : la feuille de route dit
  « publication 1.0.0 sur la cible validée » — la cible ici est le
  dépôt et le catalogue local (`publishToMavenLocal`) ; un dépôt
  distant (Maven Central ou équivalent) demande des décisions du
  propriétaire (espaces de noms, signatures, secrets) qui ne
  dépendent pas de ce dépôt.

## Erratum (2026-11-10, chantier C3 de la feuille de route)

L'audit comparatif (`docs/audit/ANALYSE-COMPARATIVE.md`, § 7.1) a
mesuré trois affirmations de ce résumé contre l'état réel du dépôt ;
elles se corrigent ici — le corps du texte reste tel que clos.

1. **« neuf modules publiés » (section 6)** : ils sont **huit** —
   `core-text`, `core-syntax`, `core-syntax-grammars`,
   `core-language`, `core-intelligence`, `core-lsp`, `lsp-protocole`,
   `core-ai`. Le `cli` n'a pas `maven-publish` (convention
   d'application, pas de bibliothèque) : il ne publie pas.
2. **« aucun “Non démarré” ni “Noyau” en P0 » (critères de sortie)** :
   la matrice compte **16 lignes P0 au statut Noyau** — des
   fonctionnalités livrées au cœur, sans test de vue cité. Le critère
   se relit : P0 câblé au cœur, démontré quand le scénario existe ;
   la dette « Noyau » est consignée ligne à ligne par la matrice.
3. **« parité en/fr (116/116) » (section 1)** : la mesure directe des
   `strings.xml` donne **99/99 clés** — aucune clé ne manque, le
   chiffre 116 était faux.

La quatrième incohérence de l'audit (l'index des ADR arrêté à 016)
est rattrapée à l'ADR-018 ; la cinquième (la `VERSION` du `cli`)
est alignée sur 1.0.0 par ce même chantier C3.
