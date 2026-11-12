package jo.celeris.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * La couche porte-popups de l'éditeur (M7.5, §7) : un ViewGroup
 * superposé à la vue d'éditeur DANS LA MÊME FENÊTRE — jamais de
 * PopupWindow : pas de jeton de fenêtre, pas de conflit de focus avec
 * l'IME, testable sous Robolectric.
 *
 * <p>PIEGE-27, non négociable : la couche ne prend JAMAIS le focus
 * d'entrée — {@code setFocusable(false)} à la construction, aucun
 * {@code requestFocus}, et chaque popup montré se rend non focalisable
 * lui aussi. Le clavier logiciel et la connexion d'entrée restent à
 * l'éditeur ; les popups se dessinent et se touchent, c'est tout.</p>
 *
 * <p>Les popups sont des vues légères posées à des coordonnées de la
 * fenêtre ; l'hôte les repositionne au défilement, au zoom et à
 * l'apparition du clavier — la couche ne fait que les porter.</p>
 */
public final class PopupLayer extends ViewGroup {

    private final Map<PopupController.PopupKind, View> popups =
            new EnumMap<>(PopupController.PopupKind.class);

    public PopupLayer(Context context) {
        this(context, null);
    }

    public PopupLayer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // PIEGE-27 : la couche ne prend jamais le focus d'entrée — le
        // clavier reste à l'éditeur qu'elle recouvre.
        setFocusable(false);
        setFocusableInTouchMode(false);
    }

    /**
     * Montre le popup donné à la position donnée : la vue se rend non
     * focalisable (PIEGE-27), la couche se redessine.
     *
     * @param kind  le popup à montrer
     * @param popup la vue du popup
     * @param x     l'abscisse de la fenêtre, en pixels
     * @param y     l'ordonnée de la fenêtre, en pixels
     */
    public void show(PopupController.PopupKind kind, View popup, int x, int y) {
        View ancienne = popups.remove(kind);
        if (ancienne != null) {
            removeView(ancienne);
        }
        // PIEGE-27 : aucun popup ne prend le focus d'entrée.
        popup.setFocusable(false);
        popup.setFocusableInTouchMode(false);
        popups.put(kind, popup);
        addView(popup);
        popup.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        popup.layout(x, y, x + popup.getMeasuredWidth(),
                y + popup.getMeasuredHeight());
        invalidate();
    }

    /**
     * Repositionne le popup donné — défilement, zoom, clavier.
     *
     * @param kind le popup à déplacer
     * @param x    la nouvelle abscisse, en pixels
     * @param y    la nouvelle ordonnée, en pixels
     */
    public void reposition(PopupController.PopupKind kind, int x, int y) {
        View popup = popups.get(kind);
        if (popup != null) {
            popup.layout(x, y, x + popup.getWidth(), y + popup.getHeight());
            invalidate();
        }
    }

    /**
     * Re-monte le popup DÉJÀ montré à la position donnée : même vue,
     * même instance — le contenu se re-mesure (il peut avoir changé)
     * et se re-pose, sans jamais quitter la couche. Le suivi au doigt
     * (le dragage de sélection qui déplace la barre image après
     * image) s'y vit d'un seul tenant : le popup ne re-naît pas à
     * chaque image, il SE DÉPLACE — ce que l'appui long a posé, le
     * dragage le garde identique.
     *
     * @param kind le popup déjà montré à re-positionner
     * @param x    la nouvelle abscisse, en pixels
     * @param y    la nouvelle ordonnée, en pixels
     */
    public void remonter(PopupController.PopupKind kind, int x, int y) {
        View popup = popups.get(kind);
        if (popup == null) {
            return;
        }
        popup.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        popup.layout(x, y, x + popup.getMeasuredWidth(),
                y + popup.getMeasuredHeight());
        invalidate();
    }

    /**
     * Ferme le popup donné.
     *
     * @param kind le popup à fermer
     * @return vrai si un popup se fermait
     */
    public boolean dismiss(PopupController.PopupKind kind) {
        View popup = popups.remove(kind);
        if (popup != null) {
            removeView(popup);
            invalidate();
            return true;
        }
        return false;
    }

    /** Vrai si le popup donné est montré. */
    public boolean isShowing(PopupController.PopupKind kind) {
        return popups.containsKey(kind);
    }

    /** La vue du popup montré, null s'il est fermé. */
    public @Nullable View popupOf(PopupController.PopupKind kind) {
        return popups.get(kind);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        // Les popups se posent en coordonnées de fenêtre : la couche ne
        // les réarrange pas à son propre redimensionnement — l'hôte les
        // repositionne (défilement, zoom, clavier).
    }
}
