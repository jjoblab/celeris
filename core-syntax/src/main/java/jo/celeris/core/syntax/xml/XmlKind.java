package jo.celeris.core.syntax.xml;

import jo.celeris.core.syntax.NodeKind;
/**
 * Sortes de nœuds propres à XML (ADR-004) : le plan et les plis lisent la
 * catégorie générique (l'élément est un TAG repliable et nommé), la sorte
 * sert aux requêtes propres au langage.
 */
enum XmlKind implements NodeKind {

    /** Élément, avec ou sans enfants. */
    ELEMENT,

    /** Section CDATA ou texte. */
    TEXTE,

    /** Prologue, doctype ou commentaire. */
    AUTRE,

    /** Structure malformée. */
    ERREUR
}
