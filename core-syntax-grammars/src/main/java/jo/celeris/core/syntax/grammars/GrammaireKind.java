package jo.celeris.core.syntax.grammars;

import jo.celeris.core.syntax.NodeKind;

/**
 * Sortes de nœuds de la voie de grammaires (ADR-004) : la voie ne
 * produit pas d'arbre de déclarations — le plan reste l'affaire des
 * analyseurs dédiés — mais des régions de pli, une par paire de
 * crochets appariés qui traverse les lignes.
 */
enum GrammaireKind implements NodeKind {

    /** La racine d'un chunk — couvre tout le chunk, jamais pliée. */
    RACINE,

    /** Une région de pli — paire de crochets multi-lignes. */
    BLOC,

    /** Couverture sans intérêt générique. */
    TEXTE
}
