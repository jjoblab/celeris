package jo.celeris.core.syntax;

/**
 * Chunk de haut niveau : l'unité de ré-analyse incrémentale (ADR-004).
 * Ancré par (ligne, colonne) de son premier jeton — les ancres survivent
 * aux décalages de lignes sans retoucher le sous-arbre, dont les bornes
 * sont relatives au début du chunk. La fin est le dernier jeton du chunk.
 *
 * <p>Le propriétaire d'une ligne intercalaire (blancs entre deux chunks)
 * est le chunk précédent : toute ligne appartient à exactement un chunk
 * au sens de la ré-analyse, la plage de possession va du début du chunk
 * au début du suivant.</p>
 *
 * @param startLine ligne du premier jeton
 * @param startCol  colonne du premier jeton
 * @param endLine   ligne du dernier jeton
 * @param endCol    colonne après le dernier jeton
 * @param root      le sous-arbre du chunk (bornes relatives)
 */
public record Chunk(int startLine, int startCol, int endLine, int endCol, SynNode root) {

    /** Vrai si le chunk couvre une structure malformée. */
    public boolean isError() {
        return root.isError();
    }
}
