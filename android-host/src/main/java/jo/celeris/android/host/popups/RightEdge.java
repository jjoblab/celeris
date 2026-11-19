package jo.celeris.android.host.popups;

import android.view.View;

import java.util.List;
import java.util.function.IntConsumer;

import jo.celeris.android.host.spi.HostDiagnostic;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.ErrorBandModel;
import jo.celeris.android.widgets.ErrorBandView;
import jo.celeris.android.widgets.FastScrollerView;
import jo.celeris.android.widgets.MinimapModel;
import jo.celeris.android.widgets.MinimapView;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.core.text.TextSession;

/**
 * Le bord droit de la couche de l'éditeur : la bande d'erreurs, la
 * minimap et le défilement rapide. La bande d'erreurs vit avec les
 * diagnostics ; la minimap porte la silhouette du document, sa
 * fenêtre visible à l'encre d'accent — l'appui et le glissement
 * sautent, le modèle se reconstruit à l'édition ; le pouce du
 * défilement rapide suit la fraction du défilement, sa hauteur la
 * part visible — tout tient : le pouce se tait, la bande reste
 * muette.
 */
public final class RightEdge {

    /** Le nombre de crans de la bande d'erreurs. */
    private static final int BAND_STEPS = 200;
    /** Le plafond de rangees de la minimap — le document court compte
     *  les siennes, une par ligne. */
    private static final int MINIMAP_ROWS = 200;

    private final PopupManager popups;
    private final EditorView editor;
    private final TextSession session;
    private final ErrorBandView errorBand;
    private final MinimapView minimap;
    private final FastScrollerView fastScroll;
    private final IntConsumer sautDiagnostic;
    private final int margePx;

    /**
     * Construit le chrome du bord droit sur le coordinateur des
     * popups : les trois vues naissent liées à leurs genres.
     *
     * @param popups        le coordinateur des popups de la scène
     * @param editor       la vue d'éditeur ouverte
     * @param session       la session d'édition courante
     * @param margePx       la respiration aux bords de la couche
     * @param palette       la palette courante des popups
     * @param sautDiagnostic le consommateur des sauts de la bande
     */
    RightEdge(PopupManager popups, EditorView editor,
            TextSession session, int margePx, PopupPalette palette,
            IntConsumer sautDiagnostic) {
        this.popups = popups;
        this.editor = editor;
        this.session = session;
        this.margePx = margePx;
        this.sautDiagnostic = sautDiagnostic;
        PopupLayer couche = popups.couche();
        errorBand = new ErrorBandView(couche.getContext(), palette);
        minimap = new MinimapView(couche.getContext(), palette);
        fastScroll = new FastScrollerView(couche.getContext(), palette);
        popups.lier(PopupController.PopupKind.ERROR_BAND, errorBand);
        popups.lier(PopupController.PopupKind.MINIMAP, minimap);
        popups.lier(PopupController.PopupKind.FAST_SCROLLER, fastScroll);
        errorBand.setOnJump(sautDiagnostic::accept);
        minimap.setOnJump(line -> editor.scrollToFractionY(
                session.document().lineCount() <= 1 ? 0f
                        : line / (float) session.document().lineCount()));
        fastScroll.setOnScroll(fraction
                -> editor.scrollToFractionY((float) fraction));
    }

    /** La palette tourne : le chrome du bord droit la suit. */
    void surPalette(PopupPalette palette) {
        errorBand.setPalette(palette);
        minimap.setPalette(palette);
        fastScroll.setPalette(palette);
    }

    /**
     * La bande d'erreurs vit avec les diagnostics : les issues et les
     * gravités du fichier ouvert, pleine hauteur respirée au bord
     * droit ; sans diagnostic, la bande se ferme.
     */
    void rafraichirBande(List<HostDiagnostic> diagnostics, String nomFichier,
            int layerWidth, int layerHeight) {
        if (diagnostics.isEmpty()) {
            popups.close(PopupController.PopupKind.ERROR_BAND);
            return;
        }
        errorBand.setModel(ErrorBandModel.of(
                ContenuPopups.issues(diagnostics, nomFichier,
                        session.document()),
                ContenuPopups.gravites(diagnostics, nomFichier),
                session.document(), BAND_STEPS), BAND_STEPS);
        errorBand.setSpanPx(Math.max(1, layerHeight - 2 * margePx));
        PopupPlacer.Cadre placee = rightEdge(errorBand, 0,
                layerWidth, layerHeight);
        popups.show(PopupController.PopupKind.ERROR_BAND,
                placee.x(), placee.y());
    }

    /**
     * La minimap et le pouce se rafraîchissent : la silhouette, la
     * fenêtre visible, la fraction du défilement et la part visible —
     * la minimap vit à gauche du pouce, le pouce à gauche de la bande.
     */
    void refresh(int layerWidth, int layerHeight) {
        // La carte honnête : le document court garde SES rangees (une
        // par ligne — la miniature ne s'étire pas), le document long
        // plafonne au même nombre de crans serrés.
        int lines = session.document().lineCount();
        minimap.setModel(MinimapModel.of(session.document(),
                Math.max(1, Math.min(MINIMAP_ROWS, lines))), lines);
        minimap.setViewport(editor.firstVisibleLine(),
                editor.visibleLines(), session.document().lineCount());
        minimap.setSpanPx(Math.max(1, layerHeight - 2 * margePx));
        PopupPlacer.Cadre carte = rightEdge(minimap,
                measuredWidth(errorBand)
                        + measuredWidth(fastScroll),
                layerWidth, layerHeight);
        popups.show(PopupController.PopupKind.MINIMAP,
                carte.x(), carte.y());
        fastScroll.setState(editor.scrollFractionY(),
                editor.visibleFractionY());
        fastScroll.setSpanPx(Math.max(1, layerHeight - 2 * margePx));
        PopupPlacer.Cadre pouce = rightEdge(fastScroll,
                measuredWidth(errorBand), layerWidth, layerHeight);
        popups.show(PopupController.PopupKind.FAST_SCROLLER,
                pouce.x(), pouce.y());
    }

    /** La largeur mesurée d'une vue de bord — son empreinte au bord droit. */
    private int measuredWidth(View vue) {
        if (vue.getMeasuredWidth() <= 0) {
            size(vue);
        }
        return vue.getMeasuredWidth();
    }

    /**
     * Le cadre du bord droit — la largeur déjà prise à sa droite
     * déduite, pleine hauteur respirée.
     */
    private PopupPlacer.Cadre rightEdge(View vue, int priseADroite,
            int layerWidth, int layerHeight) {
        PopupPlacer.Cadre size = size(vue);
        int x = Math.max(margePx, layerWidth - margePx
                - size.width() - priseADroite);
        return new PopupPlacer.Cadre(x, margePx, size.width(),
                Math.max(margePx, layerHeight - 2 * margePx));
    }

    /** La taille mesurée d'une vue — mesure à la demande, jamais imposée. */
    private PopupPlacer.Cadre size(View vue) {
        vue.measure(View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED));
        return new PopupPlacer.Cadre(0, 0, vue.getMeasuredWidth(),
                vue.getMeasuredHeight());
    }
}
