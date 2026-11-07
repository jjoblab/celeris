package jo.celeris.android.view;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.ai.SuggestionFantome;

/**
 * Le collaborateur du texte fantôme (F-01, ADR-012) : la vue affiche,
 * l'hôte décide — le geste d'acceptation (clavier Tab, appui sur le
 * fantôme) part vers le rappel de l'hôte, qui l'exécute sur sa session
 * de fantôme et redépose la suggestion qui survit.
 *
 * <p>Vue-collaborateur par délégation (même famille que
 * {@link AccessibilityHost} et {@link TouchesEditeur}) : l'état du
 * fantôme vit ici, la vue reste la racine de composition.</p>
 */
final class FantomeEditeur {

    private final EditorView vue;
    private final FantomePainter peintre;
    private @Nullable Consumer<GesteFantome> surGeste;

    /**
     * Construit le collaborateur du fantôme — la couche de peinture se
     * crée ici, la vue l'ajoute à son pipeline.
     *
     * @param vue     la vue propriétaire du fantôme
     * @param brushes la fabrique de pinceaux de la vue
     */
    FantomeEditeur(EditorView vue, BrushFactory brushes) {
        this.vue = vue;
        this.peintre = new FantomePainter(brushes);
    }

    /** La couche de peinture — la vue la pose au-dessus du texte. */
    FantomePainter peintre() {
        return peintre;
    }

    /**
     * Dépose (ou retire) la suggestion visible.
     *
     * @param suggestion la suggestion, ou {@code null} pour fermer
     */
    void setSuggestion(@Nullable SuggestionFantome suggestion) {
        peintre.setSuggestion(suggestion);
        vue.invalidate();
    }

    /** La suggestion visible, ou {@code null}. */
    @Nullable
    SuggestionFantome suggestion() {
        return peintre.suggestion();
    }

    /**
     * Le rappel des gestes du fantôme : acceptation par mot, par ligne
     * ou en bloc, refus — l'hôte exécute, la vue ne touche à aucune
     * session.
     *
     * @param surGeste le rappel, ou {@code null} pour couper
     */
    void setSurGeste(@Nullable Consumer<GesteFantome> surGeste) {
        this.surGeste = surGeste;
    }

    /**
     * L'appui vise-t-il le fantôme ? Le geste d'acceptation au doigt
     * (F-01) : la première ligne touchée accepte le premier mot.
     *
     * @param x l'abscisse de l'appui, en pixels
     * @param y l'ordonnée de l'appui, en pixels
     * @return vrai si le geste est consommé
     */
    boolean touche(float x, float y) {
        if (surGeste == null || peintre.suggestion() == null) {
            return false;
        }
        if (!peintre.premiereLigneTouchee(vue.etatImageCourante(), x, y)) {
            return false;
        }
        surGeste.accept(GesteFantome.ACCEPTER_MOT);
        return true;
    }

    /**
     * La touche Tab accepte le premier mot du fantôme — la frappe qui
     * suit le refuse (la session s'en charge, F-09).
     *
     * @return vrai si le geste est consommé
     */
    boolean surTab() {
        Consumer<GesteFantome> rappel = surGeste;
        if (rappel == null || peintre.suggestion() == null) {
            return false;
        }
        rappel.accept(GesteFantome.ACCEPTER_MOT);
        return true;
    }
}
