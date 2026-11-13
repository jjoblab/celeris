package jo.celeris.android.widgets;

import java.util.ArrayList;
import java.util.List;

/**
 * La différence ligne à ligne par blocs (D-12) : la plus longue
 * sous-suite commune des deux textes découpe le reste en blocs de
 * lignes — enlevées, ajoutées, remplacées — aux plages exactes des
 * deux côtés. Chaque bloc vit seul : la revue l'accepte ou le refuse
 * sans toucher aux autres ({@link RevueDiff}), le réalignement suit
 * l'ordre d'application.
 *
 * <p>La sortie « en ligne » juxtapose enlevées et ajoutées d'un même
 * bloc, la sortie « côte à côte » aligne les deux textes en colonnes
 * de largeur fixe — les deux lectures du même diff.</p>
 */
public final class DiffBlocs {

    /** Le type d'une ligne du diff. */
    public enum TypeLigne {

        /** La ligne existe dans l'ancien texte seulement. */
        ENLEVEE,

        /** La ligne existe dans le nouveau texte seulement. */
        AJOUTEE,

        /** La ligne existe dans les deux textes. */
        INCHANGE
    }

    /** Une ligne du diff : son type, ses lignes d'origine et son texte. */
    public record LineDiff(TypeLigne type, int ligneAncienne, int ligneNouvelle,
            String texte) {
    }

    /**
     * Un bloc du diff : les lignes enlevées puis ajoutées qui se
     * suivent, aux plages exactes des deux textes.
     *
     * @param debutAncien la première ligne enlevée, dans l'ancien
     * @param finAncien   la fin exclusive des lignes enlevées
     * @param debutNouveau la première ligne ajoutée, dans le nouveau
     * @param finNouveau  la fin exclusive des lignes ajoutées
     * @param lignes      les lignes du bloc, enlevées puis ajoutées
     */
    public record BlocDiff(int debutAncien, int finAncien, int debutNouveau,
            int finNouveau, List<LineDiff> lignes) {
    }

    private final List<BlocDiff> blocs;

    private DiffBlocs(List<BlocDiff> blocs) {
        this.blocs = List.copyOf(blocs);
    }

    /**
     * Le diff des deux textes, ligne à ligne, par blocs : les lignes
     * communes servent d'ancre, chaque intervalle entre deux ancres
     * consecutives porte les lignes enlevées puis ajoutées — un bloc
     * par changement, indépendant des autres.
     *
     * @param ancien  l'ancien texte
     * @param nouveau le nouveau texte
     * @return les blocs, dans l'ordre du texte
     */
    public static DiffBlocs de(String ancien, String nouveau) {
        List<String> anciennes = lignes(ancien);
        List<String> nouvelles = lignes(nouveau);
        List<int[]> ancres = ancres(anciennes, nouvelles);
        List<BlocDiff> blocs = new ArrayList<>();
        int ancreAncienne = -1;
        int ancreNouvelle = -1;
        // L'ancre virtuelle finale ferme le dernier bloc : les lignes
        // après la dernière ancre réelle en font partie.
        for (int rang = 0; rang <= ancres.size(); rang++) {
            int suivantAncien = rang < ancres.size() ? ancres.get(rang)[0]
                    : anciennes.size();
            int suivantNouveau = rang < ancres.size() ? ancres.get(rang)[1]
                    : nouvelles.size();
            if (suivantAncien > ancreAncienne + 1
                    || suivantNouveau > ancreNouvelle + 1) {
                List<LineDiff> lignesBloc = new ArrayList<>();
                for (int i = ancreAncienne + 1; i < suivantAncien; i++) {
                    lignesBloc.add(new LineDiff(TypeLigne.ENLEVEE, i, -1,
                            anciennes.get(i)));
                }
                for (int j = ancreNouvelle + 1; j < suivantNouveau; j++) {
                    lignesBloc.add(new LineDiff(TypeLigne.AJOUTEE, -1, j,
                            nouvelles.get(j)));
                }
                blocs.add(new BlocDiff(ancreAncienne + 1, suivantAncien,
                        ancreNouvelle + 1, suivantNouveau, lignesBloc));
            }
            if (rang < ancres.size()) {
                ancreAncienne = ancres.get(rang)[0];
                ancreNouvelle = ancres.get(rang)[1];
            }
        }
        return new DiffBlocs(blocs);
    }

    /**
     * Les ancres de la plus longue sous-suite commune : les paires de
     * lignes égales que le chemin traverse, dans l'ordre.
     */
    private static List<int[]> ancres(List<String> anciennes,
            List<String> nouvelles) {
        int[][] longueurs = new int[anciennes.size() + 1][nouvelles.size() + 1];
        for (int i = anciennes.size() - 1; i >= 0; i--) {
            for (int j = nouvelles.size() - 1; j >= 0; j--) {
                longueurs[i][j] = anciennes.get(i).equals(nouvelles.get(j))
                        ? longueurs[i + 1][j + 1] + 1
                        : Math.max(longueurs[i + 1][j], longueurs[i][j + 1]);
            }
        }
        List<int[]> ancres = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < anciennes.size() && j < nouvelles.size()) {
            if (anciennes.get(i).equals(nouvelles.get(j))) {
                ancres.add(new int[] {i, j});
                i++;
                j++;
            } else if (longueurs[i + 1][j] >= longueurs[i][j + 1]) {
                i++;
            } else {
                j++;
            }
        }
        return ancres;
    }

    /**
     * Les blocs du diff, dans l'ordre du texte.
     *
     * @return les blocs, non modifiables
     */
    public List<BlocDiff> blocs() {
        return blocs;
    }

    /**
     * La lecture en ligne : chaque bloc rend ses lignes préfixées —
     * « - » pour une enlevée, « + » pour une ajoutée.
     *
     * @return les lignes préfixées du diff complet
     */
    public List<String> enLigne() {
        List<String> rendues = new ArrayList<>();
        for (BlocDiff bloc : blocs) {
            for (LineDiff ligne : bloc.lignes()) {
                rendues.add((ligne.type() == TypeLigne.ENLEVEE ? "- " : "+ ")
                        + ligne.texte());
            }
        }
        return rendues;
    }

    /**
     * La lecture côte à côte : les lignes enlevées et ajoutées d'un
     * même bloc s'alignent en deux colonnes de la largeur donnée —
     * la gauche ancienne, la droite nouvelle, les trous en tirets.
     *
     * @param largeur la largeur de chaque colonne, en caractères
     * @return les lignes des deux colonnes, collées
     */
    public List<String> coteACote(int largeur) {
        List<String> rendues = new ArrayList<>();
        for (BlocDiff bloc : blocs) {
            List<LineDiff> enlevees = new ArrayList<>();
            List<LineDiff> ajoutees = new ArrayList<>();
            for (LineDiff ligne : bloc.lignes()) {
                if (ligne.type() == TypeLigne.ENLEVEE) {
                    enlevees.add(ligne);
                } else {
                    ajoutees.add(ligne);
                }
            }
            int hauteur = Math.max(enlevees.size(), ajoutees.size());
            for (int i = 0; i < hauteur; i++) {
                String gauche = i < enlevees.size()
                        ? enlevees.get(i).texte() : "";
                String droite = i < ajoutees.size()
                        ? ajoutees.get(i).texte() : "";
                rendues.add(colonne(gauche, largeur) + " | " + droite);
            }
        }
        return rendues;
    }

    private static String colonne(String texte, int largeur) {
        if (texte.length() >= largeur) {
            return texte.substring(0, Math.max(0, largeur - 1)) + "…";
        }
        return texte + " ".repeat(largeur - texte.length());
    }

    private static List<String> lignes(String texte) {
        List<String> lignes = new ArrayList<>();
        int debut = 0;
        for (int i = 0; i < texte.length(); i++) {
            if (texte.charAt(i) == '\n') {
                lignes.add(texte.substring(debut, i));
                debut = i + 1;
            }
        }
        lignes.add(texte.substring(debut));
        return lignes;
    }

}
