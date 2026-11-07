package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * Le panneau à rangées de texte des popups (M7.5, §7, reprise) : les
 * contenus verticaux — complétion, documentation, signature,
 * intentions, info-bulle — partagent la même rangée : un texte, une
 * encre, une bande de surlignage, un tronçon souligné (le paramètre
 * courant de l'aide aux paramètres). L'appui sur une rangée prévient
 * l'hôte : la sélection d'un candidat ou d'une intention.
 */
abstract class TextRowsPopup extends PopupPanel {

    /**
     * Une rangée : son texte, son encre (—1 pour l'encre du panneau),
     * son tronçon souligné (début inclusif, fin exclusive, —1 pour
     * aucun).
     */
    protected record Row(String text, int ink, int underlineStart,
            int underlineEnd) {

        static Row of(String text) {
            return new Row(text, -1, -1, -1);
        }

        static Row of(String text, int ink) {
            return new Row(text, ink, -1, -1);
        }
    }

    /** La respiration entre rangées, en dp. */
    private static final float ECART_DP = 3f;

    private final List<Row> rows = new ArrayList<>();
    private final int ecartPx;
    /** La rangée surlignée (bande d'accent) — —1 pour aucune. */
    private int highlighted = -1;
    private @Nullable IntConsumer onPick;

    TextRowsPopup(Context context, PopupPalette palette) {
        super(context, palette);
        this.ecartPx = Math.round(ECART_DP
                * getResources().getDisplayMetrics().density);
    }

    /** Pose les rangées — le panneau se redimensionne et se redessine. */
    protected final void setRows(List<Row> nouvelles) {
        rows.clear();
        rows.addAll(nouvelles);
        requestLayout();
        invalidate();
    }

    /** La rangée surlignée (bande d'accent), —1 pour aucune. */
    protected final void setHighlighted(int index) {
        highlighted = index;
        invalidate();
    }

    /** Le rappel de l'appui sur une rangée — l'hôte exécute. */
    public final void setOnPick(@Nullable IntConsumer rappel) {
        onPick = rappel;
    }

    @Override
    void drawContent(Canvas canvas) {
        float ligne = -textPaint().getFontMetrics().top;
        Paint pinceau = textPaint();
        for (int i = 0; i < rows.size(); i++) {
            Row rangee = rows.get(i);
            if (i == highlighted) {
                pinceau.setColor(palette.accent());
                canvas.drawRect(-paddingPx, ligne - rowHeightPx,
                        contentWidth() + paddingPx, ligne + ecartPx, pinceau);
            }
            pinceau.setColor(rangee.ink() < 0 ? palette.text() : rangee.ink());
            canvas.drawText(rangee.text(), 0f, ligne, pinceau);
            if (rangee.underlineEnd() > rangee.underlineStart()) {
                drawUnderline(canvas, rangee, ligne);
            }
            ligne += rowHeightPx + ecartPx;
        }
    }

    private void drawUnderline(Canvas canvas, Row rangee, float ligne) {
        Paint pinceau = textPaint();
        pinceau.setColor(palette.accent());
        float debut = pinceau.measureText(rangee.text(), 0,
                Math.max(0, rangee.underlineStart()));
        float fin = pinceau.measureText(rangee.text(), 0,
                Math.min(rangee.text().length(), rangee.underlineEnd()));
        float bas = ligne + Math.round(pinceau.getFontMetrics().bottom * 0.4f);
        canvas.drawRect(debut, bas, fin, bas + Math.max(2f,
                pinceau.getStrokeWidth() + 1f), pinceau);
    }

    @Override
    int contentWidth() {
        float largeur = 0f;
        for (Row rangee : rows) {
            largeur = Math.max(largeur,
                    textPaint().measureText(rangee.text()));
        }
        return Math.round(largeur);
    }

    @Override
    int contentHeight() {
        if (rows.isEmpty()) {
            return 0;
        }
        return rows.size() * rowHeightPx + (rows.size() - 1) * ecartPx;
    }

    /** Le nombre de rangées courantes — lisibilité des sous-classes. */
    protected final int rowCount() {
        return rows.size();
    }

    /** L'ordonnée du CENTRE de la rangée donnée, respiration comprise
     *  — les bancs y visent leur appui. */
    final float rowCenterY(int index) {
        return paddingPx + index * (rowHeightPx + ecartPx)
                + rowHeightPx / 2f;
    }

    /** La rangée visée par l'ordonnée donnée, —1 si hors contenu. */
    private int rowAt(float y) {
        int pas = rowHeightPx + ecartPx;
        int index = (int) Math.floor(y / pas);
        return index >= 0 && index < rows.size() ? index : -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP || onPick == null) {
            return event.getActionMasked() == MotionEvent.ACTION_DOWN;
        }
        int index = rowAt(event.getY() - paddingPx);
        if (index >= 0) {
            onPick.accept(index);
            performClick();
            return true;
        }
        return false;
    }
}
