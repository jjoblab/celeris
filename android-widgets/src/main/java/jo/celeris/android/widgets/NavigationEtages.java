package jo.celeris.android.widgets;

import java.util.function.IntPredicate;

/**
 * La navigation clavier du menu à étages (l'audit du propriétaire du
 * 2026-11-14) : la flèche SAUTE les en-têtes — jamais d'arrêt sur une
 * section. Pure et testable sans appareil : la table des en-têtes se
 * passe en prédicat, le saut se calcule seul.
 */
final class NavigationEtages {

    private NavigationEtages() {
    }

    /**
     * La rangée visée par la flèche : la PREMIÈRE flèche PREND la
     * première action (elle ne la saute pas), les suivantes avancent
     * d'autant en sautant les en-têtes.
     *
     * @param nombre    le nombre de rangées du menu
     * @param surligne  la rangée surlignée, —1 si aucune
     * @param pas       le pas de la flèche (signe = direction)
     * @param estEntete le prédicat d'en-tête d'étage (jamais pris)
     * @return l'index visé, —1 quand le saut ne tombe sur aucune action
     */
    static int saut(int nombre, int surligne, int pas,
            IntPredicate estEntete) {
        if (nombre == 0) {
            return -1;
        }
        int cible = surligne < 0 ? premiereAction(nombre, estEntete)
                : surligne;
        int pasRestant = surligne < 0 ? Math.abs(pas) - 1 : Math.abs(pas);
        int direction = pas >= 0 ? 1 : -1;
        while (pasRestant > 0) {
            cible += direction;
            if (cible < 0 || cible >= nombre) {
                break;
            }
            if (!estEntete.test(cible)) {
                pasRestant--;
            }
        }
        if (cible < 0 || cible >= nombre || estEntete.test(cible)) {
            return -1;
        }
        return cible;
    }

    /** La première rangée d'ACTION du menu — la navigation y part. */
    private static int premiereAction(int nombre, IntPredicate estEntete) {
        for (int i = 0; i < nombre; i++) {
            if (!estEntete.test(i)) {
                return i;
            }
        }
        return 0;
    }
}
