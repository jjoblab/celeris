package jo.celeris.android.view;

import android.graphics.Paint;

/**
 * Fabrique de pinceaux sur peintures Android : chaque couche y prend la
 * sienne à sa construction (PIEGE-01).
 */
public final class PaintBrushes implements BrushFactory {

    @Override
    public TextBrush newBrush() {
        Paint peinture = new Paint(Paint.ANTI_ALIAS_FLAG);
        peinture.setStyle(Paint.Style.FILL);
        return new PaintBrush(peinture);
    }
}
