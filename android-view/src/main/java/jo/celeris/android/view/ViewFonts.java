package jo.celeris.android.view;

import android.util.DisplayMetrics;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * Le collaborateur des polices de la vue (plafond de 600 lignes,
 * ADR-002) : le changement de police et sa conversion sp → pixels y
 * vivent, la vue délègue.
 *
 * <p>PIEGE-05 : les métriques se redérivent par reconsultation de la
 * règle. PIEGE-28 : la source de rangées se réinitialise même à
 * révision constante — un document jamais édité doit se re-mesurer à
 * la nouvelle police. PIEGE-23 : la conversion sp → pixels vit dans
 * {@link TaillesSp}, pure et testée — jamais un pixel cru ne franchit
 * cette porte.</p>
 */
final class ViewFonts {

    private final EditorView vue;
    private final AndroidFontRuler regle;
    /** L'écouteur 1.x à emplacement unique — repli compatibilité. */
    private @Nullable Consumer<Float> onFontSizeChanged;
    /** Les abonnés 2.0 — multiples, sans écrasement (T3). */
    private final List<Consumer<Float>> abonnesTaille =
            new CopyOnWriteArrayList<>();

    /**
     * Construit le collaborateur des polices de la vue donnée, sur la
     * règle porteuse de la table des familles (D-14, PIEGE-22 : la
     * mesure et le tracé lisent la même table).
     *
     * @param vue   la vue propriétaire des métriques
     * @param regle la règle de police partagée
     */
    ViewFonts(EditorView vue, AndroidFontRuler regle) {
        this.vue = vue;
        this.regle = regle;
    }

    /**
     * Change la police : les métriques se redérivent par reconsultation
     * de la règle (PIEGE-05), le cache se vide par révision suivie, le
     * budget de colonnes se redérive à l'image suivante. La taille
     * changeante prévient l'écouteur — l'hôte y persiste sa taille
     * (D-15).
     *
     * @param police la spécification de police, taille en sp
     */
    void setFont(FontSpec police) {
        boolean tailleChangee = police.size() != vue.policeLogique.size();
        vue.policeLogique = police;
        vue.metrics = EditorMetrics.of(enPixels(police), regle);
        vue.viseur = new HitTest(vue.metrics);
        // PIEGE-28 : la source de rangées se réinitialise même à
        // révision constante — un document jamais édité (révision 0)
        // doit se re-mesurer à la nouvelle police ; le cache se vide,
        // les mises en page de l'ancienne police ne s'y accumulent pas.
        vue.revisionSuivie = Long.MIN_VALUE;
        vue.cache.clear();
        if (tailleChangee) {
            Consumer<Float> ecouteurUnique = onFontSizeChanged;
            if (ecouteurUnique != null) {
                ecouteurUnique.accept(police.size());
            }
            for (Consumer<Float> abonne : abonnesTaille) {
                abonne.accept(police.size());
            }
        }
        vue.invalidate();
    }

    /**
     * L'écouteur des changements de taille de police (D-15) — l'hôte y
     * persiste la taille. Voie 1.x à emplacement unique : la voie 2.0
     * est {@link #addFontSizeChangeListener} (plusieurs abonnés).
     *
     * @param ecouteur l'écouteur, ou {@code null} pour le retirer
     */
    void setOnFontSizeChanged(@Nullable Consumer<Float> ecouteur) {
        onFontSizeChanged = ecouteur;
    }

    /**
     * Abonne un écouteur des changements de taille de police (voie
     * 2.0, ADR-0022 phase 2 T3) : le pincement, la molette Ctrl et le
     * clavier notifient CHACUN leurs abonnés.
     *
     * @param ecouteur l'écouteur, jamais null
     */
    void addFontSizeChangeListener(Consumer<Float> ecouteur) {
        abonnesTaille.add(Objects.requireNonNull(ecouteur));
    }

    /**
     * Désabonne un écouteur des changements de taille de police.
     *
     * @param ecouteur l'écouteur retiré
     */
    void removeFontSizeChangeListener(Consumer<Float> ecouteur) {
        abonnesTaille.remove(ecouteur);
    }

    /**
     * Pose une police sous un nom de famille (D-14) : la table de la
     * règle la gagne, la mesure et le tracé la suivent (PIEGE-22) ; la
     * famille active se redérive — métriques, viseur, cache (PIEGE-05,
     * PIEGE-28) — la famille dormante attend son setFont.
     *
     * @param famille le nom de famille, celui du {@link FontSpec}
     * @param police  la police de plateforme à poser
     */
    void poserPolice(String famille, android.graphics.Typeface police) {
        regle.poserPolice(famille, police);
        if (vue.policeLogique.family().equals(famille)) {
            setFont(vue.policeLogique);
        } else {
            vue.invalidate();
        }
    }

    /**
     * Les métriques d'une police à la densité courante — la porte de
     * la construction : la police logique (sp) s'y convertit une fois
     * pour toutes (PIEGE-23).
     *
     * @param police la spécification de police, taille en sp
     * @return les métriques résolues, en pixels
     */
    EditorMetrics metricsOf(FontSpec police) {
        return EditorMetrics.of(enPixels(police), regle);
    }

    /**
     * La conversion sp vers pixels à la densité courante : la mesure et
     * le tracé vivent en pixels, l'interface vit en sp — la conversion
     * vit dans {@link TaillesSp}, pure et testée (PIEGE-23), jamais un
     * pixel cru ne franchit cette porte.
     */
    private FontSpec enPixels(FontSpec police) {
        DisplayMetrics metriques = vue.getResources().getDisplayMetrics();
        // L'échelle de police de l'accessibilité vit dans la
        // configuration (le champ scaledDensity est dépréqué) : la
        // conversion reste sp × densité × échelle, pure et testée.
        float echelle = vue.getResources().getConfiguration().fontScale;
        return new FontSpec(police.family(), TaillesSp.enPixels(police.size(),
                metriques.density, echelle));
    }
}
