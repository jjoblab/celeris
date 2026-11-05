package jo.celeris.android.view;

import android.graphics.Canvas;
import android.graphics.Paint;

import org.jspecify.annotations.Nullable;

/**
 * Surface de dessin sur canvas Android : l'adaptateur concret de
 * {@link DrawSurface}.
 *
 * <p>PIEGE-15 : la surface est possédée par la vue, attachée au canvas à
 * chaque image et détachée après — jamais allouée dans la boucle de
 * dessin. Seuls les pinceaux de ce module y sont admis.</p>
 */
public final class CanvasSurface implements DrawSurface {

    private @Nullable Canvas canvas;

    /** Attache la surface au canvas de l'image à venir. */
    public void attach(Canvas canvas) {
        this.canvas = canvas;
    }

    /** Détache la surface : tout tracé ultérieur est une erreur. */
    public void detach() {
        canvas = null;
    }

    @Override
    public void drawText(String text, float x, float y, TextBrush brush) {
        canvas().drawText(text, x, y, peintureDe(brush));
    }

    @Override
    public void drawText(CharSequence text, int start, int end, float x,
            float y, TextBrush brush) {
        canvas().drawText(text, start, end, x, y, peintureDe(brush));
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom,
            TextBrush brush) {
        canvas().drawRect(left, top, right, bottom, peintureDe(brush));
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, TextBrush brush) {
        canvas().drawLine(x1, y1, x2, y2, peintureDe(brush));
    }

    private Canvas canvas() {
        Canvas local = canvas;
        if (local == null) {
            throw new IllegalStateException("surface detached: attach() required per frame");
        }
        return local;
    }

    private static Paint peintureDe(TextBrush brush) {
        if (brush instanceof PaintBrush pinceau) {
            return pinceau.peinture();
        }
        throw new IllegalArgumentException(
                "brush is not a paint brush of this module: " + brush.getClass());
    }
}
