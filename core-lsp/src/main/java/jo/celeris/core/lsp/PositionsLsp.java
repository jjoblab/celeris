package jo.celeris.core.lsp;

import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.text.Document;

/**
 * La transposition des positions LSP (ADR-005, point 5) : les colonnes
 * du protocole comptent les UNITÉS UTF-16 — le repère naturel d'une
 * chaîne Java — et les offsets du document comptent les mêmes unités ;
 * la frontière reste explicite, ici et nulle part ailleurs. Les bornes
 * sont clampées : jamais d'exception sur une position de serveur
 * généreux — l'audit du dépôt de départ a documenté les pièges de
 * bornes, le clampage est le rempart.
 */
public final class PositionsLsp {

    private PositionsLsp() {
    }

    /**
     * La position d'un offset du document.
     *
     * @param doc    le document porteur
     * @param offset l'offset, clampé aux bornes du document
     * @return la position ligne/colonne de cet offset
     */
    public static PositionLsp position(Document doc, int offset) {
        int borne = Math.max(0, Math.min(offset, doc.length()));
        int ligne = doc.lineForOffset(borne);
        return new PositionLsp(ligne, borne - doc.lineStart(ligne));
    }

    /**
     * L'offset d'une position — ligne clampée à la dernière, colonne
     * clampée à la fin du contenu de la ligne.
     *
     * @param doc      le document porteur
     * @param position la position LSP, même généreuse
     * @return l'offset correspondant, dans les bornes du document
     */
    public static int offset(Document doc, PositionLsp position) {
        int ligne = Math.max(0, Math.min(position.ligne(), doc.lineCount() - 1));
        int debut = doc.lineStart(ligne);
        int finContenu = doc.lineEnd(ligne);
        int colonne = Math.max(0, Math.min(position.colonne(), finContenu - debut));
        return debut + colonne;
    }

    /**
     * La plage d'une paire d'offsets.
     *
     * @param doc   le document porteur
     * @param debut l'offset de début
     * @param fin   l'offset de fin (exclusif)
     * @return la plage LSP correspondante
     */
    public static PlageLsp plage(Document doc, int debut, int fin) {
        return new PlageLsp(position(doc, debut), position(doc, fin));
    }

    /**
     * Les bornes en offsets d'une plage — chaque position clampée.
     *
     * @param doc   le document porteur
     * @param plage la plage LSP, même généreuse
     * @return les offsets {@code [debut, fin[} clampés au document
     */
    public static int[] bornes(Document doc, PlageLsp plage) {
        return new int[] {offset(doc, plage.debut()), offset(doc, plage.fin())};
    }
}
