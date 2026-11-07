package jo.celeris.android.host.spi;

/**
 * Un correctif applicable à un diagnostic de lint (C-12) : un remplacement
 * textuel exact — la ligne, la colonne du premier caractère touché, le
 * texte d'avant et le texte d'après. L'application reste le geste de
 * l'éditeur : une transaction annulable, jamais silencieuse.
 *
 * @param titre   le libellé du correctif, montré à l'utilisateur
 * @param ligne   la ligne, numérotée à partir de un
 * @param colonne la colonne du premier caractère, à partir de un
 * @param avant   le texte à remplacer, jamais vide
 * @param apres   le texte de remplacement
 */
public record CorrectifLint(String titre, int ligne, int colonne,
        String avant, String apres) {

    /**
     * Le correctif appliqué à une ligne de texte : la première
     * occurrence du texte d'avant y devient le texte d'après.
     *
     * @param texte la ligne à corriger
     * @return la ligne corrigée, identique si l'occurrence a disparu
     */
    public String appliquer(String texte) {
        int position = texte.indexOf(avant);
        if (position < 0) {
            return texte;
        }
        return texte.substring(0, position) + apres
                + texte.substring(position + avant.length());
    }

    /**
     * Le décalage du correctif dans un document plat : le début du
     * texte d'avant, depuis les débuts de lignes donnés.
     *
     * @param debutsLignes le décalage du premier caractère de chaque
     *                     ligne, numérotées à partir de zéro
     * @return le décalage du premier caractère touché
     */
    public int debut(int[] debutsLignes) {
        return debutsLignes[ligne - 1] + colonne - 1;
    }

    /**
     * Le décalage de fin du correctif dans un document plat.
     *
     * @param debutsLignes le décalage du premier caractère de chaque
     *                     ligne, numérotées à partir de zéro
     * @return le décalage après le dernier caractère touché
     */
    public int fin(int[] debutsLignes) {
        return debut(debutsLignes) + avant.length();
    }
}
