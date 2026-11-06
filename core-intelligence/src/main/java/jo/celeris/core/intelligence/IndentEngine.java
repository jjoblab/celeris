package jo.celeris.core.intelligence;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.Chunk;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.text.Document;

/**
 * Profondeur d'indentation dérivée de l'arbre (M5, A-06/A-09) : le nombre
 * de nœuds structurants qui englobent une position — la même lecture que
 * {@code TreeIndent} du socle, ramenée à l'offset du caret. Les langages à
 * balises n'émettent pas de crochets lexicographiques : leur imbrication
 * ne se lit que dans les nœuds, jamais dans le texte seul.
 */
public final class IndentEngine {

    private IndentEngine() {
    }

    /**
     * La profondeur d'imbrication à une position : les nœuds structurants
     * dont la plage contient strictement la position (ouvrir avant, fermer
     * après).
     *
     * @param doc      le document porteur
     * @param analysis l'analyse courante
     * @param offset   la position interrogée
     * @return son niveau d'imbrication, 0 hors de toute structure
     */
    public static int depthAtCaret(Document doc, @Nullable Analysis analysis, int offset) {
        if (analysis == null) {
            return 0;
        }
        int depth = 0;
        for (Chunk chunk : analysis.tree().chunks()) {
            int base = analysis.tree().chunkStartOffset(doc, chunk);
            depth += englobantAu(chunk.root(), base, offset);
        }
        return depth;
    }

    private static int englobantAu(SynNode node, int base, int offset) {
        int total = 0;
        if (node.category() != NodeCategory.OTHER
                && base + node.start() < offset && offset < base + node.end()) {
            total = 1;
        }
        for (SynNode enfant : node.children()) {
            total += englobantAu(enfant, base, offset);
        }
        return total;
    }

    /**
     * Le préfixe de blancs d'une ligne (espaces et tabulations).
     *
     * @param ligne le texte de la ligne
     * @return ses blancs de tête, éventuellement vides
     */
    public static String leadingWhitespace(String ligne) {
        int fin = 0;
        while (fin < ligne.length()
                && (ligne.charAt(fin) == ' ' || ligne.charAt(fin) == '\t')) {
            fin++;
        }
        return ligne.substring(0, fin);
    }

    /**
     * La mesure en unités d'indentation d'un préfixe de blancs.
     *
     * @param blancs le préfixe (espaces et tabulations)
     * @param style  le style de référence
     * @return le nombre d'unités couvertes, arrondi vers le bas
     */
    public static int units(String blancs, IndentStyle style) {
        return blancs.length() / Math.max(1, style.width());
    }
}
