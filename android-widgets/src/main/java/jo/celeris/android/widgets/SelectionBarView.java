package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * La barre de sélection (M7.5, §7, reprise — le design du dépôt de
 * départ, la pilule du 2026-11-13) : les actions de la sélection en
 * PASTILLES RESPIRÉES — chaque libellé respire de 12 dp horizontaux,
 * 2 dp les séparent —, un FILET sépare le groupe texte du groupe
 * icônes, deux ICÔNES le servent : la documentation (le « i » cerclé)
 * et le débordement (les trois points). La pastille pressée se
 * surligne à l'accent, la cascade d'entrée fait naître les items l'un
 * après l'autre (le pop du dépôt de départ) ; cède à la frappe (la
 * frappe remplace la sélection), survit au défilement ; jamais de
 * focus (PIEGE-27), le clavier reste à l'éditeur.
 *
 * <p>La capsule prend le rayon de sa hauteur — la pilule du dépôt de
 * départ, portée au verre du kit. L'appui sur une pastille l'exécute ;
 * celui du « i » prévient l'hôte (la documentation au point
 * sélectionné) ; celui des trois points ouvre le menu qui sert les
 * actions repliées.</p>
 */
public final class SelectionBarView extends PopupPanel {

    /** Une action de la barre : son libellé, son pictogramme propre
     *  (null : la pastille sans glyphe) et ce qu'elle fait — l'audit
     *  du propriétaire du 2026-11-14 : chaque action porte SON icône,
     *  jamais la même pour toutes. */
    public record Item(String label, IconeAction picto, Runnable action) {

        /** L'action sans glyphe — les hôtes qui ne posent pas
         *  d'icône gardent leur pastille telle quelle. */
        public Item(String label, Runnable action) {
            this(label, null, action);
        }
    }

    /** La hauteur du contenu de la pilule, en dp (14 dp de texte,
     *  2 × 6 dp de respiration — resserrée par l'audit de compacité). */
    private static final float HAUTEUR_DP = 26f;

    /** La respiration horizontale d'un libellé, en dp. */
    private static final float PAD_X_DP = 10f;

    /** L'écart entre deux pastilles, en dp. */
    private static final float ECART_DP = 2f;

    /** Le budget du filet séparateur, en dp. */
    private static final float FILET_DP = 5f;

    /** La largeur d'une icône, en dp (12 dp de glyphe, 2 × 5 dp). */
    private static final float ICONE_DP = 22f;

    /** La largeur du pictogramme d'une pastille, en dp (l'audit du
     *  propriétaire du 2026-11-14 : « les icônes sont un peu trop
     *  grosses » — le glyphe se resserre sous le libellé). */
    private static final float PICTO_PASTILLE_DP = 11f;

    /** La respiration entre le pictogramme et le libellé, en dp. */
    private static final float ECART_PICTO_DP = 4f;

    /** L'index virtuel de l'icône documentation chez le toucher. */
    private static final int ICONE_DOCS = -2;

    /** L'index virtuel de la puce de débordement chez le toucher. */
    private static final int PUCE_DEBORDEMENT = -3;

    private final List<Item> actions = new ArrayList<>();
    /** Les bornes [début, fin] de chaque pastille texte, en contenu. */
    private final List<float[]> bornes = new ArrayList<>();
    /** Les actions du débordement — servies par le menu de l'hôte. */
    private List<Item> debordement = List.of();
    private final float hauteurPx;
    private final float padX;
    private final float ecartPx;
    private final float filetPx;
    private final float iconePx;
    private final float pictoPastillePx;
    private final float ecartPictoPx;
    /** La pastille sous le doigt, —1 hors appui, ICONE_DOCS ou
     *  PUCE_DEBORDEMENT quand le doigt pose une icône. */
    private int pressee = -1;
    /** Le rappel de l'appui sur le « i » — l'hôte y sert sa documentation. */
    private @Nullable Runnable onDocs;
    /** Le rappel de l'appui sur la puce de débordement. */
    private @Nullable Runnable onOverflow;

    public SelectionBarView(Context context, PopupPalette palette) {
        super(context, palette);
        float densite = getResources().getDisplayMetrics().density;
        this.hauteurPx = HAUTEUR_DP * densite;
        this.padX = PAD_X_DP * densite;
        this.ecartPx = ECART_DP * densite;
        this.filetPx = FILET_DP * densite;
        this.iconePx = ICONE_DP * densite;
        this.pictoPastillePx = PICTO_PASTILLE_DP * densite;
        this.ecartPictoPx = ECART_PICTO_DP * densite;
    }

    /**
     * Pose les actions de la barre : les principales en pastilles, le
     * débordement derrière la puce — le menu de l'hôte les sert.
     *
     * @param principales les actions visibles, dans l'ordre d'affichage
     * @param debordement les actions repliées, vides pour cacher la puce
     */
    public void setActions(List<Item> principales, List<Item> debordement) {
        List<Item> nouveaux = List.copyOf(principales);
        List<Item> repliees = List.copyOf(debordement);
        if (nouveaux.equals(actions) && repliees.equals(this.debordement)) {
            // les mêmes actions ne font pas re-naître la barre : le drag
            // de sélection la déplace d'un seul tenant (le dépôt de départ)
            return;
        }
        actions.clear();
        actions.addAll(nouveaux);
        this.debordement = repliees;
        mesurerPastilles();
        requestLayout();
        invalidate();
    }

    /** Mesure les bornes des pastilles texte, respirations comprises —
     *  le pictogramme de l'action précède le libellé, l'emprise y
     *  compte. */
    private void mesurerPastilles() {
        bornes.clear();
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_TITRE));
        float curseur = 0f;
        for (Item item : actions) {
            float largeur = 2 * padX + emprisePicto(item)
                    + textPaint().measureText(item.label());
            bornes.add(new float[] {curseur, curseur + largeur});
            curseur += largeur + ecartPx;
        }
    }

    /** L'emprise du pictogramme de la pastille — glyphe et
     *  respiration, zéro sans glyphe. */
    private float emprisePicto(Item item) {
        return item.picto() == null ? 0f : pictoPastillePx + ecartPictoPx;
    }

    /** Vrai quand la puce de débordement se montre — l'hôte y branche. */
    public boolean aDebordement() {
        return !debordement.isEmpty();
    }

    /**
     * Le rappel de l'appui sur le « i » de documentation — l'hôte y
     * ouvre sa documentation au point sélectionné.
     *
     * @param rappel le rappel, null pour le retirer
     */
    public void setOnDocs(@Nullable Runnable rappel) {
        onDocs = rappel;
        requestLayout();
        invalidate();
    }

    /**
     * Le rappel de l'appui sur la puce de débordement — l'hôte y ouvre
     * le menu qui sert les actions repliées.
     *
     * @param rappel le rappel, null pour le retirer
     */
    public void setOnOverflow(@Nullable Runnable rappel) {
        onOverflow = rappel;
    }

    /**
     * L'abscisse du CENTRE de la puce de débordement, dans le repère du
     * contenu — l'hôte y ancre son menu.
     */
    float puceDebordementCentreX() {
        return debutIcones() + (onDocs != null ? iconePx + ecartPx : 0f)
                + iconePx * 0.5f;
    }

    /**
     * L'abscisse du CENTRE de la puce de débordement, dans le repère de
     * la VUE (respirations comprises) — l'hôte y ancre son menu.
     */
    public int ancrePuceDebordementX() {
        return Math.round(paddingPx + puceDebordementCentreX());
    }

    /** L'ordonnée du centre du contenu — les bancs y visent. */
    float centerY() {
        return paddingYPx + hauteurPx / 2f;
    }

    /** L'abscisse de début du groupe icônes (filet compris). */
    private float debutIcones() {
        return bornes.isEmpty() ? 0f
                : bornes.get(bornes.size() - 1)[1] + ecartPx + filetPx;
    }

    /** Vrai quand le « i » de documentation se montre. */
    private boolean docsVisible() {
        return onDocs != null;
    }

    @Override
    void drawContent(Canvas canvas) {
        // l'entrée tient toute entière dans la naissance du panneau — le
        // ressort unique de la carte (l'IDE de référence n'égrène pas ses
        // pastilles : la pilule se pose d'un tenant)
        for (int i = 0; i < actions.size(); i++) {
            dessinerPastille(canvas, i);
        }
        float xIcones = debutIcones();
        if (docsVisible()) {
            dessinerIconeDocs(canvas, xIcones);
        }
        if (aDebordement()) {
            dessinerPuce(canvas, xIcones + (docsVisible()
                    ? iconePx + ecartPx : 0f));
        }
        if (!actions.isEmpty() && (docsVisible() || aDebordement())) {
            dessinerFilet(canvas);
        }
    }

    /** Une pastille texte : sa bande pressée, SON pictogramme à l'encre
     *  ALLÉGÉE (l'audit du propriétaire : « il est trop foncé » — le
     *  glyphe se lit, il ne crie pas), son libellé. */
    private void dessinerPastille(Canvas canvas, int index) {
        float[] borne = bornes.get(index);
        Item item = actions.get(index);
        String label = item.label();
        if (pressee == index) {
            dessinerBandePressee(canvas, borne[0], borne[1]);
        }
        textPaint().setColor(palette.text());
        float contenu = emprisePicto(item)
                + textPaint().measureText(label);
        float debut = (borne[0] + borne[1]) / 2f - contenu / 2f;
        if (item.picto() != null) {
            IconeAction.dessiner(item.picto(), canvas, linePaint(),
                    textPaint(), debut + pictoPastillePx / 2f,
                    hauteurPx / 2f, pictoPastillePx / 2f,
                    IconeAction.encreAllgeee(palette.muted(), palette.text()));
        }
        canvas.drawText(label, debut + emprisePicto(item), baseline(),
                textPaint());
    }

    /** Le « i » de documentation : cercle cerclé, lettre au centre. */
    private void dessinerIconeDocs(Canvas canvas, float gauche) {
        if (pressee == ICONE_DOCS) {
            dessinerBandePressee(canvas, gauche, gauche + iconePx);
        }
        float cx = gauche + iconePx / 2f;
        float cy = hauteurPx / 2f;
        float densite = getResources().getDisplayMetrics().density;
        linePaint().setColor(palette.muted());
        linePaint().setStrokeWidth(1.1f * densite);
        canvas.drawCircle(cx, cy, 5.5f * densite, linePaint());
        textPaint().setColor(palette.muted());
        float taille = textPaint().getTextSize();
        textPaint().setTextSize(9.5f * densite);
        canvas.drawText("i", cx - textPaint().measureText("i") / 2f,
                cy + textPaint().measureText("A") * 0.35f, textPaint());
        textPaint().setTextSize(taille);
    }

    /** La puce de débordement : trois points à l'encre atténuée. */
    private void dessinerPuce(Canvas canvas, float gauche) {
        if (pressee == PUCE_DEBORDEMENT) {
            dessinerBandePressee(canvas, gauche, gauche + iconePx);
        }
        bandPaint().setColor(palette.muted());
        float densite = getResources().getDisplayMetrics().density;
        float cx = gauche + iconePx / 2f;
        for (int i = 0; i < 3; i++) {
            float cy = hauteurPx / 2f + (i - 1) * 3f * densite;
            canvas.drawCircle(cx, cy, 1.1f * densite, bandPaint());
        }
    }

    /** La bande de la pastille pressée — l'accent au voile doux. */
    private void dessinerBandePressee(Canvas canvas, float gauche, float fin) {
        float densite = getResources().getDisplayMetrics().density;
        float rayon = PopupTokens.RAYON_RANGEE_DP * densite;
        bandPaint().setColor((palette.accent() & 0x00FFFFFF) | 0x38000000);
        canvas.drawRoundRect(gauche, 2f * densite, fin,
                hauteurPx - 2f * densite, rayon, rayon, bandPaint());
    }

    /** Le filet séparateur du groupe texte et du groupe icônes. */
    private void dessinerFilet(Canvas canvas) {
        linePaint().setStrokeWidth(1f);
        linePaint().setColor(palette.border());
        float x = debutIcones() - filetPx / 2f;
        canvas.drawLine(x, 0f, x, hauteurPx, linePaint());
    }

    @Override
    int contentWidth() {
        if (bornes.isEmpty()) {
            return Math.round(largeurIcones());
        }
        float largeur = bornes.get(bornes.size() - 1)[1];
        if (docsVisible() || aDebordement()) {
            largeur += ecartPx + filetPx + largeurIcones();
        }
        return Math.round(largeur);
    }

    /** La largeur du groupe d'icônes de la pilule — le « i » de
     *  documentation, la puce de débordement, leur écart. */
    private float largeurIcones() {
        float largeur = docsVisible() ? iconePx : 0f;
        if (aDebordement()) {
            largeur += (docsVisible() ? ecartPx : 0f) + iconePx;
        }
        return largeur;
    }

    @Override
    int contentHeight() {
        return Math.round(hauteurPx);
    }

    /** La capsule : le rayon de la carte prend la moitié de la hauteur. */
    @Override
    float rayonCartePx() {
        return getHeight() / 2f;
    }

    /**
     * L'index de la pastille visée par l'abscisse, —1 si hors barre,
     * ICONE_DOCS ou PUCE_DEBORDEMENT si une icône y tombe.
     */
    private int chipAt(float x) {
        for (int i = 0; i < bornes.size(); i++) {
            if (x >= bornes.get(i)[0] && x < bornes.get(i)[1]) {
                return i;
            }
        }
        float icones = debutIcones();
        if (docsVisible() && x >= icones && x < icones + iconePx) {
            return ICONE_DOCS;
        }
        float puce = icones + (docsVisible() ? iconePx + ecartPx : 0f);
        if (aDebordement() && x >= puce && x < puce + iconePx) {
            return PUCE_DEBORDEMENT;
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            pressee = chipAt(event.getX() - paddingPx);
            invalidate();
            return true;
        }
        if (event.getActionMasked() != MotionEvent.ACTION_UP) {
            return false;
        }
        int index = chipAt(event.getX() - paddingPx);
        pressee = -1;
        invalidate();
        if (index == ICONE_DOCS) {
            if (onDocs != null) {
                onDocs.run();
            }
            performClick();
            return true;
        }
        if (index == PUCE_DEBORDEMENT) {
            if (onOverflow != null) {
                onOverflow.run();
            }
            performClick();
            return true;
        }
        if (index >= 0 && index < actions.size()) {
            actions.get(index).action().run();
            performClick();
            return true;
        }
        return false;
    }

    /** La ligne de base du texte, centré verticalement. */
    private float baseline() {
        return hauteurPx / 2f + textPaint().measureText("A") * 0.35f;
    }
}
