package jo.celeris.android.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * Le point d'entrée des intégrations dans la gouttière (D-03, D-04,
 * D-05, C-05, C-11, C-12) : la vue y dépose ses marques d'intégration,
 * ses lignes de diagnostics importés, ses pastilles de couleur, ses
 * repères {@code @Preview} et son écouteur de clic — la coulisse
 * s'élargit quand les intégrations parlent, le clic dans le champ des
 * numéros rend la ligne visée, le clic sur un repère délègue le rendu
 * à l'hôte.
 *
 * <p>La vue reste propriétaire : ce collaborateur ne fait que fondre
 * les marques de la syntaxe avec les intégrations et les diagnostics
 * importés, et convertir le point touché en ligne. Après un appel à
 * {@link #setIntegration}, {@link #setErreursImportees},
 * {@link #setPastilles} ou {@link #setReperesPreview}, l'hôte invalide
 * la vue — le rafraîchissement reste son geste.</p>
 */
public final class MarquesGouttiere {

    /** La coulisse réservée aux marques d'intégration : deux caractères. */
    private static final float COULISSE_INTEGRATION = 2f;

    private MarquesIntegration integration = MarquesIntegration.VIDE;

    private int[] erreursImportees = new int[0];

    private Map<Integer, Integer> pastilles = Map.of();

    private int[] reperesPreview = new int[0];

    private @Nullable IntConsumer surClicLigne;

    private @Nullable IntConsumer surRenduPreview;

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
     * Dépose les lignes porteuses d'un diagnostic importé du build
     * (C-12) : elles se fondent aux erreurs de la syntaxe — tableau
     * vide pour retirer.
     *
     * @param lignes les lignes, numérotées à partir de zéro
     */
    public void setErreursImportees(int[] lignes) {
        erreursImportees = lignes.clone();
    }

    /**
     * Dépose les pastilles de couleur (C-05) : la ligne porteuse et sa
     * couleur résolue — liste vide pour retirer.
     *
     * @param pastilles les pastilles, une par ligne porteuse au plus
     */
    public void setPastilles(List<PastilleCouleur> pastilles) {
        Map<Integer, Integer> resolues = new HashMap<>();
        for (PastilleCouleur pastille : pastilles) {
            resolues.put(pastille.ligne(), pastille.argb());
        }
        this.pastilles = Map.copyOf(resolues);
    }

    /**
     * La pastille de la ligne donnée.
     *
     * @param ligne la ligne, numérotée à partir de zéro
     * @return la couleur ARGB résolue, 0 si la ligne n'en porte pas
     */
    public int pastille(int ligne) {
        return pastilles.getOrDefault(ligne, 0);
    }

    /**
     * Dépose les repères {@code @Preview} de Compose (C-11) — tableau
     * vide pour retirer.
     *
     * @param lignes les lignes des annotations, numérotées à partir de
     *               zéro
     */
    public void setReperesPreview(int[] lignes) {
        reperesPreview = lignes.clone();
    }

    /**
     * Vrai si la ligne porte un repère {@code @Preview} (C-11).
     *
     * @param ligne la ligne, numérotée à partir de zéro
     * @return vrai si la ligne porte un repère
     */
    public boolean reperePreview(int ligne) {
        for (int repere : reperesPreview) {
            if (repere == ligne) {
                return true;
            }
        }
        return false;
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
     * L'écouteur du rendu délégué d'un repère {@code @Preview} (C-11) :
     * le clic sur le repère rend la ligne porteuse à l'hôte, le rendu
     * lui appartient — null retire l'écouteur.
     *
     * @param surRenduPreview l'écouteur, ou null
     */
    public void setSurRenduPreview(@Nullable IntConsumer surRenduPreview) {
        this.surRenduPreview = surRenduPreview;
    }

    /**
     * Les marques fondues de l'image à venir.
     *
     * @param marques les marques de la syntaxe (plis, erreurs)
     * @return les marques fondues avec les intégrations courantes et
     *         les diagnostics importés
     */
    public GutterMarks marques(GutterMarks marques) {
        return marques.avec(integration).avecErreurs(erreursImportees);
    }

    /**
     * La largeur de coulisse de l'image à venir : celle de la syntaxe,
     * élargie aux intégrations quand elles parlent — pastilles exceptées,
     * elles peignent dans la respiration du champ de numéros.
     *
     * @param coulisseSyntaxe la coulisse demandée par la syntaxe, en pixels
     * @param charWidth       la largeur d'un caractère, en pixels
     * @return la coulisse effective, en pixels
     */
    public float coulisse(float coulisseSyntaxe, float charWidth) {
        if (integration.estVide() && erreursImportees.length == 0
                && reperesPreview.length == 0) {
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

    /**
     * Le clic sur un repère {@code @Preview} de la coulisse (C-11) :
     * le rendu de la ligne visée se délègue à l'hôte, le clic se
     * consomme. Le clic hors repère ne se consomme pas — les plis et
     * les autres marques gardent leur priorité.
     *
     * @param x            l'abscisse touchée, en pixels
     * @param y            l'ordonnée touchée, en pixels
     * @param coulisse     la largeur courante de la coulisse
     * @param scrollY      le défilement vertical courant
     * @param hauteurLigne la hauteur d'une ligne, en pixels
     * @param projection   la projection courante (rangée vers ligne)
     * @return vrai quand le clic s'est consommé sur un repère
     */
    public boolean surAppuiRepere(float x, float y, float coulisse,
            float scrollY, float hauteurLigne, VisualProjection projection) {
        if (surRenduPreview == null || x > coulisse) {
            return false;
        }
        int rangee = ScrollGeometry.firstRow(scrollY + y, hauteurLigne);
        int ligne = projection.firstLineOfRow(rangee);
        if (!reperePreview(ligne)) {
            return false;
        }
        surRenduPreview.accept(ligne);
        return true;
    }
}
