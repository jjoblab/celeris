package jo.celeris.demo;

import java.util.List;
import java.util.stream.Collectors;

import jo.celeris.core.language.LanguageSupport;

/**
 * La carte d'un fichier d'exemple pour l'écran d'accueil : monogramme,
 * couleur de badge, nom de langage et compte de lignes — tout ce que
 * la carte affiche, calculé sans Android (testable Jupiter). Le badge
 * et la couleur viennent du DESCRIPTEUR du langage (M7.6) : la
 * détection parle au registre de la plateforme, plus aucun aiguillage
 * par extension — les couleurs restent les encres syntaxiques du thème
 * sombre de l'éditeur pour que l'accueil et la toile parlent la même
 * palette.
 *
 * @param nom        le nom du fichier, extension comprise
 * @param monogramme une à trois lettres du badge de langage
 * @param couleur    la couleur ARGB du badge
 * @param langage    le nom de langage affiché
 * @param lignes     le nombre de lignes du contenu
 */
public record CarteFichier(String nom, String monogramme, int couleur,
        String langage, int lignes) {

    private static final int TEXTE = 0xFF9AA0A6;

    /**
     * La carte du fichier d'exemple donné : le badge, la couleur et le
     * nom viennent du langage détecté, le texte simple sans langage.
     *
     * @param fichier le fichier du catalogue
     * @return sa carte complète
     */
    public static CarteFichier depuis(FichierExemple fichier) {
        LanguageSupport langage = LangagesDemo.langageDe(fichier.nom(),
                null, fichier.contenu());
        boolean sansLangage = langage == null;
        return new CarteFichier(fichier.nom(),
                sansLangage ? "Tx" : langage.descriptor().badge(),
                sansLangage ? TEXTE : langage.descriptor().badgeColor(),
                sansLangage ? "Text" : langage.displayName(),
                compterLignes(fichier.contenu()));
    }

    /**
     * Les cartes du catalogue complet, dans l'ordre d'affichage.
     *
     * @return les cartes, non modifiables
     */
    public static List<CarteFichier> catalogue() {
        return FichiersExemple.catalogue().stream()
                .map(CarteFichier::depuis).collect(Collectors.toList());
    }

    /**
     * Le compte de lignes du contenu : le saut final ne compte pas
     * pour une ligne ({@code String#lines} exige l'API 33, le comptage
     * reste portable jusqu'à 26).
     *
     * @param contenu le contenu intégral du fichier
     * @return le nombre de lignes affichées
     */
    private static int compterLignes(String contenu) {
        if (contenu.isEmpty()) {
            return 0;
        }
        int lignes = 1;
        for (int i = 0; i < contenu.length(); i++) {
            if (contenu.charAt(i) == '\n') {
                lignes++;
            }
        }
        return contenu.endsWith("\n") ? lignes - 1 : lignes;
    }
}
