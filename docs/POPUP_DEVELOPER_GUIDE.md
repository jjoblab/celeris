# Guide du développeur — ajouter un popup à Celeris

> La phase 9 de la mission — « documentation » — servira ici la promesse
> du mandat : expliquer comment un développeur ajoute un popup SANS
> modifier inutilement le `PopupManager`. Le manager ne bouge JAMAIS
> pour une surface nouvelle ; seule la machine à états peut gagner un
> genre (et encore : seulement si la surface vit de règles de
> coexistence nouvelles). Le montage tient en trois temps — lier,
> déclencher, déléguer (USAGE §9) ; l'inventaire des surfaces existantes
> vit dans [`POPUP_INVENTORY.md`](POPUP_INVENTORY.md), l'architecture
> qui a produit ce chemin dans
> [`POPUP_SYSTEM_ARCHITECTURE.md`](POPUP_SYSTEM_ARCHITECTURE.md), le
> pourquoi des déficits corrigés dans
> [`POPUP_SYSTEM_AUDIT.md`](POPUP_SYSTEM_AUDIT.md).

## 1. Les règles du jeu (à lire avant la première ligne)

1. **PIEGE-27** : une carte de la couche ne prend JAMAIS le focus, ne
   demande jamais le focus, n'utilise jamais `PopupWindow`. Le focus
   reste à l'éditeur ; la navigation clavier arrive par DÉLÉGATION
   (`PopupManager.surTouche` branché par `EditorView.setSurTouchePopup`).
2. **ADR-018** : toute surface fonctionnelle est une popup — carte
   ancrée, feuille du bas, feuille du haut. Jamais d'écran plein cadre,
   jamais de dialogue système. Une surface DENSE (recherche, plan,
   réglages) vit dans une feuille ; une surface PONCTUELLE vit en carte.
3. **La frontière des responsabilités** : la carte ne connaît NI les
   moteurs (LSP, index), NI l'éditeur. Le contenu arrive par
   `setXxx(...)` sur la vue ; c'est l'hôte (l'atelier) qui sert
   d'intermédiaire entre le moteur et la carte. Le kit ne connaît ni
   l'un ni l'autre.
4. **Une vue par genre, un genre par vue** : `lier` le refuse dès le
   câblage (l'audit P1 : une carte partagée fait mentir l'état).
5. **LA mesure unique** : `popups.taille(genre)` rend la carte ENTIÈRE
   (contenu + respirations) — vos ancres s'y prennent, la couche
   re-mesure à sa pose (arithmétique, la carte n'y re-naît pas).
6. **La vue ne survit jamais à son état** : tout ce qui ferme (frappe,
   défilement, exclusion, cascade, voile, tap hors cartes) retire les
   cartes d'un seul tenant — l'hôte ne recode JAMAIS les cascades.

## 2. Le montage en trois temps (le rappel de l'USAGE §9)

```java
// le couple de toujours : la machine et la couche, posées par l'hôte
PopupManager popups = new PopupManager(new PopupController(), couche);

// LIER — une carte par genre, au montage
popups.lier(PopupController.PopupKind.COMPLETION_LIST, liste);

// DÉCLENCHER — l'état s'ouvre, les exclus quittent la couche, la carte se borne
popups.montrer(PopupController.PopupKind.COMPLETION_LIST, x, y);

// DÉLÉGUER — l'éditeur garde le focus, le manager sert les touches
editeur.setSurTouchePopup(popups::surTouche);
```

Les régimes suivent seuls : `popups.surFrappe()` (la frappe referme les
bulles, la liste filtre), `popups.surDefilement()`, la feuille modale
qui s'ouvre appelle `popups.fermerSousVoile()`, la fin de vie appelle
`popups.detacher()`. Fermer : `popups.fermer(genre)` — la cascade
comprise (la documentation suit SA liste, le débordement suit SA barre).

## 3. La recette — huit pas, un genre nouveau

Le fil rouge : une carte d'**info de type** au caret (catégorie A de
l'inventaire, manquante aujourd'hui). Illustration pédagogique — la
surface n'existe pas au dépôt ; le chemin, lui, est celui que suivent
toutes les surfaces réelles du dépôt.

### Pas 1 — le genre, à la machine à états

La SEULE évolution du kit qu'un genre nouveau peut exiger : une
entrée d'énumération et, si la surface vit de règles NOUVELLES, ses
régimes. Pour une bulle qui se comporte comme le survol (fermée à la
frappe et au défilement, refermée au tap hors cartes), le régime
EXISTE déjà — l'énumération suffit :

```java
// PopupController.PopupKind — la javadoc du régime se pose avec le genre
/** L'info de type au caret — fermée à la frappe et au défilement,
 *  comme le survol : elle décrit, elle ne choisit pas. */
TYPE_INFO,
```

Les régimes sont des LISTES EXPLICITES dans la machine — le genre
nouveau qui doit céder à la frappe s'y inscrit (`fermer(...)` dans
`onKeystroke()`, `onScroll()`, `surToucheHorsCartes()`), celui qui
doit exclure s'y déclare (`fermerParExclusion` dans
`ouvrirRendus()`). CHAQUE inscription se prouve au banc
(`PopupControllerTest`) ; le manager ne sait rien des règles, il les
exécute. Si votre surface n'a AUCUNE règle nouvelle : une feuille
(`FeuilleBas`/`FeuilleHaut`) ou le chrome de l'hôte — pas de genre du
tout.

### Pas 2 — la carte, dans le kit (ou chez vous)

Deux patrons réels du dépôt : `PopupPanel` (la carte verre libre —
`SignatureHelpView`, `ColorPickerView`) et `TextRowsPopup` (les rangées
typées avec surbrillance et pick tactile — `DiagnosticTooltipView`,
`IntentionsMenuView`). Une carte du kit : Javadoc de classe publique en
français, identifiants anglais ASCII, la palette reçue au constructeur
:

```java
/** La carte d'info de type — le nom du type du symbole au caret. */
public final class TypeInfoView extends TextRowsPopup {

    /** Construit la carte sur la palette du thème courant. */
    public TypeInfoView(Context context, PopupPalette palette) {
        super(context, palette);
    }

    /** Pose le type du symbole — la vue ne demande JAMAIS au moteur. */
    public final void setType(String nomDuType) {
        // setRows(...) — les rangées typées du patron (protégé aux dérivées)
    }
}
```

Le rendu commun vient avec la base : verre, liseré hairline, coins
arrondis, clip, voile de naissance et de mort — vous ne peignez pas la
carte, vous peignez DANS la carte. Le thème se tourne par
`setPalette(...)` (`PopupPanel`), la démo le fait à chaque changement
(`AtelierPopups.surPalette`).

### Pas 3 — le lien

Au montage de l'atelier, jamais au déclenchement :

```java
TypeInfoView typeInfo = new TypeInfoView(couche.getContext(), palette);
popups.lier(PopupController.PopupKind.TYPE_INFO, typeInfo);
```

### Pas 4 — le déclenchement et les données (l'exemple complet réel)

L'exemple COMPLET le plus court du dépôt : l'atelier du sélecteur de
couleur (`demo/src/main/java/jo/celeris/demo/AtelierCouleur.java`,
129 lignes du constructeur au pick) — lier au montage, servir la carte
au geste, ancrer avec LA mesure, montrer, fermer, appliquer :

```java
// le constructeur : le lien une fois pour toutes
this.selecteur = new ColorPickerView(popups.couche().getContext(), palette);
popups.lier(PopupController.PopupKind.COLOR_PICKER, selecteur);
selecteur.setOnAppliquer(this::appliquer);

// le déclenchement : le geste sert la carte, l'ancre prend LA mesure
selecteur.ouvrir(pastille.argb(), chiffres);
Rect curseur = editeur.caretBounds();
PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
        curseur.left, curseur.bottom, taille(),
        popups.couche().getWidth(), popups.couche().getHeight(), margePx);
popups.montrer(PopupController.PopupKind.COLOR_PICKER,
        placee.x(), placee.y());

// la fin : fermer d'un appel (le tap hors cartes ferme AUSSI, tout seul)
popups.fermer(PopupController.PopupKind.COLOR_PICKER);

// LA mesure — jamais la vôtre
private PopupPlacer.Cadre taille() {
    return popups.taille(PopupController.PopupKind.COLOR_PICKER);
}
```

### Pas 5 — l'ancre

Les six ancres bornées du kit (`PopupAnchor`) : `belowOrAboveCaret`
(sous le curseur si la place y tient, au-dessus sinon — la complétion,
les intentions, le color picker), `aboveOrBelowCaret` (le côté opposé —
la signature), `atPoint` (au point — le peek), `topLeftCorner` (le fil
d'Ariane), `rightEdge` (le bord droit — disponible, aucune production
ne l'appelle), `centeredAboveSelection` (la barre de sélection). Toutes
prennent la taille mesurée (`popups.taille`) et les murs de la couche ;
la pose se borne AUX MURS quoi qu'il arrive (défense en profondeur de
la couche — même une ancre fausse ne sort pas de l'écran).

### Pas 6 — le clavier, si la carte est interactive

Le manager route déjà les touches déléguées vers la première carte
interactive ouverte (l'ordre : complétion → intentions →
débordements) ; votre carte n'a qu'à implémenter le contrat :

```java
public final class TypeInfoView extends TextRowsPopup implements PopupClavier {
    @Override public boolean surFleche(int pas) {
        // déplacer la surbrillance, rendre vrai si la carte a bougé
    }
    @Override public boolean surValidation() {
        // choisir la rangée courante, rendre vrai si elle a validé
    }
}
```

UP/DOWN déplacent, Enter/Tab valident, Escape referme — vous ne
touchez AUCUNE table de touches, aucun listener de clavier : le crochet
public `editeur.setSurTouchePopup(popups::surTouche)` fait déjà le
voyage (la composition IME en cours neutralise la délégation, une
frappe composée ne sert jamais une carte).

### Pas 7 — les régimes : rien à coder

Frappe, défilement, exclusions, cascades, voile, tap hors cartes,
détachement : TOUT est déjà servi par le manager et les régimes du
contrôleur. Votre atelier appelle `popups.surFrappe()` sur l'édition,
`popups.surDefilement()` au scroll (la démo le fait dans
`AtelierPopups`), `popups.detacher()` à la destruction — le reste suit.

### Pas 8 — le banc

Un banc Robolectric par invariant, le gabarit de `PopupManagerTest` :
lier des fausses cartes (`PanneauEssai` du banc mesure 100 + le padding
de la carte), montrer, lire l'état ET la couche (`couche.isShowing`,
`couche.popupOf`), fermer, vérifier l'isochronie. Les attentes qui
comptent : la carte de l'exclu QUITTE la couche à l'ouverture du
genre, la carte du fermé quitte la couche à la fermeture, la pose se
borne aux murs sur la largeur ENTIÈRE, `detacher()` vide tout.

## 4. Connecter les données — les moteurs réels

Le mandat §6 : les popups ne demandent que ce que les moteurs SERVENT
réellement ; la présentation ne se mélange jamais avec l'analyse. Les
trois étages de données du dépôt (l'audit §5 les a vérifiés) :

| Étage | Contrat réel | Le chemin vers la carte |
| --- | --- | --- |
| LSP | `lsp-protocole.LspSession` : `completion`, `survol`, `signature`, `definition`, `references`, `symboles`, `rennomage`, `correctifs`, `formatage`, `plis`, `diagnosticsDe`… (le serveur Java réel ne les sert pas tous — vérifiez `sertXxx()`) | l'atelier demande, la réponse revient sur le fil UI (`whenComplete` + `editeur.post` — voir `AtelierPopups.basculerCompletion`), l'atelier POSE les candidats typés sur la carte (`setItems`, `setModel`) |
| Moteurs hors ligne | `core-intelligence` : `CompletionMots`/`CompletionMatcher`, `IndexSymboles`, `Renommage`, `SafeDelete`, `Hierarchies`, `Gabarits`, `RechercheFloue`… | même chemin — l'atelier sert d'intermédiaire, la carte reste pure consommatrice |
| SPI hôte | `android-host` : `DiagnosticHote`, `CorrectifsLint`, `ValeurEnLigne`… | l'hôte implémente, l'atelier transpose sur la carte |

Le patron d'asynchrone de la démo : la réponse future revient par
`whenComplete` puis `editeur.post(...)` — la pose se joue sur le fil
UI. Si vos requêtes peuvent se devancer (deux frappes, deux requêtes),
gardez un JETON DE GÉNÉRATION chez l'hôte et ne posez que la dernière :
le kit garantit la vue ne survit jamais à son état, le tri des
réponses reste à l'hôte (limitation consignée au rapport de migration
§7).

## 5. Les feuilles — quand la surface est dense

La règle ADR-018 : les surfaces denses vivent dans les feuilles —
`FeuilleBas` et `FeuilleHaut` du kit, posées sur `android.R.id.content`,
avec voile cliquable, poignée de glissement, retour système. Elles
PRENNENT le focus par conception (elles sont modales) ; la carte qui
reste ouverte dessous n'existe pas : l'atelier appelle
`popups.fermerSousVoile()` AVANT d'ouvrir la feuille (la démo le fait
dans `CatalogueActions`, ligne 142) — l'intelligence et les actions
cèdent, le chrome survit.

## 6. Les pièges (courts, durs, déjà payés par d'autres)

1. Ne JAMAIS `requestFocus()` sur une carte de la couche (PIEGE-27 —
   le banc `PopupLayerFocusTest` le prouve à chaque passe).
2. Ne JAMAIS partager une instance entre genres — `lier` le refuse,
   l'erreur nomme le genre fautif.
3. Ne JAMAIS mesurer la carte vous-même (la mesure du manager rend la
   carte entière — six copies de ce pattern sont mortes au chantier).
4. Ne JAMAIS retirer une vue de la couche à la main (`couche.dismiss`
   appartient au manager — l'hôte qui compose à la main sans manager
   reste possible, mais il recode TOUTE la boucle, voir USAGE §9).
5. La composition IME neutralise la délégation clavier — c'est voulu.
6. La carte ne connaît pas l'éditeur : la géométrie (caret, sélection,
   viewport) se demande à `android-view` (`caretBounds`,
   `selectionBounds`, `ViewportWatch`), JAMAIS aux widgets.
7. Le plafond : 600 lignes par fichier (800 pour les bancs nommés,
   ADR-019). La décision du propriétaire du 2026-11-13 autorise la
   nomination ciblée jusqu'à 1 200 lignes et des méthodes plus longues
   POUR LES FICHIERS DU CHANTIER, le jour où l'un d'eux bute — chaque
   nomination porte sa justification dans
   `config/checkstyle/suppressions.xml`, les cliquets demeurent.
8. La Javadoc des classes publiques en français, les identifiants et
   messages d'exception en anglais ASCII — la politique de langue est
   une porte bloquante (`scripts/check-language-policy`).
9. Tout correctif de bug arrive avec le banc qui échouait avant.

## 7. La référence des surfaces existantes

Chaque surface réelle du dépôt — nom, fonction, déclencheur, service,
état, dépendances — vit dans [`POPUP_INVENTORY.md`]\
(POPUP_INVENTORY.md) : l'infrastructure (§1.1), les douze cartes de la
couche (§1.2), les feuilles (§1.3), les surfaces in-canvas assumées
(§1.4), l'orchestration de la démo (§1.6), et la classification
catégorie par catégorie (§2). L'API publique du kit se lit dans
l'USAGE §9 ; les signatures exactes dans `android-widgets` —
`PopupManager`, `PopupController`, `PopupLayer`, `PopupAnchor`,
`PopupPlacer`, `PopupPanel`, `TextRowsPopup`, `PopupClavier`,
`PopupPalette`, `PopupTokens`.

## 8. Faire évoluer le socle — le guide de maintenance

- **Une règle de coexistence nouvelle** : elle vit au contrôleur
  (`ouvrirRendus`/`closeRendus`/régimes), se prouve au banc
  (`PopupControllerTest`), et TOUTES les surfaces la reçoivent — le
  manager l'exécute sans jamais la connaître.
- **Un nouveau genre** : les huit pas du §3 — le manager ne bouge pas.
- **Une nouvelle ancre** : `PopupAnchor`, statique et pure, avec son
  banc (`PopupAnchorTest`) — les murs se passent en paramètres, la
  couche borne en profondeur par-dessus.
- **Une nouvelle carte interactive** : le patron `PopupClavier` — la
  délégation existe déjà, la carte n'a qu'à répondre.
- **Les cliquets** : couverture ≥ 90 % (modules purs), mutation
  ≥ 80 % (`core-text`), plafonds Checkstyle/PMD, politique de langue —
  une exemption se justifie par ADR, jamais à la légère (le
  propriétaire seul l'accorde).
- **La publication** : les artefacts (AAR compris) suivent
  `publishToMavenLocal` et le registre du propriétaire ; une évolution
  d'API d'un module PUR régénère son manifeste (`./gradlew
  apiManifest`) — les modules Android n'en portent pas.
- **Le suivi** : une entrée par version au `CHANGELOG.md`, les
  décisions structurantes aux ADR, les chantiers à la feuille de route
  (`feuille-de-route.md` et
  `feuille-de-route-restante.md`) ; la mission popups se suit à
  `feuille-de-route-popups.md`.
