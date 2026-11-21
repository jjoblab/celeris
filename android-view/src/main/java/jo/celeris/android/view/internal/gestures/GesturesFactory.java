package jo.celeris.android.view.internal.gestures;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import jo.celeris.android.view.internal.access.EditorAccess;

/**
 * Fabrique des détecteurs de gestes de la vue d'éditeur : la
 * construction des écouteurs anonymes quitte le constructeur de la
 * vue, qui gagne en lisibilité — le comportement est inchangé, la vue
 * reste la seule propriétaire de l'état (appui long : sélection du
 * mot sous le doigt, clavier retiré ; pincement : zoom par paliers
 * D-15, point focal stable).
 */
public final class GesturesFactory {

    private GesturesFactory() {
    }

    /**
     * Le détecteur d'appuis longs (M7.5, redressement) : le mot sous
     * le doigt se sélectionne, le clavier se retire, le dragage
     * étend — la barre de sélection du dépôt de départ s'offre
     * au-dessus du mot (le rectangle de la pilule s'est retiré, le
     * redressement du 2026-11-13).
     *
     * @param context le contexte de la vue
     * @param vue     la vue propriétaire des gestes
     * @return le détecteur branché
     */
    public static GestureDetector appuisLongs(Context context, EditorAccess acces) {
        return new GestureDetector(context, new GestureDetector
                .SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent event) {
                acces.touches().surAppuiLong(event.getX(), event.getY());
            }
        });
    }

    /**
     * Le détecteur de pincements : zoome par paliers, le point sous le
     * doigt stable (D-15) — le facteur cumulé se reconstitue depuis
     * l'écartement de départ (PIEGE-24) ; le panoramique à deux doigts
     * vit dans la vue, qui suit déjà les pointeurs.
     *
     * @param context le contexte de la vue
     * @param vue     la vue propriétaire du zoom
     * @return le détecteur branché
     */
    public static ScaleGestureDetector pincements(Context context, EditorAccess acces) {
        return new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                acces.zoom().surEngagement(detector);
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                acces.zoom().surCran(detector);
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                acces.zoom().surFin();
            }
        });
    }
}
