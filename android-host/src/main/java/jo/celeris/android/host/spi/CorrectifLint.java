package jo.celeris.android.host.spi;

/**
 * Un correctif applicable à un diagnostic de lint (C-12) : un remplacement
 * textuel exact — la ligne, la colonne du premier caractère touché, le
 * texte d'avant et le texte d'après. L'application reste le geste de
 * l'éditeur : une transaction annulable, jamais silencieuse.
 *
 * @param titre   le libellé du correctif, montré à l'utilisateur
 * @param line   la ligne, numérotée à partir de un
 * @param column la colonne du premier caractère, à partir de un
 * @param avant   le texte à remplacer, jamais vide
 * @param apres   le texte de remplacement
 */
public record CorrectifLint(String titre, int line, int column,
        String avant, String apres) {

    /**
     * Le correctif appliqué à une ligne de texte : la première
     * occurrence du texte d'avant y devient le texte d'après.
     *
     * @param text la ligne à corriger
     * @return la ligne corrigée, identique si l'occurrence a disparu
     */
    public String appliquer(String text) {
        int position = text.indexOf(avant);
        if (position < 0) {
            return text;
        }
        return text.substring(0, position) + apres
                + text.substring(position + avant.length());
    }

    /**
     * Le décalage du correctif dans un document plat : le début du
     * texte d'avant, depuis les débuts de lignes donnés.
     *
     * @param debutsLignes le décalage du premier caractère de chaque
     *                     ligne, numérotées à partir de zéro
     * @return le décalage du premier caractère touché
     */
    public int start(int[] debutsLignes) {
        return debutsLignes[line - 1] + column - 1;
    }

    /**
     * Le décalage de fin du correctif dans un document plat.
     *
     * @param debutsLignes le décalage du premier caractère de chaque
     *                     ligne, numérotées à partir de zéro
     * @return le décalage après le dernier caractère touché
     */
    public int end(int[] debutsLignes) {
        return start(debutsLignes) + avant.length();
    }
}
