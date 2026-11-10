package jo.celeris.demo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.NavigationCommands;
import jo.celeris.android.widgets.FeuilleBas;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.intelligence.Formateur;
import jo.celeris.core.intelligence.Gabarit;
import jo.celeris.core.intelligence.Gabarits;
import jo.celeris.core.intelligence.ImportAuto;
import jo.celeris.core.intelligence.IndexSymboles;
import jo.celeris.core.intelligence.IndentStyle;
import jo.celeris.core.intelligence.Renommage;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.syntax.SyntaxCoordinator;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

import org.jspecify.annotations.Nullable;

/**
 * L'atelier de refactorisation de la démonstration (M7.5, §8) : les
 * gestes des moteurs purs branchés en transactions annulables — le
 * gabarit dynamique au curseur (A-22), le formatage du document
 * (B-25), le renommage du symbole et de ses usages (B-19, B-11),
 * l'import à la tête du bloc (B-27). Les plans se calculent en pur
 * sur l'instantané, l'application appartient à la session, par la
 * voie unique (ADR-003) — chaque geste se défait d'un Ctrl+Z.
 */
final class AtelierRefactor {

    /**
     * Le gabarit de méthode du terrain : le nom et le corps sont des
     * trous liés, le premier sélectionné à l'insertion — prêt à la
     * frappe (A-22).
     */
    static final String GABARIT_METHODE = """
            private void ${nom:nouvelleMethode}() {
                ${corps:}
            }""";

    /** L'activité hôtesse — la feuille de renommage y vit. */
    private final AppCompatActivity activite;
    /** La vue d'éditeur ouverte — l'invalidation et le dévoilement. */
    private final EditorView editeur;
    /** La session d'édition courante. */
    private final TextSession session;
    /** Le nom du fichier ouvert — la langue et l'uri s'en déduisent. */
    private final String nomFichier;
    /** Le verdict de la barre d'état — un identifiant de ressource. */
    private final Verdict verdict;
    /** La palette courante des feuilles, dérivée à chaque ouverture. */
    private final Supplier<PopupPalette> palettes;

    /** La feuille de renommage — née à sa première demande. */
    private @Nullable FeuilleBas feuilleRennomage;
    /** Le champ de la feuille de renommage. */
    private @Nullable EditText champRennomage;

    /** Le verdict de l'atelier : un message à la barre d'état de l'hôte. */
    interface Verdict {

        /** Publie le message — l'hôte décide de sa barre. */
        void publier(CharSequence message);
    }

    /**
     * Construit l'atelier de refactorisation.
     *
     * @param activite   l'activité hôtesse de la feuille de renommage
     * @param editeur    la vue d'éditeur ouverte
     * @param session    la session d'édition courante
     * @param nomFichier le nom du fichier ouvert
     * @param palettes   la palette courante, dérivée à chaque ouverture
     * @param verdict    le canal des messages d'état
     */
    AtelierRefactor(AppCompatActivity activite, EditorView editeur,
            TextSession session, String nomFichier,
            Supplier<PopupPalette> palettes, Verdict verdict) {
        this.activite = activite;
        this.editeur = editeur;
        this.session = session;
        this.nomFichier = nomFichier;
        this.palettes = palettes;
        this.verdict = verdict;
    }

    /**
     * Le plan d'insertion d'un gabarit au curseur : le rendu remplace
     * la sélection, le premier trou vit sélectionné (A-22).
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante
     * @param corps     le corps du gabarit, balises comprises
     * @return le plan du geste
     */
    static EditPlan planGabarit(Document doc, MultiSelection selection,
            String corps) {
        return Gabarits.inserer(doc, selection, Gabarit.of(corps));
    }

    /**
     * Le plan de formatage du document entier : l'indentation lue dans
     * l'arbre, quatre espaces (B-25). Null quand le texte est déjà
     * en forme — le geste muet ne crée pas d'étape d'annulation.
     *
     * @param doc     le document, jamais muté
     * @param analyse l'analyse courante
     * @return le plan du geste, null si rien à changer
     */
    static @Nullable EditPlan planFormat(Document doc, Analysis analyse) {
        String formate = Formateur.formater(doc, analyse,
                IndentStyle.ESPACES_4);
        if (formate.equals(doc.text())) {
            return null;
        }
        return new EditPlan(
                Transaction.of(new TextEdit(0, doc.text(), formate)),
                MultiSelection.caretsAt(0));
    }

    /**
     * Le plan de renommage : les usages ENTIERS du nom à travers le
     * fichier indexé — déclaration et usages, jamais dans les chaînes
     * (B-19, B-11) —, les conflits signalés à côté.
     *
     * @param doc     le document, jamais muté
     * @param analyse l'analyse courante
     * @param uri     l'uri du fichier ouvert
     * @param ancien  le nom actuel du symbole
     * @param nouveau le nom demandé
     * @return le plan, ses conflits compris
     */
    static Renommage.PlanRenommage planRenommage(Document doc,
            Analysis analyse, String uri, String ancien, String nouveau) {
        IndexSymboles index = IndexSymboles.vide();
        index.remplacer(uri, doc, analyse);
        return Renommage.plan(index, ancien, nouveau);
    }

    /**
     * Applique les éditions du fichier ouvert d'un plan de renommage :
     * UNE transaction, annulable d'un geste — la sélection se projette.
     *
     * @param session la session d'édition
     * @param uri     l'uri du fichier ouvert
     * @param plan    le plan de renommage
     * @return vrai si le fichier avait des éditions à porter
     */
    static boolean appliquerRenommage(TextSession session,
            String uri, Renommage.PlanRenommage plan) {
        List<TextEdit> editions = plan.editions().get(uri);
        if (editions == null || editions.isEmpty()) {
            return false;
        }
        return session.apply(Transaction.of(editions));
    }

    /**
     * Le plan d'import : la ligne à la tête du bloc, sélection au
     * début de l'insert (B-27) — null quand l'import existe déjà.
     *
     * @param doc           le document, jamais muté
     * @param importComplet la ligne d'import, saut de ligne compris
     * @return le plan du geste, null si déjà présent
     */
    static @Nullable EditPlan planImport(Document doc, String importComplet) {
        return ImportAuto.importer(doc, importComplet);
    }

    // ---------------------------------------------------------- gestes

    /**
     * A-22 — le gabarit de méthode au curseur : le rendu remplace la
     * sélection, le premier trou vit sélectionné, prêt à la frappe —
     * transaction annulable d'un geste.
     */
    void insererGabarit() {
        EditPlan plan = planGabarit(session.document(), session.selection(),
                GABARIT_METHODE);
        if (session.apply(plan.transaction(), plan.selection())) {
            SelRange premiere = session.selection().ranges().get(0);
            NavigationCommands.selectRange(editeur, premiere.start(),
                    premiere.end());
            editeur.invalidate();
        }
    }

    /**
     * B-25 — le formatage du document : l'indentation lue dans
     * l'arbre, une transaction annulable — le geste muet (rien à
     * changer) ne crée pas d'étape.
     */
    void formaterDocument() {
        EditPlan plan = planSurAnalyse(
                analyse -> planFormat(session.document(), analyse));
        if (plan == null) {
            verdict.publier(activite.getString(R.string.format_deja_en_forme));
            return;
        }
        session.apply(plan.transaction(), plan.selection());
        editeur.invalidate();
    }

    /**
     * B-19, B-11 — le renommage du symbole au curseur : les usages
     * ENTIERS portent le nouveau nom, une transaction annulable — le
     * nouveau nom se demande en feuille du bas (ADR-018, C12 : le
     * champ tient le focus, la feuille vit hors de la couche), les
     * conflits se disent.
     */
    void renommerSymbole() {
        List<SelRange> plages = session.selection().ranges();
        int curseur = plages.isEmpty() ? 0 : plages.get(0).caret();
        String symbole = symbolAtCaret(curseur);
        if (symbole.isEmpty()) {
            verdict.publier(activite.getString(R.string.renomme_aucun_symbole));
            return;
        }
        FeuilleBas feuille = feuilleRennomage;
        if (feuille == null) {
            FeuilleBas neuve = new FeuilleBas(activite, palettes.get());
            neuve.libelleFermeture(
                    activite.getString(R.string.action_fermer));
            View contenu = activite.getLayoutInflater()
                    .inflate(R.layout.feuille_renommage, null);
            neuve.contenu(contenu);
            champRennomage = contenu.findViewById(R.id.champ_renommage);
            neuve.findViewById(R.id.bouton_renommage_annuler)
                    .setOnClickListener(vue -> neuve.cacher());
            neuve.findViewById(R.id.bouton_renommage_appliquer)
                    .setOnClickListener(vue -> {
                        EditText champ = champRennomage;
                        if (champ != null) {
                            neuve.cacher();
                            renommer(symbole,
                                    champ.getText().toString().trim());
                        }
                    });
            feuilleRennomage = neuve;
            feuille = neuve;
        }
        EditText champ = champRennomage;
        if (champ != null) {
            champ.setText(symbole);
            champ.setSelection(symbole.length());
        }
        feuille.titre(activite.getString(R.string.titre_renomme, symbole));
        feuille.setPalette(palettes.get());
        feuille.montrer(hote());
        if (champ != null) {
            champ.requestFocus();
        }
    }

    /** La touche retour rencontre la feuille — ouverte, elle se referme. */
    boolean surRetour() {
        FeuilleBas feuille = feuilleRennomage;
        return feuille != null && feuille.surRetour();
    }

    /** Le cadre de contenu de la fenêtre — toujours un FrameLayout. */
    private ViewGroup hote() {
        return (ViewGroup) activite.findViewById(android.R.id.content);
    }

    /** Le renommage effectif : le plan sur l'analyse fraîche, les conflits se disent. */
    private void renommer(String ancien, String nouveau) {
        if (nouveau.isEmpty() || nouveau.equals(ancien)) {
            return;
        }
        String uri = "file:///" + nomFichier;
        Renommage.PlanRenommage plan = planSurAnalyse(
                analyse -> planRenommage(session.document(), analyse, uri,
                        ancien, nouveau));
        if (plan == null) {
            return;
        }
        if (!plan.conflits().isEmpty()) {
            verdict.publier(activite.getString(R.string.renomme_conflit,
                    plan.conflits().get(0).nom()));
            return;
        }
        if (appliquerRenommage(session, uri, plan)) {
            editeur.invalidate();
        }
    }

    /**
     * B-27 — l'import à la tête du bloc : le doublon se refuse, le
     * geste muet ne crée pas d'étape.
     */
    void importerListe() {
        EditPlan plan = planImport(session.document(),
                "import java.util.List;\n");
        if (plan == null) {
            verdict.publier(activite.getString(R.string.import_deja_present));
            return;
        }
        session.apply(plan.transaction(), plan.selection());
        editeur.invalidate();
    }

    /** Le plan calculé sur une analyse FRAÎCHE — coordinateur ponctuel, fermé d'entrée. */
    private <T> @Nullable T planSurAnalyse(Function<Analysis, T> calcul) {
        LanguageSupport support = LangagesDemo.langageDe(nomFichier,
                null, session.document().text());
        if (support == null || support.syntax() == null) {
            return null;
        }
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session, support.syntax());
        try {
            return calcul.apply(coordinateur.analysis());
        } finally {
            coordinateur.close();
        }
    }

    /** Le symbole sous le curseur — l'identifiant Java qui le porte, vide au vide. */
    private String symbolAtCaret(int curseur) {
        String texte = session.document().text();
        int borne = Math.min(curseur, texte.length());
        int debut = borne;
        while (debut > 0 && Character.isJavaIdentifierPart(
                texte.charAt(debut - 1))) {
            debut--;
        }
        int fin = borne;
        while (fin < texte.length() && Character.isJavaIdentifierPart(
                texte.charAt(fin))) {
            fin++;
        }
        return texte.substring(debut, fin);
    }
}
