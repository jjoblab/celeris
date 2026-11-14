package jo.celeris.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * Le faux build de lint de la démonstration (C-12) : il lit le texte
 * ouvert comme le ferait l'outils — chaque ligne porteuse de trois
 * points rapporte l'avertissement d'ellipse typographique, chaque
 * ligne porteuse d'un double tiret rapporte celui du tiret cadratin —
 * et rend la sortie texte au format exact d'Android Lint. La marque
 * « noinspection » (SuppressionInspection, catégorie B) s'y honore
 * : la règle masquée en la ligne ne se rapporte plus, les autres
 * demeurent. Le vrai travail reste celui de l'éditeur : la sortie se
 * parse ({@code AnalyseurLint}), les diagnostics se peignent, les
 * correctifs s'appliquent en transactions annulables.
 */
final class FauxLint {

    private FauxLint() {
    }

    /**
     * La sortie de lint du fichier donné — la marque « noinspection »
     * d'une règle masque SON avertissement en la ligne, jamais les
     * autres.
     *
     * @param nomFichier le nom du fichier analysé
     * @param lignes     les lignes du texte ouvert
     * @return la sortie texte, au format « chemin:ligne: Warning: message [Id] »
     */
    static String sortie(String nomFichier, List<String> lignes) {
        StringBuilder rapport = new StringBuilder();
        for (int i = 0; i < lignes.size(); i++) {
            String ligne = lignes.get(i);
            if (ligne.contains("...")
                    && !masquee(ligne, "TypographyEllipsis")) {
                rapport.append(nomFichier).append(':').append(i + 1)
                        .append(": Warning: Replace \"...\" with ellipsis character")
                        .append(" [TypographyEllipsis]\n");
            }
            if (ligne.contains("--") && !masquee(ligne, "TypographyDashes")) {
                rapport.append(nomFichier).append(':').append(i + 1)
                        .append(": Warning: Use an em-dash instead of two hyphens")
                        .append(" [TypographyDashes]\n");
            }
        }
        return rapport.toString();
    }

    /** Vrai quand la ligne porte la marque de masquage de la règle. */
    private static boolean masquee(String ligne, String regle) {
        return ligne.contains(SuppressionInspection.MARQUE + regle);
    }

    /**
     * Les lignes du texte ouvert.
     *
     * @param texte le texte intégral
     * @return les lignes, numérotées à partir de zéro
     */
    static List<String> lignes(String texte) {
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
