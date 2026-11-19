package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * Tab et Shift+Tab (C1, l'analyse croisée) : le dépôt de départ
 * indente et désindente les LIGNES de la sélection — la sélection
 * multi-lignes survit au geste, le caret suit la dernière ligne.
 * Celeris n'avait rien (LineActions : move/duplicate/join/delete,
 * jamais indent).
 */
class TabIndentTest {

    private String apres(String texte, SelRange plage, boolean tab) {
        Document doc = Document.of(texte);
        EditPlan plan = tab
                ? LineActions.tab(doc, MultiSelection.of(plage),
                        IndentStyle.spaces(4))
                : LineActions.untab(doc, MultiSelection.of(plage),
                        IndentStyle.spaces(4));
        TextSession session = TextSession.of(texte, () -> 0L);
        session.apply(plan.transaction(), plan.selection());
        return session.text();
    }

    /** Tab sur une sélection multi-lignes : chaque ligne gagne un
     *  cran, la sélection SURVIT (mêmes lignes, décalées). */
    @Test
    void tabIndenteLesLignesDeLaSelection() {
        String texte = "alpha\nbeta\ngamma\n";
        // sélection des lignes 1 et 2 (beta, gamma) :
        // beta commence à 6, fin de gamma à 17 (avec son saut)
        assertEquals("alpha\n    beta\n    gamma\n",
                apres(texte, new SelRange(6, 17), true));
    }

    /** Shift+Tab retire UN cran aux lignes sélectionnées — jamais
     *  plus que leur indentation. */
    @Test
    void shiftTabRetireUnCran() {
        String texte = "alpha\n        beta\n        gamma\n";
        assertEquals("alpha\n    beta\n    gamma\n",
                apres(texte, new SelRange(10, 29), false));
    }

    /** Shift+Tab sur une ligne sans indentation : rien ne bouge. */
    @Test
    void shiftTabSansIndentationNeBougeRien() {
        String texte = "alpha\nbeta\n";
        assertEquals("alpha\nbeta\n",
                apres(texte, new SelRange(6, 11), false));
    }

    /** Tab au caret (sélection vide) : le caret gagne un cran
     *  d'indentation, pas la ligne entière. */
    @Test
    void tabAuCaretPoseUnCran() {
        String texte = "alpha\n";
        Document doc = Document.of(texte);
        EditPlan plan = LineActions.tab(doc,
                MultiSelection.caretsAt(3), IndentStyle.spaces(4));
        TextSession session = TextSession.of(texte, () -> 0L);
        session.apply(plan.transaction(), plan.selection());
        assertEquals("alp    ha\n", session.text());
        assertEquals(7, session.selection().carets().get(0));
    }
}
