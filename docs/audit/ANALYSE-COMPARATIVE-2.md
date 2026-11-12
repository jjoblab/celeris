# Analyse comparative 2 — celeris face à Android Studio (2026-11-12)

> **Question posée** : la reprise du propriétaire du 2026-11-12 a
> redressé cinq écarts constatés à la revue comparative — sont-ils bien
> couverts ? Et au-delà de ces cinq-là, quels gaps restent entre
> l'éditeur embarquable celeris et l'éditeur de l'environnement de
> référence, Android Studio ?
>
> **Méthode** : lecture du code au commit `6108228` (branche `main`) ;
> chaque reprise vérifiée à son banc nommé (les tests sont comptés au
> `rg -c "@Test"`, rejouables) ; la grille de
> [`docs/parite/MATRICE-PARITE.md`](../parite/MATRICE-PARITE.md) — dont
> l'étalon EST Android Studio (125 lignes) — recomptée ligne à ligne ;
> les faits d'Android Studio relevés dans sa documentation publique
> (Annexe B), recoupés avec l'état vérifié au 2026-11-04 par la matrice
> (ligne stable 2026.2.2). Aucun chiffre saisi à la main : tout est
> mesuré au dépôt ou sourcé.
>
> **Auteur** : jjoblab (contributeur unique). **Comparaison précédente**
> : [`ANALYSE-COMPARATIVE.md`](ANALYSE-COMPARATIVE.md) (celeris contre
> le dépôt de départ, 2026-11-10) — la présente note ouvre la fenêtre
> sur l'étalon lui-même.

---

## 1. Résumé exécutif

**Réponse courte : oui — les cinq écarts sont couverts, chacun prouvé
par un banc vert qui tourne en CI ; et la grille face à Android Studio
n'a plus aucune ligne P0 ouverte : 119 lignes couvertes sur 125, les six
restantes sont toutes P2.** La reprise a fait plus que réparer : elle a
transformé des déclarations de la matrice (« Câblé », « Démontré ») en
faits vérifiables — l'icône de pli répondait au doigt sur le papier,
elle y répond dans le banc.

La nuance qui reste : la parité **capacité par capacité** n'est pas la
parité **d'expérience**. Android Studio est un environnement complet —
projet multi-fichiers, compilation, exécution, débogage réel, index de
projet, intégration VCS entière — quand celeris est une bibliothèque
d'éditeur embarquable, tactile d'abord. Les écarts de posture sont
assumés (§ 4) ; les écarts de capacité restants sont listés (§ 3) et
sont peu nombreux, secondaires et datables.

### Verdict par dimension

| Dimension | Verdict |
| --- | --- |
| Les cinq écarts de la reprise (gouttière, minimap, toolbar, Entrée, serveur LSP) | **Couverts — bancs verts** |
| Lignes P0 de la matrice Android Studio | **Zéro ouverte (34/34 couvertes)** |
| Lignes P1 | **Zéro ouverte** |
| Lignes P2 | 6 ouvertes (§ 3) — aucune bloquante |
| Popups : moments d'ouverture/fermeture | **Alignés sur l'étalon** (complétion au préfixe, survol à l'appui long, signature dans l'appel, diagnostics poussés à l'ouverture) |
| Gouttière de plis | **Équivalente et tactile** (chevron permanent, tap ; l'étalon affiche ses chevrons au survol par défaut) |
| Minimap | **Supérieure à l'étalon** (Android Studio n'en a pas ; celeris la rend colorée) |
| Index de projet, navigation multi-fichiers, exécution, débogage réel | **Hors périmètre assumé** (§ 4) — l'hôte en apporte ce qu'il veut par le SPI |
| Charge | **Tenue** : 480 000 lignes ouvrables, incrément 2–36 ms (§ Annexe A) |

---

## 2. Les cinq écarts de la reprise — couverts ?

### 2.1 L'icône de pli répond au doigt (gouttière)

**Constat d'origine** : le chevron de pli de la coulisse ne basculait
pas au tap — la garde de `basculerPliAuPoint` ne vivait que pour
l'analyse synchrone, et le repli changeait le sens des rangées sans
invalider le cache de mise en page (PIEGE-02 : le pli ne change pas la
révision du document).

**Correctif** (commit `3e947a0`) : la garde tient pour TOUTE analyse —
`!active() || x >= coulisse` ; le tap qui plie vide le cache de
rangées avant d'invalider l'image.

**Preuve** : `PliAuTapTest` (android-view, **3 tests**) — le chevron
répond au tap ; le texte sous le pli se réhabilite au replié (le cache
se vide, PIEGE-02) ; le tap hors coulisse ne plie pas. Vert en CI à
chaque poussée (le travail `Tests de vue`).

**Face à l'étalon** : Android Studio affiche ses chevrons de pli dans
la gouttière (au survol par défaut, « Always » en option — Annexe B) ;
celeris affiche le chevron en permanence et bascule au tap — le geste
suit le support (souris/souris, doigt/tactile).

### 2.2 La minimap se rend colorée

**Constat d'origine** : la silhouette vivait en monochrome — la
charge seule se voyait, pas la matière du code.

**Correctif** (commit `023090ec`) : `MinimapModel` découpe chaque
rangée en segments encrés — code, chaînes, commentaires, nombres,
mots-clés (`ENCRE_CODE` à `ENCRE_MOTCLE`) — la rangée porte la charge
de son bouquet ET les segments de sa ligne représentative ; la
densité reste bornée par construction (une rangée par cran).

**Preuve** : `MinimapModelTest` (android-widgets, **14 tests**) —
charge, crans, segments encrés, bornage. Le saut au toucher vit dans
`ScrollFractionTest` (D-21/D-22, chantier C9).

**Face à l'étalon** : Android Studio n'a **pas** de minimap native —
c'est un avantage de celeris sur l'étalon, la fidélité au design du
dépôt de départ. (La vieille analyse du 2026-11-10 constatait
l'absence de minimap chez celeris ; l'écart s'est retourné.)

### 2.3 La toolbar porte son débordement au design du kit

**Constat d'origine** : les actions repliées vivaient au menu système
de la toolbar ; la barre de touches de code n'existait pas.

**Correctif** (commits `fb50c399`, `a0f926a`) : le menu de débordement
vit dans la **carte du kit** (`IntentionsMenuView` posée sur la
couche, rangées cliquables, fermeture avant exécution — jamais le
menu système) ; la **barre de touches** de code vit au-dessus du
clavier (`TouchBarView` : Tab, commentaire, déplacer la ligne,
dupliquer — états vivants suivis au caret). Les identifiants des
actions restent déclarés en ressources (`ids.xml`), la parité
fr/ défaut se vérifie en CI.

**Preuve** : `TouchBarViewTest` (android-widgets, **4 tests**) ; le
scénario H-13 du banc de la démo traverse la carte ; la politique de
langue de la CI vérifie la parité des chaînes (relevée par la
présente reprise : le contrôle s'est raffiné aux noms).

**Face à l'étalon** : Android Studio replie ses actions dans les menus
système et son propre cadre ; celeris suit le design du dépôt de
départ — la carte partagée du kit, tout en popups (ADR-018).

### 2.4 L'Entrée entre accolades rend le repli textuel

**Constat d'origine** : entre `{}[]`, l'Entrée plaçait le curseur à la
fin de la ligne ouverte — pas les deux sauts de l'éditeur de référence.

**Correctif** (commit `b774f2c1`) : `SmartTyping.enter` tient le repli
**textuel** même sans arbre d'analyse — `{⏎}` devient deux lignes
indentées, le curseur au milieu ; l'annulation défait le geste entier.

**Preuve** : `EntreeRepliTextuelTest` (core-intelligence, **10
tests**).

**Face à l'étalon** : l'éditeur d'Android Studio fait le double saut à
l'Entrée entre accolades (comportement IntelliJ historique) ;
l'équivalence est atteinte, par la voie textuelle quand l'arbre manque.

### 2.5 Le serveur Java déclaré s'éprouve entier — les popups au bon moment

**Constat d'origine** : les popups du serveur déclaré ne s'ouvraient
pas aux bons moments — survol null au point d'usage, diagnostics
poussés vides.

**Correctif** (commits `8e6727e0`, `a1b88c4`) : `JavaSignature` (le
paramètre courant se compte aux virgules de premier niveau, « new »
répond par son constructeur, l'en-tête d'une déclaration n'est pas un
appel) ; `JavaHover` sert le mot-clé ET le symbole — au nom déclaré
comme au **point d'usage** (la ligne de la déclaration citée) ;
l'appui long de la vue notifie l'hôte (`setSurAppuiLong`), la sélection
déjà posée.

**Preuve** (48 tests au total) : `JavaSignatureTest` (**8**),
`JavaHoverTest` (**5**), `ServeurDeclareMomentsTest` (démo, **4** —
la signature naît dans l'appel et meurt à la sortie ; le survol vit
au mot, meurt au blanc et au signe ; les diagnostics se poussent à
l'ouverture puis au changement ; la complétion répond au préfixe,
pas au blanc), le scénario H-13 de `BancSpecs` (le serveur déclaré
sert chaque capacité annoncée) et `DeclaredServersTest`.

**Face à l'étalon** : Android Studio montre l'aide aux paramètres
dans l'appel (`Ctrl+P`), la documentation au caret (`Ctrl+Q`), les
diagnostics au fil de l'analyse — celeris suit les mêmes moments,
adaptés au tactile : la complétition naît au préfixe tapé, le survol à
l'appui long, la signature quand le caret vit dans l'appel, les
diagnostics poussés à l'ouverture et au changement (Annexe B pour
les faits de l'étalon).

### 2.6 Tableau récapitulatif

| Écart redressé | Correctif | Banc | Tests |
| --- | --- | --- | --- |
| Chevron de pli inerte au tap (PIEGE-02) | `3e947a0` | `PliAuTapTest` | 3 |
| Minimap monochrome | `023090ec` | `MinimapModelTest` | 14 |
| Débordement au menu système, pas de barre de touches | `fb50c399`, `a0f926a` | `TouchBarViewTest` (+ H-13) | 4 |
| Entrée entre accolades mal placée | `b774f2c1` | `EntreeRepliTextuelTest` | 10 |
| Popups du serveur hors moment | `8e6727e0`, `a1b88c4` | `JavaSignatureTest`, `JavaHoverTest`, `ServeurDeclareMomentsTest`, `DeclaredServersTest`, H-13 | 17 + scénario |
| (porteur) qualité/plafonds du CI rouge | `ad2fdd6` | les trois travaux de la CI | verts |

---

## 3. La grille face à Android Studio — recomptée

La matrice de parité (dont **l'étalon est Android Studio**, état
vérifié le 2026-11-04, ligne stable 2026.2.2) compte 125 lignes
catégories A à H. Recomptage du 2026-11-12 :

| Statut celeris | Lignes |
| --- | --- |
| Câblé | 60 |
| Noyau | 46 |
| Démontré | 13 |
| **Couvertes** | **119 / 125** |
| Ouvertes (toutes P2) | 6 |

**Aucune ligne P0 ou P1 n'est ouverte.** Les six restantes :

| Ligne | Capacité | Priorité |
| --- | --- | --- |
| A-26 | Fichiers binaires : détection, aperçu sûr, refus d'édition destructive | P2 |
| B-12 | Hiérarchies de types et d'appels | P2 |
| B-18 | Aperçu d'un correctif (diff avant application) | P2 |
| C-10 | Compose : documentation des modificateurs au survol | P2 |
| C-13 | Extraction d'une chaîne en ressource | P2 |
| H-09 | Analyse structurelle niveau 2 (tree-sitter natif) | P2 |

Nuance honnête : sept lignes « Noyau » ne citent pas encore de banc
dédié (A-24 encodages, B-10 navigation LSP, B-13 aller-au-symbole,
B-17 inspections, C-14/C-16 langages P0/P2, G-02 budgets globaux) — la
capacité vit au cœur, la preuve dédiée reste à naître ; c'est la
première marche des recommandations (§ 5). La traîne des langages
suit son signal : vingt-quatre langages intégrés (Go et Rust livrés
le 2026-11-12), le reste du dépôt de départ (vingt-sept) dort sur la
décision du propriétaire (chantier C5).

---

## 4. Les écarts de posture — assumés, pas des gaps

Une comparaison honnête avec Android Studio doit distinguer ce qui
**manque** de ce que celeris **ne vise pas** :

- **Un IDE contre un composant**. Android Studio compile, exécute,
  débogue pour de vrai, indexe le projet entier ; celeris est un
  éditeur embarquable — l'hôte apporte l'exécution, le débogage et
  les fichiers par le SPI (`android-host`), la démonstration les
  prouve sur des références pures. Comparer le moteur d'éditeur à
  l'IDE entière serait un duel d'appareils de poids différents : la
  matrice compare les **capacités d'éditeur**, et c'est là que la
  parité P0/P1 est atteinte.
- **Tactile d'abord**. L'étalon est clavier/souris (survol au
  pointeur, menus au clic droit, raccourcis à deux mains) ; celeris
  pense doigt d'abord — pilule « ajouter un curseur », poignées de
  sélection, pincement zoom, loupe, appui long qui sélectionne le mot
  puis sert la documentation, barre de touches au-dessus du clavier
  logiciel. Le clavier matériel reste servi (D-17, table remappable).
- **Hors ligne, sans télémétrie**. L'étalon télécharge index, SDK et
  plugiciels ; celeris ne déclare aucune permission réseau — sa
  neutralité est structurelle, vérifiée en CI.
- **La minimap** : l'étalon n'en a pas ; celeris la rend colorée —
  fidèle au dépôt de départ, avantage assumé sur l'étalon.
- **Tree-sitter niveau 2** (H-09, P2 ouvert) : l'étalon a son PSI ;
  celeris vit sur son arbre tolérant maison + les grammaires
  TextMate du commerce — la voie niveau 2 reste tracée, non entamée.

---

## 5. Recommandations — les prochaines marches

1. **Fiabiliser les « Noyau » sans banc dédié** (A-24, B-10, B-13,
   B-17, C-14, C-16, G-02) : un banc nommé par ligne fait passer la
   preuve du « ça marche » au « ça se rejoue » — la règle d'or de la
   matrice.
2. **Les six P2, par valeur** : B-18 (aperçu d'un correctif avant
   application) prolonge naturellement C-12 ; A-26 (fichiers binaires)
   boucle la sécurité d'ouverture ; C-13 (extraction de ressource)
   rejoindrait l'atelier de refactorisation ; B-12 et C-10 suivront
   les besoins réels des hôtes ; H-09 (tree-sitter 2) reste le
   chantier de fond le plus lourd — à ne commencer que sur signal.
3. **La traîne des langages** dort sur le signal du propriétaire
   (vingt-sept langages du dépôt de départ restants) : chaque langue
   est un paquet de données + un banc de conformité, le coût marginal
   est documenté.
4. **Tenir les moments** : les quatre moments de popup (complétion,
   survol, signature, diagnostics) ont maintenant leurs bancs — les
   garder verts à chaque reprise du kit, c'est la discipline qui
   évite la prochaine « reprise ».

---

## Annexe A — Mesures au 2026-11-12

- **Compteurs** : 341 bancs JVM et 2 161 méthodes `@Test` (les
  quatorze modules compris), plus quatre bancs d'appareil ; jaCoCo
  ≥ 90 % de lignes sur les modules purs, mutation ≥ 80 % sur
  `core-text` ; Checkstyle/PMD bloquants (600 lignes par fichier,
  50 par méthode, complexité 15) ; manifestes d'API vérifiés à chaque
  `check`.
- **Charge** (bancs de la CI, tas dédié de 1 Go, minimum trois
  tours) : édition 50 000 lignes 0,145 ms (≤ 1 ms) ; première image
  100 000 lignes 154 ms texte (≤ 500 ms) ; ouverture 480 000 lignes
  1 214–1 441 ms (≤ 2 500 ms) ; édition au milieu 2–36 ms
  (≤ 100 ms).
- **Reprises** : les cinq écarts du § 2, preuves comptées aux bancs
  (48 tests de preuve au total, la table du § 2.6).

## Annexe B — Sources Android Studio / IntelliJ (facts de l'étalon)

- Gouttière et chevrons de pli (affichés au survol par défaut,
  « Always » en option) : page « Editor gutter », documentation
  JetBrains IDEA —
  <https://www.jetbrains.com/help/idea/editor-gutter.html> et
  « Code Folding » —
  <https://www.jetbrains.com/help/idea/code-folding-settings.html>.
- Aide aux paramètres dans l'appel (`Ctrl+P`), documentation au
  caret (`Ctrl+Q`) : « Keyboard shortcuts », Android Studio
  developers —
  <https://developer.android.com/studio/intro/keyboard-shortcuts> ;
  « Code reference information » (popup Parameter Info) —
  <https://www.jetbrains.com/help/idea/viewing-reference-information.html>.
- Complétion de base (noms de classes, méthodes, champs, mots-clés à
  portée) : « Code completion » —
  <https://www.jetbrains.com/help/idea/auto-completing-code.html>.
- Bande d'erreurs intégrée à la barre de défilement (marques
  d'erreurs/alertes) : « Error stripe and status indicator » —
  <https://www.jetbrains.com/help/rider/Code_Analysis__Status_Indicator.html>.
- Multi-curseurs et plages de sélection non contiguës :
  « Multiple cursors and selection ranges » —
  <https://www.jetbrains.com/help/idea/multicursor.html>.
- Lignes collantes (en-têtes englobants au défilement) :
  « Sticky lines » —
  <https://www.jetbrains.com/help/idea/sticky-lines.html>.
- État de l'étalon vérifié par la matrice le 2026-11-04 (ligne
  stable 2026.2.2) : en-tête de
  [`docs/parite/MATRICE-PARITE.md`](../parite/MATRICE-PARITE.md).

## Annexe C — Les commits de la reprise

`3e947a0` (l'icône de pli répond au doigt, C-1 de la reprise) ;
`b774f2c1` (l'Entrée sans arbre rend le repli textuel) ;
`8e6727e0` (le serveur Java déclaré s'éprouve entier — signature et
survol) ; `a1b88c4` (le bon moment des popups se prouve à la
source) ; `023090ec` (la minimap se rend colorée) ; `fb50c399`
(la toolbar porte son débordement au design du kit, la barre de
touches vit au-dessus du clavier) ; `a0f926a` (l'apostrophe
française s'échappe aux ressources) ; `ad2fdd6` (la porte verte se
referme — parité aux noms, plafonds repris, manifeste committé) ;
`6108228` (les READMEs relisent la vérité).
