package jo.celeris.android.view;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * Les commandes de navigation programmatique de l'éditeur : poser le
 * curseur ou sélectionner une plage à un offset donné, avec dévoilement
 * (réajustement minimum du défilement). Même forme que
 * {@link KeyActions} : les commandes statiques opèrent sur la vue par
 * ses accès de paquet — la construction de sélection, elle, est pure et
 * testée sans appareil.
 */
public final class NavigationCommands {

    private NavigationCommands() {
    }

    /**
     * La sélection « curseur à l'offset », bornée au document.
     *
     * @param offset l'offset voulu, tout entier est accepté puis borné
     * @param length la longueur du document
     * @return la sélection mono-curseur bornée
     */
    public static MultiSelection caretAt(int offset, int length) {
        int borne = Math.max(0, Math.min(offset, length));
        return MultiSelection.of(SelRange.caretAt(borne));
    }

    /**
     * La sélection de plage, bornée au document et ordonnée.
     *
     * @param debut  l'offset de début voulu
     * @param fin    l'offset de fin exclusive voulu
     * @param length la longueur du document
     * @return la sélection de plage bornée, ancre avant curseur
     */
    public static MultiSelection rangeBetween(int debut, int fin, int length) {
        int d = Math.max(0, Math.min(debut, length));
        int f = Math.max(0, Math.min(fin, length));
        if (f < d) {
            int temporaire = d;
            d = f;
            f = temporaire;
        }
        return MultiSelection.of(new SelRange(d, f));
    }

    /**
     * Pose le curseur primaire de la vue à l'offset donné et le
     * dévoile — le chemin du saut de navigation (plan, recherche, pile
     * d'appels).
     *
     * @param vue    la vue d'éditeur visée
     * @param offset l'offset absolu du curseur
     */
    public static void placeCaret(EditorView vue, int offset) {
        TextSession session = vue.session();
        session.setSelection(caretAt(offset, session.length()));
        vue.curseurClavier.revealCaret();
        vue.curseurClavier.annoncerSelection();
    }

    /**
     * Sélectionne la plage donnée de la vue et la dévoile — le chemin
     * du saut de correspondance de recherche : la plage entière se lit
     * d'un coup d'œil.
     *
     * @param vue   la vue d'éditeur visée
     * @param debut l'offset absolu de début
     * @param fin   l'offset absolu de fin exclusive
     */
    public static void selectRange(EditorView vue, int debut, int fin) {
        TextSession session = vue.session();
        session.setSelection(rangeBetween(debut, fin, session.length()));
        vue.curseurClavier.revealCaret();
        vue.curseurClavier.annoncerSelection();
    }
}
