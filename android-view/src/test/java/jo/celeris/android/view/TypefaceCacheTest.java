package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Tests du cache de polices de la vue.
 *
 * <p>PIEGE-14 : dans le dépôt de départ, des variantes de police en gras
 * étaient créées à chaque segment et à chaque image. Le couple (base,
 * gras) doit être mémoïsé et invalidé quand la police change.</p>
 */
class TypefaceCacheTest {

    /** Poignée de police minimale : un marqueur d'instance, rien d'autre. */
    private static final class PoigneeSimple implements FontHandle {
    }

    /** Fabrique comptée : chaque dérivation crée une poignée neuve. */
    private static final class FabriqueComptee implements TypefaceFactory {
        private int derivations;

        @Override
        public FontHandle deriveBold(FontHandle base) {
            derivations++;
            return new PoigneeSimple();
        }
    }

    /**
     * PIEGE-14 — le gras est mémoïsé tant que la police ne change pas.
     *
     * <p>Une seule dérivation pour des consultations répétées (chaque
     * segment, chaque image) ; la mémoïsation est par valeur de
     * spécification ; le changement de police invalide tout et la
     * dérivation repart.</p>
     */
    @Test
    void boldTypefaceMemoized_untilFontChange() {
        FabriqueComptee fabrique = new FabriqueComptee();
        TypefaceCache cache = new TypefaceCache(fabrique);
        FontHandle mono = new PoigneeSimple();
        FontSpec quatorze = new FontSpec("mono", 14f);

        // Le couple (base, gras) est mémoïsé : une seule dérivation, la
        // même instance rendue à chaque consultation.
        FontHandle gras1 = cache.boldFor(quatorze, mono);
        assertSame(gras1, cache.boldFor(quatorze, mono));
        assertSame(gras1, cache.boldFor(quatorze, mono));
        assertEquals(1, fabrique.derivations);

        // Mémoïsation par valeur : une spécification égale (autre
        // instance) touche la même entrée.
        assertSame(gras1, cache.boldFor(new FontSpec("mono", 14f), mono));
        assertEquals(1, fabrique.derivations);

        // Une autre taille est une autre police : sa propre dérivation.
        FontHandle gras16 = cache.boldFor(new FontSpec("mono", 16f), mono);
        assertNotSame(gras1, gras16);
        assertEquals(2, fabrique.derivations);

        // Le changement de police invalide la mémoïsation : la dérivation
        // repart — jamais une variante périmée.
        cache.fontChanged();
        FontHandle grasApres = cache.boldFor(quatorze, mono);
        assertNotSame(gras1, grasApres);
        assertEquals(3, fabrique.derivations);
        assertSame(grasApres, cache.boldFor(quatorze, mono));
    }
}
