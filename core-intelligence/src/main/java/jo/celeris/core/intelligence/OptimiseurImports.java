package jo.celeris.core.intelligence;

import jo.celeris.core.intelligence.internal.scan.IdentifierScan;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.text.Document;

/**
 * L'optimisation des imports (B-27) : chaque import est rapporté
 * utilisé ou non — SIGNALÉ, jamais supprimé en silence. Le sort d'un
 * import appartient à l'hôte et à l'utilisateur : l'analyseur éclaire,
 * il ne décide pas.
 *
 * <p>L'usage compte dans le code, hors des lignes d'import elles-mêmes
 * — les jetons du lexique décident, jamais les chaînes ni les
 * commentaires. Le nom simple d'un import est son dernier segment,
 * l'import statique compris.</p>
 */
public final class OptimiseurImports {

    /** Le rapport d'un import : sa ligne, son texte complet, son usage. */
    public record RapportImport(int line, String importComplet, boolean utilise) {
    }

    private OptimiseurImports() {
    }

    /**
     * Analyse les imports du document.
     *
     * @param doc     le document porteur
     * @param analyse l'analyse courante du document
     * @return un rapport par ligne d'import, dans l'ordre du source
     */
    public static List<RapportImport> analyser(Document doc, Analysis analyse) {
        Set<Integer> lignesImport = new HashSet<>();
        for (int l = 0; l < doc.lineCount(); l++) {
            if (estImport(doc.lineText(l))) {
                lignesImport.add(l);
            }
        }
        Set<String> nomsUtilises = new HashSet<>();
        for (IdentifierScan.NomOffset releve : IdentifierScan.scanner(doc, analyse.lex())) {
            if (!lignesImport.contains(doc.lineForOffset(releve.offset()))) {
                nomsUtilises.add(releve.nom());
            }
        }
        List<RapportImport> rapports = new ArrayList<>();
        for (int l = 0; l < doc.lineCount(); l++) {
            String text = doc.lineText(l).strip();
            if (lignesImport.contains(l)) {
                rapports.add(new RapportImport(l, text,
                        nomsUtilises.contains(nomSimple(text))));
            }
        }
        return List.copyOf(rapports);
    }

    private static boolean estImport(String line) {
        String text = line.strip();
        return text.startsWith("import ") && text.endsWith(";");
    }

    /** Le nom simple d'une ligne d'import : le dernier segment, point exclu. */
    private static String nomSimple(String importComplet) {
        String body = importComplet.substring("import ".length(),
                importComplet.length() - 1);
        if (body.startsWith("static ")) {
            body = body.substring("static ".length());
        }
        int dernierPoint = body.lastIndexOf('.');
        return dernierPoint < 0 ? body : body.substring(dernierPoint + 1);
    }
}
