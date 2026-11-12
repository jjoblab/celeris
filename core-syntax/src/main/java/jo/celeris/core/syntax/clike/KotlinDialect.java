package jo.celeris.core.syntax.clike;

import java.util.List;
import java.util.Set;
import jo.celeris.core.syntax.Jetons;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.syntax.TokenSlice;

/**
 * La spécialisation Kotlin du socle C-like (M7.6, H-10) : opérateurs
 * propres (appel sûr, elvis, références, intervalles — testés avant
 * la ponctuation, le deux-points isolé est une ponctuation),
 * commentaires de bloc NICHÉS par profondeur, chaînes brutes au
 * triple guillemet, gabarits {@code ${…}} dont les guillemets
 * internes ne clôturent pas, membres à fin de ligne — la langue vit
 * sans point-virgule — dont les fonctions ({@code fun},
 * {@code init}) et propriétés ({@code val}, {@code var}) de premier
 * niveau ouvrent leur chunk. La fermeture des chaînes délimitées ne
 * regarde que le guillemet, quelle que soit l'ouverture — comportement
 * hérité, l'instantané du corpus ({@code ClikeSnapshotsTest}) le
 * fige.
 */
final class KotlinDialect extends CLikeDialect {

    /** L'instance unique du dialecte Kotlin. */
    static final KotlinDialect INSTANCE = new KotlinDialect();

    private static final Set<String> RESERVED_WORDS = Set.of(
            "as", "break", "by", "catch", "class", "companion", "const", "constructor",
            "continue", "data", "do", "else", "enum", "expect", "actual", "external",
            "false", "final", "finally", "for", "fun", "get", "if", "import", "in",
            "init", "inline", "infix", "interface", "internal", "is", "lateinit",
            "object", "open", "out", "override", "package", "private", "protected",
            "public", "reified", "return", "sealed", "set", "super", "suspend",
            "tailrec", "this", "throw", "true", "try", "typealias", "val", "var",
            "vararg", "when", "where", "while", "null", "annotation", "operator",
            "crossinline", "noinline");

    private static final Set<String> TYPE_DECLARATIONS = Set.of(
            "class", "object", "interface");

    private static final Set<String> MEMBER_WORDS = Set.of("fun", "val", "var");

    private static final Set<String> MODIFIERS = Set.of(
            "private", "public", "internal", "protected", "suspend", "lateinit",
            "const", "final", "open", "abstract", "sealed", "data", "companion",
            "enum", "annotation", "operator", "infix", "inline", "external",
            "actual", "expect", "tailrec", "vararg", "out", "reified", "override",
            "crossinline", "noinline");

    private static final List<String> OPERATORS = List.of(
            "===", "!==", ">>>", "<<=", ">>=", "..<", "?:", "?.", "::", "->",
            "==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=",
            "/=", "%=", "..", "<<", ">>", "+", "-", "*", "/", "%", "=", "<",
            ">", "!", "&", "|", "^", "~", "?");

    private KotlinDialect() {
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
    List<String> compositePunctuation() {
        return List.of();
    }

    @Override
    String simplePunctuation() {
        return ";,.:";
    }

    @Override
    boolean operatorsBeforePunctuation() {
        return true;
    }

    @Override
    boolean lineHeadChunks() {
        return true;
    }

    @Override
    boolean closesString(char c, char delimiter) {
        return c == '"';
    }

    @Override
    boolean opensTemplate(CharSequence text, int at, int n, char delimiter) {
        return text.charAt(at) == '$' && at + 1 < n
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
        // la fin de tranche peut n'être que modificateurs ou annotations :
        // la garde du GroovyDialect s'applique ici aussi — jamais de lecture
        // hors bornes sur un membre orphelin
        if (afterHeader >= slice.size()) {
            return to;
        }
        String word = Jetons.mot(slice, afterHeader);
        boolean function = "fun".equals(word) || "init".equals(word);
        members.add(MemberScans.lineMember(slice, start, afterHeader, function,
                MemberScans.nameAfter(slice, afterHeader)));
        return MemberScans.memberEnd(slice, afterHeader, to);
    }
}
