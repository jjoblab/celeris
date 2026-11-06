package jo.celeris.android.host.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de la fausse exécution (E-04) : lancement signalé à l'auditeur,
 * état visible par fichier — les repères de gouttière s'y abreuvent.
 */
final class FauxExecutionTest {

    /** Enregistreur simple : les événements tombent dans une liste. */
    private static final class Enregistreur implements AuditeurExecution {
        private final List<String> evenements = new ArrayList<>();

        @Override
        public void surChangement(CheminDoc fichier, EtatExecution etat) {
            evenements.add(fichier.ecriture() + " : " + etat);
        }

        List<String> evenements() {
            return evenements;
        }
    }

    /** Le lancement est signalé : l'auditeur voit l'état en cours. */
    @Test
    void lancementSignale() {
        FauxExecution execution = new FauxExecution();
        Enregistreur auditeur = new Enregistreur();
        execution.ajouterAuditeur(auditeur);

        Lancement lancement = execution.lancer(CheminDoc.de("/src/Main.java"), false);
        assertEquals(1, auditeur.evenements().size());
        assertEquals("/src/Main.java : EN_COURS", auditeur.evenements().get(0));
        assertEquals(EtatExecution.EN_COURS, execution.etat(CheminDoc.de("/src/Main.java")));
        assertEquals(false, lancement.test());
    }

    /** La fin d'exécution re-signe : terminée ou échouée, l'état suit. */
    @Test
    void finResignalee() {
        FauxExecution execution = new FauxExecution();
        Enregistreur auditeur = new Enregistreur();
        execution.ajouterAuditeur(auditeur);
        Lancement lancement = execution.lancer(CheminDoc.de("/src/Test.java"), true);

        execution.simulerFin(lancement, true);
        assertEquals(EtatExecution.ECHOUE, execution.etat(CheminDoc.de("/src/Test.java")));
        assertEquals(2, auditeur.evenements().size());
        assertEquals("/src/Test.java : ECHOUE", auditeur.evenements().get(1));

        Lancement reussi = execution.lancer(CheminDoc.de("/src/Test.java"), true);
        execution.simulerFin(reussi, false);
        assertEquals(EtatExecution.TERMINE, execution.etat(CheminDoc.de("/src/Test.java")));
    }

    /** Un fichier jamais lancé est prêt : aucune trace fantôme. */
    @Test
    void fichierJamaisLancePret() {
        FauxExecution execution = new FauxExecution();
        assertEquals(EtatExecution.PRET, execution.etat(CheminDoc.de("/a.java")));
        assertEquals(0, execution.etats().size(), "seuls les fichiers lancés sont listés");
    }

    /** Plusieurs fichiers vivent côte à côte : les états ne se mélangent pas. */
    @Test
    void etatsIndependantsParFichier() {
        FauxExecution execution = new FauxExecution();
        Lancement un = execution.lancer(CheminDoc.de("/un.java"), false);
        execution.lancer(CheminDoc.de("/deux.java"), false);
        execution.simulerFin(un, false);

        assertEquals(EtatExecution.TERMINE, execution.etat(CheminDoc.de("/un.java")));
        assertEquals(EtatExecution.EN_COURS, execution.etat(CheminDoc.de("/deux.java")));
        assertEquals(2, execution.etats().size());
    }
}
