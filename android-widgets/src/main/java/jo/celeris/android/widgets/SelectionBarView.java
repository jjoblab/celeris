package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.animation.PathInterpolator;

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

    /** Une action de la barre : son libellé et ce qu'elle fait. */
    public record Item(String label, Runnable action) {
    }

    /** La hauteur du contenu de la pilule, en dp (14 dp de texte,
     *  2 × 8 dp de respiration — la pilule du dépôt de départ). */
    private static final float HAUTEUR_DP = 30f;

    /** La respiration horizontale d'un libellé, en dp. */
    private static final float PAD_X_DP = 12f;

    /** L'écart entre deux pastilles, en dp. */
    private static final float ECART_DP = 2f;

    /** Le budget du filet séparateur, en dp. */
    private static final float FILET_DP = 6f;

    /** La largeur d'une icône, en dp (16 dp de glyphe, 2 × 8 dp). */
    private static final float ICONE_DP = 32f;

    /** L'index virtuel de l'icône documentation chez le toucher. */
    private static final int ICONE_DOCS = -2;

    /** L'index virtuel de la puce de débordement chez le toucher. */
    private static final int PUCE_DEBORDEMENT = -3;

    /** La durée du pop d'entrée, en millisecondes. */
    private static final long DUREE_POP_MS = 160L;

    /** La durée de la cascade d'un item, en millisecondes. */
    private static final long DUREE_ITEM_MS = 120L;

    /** Le décalage de la cascade entre deux items, en millisecondes. */
    private static final long DECAL_ITEM_MS = 24L;

    /** La montée de la cascade, en dp. */
    private static final float MONTEE_DP = 5f;

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
    private final float monteePx;
    private final PathInterpolator adoucissement = new PathInterpolator(0.215f,
            0.61f, 0.355f, 1f);
    /** La pastille sous le doigt, —1 hors appui, ICONE_DOCS ou
     *  PUCE_DEBORDEMENT quand le doigt pose une icône. */
    private int pressee = -1;
    /** Le rappel de l'appui sur le « i » — l'hôte y sert sa documentation. */
    private @Nullable Runnable onDocs;
    /** Le rappel de l'appui sur la puce de débordement. */
    private @Nullable Runnable onOverflow;
    /** L'instant de départ de l'entrée, —1 entrée close. */
    private long entreeDepart = -1L;

    public SelectionBarView(Context context, PopupPalette palette) {
        super(context, palette);
        float densite = getResources().getDisplayMetrics().density;
        this.hauteurPx = HAUTEUR_DP * densite;
        this.padX = PAD_X_DP * densite;
        this.ecartPx = ECART_DP * densite;
        this.filetPx = FILET_DP * densite;
        this.iconePx = ICONE_DP * densite;
        this.monteePx = MONTEE_DP * densite;
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
        entreeDepart = getDrawingTime();
        requestLayout();
        invalidate();
    }

    /** Clôt l'entrée d'un geste — les bancs y figent la cascade. */
    void finirEntree() {
        entreeDepart = -1L;
    }

    /** Mesure les bornes des pastilles texte, respirations comprises. */
    private void mesurerPastilles() {
        bornes.clear();
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_TITRE));
        float curseur = 0f;
        for (Item item : actions) {
            float largeur = 2 * padX + textPaint().measureText(item.label());
            bornes.add(new float[] {curseur, curseur + largeur});
            curseur += largeur + ecartPx;
        }
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
        if (entreeDepart < 0L) {
            dessinerItems(canvas, 1f, 1f);
            return;
        }
        long ecoule = getDrawingTime() - entreeDepart;
        if (ecoule < 0L || ecoule > dureeTotale()) {
            entreeDepart = -1L;
            dessinerItems(canvas, 1f, 1f);
            return;
        }
        float pop = Math.min(1f, ecoule / (float) DUREE_POP_MS);
        float echelle = 0.96f + 0.04f * adoucissement.getInterpolation(pop);
        dessinerItems(canvas, echelle, Math.min(1f,
                ecoule / (float) DUREE_POP_MS * 1.4f));
        postInvalidateOnAnimation();
    }

    /** La durée totale de l'entrée : le pop, puis la cascade entière. */
    private long dureeTotale() {
        int nombre = actions.size() + (docsVisible() ? 1 : 0)
                + (aDebordement() ? 1 : 0);
        return DUREE_POP_MS + nombre * DECAL_ITEM_MS + DUREE_ITEM_MS;
    }

    /**
     * Dessine les items autour du centre à l'échelle du pop — chaque
     * pastille y suit sa cascade : montée de 5 dp, alpha à son crû.
     */
    private void dessinerItems(Canvas canvas, float echelle, float alpha) {
        float centreX = contentWidth() / 2f;
        float centreY = hauteurPx / 2f;
        canvas.save();
        canvas.translate(centreX, centreY);
        canvas.scale(echelle, echelle);
        canvas.translate(-centreX, -centreY);
        for (int i = 0; i < actions.size(); i++) {
            dessinerPastille(canvas, i, alpha * cascadeDe(i));
        }
        float xIcones = debutIcones();
        if (docsVisible()) {
            dessinerIconeDocs(canvas, xIcones,
                    alpha * cascadeDe(actions.size()));
        }
        if (aDebordement()) {
            dessinerPuce(canvas, xIcones + (docsVisible()
                    ? iconePx + ecartPx : 0f),
                    alpha * cascadeDe(actions.size() + (docsVisible() ? 1 : 0)));
        }
        if (!actions.isEmpty() && (docsVisible() || aDebordement())) {
            dessinerFilet(canvas);
        }
        canvas.restore();
    }

    /** L'avancement de la cascade de l'item donné, dans [0, 1]. */
    private float cascadeDe(int index) {
        if (entreeDepart < 0L) {
            return 1f;
        }
        float ecoule = getDrawingTime() - entreeDepart - index * DECAL_ITEM_MS;
        return Math.max(0f, Math.min(1f, ecoule / (float) DUREE_ITEM_MS));
    }

    /** Une pastille texte : sa bande pressée, son libellé, sa cascade. */
    private void dessinerPastille(Canvas canvas, int index, float avancee) {
        float[] borne = bornes.get(index);
        String label = actions.get(index).label();
        float dy = (1f - avancee) * monteePx;
        if (pressee == index) {
            dessinerBandePressee(canvas, borne[0], borne[1], dy);
        }
        textPaint().setColor(enAlpha(palette.text(), avancee));
        canvas.drawText(label, (borne[0] + borne[1]) / 2f
                - textPaint().measureText(label) / 2f,
                baseline() + dy, textPaint());
    }

    /** Le « i » de documentation : cercle cerclé, lettre au centre. */
    private void dessinerIconeDocs(Canvas canvas, float gauche, float avancee) {
        float dy = (1f - avancee) * monteePx;
        if (pressee == ICONE_DOCS) {
            dessinerBandePressee(canvas, gauche, gauche + iconePx, dy);
        }
        float cx = gauche + iconePx / 2f;
        float cy = hauteurPx / 2f + dy;
        float densite = getResources().getDisplayMetrics().density;
        linePaint().setColor(enAlpha(palette.text(), avancee));
        linePaint().setStrokeWidth(1.2f * densite);
        canvas.drawCircle(cx, cy, 7f * densite, linePaint());
        textPaint().setColor(enAlpha(palette.text(), avancee));
        float taille = textPaint().getTextSize();
        textPaint().setTextSize(11f * densite);
        canvas.drawText("i", cx - textPaint().measureText("i") / 2f,
                cy + textPaint().measureText("A") * 0.35f, textPaint());
        textPaint().setTextSize(taille);
    }

    /** La puce de débordement : trois points à l'encre du texte. */
    private void dessinerPuce(Canvas canvas, float gauche, float avancee) {
        float dy = (1f - avancee) * monteePx;
        if (pressee == PUCE_DEBORDEMENT) {
            dessinerBandePressee(canvas, gauche, gauche + iconePx, dy);
        }
        bandPaint().setColor(enAlpha(palette.text(), avancee));
        float densite = getResources().getDisplayMetrics().density;
        float cx = gauche + iconePx / 2f;
        for (int i = 0; i < 3; i++) {
            float cy = hauteurPx / 2f + dy
                    + (i - 1) * 3.5f * densite;
            canvas.drawCircle(cx, cy, 1.2f * densite, bandPaint());
        }
    }

    /** La bande de la pastille pressée — l'accent au voile doux. */
    private void dessinerBandePressee(Canvas canvas, float gauche, float fin,
            float dy) {
        float densite = getResources().getDisplayMetrics().density;
        float rayon = PopupTokens.RAYON_RANGEE_DP * densite;
        bandPaint().setColor((palette.accent() & 0x00FFFFFF) | 0x38000000);
        canvas.drawRoundRect(gauche, dy + 2f * densite, fin,
                hauteurPx + dy - 2f * densite, rayon, rayon, bandPaint());
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
            float largeur = (docsVisible() ? iconePx : 0f)
                    + (aDebordement()
                            ? (docsVisible() ? ecartPx : 0f) + iconePx : 0f);
            return Math.round(largeur);
        }
        float largeur = bornes.get(bornes.size() - 1)[1];
        if (docsVisible() || aDebordement()) {
            largeur += ecartPx + filetPx;
            if (docsVisible()) {
                largeur += iconePx;
            }
            if (aDebordement()) {
                largeur += (docsVisible() ? ecartPx : 0f) + iconePx;
            }
        }
        return Math.round(largeur);
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

    /** La couleur à l'avancée donnée — son alpha y prend sa part. */
    private static int enAlpha(int couleur, float avancee) {
        int alpha = Math.round((couleur >>> 24) * Math.max(0f, avancee));
        return (couleur & 0x00FFFFFF) | (alpha << 24);
    }
}
