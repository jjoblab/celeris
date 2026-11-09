package jo.celeris.core.syntax;

import java.util.List;
import java.util.Set;

/**
 * La spécialisation Groovy du socle C-like (M7.6, H-10, dialecte
 * Gradle) : opérateurs propres ({@code ?., *., .., <=>, =~, ==~,
 * **}), commentaires de bloc NICHÉS par profondeur, chaînes triples
 * au guillemet ET à l'apostrophe, gabarits {@code ${…}} dans les
 * seules chaînes de guillemets (les guillemets internes aux
 * accolades ne clôturent pas), membres à fin de ligne — {@code def
 * nom(...)} est une méthode quand des parenthèses suivent le nom, un
 * champ sinon — et instructions libres repliées en blocs. Les chaînes
 * slashy et dollar-slashy restent hors périmètre : le script de
 * construction Gradle ne les emploie pas, leur absence est une limite
 * documentée, pas une erreur.
 */
final class GroovyDialect extends CLikeDialect {

    /** L'instance unique du dialecte Groovy. */
    static final GroovyDialect INSTANCE = new GroovyDialect();

    private static final Set<String> RESERVED_WORDS = Set.of(
            "abstract", "as", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "def", "default", "do", "double",
            "else", "enum", "extends", "false", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "in", "instanceof", "int",
            "interface", "it", "long", "native", "new", "null", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "trait", "transient",
            "true", "try", "void", "volatile", "while");

    private static final Set<String> TYPE_DECLARATIONS = Set.of(
            "class", "interface", "enum", "trait");

    private static final Set<String> MEMBER_WORDS = Set.of("def");

    private static final Set<String> MODIFIERS = Set.of(
            "private", "public", "protected", "static", "final", "abstract",
            "synchronized", "transient", "strictfp", "native", "default");

    private static final List<String> OPERATORS = List.of(
            ">>>=", "<<=", ">>=", ">>>", "==~", "<=>", "..<", "**=", "?.", "*.",
            "==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=",
            "/=", "%=", "&=", "|=", "^=", "<<", ">>", "**", "..", "=~", "->",
            "+", "-", "*", "/", "%", "=", "<", ">", "!", "&", "|", "^", "~",
            "?");

    private GroovyDialect() {
    }

    @Override
    Set<String> reservedWords() {
        return RESERVED_WORDS;
    }

    @Override
    List<String> operators() {
        return OPERATORS;
    }

    @Override
    boolean tripleSingleQuote() {
        return true;
    }

    @Override
    boolean lineHeadChunks() {
        return true;
    }

    @Override
    boolean closesString(char c, char delimiter) {
        return c == delimiter;
    }

    @Override
    boolean opensTemplate(CharSequence text, int at, int n, char delimiter) {
        return delimiter == '"' && text.charAt(at) == '$' && at + 1 < n
                && text.charAt(at + 1) == '{';
    }

    @Override
    Set<String> typeDeclarations() {
        return TYPE_DECLARATIONS;
    }

    @Override
    Set<String> modifiers() {
        return MODIFIERS;
    }

    @Override
    Set<String> memberWords() {
        return MEMBER_WORDS;
    }

    @Override
    int parseMember(TokenSlice slice, int start, int to, List<SynNode> members) {
        int afterTrivia = Jetons.sauterTrivia(slice, start);
        int afterHeader = skipHeader(slice, afterTrivia);
        if (afterHeader < to && MEMBER_WORDS.contains(Jetons.mot(slice, afterHeader))) {
            members.add(defDeclaration(slice, start, afterHeader));
            return MemberScans.memberEnd(slice, afterHeader, to);
        }
        members.add(freeInstruction(slice, start, to));
        return lineEnd(slice, start, to);
    }

    /** La déclaration {@code def} : méthode si des parenthèses suivent le nom, champ sinon. */
    private SynNode defDeclaration(TokenSlice slice, int start, int keyword) {
        String name = MemberScans.nameAfter(slice, keyword);
        boolean method = name != null && keyword + 2 < slice.size()
                && "(".equals(Jetons.texteCrochet(slice, keyword + 2));
        return MemberScans.lineMember(slice, start, keyword, method, name);
    }

    /** Une instruction libre dans un corps de type : pliage par blocs. */
    private static SynNode freeInstruction(TokenSlice slice, int start, int to) {
        List<SynNode> children = Corps.instructions(slice, start, to,
                CLikeKind.STATEMENT, CLikeKind.BLOC, CLikeKind.ERREUR);
        return SynNode.of(CLikeKind.STATEMENT, NodeCategory.STATEMENT,
                slice.start(start), Jetons.finDe(slice, to - 1),
                children.toArray(new SynNode[0]), null, hasError(children));
    }

    /** La fin d'une instruction libre : la ligne qui la porte. */
    private static int lineEnd(TokenSlice slice, int start, int to) {
        int startLine = slice.line(start);
        for (int i = start + 1; i < to; i++) {
            if (slice.line(i) > startLine) {
                return i;
            }
        }
        return to;
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
