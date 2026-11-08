package jo.celeris.android.view;

import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * Le routage tactile de l'éditeur (M7.5, redressement) : le simple
 * appui pose le curseur et ouvre le clavier, l'APPUI LONG sélectionne
 * le mot sous le doigt et retire le clavier — le dragage qui suit
 * étend la sélection, ancre figée à l'armement (PIEGE-03), le mot de
 * départ toujours englobé. La pilule « ajouter un curseur » (A-01)
 * s'offre au-dessus du mot sélectionné : l'appui dessus ajoute le
 * curseur, l'appui ailleurs la congédie. Le panoramique à deux doigts
 * défile, l'appui sur une poignée la tire — la vue route, ce
 * collaborateur décide.
 *
 * <p>Vue-collaborateur par délégation (même famille qu'
 * {@link AccessibilityHost}) : l'état de geste — dernier point, mode
 * panoramique, poignée tirée, armement long — vit ici, la vue reste la
 * racine de composition et le clavier et le curseur passent par
 * {@link CurseurClavier}.</p>
 */
final class TouchesEditeur {

    private final EditorView vue;

    private float dernierX;
    private float dernierY;
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
     * L'appui d'un doigt : focus et clavier, puis pilule armée
     * (l'appui dessus ajoute le curseur, ailleurs elle se congedie),
     * poignée visée, chevron de pli, champ de numéros — ou pose du
     * curseur. Le simple appui ne sélectionne PAS : seul l'appui long
     * arme la sélection (M7.5, redressement).
     */
    boolean surAppui(MotionEvent event) {
        vue.curseurClavier.surAppui();
        dernierX = event.getX();
        dernierY = event.getY();
        panoramique = false;
        float x = event.getX();
        float y = event.getY();
        if (vue.pilule.isArmed()) {
            boolean surPilule = vue.pilule.contains(x, y);
            vue.pilule.hide();
            if (surPilule) {
                vue.session.setSelection(
                        ajouterCurseur(offsetAt(vue.pilule.x(), vue.pilule.y())));
                vue.curseurClavier.annoncerSelection();
            }
            vue.invalidate();
            if (surPilule) {
                return true;
            }
        }
        SelectionHandles.HandleId poignee = poigneeVisee(x, y);
        if (poignee != SelectionHandles.HandleId.NONE) {
            poigneeTiree = poignee;
            return true;
        }
        float coulisse = vue.gouttiereEtat.coulisseCourante();
        if (vue.syntaxe.basculerPliAuPoint(x, y, vue.scrollY,
                vue.metrics.lineHeight(), coulisse, vue.projection)) {
            vue.invalidate();
            return true;
        }
        if (vue.marquesGouttiere.surAppuiRepere(x, y, coulisse,
                vue.scrollY, vue.metrics.lineHeight(), vue.projection)) {
            return true;
        }
        if (vue.marquesGouttiere.surAppui(x, y, vue.largeurGouttiere,
                coulisse, vue.scrollY, vue.metrics.lineHeight(),
                vue.projection)) {
            return true;
        }
        int offset = offsetAt(x, y);
        vue.session.setSelection(
                MultiSelection.of(SelRange.caretAt(offset)));
        vue.curseurClavier.annoncerSelection();
        vue.invalidate();
        return true;
    }

    /**
     * L'appui long (M7.5, redressement) : le mot sous le doigt se
     * sélectionne, le clavier se retire — le dragage qui suit étend la
     * sélection au doigt, sans clavier. La pilule « ajouter un curseur »
     * (A-01) s'offre au-dessus du mot : l'appui dessus ajoute le
     * curseur, l'appui ailleurs la congédie.
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
            vue.glissement.up();
            dernierX = event.getX();
            dernierY = event.getY();
        }
        return true;
    }

    /**
     * Le déplacement : pincement en cours se consomme, panoramique à
     * deux doigts défile, poignée tirée s'étire, dragage armé par
     * l'appui long étend la sélection. L'ancre se choisit à la SORTIE
     * du mot : le bord que le doigt quitte reste figé (PIEGE-03), le
     * mot de départ reste englobé — tant que le doigt y vit, le mot
     * reste sélectionné tel quel.
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
        }
        return true;
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

    /** Le relâchement : tout geste se conclut. */
    boolean surRelachement() {
        vue.glissement.up();
        panoramique = false;
        armeLong = false;
        poigneeTiree = SelectionHandles.HandleId.NONE;
        return true;
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
