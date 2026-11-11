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

    private final Paint peinture = new Paint();

    /** Les familles posées par l'hôte (D-14), par nom. */
    private final Map<String, Typeface> famillesPosees = new HashMap<>();

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
     * Pose une police sous un nom de famille (D-14) : la famille nommée
     * rend cette police à la mesure comme au tracé (PIEGE-22) — la voie
     * de l'hôte pour sa police à ligatures.
     *
     * @param famille le nom de famille, celui du {@link FontSpec}
     * @param police  la police de plateforme à poser
     */
    void poserPolice(String famille, Typeface police) {
        Objects.requireNonNull(famille, "famille");
        Objects.requireNonNull(police, "police");
        famillesPosees.put(famille, police);
    }

    /**
     * La police de plateforme d'une famille — la table partagée de la
     * mesure et du tracé (PIEGE-22) : ce qui mesure est ce qui trace.
     * La famille posee par l'hôte gagne (D-14) ; « sans », « serif » et
     * « mono » suivent les défauts de la plateforme.
     *
     * @param famille la famille demandée
     * @return la police de plateforme correspondante
     */
    Typeface policeDe(String famille) {
        Typeface posee = famillesPosees.get(famille);
        if (posee != null) {
            return posee;
        }
        switch (famille) {
            case "sans":
                return Typeface.SANS_SERIF;
            case "serif":
                return Typeface.SERIF;
            default:
                return Typeface.MONOSPACE;
        }
    }
}
