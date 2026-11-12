package jo.celeris.android.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import org.jspecify.annotations.Nullable;

/**
 * Surface de dessin sur canvas Android : l'adaptateur concret de
 * {@link DrawSurface}.
 *
 * <p>PIEGE-15 : la surface est possédée par la vue, attachée au canvas à
 * chaque image et détachée après — jamais allouée dans la boucle de
 * dessin. Seuls les pinceaux de ce module y sont admis.</p>
 *
 * <p>La discipline ★ du style de peinture : le cercle creux bascule la
 * peinture en STROKE puis la REPOSE en FILL avant de rendre la main —
 * aucune trace du contour ne fuit vers le tracé suivant ; le triangle
 * se sert d'un chemin réutilisé, réécrit à chaque tracé.</p>
 */
public final class CanvasSurface implements DrawSurface {

    private @Nullable Canvas canvas;
    /** Le chemin du triangle — réutilisé, jamais alloué par image (PIEGE-15). */
    private final Path chemin = new Path();

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

    @Override
    public void drawCircle(float cx, float cy, float rayon, boolean plein,
            TextBrush brush) {
        Paint peinture = peintureDe(brush);
        if (!plein) {
            peinture.setStyle(Paint.Style.STROKE);
        }
        canvas().drawCircle(cx, cy, Math.max(0.5f, rayon), peinture);
        if (!plein) {
            // ★ la peinture retrouve son FILL — le contour ne fuit
            // jamais vers le tracé suivant
            peinture.setStyle(Paint.Style.FILL);
        }
    }

    @Override
    public void drawTriangle(float centreX, float centreY, float rayon,
            boolean pointeEnBas, TextBrush brush) {
        float etendue = Math.max(0.5f, rayon);
        chemin.rewind();
        if (pointeEnBas) {
            // ▾ — la pointe regarde en bas : la région s'ouvre
            chemin.moveTo(centreX - etendue, centreY - etendue * 0.6f);
            chemin.lineTo(centreX + etendue, centreY - etendue * 0.6f);
            chemin.lineTo(centreX, centreY + etendue * 0.7f);
        } else {
            // ▸ — la pointe regarde à droite : la région vit repliée
            chemin.moveTo(centreX - etendue * 0.6f, centreY - etendue);
            chemin.lineTo(centreX + etendue * 0.7f, centreY);
            chemin.lineTo(centreX - etendue * 0.6f, centreY + etendue);
        }
        chemin.close();
        canvas().drawPath(chemin, peintureDe(brush));
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
