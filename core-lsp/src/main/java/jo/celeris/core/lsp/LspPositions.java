package jo.celeris.core.lsp;

import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.LspPosition;
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
public final class LspPositions {

    private LspPositions() {
    }

    /**
     * La position d'un offset du document.
     *
     * @param doc    le document porteur
     * @param offset l'offset, clampé aux bornes du document
     * @return la position ligne/colonne de cet offset
     */
    public static LspPosition position(Document doc, int offset) {
        int borne = Math.max(0, Math.min(offset, doc.length()));
        int line = doc.lineForOffset(borne);
        return new LspPosition(line, borne - doc.lineStart(line));
    }

    /**
     * L'offset d'une position — ligne clampée à la dernière, colonne
     * clampée à la fin du contenu de la ligne.
     *
     * @param doc      le document porteur
     * @param position la position LSP, même généreuse
     * @return l'offset correspondant, dans les bornes du document
     */
    public static int offset(Document doc, LspPosition position) {
        int line = Math.max(0, Math.min(position.line(), doc.lineCount() - 1));
        int start = doc.lineStart(line);
        int finContenu = doc.lineEnd(line);
        int column = Math.max(0, Math.min(position.column(), finContenu - start));
        return start + column;
    }

    /**
     * La plage d'une paire d'offsets.
     *
     * @param doc   le document porteur
     * @param start l'offset de début
     * @param end   l'offset de fin (exclusif)
     * @return la plage LSP correspondante
     */
    public static RangeLsp range(Document doc, int start, int end) {
        return new RangeLsp(position(doc, start), position(doc, end));
    }

    /**
     * Les bornes en offsets d'une plage — chaque position clampée.
     *
     * @param doc   le document porteur
     * @param range la plage LSP, même généreuse
     * @return les offsets {@code [debut, fin[} clampés au document
     */
    public static int[] bounds(Document doc, RangeLsp range) {
        return new int[] {offset(doc, range.start()), offset(doc, range.end())};
    }
}
