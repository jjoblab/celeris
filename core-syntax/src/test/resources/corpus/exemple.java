package jo.celeris.demo.exemple;

import java.util.List;
import java.util.Map;

/**
 * Exemple de corpus Java original — exerce les constructions que
 * l'analyseur doit couvrir : déclarations, membres, blocs imbriqués,
 * commentaires de toutes sortes, annotations, génériques, textes en
 * bloc, énumérations, records et classes internes.
 */
public final class ExempleJava {

    /** Constante de classe, en majuscules. */
    private static final int LIMITE = 512;

    private final Map<String, List<Integer>> index;
    private int compteur;

    public ExempleJava(Map<String, List<Integer>> index) {
        this.index = index;
        this.compteur = 0;
    }

    /**
     * Additionne les valeurs de la fenêtre.
     *
     * @param valeurs la fenêtre à additionner
     * @return la somme bornée à {@link #LIMITE}
     */
    public int additionne(List<Integer> valeurs) {
        int somme = 0;
        for (int valeur : valeurs) {
            if (somme > LIMITE) {
                break;
            }
            somme += valeur;
        }
        return somme;
    }

    public String decrit(String cle) {
        return """
                la cle %s porte %d valeurs
                """.formatted(cle, index.get(cle).size());
    }

    private void reindexe() {
        for (Map.Entry<String, List<Integer>> entree : index.entrySet()) {
            List<Integer> valeurs = entree.getValue();
            if (valeurs.isEmpty()) {
                continue;
            }
            while (compteur < valeurs.size()) {
                compteur++;
            }
        }
    }

    /** Énumération imbriquée. */
    public enum Etat {
        /** État initial. */
        ATTENTE,
        /** État actif. */
        ACTIF
    }

    /** Record imbriqué. */
    public record Point(int x, int y) {
        public double norme() {
            return Math.sqrt(x * x + y * y);
        }
    }

    static final class Interne {
        private final String nom;

        Interne(String nom) {
            this.nom = nom;
        }
    }
}
