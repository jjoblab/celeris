package jo.celeris.android.view;

/**
 * Zoom au pincement (D-15) : la taille de police monte par PALIERS —
 * jamais une valeur libre qui reclasserait le texte à chaque pixel —
 * et le défilement se réajuste pour que le point du document sous le
 * doigt y reste.
 *
 * <p>Le facteur d'un détecteur de pincement est INCRÉMENTAL (PIEGE-24)
 * : il rapporte l'écartement des doigts à l'événement PRÉCÉDENT, une
 * valeur voisine de 1,00 à chaque appel. Le palier, lui, se choisit
 * sur le facteur CUMULÉ depuis le début du geste —
 * {@link #targetSize} le borne puis le compose avec la taille de
 * départ, {@link #steppedSize(float, float)} y marche palier par
 * palier.</p>
 *
 * <p>Hystérésis : un palier s'entre dès que la cible l'approche à
 * moins de quinze pour cent de l'intervalle qui le sépare du palier
 * courant, et on ne l'en quitte que franchi le même seuil côté
 * contraire — un tremblement de ±3 % des doigts ne fait jamais vibrer
 * le texte entre deux paliers. La bande retenue (« le palier s'entre à
 * 15 % de l'intervalle près ») est la seule compatible avec la
 * trajectoire documentée (facteur cumulé 1,25 depuis 14 sp : la cible
 * 17,5 reste au palier 16) ; la lecture littérale « milieu de
 * l'intervalle + 15 % » y contredirait le palier attendu — l'écart est
 * consigné au rapport M7.5.</p>
 */
public final class PinchZoom {

    /** L'échelle des tailles autorisées, croissante. */
    private static final float[] STEPS = {10f, 12f, 14f, 16f, 18f, 20f, 24f, 28f};

    /** Le facteur cumulé le plus bas servi par la cible. */
    private static final float FACTEUR_MIN = 0.25f;

    /** Le facteur cumulé le plus haut servi par la cible. */
    private static final float FACTEUR_MAX = 4f;

    /** La bande d'hystérésis, en part de l'intervalle entre deux paliers. */
    private static final float BAND = 0.15f;

    private PinchZoom() {
    }

    /**
     * La taille cible du geste : la taille de départ × le facteur
     * cumulé depuis le début du pincement, le facteur borné — la cible
     * ne s'échappe jamais, les doigts ont des limites.
     *
     * @param startSize        la taille de police au début du geste, en sp
     * @param cumulativeFactor le facteur cumulé (écartement courant
     *                        divisé par l'écartement de départ)
     * @return la taille cible, en sp
     */
    public static float targetSize(float startSize, float cumulativeFactor) {
        float facteur = Math.min(FACTEUR_MAX, Math.max(FACTEUR_MIN, cumulativeFactor));
        return startSize * facteur;
    }

    /**
     * Le palier atteint par la cible : la marche grimpe ou descend d'un
     * cran à la fois, chaque cran franchi seulement au-delà de sa bande
     * d'hystérésis — jamais de saut libre, jamais de vibration.
     *
     * @param currentStep le palier courant affiché, en sp
     * @param targetSize  la taille cible du geste, en sp
     * @return le palier retenu, en sp
     */
    public static float steppedSize(float currentStep, float targetSize) {
        int indice = indexOf(currentStep);
        while (indice + 1 < STEPS.length && targetSize >= seuilHaut(indice)) {
            indice++;
        }
        while (indice > 0 && targetSize <= seuilBas(indice)) {
            indice--;
        }
        return STEPS[indice];
    }

    /**
     * D-15 — le défilement horizontal qui garde la colonne sous le
     * doigt : la colonne visée avant le zoom — lue à la gouttière et au
     * défilement d'AVANT — se retrouve à la même abscisse après, posée
     * à la gouttière d'APRÈS. La colonne se lit dans le repère de son
     * image : mélanger les gouttières dériverait le point focal d'un
     * palier à l'autre.
     *
     * @param focalX      l'abscisse du point focal, en pixels
     * @param oldGutter   la largeur de gouttière d'avant, en pixels
     * @param newGutter   la largeur de gouttière d'après, en pixels
     * @param scrollX     le défilement horizontal d'avant, en pixels
     * @param oldCharWidth la largeur de caractère d'avant, en pixels
     * @param newCharWidth la largeur de caractère d'après, en pixels
     * @return le défilement horizontal d'après, en pixels
     */
    public static float scrollXKeepingColumn(float focalX, float oldGutter,
            float newGutter, float scrollX, float oldCharWidth,
            float newCharWidth) {
        float column = (focalX - oldGutter + scrollX)
                / Math.max(1e-6f, oldCharWidth);
        return Math.max(0f, column * newCharWidth - (focalX - newGutter));
    }

    /**
     * D-15 — le défilement vertical qui garde la rangée sous le doigt.
     *
     * @param focalY        l'ordonnée du point focal, en pixels
     * @param scrollY       le défilement vertical d'avant, en pixels
     * @param oldLineHeight la hauteur de rangée d'avant, en pixels
     * @param newLineHeight la hauteur de rangée d'après, en pixels
     * @return le défilement vertical d'après, en pixels
     */
    public static float scrollYKeepingRow(float focalY, float scrollY,
            float oldLineHeight, float newLineHeight) {
        float rangee = (focalY + scrollY) / Math.max(1e-6f, oldLineHeight);
        return Math.max(0f, rangee * newLineHeight - focalY);
    }

    /**
     * Le palier voisin au-dessus du palier courant — un cran exact, la
     * voie du raccourci clavier et de la molette (D-15) : Ctrl + plus
     * n'a jamais à traverser deux paliers d'un coup.
     *
     * @param currentStep le palier courant, en sp
     * @return le palier juste au-dessus, le courant au sommet de l'échelle
     */
    public static float stepAbove(float currentStep) {
        int indice = indexOf(currentStep);
        if (indice + 1 < STEPS.length) {
            return STEPS[indice + 1];
        }
        return STEPS[indice];
    }

    /**
     * Le palier voisin en dessous du palier courant — un cran exact,
     * borné au pied de l'échelle (D-15).
     *
     * @param currentStep le palier courant, en sp
     * @return le palier juste en dessous, le courant au pied de l'échelle
     */
    public static float stepBelow(float currentStep) {
        int indice = indexOf(currentStep);
        if (indice > 0) {
            return STEPS[indice - 1];
        }
        return STEPS[indice];
    }

    /** L'indice du palier courant, borné à l'échelle. */
    private static int indexOf(float palier) {
        int meilleur = 0;
        for (int i = 1; i < STEPS.length; i++) {
            if (Math.abs(STEPS[i] - palier) < Math.abs(STEPS[meilleur] - palier)) {
                meilleur = i;
            }
        }
        return meilleur;
    }

    /** Le seuil d'entrée du palier suivant : le palier moins la bande. */
    private static float seuilHaut(int indice) {
        return STEPS[indice + 1] - BAND * (STEPS[indice + 1] - STEPS[indice]);
    }

    /** Le seuil de sortie vers le palier précédent : le palier plus la bande. */
    private static float seuilBas(int indice) {
        return STEPS[indice - 1] + BAND * (STEPS[indice] - STEPS[indice - 1]);
    }
}
