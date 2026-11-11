package jo.celeris.serveur.java;

import java.util.ArrayList;
import java.util.List;

/**
 * Les plis du mini serveur : les corps d'accolades qui courent sur au
 * moins deux lignes, et les blocs de commentaires qui en courent
 * autant — la fin du pli EXCLUT la ligne de fermante (le pli du corps
 * laisse voir l'accolade qui le referme, comme les éditeurs de
 * référence).
 */
public final class JavaFolds {

    /** Un pli : la ligne d'ouverture et la dernière ligne repliée. */
    public record Fold(int startLine, int endLine) {
    }

    private JavaFolds() {
    }

    /**
     * Calcule les plis d'un document : corps d'accolades et blocs de
     * commentaires.
     *
     * @param source le document suivi
     * @return les plis, dans l'ordre du document
     */
    public static List<Fold> of(JavaSource source) {
        List<Fold> plis = new ArrayList<>();
        String texte = source.stripped().text();
        java.util.Deque<Integer> ouvertes = new java.util.ArrayDeque<>();
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == '{') {
                ouvertes.push(i);
            } else if (c == '}' && !ouvertes.isEmpty()) {
                int ouverture = ouvertes.pop();
                int ligneOuverture = source.lineOf(ouverture);
                int ligneFermante = source.lineOf(i);
                if (ligneFermante - 1 > ligneOuverture) {
                    plis.add(new Fold(ligneOuverture, ligneFermante - 1));
                }
            }
        }
        for (JavaSource.StrippedCode.Comment bloc
                : source.stripped().comments()) {
            int debut = source.lineOf(bloc.start());
            int fin = source.lineOf(Math.max(0, bloc.end() - 1));
            if (fin > debut) {
                plis.add(new Fold(debut, fin));
            }
        }
        plis.sort((a, b) -> Integer.compare(a.startLine(), b.startLine()));
        return List.copyOf(plis);
    }
}
