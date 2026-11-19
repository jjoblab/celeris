package jo.celeris.android.view;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La garde « ligne monstre » (B-04, l'analyse croisée) : une ligne
 * minifiée de centaines de kilo-octets ne peut pas façonner sa rangée
 * entière à chaque miss — subText géant, runs bidirectionnels sur tout
 * le texte, allocation monstrueuse. Le dépôt de départ coupe tout à
 * 5000 caractères (rendu fenêtré de la tranche visible). Celeris
 * façonnait la ligne ENTIÈRE.
 */
@RunWith(RobolectricTestRunner.class)
public class LineGuardTest {

    /** Une ligne monstre de 30 000 caractères, en document. */
    private EditorTestRig bancMonstre() {
        StringBuilder monstre = new StringBuilder();
        for (int i = 0; i < 30_000; i++) {
            monstre.append('x');
        }
        EditorTestRig rig = EditorTestRig.on(
                RuntimeEnvironment.getApplication(), monstre + "\n");
        rig.vue().layout(0, 0, 1080, 1920);
        rig.image();
        return rig;
    }

    private RowLayout rangee(EditorView vue, int number) {
        return vue.cache.find(vue.session.document().revision(), number,
                vue.theme.id(), vue.metrics.font(),
                vue.projection.columns());
    }

    /** La mise en page d'une ligne monstre est FENÊTRÉE : le texte
     *  de la rangée ne dépasse jamais le plafond — jamais de subText
     *  géant, jamais de runs sur 30 000 caractères. */
    @Test
    public void uneLigneMonstreSeFenetreAuPlafond() {
        EditorTestRig rig = bancMonstre();
        EditorView vue = rig.vue();
        RowLayout premiere = rangee(vue, 0);
        assertNotNull("la rangée est mesurée", premiere);
        assertTrue("le texte de la rangée est fenêtré (≤ 5000), il fait "
                + premiere.text().length(),
                premiere.text().length() <= 5000);
        assertTrue("la fin de tranche suit le plafond",
                premiere.end() <= 5000);
    }
}
