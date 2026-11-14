package jo.celeris.android.host.popups;

import java.util.List;
import java.util.function.Supplier;

import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.ReglagesEditeur;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * L'ampoule d'intentions de la gouttière (l'audit Android Studio du
 * 2026-11-14 : le propriétaire — « le petit icône ampoule jaune comme
 * dans Android Studio, je ne l'ai pas vue dans notre éditeur ») : le
 * petit icône jaune de l'IDE de référence vit dans la coulisse, à la
 * ligne du CARET qui porte un diagnostic — son tap ouvre le menu
 * d'intentions.
 *
 * <p>La décision reste à l'hôte, jamais devinée : le caret s'y rend à
 * chaque sélection, les diagnostics s'y lisent VIFS (le fournisseur
 * sert la liste courante — le lint du build, le serveur). La vue ne
 * connaît ni les intentions, ni les diagnostics : elle ne fait que
 * tracer l'ampoule aux lignes posées et rendre la ligne cliquée.</p>
 */
final class AmpouleIntentions {

    /** La vue dont la gouttière porte l'ampoule. */
    private final EditorView editeur;

    /** La carte du menu d'intentions — son tap y ouvre. */
    private final CarteIntentions carte;

    /** Les diagnostics courants, vifs — lint du build ou serveur. */
    private final Supplier<List<DiagnosticHote>> diagnostics;

    /** La ligne du caret, à partir de un — 0 : aucun caret lu. */
    private int ligneCaret;

    /**
     * Construit l'ampoule : le tap dans la coulisse rend la ligne à
     * l'hôte, qui y ouvre son menu d'intentions.
     *
     * @param editeur     la vue dont la gouttière porte l'ampoule
     * @param carte        la carte du menu d'intentions
     * @param diagnostics  le fournisseur des diagnostics courants
     */
    AmpouleIntentions(EditorView editeur, CarteIntentions carte,
            Supplier<List<DiagnosticHote>> diagnostics) {
        this.editeur = editeur;
        this.carte = carte;
        this.diagnostics = diagnostics;
        editeur.marquesGouttiere().setSurAmpoule(ligne -> carte.ouvrir());
    }

    /**
     * La sélection s'y rend : la ligne du caret s'y lit — l'ampoule
     * suit le comportement de l'IDE de référence (la ligne courante
     * seule peut porter le petit icône).
     *
     * @param session   la session d'édition courante
     * @param selection la sélection courante
     */
    void surSelection(TextSession session, MultiSelection selection) {
        // la PORTE des réglages peut éteindre l'ampoule — la table
        // centrale reste le point de vérité (l'audit du 2026-11-14)
        if (!editeur.reglages().ouverte(
                ReglagesEditeur.Porte.AMPOULE_INTENTIONS)) {
            return;
        }
        List<SelRange> plages = selection.ranges();
        int caret = plages.isEmpty() ? 0 : plages.get(0).caret();
        surCaret(session.document().lineForOffset(caret) + 1);
    }

    /**
     * La ligne du caret s'y rend — l'ampoule suit le comportement de
     * l'IDE de référence : elle ne vit qu'à la ligne du caret qui
     * porte un diagnostic.
     *
     * @param ligne la ligne du caret, à partir de un (0 : aucun)
     */
    void surCaret(int ligne) {
        ligneCaret = ligne;
        rafraichir();
    }

    /** Les diagnostics ont tourné : la ligne du caret se relit —
     *  l'ampoule se pose ou se retire, l'image suit. */
    void rafraichir() {
        editeur.marquesGouttiere().setAmpoules(lignesPosnees());
        editeur.invalidate();
    }

    /**
     * Les lignes porteuses de l'ampoule — la ligne du caret (convention
     * de la vue : à partir de zéro) quand elle porte un diagnostic,
     * vide sinon. Pure et publique : les bancs y vérifient la décision
     * sans appareil.
     */
    int[] lignesPosnees() {
        if (ligneAmpoule(ligneCaret, diagnostics.get()) < 0) {
            return new int[0];
        }
        return new int[] {ligneCaret - 1};
    }

    /**
     * La ligne de l'ampoule pour un caret et des diagnostics donnés —
     * le caret lui-même quand il porte un diagnostic, —1 sinon (la
     * décision de l'IDE de référence, pure et testable).
     *
     * @param ligneCaret la ligne du caret, à partir de un (0 : aucun)
     * @param diagnostics les diagnostics courants de l'hôte
     * @return la ligne 1-based porteuse, —1 si l'ampoule ne se pose pas
     */
    static int ligneAmpoule(int ligneCaret,
            List<DiagnosticHote> diagnostics) {
        if (ligneCaret < 1) {
            return -1;
        }
        for (DiagnosticHote diagnostic : diagnostics) {
            if (diagnostic.ligne() == ligneCaret) {
                return ligneCaret;
            }
        }
        return -1;
    }
}
