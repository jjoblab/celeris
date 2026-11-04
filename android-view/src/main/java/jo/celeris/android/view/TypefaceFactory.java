package jo.celeris.android.view;

/**
 * Fabrique de variantes de police côté hôte : la dérivation concrète
 * (base → gras) vit derrière cette interface, hors du modèle.
 */
public interface TypefaceFactory {

    /**
     * Dérive la variante grasse de la police de base donnée.
     *
     * @param base la police de base
     * @return la variante grasse
     */
    FontHandle deriveBold(FontHandle base);
}
