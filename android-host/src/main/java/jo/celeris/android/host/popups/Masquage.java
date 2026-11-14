package jo.celeris.android.host.popups;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import jo.celeris.android.host.R;
import jo.celeris.android.host.spi.AnalyseurLint;
import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.intelligence.SuppressionInspection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * Le masquage d'inspection (la septième surface portée de la
 * démonstration à l'orchestrateur, 2026-11-14 — catégorie B
 * de l'inventaire — « Suppress Inspection », le geste dormait) : les
 * diagnostics de la ligne du caret qui portent une RÈGLE (l'identifiant
 * entre crochets du lint) se proposent en carte — le choix pose le
 * commentaire « noinspection » en fin de ligne, l'analyse se relance :
 * la règle masquée ne se rapporte plus, les autres demeurent. Le
 * diagnostic sans règle ne se masque pas — jamais un masque sans
 * identifiant, jamais une règle devinée.
 *
 * <p>La frontière tient : la carte ne connaît NI le lint, NI
 * l'éditeur — l'atelier transpose les diagnostics courants en
 * actions ; sans diagnostic réglé à la ligne, le geste s'annonce et ne
 * touche rien.</p>
 */
final class Masquage {

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final int margePx;
    private final IntentionsMenuView carte;
    /** Les diagnostics courants de l'hôte — lint ou serveur. */
    private final Supplier<List<DiagnosticHote>> diagnostics;
    /** Le nom du fichier ouvert — le filtre des diagnostics s'y fait. */
    private final Supplier<String> nomFichier;
    /** La relance de l'analyse lint — le masquage ne vaut
     *  qu'appliqué ; null : le geste s'abrège. */
    private final Supplier<Runnable> relancerLint;
    /** Le canal des annonces — la bulle éphémère du coin bas-droit. */
    private final BiConsumer<String, String> annonces;

    /**
     * Construit le masquage sur le coordinateur des popups :
     * la carte naît liée à son genre, le patron des trois temps
     * (lier, déclencher, déléguer — USAGE §9).
     *
     * @param editeur      la vue d'éditeur ouverte
     * @param session      la session d'édition courante
     * @param popups       le coordinateur des popups de la scène
     * @param margePx      la respiration des ancres aux bords, en pixels
     * @param palette      la palette courante des popups
     * @param diagnostics  les diagnostics courants, vifs à l'hôte
     * @param nomFichier   le nom du fichier ouvert
     * @param relancerLint la relance de l'analyse après le masquage,
     *                     null abrège le geste
     * @param annonces     le canal des annonces (titre, message)
     */
    Masquage(EditorView editeur, TextSession session,
            PopupManager popups, int margePx, PopupPalette palette,
            Supplier<List<DiagnosticHote>> diagnostics,
            Supplier<String> nomFichier, Supplier<Runnable> relancerLint,
            BiConsumer<String, String> annonces) {
        this.editeur = editeur;
        this.session = session;
        this.popups = popups;
        this.margePx = margePx;
        this.diagnostics = diagnostics;
        this.nomFichier = nomFichier;
        this.relancerLint = relancerLint;
        this.annonces = annonces;
        this.carte = new IntentionsMenuView(
                popups.couche().getContext(), palette);
        popups.lier(PopupController.PopupKind.SUPPRESS_INSPECTION, carte);
    }

    /** La palette tourne : la carte de masquage la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Ouvre la carte de masquage : les diagnostics réglés de la ligne
     * du caret se proposent — le choix pose la marque et relance
     * l'analyse. Sans diagnostic réglé, le geste s'annonce et ne
     * touche rien.
     */
    void ouvrir() {
        List<IntentionsMenuView.Item> items = new ArrayList<>();
        int ligne = ligneDuCaret();
        for (DiagnosticHote diagnostic : diagnostics.get()) {
            String regle = regleDe(diagnostic, ligne);
            if (regle != null) {
                items.add(new IntentionsMenuView.Item(
                        popups.couche().getContext().getString(
                                R.string.suppression_item, regle),
                        () -> masquer(ligne, regle)));
            }
        }
        if (items.isEmpty()) {
            annoncer(R.string.suppression_aucune_inspection);
            return;
        }
        carte.setSection(popups.couche().getContext().getString(
                R.string.section_suppression));
        carte.setActions(items);
        montrer();
    }

    /** Le défilement a déplacé le curseur : la carte ouverte se
     *  REPOSITIONNE — le genre suit les régimes du menu du caret. */
    void repositionner() {
        if (popups.estOuvert(PopupController.PopupKind.SUPPRESS_INSPECTION)) {
            montrer();
        }
    }

    /** La règle du diagnostic porté par la ligne du fichier ouvert —
     *  vide ou étrangère, la règle ne se devine jamais : null. */
    private @Nullable String regleDe(DiagnosticHote diagnostic, int ligne) {
        if (diagnostic.ligne() != ligne
                || !diagnostic.chemin().nom().equals(nomFichier.get())) {
            return null;
        }
        String regle = AnalyseurLint.regle(diagnostic.message());
        return regle.isEmpty() ? null : regle;
    }

    /** Le geste de masquage : la marque se pose en fin de la ligne
     *  captée à l'ouverture, la transaction s'applique, l'analyse se
     *  relance — la règle masquée ne se rapporte plus. */
    private void masquer(int ligne, String regle) {
        EditPlan plan = SuppressionInspection.masquer(session.document(),
                ligne, regle);
        if (plan == null) {
            return;
        }
        popups.fermer(PopupController.PopupKind.SUPPRESS_INSPECTION);
        if (session.apply(plan.transaction(), plan.selection())) {
            editeur.invalidate();
            Runnable relance = relancerLint.get();
            if (relance != null) {
                relance.run();
            }
            annoncer(R.string.suppression_inspection_effectuee, regle);
        }
    }

    /** Monte la carte mesurée sous le curseur, bornée — la pose ET la
     *  re-pose y passent. */
    private void montrer() {
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                curseur.left, curseur.bottom,
                popups.taille(PopupController.PopupKind.SUPPRESS_INSPECTION),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.SUPPRESS_INSPECTION,
                placee.x(), placee.y());
    }

    /** L'annonce par ressource — le canal de l'orchestreur la porte au
     *  coin bas-droit, avec ses arguments. */
    private void annoncer(int message, Object... arguments) {
        String titre = popups.couche().getContext().getString(
                R.string.titre_notification);
        annonces.accept(titre, popups.couche().getContext()
                .getString(message, arguments));
    }

    /** La ligne du caret — à partir de un, comme les diagnostics. */
    private int ligneDuCaret() {
        List<SelRange> plages = session.selection().ranges();
        int caret = plages.isEmpty() ? 0 : plages.get(0).caret();
        return session.document().lineForOffset(caret) + 1;
    }
}
