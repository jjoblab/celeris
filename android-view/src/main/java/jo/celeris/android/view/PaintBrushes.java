package jo.celeris.android.view;

import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabrique de pinceaux sur peintures Android : chaque couche y prend la
 * sienne à sa construction (PIEGE-01).
 *
 * <p>La fabrique porte la bascule des ligatures (D-14) : chaque
 * peinture née d'ici vit dans son registre, la bascule y pose « liga »
 * (ligatures ouvertes, la police décide des glyphes) ou « -liga »
 * (ligatures fermées) — coupées par défaut, activables par
 * {@code EditorView.setLigatures}. La fabrique porte la table des
 * familles de la règle (PIEGE-22) : les pinceaux nés d'ici mesurent et
 * tracent par la même table.</p>
 */
public final class PaintBrushes implements BrushFactory {

    /** Les ligatures ouvertes : la police livre ses glyphes liés. */
    private static final String LIGATURES_OUVERTES = "liga";
    /** Les ligatures fermées : le texte reste lettre à lettre. */
    private static final String LIGATURES_FERMEES = "-liga";

    private final AndroidFontRuler regle;
    private final List<Paint> peintures = new ArrayList<>();
    private boolean ligatures;

    /**
     * Construit la fabrique sur la règle porteuse de la table des
     * familles (PIEGE-22).
     *
     * @param regle la règle de police partagée avec la mesure
     */
    public PaintBrushes(AndroidFontRuler regle) {
        this.regle = regle;
    }

    @Override
    public TextBrush newBrush() {
        Paint peinture = new Paint(Paint.ANTI_ALIAS_FLAG);
        peinture.setStyle(Paint.Style.FILL);
        peinture.setFontFeatureSettings(
                ligatures ? LIGATURES_OUVERTES : LIGATURES_FERMEES);
        peintures.add(peinture);
        return new PaintBrush(peinture, regle);
    }

    /**
     * Bascule les ligatures sur toutes les peintures nées d'ici (D-14) :
     * « liga » ouvre, « -liga » ferme — chaque pinceau vivant suit.
     *
     * @param actives vrai pour ouvrir les ligatures
     * @return vrai quand l'état a changé
     */
    boolean setLigatures(boolean actives) {
        if (actives == ligatures) {
            return false;
        }
        ligatures = actives;
        for (Paint peinture : peintures) {
            peinture.setFontFeatureSettings(
                    actives ? LIGATURES_OUVERTES : LIGATURES_FERMEES);
        }
        return true;
    }
}
