package jo.celeris.android.view;

import android.view.MotionEvent;

import java.util.List;
import java.util.function.Predicate;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * Le routage tactile de l'éditeur (redressement du propriétaire) : le
 * DOIGT POSÉ n'arme ni clavier ni curseur — le geste décide. Le
 * mouvement au-delà du seuil de tap devient un DÉFILEMENT (les deux
 * axes, un doigt) ; le relâchement SANS mouvement est un TAP, lui seul
 * pose le curseur et ouvre le clavier ; l'APPUI LONG sélectionne le
 * mot sous le doigt et retire le clavier — le dragage qui suit étend
 * la sélection, ancre figée à l'armement (PIEGE-03), et la LOUPE y
 * naît (D-16, le redressement du 2026-11-13 : « ajoute la
 * fonctionnalité de magnifier ») : le texte sous le doigt s'agrandit
 * dans une bulle au-dessus, le réglage se voit. Le panoramique à
 * deux doigts défile, l'appui sur une poignée la tire — la vue
 * route, ce collaborateur décide.
 *
 * <p>Vue-collaborateur par délégation (même famille qu'
 * {@link AccessibilityHost}) : l'état de geste — dernier point, point
 * de départ, mode panoramique, défilement engagé, poignée tirée,
 * armement long — vit ici, la vue reste la racine de composition et le
 * clavier et le curseur passent par {@link CurseurClavier}.</p>
 */
final class TouchesEditeur {

    /**
     * Le seuil de tap, en pixels : le geste qui s'éloigne du point de
     * départ au-delà de cette distance n'est plus un tap — c'est un
     * défilement (la discipline du dépôt de départ, 24 px).
     */
    private static final float SEUIL_TAP_PX = 24f;

    /**
     * L'écart maximal des deux taps d'un double-tap, en millisecondes —
     * au-delà, le second tap retombe dans la cascade du simple.
     */
    private static final long ECART_DOUBLE_TAP_MS = 300L;

    private final EditorView vue;

    private float dernierX;
    private float dernierY;
    /** Le point de départ du geste — la décision tap/défilement s'y lit. */
    private float departX;
    private float departY;
    /** Le DOWN de CE geste nous est arrivé — le fantôme l'engloutit sinon. */
    private boolean doigtPose;
    /** Le geste a dépassé le seuil : c'est un défilement, jamais un tap. */
    private boolean defilement;
    private boolean panoramique;
    private SelectionHandles.HandleId poigneeTiree =
            SelectionHandles.HandleId.NONE;
    /** L'armement long : le dragage étend la sélection du mot choisi. */
    private boolean armeLong;
    /** Les bords du mot de l'appui long ; égaux hors de tout mot. */
    private int motDebut;
    private int motFin;
    /** L'offset de l'appui long — l'ancre simple hors de tout mot. */
    private int offsetAppuiLong;
    /** L'instant du dernier tap conclu — l'écart au suivant y lit le
     *  double-tap ; zéro hors tap. */
    private long dernierTapA;
    /** La position du dernier tap conclu. */
    private float dernierTapX;
    private float dernierTapY;

    /**
     * Construit le collaborateur de la vue donnée — la vue n'est
     * consultée qu'après sa construction, jamais ici.
     *
     * @param vue la vue propriétaire des gestes
     */
    TouchesEditeur(EditorView vue) {
        this.vue = vue;
    }

    /** La poignée tirée, None hors glissement (accès paquet, tests). */
    SelectionHandles.HandleId poigneeTiree() {
        return poigneeTiree;
    }

    /**
     * Route l'événement tactile brut à son traitement : l'appui, le
     * doigt posé, le déplacement, le doigt levé et le relâchement
     * partent chacun à sa méthode ; l'action inconnue part au
     * traitement de la vue (le survol du stylet, par exemple).
     *
     * @param event   l'événement tactile
     * @param inconnu le traitement de la vue pour l'action inconnue
     * @return vrai si l'événement est consommé
     */
    boolean router(MotionEvent event, Predicate<MotionEvent> inconnu) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (vue.fantome.touche(event.getX(), event.getY())) {
                    return true;
                }
                return surAppui(event);
            case MotionEvent.ACTION_POINTER_DOWN:
                return surDoigtPose(event);
            case MotionEvent.ACTION_MOVE:
                return surDeplacement(event);
            case MotionEvent.ACTION_POINTER_UP:
                return surDoigtLeve();
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                vue.performClick();
                return surRelachement(event);
            default:
                return inconnu.test(event);
        }
    }

    /**
     * Le doigt se pose : RIEN ne s'arme — ni clavier, ni curseur (le
     * geste n'a pas encore parlé). Seule la poignée visée se retient :
     * son tirage commence au doigt posé, pas au relâchement. Les plis,
     * les marques et la pose du curseur se résolvent au TAP
     * ({@link #surRelachement}) — le doigt posé qui glisse déféfile,
     * jamais il ne déclenche l'IME. Un geste précédent non conclu se
     * referme ici : l'armement long ne survit jamais à un nouvel appui.
     */
    boolean surAppui(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        doigtPose = true;
        defilement = false;
        panoramique = false;
        armeLong = false;
        vue.loupe.cesser();
        vue.glissement.up();
        departX = dernierX = x;
        departY = dernierY = y;
        SelectionHandles.HandleId poignee = poigneeVisee(x, y);
        if (poignee != SelectionHandles.HandleId.NONE) {
            poigneeTiree = poignee;
        }
        return true;
    }

    /**
     * L'appui long (M7.5, redressement ; la reprise du design, D-04) :
     * dans la GOUTTIÈRE, il pose ou retire le point d'arrêt de la
     * ligne visée — l'hôte y répond, la vue ne touche à aucun état
     * d'exécution ; dans la zone de texte, le mot sous le doigt se
     * sélectionne, le clavier se retire — le dragage qui suit étend la
     * sélection au doigt, sans clavier, et la LOUPE y naît (D-16) :
     * le texte sous le doigt s'agrandit dans une bulle au-dessus — le
     * mot sélectionné se voit ENTIÈREMENT, aucun rectangle plein ne
     * le couvre (le redressement du 2026-11-13 : la pilule
     * « ajouter un curseur » se retire, la barre de sélection du dépôt
     * de départ l'offre à la place).
     */
    void surAppuiLong(float x, float y) {
        if (x < vue.largeurGouttiere) {
            // le point d'arrêt vit à l'appui long de gouttière (D-04) :
            // la ligne visée part à l'hôte, le geste se consomme sans
            // toucher au curseur ni au clavier
            int rangee = ScrollGeometry.firstRow(vue.scrollY + y,
                    vue.metrics.lineHeight());
            vue.rappels.notifierBasculeArret(
                    vue.projection.firstLineOfRow(rangee));
            armeLong = true;
            return;
        }
        int offset = offsetAt(x, y);
        IdentifierBounds.Around mot = IdentifierBounds.around(
                vue.session.document(), offset);
        motDebut = mot.start();
        motFin = mot.end();
        offsetAppuiLong = offset;
        if (motFin > motDebut) {
            vue.session.setSelection(MultiSelection.of(
                    new SelRange(motDebut, motFin)));
        } else {
            vue.session.setSelection(MultiSelection.of(
                    SelRange.caretAt(motDebut)));
        }
        armeLong = true;
        vue.curseurClavier.masquerClavier();
        vue.curseurClavier.annoncerSelection();
        vue.invalidate();
    }

    /** Le second doigt se pose : le panoramique s'arme, le glissement
     *  lâche — la loupe se referme (le geste à deux doigts ne règle rien). */
    boolean surDoigtPose(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            vue.loupe.cesser();
            panoramique = true;
            defilement = false;
            vue.glissement.up();
            dernierX = event.getX();
            dernierY = event.getY();
        }
        return true;
    }

    /**
     * Le déplacement : pincement en cours se consomme, panoramique à
     * deux doigts défile, poignée tirée s'étire, dragage armé par
     * l'appui long étend la sélection — et le glissement À UN DOIGT
     * au-delà du seuil de tap devient un DÉFILEMENT (les deux axes) :
     * le geste de lecture ne pose ni curseur ni clavier. L'ancre se
     * choisit à la SORTIE du mot : le bord que le doigt quitte reste
     * figé (PIEGE-03), le mot de départ reste englobé — tant que le
     * doigt y vit, le mot reste sélectionné tel quel.
     */
    boolean surDeplacement(MotionEvent event) {
        if (vue.zoom.actif()) {
            return true;
        }
        if (panoramique && event.getPointerCount() >= 2) {
            float hauteurContenu =
                    vue.projection.rowCount() * vue.metrics.lineHeight();
            vue.scrollY = ScrollGeometry.clamp(
                    vue.scrollY + dernierY - event.getY(),
                    hauteurContenu, vue.getHeight());
            vue.scrollX = ScrollGeometry.clamp(
                    vue.scrollX + dernierX - event.getX(),
                    ScrollGeometry.largeurContenu(vue.projection, vue.metrics),
                    ScrollGeometry.largeurVisible(vue.getWidth(),
                            vue.largeurGouttiere));
            dernierX = event.getX();
            dernierY = event.getY();
            vue.invalidate();
            return true;
        }
        if (poigneeTiree != SelectionHandles.HandleId.NONE) {
            tirerPoignee(event.getX(), event.getY());
            return true;
        }
        if (armeLong && !vue.glissement.isActive()) {
            armerAncre(offsetAt(event.getX(), event.getY()));
        }
        if (vue.glissement.isActive()) {
            // la loupe suit le dragage de sélection (D-16) : le texte
            // sous le doigt s'agrandit au-dessus — le réglage se voit
            suivreLoupe(event);
            vue.session.setSelection(MultiSelection.of(
                    vue.glissement.dragTo(
                            offsetAt(event.getX(), event.getY()))));
            vue.invalidate();
            return true;
        }
        return surDeplacementUnDoigt(event);
    }

    /** Le doigt levé : le panoramique s'arrête, le glissement lâche. */
    boolean surDoigtLeve() {
        panoramique = false;
        vue.glissement.up();
        return true;
    }

    /** Coupe le panoramique : le pincement engage prend la main (D-15). */
    void couperPanoramique() {
        panoramique = false;
    }

    /**
     * Le déplacement à un doigt hors sélection : le seuil de tap
     * dépassé, le geste devient un défilement — le contenu suit le
     * doigt sur les DEUX axes, borné par la géométrie de défilement.
     */
    private boolean surDeplacementUnDoigt(MotionEvent event) {
        if (!armeLong && !defilement && doigtPose
                && distanceCarree(departX, departY, event) > SEUIL_TAP_PX
                        * SEUIL_TAP_PX) {
            defilement = true;
        }
        if (defilement) {
            float hauteurContenu =
                    vue.projection.rowCount() * vue.metrics.lineHeight();
            vue.scrollY = ScrollGeometry.clamp(
                    vue.scrollY + dernierY - event.getY(),
                    hauteurContenu, vue.getHeight());
            vue.scrollX = ScrollGeometry.clamp(
                    vue.scrollX + dernierX - event.getX(),
                    ScrollGeometry.largeurContenu(vue.projection, vue.metrics),
                    ScrollGeometry.largeurVisible(vue.getWidth(),
                            vue.largeurGouttiere));
            dernierX = event.getX();
            dernierY = event.getY();
            vue.invalidate();
        }
        return true;
    }

    /** La distance au carré du point de départ — la décision tap/défilement. */
    private static float distanceCarree(float x, float y, MotionEvent event) {
        float dx = event.getX() - x;
        float dy = event.getY() - y;
        return dx * dx + dy * dy;
    }

    /**
     * Arme l'ancre à la sortie du mot : sorti à DROITE, l'ancre est le
     * début du mot — la sélection s'étend à droite ; sorti à GAUCHE,
     * l'ancre est la fin — elle s'étend à gauche. Hors de tout mot à
     * l'appui long, l'ancre simple est le point d'appui. Le doigt qui
     * reste dans le mot n'arme rien : le mot reste sélectionné.
     */
    private void armerAncre(int offsetDoigt) {
        if (motFin <= motDebut) {
            vue.glissement.down(offsetAppuiLong);
        } else if (offsetDoigt >= motFin) {
            vue.glissement.down(motDebut, motDebut, motFin);
        } else if (offsetDoigt <= motDebut) {
            vue.glissement.down(motFin, motDebut, motFin);
        }
    }

    /**
     * Le doigt levé : le geste se conclut. Le TAP — relâchement SANS
     * mouvement d'un doigt posé hors poignée, hors appui long — résout
     * la cascade (pli, marques, curseur et clavier) ; tout
     * autre geste (défilement, panoramique, dragage, poignée) se
     * conclut sans rien armer. L'annulation du geste (CANCEL) conclut
     * sans résoudre : le geste n'a jamais eu lieu.
     */
    boolean surRelachement(MotionEvent event) {
        boolean annulation = event.getActionMasked() == MotionEvent.ACTION_CANCEL;
        boolean tap = !annulation && doigtPose && !defilement && !armeLong
                && poigneeTiree == SelectionHandles.HandleId.NONE
                && !panoramique
                && distanceCarree(departX, departY, event)
                        <= SEUIL_TAP_PX * SEUIL_TAP_PX;
        doigtPose = false;
        defilement = false;
        panoramique = false;
        armeLong = false;
        poigneeTiree = SelectionHandles.HandleId.NONE;
        vue.loupe.cesser();
        vue.glissement.up();
        if (tap) {
            if (doubleTapRapproche(event) && vue.rappels.notifierDoubleTap(
                    offsetAt(event.getX(), event.getY()))) {
                // l'hôte consomme le geste : la cascade du tap simple ne
                // suit pas — le curseur ne bouge pas, le clavier ne s'arme pas
                dernierTapA = 0L;
                return true;
            }
            resoudreTap(event.getX(), event.getY());
            dernierTapA = event.getEventTime();
            dernierTapX = event.getX();
            dernierTapY = event.getY();
        } else {
            dernierTapA = 0L;
        }
        return true;
    }

    /**
     * Vrai quand CE tap en rapproche un dernier conclu — dans l'écart du
     * double-tap et à portée de seuil de tap : un double-tap, le geste
     * de l'hôte (le color picker d'un littéral de couleur, par exemple).
     */
    private boolean doubleTapRapproche(MotionEvent event) {
        if (dernierTapA == 0L
                || event.getEventTime() - dernierTapA > ECART_DOUBLE_TAP_MS) {
            return false;
        }
        float dx = event.getX() - dernierTapX;
        float dy = event.getY() - dernierTapY;
        return dx * dx + dy * dy <= SEUIL_TAP_PX * SEUIL_TAP_PX;
    }

    /**
     * La cascade du TAP (la discipline du dépôt de départ) : le
     * chevron de pli, la marque de gouttière et son repère — et la
     * pose du curseur au point touché, qui seule ouvre le clavier.
     */
    private void resoudreTap(float x, float y) {
        float coulisse = vue.gouttiereEtat.coulisseCourante();
        float bandePli = vue.gouttiereEtat.bandePliCourante();
        if (vue.syntaxe.basculerPliAuPoint(x, y, vue.scrollY,
                vue.metrics.lineHeight(), vue.largeurGouttiere, bandePli,
                vue.projection)) {
            // PIEGE-02 : le pli change le SENS des rangées sans toucher
            // la révision du document — la rangée 12 après le repli n'est
            // plus la rangée 12 d'avant, et aucune composante de clé ne
            // signe l'état des plis. Le cache des mises en page
            // s'oublie ENTIER, sinon les rangées sous le pli
            // resserviraient le texte périmé d'avant le repli.
            vue.cache.clear();
            vue.invalidate();
            return;
        }
        // l'ampoule d'intentions PRIME dans la coulisse — son tap rend
        // la ligne à l'hôte, qui y ouvre le menu des intentions
        if (vue.marquesGouttiere.surAppuiAmpoule(x, y, coulisse,
                vue.scrollY, vue.metrics.lineHeight(), vue.projection)) {
            return;
        }
        if (vue.marquesGouttiere.surAppuiRepere(x, y, coulisse,
                vue.scrollY, vue.metrics.lineHeight(), vue.projection)) {
            return;
        }
        if (vue.marquesGouttiere.surAppui(x, y, vue.largeurGouttiere,
                coulisse, bandePli, vue.scrollY, vue.metrics.lineHeight(),
                vue.projection)) {
            return;
        }
        int offset = offsetAt(x, y);
        vue.session.setSelection(
                MultiSelection.of(SelRange.caretAt(offset)));
        vue.curseurClavier.annoncerSelection();
        vue.curseurClavier.surAppui();
        vue.invalidate();
    }

    private void tirerPoignee(float x, float y) {
        List<SelRange> plages = vue.session.selection().ranges();
        if (plages.size() != 1) {
            poigneeTiree = SelectionHandles.HandleId.NONE;
            return;
        }
        // la loupe suit la poignée tirée (D-16, le dépôt de départ) :
        // l'extrémité réglée s'agrandit au-dessus du doigt
        suivreLoupe(x, y);
        vue.session.setSelection(MultiSelection.of(SelectionHandles.dragged(
                plages.get(0), poigneeTiree, offsetAt(x, y))));
        vue.invalidate();
    }

    /**
     * La loupe suit le geste de réglage : l'ordonnée SNAPPÉE au centre
     * de la rangée sous le doigt (le contenu de la bulle ne tremble
     * JAMAIS verticalement — un PAS net d'une ligne au changement de
     * rangée), l'abscisse amortie s'y pose — la sémantique des loupes
     * de plateforme (B9 du dépôt de départ).
     */
    private void suivreLoupe(MotionEvent event) {
        suivreLoupe(event.getX(), event.getY());
    }

    /**
     * La loupe suit le point du geste : le centre de la rangée sous
     * le doigt y passe, l'amortissement vit dans la loupe.
     */
    private void suivreLoupe(float x, float y) {
        int rangee = ScrollGeometry.firstRow(vue.scrollY + y,
                vue.metrics.lineHeight());
        float centreRangee = rangee * vue.metrics.lineHeight()
                + vue.metrics.lineHeight() / 2f - vue.scrollY;
        vue.loupe.suivre(x, centreRangee);
    }

    private SelectionHandles.HandleId poigneeVisee(float x, float y) {
        List<SelRange> plages = vue.session.selection().ranges();
        if (plages.size() != 1 || plages.get(0).isEmpty()) {
            return SelectionHandles.HandleId.NONE;
        }
        return SelectionHandles.hitTest(x, y, vue.largeurGouttiere,
                vue.scrollX, vue.scrollY, vue.session.document(),
                vue.projection, vue.metrics, plages.get(0).start(),
                plages.get(0).end());
    }

    private int offsetAt(float x, float y) {
        return vue.viseur.offsetAt(vue.session.document(), vue.projection,
                vue.metrics, vue.largeurGouttiere, vue.scrollX, vue.scrollY,
                x, y);
    }
}
