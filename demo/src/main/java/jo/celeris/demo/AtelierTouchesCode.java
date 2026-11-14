package jo.celeris.demo;

import android.content.Context;

import java.util.List;
import java.util.function.Supplier;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.TouchBarView;
import jo.celeris.core.intelligence.CommentRules;
import jo.celeris.core.intelligence.Commenter;
import jo.celeris.core.intelligence.IndentStyle;
import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.intelligence.LineActions;
import jo.celeris.core.language.model.LanguageConfiguration;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import java.util.ArrayList;

/**
 * L'atelier des touches de code (le redressement du propriétaire, le
 * design du dépôt de départ) : la BARRE DE TOUCHES vit au-dessus du
 * clavier — Tab, commentaire, déplacements de lignes, duplication —
 * et chaque touche branche un moteur PUR de la maison : le plan
 * d'édition part à la session, l'image suit. Les touches « vers le
 * haut » et « vers le bas » gardent leur état vivant au caret : la
 * première ligne n'y monte pas, la dernière n'y descend pas.
 *
 * <p>L'activité y pose la langue du fichier ouvert (fournisseur) :
 * le style d'indentation et les règles de commentaire s'y dérivent —
 * le canonique quatre espaces et le commentaire neutre tiennent sans
 * langage.</p>
 */
final class AtelierTouchesCode {

    private final Context contexte;
    private final EditorView editeur;
    private final TextSession session;
    private final Supplier<LanguageSupport> langue;

    /**
     * Construit l'atelier des touches de code.
     *
     * @param contexte le contexte des libellés des touches
     * @param editeur  la vue d'éditeur porteuse de l'image
     * @param session  la session d'édition touchée par les touches
     * @param langue   le support du langage ouvert, vif au document
     */
    AtelierTouchesCode(Context contexte, EditorView editeur,
            TextSession session, Supplier<LanguageSupport> langue) {
        this.contexte = contexte;
        this.editeur = editeur;
        this.session = session;
        this.langue = langue;
    }

    /** Les cinq touches usuelles de code (le dépôt de départ) : Tab,
     *  commentaire, déplacer la ligne, dupliquer — branchées aux
     *  moteurs purs de la maison. */
    List<TouchBarView.Touche> touches() {
        return List.of(
                new TouchBarView.Touche(contexte.getString(R.string.touche_tab),
                        null, this::indenterBloc),
                new TouchBarView.Touche(contexte.getString(R.string.touche_commenter),
                        null, this::commenterBloc),
                new TouchBarView.Touche(contexte.getString(R.string.touche_ligne_haut),
                        this::pasPremiereLigne, () -> deplacerBloc(-1)),
                new TouchBarView.Touche(contexte.getString(R.string.touche_ligne_bas),
                        this::pasDerniereLigne, () -> deplacerBloc(1)),
                new TouchBarView.Touche(contexte.getString(R.string.touche_dupliquer),
                        null, this::dupliquerBloc));
    }

    /** Vrai quand le caret vit AU-DELÀ de la première ligne — la
     *  touche « vers le haut » y reste vivante. */
    private boolean pasPremiereLigne() {
        return session.document().lineForOffset(
                session.selection().ranges().isEmpty() ? 0
                        : session.selection().ranges().get(0).caret()) > 0;
    }

    /** Vrai quand le caret vit EN-DEÇÀ de la dernière ligne — la
     *  touche « vers le bas » y reste vivante. */
    private boolean pasDerniereLigne() {
        return session.document().lineForOffset(
                session.selection().ranges().isEmpty() ? 0
                        : session.selection().ranges().get(0).caret())
                < session.document().lineCount() - 1;
    }

    /** Le plan du geste appliqué — la voie unique, l'image suit. */
    private boolean appliquer(EditPlan plan) {
        if (session.apply(plan.transaction(), plan.selection())) {
            editeur.invalidate();
            return true;
        }
        return false;
    }

    /** Tab : le bloc touché gagne un cran — les lignes de la
     *  sélection, le style du langage ouvert. Le plan se calcule à
     *  l'atelier d'indentation — la touche et le débordement de la
     *  barre de sélection y partagent le même moteur (jamais de
     *  doublon de logique). */
    private void indenterBloc() {
        appliquer(planIndentation(session.document(), session.selection(),
                langue));
    }

    /** Le plan d'indentation du bloc touché — chaque ligne de la
     *  sélection gagne un cran du style du langage ouvert, le
     *  canonique quatre espaces tient sans langage. La touche Tab et
     *  le débordement de la barre de sélection y lisent le même
     *  calcul (l'atelier le partage, le geste seul diffère). */
    static EditPlan planIndentation(Document doc,
            MultiSelection selection, Supplier<LanguageSupport> langue) {
        IndentStyle style = styleMaison(langue);
        String cran = style.indent(1);
        List<TextEdit> editions = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        int delta = 0;
        for (SelRange plage : selection.ranges()) {
            int premiere = doc.lineForOffset(plage.start());
            int derniere = doc.lineForOffset(Math.max(plage.start(),
                    plage.end() - 1));
            for (int ligne = premiere; ligne <= derniere; ligne++) {
                editions.add(new TextEdit(doc.lineStart(ligne) + delta,
                        "", cran));
                delta += cran.length();
            }
            apres.add(new SelRange(plage.start() + delta
                    - (derniere - premiere + 1) * cran.length(),
                    plage.end() + delta));
        }
        return new EditPlan(Transaction.of(editions),
                MultiSelection.of(apres));
    }

    /** Le style d'indentation du langage ouvert — la configuration y
     *  décide, le canonique quatre espaces tient sans langage. */
    private static IndentStyle styleMaison(
            Supplier<LanguageSupport> langue) {
        LanguageSupport langueOuverte = langue.get();
        if (langueOuverte == null) {
            return IndentStyle.spaces(4);
        }
        return langueOuverte.configuration().tabIndent()
                ? IndentStyle.tabs(langueOuverte.configuration().indentSize())
                : IndentStyle.spaces(langueOuverte.configuration().indentSize());
    }

    /** Commentaire : le bloc touché se commente ou se décommente —
     *  les règles du langage ouvert, le plan vide ne fait rien. */
    private void commenterBloc() {
        LanguageSupport langueOuverte = langue.get();
        CommentRules rules = langueOuverte == null
                ? CommentRules.of(LanguageConfiguration.EMPTY)
                : CommentRules.of(langueOuverte.configuration());
        Commenter.toggle(session.document(), session.selection(), rules)
                .ifPresent(this::appliquer);
    }

    /** Déplacement du bloc touché : le moteur pur, la direction au
     *  signe. */
    private void deplacerBloc(int direction) {
        appliquer(LineActions.moveLines(session.document(),
                session.selection(), direction));
    }

    /** Duplication du bloc touché : les curseurs descendent dans la
     *  copie. */
    private void dupliquerBloc() {
        appliquer(LineActions.duplicateLines(session.document(),
                session.selection()));
    }
}
