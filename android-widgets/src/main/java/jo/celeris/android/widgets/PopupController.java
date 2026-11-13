package jo.celeris.android.widgets;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * La machine à états des popups de l'éditeur (M7.5, §7) : qui est
 * ouvert, qui s'exclut, qui cohabite — les règles de la couche, pures
 * et testées, avant la première vue.
 *
 * <p>Règles : la liste de complétion et le menu d'intentions
 * s'excluent mutuellement ; l'aide aux paramètres cohabite avec la
 * complétion (au-dessus de la ligne) ; le survol et l'info-bulle se
 * ferment à la frappe et au défilement ; la complétion FILTRE
 * localement à la frappe — elle reste ouverte, la requête ne repart
 * pas ; la barre de recherche ne se ferme que sur demande. Les genres
 * (M7.5, reprise — vingt-six depuis la carte des détails d'exception,
 * la dixième surface réveillée du signal « terminez le reste ») : la
 * documentation de complétion n'existe pas sans sa liste — fermer la
 * liste ferme la documentation ; la barre de sélection cède à la frappe
 * (la frappe remplace la sélection) mais survit au défilement, et son
 * MENU DE DÉBORDEMENT ne survit jamais à sa barre — la barre fermée
 * l'emporte ; la bande d'erreurs ne cède ni à la frappe ni au défilement
 * — elle vit avec les diagnostics, l'hôte l'ouvre et la ferme.</p>
 *
 * <p>PIEGE-27 par construction : le contrôleur ne connaît AUCUNE vue —
 * le focus d'entrée reste à l'éditeur, les vues ne font qu'obéir aux
 * états.</p>
 */
public final class PopupController {

    /** Les popups de l'éditeur, chacun son régime d'exclusion. */
    public enum PopupKind {

        /** La liste de complétion — exclue le menu d'intentions. */
        COMPLETION_LIST,

        /** Le menu d'intentions — exclut la liste de complétion. */
        INTENTIONS_MENU,

        /** L'aide aux paramètres — cohabite avec la complétion. */
        SIGNATURE_HELP,

        /** La documentation de complétion — n'existe pas sans sa
         *  liste, fermée avec elle. */
        COMPLETION_DOC,

        /** Le survol de symbole — fermé à la frappe et au défilement. */
        HOVER,

        /** L'info-bulle de diagnostic — fermée à la frappe et au défilement. */
        DIAGNOSTIC_TOOLTIP,

        /** Le fil d'Ariane — suit le curseur. */
        BREADCRUMB,

        /** La barre de recherche — fermée sur demande seule. */
        SEARCH_BAR,

        /** La barre de sélection — cède à la frappe (la frappe remplace
         *  la sélection), survit au défilement. */
        SELECTION_BAR,

        /** Le menu de débordement de la barre de sélection — n'existe
         *  pas sans sa barre, fermé avec elle ; il vit les mêmes
         *  régimes qu'elle (cède à la frappe, survit au défilement en
         *  se repositionnant). */
        SELECTION_OVERFLOW,

        /** La bande d'erreurs — vit avec les diagnostics : ni la frappe
         *  ni le défilement ne la ferment. */
        ERROR_BAND,

        /** La minimap — ornement du bord droit (chantier C9) : vit avec
         *  le document, ni la frappe ni le défilement ne la ferment. */
        MINIMAP,

        /** Le défilement rapide — ornement du bord droit (chantier C9) :
         *  le pouce suit le défilement, le glissement l'emporte. */
        FAST_SCROLLER,

        /** La barre de touches de code (le dépôt de départ, 4.5.0) :
         *  ornement du bas de la scène AU-DESSUS DU CLAVIER — l'hôte la
         *  pose, ni la frappe ni le défilement ne la ferment (le
         *  clavier la remonte avec l'IME). */
        TOUCH_BAR,

        /** Le sélecteur de couleur (le signal du propriétaire du
         *  2026-11-13 : le color picker au double-clic d'un littéral)
         *  — fermé à la frappe et au défilement, comme le survol : le
         *  pick s'applique implicite, la carte ne survit pas au geste
         *  qui la quitte. */
        COLOR_PICKER,

        /** La carte de débordement de la barre d'application (le
         *  redressement de la toolbar) : PENDUE sous la puce au coin de
         *  scène, PAS au texte — elle vit son PROPRE genre depuis le
         *  chantier d'unification (elle détournait le menu
         *  d'intentions) : deux menus ne tiennent jamais (elle ferme la
         *  complétion et le menu du caret, eux la ferment), la frappe la
         *  ferme, le défilement l'épargne — l'ancre ne suit pas le
         *  texte, le tap hors cartes la referme. */
        TOOLBAR_OVERFLOW,

        /** La carte des usages — les références du symbole au curseur
         *  (catégorie C de l'inventaire, le signal du propriétaire du
         *  2026-11-13) : deux surfaces d'actions ne tiennent jamais
         *  (elle ferme le menu d'intentions et le débordement de la
         *  toolbar, eux la ferment), la frappe la ferme (elle décrit le
         *  symbole, la saisie remplace son objet), le défilement la
         *  ferme (son ancre vit au curseur), le tap hors cartes la
         *  referme — le choix d'une occurrence saute et la referme. */
        REFERENCES,

        /** L'info de type — le nom du type du symbole au curseur
         *  (catégorie A de l'inventaire, le fil rouge du guide du
         *  développeur) : elle vit les régimes du survol — fermée à
         *  la frappe et au défilement, refermée au tap hors cartes
         *  (elle décrit, elle ne choisit pas) ; elle cohabite avec
         *  tout : une bulle qui décrit ne chasse personne. */
        TYPE_INFO,

        /** La hiérarchie de types ou d'appels — l'arbre du symbole
         *  au curseur (catégorie C de l'inventaire, les moteurs
         *  vivaient, la surface dormait) : elle joint la famille des
         *  surfaces d'actions — elle ferme le menu d'intentions, le
         *  débordement de la toolbar ET la carte des usages, eux la
         *  ferment (une surface d'actions à la fois) ; la frappe la
         *  ferme (la saisie remplace son objet), le défilement la
         *  ferme (son ancre vit au curseur), le tap hors cartes la
         *  referme — le choix d'un sommet saute et la referme. */
        HIERARCHY,

        /** La notification — la bulle éphémère du coin bas-droit
         *  (catégorie G de l'inventaire, la forme restait à décider)
         *  : elle annonce, elle ne décrit pas le texte — le régime
         *  UNIQUE — la frappe, le défilement et le tap hors cartes
         *  l'épargnent (l'annonce vit son quart, le geste ne
         *  l'interrompt pas), l'HORLOGE la referme à son heure (le
         *  tap qui la prend aussi) ; aucune exclusion : elle cohabite
         *  avec tout ; le voile modal la coupe — la feuille la
         *  recouvre. */
        NOTIFICATION,

        /** La documentation externe — la documentation du symbole
         *  hors contexte de complétion (catégorie A de l'inventaire,
         *  la carte de documentation se dérive) : elle vit le régime
         *  de la CONSULTATION — la frappe ne la chasse pas (on tape à
         *  côté), le défilement ne la ferme pas non plus (elle se
         *  REPOSITIONNE chez l'hôte, elle suit le curseur), le tap
         *  hors cartes la referme (la consultation cède au geste qui
         *  déplace le curseur), l'échappement la referme comme bulle
         *  éphémère ; aucune exclusion : une documentation ne chasse
         *  personne. */
        EXTERNAL_DOC,

        /** La carte de génération et de gabarits — les actions de
         *  génération de code au caret (catégorie D de l'inventaire,
         *  les moteurs vivaient, la surface dormait) : constructeur,
         *  accesseurs, equals/hashCode/toString lus des champs de la
         *  classe englobante, l'entourage de la sélection (try/catch,
         *  if) et le gabarit de méthode à trous liés. Elle joint la
         *  famille des surfaces d'actions — elle ferme le menu
         *  d'intentions, le débordement de la toolbar, la carte des
         *  usages et la hiérarchie, eux la ferment (une surface
         *  d'actions à la fois) ; la saisie la chasse quand la
         *  complétion s'ouvre (un menu ne survit pas à la liste), le
         *  tap hors cartes la referme — le défilement la repositionne
         *  (le genre suit les régimes du menu du caret). */
        GABARITS,

        /** La carte des usages qui barrent — la suppression sûre
         *  refusée (catégorie D de l'inventaire, le moteur B-23
         *  servait les verdicts, la surface dormait) : les usages de
         *  la déclaration au caret se lisent AVANT de trancher — la
         *  carte les montre, chacun saute à son lieu, la suppression
         *  attend. Elle vit les régimes de la carte des usages :
         *  famille des surfaces d'actions (elle ferme les menus, eux
         *  la ferment), la frappe la ferme (la saisie remplace son
         *  objet), le défilement la ferme (son ancre vit au curseur),
         *  le tap hors cartes la referme — elle cohabite avec la
         *  complétion (elle décrit, la saisie la chassera). */
        SAFE_DELETE,

        /** La carte des cibles du déménagement — le déplacement sûr
         *  d'une méthode dans une autre classe du fichier (catégorie
         *  D de l'inventaire, le moteur B-23 servait les plans, la
         *  surface dormait) : les types candidats se proposent, le
         *  choix tente le plan — les conditions de sûreté seules
         *  décident, le refus s'annonce sans raison inventée. Elle
         *  vit les régimes du menu du caret : famille des surfaces
         *  d'actions, la complétion la chasse, la frappe seule et le
         *  défilement l'épargnent (elle se repositionne), le tap
         *  hors cartes la referme. */
        SAFE_MOVE,

        /** La carte de masquage d'inspection — la règle portée par
         *  l'avertissement de la ligne se masque en ligne (catégorie B
         *  de l'inventaire, « Suppress Inspection », le geste dormait
         *  : la marque naît, le lint l'honore à sa relance) : les
         *  diagnostics réglés de la ligne du caret s'y proposent, le
         *  choix pose le commentaire, l'analyse se relance — la règle
         *  masquée ne se rapporte plus, les autres demeurent. Elle vit
         *  les régimes du menu du caret : famille des surfaces
         *  d'actions, la complétion la chasse, la frappe seule et le
         *  défilement l'épargnent (elle se repositionne), le tap hors
         *  cartes la referme. */
        SUPPRESS_INSPECTION,

        /** Les détails d'exception — la carte de l'échec d'exécution
         *  (catégorie E de l'inventaire, « Exception Details », la
         *  surface dormait : la trace se réveille, l'analyseur de
         *  piles sert les cadres) : le type à l'encre d'erreur, le
         *  message, la pile — le cadre CONNU du fichier ouvert se
         *  prend et son saut court, l'étranger vit muet. Elle vit le
         *  régime de la CONSULTATION — la frappe ne la chasse pas, le
         *  défilement la REPOSITIONNE chez l'hôte, le tap hors cartes
         *  la referme, l'échappement aussi ; aucune exclusion : la
         *  trace ne chasse personne. */
        EXCEPTION_DETAILS
    }

    private final Set<PopupKind> ouverts = EnumSet.noneOf(PopupKind.class);

    /** La famille des surfaces d'actions — le genre et ceux qu'il
     *  chasse à l'ouverture : une surface d'actions à la fois. Les
     *  MENUS (intentions, débordement, gabarits, cibles du
     *  déménagement) cèdent aussi à la complétion — la saisie prend
     *  le relais ; les cartes qui DÉCRIVENT (usages, hiérarchie,
     *  usages qui barrent) cohabitent avec elle. */
    private static final Map<PopupKind, PopupKind[]> FAMILLE_ACTIONS =
            Map.of(
                    PopupKind.COMPLETION_LIST,
                    new PopupKind[] {
                            PopupKind.INTENTIONS_MENU,
                            PopupKind.TOOLBAR_OVERFLOW,
                            PopupKind.GABARITS,
                            PopupKind.SAFE_MOVE,
                            PopupKind.SUPPRESS_INSPECTION},
                    PopupKind.INTENTIONS_MENU,
                    new PopupKind[] {
                            PopupKind.COMPLETION_LIST,
                            PopupKind.TOOLBAR_OVERFLOW,
                            PopupKind.GABARITS,
                            PopupKind.SAFE_MOVE,
                            PopupKind.SUPPRESS_INSPECTION,
                            PopupKind.REFERENCES,
                            PopupKind.HIERARCHY,
                            PopupKind.SAFE_DELETE},
                    PopupKind.TOOLBAR_OVERFLOW,
                    new PopupKind[] {
                            PopupKind.COMPLETION_LIST,
                            PopupKind.INTENTIONS_MENU,
                            PopupKind.GABARITS,
                            PopupKind.SAFE_MOVE,
                            PopupKind.SUPPRESS_INSPECTION,
                            PopupKind.REFERENCES,
                            PopupKind.HIERARCHY,
                            PopupKind.SAFE_DELETE},
                    PopupKind.GABARITS,
                    new PopupKind[] {
                            PopupKind.COMPLETION_LIST,
                            PopupKind.INTENTIONS_MENU,
                            PopupKind.TOOLBAR_OVERFLOW,
                            PopupKind.SAFE_MOVE,
                            PopupKind.SUPPRESS_INSPECTION,
                            PopupKind.REFERENCES,
                            PopupKind.HIERARCHY,
                            PopupKind.SAFE_DELETE},
                    PopupKind.SAFE_MOVE,
                    new PopupKind[] {
                            PopupKind.COMPLETION_LIST,
                            PopupKind.INTENTIONS_MENU,
                            PopupKind.TOOLBAR_OVERFLOW,
                            PopupKind.GABARITS,
                            PopupKind.SUPPRESS_INSPECTION,
                            PopupKind.REFERENCES,
                            PopupKind.HIERARCHY,
                            PopupKind.SAFE_DELETE},
                    PopupKind.SUPPRESS_INSPECTION,
                    new PopupKind[] {
                            PopupKind.COMPLETION_LIST,
                            PopupKind.INTENTIONS_MENU,
                            PopupKind.TOOLBAR_OVERFLOW,
                            PopupKind.GABARITS,
                            PopupKind.SAFE_MOVE,
                            PopupKind.REFERENCES,
                            PopupKind.HIERARCHY,
                            PopupKind.SAFE_DELETE},
                    PopupKind.REFERENCES,
                    new PopupKind[] {
                            PopupKind.INTENTIONS_MENU,
                            PopupKind.TOOLBAR_OVERFLOW,
                            PopupKind.GABARITS,
                            PopupKind.SAFE_MOVE,
                            PopupKind.SUPPRESS_INSPECTION,
                            PopupKind.HIERARCHY,
                            PopupKind.SAFE_DELETE},
                    PopupKind.HIERARCHY,
                    new PopupKind[] {
                            PopupKind.INTENTIONS_MENU,
                            PopupKind.TOOLBAR_OVERFLOW,
                            PopupKind.GABARITS,
                            PopupKind.SAFE_MOVE,
                            PopupKind.SUPPRESS_INSPECTION,
                            PopupKind.REFERENCES,
                            PopupKind.SAFE_DELETE},
                    PopupKind.SAFE_DELETE,
                    new PopupKind[] {
                            PopupKind.INTENTIONS_MENU,
                            PopupKind.TOOLBAR_OVERFLOW,
                            PopupKind.GABARITS,
                            PopupKind.SAFE_MOVE,
                            PopupKind.SUPPRESS_INSPECTION,
                            PopupKind.REFERENCES,
                            PopupKind.HIERARCHY});

    /**
     * Ouvre le popup donné : les exclusions s'appliquent — la
     * complétion et le menu d'intentions ne tiennent jamais ensemble,
     * la carte de la toolbar non plus (deux menus ne tiennent jamais,
     * le chantier d'unification). La documentation de complétion
     * n'ouvre que sur une liste déjà ouverte — elle n'existe pas sans
     * sa liste ; le menu de débordement n'ouvre que sur une barre de
     * sélection déjà ouverte.
     *
     * @param kind le popup à ouvrir
     * @return vrai si l'état a changé
     */
    public boolean open(PopupKind kind) {
        return !ouvrirRendus(kind).isEmpty();
    }

    /**
     * Ouvre le popup donné et rend TOUT ce qui a changé — le genre
     * ouvert et les genres exclus : le coordinateur y retire d'un seul
     * tenant les vues des exclus, l'état et la couche ne divergent
     * jamais (le chantier d'unification : les zombies d'état ne
     * s'y rencontrent plus).
     *
     * @param kind le popup à ouvrir
     * @return les genres ouverts ou exclus — vide quand rien ne change
     */
    public Set<PopupKind> ouvrirRendus(PopupKind kind) {
        Set<PopupKind> rendus = EnumSet.noneOf(PopupKind.class);
        if (ouverts.contains(kind)) {
            return rendus;
        }
        PopupKind[] chasses = FAMILLE_ACTIONS.get(kind);
        if (chasses != null) {
            rendus.addAll(fermerParExclusion(chasses));
        } else if (kind == PopupKind.COMPLETION_DOC
                && !ouverts.contains(PopupKind.COMPLETION_LIST)) {
            return rendus;
        } else if (kind == PopupKind.SELECTION_OVERFLOW
                && !ouverts.contains(PopupKind.SELECTION_BAR)) {
            return rendus;
        }
        ouverts.add(kind);
        rendus.add(kind);
        return rendus;
    }

    /** Les genres exclus qui se fermaient — l'ouverture d'un menu
     *  chasse ses concurrents, l'état les rend. */
    private Set<PopupKind> fermerParExclusion(PopupKind... genres) {
        Set<PopupKind> fermes = EnumSet.noneOf(PopupKind.class);
        for (PopupKind genre : genres) {
            if (ouverts.remove(genre)) {
                fermes.add(genre);
            }
        }
        return fermes;
    }

    /**
     * Ferme le popup donné. Fermer la liste de complétion ferme sa
     * documentation — la documentation ne reste jamais seule ; fermer
     * la barre de sélection ferme son menu de débordement — le menu
     * ne survit jamais à sa barre.
     *
     * @param kind le popup à fermer
     * @return vrai si l'état a changé
     */
    public boolean close(PopupKind kind) {
        return !closeRendus(kind).isEmpty();
    }

    /**
     * Ferme le popup donné et rend TOUT ce qui s'est fermé — le genre
     * demandé et ses cascades (la documentation suit sa liste, le
     * débordement suit sa barre) : le coordinateur y retire d'un seul
     * tenant les vues des fermés, l'état et la couche ne divergent
     * jamais.
     *
     * @param kind le popup à fermer
     * @return les genres fermés — vide quand tout était déjà fermé
     */
    public Set<PopupKind> closeRendus(PopupKind kind) {
        Set<PopupKind> fermes = EnumSet.noneOf(PopupKind.class);
        if (ouverts.remove(kind)) {
            fermes.add(kind);
        }
        if (kind == PopupKind.COMPLETION_LIST) {
            if (ouverts.remove(PopupKind.COMPLETION_DOC)) {
                fermes.add(PopupKind.COMPLETION_DOC);
            }
        } else if (kind == PopupKind.SELECTION_BAR) {
            if (ouverts.remove(PopupKind.SELECTION_OVERFLOW)) {
                fermes.add(PopupKind.SELECTION_OVERFLOW);
            }
        }
        return fermes;
    }

    /** Ferme tout — la session se détache, la couche se vide.
     *
     * @return les genres qui se fermaient — la couche les retire, la
     * vue ne survit jamais à son état
     */
    public Set<PopupKind> closeAll() {
        Set<PopupKind> fermes = ouverts.isEmpty()
                ? EnumSet.noneOf(PopupKind.class) : EnumSet.copyOf(ouverts);
        ouverts.clear();
        return fermes;
    }

    /**
     * La frappe : le survol, l'info-bulle et la barre de sélection se
     * ferment (la frappe remplace la sélection — son menu de
     * débordement suit), la carte de la toolbar cède aussi (un menu
     * ne survit pas à la saisie), la carte des usages cède pareil
     * (elle décrit le symbole, la saisie remplace son objet), la
     * complétion RESTE
     * — elle filtre localement, la requête ne repart pas ; la bande
     * d'erreurs survit — elle vit avec les diagnostics.
     *
     * @return les genres qui se fermaient — la couche les retire, la
     * vue ne survit jamais à son état (le zombie ne s'y rencontre
     * jamais)
     */
    public Set<PopupKind> onKeystroke() {
        return fermer(PopupKind.HOVER, PopupKind.DIAGNOSTIC_TOOLTIP,
                PopupKind.SELECTION_BAR, PopupKind.SELECTION_OVERFLOW,
                PopupKind.COLOR_PICKER, PopupKind.TOOLBAR_OVERFLOW,
                PopupKind.REFERENCES, PopupKind.TYPE_INFO,
                PopupKind.HIERARCHY, PopupKind.SAFE_DELETE);
    }

    /**
     * Le défilement : le survol, l'info-bulle et la carte des usages
     * se ferment (son ancre vit au curseur, elle ne le suit pas) — les
     * popups qui restent se REPOSITIONNENT, ils ne se ferment pas.
     *
     * @return les genres qui se fermaient — la couche les retire, la
     * vue ne survit jamais à son état
     */
    public Set<PopupKind> onScroll() {
        return fermer(PopupKind.HOVER, PopupKind.DIAGNOSTIC_TOOLTIP,
                PopupKind.COLOR_PICKER, PopupKind.REFERENCES,
                PopupKind.TYPE_INFO, PopupKind.HIERARCHY,
                PopupKind.SAFE_DELETE);
    }

    /**
     * Le tap HORS CARTES (le chantier d'unification, phase 5 de la
     * mission) : les cartes d'actions et les bulles éphémères cèdent —
     * le survol, l'info-bulle, le sélecteur de couleur, le menu
     * d'intentions, les débordements et la carte des usages ; la
     * complétion, la signature, le fil d'Ariane et le chrome suivent
     * le curseur ou le document, ils ne cèdent pas au geste qui
     * déplace le curseur.
     *
     * @return les genres qui se fermaient — la couche les retire
     */
    public Set<PopupKind> surToucheHorsCartes() {
        return fermer(PopupKind.HOVER, PopupKind.DIAGNOSTIC_TOOLTIP,
                PopupKind.COLOR_PICKER, PopupKind.INTENTIONS_MENU,
                PopupKind.SELECTION_OVERFLOW, PopupKind.TOOLBAR_OVERFLOW,
                PopupKind.REFERENCES, PopupKind.TYPE_INFO,
                PopupKind.HIERARCHY, PopupKind.EXTERNAL_DOC,
                PopupKind.GABARITS, PopupKind.SAFE_DELETE,
                PopupKind.SAFE_MOVE, PopupKind.SUPPRESS_INSPECTION,
                PopupKind.EXCEPTION_DETAILS);
    }

    /** Les genres donnés se ferment — la copie fraîche des fermés se
     *  rend, l'état reste au contrôleur. */
    private Set<PopupKind> fermer(PopupKind... genres) {
        Set<PopupKind> fermes = EnumSet.noneOf(PopupKind.class);
        for (PopupKind genre : genres) {
            if (ouverts.remove(genre)) {
                fermes.add(genre);
            }
        }
        return fermes;
    }

    /** Vrai si le popup donné est ouvert. */
    public boolean isOpen(PopupKind kind) {
        return ouverts.contains(kind);
    }

    /** Les popups ouverts — copie défensive, l'état reste au contrôleur. */
    public Set<PopupKind> openPopups() {
        return EnumSet.copyOf(ouverts);
    }
}
