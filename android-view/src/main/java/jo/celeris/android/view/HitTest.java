package jo.celeris.android.view;

/**
 * Conversion position ← pixel : l'abscisse rend la colonne de caractère.
 *
 * <p>PIEGE-06 : la conversion coupe au PLANCHER — la frontière exacte
 * entre deux colonnes appartient à la colonne suivante, jamais d'arrondi
 * au plus proche. La discipline de visée des tests de vue en découle :
 * viser le quart gauche d'un glyphe (à distance sûre de toute frontière),
 * jamais son centre (équidistant des frontières — le piège historique).</p>
 */
public final class HitTest {

    private final float charWidth;

    /**
     * Construit le convertisseur pour les métriques données.
     *
     * @param metrics les métriques d'éditeur courantes
     */
    public HitTest(EditorMetrics metrics) {
        this.charWidth = metrics.charWidth();
    }

    /**
     * La colonne visée par une abscisse, sans borne haute.
     *
     * @param x l'abscisse en pixels
     * @return la colonne, nulle pour toute abscisse négative ou nulle
     */
    public int columnAt(float x) {
        if (x <= 0f) {
            return 0;
        }
        return (int) Math.floor(x / charWidth);
    }

    /**
     * La colonne visée par une abscisse, bornée à la colonne maximale.
     *
     * @param x         l'abscisse en pixels
     * @param maxColumn la colonne maximale rendable (fin de ligne comprise)
     * @return la colonne, bornée à [0, maxColumn]
     */
    public int columnAt(float x, int maxColumn) {
        int colonne = columnAt(x);
        if (maxColumn < 0) {
            maxColumn = 0;
        }
        return Math.min(colonne, maxColumn);
    }
}
