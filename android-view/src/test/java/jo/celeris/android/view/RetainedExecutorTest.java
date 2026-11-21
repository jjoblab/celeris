package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.android.view.internal.exec.RetainedExecutor;

/**
 * L'exécuteur RETENU du travail d'analyse (M7.5, §9 — le réglage de
 * lenteur livré) : la PREMIÈRE soumission part direct (audit H1 —
 * l'ouverture ne patiente pas), la rafale suivante ne laisse partir
 * que la DERNIÈRE tâche au calme du délai ; le délai nul rend la main
 * immédiate ; le délai se règle à chaud et atteint toutes les retenues
 * dérivées par moteur. Pur, sans horloge réelle — le minuteur s'injecte.
 */
class RetainedExecutorTest {

    /** Le minuteur d'essai : capture les planifiées, rend leurs
     *  annulations — une par planification, comme le minuteur démon. */
    private static final class MinuteurEssai implements RetainedExecutor.Delayer {

        private final List<Runnable> planifiees = new ArrayList<>();
        private long dernierDelaiMs = -1L;
        private int annulations;

        @Override
        public Runnable schedule(long delayMs, Runnable task) {
            planifiees.add(task);
            dernierDelaiMs = delayMs;
            return () -> {
                if (planifiees.remove(task)) {
                    annulations++;
                }
            };
        }

        @Override
        public void shutdown() {
            // le minuteur de banc ne pose aucun fil
        }

        /** Le terme arrive : toutes les planifiées partent. */
        void expire() {
            List<Runnable> enAttente = new ArrayList<>(planifiees);
            planifiees.clear();
            enAttente.forEach(Runnable::run);
        }
    }

    /** L'exécuteur de base d'essai : enregistre les passages. */
    private static final class BaseEssai implements java.util.concurrent.Executor {

        private final List<String> passages = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            command.run();
            passages.add("passage");
        }
    }

    /**
     * La première soumission part DIRECT (audit H1) : rien ne se
     * programme, le travail commence immédiatement — la rafale
     * suivante, elle, se retient et ne laisse partir que la dernière.
     */
    @Test
    void premiereSoumissionDirectePuisRafaleRetenue() {
        MinuteurEssai minuteur = new MinuteurEssai();
        BaseEssai base = new BaseEssai();
        RetainedExecutor executor = new RetainedExecutor(base, minuteur, 250L);

        executor.execute(() -> { });

        assertEquals(1, base.passages.size(),
                "la première soumission part direct — l'ouverture ne "
                        + "patienta pas la retenue");
        assertEquals(-1L, minuteur.dernierDelaiMs,
                "rien ne se programme à la première");

        executor.execute(() -> { });
        executor.execute(() -> { });

        assertEquals(1, minuteur.annulations,
                "chaque nouvelle soumission retire la planifiée");
        assertEquals(250L, minuteur.dernierDelaiMs,
                "le délai se programme tel quel");
        minuteur.expire();
        assertEquals(2, base.passages.size(),
                "une seule tâche de la rafale part, vers la base");
    }

    /** Le délai nul rend la main immédiate : rien ne se programme. */
    @Test
    void delaiNulRendLaMainImmediate() {
        MinuteurEssai minuteur = new MinuteurEssai();
        BaseEssai base = new BaseEssai();
        RetainedExecutor executor = new RetainedExecutor(base, minuteur, 0L);

        executor.execute(() -> { });
        executor.execute(() -> { });

        assertEquals(2, base.passages.size(), "les tâches partent immédiatement");
        assertEquals(-1L, minuteur.dernierDelaiMs, "rien ne se programme");
    }

    /** Le délai se règle à chaud : la soumission suivante le suit. */
    @Test
    void delaiSeRegleAChaud() {
        MinuteurEssai minuteur = new MinuteurEssai();
        RetainedExecutor executor = new RetainedExecutor(
                new BaseEssai(), minuteur, 250L);

        assertEquals(250L, executor.delayMs());
        executor.setDelayMs(600L);
        assertEquals(600L, executor.delayMs());
        executor.execute(() -> { });
        executor.execute(() -> { });
        assertEquals(600L, minuteur.dernierDelaiMs,
                "la reprogrammation suit le nouveau délai");
        // le délai négatif se borne : jamais de retenue négative
        executor.setDelayMs(-5L);
        assertEquals(0L, executor.delayMs());
        assertTrue(executor.delayMs() >= 0L);
    }

    /**
     * La retenue se dérive PAR MOTEUR (audit H2) : deux dérivées
     * partagent base et minuteur, mais leurs créneaux ne se touchent
     * pas — la soumission de l'une ne supprime pas la tâche de
     * l'autre, et le réglage du délai les atteint toutes.
     */
    @Test
    void lesRetenuesParMoteurNeSeSupprimentPas() {
        MinuteurEssai minuteur = new MinuteurEssai();
        BaseEssai base = new BaseEssai();
        RetainedExecutor modele = new RetainedExecutor(base, minuteur, 250L);
        RetainedExecutor moteurA = modele.pourMoteur();
        RetainedExecutor moteurB = modele.pourMoteur();

        moteurA.execute(() -> { });
        moteurB.execute(() -> { });
        moteurA.execute(() -> { });
        moteurB.execute(() -> { });

        assertEquals(0, minuteur.annulations,
                "les créneaux sont indépendants : la soumission de B "
                        + "ne supprime jamais la tâche de A");
        assertEquals(2, minuteur.planifiees.size(),
                "chaque moteur garde SA tâche en attente");
        minuteur.expire();
        assertEquals(4, base.passages.size(),
                "les deux moteurs ont servi leurs tâches");

        modele.setDelayMs(600L);
        assertEquals(600L, moteurA.delayMs(),
                "le réglage de la vue atteint les retenues dérivées");
        assertEquals(600L, moteurB.delayMs());
    }
}
