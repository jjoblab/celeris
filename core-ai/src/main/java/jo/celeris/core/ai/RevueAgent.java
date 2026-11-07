package jo.celeris.core.ai;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

/**
 * La revue des modifications d'agent (F-04, ADR-012) : la proposition
 * se découpe en blocs, chaque bloc se décide seul — accepté, refusé ou
 * en attente — et le texte fusionné suit les décisions.
 *
 * <p>Le refus d'un bloc réaligne les suivants : les éditions vivant en
 * coordonnées du document de référence (voir {@link PropositionAgent}),
 * sauter un bloc ne décale rien — les acceptées s'appliquent pareil,
 * l'ordre du document décide. Un point de contrôle
 * ({@link #pointDeControle()}) capture l'état complet des décisions ;
 * y revenir restaure la revue telle qu'elle était.</p>
 *
 * <p>L'application se prend par la voie de transaction ordinaire : une
 * seule transaction, donc une seule étape d'annulation — le rejet d'un
 * geste annule la fusion entière, jamais bloc par bloc. Le document
 * qui a bougé depuis la proposition refuse le transfert,
 * explicitement : jamais de fusion silencieuse (ADR-012).</p>
 */
public final class RevueAgent {

    /** La décision d'un bloc. */
    public enum Decision {

        /** Le bloc attend sa décision. */
        EN_ATTENTE,

        /** Le bloc est accepté : ses éditions s'appliquent. */
        ACCEPTE,

        /** Le bloc est refusé : le texte de référence y reste. */
        REFUSE
    }

    /** Le sort d'une application de revue. */
    public enum Sortie {

        /** Appliquée en transaction ordinaire — annulable, une étape. */
        APPLIQUEE,

        /** Refusée : le document a bougé depuis la proposition. */
        REFUSEE_REVISION,

        /** Refusée : la session est protégée en écriture. */
        REFUSEE_PROTECTION,

        /** Rien à appliquer : aucun bloc accepté. */
        VIDE
    }

    private final Document reference;
    private final PropositionAgent proposition;
    private final List<List<TextEdit>> blocs;
    private final List<Decision> decisions;

    private RevueAgent(Document reference, PropositionAgent proposition,
            List<List<TextEdit>> blocs, List<Decision> decisions) {
        this.reference = reference;
        this.proposition = proposition;
        this.blocs = blocs;
        this.decisions = List.copyOf(decisions);
    }

    /**
     * Ouvre la revue de la proposition sur son document de référence :
     * les éditions contiguës forment un même bloc, tous les blocs
     * attendent.
     *
     * @param reference   l'instantané de référence de la proposition
     * @param proposition la proposition à révise
     * @return la revue ouverte
     * @throws IllegalArgumentException si la proposition ne date pas de
     *         la révision du document donné
     */
    public static RevueAgent de(Document reference, PropositionAgent proposition) {
        if (proposition.revision() != reference.revision()) {
            throw new IllegalArgumentException(
                    "la proposition date de la revision "
                            + proposition.revision() + ", le document est a "
                            + reference.revision());
        }
        List<List<TextEdit>> blocs = decouperEnBlocs(proposition.edits());
        List<Decision> attente = new ArrayList<>();
        for (int i = 0; i < blocs.size(); i++) {
            attente.add(Decision.EN_ATTENTE);
        }
        return new RevueAgent(reference, proposition, blocs, attente);
    }

    /**
     * Le découpage en blocs : des éditions contiguës — celle qui suit
     * touche la plage de la précédente — forment un même bloc, la
     * matière du changement se lit d'un tenant.
     */
    private static List<List<TextEdit>> decouperEnBlocs(List<TextEdit> edits) {
        List<List<TextEdit>> blocs = new ArrayList<>();
        List<TextEdit> courant = new ArrayList<>();
        int finPrecedente = -1;
        for (TextEdit edition : edits) {
            if (!courant.isEmpty() && edition.start() > finPrecedente) {
                blocs.add(List.copyOf(courant));
                courant = new ArrayList<>();
            }
            courant.add(edition);
            finPrecedente = edition.end();
        }
        if (!courant.isEmpty()) {
            blocs.add(List.copyOf(courant));
        }
        return List.copyOf(blocs);
    }

    /**
     * Le nombre de blocs de la revue.
     *
     * @return le nombre de blocs
     */
    public int nombreDeBlocs() {
        return blocs.size();
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
     * L'étiquette de la proposition en revue — l'auteur et
     * l'intention, lus au moment de décider.
     *
     * @return l'étiquette de la proposition
     */
    public EtiquetteAgent etiquette() {
        return proposition.etiquette();
    }

    /**
     * Décide un bloc : accepté, ses éditions s'appliquent au texte
     * fusionné ; refusé, le texte de référence y reste. La décision
     * d'un bloc déjà décidé se remplace.
     *
     * @param bloc     l'index du bloc, à partir de zéro
     * @param decision la décision à poser
     * @return la revue décidée — un nouvel état, l'ancien survit
     */
    public RevueAgent decider(int bloc, Decision decision) {
        List<Decision> posees = new ArrayList<>(decisions);
        posees.set(bloc, decision);
        return new RevueAgent(reference, proposition, blocs, posees);
    }

    /**
     * Les éditions d'un bloc, dans l'ordre du document.
     *
     * @param bloc l'index du bloc, à partir de zéro
     * @return les éditions du bloc, en lecture seule
     */
    public List<TextEdit> editionsDuBloc(int bloc) {
        return blocs.get(bloc);
    }

    /**
     * La lecture en ligne d'un bloc : chaque texte retiré rend ses
     * lignes préfixées « - », chaque texte inséré ses lignes
     * préfixées « + » — la matière du diff en ligne par blocs
     * acceptés/refusés (F-04).
     *
     * @param bloc l'index du bloc, à partir de zéro
     * @return les lignes préfixées, dans l'ordre du document
     */
    public List<String> lignesDuBloc(int bloc) {
        List<String> lignes = new ArrayList<>();
        for (TextEdit edition : blocs.get(bloc)) {
            for (String ligne : couperEnLignes(edition.removed())) {
                lignes.add("- " + ligne);
            }
            for (String ligne : couperEnLignes(edition.inserted())) {
                lignes.add("+ " + ligne);
            }
        }
        return List.copyOf(lignes);
    }

    /**
     * Le texte fusionné courant : les éditions des blocs acceptés
     * appliquées au document de référence, les blocs refusés ou en
     * attente sautés — le refus d'un bloc réaligne les suivants, les
     * plages de référence ne bougent jamais.
     *
     * @return le texte après les décisions courantes
     */
    public String texte() {
        StringBuilder fusion = new StringBuilder();
        int curseur = 0;
        for (int i = 0; i < blocs.size(); i++) {
            if (decisions.get(i) != Decision.ACCEPTE) {
                continue;
            }
            for (TextEdit edition : blocs.get(i)) {
                fusion.append(reference.subText(curseur, edition.start()));
                fusion.append(edition.inserted());
                curseur = edition.end();
            }
        }
        fusion.append(reference.subText(curseur, reference.length()));
        return fusion.toString();
    }

    /**
     * Le point de contrôle de l'état courant : y revenir restaure les
     * décisions telles quelles.
     *
     * @return le point de contrôle, opaque
     */
    public RevueAgent pointDeControle() {
        return this;
    }

    /**
     * Le retour au point de contrôle : l'état enregistré redevient
     * l'état courant.
     *
     * @param controle le point de contrôle rendu par
     *                 {@link #pointDeControle()}
     * @return la revue revenue à l'état du point de contrôle
     */
    public RevueAgent retourAuPointDeControle(RevueAgent controle) {
        return controle;
    }

    /**
     * Applique la fusion courante sur la session : les éditions
     * acceptées deviennent une seule transaction — une étape
     * d'annulation, annulable d'un geste. Le document qui a bougé
     * depuis la proposition refuse le transfert, explicitement.
     *
     * @param session la session à muter, sur son fil d'écriture
     * @return le sort de l'application
     */
    public Sortie appliquer(TextSession session) {
        List<TextEdit> acceptees = editionsAcceptees();
        if (acceptees.isEmpty()) {
            return Sortie.VIDE;
        }
        if (proposition.revision() != session.document().revision()) {
            return Sortie.REFUSEE_REVISION;
        }
        if (session.isReadOnly()) {
            return Sortie.REFUSEE_PROTECTION;
        }
        // Les coordonnées de référence deviennent séquentielles : chaque
        // édition acceptée décale les suivantes de son delta.
        List<TextEdit> sequentielles = new ArrayList<>(acceptees.size());
        int delta = 0;
        for (TextEdit edition : acceptees) {
            sequentielles.add(new TextEdit(edition.start() + delta,
                    edition.removed(), edition.inserted()));
            delta += edition.inserted().length() - edition.removed().length();
        }
        boolean appliquee = session.apply(Transaction.of(sequentielles));
        return appliquee ? Sortie.APPLIQUEE : Sortie.REFUSEE_PROTECTION;
    }

    /** Les éditions des blocs acceptés, dans l'ordre du document. */
    private List<TextEdit> editionsAcceptees() {
        List<TextEdit> acceptees = new ArrayList<>();
        for (int i = 0; i < blocs.size(); i++) {
            if (decisions.get(i) == Decision.ACCEPTE) {
                acceptees.addAll(blocs.get(i));
            }
        }
        return acceptees;
    }

    /**
     * Coupe un texte en lignes : le texte vide ne rend aucune ligne,
     * le saut final n'en ouvre pas une vide.
     */
    private static List<String> couperEnLignes(String texte) {
        if (texte.isEmpty()) {
            return List.of();
        }
        List<String> lignes = new ArrayList<>();
        int debut = 0;
        for (int i = 0; i < texte.length(); i++) {
            if (texte.charAt(i) == '\n') {
                lignes.add(texte.substring(debut, i));
                debut = i + 1;
            }
        }
        if (debut < texte.length()) {
            lignes.add(texte.substring(debut));
        }
        return lignes;
    }
}
