package jo.celeris.core.syntax.clike;

import java.util.List;
import java.util.Set;
import jo.celeris.core.syntax.Jetons;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TokenSlice;

/**
 * La spécialisation Java du socle C-like (M7.6, H-10) : commentaires
 * de bloc PLATS (la première fermeture clôt tout), textes en bloc
 * soudés au jeton suivant, membres à point-virgule au nom deviné, le
 * nom d'un type lu quelle que soit la sorte du mot qui suit son
 * mot-clé, l'accolade cherchée après le nom, la feuille qui scanne
 * ses erreurs, et l'annotation {@code @interface} qui déclare un
 * type. L'instantané du corpus ({@code ClikeSnapshotsTest}) prouve
 * l'identité avec l'analyseur d'avant la factorisation.
 */
final class JavaDialect extends CLikeDialect {

    /** L'instance unique du dialecte Java. */
    static final JavaDialect INSTANCE = new JavaDialect();

    private static final Set<String> RESERVED_WORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "record", "return", "sealed",
            "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "var", "void", "volatile", "while",
            "yield", "true", "false", "null", "module", "open", "requires", "transitive",
            "exports", "opens", "to", "uses", "provides", "with", "permits", "non");

    private static final Set<String> TYPE_DECLARATIONS = Set.of(
            "class", "interface", "enum", "record");

    private static final Set<String> MODIFIERS = Set.of(
            "public", "private", "protected", "static", "final", "abstract", "sealed",
            "strictfp", "transient", "volatile", "native", "synchronized", "default",
            "non", "open", "module", "transitive");

    private static final List<String> OPERATORS = List.of(
            ">>>=", "<<=", ">>=", ">>>", "==", "!=", "<=", ">=", "&&", "||", "++",
            "--", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<", ">>", "->",
            "+", "-", "*", "/", "%", "=", "<", ">", "!", "&", "|", "^", "~", "?");

    private JavaDialect() {
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
    boolean nestedBlockComments() {
        return false;
    }

    @Override
    boolean weldTokens() {
        return true;
    }

    @Override
    boolean keepEmptyTripleLineToken() {
        return true;
    }

    @Override
    boolean closesString(char c, char delimiter) {
        return c == delimiter;
    }

    @Override
    boolean opensTemplate(CharSequence text, int at, int n, char delimiter) {
        return false;
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
    boolean anyWordAsTypeName() {
        return true;
    }

    @Override
    int typeBraceOffset() {
        return 2;
    }

    @Override
    boolean scanLeafErrors() {
        return true;
    }

    @Override
    boolean isTypeDeclaration(TokenSlice slice, int index) {
        if (slice.kind(index) != TokenKind.KEYWORD) {
            return slice.kind(index) == TokenKind.ANNOTATION
                    && "@interface".equals(Jetons.mot(slice, index));
        }
        return TYPE_DECLARATIONS.contains(Jetons.mot(slice, index));
    }

    @Override
    int advanceAfterType(TokenSlice slice, int keyword, int to) {
        int opening = Jetons.chercherAccolade(slice, keyword + typeBraceOffset());
        if (opening < 0) {
            return to;
        }
        int closing = slice.matchOf(opening);
        if (closing < 0 || closing >= to) {
            return to;
        }
        return closing + 1;
    }

    @Override
    int parseMember(TokenSlice slice, int start, int to, List<SynNode> members) {
        return MemberScans.parseSemicolonMember(slice, start, to, members);
    }
}
