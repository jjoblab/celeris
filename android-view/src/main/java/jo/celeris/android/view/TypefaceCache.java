package jo.celeris.android.view;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache de polices de la vue : le couple (base, gras) mémoïsé.
 *
 * <p>PIEGE-14 : le dépôt de départ créait des variantes en gras à chaque
 * segment et à chaque image. Ici, la variante grasse est dérivée une
 * fois par spécification, rendue telle quelle ensuite — et la
 * mémoïsation entière est invalidée au changement de police.</p>
 */
public final class TypefaceCache {

    private final TypefaceFactory fabrique;
    private final Map<FontSpec, FontHandle> grasMemoise = new HashMap<>();

    /**
     * Construit le cache sur la fabrique donnée.
     *
     * @param factory la fabrique de variantes côté hôte
     */
    public TypefaceCache(TypefaceFactory factory) {
        this.fabrique = factory;
    }

    /**
     * La variante grasse de la base pour la spécification donnée :
     * mémoïsée par valeur, dérivée à la première demande seulement.
     *
     * @param spec la spécification de police
     * @param base la police de base à dériver
     * @return la variante grasse, instance unique tant que la police ne
     *         change pas
     */
    public FontHandle boldFor(FontSpec spec, FontHandle base) {
        FontHandle gras = grasMemoise.get(spec);
        if (gras == null) {
            gras = fabrique.deriveBold(base);
            grasMemoise.put(spec, gras);
        }
        return gras;
    }

    /** Invalide toute la mémoïsation : la police a changé. */
    public void fontChanged() {
        grasMemoise.clear();
    }
}
