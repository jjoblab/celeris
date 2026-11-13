package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le sélecteur de couleur (le signal du propriétaire du 2026-11-13 :
 * « ajouter aussi le double clic dessus pour afficher le popups
 * couleur picker ») : la carte du dépôt de départ — ses blocs à elle
 * (en-tête, champ, bande, palette), le tap de cellule APPLIQUE sa
 * teinte, le drag du champ suit le doigt et applique au relâchement,
 * le littéral formaté tient la largeur du littéral d'origine.
 */
@RunWith(RobolectricTestRunner.class)
public class ColorPickerViewTest {

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

    /** La carte porte les blocs du dépôt de départ : 242 dp de large
     *  (huit cellules et leurs écarts, respirations comprises), la
     *  hauteur de ses quatre blocs. */
    @Test
    public void laCartePorteLesBlocsDuDepot() {
        ColorPickerView vue = new ColorPickerView(contexte, PopupPalette.dark());
        vue.ouvrir(0xFFFF5722, 6);
        mesurer(vue);
        assertEquals(242, vue.getMeasuredWidth());
        assertEquals(238 + 16, vue.getMeasuredHeight());
    }

    /** Le tap d'une cellule de palette APPLIQUE sa teinte —
     *  l'application implicite du dépôt de départ, jamais de bouton. */
    @Test
    public void leTapDeCelluleAppliqueSaTeinte() {
        ColorPickerView vue = new ColorPickerView(contexte, PopupPalette.dark());
        vue.ouvrir(0xFFFF5722, 6);
        List<Integer> appliquees = new ArrayList<>();
        vue.setOnAppliquer(appliquees::add);
        // la cellule 0 (rouge Material) vit à (11, 199) du contenu —
        // les respirations du panneau s'y ajoutent au toucher
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                12f + 11f, 8f + 199f, 0));
        vue.onTouchEvent(MotionEvent.obtain(0L, 16L, MotionEvent.ACTION_UP,
                12f + 11f, 8f + 199f, 0));
        assertEquals(1, appliquees.size());
        assertEquals(0xFFF44336, (int) appliquees.get(0));
    }

    /** Le drag du champ saturation-valeur suit le doigt, l'aperçu
     *  courant change, le relâchement APPLIQUE la teinte courante. */
    @Test
    public void leChampSuitLeDoigtEtAppliqueAuRelachement() {
        ColorPickerView vue = new ColorPickerView(contexte, PopupPalette.dark());
        vue.ouvrir(0xFFFF5722, 6);
        int originale = vue.courante();
        List<Integer> appliquees = new ArrayList<>();
        vue.setOnAppliquer(appliquees::add);
        // le champ vit à y ∈ [42, 154) du contenu : le doigt y glisse
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                12f + 100f, 8f + 100f, 0));
        int pendant = vue.courante();
        vue.onTouchEvent(MotionEvent.obtain(0L, 80L, MotionEvent.ACTION_UP,
                12f + 100f, 8f + 100f, 0));
        assertEquals(1, appliquees.size());
        assertEquals((int) appliquees.get(0), pendant);
        assertNotEquals("le pick change la teinte", originale, pendant);
        assertEquals("la teinte appliquée est opaque", 0xFF000000,
                appliquees.get(0) & 0xFF000000);
    }

    /** Le toucher hors des zones actives s'engloutit sans rien
     *  appliquer — la carte garde son geste, le pick ne part jamais. */
    @Test
    public void leToucherHorsZonesNappliqueRien() {
        ColorPickerView vue = new ColorPickerView(contexte, PopupPalette.dark());
        vue.ouvrir(0xFFFF5722, 6);
        List<Integer> appliquees = new ArrayList<>();
        vue.setOnAppliquer(appliquees::add);
        // dans l'en-tête, hors champ, hors bande, hors palette
        assertTrue(vue.onTouchEvent(MotionEvent.obtain(0L, 0L,
                MotionEvent.ACTION_DOWN, 12f + 100f, 8f + 10f, 0)));
        assertTrue(vue.onTouchEvent(MotionEvent.obtain(0L, 16L,
                MotionEvent.ACTION_UP, 12f + 100f, 8f + 10f, 0)));
        assertTrue(appliquees.isEmpty());
    }

    /** Le littéral formaté tient la largeur du littéral d'origine : la
     *  forme courte quantifie au nibble, la longue reste en minuscules. */
    @Test
    public void leFormatTientLaLargeurDuLitteral() {
        assertEquals("#e43", ColorPickerView.formater(0xFFF44336, 3));
        assertEquals("#822196f3", ColorPickerView.formater(0x822196F3, 8));
        assertEquals("#2196f3", ColorPickerView.formater(0xFF2196F3, 6));
        assertEquals("#8f00", ColorPickerView.formater(0x80FF0000, 4));
    }
}
