package jo.celeris.android.view;

import java.util.Arrays;
import java.util.List;

import jo.celeris.core.syntax.FoldSpan;

/**
 * Marques de gouttière (D-02, D-07) : pliable, replié, erreur — les
 * trois états par ligne, servis par les mêmes consommateurs de l'arbre
 * (A-15 : régions de pli de l'arbre, D-02 : lignes en erreur du
 * diagnostic).
 *
 * <p>Immutable pour les régions pliables et les lignes en erreur
 * (tableaux triés, recherche dichotomique, sans allocation à la
 * consultation — PIEGE-15) ; l'état replié s'interroge AU VIVANT sur le
 * modèle de plis — basculer un pli se voit à l'image suivante sans
 * reconstruire les marques. Une ligne peut porter pli ET erreur — les
 * deux états coexistent, la voie du tracé décide de leur rendu.</p>
 */
public final class GutterMarks {

    /** Les marques vides : aucune ligne pliable, repliée ou en erreur. */
    public static final GutterMarks VIDE =
            new GutterMarks(new int[0], new FoldModel(), new int[0]);

    private final int[] pliables;
    private final FoldModel replis;
    private final int[] erreurs;

    private GutterMarks(int[] pliables, FoldModel replis, int[] erreurs) {
        this.pliables = pliables;
        this.replis = replis;
        this.erreurs = erreurs;
    }

    /**
     * Les marques des trois sources.
     *
     * @param regions      les régions pliables de l'arbre (A-15)
     * @param replis       le modèle de plis repliés, interrogé au vivant
     * @param lignesErreur les lignes porteuses d'un diagnostic (D-02)
     * @return les marques fondues
     */
    public static GutterMarks of(List<FoldSpan> regions, FoldModel replis,
            int[] lignesErreur) {
        int[] enTetes = new int[regions.size()];
        for (int i = 0; i < regions.size(); i++) {
            enTetes[i] = regions.get(i).startLine();
        }
        return new GutterMarks(triee(enTetes), replis, triee(lignesErreur));
    }

    /** Vrai si la ligne porte l'en-tête d'une région pliable de l'arbre. */
    public boolean pliable(int ligne) {
        return contient(pliables, ligne);
    }

    /** Vrai si la ligne porte un pli replié. */
    public boolean replie(int ligne) {
        return replis.isFolded(ligne);
    }

    /** Vrai si la ligne porte une marque d'erreur (D-02). */
    public boolean erreur(int ligne) {
        return contient(erreurs, ligne);
    }

    private static int[] triee(int[] valeurs) {
        int[] copie = valeurs.clone();
        Arrays.sort(copie);
        return copie;
    }

    private static boolean contient(int[] triee, int valeur) {
        int bas = 0;
        int haut = triee.length - 1;
        while (bas <= haut) {
            int milieu = (bas + haut) >>> 1;
            int courante = triee[milieu];
            if (courante < valeur) {
                bas = milieu + 1;
            } else if (courante > valeur) {
                haut = milieu - 1;
            } else {
                return true;
            }
        }
        return false;
    }
}
