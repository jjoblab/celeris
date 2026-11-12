package jo.celeris.core.syntax.grammars;

import java.util.List;

import jo.celeris.core.syntax.scope.ScopeSelector;


/**
 * Une injection de grammaire (M7.6, H-05) : des règles qui s'appliquent
 * PARTOUT où leur sélecteur matche la pile de portées courante — le
 * code dans les blocs Markdown, le style des attributs HTML. La clé de
 * l'injection dans la grammaire porte sa priorité : {@code L:} la fait
 * passer AVANT les règles du contexte (l'injection gagne les ex æquo),
 * {@code R:} APRÈS (le contexte gagne) ; sans préfixe, la priorité
 * droite vaut — l'hôte reste maître chez lui.
 *
 * @param selector   le sélecteur de portées de l'injection
 * @param regles     les règles injectées
 * @param prioriteGauche vrai pour la priorité {@code L:}
 */
public record Injection(ScopeSelector selector, List<Regle> regles,
        boolean prioriteGauche) {

    /** Constructeur canonique : les règles se figent, jamais nulles. */
    public Injection {
        regles = List.copyOf(regles);
    }
}
