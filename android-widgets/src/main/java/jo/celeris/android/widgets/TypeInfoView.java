package jo.celeris.android.widgets;

import android.content.Context;

import java.util.List;

/**
 * La carte d'info de type (catégorie A de l'inventaire — le fil rouge
 * du guide du développeur, la seconde surface réveillée du signal du
 * propriétaire) : le nom du type du symbole au curseur, UNE rangée au
 * palier titre. PIEGE-27 : jamais de focus, jamais de navigation — la
 * carte DÉCRIPT, elle ne choisit pas ; la frappe et le défilement la
 * referment (les régimes du survol), l'échappement la referme comme
 * bulle éphémère, le voile la coupe.
 *
 * <p>La carte ne connaît NI les moteurs, NI l'éditeur : le type arrive
 * par {@link #setType(String)} — l'atelier seul décide d'où il vient
 * (le coach local, un serveur) ; null ne se pose jamais, la carte sans
 * type n'existe pas (aucune fausse oracle).</p>
 */
public final class TypeInfoView extends TextRowsPopup {

    /** Le type posé — vide avant la première pose, les bancs y lisent. */
    private String type = "";

    /** Construit la carte sur la palette du thème courant. */
    public TypeInfoView(Context context, PopupPalette palette) {
        super(context, palette);
    }

    /**
     * Pose le type du symbole — la vue ne demande JAMAIS au moteur.
     *
     * @param nomDuType le nom du type, jamais null ni blanc
     */
    public final void setType(String nomDuType) {
        this.type = nomDuType;
        setRows(List.of(Row.tier(nomDuType, -1, Tier.TITRE)));
    }

    /** Le type posé — la rangée titre le porte. */
    public final String type() {
        return type;
    }
}
