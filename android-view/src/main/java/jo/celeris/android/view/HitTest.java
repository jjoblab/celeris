package jo.celeris.android.view;

import jo.celeris.core.text.Document;

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

    /**
     * L'offset absolu visé par un point de l'écran : la rangée par
     * l'ordonnée (défilement compris, projection — plis et tranches
     * A-18 — traversées), puis la colonne par l'abscisse, bornée à la
     * fin de la tranche visible de la ligne (PIEGE-06 : coupe au
     * plancher).
     *
     * @param document         le document affiché
     * @param projection       la projection visuelle courante
     * @param metrics          les métriques d'éditeur courantes
     * @param largeurGouttiere la largeur de gouttière, en pixels
     * @param scrollX          le défilement horizontal, en pixels
     * @param scrollY          le défilement vertical, en pixels
     * @param x                l'abscisse du point dans la vue
     * @param y                l'ordonnée du point dans la vue
     * @return l'offset absolu visé, borné au document
     */
    public int offsetAt(Document document, VisualProjection projection,
            EditorMetrics metrics, float largeurGouttiere, float scrollX,
            float scrollY, float x, float y) {
        int rangee = ScrollGeometry.firstRow(scrollY + y, metrics.lineHeight());
        rangee = Math.max(0, Math.min(rangee, projection.rowCount() - 1));
        int ligne = projection.firstLineOfRow(rangee);
        int debutTranche = projection.spanStartAtRow(rangee);
        int longueurLigne = document.lineEnd(ligne) - document.lineStart(ligne);
        int finTranche = Math.min(projection.spanEndAtRow(rangee), longueurLigne);
        int colonne = columnAt(x - largeurGouttiere + scrollX,
                finTranche - debutTranche);
        return document.lineStart(ligne) + debutTranche + colonne;
    }
}
