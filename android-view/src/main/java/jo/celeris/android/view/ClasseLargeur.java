package jo.celeris.android.view;

/**
 * Les classes de largeur de fenêtre (D-19 — pliables, grands écrans,
 * mode bureau) : les seuils des classes de taille Material — compact
 * sous 600 dp, moyen jusqu'à 840 dp, étendu au-delà. Un pliable
 * déplié passe de compact à étendu d'un geste : la vue s'adapte par
 * {@link EditorView#setClasseLargeur(ClasseLargeur)}.
 */
public enum ClasseLargeur {

    /** Fenêtre étroite — téléphone replié, écran partagé. */
    COMPACT,

    /** Fenêtre moyenne — téléphone large, tablette en partage. */
    MOYEN,

    /** Fenêtre étendue — tablette, pliable déplié, mode bureau. */
    ETENDU;

    /** Le seuil du compact, en dp. */
    private static final float SEUIL_COMPACT = 600f;

    /** Le seuil de l'étendu, en dp. */
    private static final float SEUIL_ETENDU = 840f;

    /**
     * La classe d'une largeur de fenêtre.
     *
     * @param dp la largeur de la fenêtre la plus petite des deux
     *           dimensions, en dp
     * @return la classe : compact sous 600, moyen jusqu'à 840, étendu
     */
    public static ClasseLargeur de(float dp) {
        if (dp < SEUIL_COMPACT) {
            return COMPACT;
        }
        return dp < SEUIL_ETENDU ? MOYEN : ETENDU;
    }

    /**
     * La respiration de la gouttière pour cette classe (D-19) : le
     * compact resserre le champ de numéros — chaque pixel compte —,
     * l'étendu respire, le moyen garde la norme.
     *
     * @return la respiration de chaque côté du champ, en pixels
     */
    public float margeGouttiere() {
        return switch (this) {
            case COMPACT -> 4f;
            case MOYEN -> 8f;
            case ETENDU -> 12f;
        };
    }
}
