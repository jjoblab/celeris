package jo.celeris.demo;

import org.jspecify.annotations.Nullable;

import jo.celeris.android.view.GesteFantome;
import jo.celeris.core.ai.SessionTexteFantome;
import jo.celeris.core.ai.SuggestionFantome;
import jo.celeris.core.text.TextSession;

/**
 * Le pilote du texte fantôme de la démonstration (F-01) : le geste
 * s'applique à l'ancre décalée — chaque fragment accepté avance
 * l'ancre du suivant — et le reste du fantôme se ré-ancre à la
 * révision courante pour l'affichage. L'acceptation reste un geste de
 * l'utilisateur sur une suggestion visible : l'insertion passe par la
 * voie de transaction ordinaire, annulable.
 *
 * <p>Pur et testé sans appareil : la session, le fantôme et le décalage
 * vivent ici, la vue et l'atelier ne font que suivre.</p>
 */
final class PiloteFantome {

    private final TextSession session;
    private final SessionTexteFantome fantome;

    /** Le décalage d'ancre accumulé par les fragments acceptés. */
    private int decalage;

    /**
     * Construit le pilote sur la session ouverte.
     *
     * @param session la session d'édition
     * @param fantome la session de texte fantôme, sur son modèle
     */
    PiloteFantome(TextSession session, SessionTexteFantome fantome) {
        this.session = session;
        this.fantome = fantome;
    }

    /**
     * Demande une complétion : le décalage d'ancre repart de zéro.
     *
     * @param document l'instantané courant
     * @param offset   la position du curseur
     */
    void demander(jo.celeris.core.text.Document document, int offset) {
        decalage = 0;
        fantome.demander(document, offset);
    }

    /**
     * Le geste sur le fantôme : l'acceptation applique le fragment à
     * l'ancre décalée — transaction ordinaire, donc annulable — et le
     * décalage avance ; le refus ferme, le vol meurt avec.
     *
     * @param geste le geste demandé par la vue ou le menu
     */
    void surGeste(GesteFantome geste) {
        SuggestionFantome courante = fantome.suggestionActive();
        if (courante == null) {
            return;
        }
        if (geste == GesteFantome.REFUSER) {
            fantome.refuser();
            decalage = 0;
            return;
        }
        String fragment = switch (geste) {
            case ACCEPTER_MOT -> fantome.accepterMot();
            case ACCEPTER_LIGNE -> fantome.accepterLigne();
            case ACCEPTER_TOUT -> fantome.accepterTout();
            default -> "";
        };
        if (fragment.isEmpty()) {
            return;
        }
        int ancre = courante.offset() + decalage;
        session.replaceRange(ancre, ancre, fragment);
        decalage += fragment.length();
    }

    /**
     * La frappe suivante refuse le fantôme et annule le vol (F-09) —
     * le décalage d'ancre repart de zéro.
     */
    void surEdition() {
        fantome.frappe();
        decalage = 0;
    }

    /**
     * La suggestion à afficher : le reste du fantôme, ré-ancré au
     * décalage courant et daté de la révision du document — la vue
     * n'affiche jamais une ancre périmée.
     *
     * @return la suggestion visible, ou {@code null} s'il n'y en a pas
     */
    @Nullable
    SuggestionFantome suggestionVue() {
        SuggestionFantome courante = fantome.suggestionActive();
        if (courante == null) {
            return null;
        }
        return new SuggestionFantome(courante.texte(),
                courante.offset() + decalage, session.document().revision());
    }
}
