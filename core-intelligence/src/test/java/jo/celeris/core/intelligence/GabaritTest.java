package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.TextEdit;

/**
 * Les gabarits dynamiques (A-22, M5) : des trous tabulables — les
 * occurrences, dans l'ordre du rendu — et des variables liées — la même
 * variable éditée partout à la fois. Le rendu porte les valeurs par
 * défaut, l'insertion sélectionne le premier trou, les miroirs
 * propagent l'édition aux occurrences sœurs.
 */
class GabaritTest {

    /** Trois trous dont deux liés : les occurrences sortent dans l'ordre du rendu. */
    @Test
    void troisTrousDontDeuxLies() {
        Gabarit gabarit = Gabarit.of("val ${nom} = ${valeur}; println(${nom})");
        assertEquals(3, gabarit.trous().size());
        // l'ordre de tabulation : l'ordre des occurrences dans le rendu
        assertEquals("nom", gabarit.trous().get(0).variable());
        assertEquals("valeur", gabarit.trous().get(1).variable());
        assertEquals("nom", gabarit.trous().get(2).variable());
        // les variables distinctes, dans l'ordre de première occurrence
        assertEquals(List.of("nom", "valeur"), gabarit.variables());
        assertEquals(2, gabarit.occurrencesDe("nom").size());
        assertEquals(1, gabarit.occurrencesDe("valeur").size());
    }

    /** Le rendu porte les valeurs par défaut après le deux-points. */
    @Test
    void leRenduPorteLesDefauts() {
        Gabarit gabarit = Gabarit.of("${salut:Bonjour} ${nom:monde}");
        assertEquals("Bonjour monde", gabarit.rendu());
        Trou salut = gabarit.occurrencesDe("salut").get(0);
        assertEquals(0, salut.debut());
        assertEquals(7, salut.fin());
        Trou nom = gabarit.occurrencesDe("nom").get(0);
        assertEquals(8, nom.debut());
        assertEquals(13, nom.fin());
    }

    /** Sans défaut, le trou est vide : le curseur s'y pose. */
    @Test
    void sansDefautLeTrouEstVide() {
        Gabarit gabarit = Gabarit.of("val ${nom};");
        assertEquals("val ;", gabarit.rendu());
        Trou nom = gabarit.occurrencesDe("nom").get(0);
        assertEquals(4, nom.debut());
        assertEquals(4, nom.fin());
    }

    /** L'insertion : le rendu s'insère, le premier trou est sélectionné. */
    @Test
    void insertionSelectionneLePremierTrou() {
        Gabarit gabarit = Gabarit.of("${salut:Bonjour} ${nom:monde} !");
        EditPlan plan = Gabarits.inserer(Document.of("x"),
                MultiSelection.caretsAt(1), gabarit);
        assertEquals("Bonjour monde !", plan.transaction().edits().get(0).inserted());
        assertEquals(1, plan.transaction().edits().get(0).start());
        SelRange premier = plan.selection().ranges().get(0);
        assertEquals(1, premier.start());
        assertEquals(8, premier.end());
    }

    /** L'édition simultanée : l'édition d'une occurrence se propage aux sœurs. */
    @Test
    void editionSimultaneeAuxSoeurs() {
        Gabarit gabarit = Gabarit.of("${i:i} puis ${n:n} puis ${i}");
        // rendu : « i puis n puis  » — occurrences de i : [0,1) et [14,14)
        String rendu = gabarit.rendu();
        assertEquals("i puis n puis ", rendu);
        List<Trou> occurrences = gabarit.occurrencesDe("i");
        Trou premiere = occurrences.get(0);
        Trou derniere = occurrences.get(1);
        // l'utilisateur remplace la première occurrence par « 10 »
        List<TextEdit> miroirs = Gabarits.miroirs(rendu, occurrences, premiere,
                new TextEdit(premiere.debut(), "i", "10"));
        assertEquals(1, miroirs.size());
        assertEquals(derniere.debut(), miroirs.get(0).start());
        assertTrue(miroirs.get(0).removed().isEmpty());
        assertEquals("10", miroirs.get(0).inserted());
    }

    /** Une édition partielle d'une occurrence se propage au même décalage relatif. */
    @Test
    void editionPartielleAuMemeDecalage() {
        Gabarit gabarit = Gabarit.of("${x:alpha} ${x:omega}");
        String rendu = gabarit.rendu();
        List<Trou> occurrences = gabarit.occurrencesDe("x");
        // remplacer « pha » (offsets 2..5 de la première occurrence) par « PHA »
        List<TextEdit> miroirs = Gabarits.miroirs(rendu, occurrences,
                occurrences.get(0), new TextEdit(2, "pha", "PHA"));
        assertEquals(1, miroirs.size());
        // la sœur « omega » : le même décalage relatif [2..5) = « ega »
        assertEquals(8, miroirs.get(0).start());
        assertEquals("ega", miroirs.get(0).removed());
        assertEquals("PHA", miroirs.get(0).inserted());
    }

    /** Les trous sans variable liée n'ont pas de miroir. */
    @Test
    void trouSolitaireSansMiroir() {
        Gabarit gabarit = Gabarit.of("${seul:unique}");
        List<Trou> occurrences = gabarit.occurrencesDe("seul");
        List<TextEdit> miroirs = Gabarits.miroirs(gabarit.rendu(), occurrences,
                occurrences.get(0), new TextEdit(0, "unique", "autre"));
        assertTrue(miroirs.isEmpty());
    }
}
