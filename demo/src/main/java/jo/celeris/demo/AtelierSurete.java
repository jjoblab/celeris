package jo.celeris.demo;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.android.widgets.ReferencesView;
import jo.celeris.core.intelligence.SafeDelete;
import jo.celeris.core.intelligence.SafeMove;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SyntaxCoordinator;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

import org.jspecify.annotations.Nullable;

/**
 * L'atelier de sûreté de la démonstration (catégorie D de l'inventaire
 * — « Safe Delete / Move », les moteurs B-23 servaient verdicts et
 * plans, les surfaces dormaient) : la suppression sûre demande son
 * verdict — les usages barrent s'affichent en carte (chacun saute à
 * son lieu, la suppression attend), la voie libre efface en
 * transaction annulable ; le déplacement sûr propose ses cibles — les
 * autres types du fichier —, les conditions de sûreté seules
 * décident, le refus s'annonce sans raison inventée. Le lu décide,
 * jamais le deviné.
 *
 * <p>La frontière tient : les cartes ne connaissent NI les moteurs,
 * NI l'éditeur — l'atelier transpose les verdicts du moteur en
 * rangées ; hors déclaration, le geste s'annonce et ne touche
 * rien.</p>
 */
final class AtelierSurete {

    /** L'extrait d'un usage se rogne à cette longueur — la carte
     *  reste lisible, jamais une rangée qui mange la couche. */
    private static final int EXTRAIT_MAX = 48;

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final int margePx;
    /** La carte des usages qui barrent — le verdict refusé s'y lit. */
    private final ReferencesView carteUsages;
    /** La carte des cibles — les types candidats au déménagement. */
    private final IntentionsMenuView carteCibles;
    /** Le plan du fichier ouvert — la classe englobante s'y lit. */
    private final Supplier<List<OutlineEntry>> plan;
    /** Le nom du fichier ouvert — la langue s'en déduit. */
    private final Supplier<String> nomFichier;
    /** Le canal des annonces — la bulle éphémère du coin bas-droit. */
    private final BiConsumer<String, String> annonces;

    /**
     * Construit l'atelier de sûreté sur le coordinateur des popups :
     * les cartes naissent liées à leurs genres, le patron des trois
     * temps (lier, déclencher, déléguer — USAGE §9).
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
    AtelierSurete(EditorView editeur, TextSession session,
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
        this.carteUsages = new ReferencesView(popups.couche().getContext(),
                palette);
        this.carteCibles = new IntentionsMenuView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.SAFE_DELETE, carteUsages);
        popups.lier(PopupController.PopupKind.SAFE_MOVE, carteCibles);
    }

    /** La palette tourne : les cartes de sûreté la suivent. */
    void surPalette(PopupPalette palette) {
        carteUsages.setPalette(palette);
        carteCibles.setPalette(palette);
    }

    /**
     * La suppression sûre : le verdict du moteur décide — les usages
     * barrent s'affichent en carte (chacun saute à son lieu), la voie
     * libre efface la déclaration en transaction annulable et
     * l'annonce ; hors déclaration, le geste s'annonce et ne touche
     * rien.
     */
    void supprimerSur() {
        SafeDelete.Verdict verdict = surAnalyse(
                analyse -> SafeDelete.delete(session.document(), analyse,
                        debutSelection(), finSelection()));
        if (verdict == null) {
            annoncer(R.string.suppression_aucune_declaration);
            return;
        }
        if (!verdict.usages().isEmpty()) {
            poserUsages(verdict);
            return;
        }
        if (session.apply(Transaction.of(verdict.edits()))) {
            editeur.invalidate();
            annoncer(R.string.suppression_effectuee);
        }
    }

    /**
     * Le déplacement sûr : les types candidats du fichier se
     * proposent en carte — le choix tente le plan, les conditions de
     * sûreté seules décident. Sans candidat, le geste s'annonce et ne
     * touche rien.
     */
    void deplacerDans() {
        List<String> cibles = cibles(plan.get(), caret());
        if (cibles.isEmpty()) {
            annoncer(R.string.deplacement_aucune_cible);
            return;
        }
        List<IntentionsMenuView.Item> items = new ArrayList<>(cibles.size());
        for (String cible : cibles) {
            items.add(new IntentionsMenuView.Item(cible,
                    () -> deplacer(cible)));
        }
        carteCibles.setSection(popups.couche().getContext().getString(
                R.string.section_deplacement));
        carteCibles.setActions(items);
        montrerCibles();
    }

    /** Le défilement a déplacé le curseur : la carte des cibles
     *  ouverte se REPOSITIONNE — le genre suit les régimes du menu du
     *  caret (la carte des usages, elle, cède au défilement). */
    void repositionner() {
        if (popups.estOuvert(PopupController.PopupKind.SAFE_MOVE)) {
            montrerCibles();
        }
    }

    /**
     * Les types candidats au déménagement — les types du fichier HORS
     * la classe qui enclot le caret (les conditions de sûreté du
     * moteur refuseront ce qui pend, jamais la carte ne préjuge).
     *
     * @param plan  le plan du fichier ouvert
     * @param caret la position du curseur
     * @return les noms des types candidats, dans l'ordre du fichier
     */
    static List<String> cibles(List<OutlineEntry> plan, int caret) {
        List<String> cibles = new ArrayList<>();
        for (OutlineEntry entree : plan) {
            if (entree.category() != NodeCategory.TYPE
                    || (entree.startOffset() <= caret
                            && caret <= entree.endOffset())) {
                continue;
            }
            cibles.add(entree.name());
        }
        return cibles;
    }

    /**
     * Les occurrences des usages qui barrent — le libellé lu du
     * document (« L12 · l'extrait rogné »), le saut vers l'offset ;
     * l'atelier y branche sa fermeture et son curseur.
     *
     * @param document le document porteur, jamais muté
     * @param usages   les usages barants du verdict
     * @param sauteur  le saut vers un offset, branché par l'hôte
     * @return les occurrences, dans l'ordre du verdict
     */
    static List<ReferencesView.Occurrence> occurrences(Document document,
            List<SafeDelete.Usage> usages, Consumer<Integer> sauteur) {
        List<ReferencesView.Occurrence> occurrences =
                new ArrayList<>(usages.size());
        for (SafeDelete.Usage usage : usages) {
            occurrences.add(new ReferencesView.Occurrence(
                    libelleDe(document, usage.offset()),
                    () -> sauteur.accept(usage.offset())));
        }
        return occurrences;
    }

    /** Le libellé de l'usage : « L12 · l'extrait de la ligne, rogné »
     *  — le lieu se lit sans quitter la carte. */
    private static String libelleDe(Document document, int offset) {
        int ligne = document.lineForOffset(offset);
        String extrait = document.lineText(ligne).strip();
        if (extrait.length() > EXTRAIT_MAX) {
            extrait = extrait.substring(0, EXTRAIT_MAX) + "…";
        }
        return "L" + (ligne + 1) + " · " + extrait;
    }

    /** La carte des usages qui barrent : l'en-tête compte, le choix
     *  saute — la suppression attend son heure. */
    private void poserUsages(SafeDelete.Verdict verdict) {
        popups.controleur().open(PopupController.PopupKind.SAFE_DELETE);
        carteUsages.setTitre(popups.couche().getContext().getString(
                R.string.titre_suppression, verdict.usages().size(),
                verdict.usages().get(0).text()));
        carteUsages.setOccurrences(occurrences(session.document(),
                verdict.usages(), this::sauter));
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                curseur.left, curseur.bottom,
                popups.taille(PopupController.PopupKind.SAFE_DELETE),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.SAFE_DELETE,
                placee.x(), placee.y());
    }

    /** Le saut d'un usage qui barre : la carte se referme, le curseur
     *  porte au lieu. */
    private void sauter(int offset) {
        popups.fermer(PopupController.PopupKind.SAFE_DELETE);
        session.setSelection(MultiSelection.of(SelRange.caretAt(offset)));
    }

    /** Le déménagement vers la cible : le plan du moteur, l'application
     *  en transaction annulable, l'annonce du refus SANS raison
     *  inventée — les conditions de sûreté seules décident. */
    private void deplacer(String cible) {
        popups.fermer(PopupController.PopupKind.SAFE_MOVE);
        SafeMove.MovePlan planMouvement = surAnalyse(
                analyse -> SafeMove.move(session.document(), analyse,
                        debutSelection(), finSelection(), cible));
        if (planMouvement == null) {
            annoncer(R.string.deplacement_refuse);
            return;
        }
        if (session.apply(Transaction.of(planMouvement.edits()))) {
            editeur.invalidate();
            annoncer(R.string.deplacement_effectue, cible);
        }
    }

    /** Monte la carte des cibles mesurée sous le curseur, bornée — la
     *  pose ET la re-pose y passent. */
    private void montrerCibles() {
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                curseur.left, curseur.bottom,
                popups.taille(PopupController.PopupKind.SAFE_MOVE),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.SAFE_MOVE,
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

    /** Le calcul mené sur une analyse FRAÎCHE — coordinateur ponctuel,
     *  fermé d'entrée ; null quand la langue ne s'analyse pas. */
    private <T> @Nullable T surAnalyse(Function<Analysis, T> calcul) {
        LanguageSupport support = LangagesDemo.langageDe(nomFichier.get(),
                null, session.document().text());
        if (support == null || support.syntax() == null) {
            return null;
        }
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                support.syntax());
        try {
            return calcul.apply(coordinateur.analysis());
        } finally {
            coordinateur.close();
        }
    }

    /** Le début de la sélection — la désignation s'y lit. */
    private int debutSelection() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).start();
    }

    /** La fin (exclusive) de la sélection — la désignation s'y borne. */
    private int finSelection() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).end();
    }

    /** La position du curseur — la tête de la sélection courante. */
    private int caret() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).caret();
    }
}
