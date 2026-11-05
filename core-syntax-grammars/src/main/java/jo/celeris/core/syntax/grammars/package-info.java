/**
 * Voie de grammaires pour la longue traîne (ADR-004) : les langages
 * sans analyseur dédié se colorent et se replient par grammaire
 * déclarative au format TextMate — motifs, blocs début/fin, référentiel
 * — mûs par le même socle d'analyse incrémentale que les analyseurs
 * P0 (jetons, états transportés par ligne, crochets appariés, plis).
 * Coloration et plis seulement : la voie ne produit pas d'arbre de
 * déclarations, le plan reste l'affaire des analyseurs dédiés.
 */
@org.jspecify.annotations.NullMarked
package jo.celeris.core.syntax.grammars;
