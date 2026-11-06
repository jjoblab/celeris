package jo.celeris.android.view;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Fabrique des détecteurs de gestes de la vue d'éditeur : la
 * construction des écouteurs anonymes quitte le constructeur de la
 * vue, qui gagne en lisibilité — le comportement est inchangé, la vue
 * reste la seule propriétaire de l'état (appui long : pilule
 * multi-curseur A-01 ; pincement : zoom par paliers D-15, point focal
 * stable).
 */
final class GestesFabrique {

    private GestesFabrique() {
    }

    /**
     * Le détecteur d'appuis longs : arme la pilule « ajouter un
     * curseur » au point touché (A-01).
     *
     * @param context le contexte de la vue
     * @param vue     la vue propriétaire de la pilule
     * @return le détecteur branché
     */
    static GestureDetector appuisLongs(Context context, EditorView vue) {
        return new GestureDetector(context, new GestureDetector
                .SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent event) {
                vue.pilule.show(event.getX(), event.getY());
                vue.invalidate();
            }
        });
    }

    /**
     * Le détecteur de pincements : zoome par paliers, le point sous le
     * doigt stable (D-15) ; le panoramique à deux doigts vit dans la
     * vue, qui suit déjà les pointeurs.
     *
     * @param context le contexte de la vue
     * @param vue     la vue propriétaire du zoom
     * @return le détecteur branché
     */
    static ScaleGestureDetector pincements(Context context, EditorView vue) {
        return new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                vue.pincementActif = true;
                vue.tailleAuDepartDuPincement = vue.metrics.font().size();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                vue.zoomer(detector.getFocusX(), detector.getFocusY(),
                        detector.getScaleFactor());
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                vue.pincementActif = false;
            }
        });
    }
}
