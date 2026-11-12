package jo.celeris.core.syntax.clike;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.syntax.Jetons;
import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TokenSlice;
import org.jspecify.annotations.Nullable;

/**
 * Le parseur de la famille C (M7.6, H-10) : le socle partagé des
 * analyseurs écrits — découpe des chunks de haut niveau, analyse des
 * types en membres, repli en instructions libres, têtes de
 * déclarations sautées (annotations et modificateurs). Les règles
 * propres à une langue vivent dans son {@link CLikeDialect} ; jamais
 * d'exception : la malformation devient un nœud d'erreur et l'analyse
 * continue.
 */
final class CLikeParser implements LanguageParser {

    private final CLikeDialect dialect;

    CLikeParser(CLikeDialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public int chunkLevel() {
        return 0;
    }

    @Override
    public boolean startsChunk(TokenSlice slice, int index, int depth) {
        if (depth != 0 || index == 0) {
            return false;
        }
        TokenKind sort = slice.kind(index);
        boolean lineHead = slice.line(index) > slice.line(index - 1);
        boolean candidate = (dialect.lineHeadChunks()
                && sort == TokenKind.IDENTIFIER && lineHead)
                || sort == TokenKind.ANNOTATION
                || (sort == TokenKind.KEYWORD
                        && (dialect.typeDeclarations().contains(Jetons.mot(slice, index))
                                || dialect.memberWords().contains(Jetons.mot(slice, index))
                                || dialect.modifiers().contains(Jetons.mot(slice, index))));
        if (!candidate) {
            return false;
        }
        TokenKind previous = slice.kind(index - 1);
        if (!dialect.lineHeadChunks()) {
            return previous == TokenKind.PUNCTUATION
                    || previous == TokenKind.CLOSE_BRACKET
                    || previous == TokenKind.COMMENT
                    || previous == TokenKind.DOC
                    || previous == TokenKind.ANNOTATION;
        }
        return lineHead
                || previous == TokenKind.PUNCTUATION
                || previous == TokenKind.CLOSE_BRACKET
                || previous == TokenKind.COMMENT
                || previous == TokenKind.DOC
                || previous == TokenKind.ANNOTATION;
    }

    @Override
    public SynNode parseChunk(TokenSlice slice) {
        if (slice.size() == 0) {
            return SynNode.of(CLikeKind.AUTRE, NodeCategory.OTHER, 0, 0,
                    new SynNode[0], null, false);
        }
        int afterTrivia = Jetons.sauterTrivia(slice, 0);
        int afterHeader = skipHeader(slice, afterTrivia);
        if (afterHeader < slice.size()
                && dialect.isTypeDeclaration(slice, afterHeader)) {
            return parseType(slice, 0, afterHeader);
        }
        if (afterHeader < slice.size()
                && dialect.memberWords().contains(Jetons.mot(slice, afterHeader))) {
            List<SynNode> members = new ArrayList<>();
            dialect.parseMember(slice, 0, slice.size(), members);
            if (!members.isEmpty()) {
                return members.get(members.size() - 1);
            }
        }
        List<SynNode> children = Corps.instructions(slice, 0, slice.size(),
                CLikeKind.STATEMENT, CLikeKind.BLOC, CLikeKind.ERREUR);
        return SynNode.of(CLikeKind.AUTRE, NodeCategory.OTHER, 0,
                Jetons.finDe(slice, slice.size() - 1), children.toArray(new SynNode[0]),
                null, hasError(children));
    }

    /** Le type déclaré : son nom, ses membres entre accolades, l'erreur à la fermeture manquante. */
    private SynNode parseType(TokenSlice slice, int start, int keyword) {
        String name = typeName(slice, keyword);
        int opening = Jetons.chercherAccolade(slice, keyword + dialect.typeBraceOffset());
        if (opening < 0) {
            return leafType(slice, start, keyword, name);
        }
        int closing = slice.matchOf(opening);
        if (closing < 0) {
            List<SynNode> members = parseMembers(slice, opening + 1, slice.size());
            members.add(SynNode.of(CLikeKind.ERREUR, NodeCategory.ERROR,
                    slice.start(opening), Jetons.finDe(slice, slice.size() - 1),
                    new SynNode[0], null, true));
            return SynNode.of(CLikeKind.TYPE, NodeCategory.TYPE,
                    slice.start(start), Jetons.finDe(slice, slice.size() - 1),
                    members.toArray(new SynNode[0]), name, true);
        }
        List<SynNode> members = parseMembers(slice, opening + 1, closing);
        return SynNode.of(CLikeKind.TYPE, NodeCategory.TYPE, slice.start(start),
                slice.end(closing), members.toArray(new SynNode[0]), name, false);
    }

    /** Le type sans accolade : une feuille, son erreur scannée pour Java. */
    private SynNode leafType(TokenSlice slice, int start, int keyword,
            @Nullable String name) {
        int end = Math.min(keyword + 2, slice.size() - 1);
        boolean error = false;
        if (dialect.scanLeafErrors()) {
            for (int i = keyword; i <= end; i++) {
                if (slice.kind(i) == TokenKind.ERROR) {
                    error = true;
                }
            }
        }
        return SynNode.of(CLikeKind.TYPE, NodeCategory.TYPE, slice.start(start),
                Jetons.finDe(slice, end), new SynNode[0], name, error);
    }

    /** Le nom d'un type : tout mot pour Java, un identificateur sinon. */
    private @Nullable String typeName(TokenSlice slice, int keyword) {
        if (keyword + 1 >= slice.size()) {
            return null;
        }
        if (dialect.anyWordAsTypeName()) {
            return Jetons.mot(slice, keyword + 1);
        }
        TokenKind sort = slice.kind(keyword + 1);
        if (sort == TokenKind.IDENTIFIER || sort == TokenKind.TYPE
                || sort == TokenKind.CONSTANT) {
            return Jetons.mot(slice, keyword + 1);
        }
        return null;
    }

    /** Les membres d'un corps de type, du premier au dernier token utile. */
    private List<SynNode> parseMembers(TokenSlice slice, int from, int to) {
        List<SynNode> members = new ArrayList<>();
        int i = from;
        while (i < to) {
            int afterTrivia = Jetons.sauterTrivia(slice, i);
            if (afterTrivia >= to) {
                break;
            }
            i = parseOneMember(slice, i, to, members);
        }
        return members;
    }

    /** Un membre : le type imbriqué par le socle, le reste par le dialecte. */
    private int parseOneMember(TokenSlice slice, int start, int to,
            List<SynNode> members) {
        int afterTrivia = Jetons.sauterTrivia(slice, start);
        int afterHeader = skipHeader(slice, afterTrivia);
        if (afterHeader < to && dialect.isTypeDeclaration(slice, afterHeader)) {
            members.add(parseType(slice, start, afterHeader));
            return dialect.advanceAfterType(slice, afterHeader, to);
        }
        return dialect.parseMember(slice, start, to, members);
    }

    /** La tête d'une déclaration : annotations (et leurs arguments) et modificateurs. */
    private int skipHeader(TokenSlice slice, int from) {
        return dialect.skipHeader(slice, from);
    }

    private static boolean hasError(List<SynNode> children) {
        for (SynNode child : children) {
            if (child.isError()) {
                return true;
            }
        }
        return false;
    }
}
