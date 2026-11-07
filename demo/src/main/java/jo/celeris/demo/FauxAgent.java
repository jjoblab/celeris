package jo.celeris.demo;

import java.util.List;

import jo.celeris.core.ai.EtiquetteAgent;
import jo.celeris.core.ai.PropositionAgent;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * L'agent factice de la démonstration (F-04) : déterministe, sans
 * réseau — il propose trois insertions sur le document ouvert, une en
 * tête, une au milieu, une en pied, chacune son bloc. La proposition
 * se valide contre le document de référence comme celle d'un vrai
 * agent : la démonstration ne triche pas sur le contrat.
 */
final class FauxAgent {

    /** L'étiquette de l'agent factice, lue à la revue. */
    static final EtiquetteAgent ETIQUETTE = new EtiquetteAgent(
            "agent-factice", "Documenter la tête, le milieu et le pied");

    /**
     * Propose trois insertions sur le document donné : en-tête,
     * milieu, pied — trois blocs épars, la matière de la revue.
     *
     * @param document l'instantané de référence à proposer d'éditer
     * @return la proposition validée, datée de la révision du document
     */
    PropositionAgent proposer(Document document) {
        int milieu = pointDuMilieu(document);
        return PropositionAgent.de(document, ETIQUETTE, List.of(
                new TextEdit(0, "", "// Proposition de l'agent (tête)\n"),
                new TextEdit(milieu, "", "// Proposition de l'agent (milieu)\n"),
                new TextEdit(document.length(), "",
                        "\n// Proposition de l'agent (pied)\n")));
    }

    /**
     * L'offset du début de la ligne du milieu — la moitié du document
     * sur les fichiers d'une ligne.
     */
    private static int pointDuMilieu(Document document) {
        if (document.lineCount() <= 2) {
            return document.length() / 2;
        }
        return document.lineStart(document.lineCount() / 2);
    }
}
