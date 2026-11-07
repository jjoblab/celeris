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
        assertEquals(ClasseLargeur.COMPACT, ClasseLargeur.de(320f));
        assertEquals(ClasseLargeur.COMPACT, ClasseLargeur.de(599.9f));
        assertEquals(ClasseLargeur.MOYEN, ClasseLargeur.de(600f));
        assertEquals(ClasseLargeur.MOYEN, ClasseLargeur.de(839.9f));
        assertEquals(ClasseLargeur.ETENDU, ClasseLargeur.de(840f));
        assertEquals(ClasseLargeur.ETENDU, ClasseLargeur.de(1280f));
    }

    @Test
    void laGouttiereSAdapteALaClasse() {
        // Le compact resserre : la gouttière d'un numéro gagne des
        // pixels précieux sur un téléphone replié.
        float compacte = GutterGeometry.width(9, 10f,
                ClasseLargeur.COMPACT.margeGouttiere());
        float moyenne = GutterGeometry.width(9, 10f,
                ClasseLargeur.MOYEN.margeGouttiere());
        float etendue = GutterGeometry.width(9, 10f,
                ClasseLargeur.ETENDU.margeGouttiere());
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
        assertEquals(ClasseLargeur.COMPACT, ClasseLargeur.de(
                Math.min(360f, 720f)));
        assertEquals(ClasseLargeur.ETENDU, ClasseLargeur.de(
                Math.min(840f, 2210f)));
    }
}
