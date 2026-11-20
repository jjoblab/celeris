package jo.celeris.android.view;

import java.util.List;

/**
 * La mémoire des plis PAR SESSION (D-07, l'analyse croisée) : le pli
 * manuel appartient à SON document — l'archive et la restauration y
 * vivent, l'état du pont (EditorSyntax) reste mince.
 *
 * <p>La carte est à clés faibles : une session morte n'y retient plus
 * rien (le changement d'onglet n'accumule pas de mémoire), une
 * session vivante y retrouve SES plis au retour.</p>
 */
final class PlisParSession {

    /** Les états repliés : session → paires [début, fin]. */
    private final java.util.Map<jo.celeris.core.text.TextSession,
            List<int[]>> etats = new java.util.WeakHashMap<>();

    /** Le modèle de plis restauré par la mémoire. */
    private final FoldModel plis;

    /** Le pont de suivi : la restauration y passe (fold du pont). */
    private final FoldTracker suivi;

    PlisParSession(FoldModel plis, FoldTracker suivi) {
        this.plis = plis;
        this.suivi = suivi;
    }

    /** Archive l'état replié courant sous la session donnée — la
     *  session quittée emporte ses plis, à clés faibles.
     *
     * @param quittee la session qui part (null : rien à archiver) */
    void archiver(jo.celeris.core.text.@org.jspecify.annotations.Nullable TextSession quittee) {
        if (quittee == null) {
            return;
        }
        List<int[]> etat = new java.util.ArrayList<>();
        for (FoldModel.Pli pli : plis.plis()) {
            etat.add(new int[] { pli.debut(), pli.fin() });
        }
        etats.put(quittee, etat);
    }

    /** Restaure l'état replié de la session courante : les plis
     *  archivés reviennent par le pont (le suivi suit), un document
     *  inconnu repart déplié.
     *
     * @param courante la session qui arrive */
    void restaurer(jo.celeris.core.text.TextSession courante) {
        suivi.clear();
        List<int[]> etat = etats.get(courante);
        if (etat == null) {
            return;
        }
        for (int[] pli : etat) {
            suivi.fold(pli[0], pli[1]);
        }
    }
}
