# Résumé M8.5 — jalon de redressement (2026-11-08)

> **Déclenchement** : le constat du propriétaire dans la démonstration —
> « le design des popups est affreux, tu dois utiliser les mêmes du
> dépôt de départ, toutes » et « je ne peux pas défiler horizontal et
> vertical sans que le clavier ne devienne visible ». Périmètre : le
> design du kit de popups du dépôt de départ porté sur toutes les
> surfaces de la couche, et la discipline tactile du dépôt de départ
> reprise — le geste décide, le doigt posé n'arme rien.
>
> **État** : les DEUX redressements sont livrés, `./gradlew check`
> vert sur l'ensemble (politique comprise — la ligne de base de langue
> tient à l'identique, 408 identifiants figés).

## Le design du kit de popups — le portage intégral

La source est le kit du dépôt de départ (`view/popup/kit/`) : jetons,
carte verre, badges, rendus. Chaque valeur est
portée telle quelle, pas réinterprétée.

- **`PopupTokens`** (nouveau) : les jetons d'origine — rayons 14 dp
  (carte), 8 dp (rangée pressée), 6 dp (puce) ; respirations 12/8 dp,
  écart 6 dp ; échelle typographique 0,90/0,85/0,75/0,70 × la taille
  de texte de l'éditeur ; plafond de largeur de liste 280 dp, rangée
  de complétion 40 dp, badge au plus 20 dp. Un seul endroit où changer
  l'apparence d'une carte flottante.
- **`PopupPalette`** : le VERRE se dérive désormais du thème de l'hôte
  par `verre(...)` — fond de l'éditeur à alpha 0xDB (thème sombre) ou
  0xE0 (clair), liseré hairline blanc à 10 % (sombre) ou noir à 8 %
  (clair), le calcul exact du constructeur de thème du dépôt de
  départ. Les encres sémantiques suivent : atténué (gouttière),
  teinte de sélection (bandes douces), accent (runs de match,
  paramètre actif), mot-clé (libellés de candidats), et les TROIS
  gravités (erreur, avertissement, information) aux valeurs de
  référence du dépôt de départ. La palette porte la base
  typographique en sp — l'échelle du kit s'y multiplie.
- **`PopupPanel`** : la carte du dépôt de départ — rayon 14 dp, liseré
  d'UN pixel (pas un pixel-densité), contenu clippé au chemin arrondi
  (la bande de sélection d'une rangée ne déborde jamais sur les
  coins), filets de séparation et bandes teintées à la teinte de
  sélection. Le rafraîchissement par `setPalette` : le verre suit le
  thème vivant de l'éditeur.
- **`TextRowsPopup`** : la rangée typée du kit — un palier
  typographique par rangée (titre/corps/méta/micro), bande teintée
  douce (le motif signature), filet de tête, indentation PARAM (8 dp).
- **`CompletionListView`** : les RANGÉES DEUX LIGNES du dépôt de
  départ — badge de genre en puce arrondie 6 dp teintée à 20 %
  d'alpha, glyphe semi-gras à l'encre du genre (`BadgeGenre`, la
  palette calibrée du dépôt de départ, assombrie à 38 % sur thème
  clair) ; libellé sur la première ligne, les caractères du préfixe
  tapé (sous-séquence, cas ignorée) à l'accent et au gras ; identité
  qualifiée SOUS le libellé, en atténué ; candidat courant surligné de
  la teinte de sélection, encadré à un pixel, sous le clip arrondi ;
  largeur plafonnée à 280 dp, ellipses en queue.
- **`CompletionDocView`** : les TROIS BLOCS du panneau de
  documentation — bande signature teintée à l'encre d'accent (le
  titre, jamais fondu — PIEGE-16), libellés de genre en
  micro-typographie atténuée ouverts d'un filet, lignes PARAM
  indentées à l'encre des mots-clés, RETOUR en gras atténué, résumé au
  corps.
- **`SignatureHelpView`** : le rendu à TROIS PALIERS — paramètres déjà
  frappés à l'accent, paramètre ACTIF à l'accent et au gras, à venir
  en atténué ; la structure garde l'encre du texte jusqu'à l'actif
  puis s'atténue avec la queue (la ligne devient sa propre barre de
  progression). La découpe des plages de paramètres (`plagesParametres`,
  virgules de profondeur nulle, bords blancs exclus) est le portage
  exact, testé y compris sur les imbrications.
- **`DiagnosticTooltipView`** : le marqueur de gravité (× à l'encre
  d'erreur, ! à l'encre d'avertissement) au palier titre précède le
  message à l'encre du texte — la sévérité se lit au marqueur, comme
  la fiche du dépôt de départ.
- **`IntentionsMenuView`** : les actions au palier titre.
- **`BreadcrumbView`** : le palier méta, à l'encre atténuée.
- **`ErrorBandView`** : le TIRET du dépôt de départ — marque de 4 dp
  posée au bord droit, bande tactile de 10 dp, marque de 3 dp de haut
  au moins, et la marque COLORE À LA GRAVITÉ (erreur, avertissement,
  information) : `ErrorBandModel` porte désormais la gravité de chaque
  marque (`Marque.gravite`, l'ancienne voie tient l'erreur).
- **`SelectionBarView`** : la pastille pressée se surligne de la teinte
  de sélection (rayon rangée), les séparateurs restent des filets
  hairline.

La démonstration suit : l'atelier dérive sa palette du thème d'éditeur
COURANT (`PopupPalette.verre(theme...)` — fond, encres, teinte de
sélection, curseur pour l'accent, mot-clé de la palette syntaxique),
le verre suit la rotation des thèmes (rappel de changement chez
`BasculeTheme`) et la taille de texte de l'éditeur (le zoom
re-taille les popups). Le coach sert les candidats TYPÉS — genre de
badge (mot réservé, méthode documentée, « String » en classe,
  identifiant en variable), signature en identité qualifiée — et le
préfixe tapé pour les runs de match ; la bande d'erreurs reçoit les
gravités des diagnostics de l'hôte.

Preuve exécutée : `PopupDesignTest` (le verre, les jetons, les badges,
les plages de signature, les gravités — les valeurs du dépôt de
départ figées par contrat), `PopupRenduTest` (rendu natif : la carte
pose son verre, la rangée courante sa teinte de sélection, lues au
pixel), `PopupViewsTest`/`PopupSurfacesTest` aux nouveaux contrats.

## Le clavier du défilement — la discipline tactile du dépôt de départ

**Cause confirmée** : `TouchesEditeur.surAppui` (le DOWN) appelait
`CurseurClavier.surAppui()` — focus + `showSoftInput` — AVANT que le
geste ne parle. Toute lecture au doigt (panoramique à deux doigts
compris) réveillait l'IME ; et le glissement à UN doigt ne défilait
rien du tout (aucune voie n'existait hors le panoramique).

Le modèle du dépôt de départ reprend ses droits :

- le **DOWN** ne retient que la poignée visée (son tirage commence au
  doigt posé) ; pilule, plis, marques, curseur et clavier ATTENDENT ;
- le **MOVE** au-delà du seuil de tap (24 px, la valeur du dépôt de
  départ) engage le défilement à un doigt — les DEUX axes, borné par
  la géométrie de défilement, la pilule se congédie, l'IME ne se lève
  jamais ; l'appui long armé garde la main au dragage de sélection ;
- l'**UP** sous le seuil est un TAP : la cascade résout (pilule → pli
  → repère → marque → pose du curseur + focus + clavier) — lui seul
  ouvre l'IME ; le relâchement de l'appui long ne rouvre rien ;
- un geste non conclu ne survit pas à un nouvel appui (l'armement long
  se referme) ; le DOWN englouti par le texte fantôme ne résout jamais
  de tap (l'UP sans DOWN vu ne touche à rien) ; CANCEL conclut sans
  résoudre.

Preuve exécutée : `DefilementDoigtViewTest` (banc réel ADR-013) — le
panoramique et les glissements vertical/horizontal n'armaient pas le
clavier AVANT le correctif et défilent APRÈS ; le tap seul ouvre
l'IME et pose le curseur au point touché ; le jitter sous le seuil
reste un tap ; le relâchement de l'appui long garde l'IME caché et la
sélection vivante. Les bancs existants (sélection par appui long,
pincement, frappe) restent verts sans retouche.

## Hygiène

- L'environnement de la session a réinstallé son JDK (Temurin 21) et
  le SDK Android (plateforme 36, build-tools 36.0.0) hors du dépôt ;
  Maven Central limitant le téléchargement des gros artefacts par IP,
  le `android-all` de Robolectric est mis en cache local (`~/.m2`)
  depuis un miroir public — rien de tout cela ne touche au dépôt.
- La ligne de base de langue tient à l'identique (408 identifiants
  figés) : les nouveaux identifiants sont en anglais (`Tier`,
  `BadgeGenre`, `PopupTokens`...), la dette existante ne bouge pas.
