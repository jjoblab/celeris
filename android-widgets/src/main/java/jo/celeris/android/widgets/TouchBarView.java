package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import jo.celeris.android.widgets.internal.PopupPanel;

import java.util.ArrayList;
import java.util.List;

import java.util.function.BooleanSupplier;

import org.jspecify.annotations.Nullable;

/**
 * La barre de touches de code (le dépôt de départ, 4.5.0 — portée au
 * kit du verre) : une rangée de touches configurables AU-DESSUS DU
 * CLAVIER — Tab, commentaire, déplacements de lignes, duplication… ou
 * les actions propres de l'hôte. Chaque touche : cible tactile de
 * 44 dp, coins arrondis du kit, fond du verre de l'hôte, encre du
 * texte ; la touche INACTIVE vit à l'encre atténuée et rejette le
 * doigt (l'état activé se re-évalue au geste du caret). L'écart
 * entre touches tient à six dp, la respiration horizontale du
 * panneau aux jetons.
 *
 * <p>Petite sœur assumée de la barre de sélection : interaction
 * tactile BRUTE — la barre ne prend JAMAIS le focus (PIEGE-27), le
 * caret reste dans l'éditeur et l'IME reste ouvert — et {@code
 * performClick()} facteur commun du chemin tactile et de TalkBack.
 * Le choix d'une touche l'exécute APRÈS le performClick : le chemin
 * de l'accessibilité et celui du doigt ne font qu'un.</p>
 *
 * <p>La barre n'est JAMAIS posée par la bibliothèque — l'hôte la pose
 * au bas de sa scène (le {@code adjustResize} de l'activité la
 * remonte avec l'IME). Elle vit avec le document : ni la frappe ni le
 * défilement ne la ferment (le genre TOUCH_BAR de la machine).</p>
 */
public final class TouchBarView extends PopupPanel {

    /** Une touche de la barre : son libellé, son état activé (null =
     *  toujours active), son action au doigt. */
    public record KeyCap(String label,
            @Nullable BooleanSupplier active, Runnable action) {
    }

    /** La hauteur de la rangée, en dp — celle du dépôt de départ. */
    private static final float HEIGHT_DP = 44f;

    /** La cible tactile minimale d'une touche, en dp. */
    private static final float CIBLE_DP = 44f;

    /** L'écart entre deux touches, en dp. */
    private static final float GAP_DP = 6f;

    /** La respiration horizontale d'une touche, en dp. */
    private static final float RESPIRATION_TOUCHE_DP = 14f;

    /** Le côté minimal d'une touche — la cible tactile tient même au
     *  libellé court. */
    private static final float COTE_MIN_DP = 44f;

    private final List<KeyCap> touches = new ArrayList<>();
    private final List<Float> finsDeTouche = new ArrayList<>();
    /** La touche sous le doigt, —1 hors appui. */
    private int pressee = -1;
    private final float hauteurPx;
    private final float gapPx;
    private final float respirationTouchePx;
    private final float coteMinPx;

    public TouchBarView(Context context, PopupPalette palette) {
        super(context, palette);
        float densite = getResources().getDisplayMetrics().density;
        this.hauteurPx = HEIGHT_DP * densite;
        this.gapPx = GAP_DP * densite;
        this.respirationTouchePx = RESPIRATION_TOUCHE_DP * densite;
        this.coteMinPx = COTE_MIN_DP * densite;
    }

    /**
     * Remplace la rangée de touches — le point d'extension hôte :
     * chaque touche apporte son libellé, son état activé et son action
     * au doigt. Une liste vide vide la barre.
     *
     * @param nouvelles les touches, dans l'ordre d'affichage
     */
    public void setActions(List<KeyCap> nouvelles) {
        touches.clear();
        finsDeTouche.clear();
        for (KeyCap touche : nouvelles) {
            if (touche != null) {
                touches.add(touche);
            }
        }
        textPaint().setTextSize(textSize(PopupTokens.TEXTE_TITRE));
        float cursor = 0f;
        for (KeyCap touche : touches) {
            float largeurTexte = textPaint().measureText(touche.label());
            float width = Math.max(coteMinPx,
                    largeurTexte + 2f * respirationTouchePx);
            finsDeTouche.add(cursor + width);
            cursor += width + gapPx;
        }
        requestLayout();
        invalidate();
    }

    /** Ré-évalue les états activés des touches — l'hôte l'appelle au
     *  geste du caret ; le dessin suit l'état au moment de la frame. */
    public void refreshEnabled() {
        invalidate();
    }

    /** Vrai quand la touche visée vit à son état activé — la touche
     *  muette rejette le doigt. */
    private boolean active(int index) {
        KeyCap touche = touches.get(index);
        return touche.active() == null || touche.active().getAsBoolean();
    }

    @Override
    public void drawContent(Canvas canvas) {
        textPaint().setTextSize(textSize(PopupTokens.TEXTE_TITRE));
        Paint.FontMetrics metriques = textPaint().getFontMetrics();
        // la baseline centrée : la moitié de la rangée moins la moitié
        // de l'étendue du texte — la touche vit toute la hauteur
        float baseline = (hauteurPx - (metriques.descent - metriques.ascent))
                * 0.5f - metriques.ascent;
        for (int i = 0; i < touches.size(); i++) {
            float start = debutDe(i);
            float end = finsDeTouche.get(i);
            drawKey(canvas, i, start, end, baseline);
        }
    }

    /**
     * Une touche : pilule au rayon du kit — fond teinté à l'appui (la
     * teinte de sélection), sinon le verre du panneau ; l'encre du
     * texte à l'état actif, l'encre atténuée à l'état muet (l'équivalent
     * maison de l'alpha 0,4 du dépôt de départ).
     */
    private void drawKey(Canvas canvas, int index, float start,
            float end, float baseline) {
        float rayon = PopupTokens.RAYON_RANGEE_DP
                * getResources().getDisplayMetrics().density;
        boolean appuyee = index == pressee;
        bandPaint().setColor(appuyee ? palette.selection()
                : palette.border());
        // le fond des touches : le liseré lui-même teinté — la pilule
        // vit SUR le verre du panneau, l'appui la surligne
        bandPaint().setAlpha(appuyee ? 0xFF : 0x5A);
        canvas.drawRoundRect(start, 0f, end, hauteurPx,
                rayon, rayon, bandPaint());
        bandPaint().setAlpha(0xFF);
        boolean vivante = active(index);
        textPaint().setColor(vivante ? palette.text() : palette.muted());
        float demiTexte = textPaint().measureText(
                touches.get(index).label()) * 0.5f;
        float centre = (start + end) * 0.5f;
        canvas.drawText(touches.get(index).label(), centre - demiTexte,
                baseline, textPaint());
    }

    /** Le début d'une touche — la fin de la précédente, l'écart compris. */
    private float debutDe(int index) {
        return index == 0 ? 0f : finsDeTouche.get(index - 1) + gapPx;
    }

    @Override
    public int contentWidth() {
        return finsDeTouche.isEmpty() ? 0
                : Math.round(finsDeTouche.get(finsDeTouche.size() - 1));
    }

    @Override
    public int contentHeight() {
        return Math.round(hauteurPx);
    }

    /** L'index de la touche visée par l'abscisse, —1 si hors barre. */
    private int touches(float x) {
        for (int i = 0; i < finsDeTouche.size(); i++) {
            float start = debutDe(i);
            if (x >= start && x <= finsDeTouche.get(i)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            pressee = touches(event.getX() - paddingPx);
            invalidate();
            return pressee >= 0;
        }
        if (event.getActionMasked() != MotionEvent.ACTION_UP) {
            return false;
        }
        int index = touches(event.getX() - paddingPx);
        boolean appuyee = index >= 0 && index == pressee;
        pressee = -1;
        invalidate();
        if (appuyee) {
            // le chemin du doigt et celui de TalkBack ne font qu'un :
            // performClick choisit la touche
            toucheChoisie = index;
            performClick();
            toucheChoisie = -1;
            return true;
        }
        return false;
    }

    /** La touche choisie par performClick — le passage tactile. */
    private int toucheChoisie = -1;

    @Override
    public boolean performClick() {
        super.performClick();
        int index = toucheChoisie >= 0 ? toucheChoisie : -1;
        if (index < 0 || index >= touches.size() || !active(index)) {
            // la touche muette rejette le doigt — et le lecteur d'écran
            return false;
        }
        touches.get(index).action().run();
        return true;
    }
}
