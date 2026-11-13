package jo.celeris.core.syntax;

/**
 * Les sortes de nœuds du niveau 2 (ADR-020) : la matérialisation Java
 * de l'arbre aplati y range ses nœuds — la catégorie porte la
 * structure (le pli), la sorte reste l'étiquette neutre du niveau.
 */
enum NativeNodeKind implements NodeKind {

    /** Un nœud du niveau 2, sans sorte propre. */
    NODE
}
