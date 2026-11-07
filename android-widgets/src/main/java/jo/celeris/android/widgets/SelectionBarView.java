package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;


/**
 * La barre de sélection (M7.5, §7, reprise) : les actions de la
 * sélection en pastilles horizontales — couper, copier, coller, tout
 * sélectionner — centrée AU-DESSUS de la sélection, l'appui exécute.
 * Cède à la frappe (la frappe remplace la sélection), survit au
 * défilement ; jamais de focus (PIEGE-27), le clavier reste à
 * l'éditeur.
 */
public final class SelectionBarView extends PopupPanel {

    /** Une action de la barre : son libellé et ce qu'elle fait. */
    public record Item(String label, Runnable action) {
    }

    /** La respiration entre pastilles, en dp. */
    private static final float ECART_DP = 10f;

    private final List<Item> actions = new ArrayList<>();
    private final List<Float> finsDePuce = new ArrayList<>();
    private final int ecartPx;

    public SelectionBarView(Context context, PopupPalette palette) {
        super(context, palette);
        this.ecartPx = Math.round(ECART_DP
                * getResources().getDisplayMetrics().density);
    }

    /**
     * Pose les actions de la barre.
     *
     * @param items les actions, dans l'ordre d'affichage
     */
    public void setActions(List<Item> items) {
        actions.clear();
        actions.addAll(items);
        finsDePuce.clear();
        float curseur = 0f;
        for (Item item : items) {
            curseur += textPaint().measureText(item.label());
            finsDePuce.add(curseur);
            curseur += ecartPx;
        }
        requestLayout();
        invalidate();
    }

    @Override
    void drawContent(Canvas canvas) {
        float ligne = -textPaint().getFontMetrics().top;
        float x = 0f;
        for (int i = 0; i < actions.size(); i++) {
            textPaint().setColor(palette.text());
            canvas.drawText(actions.get(i).label(), x, ligne, textPaint());
            x = finsDePuce.get(i) + ecartPx;
            if (i < actions.size() - 1) {
                textPaint().setColor(palette.border());
                canvas.drawRect(x - ecartPx / 2f, ligne - rowHeightPx,
                        x - ecartPx / 2f + 1f, ligne, textPaint());
            }
        }
    }

    @Override
    int contentWidth() {
        if (finsDePuce.isEmpty()) {
            return 0;
        }
        return Math.round(finsDePuce.get(finsDePuce.size() - 1));
    }

    @Override
    int contentHeight() {
        return actions.isEmpty() ? 0 : rowHeightPx;
    }

    /** L'ordonnée du centre du contenu — les bancs y visent. */
    float centerY() {
        return paddingPx + rowHeightPx / 2f;
    }

    /** L'index de la pastille visée par l'abscisse, —1 si hors barre. */
    private int chipAt(float x) {
        for (int i = 0; i < finsDePuce.size(); i++) {
            if (x <= finsDePuce.get(i)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) {
            return event.getActionMasked() == MotionEvent.ACTION_DOWN;
        }
        int index = chipAt(event.getX() - paddingPx);
        if (index >= 0 && index < actions.size()) {
            actions.get(index).action().run();
            performClick();
            return true;
        }
        return false;
    }
}
