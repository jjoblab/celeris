package jo.celeris.android.view;

import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * Le point d'entrée des intégrations dans la gouttière (D-03, D-04,
 * D-05) : la vue y dépose ses marques d'intégration et son écouteur de
 * clic — la coulisse s'élargit quand les intégrations parlent, le clic
 * dans le champ des numéros rend la ligne visée.
 *
 * <p>La vue reste propriétaire : ce collaborateur ne fait que fondre
 * les marques de la syntaxe avec les intégrations et convertir le
 * point touché en ligne. Après un appel à {@link #setIntegration},
 * l'hôte invalide la vue — le rafraîchissement reste son geste.</p>
 */
public final class MarquesGouttiere {

    /** La coulisse réservée aux marques d'intégration : deux caractères. */
    private static final float COULISSE_INTEGRATION = 2f;

    private MarquesIntegration integration = MarquesIntegration.VIDE;

    private @Nullable IntConsumer surClicLigne;

    /**
     * Dépose les marques d'intégration courantes.
     *
     * @param marques les marques, {@link MarquesIntegration#VIDE} pour
     *                retirer
     */
    public void setIntegration(MarquesIntegration marques) {
        integration = marques;
    }

    /**
     * L'écouteur des clics dans le champ des numéros : la ligne visée,
     * numérotée à partir de zéro, s'y rend — null retire l'écouteur.
     *
     * @param surClicLigne l'écouteur, ou null
     */
    public void setSurClicLigne(@Nullable IntConsumer surClicLigne) {
        this.surClicLigne = surClicLigne;
    }

    /**
     * Les marques fondues de l'image à venir.
     *
     * @param marques les marques de la syntaxe (plis, erreurs)
     * @return les marques fondues avec les intégrations courantes
     */
    public GutterMarks marques(GutterMarks marques) {
        return marques.avec(integration);
    }

    /**
     * La largeur de coulisse de l'image à venir : celle de la syntaxe,
     * élargie aux intégrations quand elles parlent.
     *
     * @param coulisseSyntaxe la coulisse demandée par la syntaxe, en pixels
     * @param charWidth       la largeur d'un caractère, en pixels
     * @return la coulisse effective, en pixels
     */
    public float coulisse(float coulisseSyntaxe, float charWidth) {
        if (integration.estVide()) {
            return coulisseSyntaxe;
        }
        return Math.max(coulisseSyntaxe, COULISSE_INTEGRATION * charWidth);
    }

    /**
     * Le clic dans le champ des numéros de la gouttière : la ligne
     * visée se rend à l'écouteur, le clic se consomme.
     *
     * @param x                l'abscisse touchée, en pixels
     * @param y                l'ordonnée touchée, en pixels
     * @param largeurGouttiere la largeur courante de la gouttière
     * @param coulisse         la largeur courante de la coulisse
     * @param scrollY          le défilement vertical courant
     * @param hauteurLigne     la hauteur d'une ligne, en pixels
     * @param projection       la projection courante (rangée vers ligne)
     * @return vrai quand le clic s'est consommé dans le champ des
     *         numéros
     */
    public boolean surAppui(float x, float y, float largeurGouttiere,
            float coulisse, float scrollY, float hauteurLigne,
            VisualProjection projection) {
        if (surClicLigne == null || x <= coulisse || x >= largeurGouttiere) {
            return false;
        }
        int rangee = ScrollGeometry.firstRow(scrollY + y, hauteurLigne);
        surClicLigne.accept(projection.firstLineOfRow(rangee));
        return true;
    }
}
