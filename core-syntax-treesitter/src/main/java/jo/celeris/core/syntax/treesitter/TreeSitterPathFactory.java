package jo.celeris.core.syntax.treesitter;

import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.EnginePathFactory;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.SyntaxEngine;
import jo.celeris.core.text.TextSession;

/**
 * La fabrique de la voie tree-sitter (ADR-0024) — l'implémentation
 * que {@link java.util.ServiceLoader} charge quand CE module vit sur
 * le classpath de l'hôte. La bibliothèque partagée manquante rend
 * null : le repli déclaré du paquet sert, personne ne change.
 *
 * <p>Elle porte aussi l'INSCRIPTION de la voie au registre de la
 * plateforme ({@code TreeSitterLanguages.enregistrer}) : les sources
 * des langages servis (json, python, javascript) y entrent dès
 * l'initialisation — la préférence « tree-sitter » d'un paquet se met
 * à servir, son repli cesse de servir. Le retrait s'y fait
 * symétriquement ({@code forget}) à la fermeture.</p>
 */
public final class TreeSitterPathFactory implements EnginePathFactory {

    /** Vrai quand l'inscription au registre est faite. */
    private static volatile boolean inscrite;

    /** Inscrit la voie au registre de la plateforme — UNE fois. */
    private static synchronized void inscrire() {
        if (inscrite) {
            return;
        }
        inscrireLangage("json", SyntaxeNatif.json());
        inscrireLangage("python", SyntaxeNatif.python());
        inscrite = true;
    }

    /** Inscrit un langage — null sans bibliothèque partagée. */
    private static void inscrireLangage(String id,
            @Nullable SyntaxeNatif source) {
        if (source != null) {
            jo.celeris.core.language.packs.TreeSitterLanguages.enregistrer(
                    id, source);
        }
    }

    @Override
    public @Nullable SyntaxEngine create(@Nullable LanguageSyntax source,
            TextSession session, @Nullable Executor executor) {
        inscrire();
        return NativeEngine.of(source, session, executor);
    }

    /** La fabrique ne porte aucun état d'instance. */
    public TreeSitterPathFactory() {
    }

    /**
     * La dernière analyse publiée par le moteur de la voie — délégue
     * au moteur concret ; utile aux bancs de la voie.
     *
     * @param moteur le moteur de la voie
     * @return l'analyse publiée, ou null
     */
    public static @Nullable Analysis analysePubliee(SyntaxEngine moteur) {
        return moteur.analysePubliee();
    }
}
