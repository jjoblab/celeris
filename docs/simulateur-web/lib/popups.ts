/**
 * Celeris — données des 26 genres de popups et machine à états fidèle.
 *
 * Source de vérité : android-widgets/PopupController.java (enum PopupKind,
 * FAMILLE_ACTIONS, onKeystroke, onScroll, surToucheHorsCartes) et
 * android-widgets/PopupManager.java (SOUS_VOILE, ORDRE_CLAVIER, EPHEMERES).
 * Les libellés FR/EN viennent des strings.xml d’android-host (values-fr /
 * values). Commit de référence : 88b3206.
 */

export type GenreId =
  | 'COMPLETION_LIST'
  | 'INTENTIONS_MENU'
  | 'SIGNATURE_HELP'
  | 'COMPLETION_DOC'
  | 'HOVER'
  | 'DIAGNOSTIC_TOOLTIP'
  | 'BREADCRUMB'
  | 'SEARCH_BAR'
  | 'SELECTION_BAR'
  | 'SELECTION_OVERFLOW'
  | 'ERROR_BAND'
  | 'MINIMAP'
  | 'FAST_SCROLLER'
  | 'TOUCH_BAR'
  | 'COLOR_PICKER'
  | 'TOOLBAR_OVERFLOW'
  | 'REFERENCES'
  | 'TYPE_INFO'
  | 'HIERARCHY'
  | 'NOTIFICATION'
  | 'EXTERNAL_DOC'
  | 'GABARITS'
  | 'SAFE_DELETE'
  | 'SAFE_MOVE'
  | 'SUPPRESS_INSPECTION'
  | 'EXCEPTION_DETAILS'
  | 'SETTINGS_MENU';

export type Categorie = 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'G';

export const CATEGORIES: Record<Categorie, string> = {
  A: 'Intelligence du code',
  B: 'Diagnostics et corrections',
  C: 'Navigation',
  D: 'Refactoring et génération',
  E: 'Débogage',
  F: 'Projet et environnement',
  G: 'UI contextuelle',
};

export type RegimeFrappe = 'ferme' | 'filtre' | 'rafraichit' | 'epargne';
export type RegimeDefilement = 'ferme' | 'repositionne' | 'epargne' | 'suit';

export interface SectionDoc {
  titre: string;
  lignes: string[];
}

export interface PopupSpec {
  id: GenreId;
  constName: string;
  nomFr: string;
  categorie: Categorie;
  vue: string;
  satellite: string;
  donnees: string;
  ancre: string;
  clavier: boolean;
  frappe: RegimeFrappe;
  defilement: RegimeDefilement;
  tapHorsCartes: boolean;
  echap: boolean;
  voile: boolean;
  horlogeMs?: number;
  chrome?: boolean;
  description: string;
  sections: SectionDoc[];
  declencheurs: string[];
  fermeture: string;
  strings?: { fr: string; en: string }[];
}

export const POPUPS: PopupSpec[] = [
  {
    id: 'COMPLETION_LIST',
    constName: 'COMPLETION_LIST',
    nomFr: 'Liste de complétion',
    categorie: 'A',
    vue: 'CompletionListView',
    satellite: 'PopupsEditeur.basculerCompletion (ContenuPopups.candidats)',
    donnees: 'LSP LanguageSession.completion, repli IntelligenceHub + CoachPopups',
    ancre: 'PopupPlacer — sous le curseur (belowOrAboveCaret)',
    clavier: true,
    frappe: 'filtre',
    defilement: 'repositionne',
    tapHorsCartes: false,
    echap: true,
    voile: true,
    description:
      "La liste des candidats qui complète le mot en cours de frappe. Huit candidats au maximum, chacun deux lignes : le badge de genre (M, F, C…) puis le libellé où le préfixe déjà tapé vit à l’encre d’accent, l’identité qualifiée attenue en dessous. Le premier candidat se surligne d’entrée — la flèche déplace, Enter ou Tab accepte : la frappe filtre localement, la requête ne repart jamais au serveur.",
    sections: [
      { titre: 'Rangées de candidats (8 max)', lignes: ['Badge de genre + libellé avec runs du préfixe à l’accent', 'Identité qualifiée atténuée sur la seconde ligne', 'Rangée de 40 dp, premier candidat surligné'] },
    ],
    declencheurs: [
      'La frappe — quand le caret termine un identifiant (doitOuvrirALaFrappe)',
      'L’action de menu « Compléter le mot »',
      'La sélection, si la liste est déjà ouverte',
    ],
    fermeture: "Frappe sur blanc/ponctuation, acceptation (Enter/Tab/tap — remplace le mot), Échap. La frappe sur lettre ne la ferme pas : elle filtre.",
    strings: [
      { fr: 'Compléter le mot', en: 'Complete word' },
    ],
  },
  {
    id: 'INTENTIONS_MENU',
    constName: 'INTENTIONS_MENU',
    nomFr: "Menu d’intentions",
    categorie: 'B',
    vue: 'IntentionsMenuView',
    satellite: 'CarteIntentions.ouvrir (items de configurerIntentions — 16 intentions)',
    donnees: 'CatalogueActions (la table unique des actions de l’hôte)',
    ancre: 'PopupAnchor.belowOrAboveCaret — sans section',
    clavier: true,
    frappe: 'epargne',
    defilement: 'repositionne',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "Les intentions au curseur — l’équivalent du menu Alt+Enter d’Android Studio. Chaque rangée est une action palier titre : lint, aperçu de définition, usages, info de type, hiérarchie, documentation externe, génération, suppression sûre, déplacement, masquage, exécution… Le choix referme la carte AVANT d’exécuter l’action. La complétion le chasse (un menu ne survit pas à la liste) et la frappe seule l’épargne — il se repositionne au défilement.",
    sections: [
      { titre: 'Rangées d’actions (16 intentions réelles)', lignes: ['Lint — correctifs de la ligne', 'Aperçu de définition', 'Rechercher les usages', 'Info de type au curseur', 'Hiérarchie de types ou d’appels', 'Documentation externe', 'Générer du code', 'Suppression sûre, déplacement, masquage', 'Exécuter le fichier, diff, correctif LSP', 'Agent, IA fantôme, IA prédiction'] },
    ],
    declencheurs: [
      "L’action de menu « Intentions au curseur »",
      'Le menu d’intentions de la toolbar',
    ],
    fermeture: "Choix d’une action (ferme AVANT d’exécuter), la complétion qui s’ouvre, le tap hors cartes, Échap.",
    strings: [
      { fr: 'Intentions au curseur', en: 'Intentions at caret' },
    ],
  },
  {
    id: 'SIGNATURE_HELP',
    constName: 'SIGNATURE_HELP',
    nomFr: 'Aide aux paramètres',
    categorie: 'A',
    vue: 'SignatureHelpView',
    satellite: 'PopupsEditeur.rafraichirSignature (serveur ou coach.signatureDe)',
    donnees: 'LSP signature — surcharges ±1, ou coach en repli',
    ancre: 'PopupAnchor.aboveOrBelowCaret — le côté opposé de la complétion',
    clavier: false,
    frappe: 'rafraichit',
    defilement: 'epargne',
    tapHorsCartes: false,
    echap: true,
    voile: true,
    description:
      "L’aide aux paramètres — la signature de l’appel en cours de frappe. Les surcharges voisines (±1) vivent attenuees de part et d’autre, la signature active en trois paliers : paramètres déjà frappés à l’accent, paramètre ACTIF à l’accent et en gras, paramètres à venir attenues. Le stepper tactile ‹ i/n › passe les surcharges, l’appui long (500 ms) ouvre le peek de la documentation. Elle se rafraîchit à CHAQUE édition — la frappe ne la ferme jamais, elle la suit.",
    sections: [
      { titre: 'Surcharges voisines ±1 (atténuées)', lignes: ['La surcharge précédente et la suivante en encre atténuée'] },
      { titre: 'Signature active en trois paliers', lignes: ['Paramètres frappés — encre d’accent', 'Paramètre actif — accent + gras', 'Paramètres à venir — atténués'] },
      { titre: 'Stepper ‹ i/n ›', lignes: ['Navigation tactile entre les surcharges'] },
      { titre: 'Peek (appui long 500 ms)', lignes: ['La documentation du paramètre se consulte'] },
    ],
    declencheurs: [
      'Automatique — chaque édition dans un appel ouvert',
    ],
    fermeture: "Le modèle null (plus d’appel), Échap (éphémère), le voile modal.",
  },
  {
    id: 'COMPLETION_DOC',
    constName: 'COMPLETION_DOC',
    nomFr: 'Documentation de complétion',
    categorie: 'A',
    vue: 'CompletionDocView',
    satellite: 'posée avec la liste (docDe du premier candidat documenté)',
    donnees: 'LSP documentation du candidat — Markdown typé',
    ancre: 'PopupPlacer — au flanc de la liste',
    clavier: false,
    frappe: 'filtre',
    defilement: 'repositionne',
    tapHorsCartes: false,
    echap: false,
    voile: true,
    description:
      "La documentation flottante du candidat sélectionné — elle n’existe JAMAIS sans sa liste : fermer la liste ferme la documentation (cascade). La bande SIGNATURE se teinte, le titre vit à l’encre d’accent ; puis chaque section typée (Parameters, Returns, Throws) se pose derrière un filet, le libellé de genre en micro atténué.",
    sections: [
      { titre: 'Bande signature teintée', lignes: ['Titre à l’encre d’accent, palier titre'] },
      { titre: 'Sections typées au filet', lignes: ['**Parameters:** — lignes @param indentées', '**Returns:** — @return', '**Throws:** — @throws'] },
    ],
    declencheurs: [
      "L’ouverture de la liste — la doc suit le candidat sélectionné",
    ],
    fermeture: 'Cascade : fermer la liste ferme la doc. Jamais seule.',
  },
  {
    id: 'HOVER',
    constName: 'HOVER',
    nomFr: 'Survol de symbole',
    categorie: 'A',
    vue: 'CompletionDocView (instance du genre survol)',
    satellite: 'DocAncre — survolAuCaret, survolAuDoubleTap, apercu, renduPreview',
    donnees: 'LSP survol, table du coach, ApercuDefinition (plan local ≤ 12 lignes)',
    ancre: 'PopupAnchor.atPoint — au point du geste',
    clavier: false,
    frappe: 'ferme',
    defilement: 'ferme',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "La documentation au geste : double-tap d’un symbole, « i » de la barre de sélection, clic du repère @Preview de la gouttière. Le peek de définition (« Définition de %1$s ») y vit aussi — l’extrait du plan, douze lignes au plus. Le rendu @Preview se pose au coin de la gouttière. Fermée à la frappe et au défilement comme toute bulle qui décrit.",
    sections: [
      { titre: 'Bande signature teintée', lignes: ['La signature du symbole survolé'] },
      { titre: 'Corps de documentation', lignes: ['Sections typées au filet, Markdown lu'] },
      { titre: 'Variantes (DocAncre)', lignes: ['Peek — « Définition de %1$s », extrait ≤ 12 lignes du plan', 'Rendu @Preview — « Rendu @Preview », coin gouttière topLeftCorner'] },
    ],
    declencheurs: [
      'Le double-tap d’un symbole (après le sélecteur de couleur)',
      "Le « i » cerclé de la barre de sélection",
      "L’action de menu « Survoler le symbole »",
      'Le clic du repère @Preview dans la gouttière',
    ],
    fermeture: 'La frappe, le défilement, le tap hors cartes, Échap.',
    strings: [
      { fr: 'Définition de %1$s', en: 'Definition of %1$s' },
      { fr: 'Rendu @Preview', en: '@Preview render' },
      { fr: 'Survoler le symbole', en: 'Hover symbol doc' },
    ],
  },
  {
    id: 'DIAGNOSTIC_TOOLTIP',
    constName: 'DIAGNOSTIC_TOOLTIP',
    nomFr: 'Info-bulle de diagnostic',
    categorie: 'B',
    vue: 'DiagnosticTooltipView',
    satellite: 'InfoBulle.rafraichir — depuis surSelection, hors frappe',
    donnees: 'ContenuPopups.problemesDe — tous les diagnostics de la ligne du caret',
    ancre: 'PopupAnchor.atPoint — au caret',
    clavier: false,
    frappe: 'ferme',
    defilement: 'ferme',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "Les problèmes du lieu, EMPILÉS — l’IDE de référence ne sert jamais une bulle par diagnostic. Chaque problème garde son marqueur à son encre (« × » pour l’erreur, « ! » pour l’avertissement) au palier titre, ses lignes de message sous lui ; un filet les sépare. La clôture « Plus d’actions · menu d’intentions » ne se pose QUE si des intentions existent — l’IDE ne promet pas de correctifs sans correctifs.",
    sections: [
      { titre: 'Problèmes empilés (par problème)', lignes: ['Marqueur « × » à l’encre d’erreur — palier titre', 'Marqueur « ! » à l’encre d’avertissement — palier titre', 'Lignes du message en dessous', 'Filet entre les problèmes'] },
      { titre: "Clôture d’indice (si intentions)", lignes: ['« Plus d’actions · menu d’intentions » — encre META atténuée'] },
    ],
    declencheurs: [
      'Le déplacement du curseur sur une ligne diagnostiquée (sélection hors frappe)',
    ],
    fermeture: 'La frappe, le défilement, le tap hors cartes.',
    strings: [
      { fr: "Plus d’actions · menu d’intentions", en: 'More actions · intentions menu' },
    ],
  },
  {
    id: 'BREADCRUMB',
    constName: 'BREADCRUMB',
    nomFr: "Fil d’Ariane",
    categorie: 'C',
    vue: 'BreadcrumbView',
    satellite: 'FilAriane — ContenuPopups.pathFor(plan, caret)',
    donnees: 'OutlineEntry — le plan du document',
    ancre: 'PopupAnchor.topLeftCorner — coin haut-gauche',
    clavier: false,
    frappe: 'epargne',
    defilement: 'suit',
    tapHorsCartes: false,
    echap: false,
    voile: false,
    chrome: true,
    description:
      "Le chemin qui englobe le curseur — une seule rangée en encre META atténuée, les niveaux joints par « › », la tête tronquée d’une ellipse si le chemin dépasse. Chrome : il suit le curseur à l’édition, à la sélection et au défilement — aucune frappe ne le chasse, seul le chemin vide le fait disparaître.",
    sections: [
      { titre: 'Chemin du plan', lignes: ['Niveaux joints « › »', 'Troncature en tête avec ellipse'] },
    ],
    declencheurs: [
      'Automatique — édition, sélection, défilement (suit le curseur)',
    ],
    fermeture: 'Le chemin vide (le plan n’englobe plus le caret). Jamais à la frappe ni au défilement.',
  },
  {
    id: 'SEARCH_BAR',
    constName: 'SEARCH_BAR',
    nomFr: 'Barre de recherche',
    categorie: 'C',
    vue: '(aucune carte — état seul)',
    satellite: 'PopupsEditeur.ouvrirRecherche / fermerRecherche — le champ vit chez l’hôte (AtelierRecherche, feuille du bas)',
    donnees: 'AtelierRecherche — recherche du document ouvert',
    ancre: 'Feuille du bas (ADR-018) — pas une carte',
    clavier: false,
    frappe: 'epargne',
    defilement: 'epargne',
    tapHorsCartes: false,
    echap: false,
    voile: false,
    description:
      "La recherche dans le fichier — état au contrôleur, pas une carte : le champ de saisie vit dans la feuille du bas de l’hôte. Elle se ferme SUR DEMANDE SEULE — ni la frappe, ni le défilement, ni le tap hors cartes ne l’atteignent.",
    sections: [
      { titre: 'État seul', lignes: ['Le genre vit au contrôleur', 'La feuille du bas porte le champ de saisie et les résultats'] },
    ],
    declencheurs: ["L’action de menu « Rechercher dans le fichier »"],
    fermeture: 'Sur demande seule — le bouton fermer, la feuille refermée.',
  },
  {
    id: 'SELECTION_BAR',
    constName: 'SELECTION_BAR',
    nomFr: 'Barre de sélection',
    categorie: 'G',
    vue: 'SelectionBarView',
    satellite: 'PopupsEditeur.rafraichirBarreSelection',
    donnees: 'Actions de sélection de l’activité (Couper, Copier, Coller, Tout sélectionner)',
    ancre: 'PopupAnchor.centeredAboveSelection — pilule au-dessus de la sélection',
    clavier: false,
    frappe: 'ferme',
    defilement: 'repositionne',
    tapHorsCartes: false,
    echap: false,
    voile: true,
    description:
      "La pilule d’actions qui naît au-dessus d’une sélection : pastilles d’action, filet, « i » cerclé (le survol de documentation), puce « ⋯ » (le débordement). Le pop d’entrée dure 160 ms, la cascade 120 ms décalée de 24 ms. Elle cède à la frappe — la frappe remplace la sélection — mais SURVIT au défilement en se repositionnant.",
    sections: [
      { titre: 'Pilule', lignes: ['Pastilles : Couper · Copier · Coller · Tout sélectionner', 'Filet séparateur', '« i » cerclé — ouvre le survol de documentation', 'Puce « ⋯ » — ouvre le menu de débordement'] },
    ],
    declencheurs: ['Automatique — toute sélection unique non vide'],
    fermeture: 'La frappe (la sélection est remplacée), la sélection vide. Le débordement suit sa barre en cascade.',
  },
  {
    id: 'SELECTION_OVERFLOW',
    constName: 'SELECTION_OVERFLOW',
    nomFr: 'Débordement de sélection',
    categorie: 'G',
    vue: 'IntentionsMenuView (instance propre)',
    satellite: 'MenuDebordement — la puce « ⋯ » de la barre',
    donnees: 'Les actions repliées de la barre (Dupliquer…)',
    ancre: 'Sous sa barre — suit la barre',
    clavier: true,
    frappe: 'ferme',
    defilement: 'repositionne',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "Le menu de débordement de la barre de sélection — il n’existe JAMAIS sans sa barre : fermer la barre ferme le menu (cascade). Il vit les mêmes régimes qu’elle : cède à la frappe, survit au défilement en se repositionnant derrière elle. La section « PLUS » titre la carte, le choix ferme AVANT d’exécuter.",
    sections: [
      { titre: 'Section PLUS (menu_plus)', lignes: ['« PLUS » — en-tête de section, petites capitales muettes'] },
      { titre: 'Actions repliées', lignes: ['Dupliquer la ligne', '… les actions que la pilule ne montre pas'] },
    ],
    declencheurs: ['La puce « ⋯ » de la barre de sélection'],
    fermeture: 'Cascade avec sa barre ; la frappe ; le tap hors cartes ; Échap.',
    strings: [{ fr: 'PLUS', en: 'MORE' }],
  },
  {
    id: 'SETTINGS_MENU',
    constName: 'SETTINGS_MENU',
    nomFr: 'Carte des réglages',
    categorie: 'G',
    vue: 'ReglagesView (interrupteurs dessinés)',
    satellite: 'Reglages — le coin bas-droit de la couche',
    donnees: 'La table centrale ReglagesEditeur — dix portes stables',
    ancre: 'Coin bas-droit, bornée aux murs',
    clavier: false,
    frappe: 'ferme',
    defilement: 'repositionne',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "La carte des réglages de l’éditeur — la table centrale des commutateurs (l’audit du 2026-11-14 : « un système central editor settings ») : chaque interrupteur bascule SA porte, l’état se relit à chaque pose. Les icônes allégées servent les rangées, la glissière et son pouce se dessinent au kit.",
    sections: [
      { titre: 'Les portes', lignes: ['Indices en ligne — inlay hints', 'Barre de sélection', 'Ampoule d’intentions', 'Complétion automatique', 'Valeurs de pause', 'Texte fantôme IA', 'Minimap · Défilement rapide', 'Numéros de lignes · Retour à la ligne'] },
    ],
    declencheurs: ['Le menu Réglages de l’hôte', 'La puce de la toolbar'],
    fermeture: 'La frappe ; le tap hors cartes ; Échap.',
  },
  {
    id: 'ERROR_BAND',
    constName: 'ERROR_BAND',
    nomFr: "Bande d’erreurs",
    categorie: 'G',
    vue: 'ErrorBandView',
    satellite: 'BordDroit — setDiagnostics → rafraichirBande',
    donnees: 'DiagnosticHote (la SPI des diagnostics)',
    ancre: 'Bord droit — 200 crans, pleine hauteur respirée',
    clavier: false,
    frappe: 'epargne',
    defilement: 'suit',
    tapHorsCartes: false,
    echap: false,
    voile: false,
    chrome: true,
    description:
      "Les tirets colorés du bord droit — la gravité de chaque diagnostic du document, 200 crans au plus, pleine hauteur respirée. Chrome : ni la frappe ni le défilement ne la ferment, elle vit avec les diagnostics. Le tap sur un tiret SAUTE au diagnostic — c’est sa seule interaction.",
    sections: [
      { titre: 'Tirets de gravité', lignes: ['Erreur — encre d’erreur', 'Avertissement — encre d’avertissement', 'Le tap saute au diagnostic'] },
    ],
    declencheurs: ['Automatique — les diagnostics de l’hôte'],
    fermeture: 'Sans diagnostics. Jamais à la frappe ni au défilement.',
  },
  {
    id: 'MINIMAP',
    constName: 'MINIMAP',
    nomFr: 'Minimap',
    categorie: 'G',
    vue: 'MinimapView',
    satellite: 'BordDroit.rafraichir',
    donnees: 'Document + arbre syntaxique (200 rangées au plus)',
    ancre: 'Bord droit — à gauche du pouce',
    clavier: false,
    frappe: 'epargne',
    defilement: 'suit',
    tapHorsCartes: false,
    echap: false,
    voile: false,
    chrome: true,
    description:
      "La silhouette colorée du document au bord droit — 200 rangées au plus, le cartouche de la fenêtre visible à l’encre d’accent. Chrome : elle vit avec le document, ni la frappe ni le défilement ne la ferment — l’appui ou le glissement y saute à la ligne.",
    sections: [
      { titre: 'Silhouette', lignes: ['Rangées colorées par token', 'Cartouche de fenêtre — encre d’accent'] },
    ],
    declencheurs: ['Automatique — vit avec le document (édition, défilement)'],
    fermeture: 'Jamais — l’appui/glissement saute à la ligne.',
  },
  {
    id: 'FAST_SCROLLER',
    constName: 'FAST_SCROLLER',
    nomFr: 'Pouce de défilement rapide',
    categorie: 'G',
    vue: 'FastScrollerView',
    satellite: 'BordDroit.rafraichir',
    donnees: 'La fenêtre du document',
    ancre: 'Bord droit — le pouce suit le défilement',
    clavier: false,
    frappe: 'epargne',
    defilement: 'suit',
    tapHorsCartes: false,
    echap: false,
    voile: false,
    chrome: true,
    description:
      "Le pouce du bord droit — il suit le défilement, grossit sous le doigt, le glissement l’emporte. Chrome pur du chantier C9 : aucune frappe ne le chasse, aucun régime ne le ferme.",
    sections: [{ titre: 'Pouce', lignes: ['Grossit sous le doigt — le glissement défile'] }],
    declencheurs: ['Le doigt au bord droit'],
    fermeture: 'Jamais.',
  },
  {
    id: 'TOUCH_BAR',
    constName: 'TOUCH_BAR',
    nomFr: 'Barre de touches de code',
    categorie: 'F',
    vue: 'TouchBarView',
    satellite: 'BarreToolbar.poserBarreTouches (AtelierTouchesCode)',
    donnees: 'Touches de l’activité — Tab, Commenter, Ligne↑, Ligne↓, Dupliquer',
    ancre: 'Bas centré — au-dessus du clavier (l’IME la remonte)',
    clavier: false,
    frappe: 'epargne',
    defilement: 'epargne',
    tapHorsCartes: false,
    echap: false,
    voile: false,
    chrome: true,
    description:
      "Le dépôt de touches au-dessus du clavier — Tab, Commenter, Ligne↑, Ligne↓, Dupliquer. L’hôte la pose, le layout global la repose ; ni la frappe ni le défilement ne la ferment (le clavier la remonte avec l’IME).",
    sections: [{ titre: 'Touches', lignes: ['Tab · Commenter · Ligne↑ · Ligne↓ · Dupliquer'] }],
    declencheurs: ["Posée par l’hôte — vit avec le document"],
    fermeture: 'Jamais (vit avec le document et l’IME).',
  },
  {
    id: 'COLOR_PICKER',
    constName: 'COLOR_PICKER',
    nomFr: 'Sélecteur de couleur',
    categorie: 'G',
    vue: 'ColorPickerView',
    satellite: 'SelecteurCouleur.ouvrirAuDoubleTap',
    donnees: 'PastillesCouleur.tous — le littéral double-tappé',
    ancre: 'PopupAnchor.belowOrAboveCaret',
    clavier: false,
    frappe: 'ferme',
    defilement: 'ferme',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "Le double-tap d’un littéral de couleur ouvre le sélecteur : deux pastilles en tête (l’originale et la courante avec son hex), le champ saturation/valeur, la bande de teinte, seize teintes Material. Le pick s’applique IMPLICITE — l’alpha d’origine est préservé, le littéral se reformate en place : le pick EST la fermeture.",
    sections: [
      { titre: 'En-tête', lignes: ['Pastille originale · pastille courante + hex'] },
      { titre: 'Champ saturation / valeur', lignes: ['La zone de glissement SV'] },
      { titre: 'Bande de teinte', lignes: ['La teinte courante se prend'] },
      { titre: 'Seize teintes Material', lignes: ['La palette de raccourcis'] },
    ],
    declencheurs: ['Le double-tap d’un littéral de couleur (le premier des double-taps)'],
    fermeture: 'Le pick implicite (applique et referme), la frappe, le défilement, le tap hors cartes.',
  },
  {
    id: 'TOOLBAR_OVERFLOW',
    constName: 'TOOLBAR_OVERFLOW',
    nomFr: 'Débordement de la toolbar',
    categorie: 'F',
    vue: 'IntentionsMenuView (instance propre)',
    satellite: 'BarreToolbar — EditorActivity.brancherToolbar (action_debordement)',
    donnees: 'itemsDebordement() — 24 entrées de la barre d’application',
    ancre: 'Pendue sous la puce — coin de scène haut-droit, y = marge',
    clavier: true,
    frappe: 'ferme',
    defilement: 'epargne',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "La carte de débordement de la barre d’application — genre PROPRE depuis le chantier d’unification (elle détournait le menu d’intentions). Pendue sous la puce au coin de scène, PAS au texte : la frappe la ferme, le défilement l’épargne (l’ancre ne suit pas le texte). Deux menus ne tiennent jamais — elle ferme la complétion et le menu du caret, eux la ferment.",
    sections: [
      { titre: 'Section PLUS', lignes: ['« PLUS » — en-tête de section'] },
      { titre: 'Actions de la toolbar', lignes: ['Les 24 entrées repliées de la barre d’application'] },
    ],
    declencheurs: ['La puce de débordement de la barre d’application'],
    fermeture: 'La frappe, le tap hors cartes, Échap. Le défilement l’épargne.',
    strings: [{ fr: 'PLUS', en: 'MORE' }],
  },
  {
    id: 'SETTINGS_MENU',
    constName: 'SETTINGS_MENU',
    nomFr: 'Carte des réglages',
    categorie: 'G',
    vue: 'ReglagesView (interrupteurs dessinés)',
    satellite: 'Reglages — le coin bas-droit de la couche',
    donnees: 'La table centrale ReglagesEditeur — dix portes stables',
    ancre: 'Coin bas-droit, bornée aux murs',
    clavier: false,
    frappe: 'ferme',
    defilement: 'repositionne',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "La carte des réglages de l’éditeur — la table centrale des commutateurs (l’audit du 2026-11-14 : « un système central editor settings ») : chaque interrupteur bascule SA porte, l’état se relit à chaque pose. Les icônes allégées servent les rangées, la glissière et son pouce se dessinent au kit.",
    sections: [
      { titre: 'Les portes', lignes: ['Indices en ligne — inlay hints', 'Barre de sélection', 'Ampoule d’intentions', 'Complétion automatique', 'Valeurs de pause', 'Texte fantôme IA', 'Minimap · Défilement rapide', 'Numéros de lignes · Retour à la ligne'] },
    ],
    declencheurs: ['Le menu Réglages de l’hôte', 'La puce de la toolbar'],
    fermeture: 'La frappe ; le tap hors cartes ; Échap.',
  },
  {
    id: 'REFERENCES',
    constName: 'REFERENCES',
    nomFr: "Carte des usages",
    categorie: 'C',
    vue: 'ReferencesView (TextRowsPopup + PopupClavier)',
    satellite: 'Usages — la seconde surface portée (serveur par le fil references)',
    donnees: 'LSP LanguageSession.references — le compte du serveur, tous fichiers',
    ancre: 'PopupAnchor.belowOrAboveCaret',
    clavier: true,
    frappe: 'ferme',
    defilement: 'ferme',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "Les usages du symbole au curseur, comptés par le SERVEUR — l’en-tête porte le compte de tous les fichiers (« %1$d usages de « %2$s » »). Chaque occurrence locale vit en rangée cliquable : « L12 · l’extrait rogné à 48 caractères… », le choix saute et referme. L’occurrence ÉTRANGÈRE vit muette (« NomFichier.java · L7 ») — l’éditeur embarqué n’ouvre qu’un document, ni le doigt ni la flèche ne la prennent. Les flèches et Enter délégués sautent les étrangères.",
    sections: [
      { titre: 'En-tête qui compte', lignes: ['« %1$d usages de « %2$s » » — le compte du serveur, tous fichiers'] },
      { titre: 'Occurrences locales', lignes: ['« L12 · extrait rogné à 48 caractères + … » — cliquable, saut au choix'] },
      { titre: 'Occurrences étrangères', lignes: ['« Main.java · L7 » — encre muette, jamais prise'] },
    ],
    declencheurs: [
      "L’action de menu « Rechercher les usages »",
      "Le menu d’intentions, la puce de débordement",
    ],
    fermeture: "Le choix d’une occurrence (saute et referme), la frappe, le défilement, le tap hors cartes, Échap.",
    strings: [{ fr: '%1$d usages de « %2$s »', en: '%1$d usages of « %2$s »' }],
  },
  {
    id: 'TYPE_INFO',
    constName: 'TYPE_INFO',
    nomFr: 'Info de type',
    categorie: 'A',
    vue: 'TypeInfoView (patron TextRowsPopup)',
    satellite: 'TypeInfo — la première surface portée (LecteurTypes.typeDe)',
    donnees: 'LecteurTypes — le type lu à la déclaration, jamais deviné',
    ancre: 'PopupAnchor.belowOrAboveCaret',
    clavier: false,
    frappe: 'ferme',
    defilement: 'ferme',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "Le nom du type du symbole au curseur — UNE rangée au palier titre, la carte la plus simple (le fil rouge du guide du développeur). « Type symbole » : le type se lit chez LecteurTypes à la DÉCLARATION — sans type lisible la carte ne se pose jamais. Elle vit les régimes du survol — fermée à la frappe et au défilement, refermée au tap hors cartes — et cohabite avec tout : une bulle qui décrit ne chasse personne.",
    sections: [{ titre: 'Rangée TITRE', lignes: ['Le nom du type du symbole — une seule rangée'] }],
    declencheurs: [
      "L’action de menu « Info de type au curseur »",
      "Le menu d’intentions, la puce de débordement",
    ],
    fermeture: 'La frappe, le défilement, le tap hors cartes, Échap (éphémère).',
    strings: [{ fr: 'Info de type au curseur', en: 'Type info at caret' }],
  },
  {
    id: 'HIERARCHY',
    constName: 'HIERARCHY',
    nomFr: 'Hiérarchie de types ou d’appels',
    categorie: 'C',
    vue: 'HierarchiesView (TextRowsPopup + PopupClavier)',
    satellite: 'CarteHierarchies — la quatrième surface portée (Transpose pure)',
    donnees: 'Moteurs Hierarchies.types / Hierarchies.appelants — le plan décide de la forme',
    ancre: 'PopupAnchor.belowOrAboveCaret',
    clavier: true,
    frappe: 'ferme',
    defilement: 'ferme',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "L’arbre du symbole au curseur — aligné sur l’IDE d’Android Studio : la RACINE vit UNE fois, l’en-tête la nomme (« Hiérarchie de types de « %1$s » »), jamais elle ne se répète dans les sections. Les sommets s’indentent par PROFONDEUR (une respiration de 8 dp par niveau), le sommet connu saute à sa déclaration, l’objet introuvable vit muet à la FRONTIÈRE seule (« Hors projet ») ; la section VIDE ne se pose pas. Le plan décide : un type porté une hiérarchie de types, une fonction une hiérarchie d’appels (« Appelants », avec sa légende du doute si des sites ne se résolvent pas).",
    sections: [
      { titre: 'En-tête qui nomme la racine', lignes: ['« Hiérarchie de types de « %1$s » » — la racine vit UNE fois'] },
      { titre: 'Section Supertypes', lignes: ['Les ancêtres par générations, indentation par profondeur'] },
      { titre: 'Section Sous-types', lignes: ['Les descendants — cliquables, le saut referme'] },
      { titre: 'Section Hors projet', lignes: ['Les introuvables — encre muette, jamais pris'] },
      { titre: 'Hiérarchie d’appels', lignes: ['Section Appelants + légende du doute : « le doute se montre, jamais ne se tranche »'] },
    ],
    declencheurs: [
      "L’action de menu « Hiérarchie de types ou d’appels »",
      "Le menu d’intentions, la puce de débordement",
    ],
    fermeture: "Le choix d’un sommet (saute et referme), la frappe, le défilement, le tap hors cartes, Échap.",
    strings: [
      { fr: 'Hiérarchie de types de « %1$s »', en: 'Type hierarchy of « %1$s »' },
      { fr: "Hiérarchie d’appels de « %1$s »", en: 'Call hierarchy of « %1$s »' },
      { fr: 'Sous-types', en: 'Subtypes' },
      { fr: 'Appelants', en: 'Callers' },
      { fr: 'Hors projet', en: 'Out of project' },
    ],
  },
  {
    id: 'NOTIFICATION',
    constName: 'NOTIFICATION',
    nomFr: 'Notification',
    categorie: 'G',
    vue: 'NotificationView (Handler du fil UI, armer/desarmer)',
    satellite: 'Notifications — notifier(titre, message[, actions])',
    donnees: "L’hôte — correctifs lint, pause de débogage, exécution, sûreté, masquage",
    ancre: 'PopupAnchor.bottomRightCorner — coin bas-droit, bornée aux murs',
    clavier: false,
    frappe: 'epargne',
    defilement: 'epargne',
    tapHorsCartes: false,
    echap: false,
    voile: true,
    horlogeMs: 4000,
    description:
      "La bulle éphémère du coin bas-droit — le régime UNIQUE : l’annonce vit son quart, l’HORLOGE la referme à son heure (4000 ms), le tap qui la prend aussi. La frappe, le défilement et le tap hors cartes l'ÉPARGNENT — le geste ne l’interrompt pas ; la re-pose réarme l’horloge sans cumul. Les actions vivent en HYPERLIENS à l’encre d’accent derrière un filet — la prise referme la bulle AVANT de faire courir l’action (une action qui re-pose n’est jamais tuée par sa propre fermeture). Aucune exclusion : elle cohabite avec tout.",
    sections: [
      { titre: 'Titre', lignes: ['« Annonce » (host) — palier TITRE'] },
      { titre: 'Message', lignes: ['Les lignes de l’annonce'] },
      { titre: 'Filet + liens d’action', lignes: ['À l’encre d’accent — la prise referme AVANT d’exécuter'] },
      { titre: 'Horloge', lignes: ['4000 ms — réarmée à la re-pose, coupée par le tap qui la prend'] },
    ],
    declencheurs: ["L’hôte — les événements réels (lint, pause, exécution, sûreté, masquage)"],
    fermeture: 'Son heure (4000 ms) ou le tap qui la prend. Le voile modal la coupe.',
    strings: [
      { fr: 'Annonce (host) / Avis (demo)', en: 'Notice' },
    ],
  },
  {
    id: 'EXTERNAL_DOC',
    constName: 'EXTERNAL_DOC',
    nomFr: 'Documentation externe',
    categorie: 'A',
    vue: 'CompletionDocView (instance propre — jamais celle du survol)',
    satellite: 'DocExterne — la troisième surface portée',
    donnees: 'Le serveur par le fil survol, ou la table du coach en repli',
    ancre: 'PopupAnchor.belowOrAboveCaret',
    clavier: false,
    frappe: 'epargne',
    defilement: 'repositionne',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "La documentation du symbole hors contexte de complétion — la carte DÉRIVÉE, sa propre instance, jamais celle du survol. Elle vit le régime de la CONSULTATION : la frappe ne la chasse pas (on tape à côté), le défilement ne la ferme pas non plus — elle se REPOSITIONNE, elle suit le curseur ; le tap hors cartes la referme (la consultation cède au geste qui déplace le curseur), l’échappement aussi. Aucune exclusion : une documentation ne chasse personne.",
    sections: [
      { titre: 'Bande signature teintée', lignes: ['La signature du symbole — titre à l’encre d’accent'] },
      { titre: 'Sections typées au filet', lignes: ['Parameters, Returns, Throws — Markdown lu'] },
    ],
    declencheurs: [
      "L’action de menu « Documentation externe »",
      "Le menu d’intentions, la puce de débordement",
    ],
    fermeture: 'Le tap hors cartes, Échap (éphémère). Jamais la frappe ; le défilement la repositionne.',
    strings: [{ fr: 'Documentation externe', en: 'External documentation' }],
  },
  {
    id: 'GABARITS',
    constName: 'GABARITS',
    nomFr: 'Génération et gabarits',
    categorie: 'D',
    vue: 'IntentionsMenuView (instance propre)',
    satellite: 'Gabarits — la cinquième surface portée (Generateurs, Entourer, Gabarits A-22)',
    donnees: 'Les champs réels de la classe englobante (B-24), la sélection',
    ancre: 'PopupAnchor.belowOrAboveCaret',
    clavier: true,
    frappe: 'epargne',
    defilement: 'repositionne',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "La carte de génération de code : constructeur, accesseurs, equals/hashCode/toString — lus des champs RÉELS de la classe englobante —, l’entourage de la sélection (try/catch, if) et le gabarit de méthode à trous (canal unique A-22 : configurerGabaritMethode). Le choix referme la carte AVANT d’appliquer en transaction annulable. La saisie la chasse quand la complétion s’ouvre (un menu ne survit pas à la liste) ; le défilement la repositionne — les régimes du menu du caret.",
    sections: [
      { titre: 'Section « Génération & gabarits »', lignes: ['Constructeur de %1$s — si classe englobante au caret', 'Accesseurs de %1$s — seulement si champs réels', 'equals, hashCode et toString de %1$s', 'Entourer de try/catch — seulement si sélection non vide', 'Entourer de if', 'Insérer le gabarit de méthode — canal unique A-22'] },
    ],
    declencheurs: ["L’action de menu « Générer du code »", "Le menu d’intentions, la puce de débordement"],
    fermeture: "Le choix d’un gabarit (ferme AVANT d’appliquer), la complétion qui s’ouvre, le tap hors cartes, Échap. La frappe seule l’épargne ; le défilement la repositionne.",
    strings: [
      { fr: 'Génération & gabarits', en: 'Generate & templates' },
      { fr: 'Constructeur de %1$s', en: 'Constructor of %1$s' },
      { fr: 'Accesseurs de %1$s', en: 'Getters and setters of %1$s' },
      { fr: 'Insérer le gabarit de méthode', en: 'Insert method template' },
    ],
  },
  {
    id: 'SAFE_DELETE',
    constName: 'SAFE_DELETE',
    nomFr: 'Suppression sûre',
    categorie: 'D',
    vue: 'ReferencesView (la carte des usages qui barrent)',
    satellite: 'Surete — la sixième surface portée (moteur SafeDelete B-23)',
    donnees: 'Le verdict du moteur — les usages de la déclaration au caret',
    ancre: 'PopupAnchor.belowOrAboveCaret',
    clavier: true,
    frappe: 'ferme',
    defilement: 'ferme',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "Les usages qui barrent — la suppression sûre se lit AVANT de trancher : la carte montre chaque usage (chacun saute à son lieu), la suppression attend son heure. La voie libre efface en transaction annulable et l’annonce « Supprimée — un geste la défait » ; hors déclaration, l’annonce dit qu’aucune déclaration ne se trouve. L’en-tête porte le compte du moteur (« %1$d usages empêchent la suppression de « %2$s » »).",
    sections: [
      { titre: 'En-tête du verdict', lignes: ['« %1$d usages empêchent la suppression de « %2$s » »'] },
      { titre: 'Occurrences qui barrent', lignes: ['« L12 · extrait rogné » — cliquables, la suppression attend'] },
    ],
    declencheurs: ["L’action de menu « Suppression sûre de la déclaration »", "Le menu d’intentions, la puce de débordement"],
    fermeture: 'La frappe, le défilement, le tap hors cartes, Échap. Cohabite avec la complétion.',
    strings: [{ fr: '%1$d usages empêchent la suppression de « %2$s »', en: '%1$d usages block the deletion of « %2$s »' }],
  },
  {
    id: 'SAFE_MOVE',
    constName: 'SAFE_MOVE',
    nomFr: 'Déplacement sûr',
    categorie: 'D',
    vue: 'IntentionsMenuView (la carte des cibles)',
    satellite: 'Surete — la sixième surface portée (moteur SafeMove B-23)',
    donnees: 'Les types du fichier hors la classe englobante — les conditions de sûreté du moteur',
    ancre: 'PopupAnchor.belowOrAboveCaret',
    clavier: true,
    frappe: 'epargne',
    defilement: 'repositionne',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "Les cibles du déménagement — la méthode au caret se déplace dans une autre classe du fichier : les types candidats se proposent (« Déplacer dans »), le choix tente le plan — les conditions de sûreté du moteur SEULES décident, le refus s’annonce sans raison inventée, la réussite annonce « Déplacée dans « %1$s » — un geste la défait ». Les régimes du menu du caret : la complétion la chasse, la frappe seule et le défilement l’épargnent (elle se repositionne).",
    sections: [
      { titre: 'Section « Déplacer dans »', lignes: ['Les types du fichier hors la classe englobante — le plan ou le refus du moteur'] },
    ],
    declencheurs: ["L’action de menu « Déplacer la méthode dans une classe »", "Le menu d’intentions, la puce de débordement"],
    fermeture: "Le choix d’une cible, la complétion qui s’ouvre, le tap hors cartes, Échap. La frappe seule et le défilement l’épargnent.",
    strings: [
      { fr: 'Déplacer dans', en: 'Move into' },
      { fr: 'Déplacée dans « %1$s » — un geste la défait', en: 'Moved into « %1$s » — one gesture undoes it' },
    ],
  },
  {
    id: 'SUPPRESS_INSPECTION',
    constName: 'SUPPRESS_INSPECTION',
    nomFr: "Masquage d’inspection",
    categorie: 'B',
    vue: 'IntentionsMenuView',
    satellite: 'Masquage — la septième surface portée (AnalyseurLint.regle, SuppressionInspection)',
    donnees: "L’identifiant entre crochets du lint — jamais une règle devinée",
    ancre: 'PopupAnchor.belowOrAboveCaret',
    clavier: true,
    frappe: 'epargne',
    defilement: 'repositionne',
    tapHorsCartes: true,
    echap: true,
    voile: true,
    description:
      "La règle portée par l’avertissement de la ligne se masque en ligne : les diagnostics réglés de la ligne du caret s’y proposent (« Masquer %1$s »), le choix pose le commentaire noinspection en fin de ligne en transaction annulable, l’analyse se RELANCE (configurerRelanceLint) — la règle masquée ne se rapporte plus, les autres demeurent. Sans diagnostic réglé, l’annonce le dit. Les régimes du menu du caret.",
    sections: [
      { titre: 'Section « Masquer »', lignes: ['« Masquer %1$s » — une rangée par diagnostic réglé de la ligne du caret'] },
    ],
    declencheurs: ["L’action de menu « Masquer l’inspection de la ligne »", "Le menu d’intentions, la puce de débordement"],
    fermeture: "Le choix d’une règle, la complétion qui s’ouvre, le tap hors cartes, Échap. La frappe seule et le défilement l’épargnent.",
    strings: [
      { fr: 'Masquer', en: 'Suppress' },
      { fr: 'Masquer %1$s', en: 'Suppress %1$s' },
    ],
  },
  {
    id: 'EXCEPTION_DETAILS',
    constName: 'EXCEPTION_DETAILS',
    nomFr: "Détails d’exception",
    categorie: 'E',
    vue: 'ExceptionView',
    satellite: 'ExceptionCarte — la huitième surface portée (TraceExecution, AnalyseurPile E-06)',
    donnees: "La trace du document RÉEL — classe et méthode englobantes du caret, relue par l’analyseur de piles",
    ancre: 'PopupAnchor.belowOrAboveCaret',
    clavier: false,
    frappe: 'epargne',
    defilement: 'repositionne',
    tapHorsCartes: true,
    echap: true,
    voile: false,
    description:
      "La carte de l’échec d’exécution — le type « × java.lang.Exception » à l’encre d’erreur, le message (« L’exécution simulée échoue — la trace suit le plan du fichier »), puis la pile en cadres cliquables : le cadre du fichier ouvert saute au début de sa ligne, l’étranger vit muet. La trace naît du document réel (la méthode englobante au sommet, à la ligne du caret) puis se relit par l’analyseur de piles E-06. Le régime de la CONSULTATION : la frappe ne la chasse pas, le défilement la repositionne, PAS sous voile.",
    sections: [
      { titre: 'Titre du type', lignes: ['« × java.lang.Exception » — encre d’erreur, palier titre'] },
      { titre: 'Message', lignes: ['« L’exécution simulée échoue — la trace suit le plan du fichier »'] },
      { titre: 'Filet + cadres de pile', lignes: ['« L12 · Compte.retirer » — cadre du fichier ouvert, cliquable', '« L7 · Main.main » — étranger, encre muette'] },
    ],
    declencheurs: ["L’action « Exécuter le fichier (simulé) » — l’annonce de l’échec précède la carte"],
    fermeture: 'Le tap hors cartes, Échap. La frappe la épargne ; le défilement la repositionne ; le voile ne la coupe pas.',
    strings: [
      { fr: "L’exécution simulée échoue — la trace suit le plan du fichier", en: 'The simulated run fails — the trace follows the plan of the file' },
    ],
  },
];

export const POPUP_PAR_ID: Record<GenreId, PopupSpec> = Object.fromEntries(
  POPUPS.map((p) => [p.id, p]),
) as Record<GenreId, PopupSpec>;

/* ---------------------------------------------------------------------------
 * La machine à états — portage exact de PopupController + PopupManager.
 * ------------------------------------------------------------------------- */

/** La famille des surfaces d’actions — une surface d’actions à la fois. */
export const FAMILLE_ACTIONS: Partial<Record<GenreId, GenreId[]>> = {
  COMPLETION_LIST: ['INTENTIONS_MENU', 'TOOLBAR_OVERFLOW', 'GABARITS', 'SAFE_MOVE', 'SUPPRESS_INSPECTION'],
  INTENTIONS_MENU: ['COMPLETION_LIST', 'TOOLBAR_OVERFLOW', 'GABARITS', 'SAFE_MOVE', 'SUPPRESS_INSPECTION', 'REFERENCES', 'HIERARCHY', 'SAFE_DELETE'],
  TOOLBAR_OVERFLOW: ['COMPLETION_LIST', 'INTENTIONS_MENU', 'GABARITS', 'SAFE_MOVE', 'SUPPRESS_INSPECTION', 'REFERENCES', 'HIERARCHY', 'SAFE_DELETE'],
  GABARITS: ['COMPLETION_LIST', 'INTENTIONS_MENU', 'TOOLBAR_OVERFLOW', 'SAFE_MOVE', 'SUPPRESS_INSPECTION', 'REFERENCES', 'HIERARCHY', 'SAFE_DELETE'],
  SAFE_MOVE: ['COMPLETION_LIST', 'INTENTIONS_MENU', 'TOOLBAR_OVERFLOW', 'GABARITS', 'SUPPRESS_INSPECTION', 'REFERENCES', 'HIERARCHY', 'SAFE_DELETE'],
  SUPPRESS_INSPECTION: ['COMPLETION_LIST', 'INTENTIONS_MENU', 'TOOLBAR_OVERFLOW', 'GABARITS', 'SAFE_MOVE', 'REFERENCES', 'HIERARCHY', 'SAFE_DELETE'],
  REFERENCES: ['INTENTIONS_MENU', 'TOOLBAR_OVERFLOW', 'GABARITS', 'SAFE_MOVE', 'SUPPRESS_INSPECTION', 'HIERARCHY', 'SAFE_DELETE'],
  HIERARCHY: ['INTENTIONS_MENU', 'TOOLBAR_OVERFLOW', 'GABARITS', 'SAFE_MOVE', 'SUPPRESS_INSPECTION', 'REFERENCES', 'SAFE_DELETE'],
  SAFE_DELETE: ['INTENTIONS_MENU', 'TOOLBAR_OVERFLOW', 'GABARITS', 'SAFE_MOVE', 'SUPPRESS_INSPECTION', 'REFERENCES', 'HIERARCHY'],
  SETTINGS_MENU: ['INTENTIONS_MENU', 'TOOLBAR_OVERFLOW', 'GABARITS', 'SAFE_MOVE', 'SUPPRESS_INSPECTION', 'REFERENCES', 'HIERARCHY', 'SAFE_DELETE'],
};

/** Ce que la frappe ferme (onKeystroke — la complétion reste, elle filtre). */
export const FRAPPE_FERME: GenreId[] = [
  'HOVER', 'DIAGNOSTIC_TOOLTIP', 'SELECTION_BAR', 'SELECTION_OVERFLOW',
  'COLOR_PICKER', 'TOOLBAR_OVERFLOW', 'REFERENCES', 'TYPE_INFO',
  'HIERARCHY', 'SAFE_DELETE', 'SETTINGS_MENU',
];

/** Ce que le défilement ferme (onScroll — ancre au curseur). */
export const DEFILEMENT_FERME: GenreId[] = [
  'HOVER', 'DIAGNOSTIC_TOOLTIP', 'COLOR_PICKER', 'REFERENCES',
  'TYPE_INFO', 'HIERARCHY', 'SAFE_DELETE',
];

/** Ce que le tap hors cartes referme (surToucheHorsCartes). */
export const TAP_HORS_CARTES_FERME: GenreId[] = [
  'HOVER', 'DIAGNOSTIC_TOOLTIP', 'COLOR_PICKER', 'INTENTIONS_MENU',
  'SELECTION_OVERFLOW', 'TOOLBAR_OVERFLOW', 'REFERENCES', 'TYPE_INFO',
  'HIERARCHY', 'EXTERNAL_DOC', 'GABARITS', 'SAFE_DELETE',
  'SAFE_MOVE', 'SUPPRESS_INSPECTION', 'EXCEPTION_DETAILS',
  'SETTINGS_MENU',
];

/** Ce qu’une feuille modale coupe (SOUS_VOILE — le chrome vit avec le document). */
export const SOUS_VOILE: GenreId[] = [
  'COMPLETION_LIST', 'COMPLETION_DOC', 'SIGNATURE_HELP', 'HOVER',
  'DIAGNOSTIC_TOOLTIP', 'COLOR_PICKER', 'INTENTIONS_MENU', 'TOOLBAR_OVERFLOW',
  'SELECTION_OVERFLOW', 'SELECTION_BAR', 'REFERENCES', 'TYPE_INFO',
  'HIERARCHY', 'NOTIFICATION', 'EXTERNAL_DOC', 'GABARITS',
  'SAFE_DELETE', 'SAFE_MOVE', 'SUPPRESS_INSPECTION', 'SETTINGS_MENU',
];

/** L’ordre de l’échappement — la première carte interactive ouverte. */
export const ORDRE_CLAVIER: GenreId[] = [
  'COMPLETION_LIST', 'INTENTIONS_MENU', 'GABARITS', 'TOOLBAR_OVERFLOW',
  'SELECTION_OVERFLOW', 'REFERENCES', 'HIERARCHY', 'SAFE_DELETE',
  'SAFE_MOVE', 'SUPPRESS_INSPECTION', 'EXCEPTION_DETAILS',
];

/** Les éphémères — l’échappement sans carte interactive. */
export const EPHEMERES: GenreId[] = [
  'SIGNATURE_HELP', 'HOVER', 'DIAGNOSTIC_TOOLTIP', 'COLOR_PICKER',
  'TYPE_INFO', 'EXTERNAL_DOC', 'EXCEPTION_DETAILS',
];

/** Ouvre un genre — exclusions et préconditions exactes du contrôleur. */
export function ouvrir(ouverts: Set<GenreId>, genre: GenreId): Set<GenreId> {
  if (ouverts.has(genre)) return ouverts;
  const suivant = new Set(GenreIdList(ouverts));
  const chasses = FAMILLE_ACTIONS[genre];
  if (chasses) {
    for (const c of chasses) suivant.delete(c);
  } else if (genre === 'COMPLETION_DOC' && !ouverts.has('COMPLETION_LIST')) {
    return ouverts;
  } else if (genre === 'SELECTION_OVERFLOW' && !ouverts.has('SELECTION_BAR')) {
    return ouverts;
  }
  suivant.add(genre);
  return suivant;
}

/** Ferme un genre — cascades : la doc suit sa liste, le débordement suit sa barre. */
export function fermer(ouverts: Set<GenreId>, genre: GenreId): Set<GenreId> {
  const suivant = new Set(GenreIdList(ouverts));
  suivant.delete(genre);
  if (genre === 'COMPLETION_LIST') suivant.delete('COMPLETION_DOC');
  if (genre === 'SELECTION_BAR') suivant.delete('SELECTION_OVERFLOW');
  return suivant;
}

/** La frappe — ferme les bulles qui décrivent, la complétion filtre. */
export function frapper(ouverts: Set<GenreId>): Set<GenreId> {
  const suivant = new Set(GenreIdList(ouverts));
  for (const g of FRAPPE_FERME) suivant.delete(g);
  return suivant;
}

/** Le défilement — ferme l’ancre au curseur, repositionne le reste. */
export function defiler(ouverts: Set<GenreId>): Set<GenreId> {
  const suivant = new Set(GenreIdList(ouverts));
  for (const g of DEFILEMENT_FERME) suivant.delete(g);
  return suivant;
}

/** Le tap hors cartes — referme cartes d’actions et bulles éphémères. */
export function taperHorsCartes(ouverts: Set<GenreId>): Set<GenreId> {
  const suivant = new Set(GenreIdList(ouverts));
  for (const g of TAP_HORS_CARTES_FERME) suivant.delete(g);
  return suivant;
}

/** Le voile modal — l’intelligence et les actions cèdent, le chrome demeure. */
export function poserVoile(ouverts: Set<GenreId>): Set<GenreId> {
  const suivant = new Set(GenreIdList(ouverts));
  for (const g of SOUS_VOILE) suivant.delete(g);
  return suivant;
}

/** L’échappement — la première carte interactive, sinon le premier éphémère. */
export function echapper(ouverts: Set<GenreId>): Set<GenreId> {
  for (const g of ORDRE_CLAVIER) {
    if (ouverts.has(g)) return fermer(ouverts, g);
  }
  for (const g of EPHEMERES) {
    if (ouverts.has(g)) return fermer(ouverts, g);
  }
  return ouverts;
}

/** Ferme tout — closeAll. */
export function fermerTout(ouverts: Set<GenreId>): Set<GenreId> {
  return new Set<GenreId>();
}

function GenreIdList(s: Set<GenreId>): GenreId[] {
  return Array.from(s) as GenreId[];
}

/** Différence lisible : qu’est-ce qui s’est fermé entre deux états ? */
export function fermesEntre(avant: Set<GenreId>, apres: Set<GenreId>): GenreId[] {
  return GenreIdList(avant).filter((g) => !apres.has(g));
}
