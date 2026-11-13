package jo.celeris.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jo.celeris.android.host.spi.AnalyseurPile;
import jo.celeris.android.host.spi.CadrePile;
import jo.celeris.android.widgets.ExceptionView;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * La trace d'exécution de la démonstration (catégorie E de
 * l'inventaire — « Exception Details », la surface dormait) : l'échec
 * SIMULÉ naît du document réel — le type englobant du caret porte la
 * classe, la méthode englobante porte le sommet À LA LIGNE DU CARET
 * (jamais deviné : le plan seul sait), les cadres suivants descendent
 * les méthodes du fichier à leurs lignes de déclaration. La trace se
 * rend au format des piles de la plateforme puis se relit par
 * l'analyseur (E-06) — la boucle se prouve au banc. La pile se
 * transpose en cadres de la carte : le cadre du fichier ouvert saute,
 * l'étranger vit muet.
 */
final class TraceExecution {

    private TraceExecution() {
    }

    /** L'échec d'une exécution : le type et le message de l'exception,
     *  les cadres de la pile dans l'ordre de la trace. */
    record Echec(String type, String message, List<CadrePile> cadres) {
    }

    /**
     * L'échec simulé du fichier ouvert : la classe porte le type
     * englobant du caret, le sommet vit à la ligne du caret dans sa
     * méthode, les cadres suivants descendent les méthodes du fichier
     * à leurs lignes de déclaration — la trace se rend, puis se relit
     * par l'analyseur de piles.
     *
     * @param type     le type de l'exception (par exemple
     *                 {@code java.lang.Exception})
     * @param message  le message de l'échec
     * @param fichier  le nom du fichier ouvert — les cadres y vivent
     * @param doc      le document ouvert, jamais muté
     * @param plan     le plan du fichier ouvert
     * @param caret    la position du curseur — l'échec s'y produit
     * @return l'échec et sa pile, null quand le fichier ne porte ni
     *         type ni méthode (rien à faire échouer ne se devine
     *         jamais)
     */
    static @Nullable Echec echec(String type, String message,
            String fichier, Document doc, List<OutlineEntry> plan, int caret) {
        String classe = null;
        OutlineEntry sommet = null;
        List<OutlineEntry> methodes = new ArrayList<>();
        for (OutlineEntry entree : plan) {
            if (entree.category() == NodeCategory.TYPE) {
                if (entree.startOffset() <= caret
                        && caret <= entree.endOffset()) {
                    classe = entree.name();
                }
            }
            if (entree.category() == NodeCategory.FUNCTION) {
                methodes.add(entree);
                if (entree.startOffset() <= caret
                        && caret <= entree.endOffset()) {
                    sommet = entree;
                }
            }
        }
        if (classe == null || methodes.isEmpty()) {
            return null;
        }
        StringBuilder rendu = new StringBuilder(type).append(": ")
                .append(message).append('\n');
        if (sommet != null) {
            rendu.append(cadre(classe, sommet.name(), fichier,
                    doc.lineForOffset(caret) + 1));
        }
        for (OutlineEntry methode : methodes) {
            if (methode != sommet) {
                rendu.append(cadre(classe, methode.name(), fichier,
                        methode.startLine() + 1));
            }
        }
        return new Echec(type, message, AnalyseurPile.parser(rendu.toString()));
    }

    /**
     * Les cadres de la carte : le libellé lu de la pile (« L12 ·
     * classe.methode »), le saut vers la ligne quand le cadre vit au
     * fichier ouvert — l'étranger vit muet (le lieu étranger ne se
     * devine jamais).
     *
     * @param echec        l'échec de l'exécution
     * @param fichierOuvert le nom du fichier ouvert
     * @param sauteur      le saut vers une ligne (à partir de un),
     *                     branché par l'hôte
     * @return les cadres, dans l'ordre de la trace
     */
    static List<ExceptionView.Cadre> cadres(Echec echec, String fichierOuvert,
            Consumer<Integer> sauteur) {
        List<ExceptionView.Cadre> cadres = new ArrayList<>(
                echec.cadres().size());
        for (CadrePile pile : echec.cadres()) {
            boolean ouvert = pile.fichier().equals(fichierOuvert);
            cadres.add(new ExceptionView.Cadre(
                    "L" + pile.ligne() + " · " + pile.classe() + "."
                            + pile.methode(),
                    ouvert ? () -> sauteur.accept(pile.ligne()) : null));
        }
        return cadres;
    }

    /** Un cadre de la trace — « at classe.methode(fichier:ligne) ». */
    private static String cadre(String classe, String methode,
            String fichier, int ligne) {
        return "\tat " + classe + '.' + methode + '(' + fichier + ':'
                + ligne + ")\n";
    }
}
