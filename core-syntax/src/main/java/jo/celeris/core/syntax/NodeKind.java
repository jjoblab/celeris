package jo.celeris.core.syntax;

/**
 * Marqueur de sorte de nœud : chaque langage définit sa propre enum (les
 * sortes de Java ne sont pas celles de XML) qui implémente cette
 * interface. Les consommateurs génériques lisent la
 * {@link NodeCategory catégorie} ; la sorte sert aux requêtes propres à
 * un langage.
 */
public interface NodeKind {
}
