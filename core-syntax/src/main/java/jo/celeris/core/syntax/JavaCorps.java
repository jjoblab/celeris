package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;

/**
 * Corps et instructions Java (ADR-004) : le découpage tolérant d'un corps —
 * instructions terminées par point-virgule, blocs d'accolades imbriqués ;
 * l'accolade non fermée devient un nœud d'erreur et l'analyse continue.
 * Les expressions restent des plages : l'arbre sert la coloration, les
 * plis, le plan et l'indentation, pas la sémantique.
 */
final class JavaCorps {

    private JavaCorps() {
    }

    /**
     * Découpe une plage de jetons en instructions et blocs.
     *
     * @param slice la tranche du chunk
     * @param depuis le premier jeton du corps
     * @param vers   le jeton de clôture (exclu)
     * @return les nœuds du corps, dans l'ordre
     */
    static List<SynNode> instructions(TokenSlice slice, int depuis, int vers) {
        List<SynNode> instructions = new ArrayList<>();
        int i = depuis;
        int debut = depuis;
        while (i < vers) {
            TokenKind sorte = slice.kind(i);
            if (sorte == TokenKind.OPEN_BRACKET
                    && "{".equals(Jetons.texteCrochet(slice, i))) {
                ajouterInstruction(slice, debut, i, instructions);
                i = ajouterBloc(slice, i, vers, instructions);
                debut = i;
                continue;
            }
            if (sorte == TokenKind.OPEN_BRACKET) {
                int fermante = slice.matchOf(i);
                if (fermante < 0) {
                    instructions.add(SynNode.of(JavaKind.ERREUR, NodeCategory.ERROR,
                            slice.start(i), Jetons.finDe(slice, vers - 1),
                            new SynNode[0], null, true));
                    return instructions;
                }
                i = fermante + 1;
                continue;
            }
            if (sorte == TokenKind.CLOSE_BRACKET) {
                ajouterInstruction(slice, debut, i, instructions);
                return instructions;
            }
            if (sorte == TokenKind.PUNCTUATION
                    && ";".equals(Jetons.mot(slice, i))) {
                ajouterInstruction(slice, debut, i + 1, instructions);
                debut = i + 1;
            }
            i++;
        }
        if (debut < vers) {
            ajouterInstruction(slice, debut, vers, instructions);
        }
        return instructions;
    }

    private static void ajouterInstruction(TokenSlice slice, int debut, int fin,
                                           List<SynNode> instructions) {
        int dernier = fin - 1;
        while (dernier >= debut && dernier < slice.size()
                && Jetons.estTrivia(slice, dernier)) {
            dernier--;
        }
        if (debut > dernier || debut >= slice.size()) {
            return;
        }
        instructions.add(SynNode.of(JavaKind.STATEMENT, NodeCategory.STATEMENT,
                slice.start(debut), Jetons.finDe(slice, dernier), new SynNode[0],
                null, false));
    }

    private static int ajouterBloc(TokenSlice slice, int ouvrante, int vers,
                                   List<SynNode> instructions) {
        int fermante = slice.matchOf(ouvrante);
        if (fermante < 0) {
            instructions.add(SynNode.of(JavaKind.ERREUR, NodeCategory.ERROR,
                    slice.start(ouvrante), Jetons.finDe(slice, vers - 1),
                    new SynNode[0], null, true));
            return vers;
        }
        List<SynNode> corps = instructions(slice, ouvrante + 1, fermante);
        instructions.add(SynNode.of(JavaKind.BLOC, NodeCategory.BLOCK,
                slice.start(ouvrante), slice.end(fermante), corps.toArray(new SynNode[0]),
                null, false));
        return fermante + 1;
    }
}
