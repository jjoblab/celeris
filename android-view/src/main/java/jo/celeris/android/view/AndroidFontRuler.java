package jo.celeris.android.view;

import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * Règle de police sur peinture Android : l'adaptateur concret de
 * {@link FontRuler} (ADR-008 — la mesure vit côté hôte, jamais lue
 * directement par les modèles).
 *
 * <p>La famille « mono » (défaut) rend une police à chasse fixe ;
 * « sans » et « serif » suivent. La règle est consultée aux dérivations
 * de métriques (changements de police), jamais dans la boucle de
 * dessin.</p>
 */
public final class AndroidFontRuler implements FontRuler {

    private final Paint peinture = new Paint();

    @Override
    public float charWidth(FontSpec spec) {
        configurer(spec);
        return peinture.measureText("M");
    }

    @Override
    public float lineHeight(FontSpec spec) {
        configurer(spec);
        Paint.FontMetrics metriques = peinture.getFontMetrics();
        return metriques.descent - metriques.ascent + metriques.leading;
    }

    @Override
    public float ascent(FontSpec spec) {
        configurer(spec);
        return -peinture.getFontMetrics().ascent;
    }

    @Override
    public float descent(FontSpec spec) {
        configurer(spec);
        return peinture.getFontMetrics().descent;
    }

    private void configurer(FontSpec spec) {
        peinture.setTypeface(policeDe(spec.family()));
        peinture.setTextSize(spec.size());
    }

    /**
     * La police de plateforme d'une famille — la table partagée de la
     * mesure et du tracé (PIEGE-22) : ce qui mesure est ce qui trace.
     *
     * @param famille la famille demandée
     * @return la police de plateforme correspondante
     */
    static Typeface policeDe(String famille) {
        switch (famille) {
            case "sans":
                return Typeface.SANS_SERIF;
            case "serif":
                return Typeface.SERIF;
            case "monospace":
                return Typeface.MONOSPACE;
            default:
                return Typeface.MONOSPACE;
        }
    }
}
