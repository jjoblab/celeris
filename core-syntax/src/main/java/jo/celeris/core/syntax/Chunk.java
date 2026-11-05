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
 * <p>À l'ouverture progressive (G-01), un chunk hors fenêtre se pose
 * « en attente » : sa géométrie est connue — le découpage la calcule en
 * marchant les jetons — mais son sous-arbre est un marqueur neutre,
 * inerte pour plis, plan et diagnostics, jusqu'à sa complétion.</p>
 *
 * @param startLine ligne du premier jeton
 * @param startCol  colonne du premier jeton
 * @param endLine   ligne du dernier jeton
 * @param endCol    colonne après le dernier jeton
 * @param root      le sous-arbre du chunk (bornes relatives)
 */
public record Chunk(int startLine, int startCol, int endLine, int endCol, SynNode root) {

    /** Sorte du marqueur d'attente : aucun langage ne la déclare, le socle seul s'en sert. */
    enum Attente implements NodeKind {

        /** L'unique instance du marqueur. */
        INSTANCE
    }

    /** Vrai si le chunk couvre une structure malformée. */
    public boolean isError() {
        return root.isError();
    }

    /** Vrai quand le chunk attend son analyse (ouverture progressive, G-01). */
    public boolean estEnAttente() {
        return root.kind() == Attente.INSTANCE;
    }

    /**
     * Un chunk à la géométrie donnée, sans sous-arbre : le marqueur
     * d'attente est neutre — ni erreur, ni pliable, ni au plan.
     *
     * @param startLine ligne du premier jeton
     * @param startCol  colonne du premier jeton
     * @param endLine   ligne du dernier jeton
     * @param endCol    colonne après le dernier jeton
     * @return le chunk en attente
     */
    static Chunk enAttente(int startLine, int startCol, int endLine, int endCol) {
        return new Chunk(startLine, startCol, endLine, endCol,
                SynNode.of(Attente.INSTANCE, NodeCategory.OTHER, 0, 0,
                        new SynNode[0], null, false));
    }
}
