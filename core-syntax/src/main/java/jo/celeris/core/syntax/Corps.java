package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;

/**
 * Corps et instructions des langages à accolades (ADR-004) : le découpage
 * tolérant d'un corps — instructions terminées par point-virgule, blocs
 * d'accolades imbriqués ; l'accolade non fermée devient un nœud d'erreur
 * et l'analyse continue. Les expressions restent des plages : l'arbre
 * sert la coloration, les plis, le plan et l'indentation, pas la
 * sémantique. Les sortes de nœuds sont celles du langage appelant.
 */
final class Corps {

    private Corps() {
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
        int debut = depuis;
        while (i < vers) {
            TokenKind sorte = slice.kind(i);
            if (sorte == TokenKind.OPEN_BRACKET
                    && "{".equals(Jetons.texteCrochet(slice, i))) {
                ajouterInstruction(slice, debut, i, instructions, instruction);
                i = ajouterBloc(slice, i, vers, instructions, instruction,
                        bloc, erreur);
                debut = i;
                continue;
            }
            if (sorte == TokenKind.OPEN_BRACKET) {
                int fermante = slice.matchOf(i);
                if (fermante < 0) {
                    instructions.add(SynNode.of(erreur, NodeCategory.ERROR,
                            slice.start(i), Jetons.finDe(slice, vers - 1),
                            new SynNode[0], null, true));
                    return instructions;
                }
                i = fermante + 1;
                continue;
            }
            if (sorte == TokenKind.CLOSE_BRACKET) {
                ajouterInstruction(slice, debut, i, instructions, instruction);
                return instructions;
            }
            if (sorte == TokenKind.PUNCTUATION
                    && ";".equals(Jetons.mot(slice, i))) {
                ajouterInstruction(slice, debut, i + 1, instructions, instruction);
                debut = i + 1;
            }
            i++;
        }
        if (debut < vers) {
            ajouterInstruction(slice, debut, vers, instructions, instruction);
        }
        return instructions;
    }

    private static void ajouterInstruction(TokenSlice slice, int debut, int fin,
                                           List<SynNode> instructions,
                                           NodeKind instruction) {
        int dernier = fin - 1;
        while (dernier >= debut && dernier < slice.size()
                && Jetons.estTrivia(slice, dernier)) {
            dernier--;
        }
        if (debut > dernier || debut >= slice.size()) {
            return;
        }
        instructions.add(SynNode.of(instruction, NodeCategory.STATEMENT,
                slice.start(debut), Jetons.finDe(slice, dernier), new SynNode[0],
                null, false));
    }

    private static int ajouterBloc(TokenSlice slice, int ouvrante, int vers,
                                   List<SynNode> instructions,
                                   NodeKind instruction, NodeKind bloc,
                                   NodeKind erreur) {
        int fermante = slice.matchOf(ouvrante);
        if (fermante < 0) {
            instructions.add(SynNode.of(erreur, NodeCategory.ERROR,
                    slice.start(ouvrante), Jetons.finDe(slice, vers - 1),
                    new SynNode[0], null, true));
            return vers;
        }
        List<SynNode> corps = instructions(slice, ouvrante + 1, fermante,
                instruction, bloc, erreur);
        instructions.add(SynNode.of(bloc, NodeCategory.BLOCK,
                slice.start(ouvrante), slice.end(fermante), corps.toArray(new SynNode[0]),
                null, false));
        return fermante + 1;
    }
}
