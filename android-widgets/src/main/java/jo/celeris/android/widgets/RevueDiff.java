package jo.celeris.android.widgets;

import java.util.ArrayList;
import java.util.List;

/**
 * La revue d'un diff par blocs (D-12, F-04) : chaque bloc se décide
 * seul — accepté, refusé, ou en attente —, le texte fusionné suit les
 * décisions. Le refus d'un bloc réaligne les suivants : les plages se
 * recalculent sur le texte courant, jamais sur des positions périmées.
 *
 * <p>Un point de contrôle ({@link #pointDeControle()}) capture l'état
 * complet des décisions — y revenir restaure la revue telle qu'elle
 * était, décisions et texte compris.</p>
 */
public final class RevueDiff {

    /** La décision d'un bloc. */
    public enum Decision {

        /** Le bloc attend sa décision. */
        EN_ATTENTE,

        /** Le bloc est accepté : ses lignes s'appliquent. */
        ACCEPTE,

        /** Le bloc est refusé : l'ancien texte y reste. */
        REFUSE
    }

    private final String ancien;
    private final String nouveau;
    private final List<Decision> decisions;

    private RevueDiff(String ancien, String nouveau, List<Decision> decisions) {
        this.ancien = ancien;
        this.nouveau = nouveau;
        this.decisions = List.copyOf(decisions);
    }

    /**
     * La revue du diff des deux textes : tous les blocs attendent.
     *
     * @param ancien  l'ancien texte
     * @param nouveau le nouveau texte
     * @return la revue ouverte
     */
    public static RevueDiff de(String ancien, String nouveau) {
        DiffBlocs blocs = DiffBlocs.de(ancien, nouveau);
        List<Decision> attente = new ArrayList<>();
        for (int i = 0; i < blocs.blocs().size(); i++) {
            attente.add(Decision.EN_ATTENTE);
        }
        return new RevueDiff(ancien, nouveau, attente);
    }

    /**
     * Le nombre de blocs de la revue.
     *
     * @return le nombre de blocs
     */
    public int nombreDeBlocs() {
        return decisions.size();
    }

    /**
     * La décision courante d'un bloc.
     *
     * @param bloc l'index du bloc, à partir de zéro
     * @return la décision, jamais null
     */
    public Decision decision(int bloc) {
        return decisions.get(bloc);
    }

    /**
     * Décide un bloc : accepté, ses lignes s'appliquent au texte
     * fusionné ; refusé, l'ancien texte y reste. La décision d'un bloc
     * déjà décidé se remplace.
     *
     * @param bloc     l'index du bloc, à partir de zéro
     * @param decision la décision à poser
     * @return la revue décidée — un nouvel état, l'ancien survit
     */
    public RevueDiff decider(int bloc, Decision decision) {
        List<Decision> posees = new ArrayList<>(decisions);
        posees.set(bloc, decision);
        return new RevueDiff(ancien, nouveau, posees);
    }

    /**
     * Le texte fusionné courant : les lignes des blocs acceptés, les
     * anciennes lignes des blocs refusés ou en attente — le refus d'un
     * bloc réaligne les suivants, l'application suit l'ordre du texte.
     *
     * @return le texte après les décisions courantes
     */
    public String texte() {
        List<String> anciennes = lignes(ancien);
        List<String> nouvelles = lignes(nouveau);
        List<DiffBlocs.BlocDiff> blocs = DiffBlocs.de(ancien, nouveau).blocs();
        List<String> fusion = new ArrayList<>();
        int ligneAncienne = 0;
        for (int i = 0; i < blocs.size(); i++) {
            DiffBlocs.BlocDiff bloc = blocs.get(i);
            // Les lignes communes depuis la position courante jusqu'au
            // bloc : elles traversent toujours.
            while (ligneAncienne < bloc.debutAncien()) {
                fusion.add(anciennes.get(ligneAncienne));
                ligneAncienne++;
            }
            if (decisions.get(i) == Decision.ACCEPTE) {
                for (int j = bloc.debutNouveau(); j < bloc.finNouveau(); j++) {
                    fusion.add(nouvelles.get(j));
                }
            } else {
                for (int j = bloc.debutAncien(); j < bloc.finAncien(); j++) {
                    fusion.add(anciennes.get(j));
                }
            }
            ligneAncienne = bloc.finAncien();
        }
        while (ligneAncienne < anciennes.size()) {
            fusion.add(anciennes.get(ligneAncienne));
            ligneAncienne++;
        }
        return String.join("\n", fusion);
    }

    /**
     * Le point de contrôle de l'état courant : y revenir restaure les
     * décisions telles quelles.
     *
     * @return le point de contrôle, opaque
     */
    public RevueDiff pointDeControle() {
        return this;
    }

    /**
     * Le retour au point de contrôle : l'état enregistré redevient
     * l'état courant.
     *
     * @param controle le point de contrôle rendu par {@link #pointDeControle()}
     * @return la revue revenue à l'état du point de contrôle
     */
    public RevueDiff retourAuPointDeControle(RevueDiff controle) {
        return controle;
    }

    private static List<String> lignes(String texte) {
        List<String> lignes = new ArrayList<>();
        int debut = 0;
        for (int i = 0; i < texte.length(); i++) {
            if (texte.charAt(i) == '\n') {
                lignes.add(texte.substring(debut, i));
                debut = i + 1;
            }
        }
        lignes.add(texte.substring(debut));
        return lignes;
    }
}
