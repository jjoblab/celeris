package jo.celeris.android.widgets;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.syntax.SyntaxIssue;
import jo.celeris.core.text.Document;

/**
 * La bande d'erreurs annotée (M5, D-08) : les diagnostics du même arbre
 * que la coloration deviennent des marques le long du défilement. La
 * densité est bornée — au plus une marque par cran de bande, la
 * première l'emporte — et le clic saute au diagnostic du cran le plus
 * proche. Pur et testable sans SDK : la vue le possède, le peintre le
 * lit.
 */
public final class ErrorBandModel {

    /** Une marque : la ligne du diagnostic, son cran de bande, sa cible de saut. */
    public record Marque(int ligne, int cran, int startOffset) {
    }

    private final List<Marque> marques;

    private ErrorBandModel(List<Marque> marques) {
        this.marques = List.copyOf(marques);
    }

    /**
     * Construit la bande des diagnostics d'un document.
     *
     * @param diagnostics les diagnostics courants, dans l'ordre du source
     * @param doc         le document porteur (offsets → lignes)
     * @param hauteur     la hauteur de la bande en crans
     * @return la bande, densité bornée à un cran
     */
    public static ErrorBandModel of(List<SyntaxIssue> diagnostics, Document doc,
            int hauteur) {
        int crans = Math.max(1, hauteur);
        List<Marque> marques = new ArrayList<>();
        int cranOccupe = -1;
        for (SyntaxIssue diagnostic : diagnostics) {
            if (diagnostic.startOffset() >= doc.length()) {
                continue;
            }
            int ligne = doc.lineForOffset(diagnostic.startOffset());
            int cran = Math.min(crans - 1, ligne * crans / Math.max(1, doc.lineCount()));
            if (cran != cranOccupe) {
                marques.add(new Marque(ligne, cran, diagnostic.startOffset()));
                cranOccupe = cran;
            }
        }
        return new ErrorBandModel(marques);
    }

    /**
     * Les marques, dans l'ordre du document.
     *
     * @return la liste, vide sans diagnostic
     */
    public List<Marque> marques() {
        return marques;
    }

    /**
     * La cible de saut d'un clic sur la bande : le diagnostic du cran
     * marqué le plus proche.
     *
     * @param cran le cran cliqué
     * @return l'offset du diagnostic, -1 sans marque
     */
    public int clic(int cran) {
        Marque proche = null;
        for (Marque marque : marques) {
            if (proche == null
                    || Math.abs(marque.cran() - cran) < Math.abs(proche.cran() - cran)) {
                proche = marque;
            }
        }
        return proche == null ? -1 : proche.startOffset();
    }
}
