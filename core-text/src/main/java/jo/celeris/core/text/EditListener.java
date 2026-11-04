package jo.celeris.core.text;

/**
 * Auditeur d'édition : notifié de chaque édition appliquée par la voie de
 * transaction unique — y compris lors du rejeu de l'annulation et du
 * refaire (PIEGE-07).
 *
 * <p>Le pont LSP s'y branche : sans notification d'annulation, il tirerait
 * sur une version périmée du document. Le texte retiré est porté par
 * l'événement (état pré-édition), le texte inséré par l'éditon inverse au
 * rejeu.</p>
 */
@FunctionalInterface
public interface EditListener {

    /**
     * Appelé après l'application d'une édition.
     *
     * @param start    début de la plage remplacée, dans l'état pré-édition
     * @param end      fin (exclusive) de la plage remplacée
     * @param removed  le texte qui occupait la plage
     * @param inserted le texte qui la remplace
     */
    void onTextEdit(int start, int end, String removed, String inserted);
}
