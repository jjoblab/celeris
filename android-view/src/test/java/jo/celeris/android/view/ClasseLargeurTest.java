package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Les classes de largeur de fenêtre (D-19) : les seuils des classes
 * de taille Material décident, la gouttière s'adapte — le compact
 * resserre le champ de numéros, l'étendu le respire, le pliable
 * déplié change de classe d'un geste.
 */
class ClasseLargeurTest {

    @Test
    void lesSeuilsDecidentDeLaClasse() {
        assertEquals(WidthClass.COMPACT, WidthClass.of(320f));
        assertEquals(WidthClass.COMPACT, WidthClass.of(599.9f));
        assertEquals(WidthClass.MOYEN, WidthClass.of(600f));
        assertEquals(WidthClass.MOYEN, WidthClass.of(839.9f));
        assertEquals(WidthClass.ETENDU, WidthClass.of(840f));
        assertEquals(WidthClass.ETENDU, WidthClass.of(1280f));
    }

    @Test
    void laGouttiereSAdapteALaClasse() {
        // Le compact resserre : la gouttière d'un numéro gagne des
        // pixels précieux sur un téléphone replié.
        float compacte = GutterGeometry.width(9, 10f,
                WidthClass.COMPACT.gutterMarginDp());
        float moyenne = GutterGeometry.width(9, 10f,
                WidthClass.MOYEN.gutterMarginDp());
        float etendue = GutterGeometry.width(9, 10f,
                WidthClass.ETENDU.gutterMarginDp());
        assertEquals(18f, compacte, 1e-6f, "compact : 10 + 2 x 4");
        assertEquals(26f, moyenne, 1e-6f, "moyen : 10 + 2 x 8");
        assertEquals(34f, etendue, 1e-6f, "étendu : 10 + 2 x 12");
        assertEquals(true, compacte < moyenne && moyenne < etendue,
                "la gouttière s'élargit avec la fenêtre");
    }

    @Test
    void lePliableDeplieChangeDeClasseDUnGeste() {
        // Un pliable type : 360 dp replié, 840 dp déplié (la plus
        // petite dimension décide).
        assertEquals(WidthClass.COMPACT, WidthClass.of(
                Math.min(360f, 720f)));
        assertEquals(WidthClass.ETENDU, WidthClass.of(
                Math.min(840f, 2210f)));
    }
}
