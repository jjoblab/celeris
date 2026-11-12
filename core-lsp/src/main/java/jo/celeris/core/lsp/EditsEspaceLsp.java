package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jo.celeris.core.lsp.types.EditionDocumentLsp;
import jo.celeris.core.lsp.types.EditionTexteLsp;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

/**
 * La transposition des éditions d'espace de travail (B-17, B-19) : les
 * plages LSP (unités UTF-16, bornes clampées) deviennent une
 * transaction de la voie unique — diffable, annulable, rejouée par la
 * même voie au retour arrière (PIEGE-07).
 *
 * <p>Les éditions s'appliquent en ordre DESCROISSANT : les offsets
 * originaux du serveur restent valides au fil de l'application, chaque
 * édition s'entendant dans l'état du document après les précédentes —
 * les précédentes sont toutes APRÈS elle.</p>
 */
public final class EditsEspaceLsp {

    private EditsEspaceLsp() {
    }

    /**
     * La transaction des éditions d'un document.
     *
     * @param doc      le document porteur — l'état sur lequel les plages
     *                 du serveur s'entendent
     * @param edition  les éditions à transposer
     * @return la transaction, éditions triées en ordre descendant
     */
    public static Transaction transaction(Document doc, EditionDocumentLsp edition) {
        List<TextEdit> editions = new ArrayList<>();
        for (EditionTexteLsp editionTexte : edition.editions()) {
            int[] bornes = PositionsLsp.bornes(doc, editionTexte.plage());
            editions.add(new TextEdit(bornes[0],
                    doc.subText(bornes[0], bornes[1]), editionTexte.nouveauTexte()));
        }
        editions.sort(Comparator.comparingInt(TextEdit::start).reversed());
        return Transaction.of(editions);
    }
}
