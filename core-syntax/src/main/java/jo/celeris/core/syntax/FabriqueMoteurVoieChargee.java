package jo.celeris.core.syntax;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.TextSession;

/**
 * Le chargeur de la fabrique de la voie tree-sitter (ADR-0024) :
 * {@link ServiceLoader} y cherche l'implémentation que le module
 * opt-in {@code core-syntax-treesitter} inscrit dans son META-INF.
 * Le module absent : aucune implémentation, la méthode rend toujours
 * null — le pont branche la voie des grammaires, personne ne change.
 *
 * <p>Le chargeur est paresseux et unique par classloader : la
 * première demande paie la lecture du META-INF, les suivantes lisent
 * le résultat en champ.</p>
 */
public final class FabriqueMoteurVoieChargee {

    /** La fabrique trouvée, ou null — chargée une fois. */
    private static @Nullable FabriqueMoteurVoie fabrique;

    private FabriqueMoteurVoieChargee() {
    }

    /**
     * Le moteur de la voie pour la source donnée — null sans module
     * de la voie, sans bibliothèque partagée ou hors de la voie.
     *
     * @param source    la source syntaxique du langage, ou null
     * @param session   la session d'édition suivie
     * @param executeur l'exécuteur du travail, null pour la voie
     *                  synchrone
     * @return le moteur prêt, null hors de la voie
     */
    public static @Nullable MoteurSyntaxe creer(@Nullable LanguageSyntax source,
            TextSession session, @Nullable Executor executeur) {
        FabriqueMoteurVoie locale = fabrique;
        if (locale == null) {
            Optional<FabriqueMoteurVoie> trouve = ServiceLoader.load(
                    FabriqueMoteurVoie.class,
                    FabriqueMoteurVoie.class.getClassLoader()).findFirst();
            locale = trouve.orElse(null);
            fabrique = locale;
        }
        return locale == null ? null : locale.creer(source, session, executeur);
    }
}
