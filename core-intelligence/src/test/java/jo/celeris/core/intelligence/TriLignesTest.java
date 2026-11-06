package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * Le tri de lignes (A-12, M5) : naturel — les tranches numériques se
 * comparent en nombres (« item2 » avant « item10 »), les lettres en
 * ordre lexicographique — et sensible à la casse au choix de l'hôte.
 * Le bloc trié remplace le bloc d'origine, sélectionné après le geste.
 */
class TriLignesTest {

    /** Le tri naturel : « item2 » avant « item10 » (A-12). */
    @Test
    void triNaturelItem2AvantItem10() {
        Document doc = Document.of("item10\nitem2");
        EditPlan plan = LineSorter.sort(doc,
                MultiSelection.of(new SelRange(0, 13)), true);
        assertEquals("item10\nitem2", plan.transaction().edits().get(0).removed());
        assertEquals("item2\nitem10", plan.transaction().edits().get(0).inserted());
    }

    /** Les tranches numériques se comparent en nombres, au milieu du texte. */
    @Test
    void tranchesNumeriquesAuMilieu() {
        Document doc = Document.of("f10(0)\nf2(0)");
        EditPlan plan = LineSorter.sort(doc,
                MultiSelection.of(new SelRange(0, 11)), true);
        assertEquals("f2(0)\nf10(0)", plan.transaction().edits().get(0).inserted());
    }

    /** Sensible à la casse : les majuscules passent avant les minuscules. */
    @Test
    void sensibiliteALaCasse() {
        Document doc = Document.of("Banane\napple");
        EditPlan sensible = LineSorter.sort(doc,
                MultiSelection.of(new SelRange(0, 12)), true);
        assertEquals("Banane\napple", sensible.transaction().edits().get(0).inserted());

        EditPlan insensible = LineSorter.sort(doc,
                MultiSelection.of(new SelRange(0, 12)), false);
        assertEquals("apple\nBanane", insensible.transaction().edits().get(0).inserted());
    }

    /** Le tri porte sur le bloc touché, pas sur le reste du document. */
    @Test
    void leTriPorteSurLeBlocSeul() {
        Document doc = Document.of("header\nb\na\nfooter");
        // sélection couvrant les lignes 1 et 2
        EditPlan plan = LineSorter.sort(doc,
                MultiSelection.of(new SelRange(7, 11)), true);
        assertEquals("b\na", plan.transaction().edits().get(0).removed());
        assertEquals("a\nb", plan.transaction().edits().get(0).inserted());
        assertEquals(7, plan.transaction().edits().get(0).start());
    }

    /** Le bloc trié reste sélectionné après le geste. */
    @Test
    void leBlocTrieResteSelectionne() {
        Document doc = Document.of("item10\nitem2");
        EditPlan plan = LineSorter.sort(doc,
                MultiSelection.of(new SelRange(0, 13)), true);
        SelRange apres = plan.selection().ranges().get(0);
        assertEquals(0, apres.start());
        assertEquals(12, apres.end());
    }
}
