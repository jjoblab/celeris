package jo.celeris.android.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.TreeOutline;
import jo.celeris.core.text.Document;

/**
 * Le fil d'Ariane (M5, D-09) : le chemin du symbole courant sort du
 * même arbre que la coloration — les entrées du plan qui englobent le
 * curseur, de la plus externe à la plus interne. La troncature par la
 * largeur garde les niveaux profonds et élague la tête : l'ellipse
 * signale ce qui tombe, elle compte dans la largeur. Pur et testable
 * sans SDK.
 */
public final class BreadcrumbModel {

    private static final String ELLIPSE = "…";
    private static final String SEPARATEUR = " › ";

    private BreadcrumbModel() {
    }

    /**
     * Le chemin du symbole courant : les entrées du plan qui englobent
     * la position, de la plus externe à la plus interne.
     *
     * @param doc      le document porteur
     * @param analysis l'analyse courante
     * @param offset   la position du curseur
     * @return les entrées englobantes, vide hors de tout symbole
     */
    public static List<OutlineEntry> chemin(Document doc, Analysis analysis, int offset) {
        List<OutlineEntry> toutes = TreeOutline.entries(doc, analysis);
        List<OutlineEntry> contenantes = new ArrayList<>();
        for (OutlineEntry entree : toutes) {
            if (entree.startOffset() <= offset && offset < entree.endOffset()) {
                contenantes.add(entree);
            }
        }
        // de la plus externe à la plus interne : par taille décroissante
        contenantes.sort((a, b) -> Integer.compare(
                b.endOffset() - b.startOffset(), a.endOffset() - a.startOffset()));
        return List.copyOf(contenantes);
    }

    /**
     * La troncature par la largeur : les niveaux les plus profonds
     * restent, la tête élaguée est signalée par l'ellipse — qui compte
     * dans la largeur. Quand tout tient, rien ne tombe.
     *
     * @param niveaux les noms des niveaux, de la plus externe à la plus interne
     * @param largeur la largeur disponible
     * @param mesure  le mesureur de largeur d'un nom (le peintre fournit le sien)
     * @return les niveaux affichés, éventuellement tête élaguée
     */
    public static List<String> tronque(List<String> niveaux, int largeur,
            ToIntFunction<String> mesure) {
        if (niveaux.isEmpty() || largeur(niveaux, mesure) <= largeur) {
            return niveaux;
        }
        for (int conserves = niveaux.size() - 1; conserves >= 1; conserves--) {
            List<String> suffixe = niveaux.subList(niveaux.size() - conserves,
                    niveaux.size());
            List<String> avecEllipse = new ArrayList<>(conserves + 1);
            avecEllipse.add(ELLIPSE);
            avecEllipse.addAll(suffixe);
            if (largeur(avecEllipse, mesure) <= largeur) {
                return List.copyOf(avecEllipse);
            }
        }
        return List.of(ELLIPSE, niveaux.get(niveaux.size() - 1));
    }

    private static int largeur(List<String> niveaux, ToIntFunction<String> mesure) {
        int total = 0;
        for (int i = 0; i < niveaux.size(); i++) {
            if (i > 0) {
                total += SEPARATEUR.length();
            }
            total += mesure.applyAsInt(niveaux.get(i));
        }
        return total;
    }
}
