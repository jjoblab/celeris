package jo.celeris.android.view.internal.fonts;

import android.util.DisplayMetrics;
import jo.celeris.android.view.AndroidFontRuler;
import jo.celeris.android.view.EditorMetrics;
import jo.celeris.android.view.FontSpec;
import jo.celeris.android.view.HitTest;
import jo.celeris.android.view.SpSizes;
import jo.celeris.android.view.internal.access.EditorAccess;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


/**
 * Le collaborateur des polices de la vue (plafond de 600 lignes,
 * ADR-002) : le changement de police et sa conversion sp → pixels y
 * vivent, la vue délègue.
 *
 * <p>PIEGE-05 : les métriques se redérivent par reconsultation de la
 * règle. PIEGE-28 : la source de rangées se réinitialise même à
 * révision constante — un document jamais édité doit se re-mesurer à
 * la nouvelle police. PIEGE-23 : la conversion sp → pixels vit dans
 * {@link SpSizes}, pure et testée — jamais un pixel cru ne franchit
 * cette porte.</p>
 */
public final class ViewFonts {

    private final EditorAccess acces;
    private final AndroidFontRuler regle;
    /** L'écouteur 1.x à emplacement unique — repli compatibilité. */
    /** Les abonnés 2.0 — multiples, sans écrasement (T3). */
    private final List<Consumer<Float>> sizeSubscribers =
            new CopyOnWriteArrayList<>();

    /**
     * Construit le collaborateur des polices de la vue donnée, sur la
     * règle porteuse de la table des familles (D-14, PIEGE-22 : la
     * mesure et le tracé lisent la même table).
     *
     * @param vue   la vue propriétaire des métriques
     * @param regle la règle de police partagée
     */
    public ViewFonts(EditorAccess acces, AndroidFontRuler regle) {
        this.acces = acces;
        this.regle = regle;
    }

    /**
     * Change la police : les métriques se redérivent par reconsultation
     * de la règle (PIEGE-05), le cache se vide par révision suivie, le
     * budget de colonnes se redérive à l'image suivante. La taille
     * changeante prévient l'écouteur — l'hôte y persiste sa taille
     * (D-15).
     *
     * @param font la spécification de police, taille en sp
     */
    public void setFont(FontSpec font) {
        boolean tailleChangee = font.size() != acces.policeLogique().size();
        acces.setPoliceLogique(font);
        acces.setMetrics(EditorMetrics.of(enPixels(font), regle));
        acces.setViseur(new HitTest(acces.metrics()));
        // PIEGE-28 : la source de rangées se réinitialise même à
        // révision constante — un document jamais édité (révision 0)
        // doit se re-mesurer à la nouvelle police ; le cache se vide,
        // les mises en page de l'ancienne police ne s'y accumulent pas.
        acces.setRevisionSuivie(Long.MIN_VALUE);
        acces.cache().clear();
        if (tailleChangee) {
            for (Consumer<Float> abonne : sizeSubscribers) {
                abonne.accept(font.size());
            }
        }
        acces.invalidate();
    }

    /**
     * Abonne un écouteur des changements de taille de police (voie
     * 2.0, ADR-0022 phase 2 T3) : le pincement, la molette Ctrl et le
     * clavier notifient CHACUN leurs abonnés.
     *
     * @param ecouteur l'écouteur, jamais null
     */
    public void addFontSizeChangeListener(Consumer<Float> ecouteur) {
        sizeSubscribers.add(Objects.requireNonNull(ecouteur));
    }

    /**
     * Désabonne un écouteur des changements de taille de police.
     *
     * @param ecouteur l'écouteur retiré
     */
    public void removeFontSizeChangeListener(Consumer<Float> ecouteur) {
        sizeSubscribers.remove(ecouteur);
    }

    /**
     * Pose une police sous un nom de famille (D-14) : la table de la
     * règle la gagne, la mesure et le tracé la suivent (PIEGE-22) ; la
     * famille active se redérive — métriques, viseur, cache (PIEGE-05,
     * PIEGE-28) — la famille dormante attend son setFont.
     *
     * @param family le nom de famille, celui du {@link FontSpec}
     * @param font  la police de plateforme à poser
     */
    public void setFont(String family, android.graphics.Typeface font) {
        acces.setFont(family, font);
        if (acces.policeLogique().family().equals(family)) {
            setFont(acces.policeLogique());
        } else {
            acces.invalidate();
        }
    }

    /**
     * Les métriques d'une police à la densité courante — la porte de
     * la construction : la police logique (sp) s'y convertit une fois
     * pour toutes (PIEGE-23).
     *
     * @param font la spécification de police, taille en sp
     * @return les métriques résolues, en pixels
     */
    public EditorMetrics metricsOf(FontSpec font) {
        return EditorMetrics.of(enPixels(font), regle);
    }

    /**
     * La conversion sp vers pixels à la densité courante : la mesure et
     * le tracé vivent en pixels, l'interface vit en sp — la conversion
     * vit dans {@link SpSizes}, pure et testée (PIEGE-23), jamais un
     * pixel cru ne franchit cette porte.
     */
    private FontSpec enPixels(FontSpec font) {
        DisplayMetrics metriques = acces.getResources().getDisplayMetrics();
        // L'échelle de police de l'accessibilité vit dans la
        // configuration (le champ scaledDensity est dépréqué) : la
        // conversion reste sp × densité × échelle, pure et testée.
        float echelle = acces.getResources().getConfiguration().fontScale;
        return new FontSpec(font.family(), SpSizes.enPixels(font.size(),
                metriques.density, echelle));
    }
}
