package jo.celeris.core.intelligence;

import jo.celeris.core.language.LanguageConfiguration;

import org.jspecify.annotations.Nullable;

/**
 * Les règles de commentaire d'un langage (M5, A-11 ; M7.6 : pilotées
 * par la configuration) : le préfixe de ligne, les délimiteurs de
 * bloc. Un langage sans commentaire (JSON) masque l'action — jamais de
 * préfixe deviné. Les règles viennent du paquet de langage
 * ({@link LanguageConfiguration.Comments}) : plus aucun aiguillage par
 * identifiant.
 *
 * @param ligne      le préfixe de commentaire de ligne, ou null
 * @param blocDebut l'ouverture du commentaire de bloc, ou null
 * @param blocFin   la fermeture du commentaire de bloc, ou null
 */
public record CommentRules(@Nullable String ligne, @Nullable String blocDebut,
        @Nullable String blocFin) {

    /**
     * Les règles d'une configuration de langage.
     *
     * @param configuration la configuration du langage
     * @return ses règles, éventuellement sans commentaire
     */
    public static CommentRules of(LanguageConfiguration configuration) {
        LanguageConfiguration.Comments commentaires = configuration.comments();
        return new CommentRules(commentaires.lineComment(),
                commentaires.blockStart(), commentaires.blockEnd());
    }

    /**
     * Vrai si le langage se commente : au moins une forme existe.
     *
     * @return la disponibilité du commentaire
     */
    public boolean commentable() {
        return ligne != null || blocDebut != null;
    }
}
