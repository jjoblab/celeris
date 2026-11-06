package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.TreeIndent;
import jo.celeris.core.text.Document;

/**
 * Le formatage du code (B-25) : l'indentation se règle sur les niveaux
 * de l'arbre — le même arbre que la coloration (ADR-004) — chaque ligne
 * porte le retrait de son niveau, les lignes vides restent vides. Le
 * formatage est IDEMPOTENT par construction : les niveaux ne dépendent
 * pas du blanc, reformater le formaté redit le formaté.
 */
public final class Formateur {

    private Formateur() {
    }

    /**
     * Formate le document : le retrait de chaque ligne devient celui de
     * son niveau d'arbre, dans le style donné.
     *
     * @param doc     le document porteur
     * @param analyse l'analyse courante du document
     * @param style   le style d'indentation, choisi par l'hôte
     * @return le texte formaté
     */
    public static String formater(Document doc, Analysis analyse, IndentStyle style) {
        List<String> lignes = new ArrayList<>(doc.lineCount());
        for (int l = 0; l < doc.lineCount(); l++) {
            lignes.add(formaterLigne(doc, analyse, style, l));
        }
        String joint = String.join("\n", lignes);
        return joint.endsWith("\n") ? joint : joint + "\n";
    }

    private static String formaterLigne(Document doc, Analysis analyse, IndentStyle style,
                                        int ligne) {
        String contenu = doc.lineText(ligne).stripLeading();
        if (contenu.isBlank()) {
            return "";
        }
        int niveau = TreeIndent.levelAt(doc, analyse, ligne);
        if (commenceParFermant(contenu)) {
            // la ligne qui ferme un groupe porte le niveau de ce groupe :
            // levelAt compte le groupe fermant et un cran de plus — deux de moins
            niveau = Math.max(0, niveau - 2);
        }
        return style.indent(niveau) + contenu;
    }

    private static boolean commenceParFermant(String contenu) {
        return contenu.startsWith("}") || contenu.startsWith(")")
                || contenu.startsWith("]") || contenu.startsWith("</");
    }
}
