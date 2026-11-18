package jo.celeris.core.syntax;

import org.jspecify.annotations.Nullable;

/**
 * La fabrique du moteur de la voie tree-sitter, VUE PAR LE PONT
 * (ADR-0024) : le module opt-in {@code core-syntax-treesitter} en
 * fournit l'implémentation — chargée par {@link ServiceLoader} quand
 * il vit sur le classpath de l'hôte, introuvable sinon. Le pont ne
 * connaît ni la classe, ni la bibliothèque : il demande, la voie
 * répond.
 *
 * <p>Le découplage est total dans l'autre sens aussi : core-syntax
 * ne dépend JAMAIS du module de la voie — c'est lui qui dépend de
 * core-syntax (il implémente l'interface et s'inscrit par le fichier
 * de services du META-INF de son artefact).</p>
 */
public interface FabriqueMoteurVoie {

    /**
     * Le moteur de la voie pour la source donnée — null quand la
     * source n'est pas de la voie (le pont y branche la voie des
     * grammaires) ou que la bibliothèque partagée manque.
     *
     * @param source    la source syntaxique du langage, ou null
     * @param session   la session d'édition suivie
     * @param executeur l'exécuteur du travail, null pour la voie
     *                  synchrone
     * @return le moteur prêt, null hors de la voie
     */
    @Nullable MoteurSyntaxe creer(@Nullable LanguageSyntax source,
            jo.celeris.core.text.TextSession session,
            java.util.concurrent.@Nullable Executor executeur);
}
