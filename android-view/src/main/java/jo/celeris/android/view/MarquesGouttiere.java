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
 * repères {@code @Preview}, ses lignes d'AMPOULE d'intentions et son
 * écouteur de clic — la coulisse s'élargit quand les intégrations
 * parlent, le clic dans le champ des numéros rend la ligne visée, le
 * clic sur un repère délègue le rendu à l'hôte.
 *
 * <p>L'AMPOULE D'INTENTIONS (l'audit Android Studio du 2026-11-14 :
 * le propriétaire — « le petit icône ampoule jaune comme dans Android
 * Studio, je ne l'ai pas vue dans notre éditeur ») suit le patron du
 * repère {@code @Preview} : les lignes porteuses se déposent, le clic
 * dans la coulisse rend la ligne visée à l'hôte — qui y ouvre le menu
 * d'intentions. L'ampoule vit à la ligne du CARET quand elle porte un
 * diagnostic (le comportement de l'IDE de référence) : c'est l'hôte
 * qui décide, la vue ne devine rien.</p>
 *
 * <p>La vue reste propriétaire : ce collaborateur ne fait que fondre
 * les marques de la syntaxe avec les intégrations et les diagnostics
 * importés, et convertir le point touché en ligne. Après un appel à
 * {@link #setIntegration}, {@link #setErreursImportees},
 * {@link #setPastilles}, {@link #setReperesPreview} ou
 * {@link #setAmpoules}, l'hôte invalide la vue — le rafraîchissement
 * reste son geste.</p>
 */
public final class MarquesGouttiere {

    private MarquesIntegration integration = MarquesIntegration.VIDE;

    private int[] erreursImportees = new int[0];

    private Map<Integer, Integer> pastilles = Map.of();

    private int[] reperesPreview = new int[0];

    /** Les lignes porteuses de l'ampoule d'intentions — la ligne du
     *  caret qui porte un diagnostic, jamais devinée. */
    private int[] ampoules = new int[0];

    private boolean coulisseIntegration;

    private boolean coulisseErreursImportees;

    private boolean coulissePreview;

    /** La dernière configuration servie — mémoïsée : le chemin d'image
     *  n'alloue rien en régime établi (PIEGE-15). */
    private GutterLanes dernieresCoulisses = new GutterLanes(false,
            false, false, false);
    private boolean dernierSyntaxeActif;

    private @Nullable IntConsumer surClicLigne;

    private @Nullable IntConsumer surRenduPreview;

    /** L'écouteur du clic sur l'ampoule d'intentions : la ligne visée
     *  s'y rend — l'hôte y ouvre son menu. */
    private @Nullable IntConsumer surAmpoule;

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
     * Dépose les lignes porteuses de l'AMPOULE d'intentions (l'audit
     * Android Studio : la ligne du caret qui porte un diagnostic —
     * jamais devinée, l'hôte seul décide) — tableau vide pour retirer.
     *
     * @param lignes les lignes de l'ampoule, numérotées à partir de zéro
     */
    public void setAmpoules(int[] lignes) {
        ampoules = lignes.clone();
    }

    /**
     * Vrai si la ligne porte l'ampoule d'intentions.
     *
     * @param ligne la ligne, numérotée à partir de zéro
     * @return vrai si la ligne porte l'ampoule
     */
    public boolean ampoule(int ligne) {
        for (int ampoule : ampoules) {
            if (ampoule == ligne) {
                return true;
            }
        }
        return false;
    }

    /**
     * L'écouteur du clic sur l'ampoule d'intentions : la ligne visée
     * s'y rend — l'hôte y ouvre le menu d'intentions. Null retire
     * l'écouteur.
     *
     * @param surAmpoule l'écouteur, ou null
     */
    public void setSurAmpoule(@Nullable IntConsumer surAmpoule) {
        this.surAmpoule = surAmpoule;
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
     * Réserve (ou retire) la coulisse des intégrations (D-03) : la
     * largeur découle de la configuration, pas de l'arrivée des
     * marques (PIEGE-25).
     *
     * @param activee vrai pour réserver la coulisse
     */
    public void setCoulisseIntegration(boolean activee) {
        coulisseIntegration = activee;
    }

    /**
     * Réserve (ou retire) la coulisse des diagnostics importés du
     * build (C-12).
     *
     * @param activee vrai pour réserver la coulisse
     */
    public void setCoulisseErreursImportees(boolean activee) {
        coulisseErreursImportees = activee;
    }

    /**
     * Réserve (ou retire) la coulisse des repères {@code @Preview}
     * (C-11).
     *
     * @param activee vrai pour réserver la coulisse
     */
    public void setCoulissePreview(boolean activee) {
        coulissePreview = activee;
    }

    /**
     * La configuration de coulisse de l'image à venir (M7.5, §5) : les
     * intégrations ACTIVÉES — jamais celles qui parlent. La syntaxe
     * s'y ajoute du côté de la vue, qui sait si l'analyse est branchée.
     *
     * @param syntaxeActivee vrai quand l'analyse est branchée
     * @return la configuration courante
     */
    public GutterLanes coulisses(boolean syntaxeActivee) {
        if (syntaxeActivee == dernierSyntaxeActif
                && dernieresCoulisses.integration() == coulisseIntegration
                && dernieresCoulisses.importedDiagnostics() == coulisseErreursImportees
                && dernieresCoulisses.preview() == coulissePreview) {
            return dernieresCoulisses;
        }
        dernierSyntaxeActif = syntaxeActivee;
        dernieresCoulisses = new GutterLanes(syntaxeActivee,
                coulisseIntegration, coulisseErreursImportees, coulissePreview);
        return dernieresCoulisses;
    }

    /**
     * Le clic dans le champ des numéros de la gouttière : la ligne
     * visée se rend à l'écouteur, le clic se consomme. Le champ vit
     * entre la coulisse à gauche et la bande de plis à droite — le
     * chevron garde sa cible, les numéros la leur.
     *
     * @param x                l'abscisse touchée, en pixels
     * @param y                l'ordonnée touchée, en pixels
     * @param largeurGouttiere la largeur courante de la gouttière
     * @param coulisse         la largeur courante de la coulisse
     * @param bandePli         la largeur courante de la bande de plis
     * @param scrollY          le défilement vertical courant
     * @param hauteurLigne     la hauteur d'une ligne, en pixels
     * @param projection       la projection courante (rangée vers ligne)
     * @return vrai quand le clic s'est consommé dans le champ des
     *         numéros
     */
    public boolean surAppui(float x, float y, float largeurGouttiere,
            float coulisse, float bandePli, float scrollY, float hauteurLigne,
            VisualProjection projection) {
        if (surClicLigne == null || x <= coulisse
                || x >= largeurGouttiere - bandePli) {
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

    /**
     * Le clic sur l'AMPOULE d'intentions de la coulisse : la ligne
     * visée se rend à l'hôte — qui y ouvre le menu d'intentions —, le
     * clic se consomme. Le clic hors ampoule ne se consomme pas : les
     * plis, les numéros et les autres marques gardent leur priorité.
     *
     * @param x            l'abscisse touchée, en pixels
     * @param y            l'ordonnée touchée, en pixels
     * @param coulisse     la largeur courante de la coulisse
     * @param scrollY      le défilement vertical courant
     * @param hauteurLigne la hauteur d'une ligne, en pixels
     * @param projection   la projection courante (rangée vers ligne)
     * @return vrai quand le clic s'est consommé sur l'ampoule
     */
    public boolean surAppuiAmpoule(float x, float y, float coulisse,
            float scrollY, float hauteurLigne, VisualProjection projection) {
        if (surAmpoule == null || x > coulisse) {
            return false;
        }
        int rangee = ScrollGeometry.firstRow(scrollY + y, hauteurLigne);
        int ligne = projection.firstLineOfRow(rangee);
        if (!ampoule(ligne)) {
            return false;
        }
        surAmpoule.accept(ligne);
        return true;
    }
}
