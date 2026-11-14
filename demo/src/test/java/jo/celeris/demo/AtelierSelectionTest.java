package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jo.celeris.android.widgets.SelectionBarView;

/**
 * La règle des actions vives de la barre de sélection (l'audit du
 * propriétaire du 2026-11-14 : « surtout coller ne doit être visible
 * que quand il est nécessaire ») : la décision pure COLLER ne se
 * propose que lorsque le presse-papiers est plein — la pilule ne
 * promène jamais une option inutile ; le débordement enrichi porte
 * commenter, casse, indentation et duplication, chaque action SON
 * pictogramme — jamais la même icône pour toutes.
 */
class AtelierSelectionTest {

    /** Le presse-papiers vide : Coller ne se propose JAMAIS —
     *  l'option inutile ne se promène pas sur la pilule. */
    @Test
    void collerNeSeProposePasSurPressePapiersVide() {
        assertFalse(AtelierSelection.collerSePropose(null, "Coller"),
                "pressee vide : Coller ne se propose jamais");
        assertFalse(AtelierSelection.collerSePropose("", "Coller"),
                "pressee blanche : Coller ne se propose jamais");
    }

    /** Le presse-papiers plein : Coller se propose — la copie vient de
     *  rendre l'action nécessaire. */
    @Test
    void collerSeProposeSurPressePapiersPlein() {
        assertTrue(AtelierSelection.collerSePropose("solde", "Coller"),
                "pressee plein : Coller se propose");
    }

    /** La décision reste lisible sans libellé — le banc ne monte pas
     *  la scène pour lire une règle pure. */
    @Test
    void laDecisionSeLitSansLibelle() {
        assertFalse(AtelierSelection.collerSePropose("solde", null),
                "sans libellé, rien ne se propose");
    }

    /** Les items vivants portent CHACUN leur pictogramme — jamais la
     *  même icône pour toutes (l'audit du propriétaire). */
    @Test
    void lesActionsPortentLeurPictogrammePropre() {
        AtomicInteger posee = new AtomicInteger();
        List<SelectionBarView.Item> items = List.of(
                new SelectionBarView.Item("Couper",
                        jo.celeris.android.widgets.IconeAction.COUPER,
                        posee::incrementAndGet),
                new SelectionBarView.Item("Copier",
                        jo.celeris.android.widgets.IconeAction.COPIER,
                        posee::incrementAndGet),
                new SelectionBarView.Item("Coller",
                        jo.celeris.android.widgets.IconeAction.COLLER,
                        posee::incrementAndGet),
                new SelectionBarView.Item("Tout",
                        jo.celeris.android.widgets.IconeAction.TOUT,
                        posee::incrementAndGet));
        assertEquals(4, items.size(), "quatre pastilles vivantes");
        assertEquals(jo.celeris.android.widgets.IconeAction.COUPER,
                items.get(0).picto(), "les ciseaux de la coupe");
        assertEquals(jo.celeris.android.widgets.IconeAction.COPIER,
                items.get(1).picto(), "les pages de la copie");
        assertEquals(jo.celeris.android.widgets.IconeAction.COLLER,
                items.get(2).picto(), "le presse-papiers du collage");
        assertEquals(jo.celeris.android.widgets.IconeAction.TOUT,
                items.get(3).picto(), "les coins du tout-sélectionner");
    }
}
