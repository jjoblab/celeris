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

    /** La gravité d'une marque : 3 l'erreur, 2 l'avertissement (la
     *  convention du dépôt de départ — autre chose vaut information). */
    public static final int GRAVITE_ERREUR = 3;

    /** Une marque : la ligne du diagnostic, son cran de bande, sa
     *  cible de saut, sa gravité. */
    public record Marque(int ligne, int cran, int startOffset, int gravite) {
    }

    private final List<Marque> marques;

    private ErrorBandModel(List<Marque> marques) {
        this.marques = List.copyOf(marques);
    }

    /**
     * Construit la bande des diagnostics d'un document — les marques
     * tiennent l'erreur pour gravité (la voie historique).
     *
     * @param diagnostics les diagnostics courants, dans l'ordre du source
     * @param doc         le document porteur (offsets → lignes)
     * @param hauteur     la hauteur de la bande en crans
     * @return la bande, densité bornée à un cran
     */
    public static ErrorBandModel of(List<SyntaxIssue> diagnostics, Document doc,
            int hauteur) {
        int[] gravites = new int[diagnostics.size()];
        java.util.Arrays.fill(gravites, GRAVITE_ERREUR);
        return of(diagnostics, gravites, doc, hauteur);
    }

    /**
     * Construit la bande des diagnostics d'un document, gravité par
     * gravité — la marque porte la sienne, le peintre la suit.
     *
     * @param diagnostics les diagnostics courants, dans l'ordre du source
     * @param gravites    la gravité de chaque diagnostic (3 erreur,
     *                    2 avertissement, autre information), même ordre
     * @param doc         le document porteur (offsets → lignes)
     * @param hauteur     la hauteur de la bande en crans
     * @return la bande, densité bornée à un cran
     */
    public static ErrorBandModel of(List<SyntaxIssue> diagnostics,
            int[] gravites, Document doc, int hauteur) {
        int crans = Math.max(1, hauteur);
        List<Marque> marques = new ArrayList<>();
        int cranOccupe = -1;
        for (int i = 0; i < diagnostics.size(); i++) {
            SyntaxIssue diagnostic = diagnostics.get(i);
            if (diagnostic.startOffset() >= doc.length()) {
                continue;
            }
            int ligne = doc.lineForOffset(diagnostic.startOffset());
            int cran = Math.min(crans - 1, ligne * crans / Math.max(1, doc.lineCount()));
            if (cran != cranOccupe) {
                marques.add(new Marque(ligne, cran, diagnostic.startOffset(),
                        i < gravites.length ? gravites[i] : GRAVITE_ERREUR));
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
