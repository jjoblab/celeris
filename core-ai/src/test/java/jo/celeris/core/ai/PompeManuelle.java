package jo.celeris.core.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Pompe manuelle : un exécuteur de test dont les tâches attendent qu'on
 * les exécute — le temps des livraisons devient déterministe, aucun
 * sommeil nulle part (ADR-007). Chaque pompe est son exécuteur : le
 * modèle distant y jouera le réseau, la session y jouera son fil de
 * publication.
 */
final class PompeManuelle implements Executor {

    private final List<Runnable> taches = new ArrayList<>();

    @Override
    public void execute(Runnable tache) {
        taches.add(tache);
    }

    /** Exécute la plus ancienne tâche en attente, s'il y en a une. */
    void executerUne() {
        if (taches.isEmpty()) {
            throw new IllegalStateException("no pending task");
        }
        taches.remove(0).run();
    }

    /** Exécute toutes les tâches en attente, dans l'ordre d'arrivée. */
    void executerTout() {
        while (!taches.isEmpty()) {
            executerUne();
        }
    }

    /** Le nombre de tâches en attente. */
    int enAttente() {
        return taches.size();
    }
}
