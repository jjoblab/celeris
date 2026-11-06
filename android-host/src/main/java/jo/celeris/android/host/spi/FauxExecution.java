package jo.celeris.android.host.spi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La fausse exécution de référence (E-04) : les lancements vivent en
 * mémoire, les fins se simulent explicitement, chaque changement se
 * signale aux auditeurs — déterministe, pour les tests et la
 * démonstration.
 */
public final class FauxExecution implements IntegrationExecution {

    private final Map<CheminDoc, EtatExecution> etats = new LinkedHashMap<>();

    private final Map<String, Lancement> lancements = new LinkedHashMap<>();

    private final List<AuditeurExecution> auditeurs = new ArrayList<>();

    private long compteur;

    @Override
    public Lancement lancer(CheminDoc fichier, boolean test) {
        compteur++;
        Lancement lancement = new Lancement("lancement-" + compteur, fichier, test);
        lancements.put(lancement.id(), lancement);
        changer(fichier, EtatExecution.EN_COURS);
        return lancement;
    }

    @Override
    public EtatExecution etat(CheminDoc fichier) {
        return etats.getOrDefault(fichier, EtatExecution.PRET);
    }

    @Override
    public List<CheminDoc> etats() {
        return List.copyOf(etats.keySet());
    }

    @Override
    public void ajouterAuditeur(AuditeurExecution auditeur) {
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
        changer(lancement.fichier(), echec ? EtatExecution.ECHOUE : EtatExecution.TERMINE);
    }

    private void changer(CheminDoc fichier, EtatExecution etat) {
        etats.put(fichier, etat);
        for (AuditeurExecution auditeur : List.copyOf(auditeurs)) {
            auditeur.surChangement(fichier, etat);
        }
    }
}
