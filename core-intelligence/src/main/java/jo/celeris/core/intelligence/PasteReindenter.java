package jo.celeris.core.intelligence;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.text.Document;

/**
 * Le collage avec réindentation (M5, A-09) : un collage multiligne
 * s'aligne sur le contexte d'accueil — la profondeur lue dans l'arbre au
 * point de collage — en conservant la structure relative du texte collé.
 * La première ligne ne bouge pas : elle complète la ligne d'accueil.
 */
public final class PasteReindenter {

    private PasteReindenter() {
    }

    /**
     * Réindenté le texte collé pour un point d'accueil.
     *
     * @param doc       le document, jamais muté
     * @param analysis  l'analyse courante, éventuellement absente
     * @param offset    le point d'accueil du collage
     * @param pasted    le texte collé
     * @param style     le style d'indentation de l'hôte
     * @return le texte à coller : la première ligne inchangée, les
     *         suivantes réindentées à l'accueil, structure relative
     *         conservée
     */
    public static String reindent(Document doc, @Nullable Analysis analysis, int offset,
            String pasted, IndentStyle style) {
        String[] lignes = pasted.split("\n", -1);
        if (lignes.length <= 1) {
            return pasted;
        }
        int base = baseUnits(doc, analysis, offset, style);
        int premiere = IndentEngine.units(
                IndentEngine.leadingWhitespace(lignes[0]), style);
        StringBuilder resultat = new StringBuilder(pasted.length() + 16);
        resultat.append(lignes[0]);
        for (int i = 1; i < lignes.length; i++) {
            resultat.append('\n');
            String blancs = IndentEngine.leadingWhitespace(lignes[i]);
            int relatif = IndentEngine.units(blancs, style) - premiere;
            int niveau = Math.max(0, base + relatif);
            resultat.append(style.indent(niveau));
            resultat.append(lignes[i], blancs.length(), lignes[i].length());
        }
        return resultat.toString();
    }

    /**
     * Le niveau d'accueil : la profondeur de l'arbre au point de collage,
     * l'indentation de la ligne d'accueil en repli.
     */
    private static int baseUnits(Document doc, @Nullable Analysis analysis, int offset,
            IndentStyle style) {
        if (analysis != null) {
            return IndentEngine.depthAtCaret(doc, analysis, offset);
        }
        int ligne = doc.lineForOffset(offset);
        return IndentEngine.units(
                IndentEngine.leadingWhitespace(doc.lineText(ligne)), style);
    }
}
