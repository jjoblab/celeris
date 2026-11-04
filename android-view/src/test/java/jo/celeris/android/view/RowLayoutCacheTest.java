package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Tests du cache de mises en page par rangée (ADR-006 : les caches sont
 * indexés par valeurs immuables).
 *
 * <p>PIEGE-02 : dans le dépôt de départ, la clé du cache était signée par
 * un état mutable d'un objet partagé (la couleur du pinceau) : deux
 * contenus distincts partageaient alors une entrée de cache. La clé ne
 * signe que des valeurs immuables : révision de l'instantané, numéro de
 * rangée, identifiant de thème et spécification de police.</p>
 */
class RowLayoutCacheTest {

    private static final FontSpec MONO = new FontSpec("mono", 14f);
    private static final int THEME = 1;

    /** Pinceau mutable minimal : la couleur courante, rien d'autre. */
    private static final class PinceauMutable implements TextBrush {
        private int couleur;

        @Override
        public void setColor(int argb) {
            couleur = argb;
        }

        @Override
        public int color() {
            return couleur;
        }
    }

    /** Rend une mise en page obligatoirement présente (échec net sinon). */
    private static RowLayout present(@Nullable RowLayout layout) {
        if (layout == null) {
            throw new AssertionError("mise en page absente du cache");
        }
        return layout;
    }

    /**
     * PIEGE-02 — la clé du cache ne signe que des valeurs immuables.
     *
     * <p>Deux révisions distinctes, tout le reste identique, donnent deux
     * entrées distinctes ; l'état mutable d'un objet partagé (le pinceau
     * du pipeline) changeant entre deux consultations ne change rien ; une
     * spécification égale par la valeur touche l'entrée de son égale.</p>
     */
    @Test
    void layoutCache_keyIncludesImmutableValueOnly() {
        RowLayoutCache cache = new RowLayoutCache();

        // Deux révisions, tout le reste identique : deux contenus, deux
        // entrées — jamais une entrée partagée.
        cache.store(new RowKey(1L, 0, THEME, MONO),
                new RowLayout(0, 0, 3, "abc", 25.2f));
        cache.store(new RowKey(2L, 0, THEME, MONO),
                new RowLayout(0, 0, 3, "xyz", 25.2f));
        RowLayout premiere = cache.find(1L, 0, THEME, MONO);
        RowLayout seconde = cache.find(2L, 0, THEME, MONO);
        assertNotNull(premiere);
        assertNotNull(seconde);
        assertEquals("abc", premiere.text());
        assertEquals("xyz", seconde.text());
        assertSame(premiere, cache.find(1L, 0, THEME, MONO));

        // Un état mutable partagé change entre deux consultations : les
        // résultats ne bougent pas — la clé ne voit jamais cet état.
        TextBrush pinceau = new PinceauMutable();
        pinceau.setColor(0xFFFF0000);
        assertEquals("abc", present(cache.find(1L, 0, THEME, MONO)).text());
        pinceau.setColor(0xFF00FF00);
        assertEquals("abc", present(cache.find(1L, 0, THEME, MONO)).text());

        // Clé par valeur : une spécification égale (autre instance) touche
        // la même entrée que la sienne.
        FontSpec memeSpec = new FontSpec("mono", 14f);
        assertEquals(MONO, memeSpec);
        assertSame(premiere, cache.find(1L, 0, THEME, memeSpec));
    }

    /** Une rangée absente rend null ; après dépôt, elle est trouvée. */
    @Test
    void absentePuisPresente() {
        RowLayoutCache cache = new RowLayoutCache();
        assertNull(cache.find(9L, 4, THEME, MONO));
        cache.store(new RowKey(9L, 4, THEME, MONO),
                new RowLayout(4, 0, 2, "ab", 16.8f));
        assertNotNull(cache.find(9L, 4, THEME, MONO));
        assertEquals(1, cache.size());
    }

    /** L'invalidation par révision retire exactement cette révision. */
    @Test
    void invalidationParRevision() {
        RowLayoutCache cache = new RowLayoutCache();
        cache.store(new RowKey(1L, 0, THEME, MONO), new RowLayout(0, 0, 1, "a", 8.4f));
        cache.store(new RowKey(1L, 1, THEME, MONO), new RowLayout(1, 0, 1, "b", 8.4f));
        cache.store(new RowKey(2L, 0, THEME, MONO), new RowLayout(0, 0, 1, "c", 8.4f));

        cache.invalidate(1L);

        assertNull(cache.find(1L, 0, THEME, MONO));
        assertNull(cache.find(1L, 1, THEME, MONO));
        assertNotNull(cache.find(2L, 0, THEME, MONO));
        assertEquals(1, cache.size());
    }

    /** Le redépôt d'une rangée existante remplace l'entrée, sans doublon. */
    @Test
    void redepotRemplace() {
        RowLayoutCache cache = new RowLayoutCache();
        RowKey cle = new RowKey(3L, 0, THEME, MONO);
        cache.store(cle, new RowLayout(0, 0, 1, "a", 8.4f));
        cache.store(cle, new RowLayout(0, 0, 2, "bc", 16.8f));

        assertEquals(1, cache.size());
        assertEquals("bc", present(cache.find(3L, 0, THEME, MONO)).text());
    }

    /**
     * PIEGE-02 — la clé signe aussi la configuration de retour à la
     * ligne : même révision, même rangée, même thème et même police, un
     * autre budget de colonnes est un autre contenu — jamais une entrée
     * partagée.
     */
    @Test
    void layoutCache_keySignsWrapConfiguration() {
        RowLayoutCache cache = new RowLayoutCache();

        // Même rangée sous deux budgets : deux tranches distinctes.
        cache.store(new RowKey(1L, 0, THEME, MONO, 4),
                new RowLayout(0, 0, 4, "aaaa", 40f));
        cache.store(new RowKey(1L, 0, THEME, MONO, 0),
                new RowLayout(0, 0, 10, "aaaaaaaaaa", 100f));

        assertEquals("aaaa", present(cache.find(1L, 0, THEME, MONO, 4)).text());
        assertEquals("aaaaaaaaaa", present(cache.find(1L, 0, THEME, MONO, 0))
                .text());
        assertEquals(2, cache.size());
    }
}
