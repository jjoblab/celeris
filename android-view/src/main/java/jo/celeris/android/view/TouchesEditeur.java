package jo.celeris.android.view;

import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * Le routage tactile de l'éditeur (redressement du propriétaire) : le
 * DOIGT POSÉ n'arme ni clavier ni curseur — le geste décide. Le
 * mouvement au-delà du seuil de tap devient un DÉFILEMENT (les deux
 * axes, un doigt) ; le relâchement SANS mouvement est un TAP, lui seul
 * pose le curseur et ouvre le clavier ; l'APPUI LONG sélectionne le
 * mot sous le doigt et retire le clavier — le dragage qui suit étend
 * la sélection, ancre figée à l'armement (PIEGE-03). La pilule
 * « ajouter un curseur » (A-01) s'offre au-dessus du mot sélectionné :
 * le tap dessus ajoute le curseur, le tap ailleurs la congédie. Le
 * panoramique à deux doigts défile, l'appui sur une poignée la tire —
 * la vue route, ce collaborateur décide.
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
     * Le doigt se pose : RIEN ne s'arme — ni clavier, ni curseur (le
     * geste n'a pas encore parlé). Seule la poignée visée se retient :
     * son tirage commence au doigt posé, pas au relâchement. La pilule,
     * les plis, les marques et la pose du curseur se résolvent au TAP
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
     * L'appui long (M7.5, redressement) : le mot sous le doigt se
     * sélectionne, le clavier se retire — le dragage qui suit étend la
     * sélection au doigt, sans clavier. La pilule « ajouter un curseur »
     * (A-01) s'offre au-dessus du mot : le tap dessus ajoute le
     * curseur, le tap ailleurs la congédie.
     */
    void surAppuiLong(float x, float y) {
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
        vue.pilule.show(x, y);
        vue.invalidate();
    }

    /** Le second doigt posé : le panoramique s'arme, le glissement lâche. */
    boolean surDoigtPose(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
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
            vue.pilule.hide();
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
            vue.pilule.hide();
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
            vue.pilule.hide();
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
     * la cascade (pilule, pli, marques, curseur et clavier) ; tout
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
        vue.glissement.up();
        if (tap) {
            resoudreTap(event.getX(), event.getY());
        }
        return true;
    }

    /**
     * La cascade du TAP (la discipline du dépôt de départ) : la pilule
     * armée d'abord (l'appui dessus ajoute le curseur, ailleurs elle se
     * congédie), le chevron de pli, la marque de gouttière et son
     * repère — et la pose du curseur au point touché, qui seule ouvre
     * le clavier.
     */
    private void resoudreTap(float x, float y) {
        if (vue.pilule.isArmed()) {
            boolean surPilule = vue.pilule.contains(x, y);
            vue.pilule.hide();
            if (surPilule) {
                vue.session.setSelection(
                        ajouterCurseur(offsetAt(vue.pilule.x(), vue.pilule.y())));
                vue.curseurClavier.annoncerSelection();
                vue.invalidate();
                return;
            }
            vue.invalidate();
        }
        float coulisse = vue.gouttiereEtat.coulisseCourante();
        if (vue.syntaxe.basculerPliAuPoint(x, y, vue.scrollY,
                vue.metrics.lineHeight(), coulisse, vue.projection)) {
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
        if (vue.marquesGouttiere.surAppuiRepere(x, y, coulisse,
                vue.scrollY, vue.metrics.lineHeight(), vue.projection)) {
            return;
        }
        if (vue.marquesGouttiere.surAppui(x, y, vue.largeurGouttiere,
                coulisse, vue.scrollY, vue.metrics.lineHeight(),
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

    /**
     * Ajoute un curseur à l'offset visé, les curseurs existants
     * tiennent (A-01) — les plages non vides se replient en curseurs :
     * la pilule ajoute un CURSEUR, jamais une plage.
     */
    private MultiSelection ajouterCurseur(int offset) {
        List<SelRange> plages = new ArrayList<>();
        for (SelRange plage : vue.session.selection().ranges()) {
            if (plage.isEmpty()) {
                plages.add(plage);
            }
        }
        plages.add(SelRange.caretAt(offset));
        return MultiSelection.of(plages);
    }

    private void tirerPoignee(float x, float y) {
        List<SelRange> plages = vue.session.selection().ranges();
        if (plages.size() != 1) {
            poigneeTiree = SelectionHandles.HandleId.NONE;
            return;
        }
        vue.session.setSelection(MultiSelection.of(SelectionHandles.dragged(
                plages.get(0), poigneeTiree, offsetAt(x, y))));
        vue.invalidate();
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
