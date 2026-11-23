package jo.celeris.core.syntax.treesitter;

import com.itsaky.androidide.treesitter.TSLanguage;

/**
 * Le fournisseur de la langue tree-sitter de la liaison (ADR-0033) :
 * l'hôte embarque l'artefact de SA grammaire
 * ({@code com.itsaky.androidide.treesitter:tree-sitter-&lt;langue&gt;})
 * et le fournisseur y rend la classe de langue publiée par la
 * liaison — {@code TSLanguageJson::getInstance} pour json, par
 * exemple. Le chargement reste paresseux : la langue ne se charge
 * qu'à la première demande du moteur, jamais à l'enregistrement —
 * l'hôte peut tout enregistrer au démarrage, chaque langue se paie
 * à sa première vue.
 */
@FunctionalInterface
public interface TSLanguageSupplier {

    /**
     * La langue tree-sitter de la liaison — la grammaire embarquée
     * par l'hôte se charge par son bloc statique.
     *
     * @return la langue de la grammaire, jamais null (l'hôte ne
     *         registre que ce qu'il embarque)
     */
    TSLanguage langue();
}
