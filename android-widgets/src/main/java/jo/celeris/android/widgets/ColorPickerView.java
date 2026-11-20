package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import jo.celeris.android.widgets.internal.PopupPanel;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * Le sélecteur de couleur (le signal du propriétaire du 2026-11-13 :
 * « ajouter aussi le double clic dessus pour afficher le popups
 * couleur picker ») : la carte de verre du kit, au design du dépôt de
 * départ — en-tête des deux pastilles (l'ORIGINALE et la COURANTE,
 * leurs littéraux hex vivants), champ saturation-valeur, bande de
 * teinte, palette de seize teintes Material. Le geste y vit : le drag
 * du champ et de la bande suit le doigt, l'aperçu courant respire à
 * chaque cran, le tap d'une cellule APPLIQUE — pas de boutons
 * Appliquer ni Annuler, l'application est implicite (le motif du dépôt
 * de départ), l'annulation est le tap hors carte.
 *
 * <p>La carte ne touche à AUCUN document : le rappel
 * {@link #setOnAppliquer} rend la teinte courante (RGB opaque) —
 * l'HÔTE compose avec l'alpha d'origine, formate à la largeur du
 * littéral et remplace en place ({@link #formater} sert la composition
 * du littéral : la largeur tient, le code environnant ne bouge pas).
 * PIEGE-27 par construction : la carte ne prend jamais le focus.</p>
 */
public final class ColorPickerView extends PopupPanel {

    /** La hauteur de l'en-tête des deux pastilles, en dp. */
    private static final float ENTETE_DP = 34f;

    /** La hauteur du champ saturation-valeur, en dp. */
    private static final float CHAMP_SV_DP = 112f;

    /** La hauteur de la bande de teinte, en dp. */
    private static final float BAND_TINT_DP = 18f;

    /** Le côté d'une cellule de palette, en dp. */
    private static final float CELLULE_DP = 22f;

    /** L'écart entre deux cellules, en dp. */
    private static final float CELL_GAP_DP = 6f;

    /** L'écart entre deux blocs voisins, en dp. */
    private static final float BLOCK_GAP_DP = 8f;

    /** Le côté d'une pastille d'en-tête, en dp. */
    private static final float HEADER_CHIP_DP = 22f;

    /** Le rayon d'une pastille ou cellule, en dp. */
    private static final float RAYON_PASTILLE_DP = 4f;

    /** La palette Material du dépôt de départ : deux rangées de huit. */
    private static final int[] PALETTE = {
            0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
            0xFF3F51B5, 0xFF2196F3, 0xFF009688, 0xFF4CAF50,
            0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFF795548,
            0xFF9E9E9E, 0xFF607D8B, 0xFF000000, 0xFFFFFFFF, };

    private final float densite;
    private final Paint pinceauBande = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinceauSpectre = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinceauTrait = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinceauCurseur = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinceauTeinte = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinceauVoile = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF coins = new RectF();

    /** La couleur d'origine du littéral — son alpha survit au pick. */
    private int originale;
    /** La largeur du littéral d'origine (ses chiffres, sans le dièse). */
    private int digits;
    /** L'état HSV courant — la teinte, la saturation, la valeur. */
    private final float[] hsv = {0f, 0f, 0f};
    /** Le rappel d'application — la teinte courante y part, opaque. */
    private @Nullable Consumer<Integer> surAppliquer;
    /** La zone pressée : 0 aucune, 1 le champ SV, 2 la bande de teinte. */
    private int zonePressee;
    /** Les dégradés du champ, rebâtis quand la teinte ou le rect bouge. */
    private @Nullable LinearGradient degradeTeinte;
    private @Nullable LinearGradient degradeNoir;

    public ColorPickerView(Context context, PopupPalette palette) {
        super(context, palette);
        this.densite = getResources().getDisplayMetrics().density;
        pinceauTrait.setStyle(Paint.Style.STROKE);
        pinceauCurseur.setStyle(Paint.Style.STROKE);
    }

    /**
     * Ouvre le sélecteur sur un littéral : sa couleur d'origine (son
     * alpha survivra au pick, affiché compris) et sa largeur en
     * chiffres.
     *
     * @param originale la couleur ARGB du littéral d'origine
     * @param digits  la largeur du littéral, en chiffres sans dièse
     */
    public void open(int originale, int digits) {
        this.originale = originale;
        this.digits = digits;
        Color.colorToHSV(originale & 0xFFFFFF, hsv);
        degradeTeinte = null;
        degradeNoir = null;
        invalidate();
    }

    /**
     * Pose (ou coupe) le rappel d'application : la teinte courante y
     * part, opaque — l'hôte compose l'alpha d'origine et remplace le
     * littéral en place.
     *
     * @param rappel le consommateur de teinte courante, null pour couper
     */
    public void setOnAppliquer(@Nullable Consumer<Integer> rappel) {
        surAppliquer = rappel;
    }

    /**
     * Le littéral formaté d'une couleur — la largeur du littéral
     * d'origine tient (ses chiffres), la forme courte quantifie chaque
     * composante au nibble le plus proche, la sortie reste en
     * minuscules : le pick remplace le littéral en place, le code
     * environnant ne bouge pas.
     *
     * @param argb     la couleur au format ARGB
     * @param digits la largeur voulue du littéral (sans le dièse)
     * @return le littéral formaté, dièse compris
     */
    public static String formater(int argb, int digits) {
        return switch (digits) {
            case 3 -> String.format("#%1$x%2$x%3$x",
                    quantifie((argb >>> 16) & 0xFF),
                    quantifie((argb >>> 8) & 0xFF),
                    quantifie(argb & 0xFF));
            case 4 -> String.format("#%1$x%2$x%3$x%4$x",
                    quantifie((argb >>> 24) & 0xFF),
                    quantifie((argb >>> 16) & 0xFF),
                    quantifie((argb >>> 8) & 0xFF),
                    quantifie(argb & 0xFF));
            case 6 -> String.format("#%06x", argb & 0xFFFFFF);
            default -> String.format("#%08x", argb);
        };
    }

    /** La composante quantifiée au nibble le plus proche. */
    private static int quantifie(int composante) {
        return Math.round(composante * 15f / 255f);
    }

    /** La teinte courante, opaque — le pick de l'hôte. */
    int courante() {
        return compose(0xFF);
    }

    /** La teinte courante composée à l'alpha donné. */
    private int compose(int alpha) {
        return (alpha << 24) | (Color.HSVToColor(hsv) & 0xFFFFFF);
    }

    @Override
    public void drawContent(Canvas canvas) {
        float width = contentWidth();
        drawHeader(canvas, width);
        float yChamp = ENTETE_DP * densite + BLOCK_GAP_DP * densite;
        drawField(canvas, width, yChamp);
        float yBande = yChamp + CHAMP_SV_DP * densite + BLOCK_GAP_DP * densite;
        drawBand(canvas, width, yBande);
        drawPalette(canvas, yBande + BAND_TINT_DP * densite
                + BLOCK_GAP_DP * densite);
    }

    /** L'en-tête : la pastille d'origine et son littéral, la courante
     *  et le sien — l'aperçu avant/après vit à chaque cran du drag. */
    private void drawHeader(Canvas canvas, float width) {
        float cote = HEADER_CHIP_DP * densite;
        float rayon = RAYON_PASTILLE_DP * densite;
        float centre = ENTETE_DP * densite / 2f;
        // la pastille d'origine, à gauche
        pinceauBande.setColor(originale);
        coins.set(0f, centre - cote / 2f, cote, centre + cote / 2f);
        canvas.drawRoundRect(coins, rayon, rayon, pinceauBande);
        traceTrim(canvas, coins, rayon);
        textPaint().setColor(palette.text());
        canvas.drawText(formater(originale, digits),
                cote + BLOCK_GAP_DP * densite, baseline(centre), textPaint());
        // la pastille courante, au bord droit
        int courante = compose(originale >>> 24);
        String label = formater(courante, digits);
        float largeurLabel = textPaint().measureText(label);
        pinceauBande.setColor(courante);
        coins.set(width - cote, centre - cote / 2f, width, centre + cote / 2f);
        canvas.drawRoundRect(coins, rayon, rayon, pinceauBande);
        traceTrim(canvas, coins, rayon);
        canvas.drawText(label, width - cote - BLOCK_GAP_DP * densite
                - largeurLabel, baseline(centre), textPaint());
    }

    /** Le champ saturation-valeur : blanc vers la teinte, transparent
     *  vers le noir — le marqueur y vit, double cercle lisible sur tout
     *  fond (le motif du dépôt de départ). */
    private void drawField(Canvas canvas, float width, float y) {
        float height = CHAMP_SV_DP * densite;
        LinearGradient teinte = degradeTeinte;
        LinearGradient noir = degradeNoir;
        if (teinte == null || noir == null) {
            int teintePure = Color.HSVToColor(new float[] {hsv[0], 1f, 1f});
            teinte = new LinearGradient(0f, 0f, width, 0f,
                    Color.WHITE, teintePure, Shader.TileMode.CLAMP);
            noir = new LinearGradient(0f, 0f, 0f, height,
                    Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP);
            degradeTeinte = teinte;
            degradeNoir = noir;
        }
        pinceauTeinte.setShader(teinte);
        canvas.drawRect(0f, y, width, y + height, pinceauTeinte);
        pinceauVoile.setShader(noir);
        canvas.drawRect(0f, y, width, y + height, pinceauVoile);
        traceTrimRect(canvas, 0f, y, width, y + height);
        // le marqueur : double cercle, sombre dehors, blanc dedans
        float mx = hsv[1] * width;
        float my = (1f - hsv[2]) * height;
        float rayon = 6f * densite;
        pinceauCurseur.setColor(0xFF1F1F1F);
        pinceauCurseur.setStrokeWidth(2f * densite);
        canvas.drawCircle(mx, y + my, rayon, pinceauCurseur);
        pinceauCurseur.setColor(Color.WHITE);
        pinceauCurseur.setStrokeWidth(1.5f * densite);
        canvas.drawCircle(mx, y + my, rayon - 1.5f * densite, pinceauCurseur);
    }

    /** La bande de teinte : le spectre, curseur à deux traits qui
     *  dépasse de la bande — sombre et blanc superposés. */
    private void drawBand(Canvas canvas, float width, float y) {
        float height = BAND_TINT_DP * densite;
        if (pinceauSpectre.getShader() == null) {
            pinceauSpectre.setShader(new LinearGradient(0f, 0f, width, 0f,
                    new int[] {Color.RED, Color.YELLOW, Color.GREEN,
                            Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED},
                    new float[] {0f, 1f / 6f, 2f / 6f, 3f / 6f, 4f / 6f,
                            5f / 6f, 1f},
                    Shader.TileMode.CLAMP));
        }
        canvas.save();
        canvas.translate(0f, y);
        canvas.drawRect(0f, 0f, width, height, pinceauSpectre);
        canvas.restore();
        traceTrimRect(canvas, 0f, y, width, y + height);
        float x = hsv[0] / 360f * width;
        float depasse = 2f * densite;
        pinceauCurseur.setColor(0xFF1F1F1F);
        pinceauCurseur.setStrokeWidth(2f);
        canvas.drawLine(x, y - depasse, x, y + height + depasse, pinceauCurseur);
        pinceauCurseur.setColor(Color.WHITE);
        pinceauCurseur.setStrokeWidth(1f);
        canvas.drawLine(x, y - depasse, x, y + height + depasse, pinceauCurseur);
    }

    /** La palette : seize teintes Material, la teinte portée liserée
     *  à l'accent — le tap d'une cellule applique. */
    private void drawPalette(Canvas canvas, float y) {
        float cote = CELLULE_DP * densite;
        float gap = CELL_GAP_DP * densite;
        float rayon = RAYON_PASTILLE_DP * densite;
        int courante = compose(0xFF) & 0xFFFFFF;
        for (int i = 0; i < PALETTE.length; i++) {
            float x = (i % 8) * (cote + gap);
            float cy = y + (i / 8) * (cote + gap);
            pinceauBande.setColor(PALETTE[i]);
            coins.set(x, cy, x + cote, cy + cote);
            canvas.drawRoundRect(coins, rayon, rayon, pinceauBande);
            boolean portee = (PALETTE[i] & 0xFFFFFF) == courante;
            pinceauTrait.setColor(portee ? palette.accent() : palette.border());
            pinceauTrait.setStrokeWidth((portee ? 2f : 1f) * densite);
            canvas.drawRoundRect(coins, rayon, rayon, pinceauTrait);
        }
    }

    @Override
    public int contentWidth() {
        return Math.round(8 * CELLULE_DP * densite + 7 * CELL_GAP_DP * densite);
    }

    @Override
    public int contentHeight() {
        return Math.round((ENTETE_DP + CHAMP_SV_DP + BAND_TINT_DP
                + 2 * CELLULE_DP + CELL_GAP_DP + 3 * BLOCK_GAP_DP)
                * densite);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() - paddingPx;
        float y = event.getY() - paddingYPx;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                zonePressee = zoneDe(x, y);
                if (zonePressee != 0) {
                    presser(x, y);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (zonePressee != 0) {
                    presser(x, y);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                int cellule = celluleDe(x, y);
                if (cellule >= 0) {
                    Color.colorToHSV(PALETTE[cellule], hsv);
                    appliquer();
                } else if (zonePressee != 0) {
                    appliquer();
                }
                zonePressee = 0;
                invalidate();
                performClick();
                return true;
            default:
                return false;
        }
    }

    /** La zone du point : 1 le champ, 2 la bande, 0 ailleurs dans la carte. */
    private int zoneDe(float x, float y) {
        float champY = (ENTETE_DP + BLOCK_GAP_DP) * densite;
        float bandeY = champY + CHAMP_SV_DP * densite + BLOCK_GAP_DP * densite;
        if (y >= champY && y < champY + CHAMP_SV_DP * densite) {
            return 1;
        }
        if (y >= bandeY && y < bandeY + BAND_TINT_DP * densite) {
            return 2;
        }
        return 0;
    }

    /** La cellule de palette du point, —1 hors palette. */
    int celluleDe(float x, float y) {
        float cote = CELLULE_DP * densite;
        float gap = CELL_GAP_DP * densite;
        float paletteY = (ENTETE_DP + BLOCK_GAP_DP + CHAMP_SV_DP
                + BLOCK_GAP_DP + BAND_TINT_DP + BLOCK_GAP_DP) * densite;
        if (y < paletteY || x < 0f || x > contentWidth()) {
            return -1;
        }
        int column = (int) (x / (cote + gap));
        int rangee = (int) ((y - paletteY) / (cote + gap));
        if (column > 7 || rangee > 1
                || x > column * (cote + gap) + cote
                || y > paletteY + rangee * (cote + gap) + cote) {
            return -1;
        }
        return rangee * 8 + column;
    }

    /** Le point pressé suit sa zone : saturation-valeur au champ,
     *  teinte à la bande — bornés au rect. */
    private void presser(float x, float y) {
        float width = contentWidth();
        if (zonePressee == 1) {
            hsv[1] = borne(x / width);
            hsv[2] = borne(1f - (y - (ENTETE_DP + BLOCK_GAP_DP) * densite)
                    / (CHAMP_SV_DP * densite));
        } else if (zonePressee == 2) {
            hsv[0] = borne(x / width) * 360f;
        }
        degradeTeinte = null;
        degradeNoir = null;
    }

    /** L'application implicite : la teinte courante part à l'hôte. */
    private void appliquer() {
        Consumer<Integer> rappel = surAppliquer;
        if (rappel != null) {
            rappel.accept(compose(0xFF));
        }
    }

    /** La valeur bornée à [0, 1]. */
    private static float borne(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    /** La ligne de base d'un texte centré verticalement. */
    private float baseline(float centre) {
        return centre + textPaint().measureText("A") * 0.35f;
    }

    /** Le liseré d'une pastille. */
    private void traceTrim(Canvas canvas, RectF rect, float rayon) {
        pinceauTrait.setColor(palette.border());
        pinceauTrait.setStrokeWidth(1f);
        canvas.drawRoundRect(rect, rayon, rayon, pinceauTrait);
    }

    /** Le liseré d'un rect droit. */
    private void traceTrimRect(Canvas canvas, float gauche, float haut,
            float droite, float basse) {
        pinceauTrait.setColor(palette.border());
        pinceauTrait.setStrokeWidth(1f);
        canvas.drawRect(gauche, haut, droite, basse, pinceauTrait);
    }
}
