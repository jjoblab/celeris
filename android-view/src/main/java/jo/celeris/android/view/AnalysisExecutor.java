package jo.celeris.android.view;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * L'exécuteur du travail d'analyse de la vue (ADR-007, G-01) : un fil
 * unique démon, créé paresseusement — le fil ne naît qu'à la première
 * tâche soumise, une vue sans langage n'ouvre aucun fil. La vue est la
 * racine de composition ; l'hôte qui veut partager son pool injecte le
 * sien au pont {@link EditorSyntax} directement.
 *
 * <p>L'annulation fait le ménage : chaque travail supplanté rend son
 * jeton au premier point de contrôle, le fil ne s'empile jamais de
 * passes périmées.</p>
 *
 * <p>Le service s'éteint (chantier C2) : la vue libérée arrête le fil
 * par {@code shutdownNow()} — les fils « celeris-analyse » ne
 * prolifèrent plus au fil des montées et libérations.</p>
 */
final class AnalysisExecutor {

    private AnalysisExecutor() {
    }

    /**
     * L'exécuteur par défaut de la vue : fil unique démon, nommé pour
     * le diagnostic, arrêtable à la libération (chantier C2).
     *
     * @return l'exécuteur du travail d'analyse
     */
    static ExecutorService parDefaut() {
        return Executors.newSingleThreadExecutor(future -> {
            Thread thread = new Thread(future, "celeris-analyse");
            thread.setDaemon(true);
            return thread;
        });
    }
}
