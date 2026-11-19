package jo.celeris.core.syntax.clike;

import java.util.ArrayList;
import java.util.List;
import jo.celeris.core.syntax.Tokens;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.NodeKind;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TokenSlice;

/**
 * Corps et instructions des langages à accolades (ADR-004) : le découpage
 * tolérant d'un corps — instructions terminées par point-virgule, blocs
 * d'accolades imbriqués ; l'accolade non fermée devient un nœud d'erreur
 * et l'analyse continue. Les expressions restent des plages : l'arbre
 * sert la coloration, les plis, le plan et l'indentation, pas la
 * sémantique. Les sortes de nœuds sont celles du langage appelant.
 */
final class BodyParser {

    private BodyParser() {
    }

    /**
     * Découpe une plage de jetons en instructions et blocs.
     *
     * @param slice       la tranche du chunk
     * @param depuis      le premier jeton du corps
     * @param vers        le jeton de clôture (exclu)
     * @param instruction la sorte du langage pour une instruction
     * @param bloc        la sorte du langage pour un bloc
     * @param erreur      la sorte du langage pour une structure malformée
     * @return les nœuds du corps, dans l'ordre
     */
    static List<SynNode> instructions(TokenSlice slice, int depuis, int vers,
                                      NodeKind instruction, NodeKind bloc,
                                      NodeKind erreur) {
        List<SynNode> instructions = new ArrayList<>();
        int i = depuis;
        int start = depuis;
        while (i < vers) {
            TokenKind sorte = slice.kind(i);
            if (sorte == TokenKind.OPEN_BRACKET
                    && "{".equals(Tokens.bracketText(slice, i))) {
                addStatement(slice, start, i, instructions, instruction);
                i = addBlock(slice, i, vers, instructions, instruction,
                        bloc, erreur);
                start = i;
                continue;
            }
            if (sorte == TokenKind.OPEN_BRACKET) {
                int fermante = slice.matchOf(i);
                if (fermante < 0) {
                    instructions.add(SynNode.of(erreur, NodeCategory.ERROR,
                            slice.start(i), Tokens.finDe(slice, vers - 1),
                            new SynNode[0], null, true));
                    return instructions;
                }
                i = fermante + 1;
                continue;
            }
            if (sorte == TokenKind.CLOSE_BRACKET) {
                addStatement(slice, start, i, instructions, instruction);
                return instructions;
            }
            if (sorte == TokenKind.PUNCTUATION
                    && ";".equals(Tokens.mot(slice, i))) {
                addStatement(slice, start, i + 1, instructions, instruction);
                start = i + 1;
            }
            i++;
        }
        if (start < vers) {
            addStatement(slice, start, vers, instructions, instruction);
        }
        return instructions;
    }

    private static void addStatement(TokenSlice slice, int start, int end,
                                           List<SynNode> instructions,
                                           NodeKind instruction) {
        int dernier = end - 1;
        while (dernier >= start && dernier < slice.size()
                && Tokens.isTrivia(slice, dernier)) {
            dernier--;
        }
        if (start > dernier || start >= slice.size()) {
            return;
        }
        instructions.add(SynNode.of(instruction, NodeCategory.STATEMENT,
                slice.start(start), Tokens.finDe(slice, dernier), new SynNode[0],
                null, false));
    }

    private static int addBlock(TokenSlice slice, int ouvrante, int vers,
                                   List<SynNode> instructions,
                                   NodeKind instruction, NodeKind bloc,
                                   NodeKind erreur) {
        int fermante = slice.matchOf(ouvrante);
        if (fermante < 0) {
            instructions.add(SynNode.of(erreur, NodeCategory.ERROR,
                    slice.start(ouvrante), Tokens.finDe(slice, vers - 1),
                    new SynNode[0], null, true));
            return vers;
        }
        List<SynNode> body = instructions(slice, ouvrante + 1, fermante,
                instruction, bloc, erreur);
        instructions.add(SynNode.of(bloc, NodeCategory.BLOCK,
                slice.start(ouvrante), slice.end(fermante), body.toArray(new SynNode[0]),
                null, false));
        return fermante + 1;
    }
}
