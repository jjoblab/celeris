package jo.celeris.android.view.internal.input;

import jo.celeris.android.view.internal.model.IdentifierBounds;
import android.view.MotionEvent;
import jo.celeris.android.view.ScrollGeometry;
import jo.celeris.android.view.SelectionHandles;
import jo.celeris.android.view.internal.access.EditorAccess;
import jo.celeris.android.view.internal.accessibility.AccessibilityHost;

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
 * clavier et le curseur passent par {@link KeyboardCursor}.</p>
 */
public final class EditorKeys  {

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
    private static final long DOUBLE_TAP_GAP_MS = 300L;

    private final EditorAccess acces;

    private float dernierX;
    private float dernierY;
    /** Le point de départ du geste — la décision tap/défilement s'y lit. */
    private float departX;
    private float departY;
    /** Le DOWN de CE geste nous est arrivé — le fantôme l'engloutit sinon. */
    private boolean fingerDown;
    /** Le geste a dépassé le seuil : c'est un défilement, jamais un tap. */
    private boolean scroll;
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
    public EditorKeys(EditorAccess acces) {
        this.acces = acces;
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
    public boolean router(MotionEvent event, Predicate<MotionEvent> inconnu) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (acces.ghost().touche(event.getX(), event.getY())) {
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
                acces.performClick();
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
    public boolean surAppui(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        fingerDown = true;
        scroll = false;
        panoramique = false;
        armeLong = false;
        acces.magnifier().cesser();
        acces.glissement().up();
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
    public void surAppuiLong(float x, float y) {
        if (x < acces.largeurGouttiere()) {
            // le point d'arrêt vit à l'appui long de gouttière (D-04) :
            // la ligne visée part à l'hôte, le geste se consomme sans
            // toucher au curseur ni au clavier
            int rangee = ScrollGeometry.firstRow(acces.scrollY() + y,
                    acces.metrics().lineHeight());
            acces.rappels().notifierBasculeBreakpoint(
                    acces.projection().firstLineOfRow(rangee));
            armeLong = true;
            return;
        }
        int offset = offsetAt(x, y);
        IdentifierBounds.Around mot = IdentifierBounds.around(
                acces.session().document(), offset);
        motDebut = mot.start();
        motFin = mot.end();
        offsetAppuiLong = offset;
        if (motFin > motDebut) {
            acces.session().setSelection(MultiSelection.of(
                    new SelRange(motDebut, motFin)));
        } else {
            acces.session().setSelection(MultiSelection.of(
                    SelRange.caretAt(motDebut)));
        }
        armeLong = true;
        acces.curseurClavier().masquerClavier();
        acces.curseurClavier().annoncerSelection();
        acces.invalidate();
    }

    /** Le second doigt se pose : le panoramique s'arme, le glissement
     *  lâche — la loupe se referme (le geste à deux doigts ne règle rien). */
    boolean surDoigtPose(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            acces.magnifier().cesser();
            panoramique = true;
            scroll = false;
            acces.glissement().up();
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
        if (acces.zoom().actif()) {
            return true;
        }
        if (panoramique && event.getPointerCount() >= 2) {
            float contentHeight =
                    acces.projection().rowCount() * acces.metrics().lineHeight();
            acces.setScrollY(ScrollGeometry.clamp(
                    acces.scrollY() + dernierY - event.getY(),
                    contentHeight, acces.getHeight()));
            acces.setScrollX(ScrollGeometry.clamp(
                    acces.scrollX() + dernierX - event.getX(),
                    ScrollGeometry.contentWidth(acces.projection(), acces.metrics()),
                    ScrollGeometry.visibleWidth(acces.getWidth(),
                            acces.largeurGouttiere())));
            dernierX = event.getX();
            dernierY = event.getY();
            acces.invalidate();
            return true;
        }
        if (poigneeTiree != SelectionHandles.HandleId.NONE) {
            pullHandle(event.getX(), event.getY());
            return true;
        }
        if (armeLong && !acces.glissement().isActive()) {
            armerAncre(offsetAt(event.getX(), event.getY()));
        }
        if (acces.glissement().isActive()) {
            // la loupe suit le dragage de sélection (D-16) : le texte
            // sous le doigt s'agrandit au-dessus — le réglage se voit
            followMagnifier(event);
            acces.session().setSelection(MultiSelection.of(
                    acces.glissement().dragTo(
                            offsetAt(event.getX(), event.getY()))));
            acces.invalidate();
            return true;
        }
        return onOneFingerMove(event);
    }

    /** Le doigt levé : le panoramique s'arrête, le glissement lâche. */
    boolean surDoigtLeve() {
        panoramique = false;
        acces.glissement().up();
        return true;
    }

    /** Coupe le panoramique : le pincement engage prend la main (D-15). */
    public void couperPanoramique() {
        panoramique = false;
    }

    /**
     * Le déplacement à un doigt hors sélection : le seuil de tap
     * dépassé, le geste devient un défilement — le contenu suit le
     * doigt sur les DEUX axes, borné par la géométrie de défilement.
     */
    private boolean onOneFingerMove(MotionEvent event) {
        if (!armeLong && !scroll && fingerDown
                && distanceCarree(departX, departY, event) > SEUIL_TAP_PX
                        * SEUIL_TAP_PX) {
            scroll = true;
        }
        if (scroll) {
            float contentHeight =
                    acces.projection().rowCount() * acces.metrics().lineHeight();
            acces.setScrollY(ScrollGeometry.clamp(
                    acces.scrollY() + dernierY - event.getY(),
                    contentHeight, acces.getHeight()));
            acces.setScrollX(ScrollGeometry.clamp(
                    acces.scrollX() + dernierX - event.getX(),
                    ScrollGeometry.contentWidth(acces.projection(), acces.metrics()),
                    ScrollGeometry.visibleWidth(acces.getWidth(),
                            acces.largeurGouttiere())));
            dernierX = event.getX();
            dernierY = event.getY();
            acces.invalidate();
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
            acces.glissement().down(offsetAppuiLong);
        } else if (offsetDoigt >= motFin) {
            acces.glissement().down(motDebut, motDebut, motFin);
        } else if (offsetDoigt <= motDebut) {
            acces.glissement().down(motFin, motDebut, motFin);
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
    public boolean surRelachement(MotionEvent event) {
        boolean annulation = event.getActionMasked() == MotionEvent.ACTION_CANCEL;
        boolean tap = !annulation && fingerDown && !scroll && !armeLong
                && poigneeTiree == SelectionHandles.HandleId.NONE
                && !panoramique
                && distanceCarree(departX, departY, event)
                        <= SEUIL_TAP_PX * SEUIL_TAP_PX;
        fingerDown = false;
        scroll = false;
        panoramique = false;
        armeLong = false;
        poigneeTiree = SelectionHandles.HandleId.NONE;
        acces.magnifier().cesser();
        acces.glissement().up();
        if (tap) {
            if (doubleTapRapproche(event) && acces.rappels().notifierDoubleTap(
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
                || event.getEventTime() - dernierTapA > DOUBLE_TAP_GAP_MS) {
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
        float lane = acces.gutterState().currentLane();
        float foldBand = acces.gutterState().currentFoldBand();
        if (acces.syntaxe().toggleFoldAt(x, y, acces.scrollY(),
                acces.metrics().lineHeight(), acces.largeurGouttiere(), foldBand,
                acces.projection())) {
            // PIEGE-02 : le pli change le SENS des rangées sans toucher
            // la révision du document — la rangée 12 après le repli n'est
            // plus la rangée 12 d'avant, et aucune composante de clé ne
            // signe l'état des plis. Le cache des mises en page
            // s'oublie ENTIER, sinon les rangées sous le pli
            // resserviraient le texte périmé d'avant le repli.
            acces.cache().clear();
            acces.invalidate();
            return;
        }
        // l'ampoule d'intentions PRIME dans la coulisse — son tap rend
        // la ligne à l'hôte, qui y ouvre le menu des intentions
        if (acces.gutterMarks().surAppuiAmpoule(x, y, lane,
                acces.scrollY(), acces.metrics().lineHeight(), acces.projection())) {
            return;
        }
        if (acces.gutterMarks().onMarkPress(x, y, lane,
                acces.scrollY(), acces.metrics().lineHeight(), acces.projection())) {
            return;
        }
        if (acces.gutterMarks().surAppui(x, y, acces.largeurGouttiere(),
                lane, foldBand, acces.scrollY(), acces.metrics().lineHeight(),
                acces.projection())) {
            return;
        }
        int offset = offsetAt(x, y);
        acces.session().setSelection(
                MultiSelection.of(SelRange.caretAt(offset)));
        acces.curseurClavier().annoncerSelection();
        acces.curseurClavier().surAppui();
        acces.invalidate();
    }

    private void pullHandle(float x, float y) {
        List<SelRange> ranges = acces.session().selection().ranges();
        if (ranges.size() != 1) {
            poigneeTiree = SelectionHandles.HandleId.NONE;
            return;
        }
        // la loupe suit la poignée tirée (D-16, le dépôt de départ) :
        // l'extrémité réglée s'agrandit au-dessus du doigt
        followMagnifier(x, y);
        acces.session().setSelection(MultiSelection.of(SelectionHandles.dragged(
                ranges.get(0), poigneeTiree, offsetAt(x, y))));
        acces.invalidate();
    }

    /**
     * La loupe suit le geste de réglage : l'ordonnée SNAPPÉE au centre
     * de la rangée sous le doigt (le contenu de la bulle ne tremble
     * JAMAIS verticalement — un PAS net d'une ligne au changement de
     * rangée), l'abscisse amortie s'y pose — la sémantique des loupes
     * de plateforme (B9 du dépôt de départ).
     */
    private void followMagnifier(MotionEvent event) {
        followMagnifier(event.getX(), event.getY());
    }

    /**
     * La loupe suit le point du geste : le centre de la rangée sous
     * le doigt y passe, l'amortissement vit dans la loupe.
     */
    private void followMagnifier(float x, float y) {
        int rangee = ScrollGeometry.firstRow(acces.scrollY() + y,
                acces.metrics().lineHeight());
        float centreRangee = rangee * acces.metrics().lineHeight()
                + acces.metrics().lineHeight() / 2f - acces.scrollY();
        acces.magnifier().suivre(x, centreRangee);
    }

    private SelectionHandles.HandleId poigneeVisee(float x, float y) {
        List<SelRange> ranges = acces.session().selection().ranges();
        if (ranges.size() != 1 || ranges.get(0).isEmpty()) {
            return SelectionHandles.HandleId.NONE;
        }
        return SelectionHandles.hitTest(x, y, acces.largeurGouttiere(),
                acces.scrollX(), acces.scrollY(), acces.session().document(),
                acces.projection(), acces.metrics(), ranges.get(0).start(),
                ranges.get(0).end());
    }

    private int offsetAt(float x, float y) {
        return acces.viseur().offsetAt(acces.session().document(), acces.projection(),
                acces.metrics(), acces.largeurGouttiere(), acces.scrollX(), acces.scrollY(),
                x, y);
    }
}
