package jo.celeris.android.view.internal.accessibility;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.accessibility.AccessibilityNodeProvider;
import jo.celeris.android.view.FrameState;
import jo.celeris.android.view.MovementGranularity;
import jo.celeris.android.view.ScrollGeometry;
import jo.celeris.android.view.VirtualRows;
import jo.celeris.android.view.internal.access.EditorAccess;

import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.jspecify.annotations.Nullable;

/**
 * L'hôte d'accessibilité de la vue (G-05, ADR-008) : la colle qui
 * expose l'éditeur comme un nœud éditable dont les enfants virtuels
 * sont les RANGÉES VISIBLES ({@link VirtualRows}) — le lecteur d'écran
 * les lit une à une, la fenêtre seule se sert (G-01). Les
 * granularités de mouvement délèguent à {@link MovementGranularity},
 * la sémantique du clavier matériel (D-17) servie au lecteur d'écran.
 *
 * <p>L'identifiant virtuel 0 désigne l'éditeur lui-même ; la rangée
 * visuelle d'indice r porte l'identifiant r + 1.</p>
 */
public final class AccessibilityHost  {

    private static final String TEXT_CLASS = "android.widget.EditText";
    private static final String ROW_CLASS = "android.widget.TextView";

    private final EditorAccess acces;
    private final Fournisseur fournisseur = new Fournisseur();

    /**
     * Construit l'hôte d'une vue.
     *
     * @param vue la vue propriétaire de la session et du tracé
     */
    public AccessibilityHost(EditorAccess acces) {
        this.acces = acces;
    }

    /** Le fournisseur de l'arbre virtuel — possédé, jamais réalloué. */
    public AccessibilityNodeProvider fournisseur() {
        return fournisseur;
    }

    /** Remplit le nœud d'accessibilité de l'éditeur (la racine). */
    public void remplir(AccessibilityNodeInfo info) {
        info.setClassName(TEXT_CLASS);
        info.setEditable(!acces.session().isReadOnly());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            info.setTextSelectable(true);
        }
        info.addAction(AccessibilityAction.ACTION_NEXT_AT_MOVEMENT_GRANULARITY);
        info.addAction(AccessibilityAction.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY);
        info.addAction(AccessibilityAction.ACTION_SET_SELECTION);
        info.addAction(AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityAction.ACTION_SCROLL_BACKWARD);
        info.setMovementGranularities(masqueGranularites());
        List<SelRange> ranges = acces.session().selection().ranges();
        if (!ranges.isEmpty()) {
            info.setTextSelection(ranges.get(0).start(), ranges.get(0).end());
        }
    }

    /**
     * Exécute une action d'accessibilité sur l'éditeur : mouvement à
     * granularité, sélection posée, défilement d'une page.
     *
     * @param action l'action demandée
     * @param args   les arguments de l'action
     * @return vrai si l'action est consommée
     */
    public boolean execute(int action, @Nullable Bundle args) {
        if (action == AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                || action
                        == AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY) {
            return mouvoir(action, args);
        }
        if (action == AccessibilityNodeInfo.ACTION_SET_SELECTION) {
            return setSelection(args);
        }
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            acces.curseurClavier().defilerPage(action
                    == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            return true;
        }
        return false;
    }

    /** Remplit le nœud d'une rangée virtuelle. */
    private void fillRow(AccessibilityNodeInfo info, VirtualRows.Row rangee) {
        info.setClassName(ROW_CLASS);
        info.setText(rangee.text());
        info.setParent(acces.vue());
        info.setBoundsInScreen(bornesAEcran(rangee));
        info.addAction(AccessibilityAction.ACTION_CLICK);
        info.addAction(AccessibilityAction.ACTION_NEXT_AT_MOVEMENT_GRANULARITY);
        info.addAction(AccessibilityAction.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY);
    }

    /** Les bornes d'une rangée rapportées à l'écran. */
    private static Rect bornesAEcran(VirtualRows.Row rangee) {
        return new Rect((int) rangee.left(), (int) rangee.top(),
                (int) rangee.right(), (int) rangee.bottom());
    }

    private boolean mouvoir(int action, @Nullable Bundle args) {
        if (args == null) {
            return false;
        }
        int brute = args.getInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT, -1);
        MovementGranularity granularite = granulariteDe(brute);
        if (granularite == null) {
            return false;
        }
        boolean etendre = args.getBoolean(
                AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                false);
        Document document = acces.session().document();
        List<SelRange> ranges = acces.session().selection().ranges();
        int origine = ranges.isEmpty() ? 0 : ranges.get(0).caret();
        FrameState state = acces.etatImageCourante();
        int pageRows = ScrollGeometry.visibleRows(acces.getHeight(),
                state.metrics().lineHeight());
        int target = action
                == AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                ? MovementGranularity.next(document, state.projection(), origine,
                        granularite, pageRows)
                : MovementGranularity.previous(document, state.projection(), origine,
                        granularite, pageRows);
        SelRange apres = etendre
                ? new SelRange(ranges.get(0).anchor(), target)
                : SelRange.caretAt(target);
        acces.session().setSelection(MultiSelection.of(apres));
        acces.invalidate();
        acces.curseurClavier().annoncerSelection();
        return true;
    }

    private boolean setSelection(@Nullable Bundle args) {
        if (args == null) {
            return false;
        }
        int start = args.getInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, -1);
        int end = args.getInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, -1);
        if (start < 0 || end < 0) {
            return false;
        }
        Document document = acces.session().document();
        acces.session().setSelection(MultiSelection.of(new SelRange(
                Math.min(start, document.length()),
                Math.min(end, document.length()))));
        acces.invalidate();
        acces.curseurClavier().annoncerSelection();
        return true;
    }

    /** Pose le curseur au début d'une rangée virtuelle cliquée. */
    private boolean clickRow(int rangee) {
        FrameState state = acces.etatImageCourante();
        int line = state.projection().firstLineOfRow(rangee);
        int column = state.projection().spanStartAtRow(rangee);
        acces.session().setSelection(MultiSelection.of(SelRange.caretAt(
                state.document().lineStart(line) + column)));
        acces.invalidate();
        acces.curseurClavier().annoncerSelection();
        return true;
    }

    private static int masqueGranularites() {
        int masque = 0;
        for (MovementGranularity granularite : MovementGranularity.values()) {
            masque |= granularite(granularite);
        }
        return masque;
    }

    private static int granularite(MovementGranularity granularite) {
        return switch (granularite) {
            case CHARACTER -> AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER;
            case WORD -> AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD;
            case LINE -> AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE;
            case PAGE -> AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE;
        };
    }

    private static @Nullable MovementGranularity granulariteDe(int brute) {
        return switch (brute) {
            case AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER ->
                MovementGranularity.CHARACTER;
            case AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD ->
                MovementGranularity.WORD;
            case AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE ->
                MovementGranularity.LINE;
            case AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE ->
                MovementGranularity.PAGE;
            default -> null;
        };
    }

    /** Les rangées virtuelles de la fenêtre courante. */
    private List<VirtualRows.Row> rows() {
        FrameState state = acces.etatImageCourante();
        return VirtualRows.visible(state.document(), state.projection(),
                state.metrics(), state.firstRow(), state.visibleRows(),
                state.gutterWidth(), state.scrollX(), state.scrollY());
    }

    /** Le fournisseur de l'arbre virtuel : sert les rangées à la demande. */
    private final class Fournisseur extends AccessibilityNodeProvider {

        /**
         * Les nœuds se créent par obtain() : les constructeurs publics
         * n'arrivent qu'au niveau 30, la dépréciation du niveau 36
         * recommande précisément ces constructeurs — obtain() reste la
         * voie compatible niveau 26.
         */
        @SuppressWarnings("deprecation")
        @Override
        public @Nullable AccessibilityNodeInfo createAccessibilityNodeInfo(
                int virtualViewId) {
            if (virtualViewId == AccessibilityNodeProvider.HOST_VIEW_ID) {
                AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain(acces.vue());
                remplir(info);
                return info;
            }
            int rangee = virtualViewId - 1;
            List<VirtualRows.Row> rows = rows();
            if (rangee < 0 || rangee >= rows.size()) {
                return null;
            }
            AccessibilityNodeInfo info =
                    AccessibilityNodeInfo.obtain(acces.vue(), virtualViewId);
            fillRow(info, rows.get(rangee));
            return info;
        }

        @Override
        public boolean performAction(int virtualViewId, int action,
                @Nullable Bundle arguments) {
            if (virtualViewId == AccessibilityNodeProvider.HOST_VIEW_ID) {
                return acces.vue().performAccessibilityAction(action, arguments);
            }
            List<VirtualRows.Row> rows = rows();
            int rangee = virtualViewId - 1;
            if (rangee < 0 || rangee >= rows.size()) {
                return false;
            }
            if (action == AccessibilityNodeInfo.ACTION_CLICK) {
                return clickRow(rows.get(rangee).row());
            }
            return acces.vue().performAccessibilityAction(action, arguments);
        }

        @Override
        public @Nullable AccessibilityNodeInfo findFocus(int focus) {
            return null;
        }
    }
}
