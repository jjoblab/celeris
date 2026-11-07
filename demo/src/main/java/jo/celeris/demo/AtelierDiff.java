package jo.celeris.demo;

import java.util.List;

import jo.celeris.android.widgets.DiffBlocs;
import jo.celeris.android.widgets.RevueDiff;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier de revue du diff de la démonstration (D-12) : le diff du
 * texte courant face au contenu d'origine se relit bloc par bloc —
 * accepter applique les lignes du bloc, refuser les garde anciennes,
 * le réalignement suit. La décision finale s'applique en une
 * transaction de remplacement intégral, annulable d'un geste.
 */
final class AtelierDiff {

    private final String reference;
    private final TextSession session;

    private RevueDiff revue;

    /** L'écouteur de l'état de la revue : le bloc courant et sa décision. */
    interface SurEtat {

        /**
         * L'état change : le bloc courant, ses lignes préfixées, les
         * décisions rendues et le nombre de blocs.
         *
         * @param bloc     l'index du bloc courant, -1 quand tout est décidé
         * @param lignes   les lignes du bloc courant, préfixées
         * @param decidable vrai tant qu'un bloc attend
         */
        void etat(int bloc, List<String> lignes, boolean decidable);
    }

    /**
     * Construit l'atelier sur la session ouverte.
     *
     * @param reference le contenu d'origine du fichier
     * @param session   la session d'édition courante
     */
    AtelierDiff(String reference, TextSession session) {
        this.reference = reference;
        this.session = session;
    }

    /**
     * Ouvre (ou rouvre) la revue du texte courant face à l'origine.
     *
     * @param surEtat l'écouteur des états successifs
     */
    void ouvrir(SurEtat surEtat) {
        revue = RevueDiff.de(reference, session.document().text());
        annoncer(surEtat);
    }

    /**
     * Décide le bloc courant et avance.
     *
     * @param decision la décision du bloc courant
     * @param surEtat  l'écouteur des états successifs
     */
    void decider(RevueDiff.Decision decision, SurEtat surEtat) {
        if (revue == null) {
            return;
        }
        for (int i = 0; i < revue.nombreDeBlocs(); i++) {
            if (revue.decision(i) == RevueDiff.Decision.EN_ATTENTE) {
                revue = revue.decider(i, decision);
                break;
            }
        }
        annoncer(surEtat);
    }

    /**
     * Applique le texte fusionné des décisions courantes : une
     * transaction de remplacement intégral — annulable.
     *
     * @return vrai si le texte a changé
     */
    boolean appliquer() {
        if (revue == null) {
            return false;
        }
        String fusion = revue.texte();
        if (fusion.equals(session.document().text())) {
            return false;
        }
        return session.replaceRange(0, session.document().text().length(),
                fusion);
    }

    private void annoncer(SurEtat surEtat) {
        int courant = -1;
        List<String> lignes = List.of();
        for (int i = 0; i < revue.nombreDeBlocs(); i++) {
            if (revue.decision(i) == RevueDiff.Decision.EN_ATTENTE) {
                courant = i;
                lignes = lignesDuBloc(i);
                break;
            }
        }
        surEtat.etat(courant, lignes, courant >= 0);
    }

    private List<String> lignesDuBloc(int bloc) {
        DiffBlocs diffs = DiffBlocs.de(reference, session.document().text());
        List<String> rendues = new java.util.ArrayList<>();
        for (DiffBlocs.LigneDiff ligne : diffs.blocs().get(bloc).lignes()) {
            rendues.add((ligne.type() == DiffBlocs.TypeLigne.ENLEVEE ? "- " : "+ ")
                    + ligne.texte());
        }
        return rendues;
    }
}
