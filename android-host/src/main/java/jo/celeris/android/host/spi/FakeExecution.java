package jo.celeris.android.host.spi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La fausse exécution de référence (E-04) : les lancements vivent en
 * mémoire, les fins se simulent explicitement, chaque changement se
 * signale aux auditeurs — déterministe, pour les tests et la
 * exemple d'intégration.
 */
public final class FakeExecution implements IntegrationExecution {

    private final Map<CheminDoc, RunState> states = new LinkedHashMap<>();

    private final Map<String, Lancement> lancements = new LinkedHashMap<>();

    private final List<AuditeurExecution> auditeurs = new ArrayList<>();

    private long compteur;

    @Override
    public Lancement lancer(CheminDoc fichier, boolean test) {
        compteur++;
        Lancement lancement = new Lancement("lancement-" + compteur, fichier, test);
        lancements.put(lancement.id(), lancement);
        change(fichier, RunState.EN_COURS);
        return lancement;
    }

    @Override
    public RunState state(CheminDoc fichier) {
        return states.getOrDefault(fichier, RunState.PRET);
    }

    @Override
    public List<CheminDoc> states() {
        return List.copyOf(states.keySet());
    }

    @Override
    public void addListener(AuditeurExecution auditeur) {
        auditeurs.add(auditeur);
    }

    /**
     * Simule la fin d'un lancement : l'état du fichier suit et les
     * auditeurs sont prévenus.
     *
     * @param lancement le lancement terminé
     * @param echec     vrai si l'exécution a échoué
     */
    public void simulerFin(Lancement lancement, boolean echec) {
        change(lancement.fichier(), echec ? RunState.ECHOUE : RunState.TERMINE);
    }

    private void change(CheminDoc fichier, RunState state) {
        states.put(fichier, state);
        for (AuditeurExecution auditeur : List.copyOf(auditeurs)) {
            auditeur.surChangement(fichier, state);
        }
    }
}
