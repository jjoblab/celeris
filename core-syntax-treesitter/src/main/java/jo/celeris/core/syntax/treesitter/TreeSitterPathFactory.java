package jo.celeris.core.syntax.treesitter;

import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.EnginePathFactory;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.SyntaxEngine;
import jo.celeris.core.text.TextSession;

/**
 * La fabrique de la voie tree-sitter (ADR-0024, généralisée par
 * l'ADR-0033) — l'implémentation que {@link java.util.ServiceLoader}
 * charge quand CE module vit sur le classpath de l'hôte. La
 * bibliothèque partagée manquante rend null : le repli déclaré du
 * paquet sert, personne ne change.
 *
 * <p>La fabrique FABRIQUE, c'est tout : elle n'inscrit plus rien au
 * registre de la plateforme (l'inscription automatic de l'ancienne
 * voie gravait json/python sans demande — le modèle de l'éditeur de
 * référence fait vivre le choix chez l'hôte : les grammaires
 * s'enregistrent par {@link TreeSitterGrammars#enregistrer}, la
 * langue ne se paie que si l'hôte l'embarque par dépendance). Sans
 * enregistrement, le registre reste vide, la préférence des paquets
 * suit son repli déclaré — l'orthogonalité des deux voies se
 * PROUVE : la voie native sans les grammaires TextMate, les
 * grammaires TextMate sans la voie native.</p>
 */
public final class TreeSitterPathFactory implements EnginePathFactory {

    /** La fabrique ne porte aucun état d'instance. */
    public TreeSitterPathFactory() {
    }

    @Override
    public @Nullable SyntaxEngine create(@Nullable LanguageSyntax source,
            TextSession session, @Nullable Executor executor) {
        return NativeEngine.of(source, session, executor);
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
