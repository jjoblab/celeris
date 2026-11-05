package jo.celeris.core.syntax.grammars;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TokenSlice;

/**
 * Parseur générique de la voie de grammaires (ADR-004) : sans arbre de
 * déclarations — la voie ne connaît pas la structure des langages
 * qu'elle colore — il découpe les chunks au premier jeton de chaque
 * ligne de premier niveau (la ré-analyse reste locale à la ligne
 * éditée) et matérialise une région de pli par paire de crochets
 * appariés qui traverse les lignes : chaînes triples, tableaux,
 * sections délimitées. Jamais d'exception : le crochet non fermé
 * n'ouvre aucune région, la malformation se tolère en plages.
 */
final class GrammaireParseur implements LanguageParser {

    @Override
    public int chunkLevel() {
        return 0;
    }

    @Override
    public boolean startsChunk(TokenSlice slice, int index, int depth) {
        return depth == 0 && index > 0 && slice.line(index) > slice.line(index - 1);
    }

    @Override
    public SynNode parseChunk(TokenSlice slice) {
        if (slice.size() == 0) {
            return SynNode.of(GrammaireKind.RACINE, NodeCategory.OTHER, 0, 0,
                    new SynNode[0], null, false);
        }
        List<SynNode> regions = new ArrayList<>();
        for (int i = 0; i < slice.size(); i++) {
            if (slice.kind(i) != TokenKind.OPEN_BRACKET) {
                continue;
            }
            int fermante = slice.matchOf(i);
            if (fermante > i && slice.line(fermante) > slice.line(i)) {
                regions.add(SynNode.of(GrammaireKind.BLOC, NodeCategory.BLOCK,
                        slice.start(i), slice.end(fermante), new SynNode[0], null,
                        false));
            }
        }
        return SynNode.of(GrammaireKind.RACINE, NodeCategory.OTHER, slice.start(0),
                slice.end(slice.size() - 1), regions.toArray(new SynNode[0]), null,
                false);
    }
}
