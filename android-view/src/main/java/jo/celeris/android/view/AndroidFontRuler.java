package jo.celeris.android.view;

import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Règle de police sur peinture Android : l'adaptateur concret de
 * {@link FontRuler} (ADR-008 — la mesure vit côté hôte, jamais lue
 * directement par les modèles).
 *
 * <p>La famille « mono » (défaut) rend une police à chasse fixe ;
 * « sans » et « serif » suivent. La règle porte la table des familles
 * (D-14) : une police posée par l'hôte y vit sous son nom — la police
 * à ligatures de l'hôte, par exemple — et la mesure et le tracé la
 * lisent ensemble (PIEGE-22 : ce qui mesure est ce qui trace). La règle
 * est consultée aux dérivations de métriques (changements de police),
 * jamais dans la boucle de dessin.</p>
 */
public final class AndroidFontRuler implements FontRuler {

    private final Paint paint = new Paint();

    /** Les familles posées par l'hôte (D-14), par nom. */
    private final Map<String, Typeface> famillesPosees = new HashMap<>();

    @Override
    public float charWidth(FontSpec spec) {
        configurer(spec);
        return paint.measureText("M");
    }

    @Override
    public float lineHeight(FontSpec spec) {
        configurer(spec);
        Paint.FontMetrics metriques = paint.getFontMetrics();
        return metriques.descent - metriques.ascent + metriques.leading;
    }

    @Override
    public float ascent(FontSpec spec) {
        configurer(spec);
        return -paint.getFontMetrics().ascent;
    }

    @Override
    public float descent(FontSpec spec) {
        configurer(spec);
        return paint.getFontMetrics().descent;
    }

    private void configurer(FontSpec spec) {
        paint.setTypeface(policeDe(spec.family()));
        paint.setTextSize(spec.size());
    }

    /**
     * Pose une police sous un nom de famille (D-14) : la famille nommée
     * rend cette police à la mesure comme au tracé (PIEGE-22) — la voie
     * de l'hôte pour sa police à ligatures.
     *
     * @param family le nom de famille, celui du {@link FontSpec}
     * @param font  la police de plateforme à poser
     */
    void setFont(String family, Typeface font) {
        Objects.requireNonNull(family, "famille");
        Objects.requireNonNull(font, "police");
        famillesPosees.put(family, font);
    }

    /**
     * La police de plateforme d'une famille — la table partagée de la
     * mesure et du tracé (PIEGE-22) : ce qui mesure est ce qui trace.
     * La famille posee par l'hôte gagne (D-14) ; « sans », « serif » et
     * « mono » suivent les défauts de la plateforme.
     *
     * @param family la famille demandée
     * @return la police de plateforme correspondante
     */
    Typeface policeDe(String family) {
        Typeface posee = famillesPosees.get(family);
        if (posee != null) {
            return posee;
        }
        switch (family) {
            case "sans":
                return Typeface.SANS_SERIF;
            case "serif":
                return Typeface.SERIF;
            default:
                return Typeface.MONOSPACE;
        }
    }
}
