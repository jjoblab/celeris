package jo.celeris.android.widgets;

import android.content.Context;

import java.util.List;

/**
 * Le fil d'Ariane (M7.5, §7, reprise — D-09) : le chemin du symbole
 * courant en un bandeau d'une rangée au palier méta, à l'encre
 * atténuée, les niveaux joints par le séparateur, la troncature par la
 * largeur élague la TÊTE en ellipse — le modèle déjà livré
 * ({@link BreadcrumbModel}) décide, le peintre fournit son mesureur.
 * Suit le curseur : il se redessine, jamais ne se ferme au défilement.
 */
public final class BreadcrumbView extends TextRowsPopup {

    private static final String SEPARATEUR = " › ";

    public BreadcrumbView(Context context, PopupPalette palette) {
        super(context, palette);
    }

    /**
     * Pose le chemin à montrer — la troncature par la largeur
     * s'applique, la tête élaguée signalée par l'ellipse.
     *
     * @param niveaux les noms des niveaux, de la plus externe à la
     *                plus interne ; vide hors de tout symbole
     * @param largeurDisponible la largeur du bandeau, en pixels
     */
    public void setPath(List<String> niveaux, int largeurDisponible) {
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_META));
        List<String> affiches = BreadcrumbModel.tronque(niveaux,
                Math.max(0, largeurDisponible - 2 * paddingPx),
                this::largeurDe);
        if (affiches.isEmpty()) {
            setRows(List.of());
            return;
        }
        setRows(List.of(Row.tier(String.join(SEPARATEUR, affiches),
                palette.muted(), Tier.META)));
    }

    private int largeurDe(String nom) {
        return Math.round(textPaint().measureText(nom));
    }
}
