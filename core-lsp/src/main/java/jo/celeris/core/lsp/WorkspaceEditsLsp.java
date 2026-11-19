package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jo.celeris.core.lsp.types.DocumentEditLsp;
import jo.celeris.core.lsp.types.TextEditLsp;
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
public final class WorkspaceEditsLsp {

    private WorkspaceEditsLsp() {
    }

    /**
     * La transaction des éditions d'un document.
     *
     * @param doc      le document porteur — l'état sur lequel les plages
     *                 du serveur s'entendent
     * @param edit  les éditions à transposer
     * @return la transaction, éditions triées en ordre descendant
     */
    public static Transaction transaction(Document doc, DocumentEditLsp edit) {
        List<TextEdit> edits = new ArrayList<>();
        for (TextEditLsp editionTexte : edit.edits()) {
            int[] bounds = LspPositions.bounds(doc, editionTexte.range());
            // un serveur généreux peut rendre une plage inversée : le
            // clampage l'aurait laissée vivre, la transaction non — les
            // bornes s'ordonnent, la doctrine du rempart tient
            int start = Math.min(bounds[0], bounds[1]);
            int end = Math.max(bounds[0], bounds[1]);
            edits.add(new TextEdit(start,
                    doc.subText(start, end), editionTexte.newText()));
        }
        edits.sort(Comparator.comparingInt(TextEdit::start).reversed());
        return Transaction.of(edits);
    }
}
