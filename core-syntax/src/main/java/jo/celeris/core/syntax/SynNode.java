package jo.celeris.core.syntax;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Nœud d'un arbre tolérant (ADR-004) : sorte propre au langage, catégorie
 * générique, bornes RELATIVES au début de son chunk (le chunk porte
 * l'ancre absolue), enfants, nom de plan le cas échéant et drapeau
 * d'erreur. Immuable ; le tableau d'enfants ne se mute jamais.
 *
 * <p>La tolérance est la règle : un source malformé produit des nœuds
 * ERROR, jamais une exception — l'arbre couvre toujours tout le
 * texte du chunk.</p>
 */
public final class SynNode {

    private final NodeKind kind;
    private final NodeCategory category;
    private final int start;
    private final int end;
    private final SynNode[] children;
    private final @Nullable String outlineName;
    private final boolean error;

    private SynNode(NodeKind kind, NodeCategory category, int start, int end,
                    SynNode[] children, @Nullable String outlineName, boolean error) {
        this.kind = kind;
        this.category = category;
        this.start = start;
        this.end = end;
        this.children = children;
        this.outlineName = outlineName;
        this.error = error;
    }

    /**
     * Construit un nœud.
     *
     * @param kind        la sorte propre au langage
     * @param category    la catégorie générique
     * @param start       début relatif au chunk
     * @param end         fin exclusive relative au chunk
     * @param children    les enfants (vide pour une feuille)
     * @param outlineName le nom pour le plan, ou null si le nœud n'y paraît pas
     * @param error       vrai si la structure est malformée
     * @return le nœud immuable
     */
    public static SynNode of(NodeKind kind, NodeCategory category, int start, int end,
                             SynNode[] children, @Nullable String outlineName, boolean error) {
        return new SynNode(kind, category, start, end,
                children.clone(), outlineName, error);
    }

    /** La sorte propre au langage. */
    public NodeKind kind() {
        return kind;
    }

    /** La catégorie générique (plan, plis, diagnostics). */
    public NodeCategory category() {
        return category;
    }

    /** Début relatif au chunk. */
    public int start() {
        return start;
    }

    /** Fin exclusive relative au chunk. */
    public int end() {
        return end;
    }

    /** Les enfants, dans l'ordre du source. */
    public List<SynNode> children() {
        return List.of(children);
    }

    /** Le nom pour le plan, null si le nœud n'y paraît pas. */
    public @Nullable String outlineName() {
        return outlineName;
    }

    /** Vrai si la structure couverte est malformée. */
    public boolean isError() {
        return error;
    }
}
