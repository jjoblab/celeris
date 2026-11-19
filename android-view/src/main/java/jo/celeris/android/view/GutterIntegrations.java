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
 * {@link #setChips}, {@link #setPreviewMarks} ou
 * {@link #setAmpoules}, l'hôte invalide la vue — le rafraîchissement
 * reste son geste.</p>
 */
public final class GutterIntegrations {

    private IntegrationMarks integration = IntegrationMarks.EMPTY;

    private int[] erreursImportees = new int[0];

    private Map<Integer, Integer> chips = Map.of();

    private int[] previewMarks = new int[0];

    /** Les lignes porteuses de l'ampoule d'intentions — la ligne du
     *  caret qui porte un diagnostic, jamais devinée. */
    private int[] ampoules = new int[0];

    private boolean integrationLane;

    private boolean importedErrorsLane;

    private boolean previewLane;

    /** La dernière configuration servie — mémoïsée : le chemin d'image
     *  n'alloue rien en régime établi (PIEGE-15). */
    private GutterLanes recentLanes = new GutterLanes(false,
            false, false, false);
    private boolean dernierSyntaxeActif;

    private @Nullable IntConsumer onLineClick;

    private @Nullable IntConsumer surRenduPreview;

    /** L'écouteur du clic sur l'ampoule d'intentions : la ligne visée
     *  s'y rend — l'hôte y ouvre son menu. */
    private @Nullable IntConsumer surAmpoule;

    /**
     * Dépose les marques d'intégration courantes.
     *
     * @param marks les marques, {@link IntegrationMarks#EMPTY} pour
     *                retirer
     */
    public void setIntegration(IntegrationMarks marks) {
        integration = marks;
    }

    /**
     * Dépose les lignes porteuses d'un diagnostic importé du build
     * (C-12) : elles se fondent aux erreurs de la syntaxe — tableau
     * vide pour retirer.
     *
     * @param lines les lignes, numérotées à partir de zéro
     */
    public void setErreursImportees(int[] lines) {
        erreursImportees = lines.clone();
    }

    /**
     * Dépose les pastilles de couleur (C-05) : la ligne porteuse et sa
     * couleur résolue — liste vide pour retirer.
     *
     * @param chips les pastilles, une par ligne porteuse au plus
     */
    public void setChips(List<ColorChip> chips) {
        Map<Integer, Integer> resolues = new HashMap<>();
        for (ColorChip chip : chips) {
            resolues.put(chip.line(), chip.argb());
        }
        this.chips = Map.copyOf(resolues);
    }

    /**
     * La pastille de la ligne donnée.
     *
     * @param line la ligne, numérotée à partir de zéro
     * @return la couleur ARGB résolue, 0 si la ligne n'en porte pas
     */
    public int chip(int line) {
        return chips.getOrDefault(line, 0);
    }

    /**
     * Dépose les repères {@code @Preview} de Compose (C-11) — tableau
     * vide pour retirer.
     *
     * @param lines les lignes des annotations, numérotées à partir de
     *               zéro
     */
    public void setPreviewMarks(int[] lines) {
        previewMarks = lines.clone();
    }

    /**
     * Dépose les lignes porteuses de l'AMPOULE d'intentions (l'audit
     * Android Studio : la ligne du caret qui porte un diagnostic —
     * jamais devinée, l'hôte seul décide) — tableau vide pour retirer.
     *
     * @param lines les lignes de l'ampoule, numérotées à partir de zéro
     */
    public void setAmpoules(int[] lines) {
        ampoules = lines.clone();
    }

    /**
     * Vrai si la ligne porte l'ampoule d'intentions.
     *
     * @param line la ligne, numérotée à partir de zéro
     * @return vrai si la ligne porte l'ampoule
     */
    public boolean ampoule(int line) {
        for (int ampoule : ampoules) {
            if (ampoule == line) {
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
     * @param line la ligne, numérotée à partir de zéro
     * @return vrai si la ligne porte un repère
     */
    public boolean reperePreview(int line) {
        for (int repere : previewMarks) {
            if (repere == line) {
                return true;
            }
        }
        return false;
    }

    /**
     * L'écouteur des clics dans le champ des numéros : la ligne visée,
     * numérotée à partir de zéro, s'y rend — null retire l'écouteur.
     *
     * @param onLineClick l'écouteur, ou null
     */
    public void setOnLineClick(@Nullable IntConsumer onLineClick) {
        this.onLineClick = onLineClick;
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
     * @param marks les marques de la syntaxe (plis, erreurs)
     * @return les marques fondues avec les intégrations courantes et
     *         les diagnostics importés
     */
    public GutterMarks marks(GutterMarks marks) {
        return marks.avec(integration).avecErreurs(erreursImportees);
    }

    /**
     * Réserve (ou retire) la coulisse des intégrations (D-03) : la
     * largeur découle de la configuration, pas de l'arrivée des
     * marques (PIEGE-25).
     *
     * @param activee vrai pour réserver la coulisse
     */
    public void setIntegrationLane(boolean activee) {
        integrationLane = activee;
    }

    /**
     * Réserve (ou retire) la coulisse des diagnostics importés du
     * build (C-12).
     *
     * @param activee vrai pour réserver la coulisse
     */
    public void setImportedErrorsLane(boolean activee) {
        importedErrorsLane = activee;
    }

    /**
     * Réserve (ou retire) la coulisse des repères {@code @Preview}
     * (C-11).
     *
     * @param activee vrai pour réserver la coulisse
     */
    public void setPreviewLane(boolean activee) {
        previewLane = activee;
    }

    /**
     * La configuration de coulisse de l'image à venir (M7.5, §5) : les
     * intégrations ACTIVÉES — jamais celles qui parlent. La syntaxe
     * s'y ajoute du côté de la vue, qui sait si l'analyse est branchée.
     *
     * @param syntaxeActivee vrai quand l'analyse est branchée
     * @return la configuration courante
     */
    public GutterLanes lanes(boolean syntaxeActivee) {
        if (syntaxeActivee == dernierSyntaxeActif
                && recentLanes.integration() == integrationLane
                && recentLanes.importedDiagnostics() == importedErrorsLane
                && recentLanes.preview() == previewLane) {
            return recentLanes;
        }
        dernierSyntaxeActif = syntaxeActivee;
        recentLanes = new GutterLanes(syntaxeActivee,
                integrationLane, importedErrorsLane, previewLane);
        return recentLanes;
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
     * @param lane         la largeur courante de la coulisse
     * @param foldBand         la largeur courante de la bande de plis
     * @param scrollY          le défilement vertical courant
     * @param hauteurLigne     la hauteur d'une ligne, en pixels
     * @param projection       la projection courante (rangée vers ligne)
     * @return vrai quand le clic s'est consommé dans le champ des
     *         numéros
     */
    public boolean surAppui(float x, float y, float largeurGouttiere,
            float lane, float foldBand, float scrollY, float hauteurLigne,
            VisualProjection projection) {
        if (onLineClick == null || x <= lane
                || x >= largeurGouttiere - foldBand) {
            return false;
        }
        int rangee = ScrollGeometry.firstRow(scrollY + y, hauteurLigne);
        onLineClick.accept(projection.firstLineOfRow(rangee));
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
     * @param lane     la largeur courante de la coulisse
     * @param scrollY      le défilement vertical courant
     * @param hauteurLigne la hauteur d'une ligne, en pixels
     * @param projection   la projection courante (rangée vers ligne)
     * @return vrai quand le clic s'est consommé sur un repère
     */
    public boolean onMarkPress(float x, float y, float lane,
            float scrollY, float hauteurLigne, VisualProjection projection) {
        if (surRenduPreview == null || x > lane) {
            return false;
        }
        int rangee = ScrollGeometry.firstRow(scrollY + y, hauteurLigne);
        int line = projection.firstLineOfRow(rangee);
        if (!reperePreview(line)) {
            return false;
        }
        surRenduPreview.accept(line);
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
     * @param lane     la largeur courante de la coulisse
     * @param scrollY      le défilement vertical courant
     * @param hauteurLigne la hauteur d'une ligne, en pixels
     * @param projection   la projection courante (rangée vers ligne)
     * @return vrai quand le clic s'est consommé sur l'ampoule
     */
    public boolean surAppuiAmpoule(float x, float y, float lane,
            float scrollY, float hauteurLigne, VisualProjection projection) {
        if (surAmpoule == null || x > lane) {
            return false;
        }
        int rangee = ScrollGeometry.firstRow(scrollY + y, hauteurLigne);
        int line = projection.firstLineOfRow(rangee);
        if (!ampoule(line)) {
            return false;
        }
        surAmpoule.accept(line);
        return true;
    }
}
