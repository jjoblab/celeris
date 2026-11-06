package jo.celeris.android.host.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * L'implémentation de référence du système de fichiers virtuel (E-01) :
 * tout en mémoire, déterministe, pour les tests et les faux de
 * démonstration. L'ordre de listage est l'ordre lexicographique des
 * écritures — stable par construction.
 */
public final class MemoireSystemeFichiers implements SystemeFichiersVirtuel {

    private final Map<CheminDoc, String> fichiers = new TreeMap<>(
            (a, b) -> a.ecriture().compareTo(b.ecriture()));

    /** Construit un système vide : seule la racine existe. */
    public MemoireSystemeFichiers() {
    }

    @Override
    public FichierVirtuel ouvrir(CheminDoc chemin) {
        String contenu = fichiers.get(chemin);
        if (contenu == null) {
            throw new FichierAbsentException(chemin);
        }
        return new FichierVirtuel(chemin, contenu);
    }

    @Override
    public void ecrire(CheminDoc chemin, String contenu) {
        fichiers.put(chemin, contenu);
    }

    @Override
    public List<CheminDoc> lister(CheminDoc dossier) {
        List<CheminDoc> enfants = new ArrayList<>();
        for (CheminDoc chemin : fichiers.keySet()) {
            if (chemin.parent().equals(dossier)) {
                enfants.add(chemin);
            }
        }
        return enfants;
    }

    @Override
    public boolean estFichier(CheminDoc chemin) {
        return fichiers.containsKey(chemin);
    }
}
