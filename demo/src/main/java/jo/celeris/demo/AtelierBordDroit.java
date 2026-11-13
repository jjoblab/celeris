package jo.celeris.demo;

import android.view.View;

import java.util.List;
import java.util.function.IntConsumer;

import jo.celeris.android.host.spi.DiagnosticHote;
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
 * L'atelier du bord droit de la couche (chantier C9) : la bande
 * d'erreurs, la minimap et le défilement rapide — extraits
 * d'AtelierPopups qui crevait ses plafonds (ADR-002 : 600 lignes par
 * fichier, complexité de classe 80). La bande d'erreurs vit avec les
 * diagnostics ; la minimap porte la silhouette du document, sa fenêtre
 * visible à l'encre d'accent — l'appui et le glissement sautent, le
 * modèle se reconstruit à l'édition ; le pouce du défilement rapide
 * suit la fraction du défilement, sa hauteur la part visible — tout
 * tient : le pouce se tait, la bande reste muette.
 */
final class AtelierBordDroit {

    /** Le nombre de crans de la bande d'erreurs. */
    private static final int CRANS_BANDE = 200;
    /** Le plafond de rangees de la minimap — le document court compte
     *  les siennes, une par ligne. */
    private static final int RANGEES_MINIMAP = 200;

    private final PopupManager popups;
    private final EditorView editeur;
    private final TextSession session;
    private final ErrorBandView bandeErreurs;
    private final MinimapView minimap;
    private final FastScrollerView defilementRapide;
    private final IntConsumer sautDiagnostic;
    private final int margePx;

    AtelierBordDroit(PopupManager popups, EditorView editeur,
            TextSession session, int margePx, PopupPalette palette,
            IntConsumer sautDiagnostic) {
        this.popups = popups;
        this.editeur = editeur;
        this.session = session;
        this.margePx = margePx;
        this.sautDiagnostic = sautDiagnostic;
        PopupLayer couche = popups.couche();
        bandeErreurs = new ErrorBandView(couche.getContext(), palette);
        minimap = new MinimapView(couche.getContext(), palette);
        defilementRapide = new FastScrollerView(couche.getContext(), palette);
        popups.lier(PopupController.PopupKind.ERROR_BAND, bandeErreurs);
        popups.lier(PopupController.PopupKind.MINIMAP, minimap);
        popups.lier(PopupController.PopupKind.FAST_SCROLLER, defilementRapide);
        bandeErreurs.setOnJump(sautDiagnostic::accept);
        minimap.setOnJump(ligne -> editeur.scrollToFractionY(
                session.document().lineCount() <= 1 ? 0f
                        : ligne / (float) session.document().lineCount()));
        defilementRapide.setOnScroll(fraction
                -> editeur.scrollToFractionY((float) fraction));
    }

    /** La palette tourne : le chrome du bord droit la suit. */
    void surPalette(PopupPalette palette) {
        bandeErreurs.setPalette(palette);
        minimap.setPalette(palette);
        defilementRapide.setPalette(palette);
    }

    /**
     * La bande d'erreurs vit avec les diagnostics : les issues et les
     * gravités du fichier ouvert, pleine hauteur respirée au bord
     * droit ; sans diagnostic, la bande se ferme.
     */
    void rafraichirBande(List<DiagnosticHote> diagnostics, String nomFichier,
            int largeurCouche, int hauteurCouche) {
        if (diagnostics.isEmpty()) {
            popups.fermer(PopupController.PopupKind.ERROR_BAND);
            return;
        }
        bandeErreurs.setModel(ErrorBandModel.of(
                PopupCoach.issues(diagnostics, nomFichier,
                        session.document()),
                PopupCoach.gravites(diagnostics, nomFichier),
                session.document(), CRANS_BANDE), CRANS_BANDE);
        bandeErreurs.setSpanPx(Math.max(1, hauteurCouche - 2 * margePx));
        PopupPlacer.Cadre placee = bordDroit(bandeErreurs, 0,
                largeurCouche, hauteurCouche);
        popups.montrer(PopupController.PopupKind.ERROR_BAND,
                placee.x(), placee.y());
    }

    /**
     * La minimap et le pouce se rafraîchissent : la silhouette, la
     * fenêtre visible, la fraction du défilement et la part visible —
     * la minimap vit à gauche du pouce, le pouce à gauche de la bande.
     */
    void rafraichir(int largeurCouche, int hauteurCouche) {
        // La carte honnête : le document court garde SES rangees (une
        // par ligne — la miniature ne s'étire pas), le document long
        // plafonne au même nombre de crans serrés.
        int lignes = session.document().lineCount();
        minimap.setModel(MinimapModel.of(session.document(),
                Math.max(1, Math.min(RANGEES_MINIMAP, lignes))), lignes);
        minimap.setViewport(editeur.premiereLigneVisible(),
                editeur.lignesVisibles(), session.document().lineCount());
        minimap.setSpanPx(Math.max(1, hauteurCouche - 2 * margePx));
        PopupPlacer.Cadre carte = bordDroit(minimap,
                largeurMesuree(bandeErreurs)
                        + largeurMesuree(defilementRapide),
                largeurCouche, hauteurCouche);
        popups.montrer(PopupController.PopupKind.MINIMAP,
                carte.x(), carte.y());
        defilementRapide.setEtat(editeur.scrollFractionY(),
                editeur.visibleFractionY());
        defilementRapide.setSpanPx(Math.max(1, hauteurCouche - 2 * margePx));
        PopupPlacer.Cadre pouce = bordDroit(defilementRapide,
                largeurMesuree(bandeErreurs), largeurCouche, hauteurCouche);
        popups.montrer(PopupController.PopupKind.FAST_SCROLLER,
                pouce.x(), pouce.y());
    }

    /** La largeur mesurée d'une vue de bord — son empreinte au bord droit. */
    private int largeurMesuree(View vue) {
        if (vue.getMeasuredWidth() <= 0) {
            taille(vue);
        }
        return vue.getMeasuredWidth();
    }

    /**
     * Le cadre du bord droit — la largeur déjà prise à sa droite
     * déduite, pleine hauteur respirée.
     */
    private PopupPlacer.Cadre bordDroit(View vue, int priseADroite,
            int largeurCouche, int hauteurCouche) {
        PopupPlacer.Cadre taille = taille(vue);
        int x = Math.max(margePx, largeurCouche - margePx
                - taille.largeur() - priseADroite);
        return new PopupPlacer.Cadre(x, margePx, taille.largeur(),
                Math.max(margePx, hauteurCouche - 2 * margePx));
    }

    /** La taille mesurée d'une vue — mesure à la demande, jamais imposée. */
    private PopupPlacer.Cadre taille(View vue) {
        vue.measure(View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED));
        return new PopupPlacer.Cadre(0, 0, vue.getMeasuredWidth(),
                vue.getMeasuredHeight());
    }
}
