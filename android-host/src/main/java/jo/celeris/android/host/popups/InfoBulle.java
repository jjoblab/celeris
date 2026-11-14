package jo.celeris.android.host.popups;

import android.graphics.Rect;

import java.util.List;
import java.util.function.Supplier;

import jo.celeris.android.host.R;
import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.DiagnosticTooltipView;
import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * L'info-bulle de diagnostic (le satellite de la fiche — la pose
 * déménage du corps de l'orchestrateur au satellite, 2026-11-14) :
 * la fiche EMPILE les problèmes du lieu (l'audit Android Studio :
 * l'IDE de référence ne sert jamais une bulle par diagnostic — la
 * décision pure vit chez {@link ContenuPopups#problemesDe}) et
 * l'INDICE D'ACTIONS clôt (« Plus d'actions · menu d'intentions »,
 * l'équivalent du lien « More actions… » de l'IDE) quand des
 * intentions existent — l'IDE ne promet pas de correctifs sans
 * correctifs. Fermée à la frappe et au défilement par le contrôleur ;
 * l'ancre vit chez {@link PopupAnchor#atPoint}.
 */
final class InfoBulle {

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final int margePx;
    private final DiagnosticTooltipView carte;
    /** Les diagnostics courants de l'hôte — vifs au déclenchement. */
    private final Supplier<List<DiagnosticHote>> diagnostics;
    /** Le nom du fichier ouvert — le filtre des diagnostics s'y fait. */
    private final Supplier<String> nomFichier;
    /** Les intentions du menu — l'indice d'actions ne se promet pas
     *  sans correctifs. */
    private final Supplier<List<IntentionsMenuView.Item>> intentions;

    /**
     * Construit l'info-bulle sur le coordinateur des popups : la carte
     * naît liée à son genre, le patron des trois temps (lier,
     * déclencher, déléguer — USAGE §9).
     *
     * @param editeur     la vue d'éditeur ouverte
     * @param session     la session d'édition courante
     * @param popups      le coordinateur des popups de la scène
     * @param margePx     la respiration des ancres aux bords, en pixels
     * @param palette     la palette courante des popups
     * @param diagnostics les diagnostics de l'hôte, vifs
     * @param nomFichier  le nom du fichier ouvert
     * @param intentions  les intentions du menu d'actions, vives
     */
    InfoBulle(EditorView editeur, TextSession session, PopupManager popups,
            int margePx, PopupPalette palette,
            Supplier<List<DiagnosticHote>> diagnostics,
            Supplier<String> nomFichier,
            Supplier<List<IntentionsMenuView.Item>> intentions) {
        this.editeur = editeur;
        this.session = session;
        this.popups = popups;
        this.margePx = margePx;
        this.diagnostics = diagnostics;
        this.nomFichier = nomFichier;
        this.intentions = intentions;
        this.carte = new DiagnosticTooltipView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP, carte);
    }

    /** La palette tourne : la fiche la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Rafraîchit la fiche au curseur : les problèmes du lieu s'y
     * empilent (l'audit Android Studio — la décision pure vit chez
     * {@link ContenuPopups#problemesDe}), l'indice d'actions clôt
     * quand des intentions existent. Sans problème au lieu, la fiche
     * ne se pose pas.
     *
     * @param selection la sélection courante
     */
    void rafraichir(MultiSelection selection) {
        List<SelRange> plages = selection.ranges();
        if (plages.size() != 1) {
            return;
        }
        int ligne = session.document().lineForOffset(plages.get(0).caret());
        List<DiagnosticTooltipView.Probleme> problemes =
                ContenuPopups.problemesDe(diagnostics.get(),
                        nomFichier.get(), ligne);
        if (problemes.isEmpty()) {
            return;
        }
        popups.controleur().open(
                PopupController.PopupKind.DIAGNOSTIC_TOOLTIP);
        carte.setProblemes(problemes, indiceDActions());
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.atPoint(
                curseur.left, curseur.top,
                popups.taille(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP,
                placee.x(), placee.y());
    }

    /** L'indice d'actions qui clôt la fiche — le lien du bas de
     *  l'IDE de référence (« More actions… ») : il ne se pose que
     *  quand des intentions existent au caret (l'IDE ne promet pas
     *  de correctifs sans correctifs). */
    private @Nullable String indiceDActions() {
        if (intentions.get().isEmpty()) {
            return null;
        }
        return popups.couche().getContext().getString(R.string.indice_actions);
    }

}
