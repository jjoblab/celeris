package jo.celeris.android.widgets;

import android.content.Context;

import java.util.List;

/**
 * L'aide aux paramètres (M7.5, §7, reprise — B-07, D-10) : le modèle
 * déjà livré ({@link SignatureAideModel}) devient une rangée — le
 * libellé de la signature, le paramètre courant SOULIGNÉ par le
 * tronçon exact que le modèle calcule DANS LA ZONE DES PARAMÈTRES.
 * Le placement au-dessus du curseur vit chez {@link PopupAnchor} et
 * {@link PopupPlacer}.
 */
public final class SignatureHelpView extends TextRowsPopup {

    public SignatureHelpView(Context context, PopupPalette palette) {
        super(context, palette);
    }

    /**
     * Pose la signature à montrer.
     *
     * @param model la signature active, ou null pour vider le panneau
     */
    public void setModel(SignatureAideModel model) {
        if (model == null) {
            setRows(List.of());
            return;
        }
        SignatureAideModel.Plage plage = model.plageParametreCourant();
        int debut = plage == null ? -1 : plage.debut();
        int fin = plage == null ? -1 : plage.fin();
        setRows(List.of(new Row(model.libelle(), -1, debut, fin)));
    }
}
