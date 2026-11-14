package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La carte À ÉTAGES (l'audit du propriétaire du 2026-11-14 : « je veux
 * le séparer en étages ou colonnes qui deviennent visibles
 * contextuellement ») : l'étage VIDE ne se montre jamais, les en-têtes
 * ne se prennent pas, la flèche déléguée SAUTE les en-têtes — la
 * visibilité contextuelle naît de la donnée.
 */
@RunWith(RobolectricTestRunner.class)
public class MenuEtagesTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** L'étage vide ne se montre jamais — même pas son en-tête. */
    @Test
    public void letageVideNeSeMontreJamais() {
        MenuEtages carte = new MenuEtages(contexte, PopupPalette.dark());
        carte.setEtages(List.of(
                MenuEtages.Etage.depuis("GO TO", List.of()),
                MenuEtages.Etage.depuis("EDIT", List.of(
                        new SelectionBarView.Item("Dupliquer", () -> { })))));
        assertTrue("la carte porte l'étage plein", carte.aActions());
        assertEquals("la première rangée est l'en-tête d'EDIT",
                null, carte.itemAt(0));
        assertEquals("la seconde rangée est l'action",
                "Dupliquer", carte.itemAt(1).label());
    }

    /** Tout vide : la carte ne porte rien, l'hôte n'y pose rien. */
    @Test
    public void toutVideNePorteRien() {
        MenuEtages carte = new MenuEtages(contexte, PopupPalette.dark());
        carte.setEtages(List.of(
                MenuEtages.Etage.depuis("GO TO", List.of()),
                MenuEtages.Etage.depuis("EDIT", List.of())));
        assertFalse("sans étage vivant, la carte ne porte rien",
                carte.aActions());
    }

    /** La flèche déléguée SAUTE les en-têtes — jamais d'arrêt sur une
     *  section, la validation prend l'action surlignée. */
    @Test
    public void laFlecheSauteLesEnTetes() {
        MenuEtages carte = new MenuEtages(contexte, PopupPalette.dark());
        AtomicInteger prises = new AtomicInteger();
        carte.setEtages(List.of(
                new MenuEtages.Etage("GO TO", List.of(
                        new MenuEtages.Item("Usages",
                                IconeAction.USAGES, () -> { }))),
                new MenuEtages.Etage("EDIT", List.of(
                        new MenuEtages.Item("Dupliquer",
                                IconeAction.DUPLIQUER,
                                prises::incrementAndGet)))));
        assertTrue("la première flèche prend la première action",
                carte.surFleche(1));
        assertEquals(1, carte.surbrillance());
        // la flèche SAUTE l'en-tête d'EDIT (rangée 2) : la surbrillance
        // tombe sur l'action (rangée 3)
        assertTrue("la flèche suivante saute l'en-tête",
                carte.surFleche(1));
        assertEquals(3, carte.surbrillance());
        assertTrue("la validation prend l'action surlignée",
                carte.surValidation());
        assertEquals("l'action surlignée a vécu", 1, prises.get());
    }
}
