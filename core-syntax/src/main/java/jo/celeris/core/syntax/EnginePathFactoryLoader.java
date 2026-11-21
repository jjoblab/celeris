package jo.celeris.core.syntax;

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
public final class EnginePathFactoryLoader {

    /** La fabrique trouvée, ou null — chargée une fois, résultat nul
     *  compris (l'audit coloration H7 : sans module opt-in, chaque
     *  rattachement relisait le META-INF ; le vide se paie une fois
     *  lui aussi). */
    private static @Nullable EnginePathFactory fabrique;

    /** Vrai dès que la première demande a lu le descripteur — le nul
     *  ne se distingue plus du « pas encore chargé ». */
    private static boolean charge;

    private EnginePathFactoryLoader() {
    }

    /**
     * Le moteur de la voie pour la source donnée — null sans module
     * de la voie, sans bibliothèque partagée ou hors de la voie.
     *
     * @param source    la source syntaxique du langage, ou null
     * @param session   la session d'édition suivie
     * @param executor l'exécuteur du travail, null pour la voie
     *                  synchrone
     * @return le moteur prêt, null hors de la voie
     */
    public static synchronized @Nullable SyntaxEngine create(@Nullable LanguageSyntax source,
            TextSession session, @Nullable Executor executor) {
        if (!charge) {
            charge = true;
            fabrique = ServiceLoader.load(
                    EnginePathFactory.class,
                    EnginePathFactory.class.getClassLoader()).findFirst()
                    .orElse(null);
        }
        EnginePathFactory locale = fabrique;
        return locale == null ? null : locale.create(source, session, executor);
    }
}
