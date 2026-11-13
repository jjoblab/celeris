package jo.celeris.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
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
 * l'apparition du clavier — la couche ne fait que les porter. Le
 * chantier d'unification (2026-11-13) y ajoute la défense en profondeur :
 * toute pose se BORNE aux murs de la couche, et le doigt qui tombe hors
 * de toute carte AVERTIT le coordinateur sans consommer le geste — le
 * caret de l'éditeur poursuit sa vie, les cartes qui cèdent se
 * referment.</p>
 */
public final class PopupLayer extends ViewGroup {

    private final Map<PopupController.PopupKind, View> popups =
            new EnumMap<>(PopupController.PopupKind.class);

    /** L'avis du tap hors cartes — le coordinateur y ferme les cartes
     *  qui cèdent au geste extérieur. */
    private @Nullable Runnable surToucheHorsCartes;

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
     * focalisable (PIEGE-27), la couche se redessine. La MÊME vue
     * re-posée sous son genre ne re-naît ni ne meurt — elle se
     * re-mesure et se re-place (le filtre de complétion, sans
     * refermer) ; une vue qui vivait sous un AUTRE genre le quitte —
     * une vue ne vit jamais sous deux genres, son attache reste
     * unique.
     *
     * @param kind  le popup à montrer
     * @param popup la vue du popup
     * @param x     l'abscisse de la fenêtre, en pixels
     * @param y     l'ordonnée de la fenêtre, en pixels
     */
    public void show(PopupController.PopupKind kind, View popup, int x, int y) {
        View ancienne = popups.remove(kind);
        if (ancienne != null && ancienne != popup) {
            // l'ancienne carte meurt en voile — le remplacement se
            // croise en douceur, jamais un échange brusque
            refermer(ancienne);
        }
        // la même vue sous un autre genre : elle le quitte — l'attache
        // reste unique, jamais la vue ne se re-pose par-dessus elle-même
        popups.values().removeIf(vue -> vue == popup);
        // PIEGE-27 : aucun popup ne prend le focus d'entrée.
        popup.setFocusable(false);
        popup.setFocusableInTouchMode(false);
        popups.put(kind, popup);
        boolean nouvelle = popup.getParent() != this;
        if (popup instanceof PopupPanel panneau) {
            // la carte re-posée défait sa mort : le retrait différé se
            // dissout, jamais la carte vivante ne se retire d'elle-même
            panneau.annulerMort();
        }
        if (nouvelle) {
            addView(popup);
            // la fluidité du propriétaire (2026-11-13) : la carte NAÎT en
            // voile qui monte — jamais un remplacement sans le re-naitre
            if (popup instanceof PopupPanel panneau) {
                panneau.naissance();
            }
        }
        popup.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        poser(kind, popup, x, y);
    }

    /** La vue se pose bornée aux murs de la couche — la défense en
     *  profondeur : les ancres bornent déjà, la couche veille quand
     *  l'hôte calcule lui-même (les poses d'avant layout restent
     *  telles quelles, la couche n'a pas encore de murs). */
    private void poser(PopupController.PopupKind kind, View popup, int x, int y) {
        int borneX = getWidth() > 0
                ? Math.max(0, Math.min(x, getWidth() - popup.getMeasuredWidth())) : x;
        int borneY = getHeight() > 0
                ? Math.max(0, Math.min(y, getHeight() - popup.getMeasuredHeight())) : y;
        popup.layout(borneX, borneY,
                borneX + popup.getMeasuredWidth(),
                borneY + popup.getMeasuredHeight());
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
            poserVueMesuree(popup, x, y);
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
        poserVueMesuree(popup, x, y);
        invalidate();
    }

    /** La vue MESURÉE se pose bornée — le re-monte de la couche y
     *  passe, la largeur et la hauteur mesurées servent le bornage. */
    private void poserVueMesuree(View popup, int x, int y) {
        int borneX = getWidth() > 0
                ? Math.max(0, Math.min(x, getWidth() - popup.getMeasuredWidth())) : x;
        int borneY = getHeight() > 0
                ? Math.max(0, Math.min(y, getHeight() - popup.getMeasuredHeight())) : y;
        popup.layout(borneX, borneY,
                borneX + popup.getMeasuredWidth(),
                borneY + popup.getMeasuredHeight());
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
            // la carte MEURT en voile qui descend — le retrait suit à la
            // fin du voile (l'état de la couche tourne AUSSI : le
            // popup n'y montre déjà plus)
            refermer(popup);
            invalidate();
            return true;
        }
        return false;
    }

    /**
     * Referme une carte : la carte du kit descend en voile puis se
     * retire ; la vue qui n'est pas du kit part aussitôt. La carte
     * re-posée entre sa mort et son retrait ne se retire JAMAIS
     * d'elle-même (la course du retrait différé, chantier
     * d'unification : le retrait ne part que si la carte n'a pas été
     * re-prise).
     */
    private void refermer(View popup) {
        if (popup instanceof PopupPanel panneau) {
            panneau.mourir(() -> {
                if (!popups.containsValue(popup)) {
                    removeView(popup);
                }
            });
        } else {
            removeView(popup);
        }
    }

    /**
     * Pose (ou coupe) l'avis du tap HORS CARTES : le doigt qui tombe
     * hors de toute carte portée prévient le coordinateur — SANS
     * consommer le geste, qui poursuit sa vie vers l'éditeur (le caret
     * bouge, la sélection suit).
     *
     * @param avis l'avertisseur du tap extérieur, null pour couper
     */
    public void setSurToucheHorsCartes(@Nullable Runnable avis) {
        surToucheHorsCartes = avis;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                && horsDesCartes(event)) {
            Runnable avis = surToucheHorsCartes;
            if (avis != null) {
                avis.run();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    /** Vrai quand le doigt tombe hors de TOUTE carte portée — aucune
     *  carte ne se touchera sur ce geste. */
    private boolean horsDesCartes(MotionEvent event) {
        for (int i = 0; i < getChildCount(); i++) {
            View enfant = getChildAt(i);
            if (event.getX() >= enfant.getLeft() && event.getX() < enfant.getRight()
                    && event.getY() >= enfant.getTop()
                    && event.getY() < enfant.getBottom()) {
                return false;
            }
        }
        return true;
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
