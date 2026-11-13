package jo.celeris.demo;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.NavigationCommands;
import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.intelligence.Generateurs;
import jo.celeris.core.intelligence.Symbole;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SyntaxCoordinator;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

import org.jspecify.annotations.Nullable;

/**
 * L'atelier de génération et de gabarits de la démonstration (catégorie
 * D de l'inventaire — « Generate Code / Gabarits » et « Surround With /
 * Live Templates », les moteurs vivaient, la surface dormait) : la
 * carte des actions de génération au caret — constructeur, accesseurs,
 * equals/hashCode/toString lus des CHAMPS RÉELS de la classe englobante
 * (B-24), l'entourage de la sélection (try/catch, if), le gabarit de
 * méthode à trous liés (A-22, le geste de la refactorisation — un seul
 * canal, la carte est une peau de plus). Le choix génère et applique en
 * transaction annulable ; la classe inconnue ne propose que ce qui ne
 * la demande pas (l'entourage et le gabarit).
 *
 * <p>La frontière tient : la carte ne connaît NI les moteurs, NI
 * l'éditeur — l'atelier transpose le plan des déclarations du fichier
 * ouvert en actions ; la sélection vide ne propose pas d'entourage
 * (rien à entourer ne se devine jamais).</p>
 */
final class AtelierGabarits {

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final int margePx;
    private final IntentionsMenuView carte;
    /** Le plan du fichier ouvert — la classe englobante s'y lit. */
    private final Supplier<List<OutlineEntry>> plan;
    /** Le nom du fichier ouvert — la langue s'en déduit. */
    private final Supplier<String> nomFichier;
    /** Le geste du gabarit de méthode — le canal unique de la
     *  refactorisation (A-22), la carte n'est qu'une peau de plus. */
    private final Runnable gesteGabaritMethode;

    /**
     * Construit l'atelier de gabarits sur le coordinateur des popups :
     * la carte naît liée à son genre, le patron des trois temps (lier,
     * déclencher, déléguer — USAGE §9).
     *
     * @param editeur        la vue d'éditeur ouverte
     * @param session        la session d'édition courante
     * @param popups         le coordinateur des popups de la scène
     * @param margePx        la respiration des ancres aux bords, en pixels
     * @param palette        la palette courante des popups
     * @param plan           le plan courant du fichier ouvert
     * @param nomFichier     le nom du fichier ouvert
     * @param gesteGabaritMethode le geste d'insertion du gabarit de
     *                            méthode (A-22) — le canal unique de la
     *                            refactorisation, la carte n'est qu'une
     *                            peau de plus
     */
    AtelierGabarits(EditorView editeur, TextSession session,
            PopupManager popups, int margePx, PopupPalette palette,
            Supplier<List<OutlineEntry>> plan, Supplier<String> nomFichier,
            Runnable gesteGabaritMethode) {
        this.editeur = editeur;
        this.session = session;
        this.popups = popups;
        this.margePx = margePx;
        this.plan = plan;
        this.nomFichier = nomFichier;
        this.gesteGabaritMethode = gesteGabaritMethode;
        this.carte = new IntentionsMenuView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.GABARITS, carte);
    }

    /** La palette tourne : la carte de gabarits la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Ouvre la carte de génération au caret : les actions se lisent du
     * document réel — la classe englobante porte ses champs, la
     * sélection porte son entourage —, la carte se pose sous le
     * curseur, bornée. Sans aucune action servie, la carte ne se pose
     * pas.
     */
    void ouvrir() {
        List<IntentionsMenuView.Item> items = new ArrayList<>();
        String classe = classeEnglobante();
        if (classe != null) {
            List<Generateurs.Champ> champs = surAnalyse(
                    analyse -> Generateurs.champs(session.document(),
                            analyse, classe));
            if (champs == null) {
                return;
            }
            ajouterClasse(items, classe, champs);
        }
        ajouterEntourage(items);
        ajouterGabaritMethode(items);
        if (items.isEmpty()) {
            return;
        }
        carte.setSection(popups.couche().getContext().getString(
                R.string.section_gabarits));
        carte.setActions(items);
        montrer();
    }

    /** Le défilement a déplacé le curseur : la carte ouverte se
     *  REPOSITIONNE — le genre suit les régimes du menu du caret. */
    void repositionner() {
        if (popups.estOuvert(PopupController.PopupKind.GABARITS)) {
            montrer();
        }
    }

    /**
     * Les actions de génération de la classe : constructeur toujours,
     * accesseurs quand les champs existent, le trio
     * equals/hashCode/toString toujours — la classe sert son règlement.
     */
    private void ajouterClasse(List<IntentionsMenuView.Item> items,
            String classe, List<Generateurs.Champ> champs) {
        items.add(new IntentionsMenuView.Item(
                popups.couche().getContext().getString(
                        R.string.gabarits_constructeur, classe),
                () -> generer(Generateurs.constructeur(classe, champs))));
        if (!champs.isEmpty()) {
            items.add(new IntentionsMenuView.Item(
                    popups.couche().getContext().getString(
                            R.string.gabarits_accesseurs, classe),
                    () -> generer(Generateurs.accesseurs(champs))));
        }
        items.add(new IntentionsMenuView.Item(
                popups.couche().getContext().getString(
                        R.string.gabarits_equality, classe),
                () -> generer(
                        Generateurs.equalsHashCodeToString(classe, champs))));
    }

    /** Les actions d'entourage : la sélection NON VIDE seule s'y
     *  propose — rien à entourer ne se devine jamais. */
    private void ajouterEntourage(List<IntentionsMenuView.Item> items) {
        if (!selectionUniqueNonVide()) {
            return;
        }
        items.add(new IntentionsMenuView.Item(
                popups.couche().getContext().getString(
                        R.string.gabarits_entourer_try),
                () -> entourer("try {",
                        "} catch (Exception e) {\n    throw e;\n}")));
        items.add(new IntentionsMenuView.Item(
                popups.couche().getContext().getString(
                        R.string.gabarits_entourer_if),
                () -> entourer("if (condition) {", "}")));
    }

    /** Le gabarit de méthode — le canal de la refactorisation, une
     *  peau de plus : la carte ne duplique jamais la logique. */
    private void ajouterGabaritMethode(List<IntentionsMenuView.Item> items) {
        items.add(new IntentionsMenuView.Item(
                popups.couche().getContext().getString(R.string.action_gabarit),
                () -> {
                    popups.fermer(PopupController.PopupKind.GABARITS);
                    gesteGabaritMethode.run();
                }));
    }

    /** Le geste de génération : le plan se calcule sur l'analyse
     *  fraîche, l'application appartient à la session — la carte se
     *  referme d'abord (le geste prend le relais). */
    private void generer(String genere) {
        popups.fermer(PopupController.PopupKind.GABARITS);
        EditPlan plan = surAnalyse(analyse -> planGenere(
                session.document(), analyse, classeEnglobante(), genere));
        if (plan == null || !session.apply(plan.transaction(), plan.selection())) {
            return;
        }
        selectionner();
        editeur.invalidate();
    }

    /** Le geste d'entourage : le plan ceint la sélection, l'application
     *  suit, le bloc ceint vit sélectionné. */
    private void entourer(String avant, String apres) {
        popups.fermer(PopupController.PopupKind.GABARITS);
        EditPlan plan = Entourer.entourer(session.document(),
                session.selection(), avant, apres);
        if (plan == null || !session.apply(plan.transaction(), plan.selection())) {
            return;
        }
        selectionner();
        editeur.invalidate();
    }

    /** La sélection courante se révèle à la vue — le caret suit sa
     *  sélection (le bloc ceint, la suite du généré). */
    private void selectionner() {
        List<SelRange> plages = session.selection().ranges();
        if (!plages.isEmpty()) {
            NavigationCommands.selectRange(editeur, plages.get(0).start(),
                    plages.get(0).end());
        }
    }

    /** Monte la carte mesurée sous le curseur, bornée — la pose ET la
     *  re-pose y passent (le genre suit le menu du caret). */
    private void montrer() {
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                curseur.left, curseur.bottom, taille(),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.GABARITS,
                placee.x(), placee.y());
    }

    /** La taille mesurée de la carte — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre taille() {
        return popups.taille(PopupController.PopupKind.GABARITS);
    }

    /**
     * Le plan d'insertion du code généré avant l'accolade fermante de
     * la classe (B-24) : le texte gagne son cran de membre, le caret se
     * porte à sa suite — pur, jamais le document muté.
     *
     * @param doc       le document, jamais muté
     * @param analyse   l'analyse courante du document
     * @param nomClasse la classe englobante du caret
     * @param genere    le texte généré par les moteurs
     * @return le plan du geste, null si la classe est inconnue
     */
    static @Nullable EditPlan planGenere(Document doc, Analysis analyse,
            @Nullable String nomClasse, String genere) {
        if (nomClasse == null) {
            return null;
        }
        Symbole classe = Generateurs.classeDe(doc, analyse,
                nomClasse);
        if (classe == null) {
            return null;
        }
        int position = Generateurs.positionInsertion(classe);
        String texte = "\n" + membre(genere) + "\n";
        return new EditPlan(
                Transaction.of(new TextEdit(position, "", texte)),
                MultiSelection.caretsAt(position + texte.length() - 1));
    }

    /** Le généré à son cran de membre : chaque ligne portante gagne
     *  quatre espaces — le fermant compris, les vides restent vides
     *  (les moteurs rendent au niveau zéro, la classe les reçoit à
     *  son niveau). */
    private static String membre(String genere) {
        StringBuilder rendu = new StringBuilder(genere.length() + 16);
        for (String ligne : genere.split("\n", -1)) {
            if (rendu.length() > 0) {
                rendu.append('\n');
            }
            if (!ligne.isBlank()) {
                rendu.append("    ");
            }
            rendu.append(ligne);
        }
        return rendu.toString();
    }

    /** La classe englobante du caret — l'entrée de TYPE la plus
     *  intime du plan ; null hors classe. */
    private @Nullable String classeEnglobante() {
        int caret = caret();
        String trouvee = null;
        for (OutlineEntry entree : plan.get()) {
            if (entree.category() == NodeCategory.TYPE
                    && entree.startOffset() <= caret
                    && caret <= entree.endOffset()) {
                trouvee = entree.name();
            }
        }
        return trouvee;
    }

    /** Vrai quand la sélection courante est une plage unique non vide. */
    private boolean selectionUniqueNonVide() {
        return session.selection().ranges().size() == 1
                && !session.selection().ranges().get(0).isEmpty();
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

    /** La position du curseur — la tête de la sélection courante. */
    private int caret() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).caret();
    }
}
