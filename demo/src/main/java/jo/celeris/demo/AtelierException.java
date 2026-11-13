package jo.celeris.demo;

import android.graphics.Rect;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.NavigationCommands;
import jo.celeris.android.widgets.ExceptionView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;


/**
 * L'atelier d'exécution et d'exception de la démonstration (catégorie
 * E de l'inventaire — « Exception Details », la surface dormait) : le
 * lancement passe par la fausse exécution de référence (E-04 — les
 * lancements vivent, les fins se simulent), l'échec SIMULÉ porte la
 * trace du document réel (le type englobant du caret, la méthode
 * englobante au sommet — jamais deviné : le plan seul sait), la carte
 * des détails se pose sous le curseur : le type à l'encre d'erreur, le
 * message, la pile — le cadre du fichier ouvert SAUTE à sa ligne,
 * l'étranger vit muet. La carte vit le régime de la consultation
 * (frappe épargnée, défilement repositionné, geste extérieur et
 * échappement referment).
 *
 * <p>La frontière tient : la carte ne connaît NI l'exécution, NI
 * l'éditeur — l'atelier transpose la trace en cadres ; le fichier
 * sans méthode ne peut échouer — l'exécution s'annonce terminée.</p>
 */
final class AtelierException {

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final int margePx;
    private final ExceptionView carte;
    /** Le plan du fichier ouvert — la trace s'y lit. */
    private final Supplier<List<OutlineEntry>> plan;
    /** Le nom du fichier ouvert — la classe et les cadres s'y portent. */
    private final Supplier<String> nomFichier;
    /** Le canal des annonces — la bulle éphémère du coin bas-droit. */
    private final BiConsumer<String, String> annonces;

    /**
     * Construit l'atelier d'exception sur le coordinateur des popups :
     * la carte naît liée à son genre, le patron des trois temps
     * (lier, déclencher, déléguer — USAGE §9).
     *
     * @param editeur    la vue d'éditeur ouverte
     * @param session    la session d'édition courante
     * @param popups     le coordinateur des popups de la scène
     * @param margePx    la respiration des ancres aux bords, en pixels
     * @param palette    la palette courante des popups
     * @param plan       le plan courant du fichier ouvert
     * @param nomFichier le nom du fichier ouvert
     * @param annonces   le canal des annonces (titre, message)
     */
    AtelierException(EditorView editeur, TextSession session,
            PopupManager popups, int margePx, PopupPalette palette,
            Supplier<List<OutlineEntry>> plan, Supplier<String> nomFichier,
            BiConsumer<String, String> annonces) {
        this.editeur = editeur;
        this.session = session;
        this.popups = popups;
        this.margePx = margePx;
        this.plan = plan;
        this.nomFichier = nomFichier;
        this.annonces = annonces;
        this.carte = new ExceptionView(popups.couche().getContext(), palette);
        popups.lier(PopupController.PopupKind.EXCEPTION_DETAILS, carte);
    }

    /** La palette tourne : la carte d'exception la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Exécute le fichier ouvert — l'exécution simulée du terrain
     * (comme le faux build de lint simule l'analyse) : l'échec porte
     * la trace du document réel (le type et les méthodes du plan, la
     * ligne du caret au sommet), la carte des détails se pose ; le
     * fichier sans méthode ne peut échouer — l'exécution s'annonce
     * terminée.
     */
    void executer() {
        TraceExecution.Echec echec = TraceExecution.echec(
                "java.lang.Exception",
                popups.couche().getContext().getString(
                        R.string.execution_echec_message),
                nomFichier.get(), session.document(), plan.get(), caret());
        if (echec == null) {
            annoncer(R.string.execution_terminee);
            return;
        }
        annoncer(R.string.execution_echouee);
        poser(echec);
    }

    /** Le défilement a déplacé le curseur : la carte ouverte se
     *  REPOSITIONNE — le genre suit le régime de la consultation. */
    void repositionner() {
        if (popups.estOuvert(PopupController.PopupKind.EXCEPTION_DETAILS)) {
            montrer();
        }
    }

    /** Pose la carte de l'échec : le type et le message, la pile
     *  transposée — le cadre du fichier ouvert saute, l'étranger vit
     *  muet. */
    private void poser(TraceExecution.Echec echec) {
        carte.setException(echec.type(), echec.message());
        carte.setCadres(TraceExecution.cadres(echec, nomFichier.get(),
                this::sauter));
        montrer();
    }

    /** Le saut d'un cadre : la carte se referme, le curseur porte au
     *  début de la ligne du cadre. */
    private void sauter(int ligne) {
        popups.fermer(PopupController.PopupKind.EXCEPTION_DETAILS);
        int borne = Math.min(Math.max(1, ligne),
                session.document().lineCount());
        NavigationCommands.placeCaret(editeur,
                session.document().lineStart(borne - 1));
    }

    /** Monte la carte mesurée sous le curseur, bornée — la pose ET la
     *  re-pose y passent (la même carte se re-place). */
    private void montrer() {
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                curseur.left, curseur.bottom, taille(),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.EXCEPTION_DETAILS,
                placee.x(), placee.y());
    }

    /** La taille mesurée de la carte — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre taille() {
        return popups.taille(PopupController.PopupKind.EXCEPTION_DETAILS);
    }

    /** L'annonce par ressource — le canal de l'orchestreur la porte au
     *  coin bas-droit, avec ses arguments. */
    private void annoncer(int message, Object... arguments) {
        String titre = popups.couche().getContext().getString(
                R.string.titre_notification);
        annonces.accept(titre, popups.couche().getContext()
                .getString(message, arguments));
    }

    /** La position du curseur — la tête de la sélection courante. */
    private int caret() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).caret();
    }
}
