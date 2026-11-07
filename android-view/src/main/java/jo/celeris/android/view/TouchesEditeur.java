package jo.celeris.android.view;

import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * Le routage tactile de l'éditeur : l'appui pose le curseur puis
 * étend la sélection (ancre figée au DOWN, PIEGE-03) ou tire une
 * poignée, le panoramique à deux doigts défile, l'appui long arme la
 * pilule « ajouter un curseur » (A-01) — la vue route, ce
 * collaborateur décide.
 *
 * <p>Vue-collaborateur par délégation (même famille qu'
 * {@link AccessibilityHost}) : l'état de geste — dernier point, mode
 * panoramique, poignée tirée — vit ici, la vue reste la racine de
 * composition et le clavier et le curseur passent par
 * {@link CurseurClavier}.</p>
 */
final class TouchesEditeur {

    private final EditorView vue;

    private float dernierX;
    private float dernierY;
    private boolean panoramique;
    private SelectionHandles.HandleId poigneeTiree =
            SelectionHandles.HandleId.NONE;

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
     * L'appui d'un doigt : focus et clavier, puis pilule armée,
     * poignée visée, chevron de pli, champ de numéros — ou pose du
     * curseur, seul ou supplémentaire sous pilule.
     */
    boolean surAppui(MotionEvent event) {
        vue.curseurClavier.surAppui();
        dernierX = event.getX();
        dernierY = event.getY();
        panoramique = false;
        float x = event.getX();
        float y = event.getY();
        if (vue.pilule.isArmed() && vue.pilule.contains(x, y)) {
            vue.pilule.hide();
            vue.invalidate();
            return true;
        }
        SelectionHandles.HandleId poignee = poigneeVisee(x, y);
        if (poignee != SelectionHandles.HandleId.NONE) {
            poigneeTiree = poignee;
            return true;
        }
        float coulisse = vue.marquesGouttiere.coulisse(vue.syntaxe.coulisse(
                vue.metrics.charWidth(), vue.margeGouttiere),
                vue.metrics.charWidth());
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
        vue.glissement.down(offset);
        if (vue.pilule.isArmed()) {
            vue.session.setSelection(ajouterCurseur(offset));
        } else {
            vue.session.setSelection(
                    MultiSelection.of(SelRange.caretAt(offset)));
        }
        vue.annoncerSelection();
        vue.invalidate();
        return true;
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
     * deux doigts défile, poignée tirée s'étire, glissement étend la
     * sélection.
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
        if (vue.glissement.isActive()) {
            vue.session.setSelection(MultiSelection.of(
                    vue.glissement.dragTo(
                            offsetAt(event.getX(), event.getY()))));
            vue.invalidate();
        }
        return true;
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
        poigneeTiree = SelectionHandles.HandleId.NONE;
        return true;
    }

    /** Ajoute un curseur à l'offset visé, les existants tiennent (A-01). */
    private MultiSelection ajouterCurseur(int offset) {
        List<SelRange> plages =
                new ArrayList<>(vue.session.selection().ranges());
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
