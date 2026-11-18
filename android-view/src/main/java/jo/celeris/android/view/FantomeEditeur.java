package jo.celeris.android.view;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
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
 * fantôme vit ici, la vue reste la racine de composition. Les gestes
 * notifient des abonnés MULTIPLES (ADR-0022, phase 2, T3) : le kit
 * et l'hôte s'abonnent chacun.</p>
 */
final class FantomeEditeur {

    private final EditorView vue;
    private final FantomePainter peintre;
    /** Le rappel 1.x à emplacement unique — repli compatibilité. */
    private @Nullable Consumer<GesteFantome> surGeste;
    /** Les abonnés 2.0 des gestes — multiples (T3). */
    private final List<Consumer<GesteFantome>> abonnesGeste =
            new CopyOnWriteArrayList<>();

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
     * session. Voie 1.x à emplacement unique : la voie 2.0 est
     * {@link #addGhostGestureListener}.
     *
     * @param surGeste le rappel, ou {@code null} pour couper
     */
    void setSurGeste(@Nullable Consumer<GesteFantome> surGeste) {
        this.surGeste = surGeste;
    }

    /** Abonne un écouteur des gestes du fantôme (voie 2.0, T3). */
    void addGhostGestureListener(Consumer<GesteFantome> ecouteur) {
        abonnesGeste.add(Objects.requireNonNull(ecouteur));
    }

    /** Désabonne un écouteur des gestes du fantôme (voie 2.0). */
    void removeGhostGestureListener(Consumer<GesteFantome> ecouteur) {
        abonnesGeste.remove(ecouteur);
    }

    /** Notifie le geste — le rappel 1.x puis CHAQUE abonné 2.0. */
    private void notifierGeste(GesteFantome geste) {
        Consumer<GesteFantome> rappel = surGeste;
        if (rappel != null) {
            rappel.accept(geste);
        }
        for (Consumer<GesteFantome> abonne : abonnesGeste) {
            abonne.accept(geste);
        }
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
        if (!aAbonnes() || peintre.suggestion() == null) {
            return false;
        }
        if (!peintre.premiereLigneTouchee(vue.etatImageCourante(), x, y)) {
            return false;
        }
        notifierGeste(GesteFantome.ACCEPTER_MOT);
        return true;
    }

    /**
     * La touche Tab accepte le premier mot du fantôme — la frappe qui
     * suit le refuse (la session s'en charge, F-09).
     *
     * @return vrai si le geste est consommé
     */
    boolean surTab() {
        if (!aAbonnes() || peintre.suggestion() == null) {
            return false;
        }
        notifierGeste(GesteFantome.ACCEPTER_MOT);
        return true;
    }

    /** Des abonnés vivent-ils — voie 1.x ou 2.0 (T3). */
    private boolean aAbonnes() {
        return surGeste != null || !abonnesGeste.isEmpty();
    }
}
