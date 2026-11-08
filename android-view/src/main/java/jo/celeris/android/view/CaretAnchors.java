package jo.celeris.android.view;

import android.graphics.Rect;

/**
 * Les ancres de popups de la scène (M7.5, §7) : le rectangle du
 * curseur principal — l'ancre des popups du caret (complétion, aide
 * aux paramètres, menu d'intentions) — et celui de la sélection
 * principale — l'ancre de la barre de sélection. Extraits de la vue
 * (plafond de 600 lignes, ADR-002) : la vue délègue, la géométrie vit
 * ici.
 */
final class CaretAnchors {

    private final EditorView vue;

    /**
     * Construit les ancres de la vue donnée — la vue n'est consultée
     * qu'après sa construction, jamais ici.
     *
     * @param vue la vue propriétaire des ancres
     */
    CaretAnchors(EditorView vue) {
        this.vue = vue;
    }

    /**
     * Le rectangle du curseur principal, en coordonnées de la vue —
     * largeur nulle : l'ancre est le bord gauche du caractère visé,
     * hauteur celle de la rangée.
     *
     * @return le rectangle du curseur principal
     */
    Rect caretBounds() {
        vue.preparerEtatImage();
        return PopupAnchors.caretRect(vue.etatImage.document(),
                vue.projection, vue.metrics, vue.largeurGouttiere,
                vue.scrollX, vue.scrollY,
                vue.session.selection().ranges());
    }

    /**
     * Le rectangle de la sélection principale (première plage), en
     * coordonnées de la vue — l'union des extrémités de la plage. Une
     * plage vide rend le rectangle du curseur.
     *
     * @return le rectangle de la sélection principale
     */
    Rect selectionBounds() {
        vue.preparerEtatImage();
        return PopupAnchors.selectionRect(vue.etatImage.document(),
                vue.projection, vue.metrics, vue.largeurGouttiere,
                vue.scrollX, vue.scrollY,
                vue.session.selection().ranges());
    }
}
