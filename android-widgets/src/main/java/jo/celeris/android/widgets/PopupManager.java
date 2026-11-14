package jo.celeris.android.widgets;

import android.view.KeyEvent;
import android.view.View;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Le coordinateur des popups (le chantier d'unification, phase 3 de la
 * mission) : l'UNIQUE écrivain de l'état ET des vues. La machine à états
 * ({@link PopupController}) décide qui vit, la couche ({@link PopupLayer})
 * porte les cartes — le manager tient les DEUX d'un seul tenant : toute
 * ouverture rend ses exclus (leurs cartes quittent la couche), toute
 * fermeture rend ses cascades (la documentation suit sa liste, le
 * débordement suit sa barre) — le zombie d'état ne se rencontre plus,
 * l'hôte ne code plus jamais les cascades en double.
 *
 * <p>Le montage tient en trois temps : {@link #lier} une vue par genre
 * (JAMAIS une vue pour deux genres — le partage d'instances désynchronisait
 * l'état, l'audit P1), déclencher par
 * {@link #montrer} / {@link #fermer} / {@link #surFrappe} /
 * {@link #surDefilement}, déléguer le clavier par {@link #surTouche} (la
 * carte interactive reçoit flèches et validation, l'éditeur garde le
 * focus — PIEGE-27). La mesure unique ({@link #taille}) sert les ancres
 * de l'hôte ; le tap hors cartes ferme les genres qui cèdent ; une
 * feuille modale qui s'ouvre appelle {@link #fermerSousVoile}.</p>
 *
 * <p>Le manager ne connaît NI les moteurs (LSP, index), NI l'éditeur :
 * le contenu vit chez l'hôte, la géométrie chez lui — la séparation
 * constatée de l'audit demeure, seule la BOUCLE état→vues devient
 * commune.</p>
 */
public final class PopupManager {

    /**
     * L'ordre du routage clavier : la complétion d'abord (elle reçoit la
     * frappe de saisie), les menus ensuite — ils ne tiennent jamais
     * ensemble, l'ordre ne départage que les débordements —, la carte
     * des usages et la hiérarchie en dernier (elles ne tiennent jamais
     * avec les menus, le routage ne les sert que seules — elles ne
     * tiennent jamais ensemble non plus, l'ordre ne départage pas). La
     * carte de gabarits vit avec les menus du caret ; la carte des
     * usages qui barrent et celle des cibles du déménagement ferment
     * la file — elles ne tiennent jamais avec les précédentes ; la
     * carte de masquage d'inspection vit avec les menus du caret.
     */
    private static final PopupController.PopupKind[] ORDRE_CLAVIER = {
            PopupController.PopupKind.COMPLETION_LIST,
            PopupController.PopupKind.INTENTIONS_MENU,
            PopupController.PopupKind.GABARITS,
            PopupController.PopupKind.TOOLBAR_OVERFLOW,
            PopupController.PopupKind.SELECTION_OVERFLOW,
            PopupController.PopupKind.REFERENCES,
            PopupController.PopupKind.HIERARCHY,
            PopupController.PopupKind.SAFE_DELETE,
            PopupController.PopupKind.SAFE_MOVE,
            PopupController.PopupKind.SUPPRESS_INSPECTION,
            PopupController.PopupKind.EXCEPTION_DETAILS};

    /** Ce qu'une feuille modale coupe : l'intelligence et les actions
     *  cèdent au voile — le fil d'Ariane et le chrome (bande, minimap,
     *  pouce, barre de touches) vivent avec le document. */
    private static final PopupController.PopupKind[] SOUS_VOILE = {
            PopupController.PopupKind.COMPLETION_LIST,
            PopupController.PopupKind.COMPLETION_DOC,
            PopupController.PopupKind.SIGNATURE_HELP,
            PopupController.PopupKind.HOVER,
            PopupController.PopupKind.DIAGNOSTIC_TOOLTIP,
            PopupController.PopupKind.COLOR_PICKER,
            PopupController.PopupKind.INTENTIONS_MENU,
            PopupController.PopupKind.TOOLBAR_OVERFLOW,
            PopupController.PopupKind.SELECTION_OVERFLOW,
            PopupController.PopupKind.SELECTION_BAR,
            PopupController.PopupKind.REFERENCES,
            PopupController.PopupKind.TYPE_INFO,
            PopupController.PopupKind.HIERARCHY,
            PopupController.PopupKind.NOTIFICATION,
            PopupController.PopupKind.EXTERNAL_DOC,
            PopupController.PopupKind.GABARITS,
            PopupController.PopupKind.SAFE_DELETE,
            PopupController.PopupKind.SAFE_MOVE,
            PopupController.PopupKind.SUPPRESS_INSPECTION,
            PopupController.PopupKind.SETTINGS_MENU};

    /** Ce que l'échappement referme quand aucune carte interactive ne
     *  tient — les bulles qui suivent le contexte et les cartes de
     *  consultation qui décrivent sans choisir, pas le chrome (l'annonce
     *  éphémère vit son quart, seule son heure la conclut). */
    private static final PopupController.PopupKind[] EPHEMERES = {
            PopupController.PopupKind.SIGNATURE_HELP,
            PopupController.PopupKind.HOVER,
            PopupController.PopupKind.DIAGNOSTIC_TOOLTIP,
            PopupController.PopupKind.COLOR_PICKER,
            PopupController.PopupKind.TYPE_INFO,
            PopupController.PopupKind.EXTERNAL_DOC,
            PopupController.PopupKind.EXCEPTION_DETAILS};

    private final PopupController controleur;
    private final PopupLayer couche;
    /** La vue liée par genre — une vue par genre, un genre par vue. */
    private final Map<PopupController.PopupKind, View> vues =
            new EnumMap<>(PopupController.PopupKind.class);

    /**
     * Construit le coordinateur sur la machine et la couche montées par
     * l'hôte (le même couple que l'atelier posait — le manager prend la
     * boucle, pas la propriété) : l'avis du tap hors cartes s'y branche
     * d'entrée, les genres qui cèdent referment leurs cartes.
     *
     * @param controleur la machine à états des popups
     * @param couche     la couche porte-popups posée sur l'éditeur
     */
    public PopupManager(PopupController controleur, PopupLayer couche) {
        this.controleur = controleur;
        this.couche = couche;
        couche.setSurToucheHorsCartes(this::surToucheHorsCartes);
    }

    /**
     * Lie une vue à son genre — UNE vue par genre, UN genre par vue : la
     * même carte sous deux genres désynchronisait l'état de la couche
     * (l'audit P1 : le survol volait la documentation de complétion) ;
     * le lien l'interdit dès le montage.
     *
     * @param genre le genre à lier
     * @param vue   la carte du genre
     * @throws IllegalArgumentException si la vue vit déjà sous un autre genre
     */
    public void lier(PopupController.PopupKind genre, View vue) {
        for (Map.Entry<PopupController.PopupKind, View> lien : vues.entrySet()) {
            if (lien.getValue() == vue && lien.getKey() != genre) {
                throw new IllegalArgumentException(genre + " ne peut pas lier "
                        + "la carte : elle vit déjà sous " + lien.getKey()
                        + " — une carte ne sert jamais deux genres");
            }
        }
        vues.put(genre, vue);
    }

    /** La vue liée du genre, null s'il n'en vit aucune. */
    public View vue(PopupController.PopupKind genre) {
        return vues.get(genre);
    }

    /** La machine coordonnée — les bancs de l'hôte y lisent. */
    public PopupController controleur() {
        return controleur;
    }

    /** La couche coordonnée — l'hôte y mesure, la scène la porte. */
    public PopupLayer couche() {
        return couche;
    }

    /** Vrai si le genre est ouvert — l'état seul décide, la couche suit. */
    public boolean estOuvert(PopupController.PopupKind genre) {
        return controleur.isOpen(genre);
    }

    /**
     * La mesure du genre — LA mesure unique : l'hôte ancre avec, la
     * couche re-mesure à sa pose (la double mesure est arithmétique, le
     * contenu n'y re-naît pas).
     *
     * @param genre le genre à mesurer
     * @return la taille mesurée sans contrainte
     * @throws IllegalStateException si le genre n'est pas lié
     */
    public PopupPlacer.Cadre taille(PopupController.PopupKind genre) {
        View vue = carteDe(genre);
        vue.measure(View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED));
        return new PopupPlacer.Cadre(0, 0, vue.getMeasuredWidth(),
                vue.getMeasuredHeight());
    }

    /**
     * Montre le genre lié à la position donnée : l'état s'ouvre — les
     * exclus rendus voient leurs cartes quitter la couche AVANT la pose
     * (deux menus ne tiennent jamais) — puis la carte se pose bornée.
     * Le genre refusé par la machine (la documentation sans sa liste,
     * le débordement sans sa barre) ne se montre pas : l'état et la
     * couche restent d'un seul tenant.
     *
     * @param genre le genre à montrer
     * @param x     l'abscisse de pose, en pixels
     * @param y     l'ordonnée de pose, en pixels
     * @return vrai si l'ÉTAT a changé — faux au re-pose (la carte se
     *         re-montre quand même : le filtre de complétion)
     */
    public boolean montrer(PopupController.PopupKind genre, int x, int y) {
        View vue = carteDe(genre);
        Set<PopupController.PopupKind> rendus = controleur.ouvrirRendus(genre);
        if (rendus.isEmpty() && !controleur.isOpen(genre)) {
            // la machine refuse : la carte ne se pose pas (l'état et la
            // couche ne divergent jamais)
            return false;
        }
        for (PopupController.PopupKind sortant : rendus) {
            if (sortant != genre) {
                couche.dismiss(sortant);
            }
        }
        couche.show(genre, vue, x, y);
        return rendus.contains(genre);
    }

    /**
     * Ferme le genre — les cascades comprises (la documentation suit sa
     * liste, le débordement suit sa barre) : l'état et les cartes
     * ferment d'un seul appel.
     *
     * @param genre le genre à fermer
     * @return vrai si quelque chose s'est fermé
     */
    public boolean fermer(PopupController.PopupKind genre) {
        Set<PopupController.PopupKind> fermes = controleur.closeRendus(genre);
        retirer(fermes);
        return !fermes.isEmpty();
    }

    /**
     * Ferme tout — la session se détache, la couche se vide : les genres
     * fermés se rendent, l'hôte n'a plus rien à retirer.
     *
     * @return les genres qui se fermaient
     */
    public Set<PopupController.PopupKind> fermerTout() {
        Set<PopupController.PopupKind> fermes = controleur.closeAll();
        retirer(fermes);
        return fermes;
    }

    /**
     * La frappe déléguée : la machine rend ses régimes, le manager
     * retire les cartes des fermés — la vue ne survit jamais à son état.
     *
     * @return les genres qui se fermaient
     */
    public Set<PopupController.PopupKind> surFrappe() {
        Set<PopupController.PopupKind> fermes = controleur.onKeystroke();
        retirer(fermes);
        return fermes;
    }

    /**
     * Le défilement délégué : le survol et l'info-bulle se ferment, les
     * cartes restantes se repositionnent chez l'hôte — le manager
     * retire les cartes des fermés.
     *
     * @return les genres qui se fermaient
     */
    public Set<PopupController.PopupKind> surDefilement() {
        Set<PopupController.PopupKind> fermes = controleur.onScroll();
        retirer(fermes);
        return fermes;
    }

    /**
     * La feuille modale qui s'ouvre coupe l'intelligence et les actions
     * de la couche — une complétion ne survit jamais sous le voile ; le
     * fil d'Ariane et le chrome vivent avec le document, ils restent.
     *
     * @return les genres qui se fermaient
     */
    public Set<PopupController.PopupKind> fermerSousVoile() {
        Set<PopupController.PopupKind> fermes =
                EnumSet.noneOf(PopupController.PopupKind.class);
        for (PopupController.PopupKind genre : SOUS_VOILE) {
            fermes.addAll(controleur.closeRendus(genre));
        }
        retirer(fermes);
        return fermes;
    }

    /**
     * La touche matérielle déléguée par l'éditeur (PIEGE-27 : il garde
     * le focus, il rend le verdict des touches aux popups) : la
     * première carte interactive ouverte reçoit les flèches, les pages
     * (PAGE_UP, PAGE_DOWN — le pas étendu, une page par unité : la
     * carte qui ne pagine pas décline) et la validation (Enter, Tab —
     * la complétion ouverte précède le fantôme IA) ; l'échappement
     * referme la première carte interactive, sinon les bulles
     * éphémères. Toute autre touche retourne faux — l'éditeur suit sa
     * vie normale.
     *
     * @param keyCode le code de la touche déléguée
     * @return vrai si la touche a servi un popup
     */
    public boolean surTouche(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            return surEchappement();
        }
        for (PopupController.PopupKind genre : ORDRE_CLAVIER) {
            if (controleur.isOpen(genre) && surToucheCarte(genre, keyCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Le détachement de fin de vie : tout se ferme, l'avis du tap hors
     * cartes se coupe, les liens se dissolvent — la scène peut recycler.
     */
    public void detacher() {
        fermerTout();
        couche.setSurToucheHorsCartes(null);
        vues.clear();
    }

    /** L'échappement : la première carte interactive se referme, sinon
     *  les bulles éphémères — jamais le chrome, jamais la barre. */
    private boolean surEchappement() {
        for (PopupController.PopupKind genre : ORDRE_CLAVIER) {
            if (controleur.isOpen(genre)) {
                fermer(genre);
                return true;
            }
        }
        boolean ferme = false;
        for (PopupController.PopupKind genre : EPHEMERES) {
            ferme |= fermer(genre);
        }
        return ferme;
    }

    /** La touche servie par la carte interactive du genre — la carte
     *  décline, la touche retourne à l'éditeur. */
    private boolean surToucheCarte(PopupController.PopupKind genre,
            int keyCode) {
        View vue = vues.get(genre);
        if (!(vue instanceof PopupClavier carte)) {
            return false;
        }
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP) {
            return carte.surPage(-1);
        }
        if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
            return carte.surPage(1);
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            return carte.surFleche(-1);
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            return carte.surFleche(1);
        }
        if (keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                || keyCode == KeyEvent.KEYCODE_TAB) {
            return carte.surValidation();
        }
        return false;
    }

    /** Le tap hors cartes : les genres qui cèdent au geste extérieur
     *  se referment — la couche a prévenu, les cartes partent. */
    private void surToucheHorsCartes() {
        retirer(controleur.surToucheHorsCartes());
    }

    /** Les cartes des genres fermés quittent la couche — la vue ne
     *  survit jamais à son état. */
    private void retirer(Set<PopupController.PopupKind> fermes) {
        for (PopupController.PopupKind ferme : fermes) {
            couche.dismiss(ferme);
        }
    }

    /** La carte liée du genre — jamais de genre orphelin. */
    private View carteDe(PopupController.PopupKind genre) {
        View vue = vues.get(genre);
        if (vue == null) {
            throw new IllegalStateException("genre non lié : " + genre);
        }
        return vue;
    }
}
