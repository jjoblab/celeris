# USAGE — le guide d'intégration hôte

> Guide d'intégration de **celeris** pour les applications hôtes Android.
> Version 1.1.0 — chaque coordonnée d'artefact et chaque nom d'API cité
> ici est vérifié contre les manifestes `api/*.api` du dépôt (ADR-009) et
> contre les sources publiques des modules Android. Ce qui manque encore
> se lit honnêtement en § 18 ; la terminologie vit dans
> [`docs/GLOSSAIRE.md`](docs/GLOSSAIRE.md), la migration depuis le dépôt
> de départ dans [`MIGRATION.md`](MIGRATION.md).

## Sommaire

1. [Ce que vous intégrez](#1-ce-que-vous-intégrez)
2. [Coordonnées et disponibilité](#2-coordonnées-et-disponibilité)
3. [Déclarer les dépendances](#3-déclarer-les-dépendances)
4. [Montage de la vue](#4-montage-de-la-vue)
5. [Le modèle de texte](#5-le-modèle-de-texte)
6. [Le registre des langages](#6-le-registre-des-langages)
7. [Ajouter un langage — le paquet de données](#7-ajouter-un-langage--le-paquet-de-données)
8. [Serveurs de langage (LSP)](#8-serveurs-de-langage-lsp)
9. [Popups et feuilles (ADR-018)](#9-popups-et-feuilles-adr-018)
10. [Le SPI hôte — VCS, exécution, débogueur, fichiers](#10-le-spi-hôte--vcs-exécution-débogueur-fichiers)
11. [Contrats IA et consentement](#11-contrats-ia-et-consentement)
12. [Thèmes et couleurs](#12-thèmes-et-couleurs)
13. [Budgets de performance attendus](#13-budgets-de-performance-attendus)
14. [Grands documents et coupures](#14-grands-documents-et-coupures)
15. [L'exécutable en ligne de commande](#15-lexécutable-en-ligne-de-commande)
16. [Intégrité, provenance et périmètre de l'API](#16-intégrité-provenance-et-périmètre-de-lapi)
17. [Vie privée et sécurité](#17-vie-privée-et-sécurité)
18. [Ce qui manque encore](#18-ce-qui-manque-encore)
19. [Pièges d'intégration](#19-pièges-dintégration)

## 1. Ce que vous intégrez

celeris est un éditeur de code embarquable : vous gardez votre activité,
vos écrans, votre navigation — la bibliothèque rend des **surfaces
d'édition** (vue Canvas, popups, complétion, diagnostics, refactorings)
et un **cœur JVM pur** qui ne dépend pas du SDK Android. Le cœur se
construit et se teste sur JVM seule ; la vue et le kit de popups sont
des bibliothèques Android ; l'hôte assemble.

| Module | Nature | Ce que l'hôte en fait |
| --- | --- | --- |
| `core-text` | JVM pure | le document, la session d'édition, les transactions, le multi-curseur, l'annulation |
| `core-syntax` | JVM pure | l'analyse syntaxique incrémentale, les plis, le plan, les crochets |
| `core-syntax-grammars` | JVM pure | la voie de grammaires TextMate — la longue traîne des langages en données |
| `core-language` | JVM pure | les contrats de langage, les paquets de données, le registre |
| `core-intelligence` | JVM pure | l'index de symboles, la complétion, les refactorings, le formatage |
| `core-lsp` | JVM pure | le domaine LSP : types immuables, ordonnancement, UTF-16 (ADR-015) |
| `lsp-protocole` | JVM pure | la frontière protocolaire du LSP : session et montages embarqués |
| `core-ai` | JVM pure | les contrats IA, le texte fantôme, les outils d'agent (ADR-012) |
| `android-view` | Android | la vue Canvas : rendu, IME, sélections, accessibilité |
| `android-widgets` | Android | les popups et feuilles du kit unifié (ADR-018) |
| `android-host` | Android | les adaptateurs hôte : SPI VCS/exécution/débogueur, fichiers |
| `cli` | application JVM | l'exécutable sans Android — voir § 15 |

Exigences vérifiées : artefacts **Java 17** construits sur JDK 21,
`minSdk 26`, `compileSdk 36`, licence Apache-2.0, aucune dépendance au
runtime Kotlin. Le seul module qui expose la bibliothèque LSP4J est
`lsp-protocole` — votre code hôte n'en voit jamais le nom (ADR-015) ;
le moteur de grammaires de `core-syntax-grammars` vit dans le NOTICE et
le SBOM, jamais dans un identifiant.

## 2. Coordonnées et disponibilité

Les coordonnées Maven, groupe `jo.celeris`, version `1.1.0` —
l'artifactId porte le nom du module. **Douze artefacts sont publiés**,
chacun avec son jar (ou son AAR), ses sources, sa Javadoc, son SBOM
CycloneDX et sa somme SHA-256 (§ 16) — les neuf modules purs :

`jo.celeris:core-text:1.1.0` · `jo.celeris:core-syntax:1.1.0` ·
`jo.celeris:core-syntax-grammars:1.1.0` · `jo.celeris:core-language:1.1.0` ·
`jo.celeris:core-intelligence:1.1.0` · `jo.celeris:core-lsp:1.1.0` ·
`jo.celeris:core-ai:1.1.0` · `jo.celeris:lsp-protocole:1.1.0` ·
`jo.celeris:serveur-lsp-java:1.1.0`

et, depuis le 2026-11-11 (décision du propriétaire), les trois
bibliothèques Android en AAR :

`jo.celeris:android-view:1.1.0` · `jo.celeris:android-widgets:1.1.0` ·
`jo.celeris:android-host:1.1.0`

État vérifié au 2026-11-11 :

- **Publication locale** : `./gradlew publishToMavenLocal
  publicationChecksums` est vert — un projet vierge consomme les douze
  artefacts depuis `mavenLocal()` dès aujourd'hui.
- **Publication distante** : le registre de paquets de l'hébergeur
  (`https://maven.pkg.github.com/jjoblab/celeris`) porte les douze
  artefacts `1.1.0` depuis le 2026-11-12 (chantier C1 clos) — un projet
  vierge consomme `core-text` (compile, exécute, édite), l'AAR
  `android-view` s'y vérifie, les sommes SHA-256 correspondent.
  Le dépôt GitHub du projet est le point de contact ; l'annonce de la
  publication distante s'y lira le moment venu.
- **Modules Android** (`android-view`, `android-widgets`, `android-host`) :
  publiés comme les modules purs depuis le 2026-11-11 — AAR de la
  variante release, sources, Javadoc, SBOM et sommes de contrôle ;
  l'inclusion de source reste possible (§ 3.3) pour lire et adapter.

## 3. Déclarer les dépendances

### 3.1 Aujourd'hui — le dépôt local

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("jo.celeris:core-text:1.1.0")
    implementation("jo.celeris:core-syntax:1.1.0")
    implementation("jo.celeris:core-syntax-grammars:1.1.0")
    implementation("jo.celeris:core-language:1.1.0")
    implementation("jo.celeris:core-intelligence:1.1.0")
    implementation("jo.celeris:core-lsp:1.1.0")
    implementation("jo.celeris:core-ai:1.1.0")
    // uniquement si vous montez des serveurs de langage embarqués
    implementation("jo.celeris:lsp-protocole:1.1.0")
    // surfaces Android (AAR, publiées depuis le 2026-11-11)
    implementation("jo.celeris:android-view:1.1.0")
    implementation("jo.celeris:android-widgets:1.1.0")
    implementation("jo.celeris:android-host:1.1.0")
}
```

Le cœur n'exige que `core-text` ; chaque autre module ne tire que ce
que son rôle demande — les règles de dépendance vivent dans
[`ARCHITECTURE.md`](ARCHITECTURE.md). Le vôtre inclut ce qu'il expose.

### 3.2 Quand la publication distante existera — le registre de l'hébergeur

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/jjoblab/celeris")
        credentials {
            username = System.getenv("GITHUB_USER")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}
```

Le jeton est un *personal access token* de portée `read:packages`, le
nom d'utilisateur votre identifiant GitHub. Les coordonnées ne changent
pas — seul le dépôt s'ajoute (annonce et vérification des sommes au
chantier C1, § 18).

### 3.3 Les modules Android — artefacts, ou inclusion de source

Depuis le 2026-11-11, les trois modules Android se consomment comme les
modules purs — par coordonnées (§ 3.1) : l'AAR de la variante release,
ses sources et sa Javadoc se publient à chaque publication, SBOM et
sommes de contrôle compris.

L'inclusion de source reste possible — pour lire, adapter ou déboguer.
Les trois modules et leurs ressources se copient dans votre
build (ils sont autonomes, sans dépendance externe au runtime) puis se
déclarent dans votre `settings.gradle.kts` :

```kotlin
include(":celeris-android-view")
include(":celeris-android-widgets")
include(":celeris-android-host")

project(":celeris-android-view").projectDir =
        file("celeris/android-view")
project(":celeris-android-widgets").projectDir =
        file("celeris/android-widgets")
project(":celeris-android-host").projectDir =
        file("celeris/android-host")
```

```kotlin
dependencies {
    implementation(project(":celeris-android-view"))
    implementation(project(":celeris-android-widgets"))
    implementation(project(":celeris-android-host"))
}
```

Ces modules référencent les artefacts du cœur par les mêmes
coordonnées (`jo.celeris:*:1.1.0`) — votre `mavenLocal()` ou le dépôt
distant les résout. La publication des modules Android comme artefacts
est tranchée depuis le 2026-11-11 (décision du propriétaire, ADR-011) ;
l'inclusion de source reste la voie de secours pour lire et adapter.

## 4. Montage de la vue

### 4.1 Le gabarit

La vue est un `View` Android ordinaire ; la couche porte-popups se
superpose dans la même fenêtre et **ne prend jamais le focus**. Les
popups s'y posent en coordonnées de scène et se repositionnent au
défilement.

```xml
<FrameLayout
    android:id="@+id/scene_edition"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <jo.celeris.android.view.EditorView
        android:id="@+id/editeur"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <jo.celeris.android.widgets.PopupLayer
        android:id="@+id/couche_popups"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</FrameLayout>
```

### 4.2 Le premier montage en code

```java
import jo.celeris.android.view.EditorThemes;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.FontSpec;
import jo.celeris.core.text.TextSession;

TextSession session = TextSession.of(contenu, System::nanoTime);
EditorView editeur = findViewById(R.id.editeur);
editeur.setSession(session);
editeur.setTheme(EditorThemes.SOMBRE);
editeur.setFont(new FontSpec("mono", 14f));
editeur.setLanguage(langue);        // LanguageSupport, ou null
```

La vue ne crée jamais la session : `setSession(TextSession)` la lui
confie (§ 5), `setLanguage(LanguageSupport)` lui dit la couleur, les
plis, le plan. Sans langage, l'édition reste entière — la coloration
seule se tait.

### 4.3 Les réglages usuels de la vue

| Méthode | Rôle |
| --- | --- |
| `setSession(TextSession)` | confie le document vivant à la vue |
| `setLanguage(LanguageSupport)` | la couleur, les plis, le plan, l'indentation |
| `setTheme(EditorTheme)` | l'un des trois thèmes livrés, ou le vôtre |
| `setFont(FontSpec)` | la police (`family`, `size` en sp) |
| `setSoftWrap(boolean)` | le retour à la ligne visuel |
| `setNumerosVisibles(boolean)` | les numéros de gouttière |
| `setKeyBindings(KeyBindings)` | les liaisons clavier (`KeyBindings.defaults()` pour partir) |
| `setRetenueAnalyseMs(long)` | la retenue d'analyse avant re-travail (250 ms par défaut) |
| `intelligence()` | le hub des fonctions assistées (§ 14) |
| `marquesGouttiere()` | les marques de gouttière (§ 10) |
| `setOnFontSizeChanged(Consumer<Float>)` | le pincement change la taille, l'hôte suit |
| `setSurPlan(Consumer<List<OutlineEntry>>)` | le plan du document suit l'édition |

L'accessibilité est native : la vue sert un arbre virtuel
(`getAccessibilityNodeProvider`), annonce la sélection et répond aux
actions de déplacement — l'auditeur automatique de la CI en vérifie
les règles (ADR-008).

## 5. Le modèle de texte

Tout passe par la `TextSession` (`core-text`) — les positions sont des
offsets UTF-16 en `int`, le document est immuable par instantané
(`Document`), la mutation vit dans la `Transaction` (ADR-003).

```java
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

TextSession session = TextSession.of(contenu, System::nanoTime);

// la voie rapide — frappe, remplacement, annulation
session.type("bonjour");
session.replaceRange(0, 5, "Salut");
session.undo();
session.redo();

// la voie explicite — plusieurs éditions, une annulation
Transaction t = Transaction.of(
        new TextEdit(0, "ancien", "nouveau"),
        new TextEdit(20, "x", "y"));
session.apply(t);

// la sélection multi-curseur
session.setSelection(MultiSelection.of(
        SelRange.caretAt(12), new SelRange(40, 45)));

// la recherche et le remplacement global
int remplaces = session.replaceAll("ancien", "nouveau",
        new FindOptions().withCaseSensitive(true).withWholeWord(true));

// l'ancrage — des plages qui survivent aux éditions
session.addProjector(ancrages);   // AnchoredRanges (PositionProjector)
```

Les auditeurs sont explicites : `addEditListener`, `addSelectionListener`
— jamais d'appel de repli sur le fil de l'hôte. Les limites d'annulation
vivent dans la session (`undoDepth`, `redoDepth`, `DEFAULT_MAX_DEPTH`).
La recherche tolère les expressions rationnelles et les budgets bornés
(`FindReplace` lève `RegexBudgetExceededException` au-delà) — la
méthode `Document.isLarge()` dit le régime du document (§ 14).

## 6. Le registre des langages

Le registre (`core-language`) assemble paquets intégrés, paquets
propres et serveurs déclarés — puis répond par la détection (ADR-017).
L'assemblage se fait **une fois, hors fil UI** (ADR-007) ; la
démonstration du dépôt (`LangagesDemo`) en est le modèle vivant.

```java
import jo.celeris.core.intelligence.WordCompletion;
import jo.celeris.core.language.Completion;
import jo.celeris.core.language.LanguageDetection;
import jo.celeris.core.language.LanguagePlatform;
import jo.celeris.core.language.LanguageRegistry;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.grammars.PackGrammars;

LanguageRegistry registre = LanguagePlatform
        .with(PackGrammars.INSTANCE)     // la voie de grammaires
        .builtinPacks()                  // les 22 paquets intégrés
        .servers(declarations)           // vos serveurs déclarés (§ 8)
        .generic(Completion.class, WordCompletion.INSTANCE)
        .build();                        // repli de complétion par mots

LanguageDetection detection = registre.detect(
        nomFichier, premiereLigne, echantillon);
LanguageSupport langue = detection instanceof LanguageDetection.Resolved r
        ? r.support()
        : null;                          // Unresolved ou Ambiguous
editeur.setLanguage(langue);
```

Les vingt-quatre paquets intégrés (vingt-deux à la 1.0.0, Go et Rust depuis la 1.2.0) : `c`,
`cpp`, `css`, `go`, `groovy`, `html`, `java`, `javascript`, `json`,
`kotlin`, `kts`, `markdown`, `php`, `proguard`, `properties`,
`python`, `ruby`, `rust`, `shell`, `sql`, `toml`, `typescript`, `xml`,
`yaml`. Six langages P0 portent en plus un
**analyseur écrit** (`JavaSyntax`, `KotlinSyntax`, `KtsSyntax`,
`GroovySyntax`, `XmlSyntax`, `JsonSyntax` de `core-syntax`) — arbre
tolérant incrémental, plis, plan, crochets et indentation sur le même
arbre (ADR-004) ; tous les autres vivent par la grammaire TextMate
compilée (ADR-014).

La **recharge à chaud** est le modèle : reconstituez le registre, les
sessions ouvertes gardent leur langage, les nouveaux fichiers parlent
au registre neuf. `registry.byId(id)` et `registry.byFilename(nom)`
court-circuitent la détection quand l'hôte sait déjà.

## 7. Ajouter un langage — le paquet de données

Un langage entier est un **dossier de données** (`langages/<id>/`),
aucune recompilation — six fichiers, trois obligatoires :

| Fichier | Contenu | Obligatoire |
| --- | --- | --- |
| `pack.json` | identité : `id`, `version`, `schemaVersion` (1), `license`, `provenance` | oui |
| `language.json` | descripteur (noms, extensions, motifs, première ligne, badge) et configuration (commentaires, `brackets`, fermeture, indentation, plis) — le sous-ensemble du format VS Code | oui |
| `grammar.json` | la grammaire TextMate, format réel des extensions VS Code, inclusions et injections comprises | oui |
| `outline.json` | le plan par règles (sélecteur → catégorie → groupe de capture) | non |
| `snippets.json` | les gabarits (`PackSnippet` : préfixe, trous tabulables, corps multiligne) | non |
| `servers.json` | les serveurs de langage déclarés pour ce langage (§ 8) | non |

Chargement depuis votre stockage :

```java
import jo.celeris.core.language.packs.Packs;

LanguagePlatform plateforme = LanguagePlatform
        .with(PackGrammars.INSTANCE)
        .builtinPacks()
        .addPackSource(Packs.folder(chemin));  // un sous-dossier par paquet
// Packs.archive(zip) — entrées plates, liens symboliques et remontées refusés
// Packs.classpath(prefixe) — les paquets dans vos ressources
```

Le chargeur (`PackLoader`) borne tailles et profondeur et refuse les
archives montantes — un paquet hostile ne sort pas de son dossier. Un
analyseur écrit reste possible pour un langage exigeant et se déclare
attaché par préférence dans le `language.json` du paquet.

## 8. Serveurs de langage (LSP)

La frontière est double (ADR-015) : `core-lsp` porte le domaine immuable
et l'ordonnanceur ; `lsp-protocole` porte la session et les montages.
Votre code hôte ne voit **jamais** la bibliothèque protocolaire —
seulement ces deux paquets.

**La déclaration décide, jamais le code** (revue de sécurité M9) : le
fichier `servers.json` de l'hôte dit quel serveur sert quel langage, et
l'hôte seul décide de le monter. Le format :

```json
{
  "servers": [
    {
      "language": "java",
      "transport": "embedded",
      "serverClass": "jo.celeris.serveur.java.JavaLanguageServer",
      "clientInterface": "org.eclipse.lsp4j.services.LanguageClient",
      "clientName": "mon-hote"
    }
  ]
}
```

```java
import jo.celeris.core.language.model.LanguageServerDeclaration;
import jo.celeris.lsp.protocole.ServeurEmbarque;

List<LanguageServerDeclaration> declarations =
        LanguageServerDeclaration.parse(jsonServeurs);
LanguageServerDeclaration d = declarations.get(0);   // "embedded"

ServeurEmbarque serveur = ServeurEmbarque.monter(
        d.serverClass(), d.clientInterface(), d.clientName());
CapacitesServeur capacites = serveur.session()
        .initialiser(d.initializationOptions());
serveur.session().ouvrir(uri, "xml", texte);
```

- `LanguageServerDeclaration.embedded(...)` monte le serveur **en
  processus**, lancé par son API, jamais par sa ligne de commande ;
- `stdio(...)` et `tcp(...)` préparent les transports externes — le
  montage effectif reste à votre main (l'hôte garde la main sur les
  processus, ADR-009) ;
- `TubesMemoire.creer()` donne des tubes mémoire si vous pilotez vous-
  même un binaire ; `LspSession.sur(entree, sortie, nom)` habille vos
  propres flux.

La session couvre les familles du protocole : complétion, survol, signature,
définition, références, symboles, surlignages, renommage (préparation
comprise), correctifs, formatage (document et plage), plis, plages de
sélection, jetons sémantiques et indices en ligne ; les diagnostics
arrivent par `ajouterAuditeurDiagnostics`. Les requêtes concurrentes
sont fusionnées, annulables et priorisées par l'ordonnanceur
(`OrdonnanceurLsp`, `RequeteAnnuleeException`) — le dialogue avec la
vue suit les versions du document (`SuiviVersions`). Fermez toujours :
`LspSession` et `ServeurEmbarque` sont `AutoCloseable`.

## 9. Popups et feuilles (ADR-018)

**Règle absolue** : toute surface fonctionnelle est une popup — feuille
du bas, feuille du haut ou popup ancrée au point d'intérêt. Jamais
d'écran plein cadre, jamais de dialogue système : **l'hôte reste
maître de son écran et de sa navigation**. La bibliothèque rend des
surfaces, elle n'impose pas d'écran.

Le montage tient en trois temps — **lier, déclenquer, déléguer :

1. **la couche** — `PopupLayer` posée sur la vue dans votre gabarit
   (§ 4.1) ; elle porte les popups en coordonnées de scène et se
   repositionne au défilement ;
2. **le manager** — `PopupManager` tient la machine
   (`PopupController`) ET la couche d'un seul tenant : l'hôte ne
   retire plus jamais une vue lui-même, l'état et les cartes ne
   divergent jamais.

```java
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.widgets.PopupManager;

PopupManager popups = new PopupManager(new PopupController(), couche);
// lier : UNE vue par genre, UN genre par vue — la carte partagée
// entre genres se refuse dès le câblage
popups.lier(PopupController.PopupKind.COMPLETION_LIST, liste);
popups.lier(PopupController.PopupKind.COMPLETION_DOC, doc);
// déclencher : l'état s'ouvre, les exclus quittent la couche AVANT
// la pose, la carte se borne aux murs de la couche
popups.montrer(PopupController.PopupKind.COMPLETION_LIST, x, y);
// les régimes : la frappe referme les bulles, la liste filtre — les
// cartes des fermés quittent la couche, sans boucle à coder
popups.surFrappe();
popups.surDefilement();
// les cascades : fermer la liste emporte SA documentation
popups.fermer(PopupController.PopupKind.COMPLETION_LIST);
// la feuille modale qui s'ouvre coupe l'intelligence, le chrome vit
popups.fermerSousVoile();
// la mesure unique — vos ancres (PopupAnchor) y anticipent la pose
PopupPlacer.Cadre taille = popups.taille(PopupController.PopupKind.HOVER);
// le détachement de fin de vie : tout se ferme, l'avis du tap se coupe
popups.detacher();
```

La règle qui porte ce rendu : **la vue ne survit jamais à son état** —
le manager la garantit. Tout ce qui ferme (frappe, défilement,
exclusion, cascade, voile) rend ses genres et RETIRE leurs cartes
d'un seul tenant ; la documentation suit sa liste, le débordement
suit sa barre, jamais l'hôte ne recode les cascades. Le re-pose de
la MÊME vue sous son genre (le filtre de complétion, à chaque
frappe) ne la re-naît pas : elle se re-mesure et se re-place ; une
même vue changée de genre quitte l'ancien — une vue ne vit jamais
sous deux genres. La machine reste publique pour qui compose à la
main : `onKeystroke`, `onScroll` et `closeAll` rendent les genres
qu'ils fermaient — l'hôte sans manager les retire de sa
`PopupLayer` lui-même, sinon la vue y reste en zombie.

3. **le clavier délégué** — PIEGE-27 : les popups ne prennent JAMAIS
   le focus, l'éditeur le garde et délègue. Le crochet public se
   consulte en tête de `onKeyDown`, le verdict consommé rend la main
   sans toucher au curseur :

```java
editeur.setSurTouchePopup(popups::surTouche);
```

UP et DOWN déplacent la surbrillance de la première carte
interactive ouverte, PAGE_UP et PAGE_DOWN sautent d'une PAGE (le pas étendu — huit rangées à la complétion, son plafond visible ; la carte sans pages décline, la touche retourne à l'éditeur et le document y défile), Enter et Tab valident, Escape referme (la
complétion emporte sa documentation) ; la touche inconnue retourne à
l'éditeur — la table de raccourcis décide sa vie normale. Les cartes
interactives du kit (complétion, menus d'intentions et de
débordement, usages, gabarits, cibles du déménagement) reçoivent par le contrat `PopupClavier` (`surFleche`,
`surPage`, `surValidation`) — les vôtres y gagnent la même délégation. Le doigt
qui tombe HORS de toute carte referme les genres qui cèdent (les
menus, les bulles — la complétion suit le curseur, le chrome vit
avec le document) : l'avis vit à la couche, branché par le manager,
le geste poursuit sa vie vers l'éditeur.

Les surfaces denses vivent dans les feuilles — `FeuilleBas` et
`FeuilleHaut` (`android-widgets`) pour la recherche, les réglages et
le plan, les rappels contextuels compris, construites avec votre
`PopupPalette` ; la démonstration dérive la sienne du thème et de la
taille de texte.

Les genres du contrôleur (`PopupKind`) — vingt-six depuis le feu vert
du 2026-11-14 — couvrent les surfaces de l'éditeur :
complétion et sa documentation, menu d'intentions, aide aux
paramètres, survol, info-bulle de diagnostic, fil d'Ariane,
recherche, barre de sélection et son débordement, bande d'erreurs,
minimap, pouce de défilement, barre de touches, sélecteur de
couleur, débordement de la barre d'application, carte des usages,
info de type, hiérarchie de types et d'appels, notification,
documentation externe, carte de génération
(constructeur, accesseurs, equals/hashCode/toString, entourage
try/catch et if, gabarit de méthode), usages qui barrent la
suppression sûre, cibles du déplacement sûr, masquage d'inspection
(la marque « noinspection », le lint l'honore), détails
d'exception (l'exécution simulée porte la trace du document, le
cadre connu saute) ; les valeurs de débogage en ligne vivent
in-canvas (`EditorView.setValeursEnLigne`) et l'aller à
fichier/symbole en feuille (`FeuilleAller`) — vos surfaces propres
vivent dans les feuilles. L'inventaire des surfaces vit à
`docs/POPUP_INVENTORY.md` : aucune catégorie A–E ne compte de
manquant. Le kit est thémé et accessible par construction
(ADR-008) : poignée de glissement, bouton de fermeture, voile,
fermeture au retour.

## 10. Le SPI hôte — VCS, exécution, débogueur, fichiers

Le module `android-host` expose les contrats que **vous implémentez**
pour brancher votre monde — l'éditeur les consomme, jamais l'inverse.

| Interface | Méthodes | Ce que l'éditeur en tire |
| --- | --- | --- |
| `IntegrationVcs` | `etatLignes(CheminDoc)`, `blame(CheminDoc)`, `historique(CheminDoc)` | les marques de gouttière (modifié/ajouté), le blame, l'historique |
| `IntegrationExecution` | `lancer(CheminDoc, boolean)`, `etat(CheminDoc)`, `etats()`, `ajouterAuditeur(AuditeurExecution)` | les repères de lancement et d'état par fichier |
| `IntegrationDebogueur` | `poserArret`, `retirerArret`, `basculerArret`, `arrets()`, `pileCourante()`, `valeursEnLigne(int)` | les points d'arrêt, la ligne courante, les valeurs au survol |
| `SystemeFichiersVirtuel` | `ouvrir(CheminDoc)`, `ecrire(CheminDoc, String)`, `lister(CheminDoc)`, `estFichier(CheminDoc)` | l'ouverture et l'enregistrement, sans couche stockage imposée |

Les contrats sont **synchrones et sans état côté éditeur** : vous
rafraîchissez quand vous le décidez (révision changée, enregistrement,
focus de fichier). Le débogueur est borné à la pause et à la pile en
cours — jamais d'exécution libre derrière le dos de l'hôte.

Deux adaptations prêtes à l'emploi : `ArbreDocumentsSaf` (l'arbre de
documents sur l'Architecture de stockage d'Android) et
`MemoireSystemeFichiers` (en mémoire, pour les tests). L'**historique
local** (`HistoriqueLocal`, A-14) enveloppe n'importe quel
`SystemeFichiersVirtuel` d'archives par fichier — `revisions(chemin)`,
`restaurer(chemin, revision)` — isolées de votre VCS.

Côté vue, les marques se posent ainsi :

```java
editeur.marquesGouttiere().setIntegration(marquesVcs);
editeur.marquesGouttiere().setErreursImportees(lignesEnErreur);
editeur.marquesGouttiere().setReperesPreview(lignesDeLancement);
editeur.marquesGouttiere().setSurClicLigne(ligne -> ouvrirFichier(ligne));
```

Des doubles de test accompagnent le module : `FauxVcs`,
`FauxExecution`, `FauxDebogueur` — vos bancs n'attendent pas votre
infrastructure.

## 11. Contrats IA et consentement

**Aucun transport ne vit dans le cœur** (ADR-012) : les modèles se
branchent par vous, et rien ne part sans le consentement explicite de
l'utilisateur. La bibliothèque ne connaît ni réseau, ni clé, ni
fournisseur — vos `ModeleTexteFantome` et `ModelePrediction` sont vos
liaisons, votre affaire, votre politique.

Le montage en quatre temps :

```java
import jo.celeris.core.ai.ExclusionsContexte;
import jo.celeris.core.ai.GardeContexte;
import jo.celeris.core.ai.SessionTexteFantome;

// 1. votre modèle — le vôtre, avec son transport à vous
ModeleTexteFantome fantome = ...;
ModelePrediction prediction = ...;

// 2. la garde — exclusions de chemins et consentement relu à chaque départ
GardeContexte garde = new GardeContexte(fantome, prediction,
        cheminDuDocument,
        ExclusionsContexte.de(".git/\n*.env\n*.jks"),
        this::consentementAccorde);   // BooleanSupplier, faux par défaut

// 3. la session fantôme, sur votre exécuteur
SessionTexteFantome sessionFantome = new SessionTexteFantome(garde, executor);
editeur.setFantome(sessionFantome.suggestionActive());   // au retour

// 4. les gestes — accepter mot à mot, ligne à ligne, tout, ou refuser
sessionFantome.accepterMot();
sessionFantome.accepterLigne();
sessionFantome.accepterTout();
sessionFantome.refuser();
```

- `SessionTexteFantome.demander(Document, int)` lance la suggestion au
  curseur, `frappe()` la retire à la frappe suivante ;
- **sans consentement, la demande ne part pas** — la garde enveloppe
  les deux contrats, les exclusions ferment les fichiers sensibles
  avant tout envoi ;
- les **agents** consomment l'éditeur par `OutilsEditeur`
  (`lire`, `instantane`, `symboles`, `diagnostics`, `instructions`,
  `appliquer`) — `OutilsEditeurSurSession` l'adosse à une session
  vivante ; `ConnecteurMcp` déclare les outils vers l'extérieur,
  `RevueAgent` garde la revue humaine des propositions (F-04) ;
- `FauxModeleLocal` et `FauxModeleDistant` sont des modèles
  déterministes pour vos tests — la démonstration n'a jamais branché
  de réseau.

## 12. Thèmes et couleurs

Trois thèmes livrés, un contraste vérifié (banc WCAG ≥ 4,5:1) :
`EditorThemes.CLAIR`, `EditorThemes.SOMBRE`,
`EditorThemes.CONTRASTE_ELEVE` — posés par `setTheme(EditorTheme)`.

La couleur syntaxique se résout par **pile de portées** TextMate
(H-07), pas par sorte de jeton : les gabarits vivent dans
`ScopeThemes` (`core-syntax`). Votre thème VS Code s'importe tel quel
par `TokenColorsTheme` (`core-language`) — un JSON portant `name`,
`colors` (`editor.background`, `editor.foreground`) et `tokenColors`
(sélecteur `scope`, `settings.foreground`, `settings.fontStyle`) ; la
démonstration en montre un vivant (`themes/atelier.json`). Les
popups se règlent par votre `PopupPalette`, dérivée du thème et de la
taille de texte.

## 13. Budgets de performance attendus

Budgets mesurés sur les bancs de la CI (tas dédié de 1 Go, minimum
trois tours, calibrages consignés dans les bancs — ADR-006/007) :

| Charge | Budget | Mesuré |
| --- | --- | --- |
| Édition, 50 000 lignes | ≤ 1 ms | 0,145 ms |
| Remplacement global et annulation | ≤ 150 ms | 39,8/37,0 ms (passe), 0,85/1,06 ms (annulation) |
| Ré-analyse après 1 caractère, 50 000 lignes | ≤ 10 ms hors fil UI | plancher 1,6 µs (lexicographie) |
| Première image, 100 000 lignes | ≤ 500 ms | 154 ms (texte), 224 ms (analyse par fenêtre) |
| Ouverture, 480 000 lignes (12,1 M caractères) — la fenêtre d'abord | ≤ 2 500 ms | 1214–1441 ms, tas +90 Mo |
| Édition au milieu, 480 000 lignes — l'incrément | ≤ 100 ms | 2–36 ms |
| Frappe jusqu'au pixel (image à 60 Hz) | ≤ 16 ms | fantôme tracé sous 16 ms |

Ce que cela impose à l'hôte : l'analyse vit **hors fil UI**
(ADR-007) — le registre s'assemble sur votre exécuteur (§ 6), la vue
retenue par `setRetenueAnalyseMs` (250 ms par défaut) avant de
re-travailler ; le défilement ne coûte rien parce que la fenêtre
visible seule se note et que le travail en vol s'annule. Ne bloquez
jamais le fil UI pour interroger le modèle ou le serveur — les
contrats rendent des `CompletableFuture`, consommez-les asynchrones.

## 14. Grands documents et coupures

Au-delà des seuils « gros document » du dépôt de départ — **2,5 M
caractères ou 50 000 lignes** — le régime lourd s'arme : la
complétion au curseur se coupe, **l'édition reste entière** —
coloration, frappe et annulation ne se coupent jamais.

```java
import jo.celeris.android.view.LargeFileMode;

editeur.intelligence().setLargeFileMode(LargeFileMode.AUTO);
// AUTO — suit les seuils du document (défaut)
// ON   — force les coupures, même sous les seuils
// OFF  — les ferme, même au-delà (à vos risques et périls de fluidité)
```

Le hub `intelligence()` porte aussi le fournisseur de complétion de
repli (`IntelligenceHub.Provider.builtins()` pour partir,
`setProvider` pour le vôtre).

## 15. L'exécutable en ligne de commande

Le module `cli` prouve la chaîne sans Android — diagnostiquer,
formater, refactorer en scripts de banc ou d'intégration :

```bash
./gradlew :cli:run --args="analyse src/exemple/Main.java"
```

```text
usage : celeris [version | analyse <fichier> | recherche <fichier> <motif> | diagnostiquer <fichier> | formater <fichier> | refactorer <fichier> renommer <ancien> <nouveau>]
```

Codes de sortie : `0` succès · `1` fichier illisible · `2` usage
inconnu · `3` diagnostics trouvés (diagnostiquer) · `4` conflit de
renommage (refactorer). Le jar se construit par
`./gradlew :cli:installDist` — l'exécutable vit dans
`cli/build/install/cli/bin/`.

## 16. Intégrité, provenance et périmètre de l'API

- **Sommes de contrôle** : chaque publication écrit un `.sha256` à
  côté de chaque artefact (jar, sources, Javadoc, POM). Vérifiez ce
  que vous consommez :

```bash
sha256sum core-text-1.1.0.jar
# à comparer au condensé de core-text-1.1.0.jar.sha256
```

- **SBOM** : chaque artefact embarque un SBOM CycloneDX — la
  composition est auditable, les licences au [`NOTICE`](NOTICE), les
  références vérifiées dans [`docs/REFERENCES.md`](docs/REFERENCES.md).
- **Périmètre de l'API** : la source de vérité est le manifeste
  committé `api/<module>.api` — un ensemble **fermé et listé**
  (ADR-009), régénéré par `./gradlew apiManifest` et contrôlé par
  `apiCheck` à chaque build du dépôt. Si un nom cité dans ce guide
  diverge un jour du manifeste, c'est le manifeste qui dit vrai.
- **Provenance** : les dépendances du dépôt sont épinglées par
  catalogue et balayées par le contrôle de provenance — aucun
  artefact d'organisation interdite n'entre dans le build.

## 17. Vie privée et sécurité

- **Zéro télémétrie** dans les paquets celeris — ni collecte, ni
  déclaration d'usage ; la démonstration ne déclare aucune permission
  réseau, sa neutralité est structurelle et vérifiée en CI.
- **Zéro transport IA** dans le cœur (§ 11) — votre politique de
  consentement commande, la garde exécute.
- **Signalement des vulnérabilités** : par courriel à l'adresse du
  [`SECURITY.md`](SECURITY.md), jamais d'issue publique — versions
  prises en charge et délais annoncés s'y lisent.
- **Serveurs de langage** : l'éditeur n'en monte aucun de lui-même —
  vos déclarations décident, vos processus restent les vôtres.

## 18. Ce qui manque encore

État vérifié au 2026-11-11 (la liste tenue à jour vit dans
[`docs/feuille-de-route-restante.md`](docs/feuille-de-route-restante.md)) :

- **Publication distante** (C1, clos le 2026-11-12) : les douze artefacts vivent
  au registre de l'hébergeur — le chantier de la 1.2.0 le consigne.

## 19. Pièges d'intégration

1. **La vue sans session ne dit rien** — `setSession` d'abord ; un
   `EditorView` sans session n'affiche qu'un document vide immuable.
2. **Les positions sont des offsets UTF-16** (`int`), pas des octets
   ni des code points — `Document.lineForOffset` fait le pont vers
   les lignes, jamais l'inverse.
3. **Les rangées visuelles ne s'exposent pas** : le retour à la ligne
   visuel, l'accessibilité et la projection passent par la vue et
   l'arbre — ne calculez pas de « ligne affichée » vous-même.
4. **Le registre s'assemble hors fil UI** (ADR-007) : au premier
   dégelui, la lecture des paquets et la compilation des grammaires
   paient — préchargez comme la démonstration.
5. **Un serveur embarqué lance en processus** : il partage le tas de
   votre application — surveillez sa mémoire, et sous R8 gardez les
   règles de conservation du protocole
   (`lsp-protocole/regles-conservation.pro`, cf. la démonstration) ;
   le désucrage se règle côté hôte pour les bibliothèques Java 11.
6. **Fermez ce que vous montez** : `LspSession`,
   `ServeurEmbarque`, `TubesMemoire.Direction` sont `AutoCloseable` —
   la fuite d'un serveur en processus se paie en tas.
7. **Le consentement se relit à chaque départ** (F-10) : un
   `BooleanSupplier` capturé une fois pour toutes est un bug de
   politique — la démonstration relit sa préférence à chaque demande.
8. **Aucune version en dur** côté dépendances de celeris si vous
   reprenez les sources : le catalogue `gradle/libs.versions.toml`
   du dépôt est la référence (ADR-011).
9. **Les modules Android exigent un SDK** : sans `ANDROID_HOME`, le
   build du dépôt n'inclut que les modules purs — vos inclusions de
   source (§ 3.3) suivent la même règle ; la variable
   `CELERIS_SKIP_ANDROID=1` force le refus.

---

Pour la migration depuis le dépôt de départ, suivez
[`MIGRATION.md`](MIGRATION.md) ; pour l'architecture et les règles de
dépendance, [`ARCHITECTURE.md`](ARCHITECTURE.md) ; pour les décisions,
la série des ADR dans [`docs/adr/`](docs/adr/).
