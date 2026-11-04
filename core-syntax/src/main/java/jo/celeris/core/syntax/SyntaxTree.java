package jo.celeris.core.syntax;

import java.util.List;

import jo.celeris.core.text.Document;

/**
 * Arbre syntaxique versionné par instantané de document (ADR-004) : la
 * révision est celle du document qui a produit l'arbre ; l'analyse est
 * rejouable sur tout instantané, y compris au retour de l'annulation. La
 * liste des chunks est triée par position ; chacun porte son sous-arbre à
 * bornes relatives et son ancre (ligne, colonne) — la résolution absolue
 * passe par le document, jamais par des offsets stockés qui périmeraient.
 */
public final class SyntaxTree {

    private final int revision;
    private final List<Chunk> chunks;

    SyntaxTree(int revision, List<Chunk> chunks) {
        this.revision = revision;
        this.chunks = List.copyOf(chunks);
    }

    /** La révision du document qui a produit cet arbre. */
    public int revision() {
        return revision;
    }

    /** Les chunks de haut niveau, triés par position. */
    public List<Chunk> chunks() {
        return chunks;
    }

    /**
     * Offset absolu du début du chunk (son ancre).
     *
     * @param doc   le document porteur
     * @param chunk le chunk visé
     * @return l'offset du premier jeton du chunk
     */
    public int chunkStartOffset(Document doc, Chunk chunk) {
        return doc.lineStart(chunk.startLine()) + chunk.startCol();
    }

    /**
     * Offset absolu de la fin du chunk (après son dernier jeton).
     *
     * @param doc   le document porteur
     * @param chunk le chunk visé
     * @return l'offset après le dernier jeton du chunk
     */
    public int chunkEndOffset(Document doc, Chunk chunk) {
        return doc.lineStart(chunk.endLine()) + chunk.endCol();
    }
}
