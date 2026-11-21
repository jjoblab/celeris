package jo.celeris.android.view.internal.caret;

import jo.celeris.android.view.internal.model.PopupAnchors;
import android.graphics.Rect;
import jo.celeris.android.view.internal.access.EditorAccess;

/**
 * Les ancres de popups de la scène (M7.5, §7) : le rectangle du
 * curseur principal — l'ancre des popups du caret (complétion, aide
 * aux paramètres, menu d'intentions) — et celui de la sélection
 * principale — l'ancre de la barre de sélection. Extraits de la vue
 * (plafond de 600 lignes, ADR-002) : la vue délègue, la géométrie vit
 * ici.
 */
public final class CaretAnchors {

    private final EditorAccess acces;

    /**
     * Construit les ancres de la vue donnée — la vue n'est consultée
     * qu'après sa construction, jamais ici.
     *
     * @param vue la vue propriétaire des ancres
     */
    public CaretAnchors(EditorAccess acces) {
        this.acces = acces;
    }

    /**
     * Le rectangle du curseur principal, en coordonnées de la vue —
     * largeur nulle : l'ancre est le bord gauche du caractère visé,
     * hauteur celle de la rangée.
     *
     * @return le rectangle du curseur principal
     */
    public Rect caretBounds() {
        acces.preparerEtatImage();
        return PopupAnchors.caretRect(acces.etatImage().document(),
                acces.projection(), acces.metrics(), acces.largeurGouttiere(),
                acces.scrollX(), acces.scrollY(),
                acces.session().selection().ranges());
    }

    /**
     * Le rectangle de la sélection principale (première plage), en
     * coordonnées de la vue — l'union des extrémités de la plage. Une
     * plage vide rend le rectangle du curseur.
     *
     * @return le rectangle de la sélection principale
     */
    public Rect selectionBounds() {
        acces.preparerEtatImage();
        return PopupAnchors.selectionRect(acces.etatImage().document(),
                acces.projection(), acces.metrics(), acces.largeurGouttiere(),
                acces.scrollX(), acces.scrollY(),
                acces.session().selection().ranges());
    }
}
