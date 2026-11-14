# celeris

**celeris** (latin : « rapide, vif ») est un éditeur de code embarquable pour
Android : une bibliothèque que toute application hôte intègre pour obtenir
une surface d'édition comparable à celle d'un environnement de développement
de référence — édition multi-curseur, coloration syntaxique, plis,
recherche/remplacement, complétion, refactorings, texte fantôme — pensée
**tactile d'abord**, hors ligne d'abord, et sobre en mémoire comme en batterie.

Le cœur du moteur ne dépend **pas du SDK Android** : il se construit et se
teste sur JVM pure, en quelques secondes. Le même cœur alimente la vue Canvas
Android, l'application de démonstration et un exécutable en ligne de commande.

## État du projet

**1.1.0 taguée `v1.1.0` le 2026-11-12 ; la 1.2.0 est en chantier.**
Les jalons M0 à M9 sont bouclés : moteur de texte, vue Canvas, syntaxe,
édition assistée, sémantique, Android et SPI, IA native, plateforme de
vingt-quatre langages (grammaires réellement TextMate, LSP derrière la
frontière protocolaire), durcissement. Les chantiers post-1.0 : onze
clos (C1, C2, C3, C4, C6, C7, C8, C9, C10, C12, C13) ; C5 vit (Go
et Rust livrés le 2026-11-12, la traîne dort sur le signal du
propriétaire), C11 est livré et passé vert (la recette manuelle du
propriétaire reste), C14 attend sa décision. La reprise du propriétaire
du 2026-11-12 a redressé cinq écarts de la revue comparative : l'icône
de pli répond au doigt (la garde vit pour toute analyse), la minimap
se rend COLORÉE, la toolbar porte son débordement dans la carte du kit
et sa barre de touches au-dessus du clavier, l'Entrée entre accolades
rend le repli textuel, et le serveur Java déclaré s'éprouve entier
(signature jointe au survol, diagnostics poussés). Les étapes restantes
vivent dans
[`docs/feuille-de-route-restante.md`](docs/feuille-de-route-restante.md) ;
l'historique des chantiers et le journal des clôtures dans
[`docs/feuille-de-route.md`](docs/feuille-de-route.md) ; la revue
comparative courante dans
[`docs/audit/ANALYSE-COMPARATIVE-2.md`](docs/audit/ANALYSE-COMPARATIVE-2.md).

## Publication

Douze artefacts Maven, groupe `jo.celeris`, version 1.1.0 — chacun avec son
jar (ou son AAR pour les bibliothèques Android, depuis le 2026-11-11), ses
sources, sa Javadoc, son SBOM CycloneDX et sa somme SHA-256 (ADR-011) :
`core-text`, `core-syntax`, `core-syntax-grammars`, `core-language`,
`core-intelligence`, `core-lsp`, `core-ai`, `lsp-protocole`,
`serveur-lsp-java` (JVM pure) ;
`android-view`, `android-widgets`, `android-host` (AAR). La publication
distante est close (chantier C1, 2026-11-12) : les douze artefacts
paraissent au registre de paquets de l'hébergeur
(`maven.pkg.github.com/jjoblab/celeris`), un projet vierge les consomme
et les sommes correspondent octet pour octet. Coordonnées et déclaration
des dépendances :
[`USAGE.md`](USAGE.md) (§ 2, § 3).

## Les popups de l'éditeur

**Règle absolue (ADR-018)** : toute surface fonctionnelle est une popup —
jamais de dialogue système, jamais d'écran plein cadre. **PIEGE-27** : le
clavier n'est jamais volé — l'éditeur garde le focus et DÉLÈGUE flèches,
Entrée et Échap aux cartes ouvertes.

Les popups vivent dans les modules de l'éditeur : `android-widgets` porte
le kit (cartes, manager, machine à états, feuilles), `android-host`
porte l'orchestrateur **`PopupsEditeur`** — l'application hôte n'a
presque rien à écrire (déménagement du 2026-11-14, PORT ACHEVÉ : la
démonstration ne teste plus qu’elle-même — les HUIT surfaces réveillées
du feu vert vivent chez l’orchestrateur par leurs satellites : l’info
de type (`TypeInfo`), les usages (`Usages`), la documentation externe
(`DocExterne`), les hiérarchies (`CarteHierarchies`), les gabarits
(`Gabarits`), la sûreté (`Surete`), le masquage d’inspection
(`Masquage`) et les détails d’exception (`ExceptionCarte`) — le pont
de test est mort, l’audit Android Studio du propriétaire y a aligné
chaque carte sur l’IDE de référence ; deux gestes de l’hôte se posent
par configuration : `configurerGabaritMethode` et
`configurerRelanceLint`).

**Le simulateur interactif**
([`docs/simulateur/popups.html`](docs/simulateur/popups.html)) fait vivre
la couche entière dans un navigateur, sans dépendance ni réseau : les
26 genres avec leurs sections exactes, la machine à états réelle jouable
(frappe, défilement, tap hors cartes, échappement, exclusions), les
feuilles modales et leurs ateliers. Chaque surface y détaille son
utilisation — quand l'ouvrir, comment, ce qu'elle affiche, ses régimes,
l'API hôte ; les boucles complètes tournent sur le scénario de la
démonstration (masquage d'inspection avec relance du lint, suppression
sûre, exception relue du document réel, annulation de chaque écriture).
Les maquettes statiques d'origine restent dans
[`docs/maquettes/popups.html`](docs/maquettes/popups.html).

```java
import jo.celeris.android.host.popups.CoachPopups;
import jo.celeris.android.host.popups.PopupsEditeur;

// la scène : l'éditeur, sa session, la couche posée dessus (USAGE.md § 4.1)
PopupsEditeur popups = new PopupsEditeur(editeur, session, couche,
        theme, tailleTexteSp, CoachPopups.muet());

// le contenu : un serveur de langage (optionnel) et les tables de l'hôte
popups.brancherServeur(serveur, "file:///Main.java", langage);
popups.configurerIntentions(intentions);
popups.configurerActionsSelection(principales, debordement);
popups.setPlan(entreesDuFichier);
popups.setDiagnostics(diagnosticsDeLhote);

// les déclenchements maison : le double-tap et le clavier délégué —
// tout le reste (frappe, sélection, défilement) vit déjà dans la vue
editeur.setSurDoubleTap(offset ->
        popups.ouvrirColorPicker(offset)
                || popups.ouvrirSurvolAuDoubleTap(offset));
editeur.setSurTouchePopup(popups::surTouche);
```

Sans serveur, la complétion des mots du document vit déjà (le coach
muet suffit) ; le coach de l'hôte enrichit : mots réservés du langage,
signatures connues, documentation — `CoachPopups` en porte les tables.
La complétion se propose à la frappe, l'aide aux paramètres suit l'appel
ouvert, le fil d'Ariane descend le plan, la barre de sélection suit la
sélection, la bande d'erreurs vit avec les diagnostics — l'orchestrateur
branche les écouteurs, l'hôte ne code aucune cascade (frappe, exclusion,
voile : le manager tient l'état ET les cartes d'un seul tenant).

### Les différentes popups

| Popup | Genre (`PopupKind`) | Déclencheur | Données |
| --- | --- | --- | --- |
| Liste de complétion | `COMPLETION_LIST` | la frappe propose, filtre ensuite | serveur LSP ou mots du document |
| Documentation de complétion | `COMPLETION_DOC` | suit sa liste | serveur LSP / coach |
| Aide aux paramètres | `SIGNATURE_HELP` | édition dans un appel ouvert | serveur / coach |
| Survol de symbole | `HOVER` | double-tap, « i » de la barre | serveur / coach |
| Aperçu de définition (peek) | carte `HOVER` | menu, intentions | plan local du fichier |
| Menu d'intentions | `INTENTIONS_MENU` | action de menu au curseur | hôte |
| Info-bulle de diagnostic | `DIAGNOSTIC_TOOLTIP` | sélection sur ligne marquée | SPI `DiagnosticHote` |
| Fil d'Ariane | `BREADCRUMB` | suit le curseur | plan |
| Barre de sélection | `SELECTION_BAR` | sélection non vide | hôte |
| Débordement de la barre | `SELECTION_OVERFLOW` | puce « ⋯ » de la barre | hôte |
| Barre de recherche | `SEARCH_BAR` | demande seule | hôte |
| Bande d'erreurs | `ERROR_BAND` | vit avec les diagnostics | SPI `DiagnosticHote` |
| Minimap | `MINIMAP` | vit avec le document | document |
| Pouce de défilement rapide | `FAST_SCROLLER` | le doigt au bord droit | fenêtre du document |
| Barre de touches de code | `TOUCH_BAR` | posée par l'hôte | hôte |
| Sélecteur de couleur | `COLOR_PICKER` | double-tap d'un littéral | littéral survolé |
| Notification | `NOTIFICATION` | demande de l'hôte, horloge 4 s | hôte |
| Débordement de la barre d'application | `TOOLBAR_OVERFLOW` | puce de la toolbar | hôte |
| Carte des usages | `REFERENCES` | action au curseur | usages du symbole, saut au point |
| Info de type | `TYPE_INFO` | survol du symbole | type lu à la déclaration |
| Hiérarchie | `HIERARCHY` | action au curseur | moteurs de types et d'appels |
| Documentation externe | `EXTERNAL_DOC` | action au curseur | documentation du symbole hors complétion |
| Carte de génération | `GABARITS` | action au caret | champs réels de la classe englobante |
| Suppression sûre | `SAFE_DELETE` | action au curseur | verdicts du moteur de sûreté |
| Déplacement sûr | `SAFE_MOVE` | action au curseur | types candidats au déménagement |
| Masquage d'inspection | `SUPPRESS_INSPECTION` | action au caret | diagnostics réglés de la ligne |
| Détails d'exception | `EXCEPTION_DETAILS` | l'échec d'exécution | trace réelle du document |

Les surfaces denses — recherche, plan, réglages, diff, correctifs,
révocations IA, aller à fichier/symbole (`FeuilleAller`, recherche
floue) — vivent en **feuilles** modales (`FeuilleBas`,
`FeuilleHaut`). Le guide complet (clavier délégué, régimes, composition à
la main) vit dans [`USAGE.md`](USAGE.md) § 9 ; l'inventaire exhaustif,
fiche par fiche, dans [`docs/POPUP_INVENTORY.md`](docs/POPUP_INVENTORY.md).

## Vie privée

Aucun paquet celeris n'embarque de télémétrie — ni collecte, ni
déclaration d'usage. Les contrats IA du cœur ne connaissent aucun
transport : les modèles se branchent par l'hôte, et rien ne part sans
le consentement explicite de l'utilisateur (`GardeContexte`,
[`docs/adr/012-contrats-ia-agents.md`](docs/adr/012-contrats-ia-agents.md)).
L'hôte qui veut une télémétrie l'ajoute derrière son propre
consentement, anonyme et documentée — jamais dans le cœur. La
démonstration ne déclare d'ailleurs aucune permission réseau : sa
neutralité par défaut est structurelle, vérifiée en CI.

## Charge et limites

Les budgets mesurés — bancs de la CI, tas dédié de 1 Go, minimum de
trois tours (calibrages consignés dans les bancs) :

| Charge | Budget | Mesuré |
| --- | --- | --- |
| Édition, 50 000 lignes | ≤ 1 ms | 0,145 ms |
| Première image, 100 000 lignes | ≤ 500 ms | 154 ms (texte), 224 ms (analyse par fenêtre) |
| Ouverture, 480 000 lignes (12,1 M caractères) — la fenêtre d'abord | ≤ 2 500 ms | 1214–1441 ms, tas +90 Mo |
| Édition au milieu, 480 000 lignes — l'incrément | ≤ 100 ms | 2–36 ms |

Le défilement ne coûte rien : la fenêtre visible se note, le travail
en vol ne s'annule pas ; l'analyse vit hors fil
([ADR-007](docs/adr/007-modele-de-threading.md)) — la fenêtre d'abord,
la complétion par lots ensuite. Au-delà des seuils « gros document »
du dépôt de départ (2,5 M caractères ou 50 000 lignes), le régime des
fonctions lourdes coupe la complétion au curseur — l'édition reste
entière : coloration, frappe et annulation ne se coupent jamais.
Tri-état à la manière du dépôt de départ :
`editorView.intelligence().setLargeFileMode(AUTO | ON | OFF)` — AUTO
(défaut) suit les seuils du document, ON force les coupures, OFF les
ferme même au-delà des seuils.

## Bancs et cliquets

331 bancs JVM et 2 331 méthodes `@Test`, plus les quatre bancs
d'appareil (relevé du 2026-11-14, les quatorze modules compris). Le volume n'est pas un but en soi (décision du
propriétaire du 2026-11-11 : la parité de volume avec l'audit du dépôt
de départ ne se poursuit pas) — les cliquets font foi : couverture
JaCoCo ≥ 90 % de lignes sur les modules purs, score de mutation ≥ 80 %
sur `core-text` (ADR-010), plafonds Checkstyle/PMD (600/800 lignes par
fichier, ADR-002/019 ; complexité 15), manifestes d'API vérifiés à
chaque construction (ADR-009).

## Modules

Chaque module porte son propre `README.md` (rôle, coordonnées, bancs,
portes) :

| Module | Nature | Rôle |
| --- | --- | --- |
| [`core-text`](core-text/README.md) | JVM pure | modèle de texte, instantanés, transactions, multi-sélection, annulation |
| [`core-syntax`](core-syntax/README.md) | JVM pure | analyse syntaxique incrémentale, structure (plis, plan, crochets) |
| [`core-syntax-grammars`](core-syntax-grammars/README.md) | JVM pure | voie de grammaires TextMate, longue traîne (ADR-014) |
| [`core-language`](core-language/README.md) | JVM pure | plateforme de langages : contrats, paquets de données, registre (ADR-017) |
| [`core-intelligence`](core-intelligence/README.md) | JVM pure | index de symboles, complétion, refactorings, formatage, moteurs des surfaces réveillées |
| [`core-lsp`](core-lsp/README.md) | JVM pure | domaine LSP : types immuables, ordonnancement, UTF-16 (ADR-015) |
| [`lsp-protocole`](lsp-protocole/README.md) | JVM pure | frontière protocolaire du LSP : session, montages embarqués (ADR-015) |
| [`serveur-lsp-java`](serveur-lsp-java/README.md) | JVM pure | mini serveur de langage Java : compagnon de banc de l'éditeur (ADR-015) |
| [`core-ai`](core-ai/README.md) | JVM pure | contrats IA et API d'outils pour agents |
| [`android-view`](android-view/README.md) | bibliothèque Android | vue Canvas, rendu, entrées, IME, accessibilité, gouttière |
| [`android-widgets`](android-widgets/README.md) | bibliothèque Android | kit de popups (ADR-018), complétion, recherche, plan, minimap, défilement rapide |
| [`android-host`](android-host/README.md) | bibliothèque Android | adaptateurs hôte (SPI) : fichiers, VCS, exécution, débogueur, historique local — et l'orchestration des popups (`PopupsEditeur`, déménagement du 2026-11-14) |
| [`cli`](cli/README.md) | application JVM | exécutable sans Android (diagnostiquer, formater, refactorer) |
| [`demo`](demo/README.md) | application Android | terrain de démonstration complet |

Douze des quatorze modules se publient en artefacts Maven
(`jo.celeris:*`, section « Publication ») ; la cli et la démo sont des
applications, jamais publiées.

## Documentation

- [`USAGE.md`](USAGE.md) — guide d'intégration hôte : dépendances, montage de la vue, langages, serveurs de langage, popups, SPI, contrats IA, budgets (chantier C4).
- [`ARCHITECTURE.md`](ARCHITECTURE.md) — vue d'ensemble technique et règles de dépendance.
- [`MIGRATION.md`](MIGRATION.md) — guide de migration du dépôt de départ vers celeris (final à M9).
- [`docs/adr/`](docs/adr/) — décisions d'architecture (ADR-001 à ADR-019).
- [`docs/feuille-de-route-restante.md`](docs/feuille-de-route-restante.md) — les étapes restantes (état vérifié au 2026-11-11) ; [`docs/feuille-de-route.md`](docs/feuille-de-route.md) — l'historique des chantiers post-1.0 et des jalons M1 à M9, avec le journal des clôtures.
- [`docs/parite/MATRICE-PARITE.md`](docs/parite/MATRICE-PARITE.md) — référentiel de parité, catégories A à H.
- [`docs/audit/`](docs/audit/) — état des lieux du dépôt de départ, registre de portage, pièges hérités, spikes, audit d'accessibilité et revue de sécurité (M9), analyse comparative avec le dépôt de départ.
- [`docs/REFERENCES.md`](docs/REFERENCES.md) — projets et normes cités, licences vérifiées.
- [`docs/maquettes/popups.html`](docs/maquettes/popups.html) — maquettes réactives des popups (ADR-018) : feuille du bas, feuille du haut, popup ancrée.
- [`docs/GLOSSAIRE.md`](docs/GLOSSAIRE.md) — terminologie française de référence.

## Language / Langue

Code, identifiants et messages d'exception : anglais. Javadoc, commentaires
et documentation : français (typographie comprise). Voir
`scripts/check-language-policy`.

## Licence

Apache-2.0 — voir [`LICENSE`](LICENSE) et [`NOTICE`](NOTICE).
