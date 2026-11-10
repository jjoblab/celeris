package jo.celeris.core.syntax;

/**
 * Le style d'une portée (M7.6, H-07) : l'encre et ses trois attributs
 * — gras, italique, souligné. L'encre absente (zéro) dit « le thème ne
 * la fixe pas » : la couleur du consommateur fait foi (la palette par
 * sorte du thème d'éditeur), les attributs s'appliquent quand même.
 *
 * @param foreground l'encre ARGB opaque, ou zéro si le style ne la fixe pas
 * @param bold       vrai quand la portée se trace en graisse
 * @param italic     vrai quand la portée se trace en italique
 * @param underline  vrai quand la portée se trace soulignée
 */
public record ScopeStyle(int foreground, boolean bold, boolean italic,
        boolean underline) {

    /** Constructeur canonique : l'encre est absente ou opaque. */
    public ScopeStyle {
        if (foreground != 0 && (foreground >>> 24) != 0xFF) {
            throw new IllegalArgumentException(
                    "scope style foreground must be opaque: " + foreground);
        }
    }

    /**
     * Le style d'encre seule — les attributs éteints.
     *
     * @param foreground l'encre ARGB opaque
     * @return le style d'encre seule
     */
    public static ScopeStyle ink(int foreground) {
        return new ScopeStyle(foreground, false, false, false);
    }

    /** Vrai quand le style fixe une encre. */
    public boolean hasInk() {
        return foreground != 0;
    }
}
