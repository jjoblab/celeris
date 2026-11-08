package jo.celeris.android.widgets;

/**
 * Le badge de genre d'un candidat de complétion (portage du dépôt de
 * départ) : chaque suggestion porte un glyphe dans un carré arrondi
 * teinté par la nature du candidat — « K » violet pour un mot-clé,
 * « C » doré pour une classe, « I » cyan pour une interface, « E »
 * orange pour un enum, « M » pour une méthode (à l'encre d'accent du
 * thème), « F » bleu pour un champ, « v » pour une variable, « p »
 * gris pour un paquet, « {} » vert pour un extrait, « @ » pour une
 * annotation, « R » pour un enregistrement, « # » pour une constante
 * d'enum, « T » pour un paramètre de type.
 *
 * <p>La palette est calibrée pour un fond verre SOMBRE ; sur un thème
 * clair elle s'assombrit (mélange vers le noir à 38 %) — les teintes
 * moyennes manquent de contraste sur le verre clair. Classe pure :
 * glyphe et couleur ARGB calculables et testables sans Canvas.</p>
 */
public final class BadgeGenre {

    /** Les métadonnées du badge : le glyphe affiché et sa couleur. */
    public record Meta(String glyphe, int couleur) {
    }

    /** Le genre d'un candidat — le vocabulaire des hôtes. */
    public enum Genre {
        /** Mot réservé du langage. */
        MOT_CLE,
        /** Méthode, fonction, constructeur — à l'encre d'accent. */
        METHODE,
        /** Champ, propriété, constante nommée. */
        CHAMP,
        /** Variable locale, paramètre. */
        VARIABLE,
        /** Classe, enregistrement, paramètre de type. */
        CLASSE,
        /** Interface. */
        INTERFACE,
        /** Énumération, membre d'énumération. */
        ENUM,
        /** Paquet, module, dossier. */
        PAQUET,
        /** Extrait de code. */
        EXTRAIT,
        /** Annotation. */
        ANNOTATION
    }

    // ── Palette par genre — calibrée fond sombre ───────────────────
    private static final int DORE = 0xFFE6C178;
    private static final int INTERFACE = 0xFF57B6C2;
    private static final int ENUM = 0xFFD9A066;
    private static final int CHAMP = 0xFF61AFEF;
    private static final int VARIABLE = 0xFF5CCFE6;
    private static final int PAQUET = 0xFFA0A1AA;
    private static final int MOT_CLE = 0xFFCD7EE0;
    private static final int EXTRAIT = 0xFF98C97A;

    private BadgeGenre() {
    }

    /**
     * Les métadonnées du badge d'un genre.
     *
     * @param genre  la nature du candidat
     * @param sombre vrai pour un thème sombre (palette telle quelle),
     *               faux pour un thème clair (palette assombrie)
     * @param accent l'encre d'accent du thème — le badge de la méthode
     * @return le glyphe et sa couleur
     */
    public static Meta meta(Genre genre, boolean sombre, int accent) {
        return switch (genre) {
            case MOT_CLE -> new Meta("K", ton(MOT_CLE, sombre));
            case METHODE -> new Meta("M", accent);
            case CHAMP -> new Meta("F", ton(CHAMP, sombre));
            case VARIABLE -> new Meta("v", ton(VARIABLE, sombre));
            case CLASSE -> new Meta("C", ton(DORE, sombre));
            case INTERFACE -> new Meta("I", ton(INTERFACE, sombre));
            case ENUM -> new Meta("E", ton(ENUM, sombre));
            case PAQUET -> new Meta("p", ton(PAQUET, sombre));
            case EXTRAIT -> new Meta("{}", ton(EXTRAIT, sombre));
            case ANNOTATION -> new Meta("@", ton(DORE, sombre));
        };
    }

    /** La couleur telle quelle sur sombre, assombrie sur clair. */
    private static int ton(int couleur, boolean sombre) {
        return sombre ? couleur : assombrir(couleur);
    }

    /** Mélange la couleur vers le noir à 38 % (contraste du verre clair). */
    private static int assombrir(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (int) (((argb >>> 16) & 0xFF) * 0.62f);
        int g = (int) (((argb >>> 8) & 0xFF) * 0.62f);
        int b = (int) ((argb & 0xFF) * 0.62f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * La taille de police du glyphe relativement à la taille du badge
     * — un glyphe long (« {} ») se réduit pour tenir dans le carré.
     *
     * @param glyphe le glyphe du badge
     * @return le facteur de taille du glyphe
     */
    public static float facteurTailleGlyphe(String glyphe) {
        return glyphe != null && glyphe.length() > 1 ? 0.42f : 0.56f;
    }
}
