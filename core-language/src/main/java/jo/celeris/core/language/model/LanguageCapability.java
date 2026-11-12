package jo.celeris.core.language.model;

import jo.celeris.core.language.Completion;
/**
 * Une capacité d'un langage (M7.6) : l'interface-marqueur que
 * implements chaque fournisseur enregistré dans un
 * {@link LanguageSupport}. Les clés sont des interfaces du domaine —
 * {@link Completion} aujourd'hui, les suivantes (formatage, plan,
 * survol, renommage, optimiseur d'imports, gabarits, linter) entrent
 * avec les jalons qui câblent leurs consommateurs.
 */
public interface LanguageCapability {
}
