package jo.celeris.demo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jo.celeris.core.intelligence.RechercheFloue;
import jo.celeris.core.syntax.structure.OutlineEntry;

/**
 * Les résultats de l'aller à (catégorie C de l'inventaire — « Go to
 * File / Class / Symbol », manquant) : la requête tape quelques
 * lettres, la recherche floue (B-13) répond — les fichiers du catalogue
 * et les symboles du plan ouvert s'y mélangent par score, les fichiers
 * d'abord à égalité ; la requête muette sert les fichiers seuls (le
 * catalogue en raccourci, comme tout éditeur). Pur et testé sans
 * appareil : la feuille n'y ajoute que les vues et le saut.
 */
final class ResultatsAller {

    /** Un résultat de l'aller à : son genre (fichier ou symbole), son
     *  libellé et son détail — le genre est une CLÉ, la feuille la
     *  localise. */
    record Resultat(String genre, String libelle, String detail) {
    }

    /** La clé du genre fichier — la feuille la localise. */
    static final String GENRE_FICHIER = "fichier";

    /** La clé du genre symbole — la feuille la localise. */
    static final String GENRE_SYMBOLE = "symbole";

    private ResultatsAller() {
    }

    /**
     * La recherche des résultats : les fichiers du catalogue et les
     * symboles du plan ouvert répondent à la requête par le score flou
     * (B-13), à égalité les fichiers d'abord — jamais le plafond ne
     * déborde.
     *
     * @param requete  les lettres tapées, muette pour le catalogue seul
     * @param fichiers les noms des fichiers du catalogue
     * @param plan     les entrées du plan du fichier ouvert
     * @param plafond  le nombre maximal de résultats
     * @return les résultats, par score décroissant, le plafond compris
     */
    static List<Resultat> recherche(String requete, List<String> fichiers,
            List<OutlineEntry> plan, int plafond) {
        List<Resultat> candidats = new ArrayList<>();
        if (requete == null || requete.isBlank()) {
            for (String fichier : fichiers) {
                if (candidats.size() >= plafond) {
                    break;
                }
                candidats.add(new Resultat(GENRE_FICHIER, fichier,
                        extension(fichier)));
            }
            return candidats;
        }
        for (String fichier : fichiers) {
            if (RechercheFloue.correspond(fichier, requete)) {
                candidats.add(new Resultat(GENRE_FICHIER, fichier,
                        extension(fichier)));
            }
        }
        for (OutlineEntry entree : plan) {
            if (RechercheFloue.correspond(entree.name(), requete)) {
                candidats.add(new Resultat(GENRE_SYMBOLE, entree.name(),
                        "L" + (entree.startLine() + 1)));
            }
        }
        candidats.sort(Comparator.comparingInt(
                (Resultat resultat) -> RechercheFloue.score(
                        resultat.libelle(), requete)).reversed());
        return candidats.size() <= plafond ? List.copyOf(candidats)
                : List.copyOf(candidats.subList(0, plafond));
    }

    /** L'extension du fichier — le détail du résultat fichier. */
    private static String extension(String fichier) {
        int point = fichier.lastIndexOf('.');
        return point < 0 ? "" : fichier.substring(point + 1);
    }

}
