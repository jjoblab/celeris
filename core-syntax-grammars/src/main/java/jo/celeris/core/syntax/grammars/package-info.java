/**
 * Voie de grammaires pour la longue traîne (ADR-004 révisé, ADR-014) :
 * les langages sans analyseur dédié se colorent et se replient par
 * grammaire déclarative au format TextMate RÉEL — captures en objets,
 * inclusions du référentiel résolues en références, {@code $self} et
 * {@code $base}, fermetures à références de l'ouverture — mûs par
 * joni (Oniguruma en Java) et le même socle d'analyse incrémentale
 * que les analyseurs P0 (jetons, états transportés par ligne,
 * crochets appariés, plis). Coloration et plis seulement : la voie
 * ne produit pas d'arbre de déclarations, le plan reste l'affaire des
 * analyseurs dédiés. Le bornage du prompt vit ici : formes
 * catastrophiques visibles refusées au chargement, budget par ligne
 * au lexeur, rapport de chargement pour ce qui s'écarte — jamais en
 * silence. La plateforme de langages (moteurs, préféré et repli) se
 * déclare dans {@code core-language} ; ce paquet fournit le moteur
 * TextMate et le catalogue de ses langages.
 */
@org.jspecify.annotations.NullMarked
package jo.celeris.core.syntax.grammars;
