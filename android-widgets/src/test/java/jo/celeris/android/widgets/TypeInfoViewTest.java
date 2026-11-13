package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La carte d'info de type (catégorie A de l'inventaire — le fil rouge
 * du guide du développeur, la seconde surface réveillée du signal du
 * propriétaire) : le nom du type du symbole au caret, UNE rangée au
 * palier titre — elle décrit, elle ne choisit pas (aucune navigation
 * clavier : la carte passive ne réalise pas {@code PopupClavier}, la
 * frappe la referme avant qu'une flèche ne la serve).
 */
@RunWith(RobolectricTestRunner.class)
public class TypeInfoViewTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    private void mesurer(View vue) {
        vue.measure(View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED));
    }

    /** Le type se pose en UNE rangée titre : la carte se mesure à son
     *  contenu, respirations comprises — le contenu suit la pose. */
    @Test
    public void leTypeSePoseEnRangéeDeTitre() {
        TypeInfoView vue = new TypeInfoView(contexte, PopupPalette.dark());
        vue.setType("int");
        mesurer(vue);
        assertTrue("la carte tient sa rangée", vue.getMeasuredHeight() > 0);
        assertEquals("int", vue.type());
        // la pose suivante remplace le contenu : jamais deux rangées
        vue.setType("java.util.List<String>");
        mesurer(vue);
        assertEquals("java.util.List<String>", vue.type());
        assertTrue("la carte garde une seule rangée",
                vue.rowCount() == 1);
    }

    /** La carte passive ne réalise pas le contrat clavier : la flèche
     *  déléguée ne la trouve pas — la carte décrit, le routage la
     *  saute, la touche retourne à l'éditeur. */
    @Test
    public void laCartePassiveNeRealisePasLeClavier() {
        TypeInfoView vue = new TypeInfoView(contexte, PopupPalette.dark());
        vue.setType("int");
        assertFalse("la carte passive ne règne pas sur le clavier",
                PopupClavier.class.isInstance(vue));
    }
}
