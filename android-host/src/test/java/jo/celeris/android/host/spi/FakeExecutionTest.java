package jo.celeris.android.host.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de la fausse exécution (E-04) : lancement signalé à l'auditeur,
 * état visible par fichier — les repères de gouttière s'y abreuvent.
 */
final class FakeExecutionTest {

    /** Enregistreur simple : les événements tombent dans une liste. */
    private static final class Enregistreur implements AuditeurExecution {
        private final List<String> evenements = new ArrayList<>();

        @Override
        public void surChangement(CheminDoc fichier, RunState state) {
            evenements.add(fichier.writing() + " : " + state);
        }

        List<String> evenements() {
            return evenements;
        }
    }

    /** Le lancement est signalé : l'auditeur voit l'état en cours. */
    @Test
    void lancementSignale() {
        FakeExecution execution = new FakeExecution();
        Enregistreur auditeur = new Enregistreur();
        execution.addListener(auditeur);

        Lancement lancement = execution.lancer(CheminDoc.of("/src/Main.java"), false);
        assertEquals(1, auditeur.evenements().size());
        assertEquals("/src/Main.java : EN_COURS", auditeur.evenements().get(0));
        assertEquals(RunState.EN_COURS, execution.state(CheminDoc.of("/src/Main.java")));
        assertEquals(false, lancement.test());
    }

    /** La fin d'exécution re-signe : terminée ou échouée, l'état suit. */
    @Test
    void finResignalee() {
        FakeExecution execution = new FakeExecution();
        Enregistreur auditeur = new Enregistreur();
        execution.addListener(auditeur);
        Lancement lancement = execution.lancer(CheminDoc.of("/src/Test.java"), true);

        execution.simulerFin(lancement, true);
        assertEquals(RunState.ECHOUE, execution.state(CheminDoc.of("/src/Test.java")));
        assertEquals(2, auditeur.evenements().size());
        assertEquals("/src/Test.java : ECHOUE", auditeur.evenements().get(1));

        Lancement reussi = execution.lancer(CheminDoc.of("/src/Test.java"), true);
        execution.simulerFin(reussi, false);
        assertEquals(RunState.TERMINE, execution.state(CheminDoc.of("/src/Test.java")));
    }

    /** Un fichier jamais lancé est prêt : aucune trace fantôme. */
    @Test
    void fichierJamaisLancePret() {
        FakeExecution execution = new FakeExecution();
        assertEquals(RunState.PRET, execution.state(CheminDoc.of("/a.java")));
        assertEquals(0, execution.states().size(), "seuls les fichiers lancés sont listés");
    }

    /** Plusieurs fichiers vivent côte à côte : les états ne se mélangent pas. */
    @Test
    void etatsIndependantsParFichier() {
        FakeExecution execution = new FakeExecution();
        Lancement un = execution.lancer(CheminDoc.of("/un.java"), false);
        execution.lancer(CheminDoc.of("/deux.java"), false);
        execution.simulerFin(un, false);

        assertEquals(RunState.TERMINE, execution.state(CheminDoc.of("/un.java")));
        assertEquals(RunState.EN_COURS, execution.state(CheminDoc.of("/deux.java")));
        assertEquals(2, execution.states().size());
    }
}
