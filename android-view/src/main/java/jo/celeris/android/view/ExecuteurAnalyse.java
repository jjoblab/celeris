package jo.celeris.android.view;

import java.util.concurrent.Executor;
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
 */
final class ExecuteurAnalyse {

    private ExecuteurAnalyse() {
    }

    /**
     * L'exécuteur par défaut de la vue : fil unique démon, nommé pour
     * le diagnostic.
     *
     * @return l'exécuteur du travail d'analyse
     */
    static Executor parDefaut() {
        return Executors.newSingleThreadExecutor(future -> {
            Thread fil = new Thread(future, "celeris-analyse");
            fil.setDaemon(true);
            return fil;
        });
    }
}
