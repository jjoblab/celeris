package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TreeOutline;
import jo.celeris.core.text.Document;

/**
 * Le balayage structurel des flux de B-20 (variantes méthode et
 * paramètre) : les jetons d'une plage aux offsets absolus, les
 * références libres d'une sélection, les champs de la classe porteuse
 * et les appels d'un nom dans le document. Le lexique dit ce qui existe
 * (jamais les chaînes ni les commentaires), l'arbre dit où vivent les
 * déclarations ; ce que les deux ne disent pas n'existe pas — la lecture
 * REFUSE, elle ne devine jamais. La lecture de la méthode porteuse vit
 * dans {@link MethodStructure}, la course de type dans {@link TypeRun},
 * la lecture des types d'expression dans {@link ExpressionTypes}.
 *
 * <p>Le lexeur soude les jetons (le suivant commence à la fin du
 * précédent) : les textes se décollent des espaces de soudure et les
 * offsets pointent le premier caractère réel. Une référence suit un nom
 * de la sélection qui n'est ni un membre pointé (précédé d'un point),
 * ni un nom d'appel (suivi d'une parenthèse), ni un nom construit
 * (précédé de {@code new}). Un récepteur non résolu se lit comme un nom
 * de classe (référence statique), jamais comme une variable.</p>
 */
final class FlowAnalysis {

    /** Un jeton porté à l'offset absolu, avec son texte décollé. */
    record AbsoluteToken(TokenKind kind, int offset, String text) {
    }

    /** Une référence de la sélection : nom, offset, récepteur ou non. */
    record Reference(String name, int offset, boolean receiver) {
    }

    /** Un appel du nom : les offsets de ses parenthèses ouvrante et fermante. */
    record Call(int openParen, int closeParen) {
    }

    /** Les appels d'un nom dans le document et leur verdict. */
    record CallSites(List<Call> calls, boolean ambiguous, boolean methodReference) {
    }

    private FlowAnalysis() {
    }

    /**
     * Les jetons d'une plage, aux offsets absolus — hors commentaires,
     * documentation et chaînes : ce que le lexique ne dit pas n'existe pas.
     *
     * @param doc   le document porteur
     * @param lex   le lexique de la même analyse
     * @param from  début de la plage
     * @param to    fin exclusive de la plage
     * @return les jetons dans l'ordre du source
     */
    static List<AbsoluteToken> tokens(Document doc, LexDocument lex, int from, int to) {
        List<AbsoluteToken> tokens = new ArrayList<>();
        int first = doc.lineForOffset(Math.max(from, 0));
        int last = doc.lineForOffset(Math.max(from, Math.max(to - 1, 0)));
        for (int line = first; line <= last && line < lex.lineCount(); line++) {
            int base = doc.lineStart(line);
            for (Token token : lex.line(line).tokens()) {
                int weld = base + token.start();
                int tokenEnd = base + token.end();
                int real = weld;
                while (real < tokenEnd && Character.isWhitespace(doc.charAt(real))) {
                    real++;
                }
                String text = doc.subText(real, tokenEnd).strip();
                if (!text.isEmpty() && real < to && tokenEnd > from) {
                    tokens.add(new AbsoluteToken(token.kind(), real, text));
                }
            }
        }
        return tokens;
    }

    /**
     * Les références d'une sélection : les noms lus — jamais les membres
     * pointés, jamais les noms d'appel, jamais les constructions.
     *
     * @param tokens les jetons de la sélection
     * @return les références dans l'ordre du source
     */
    static List<Reference> references(List<AbsoluteToken> tokens) {
        List<Reference> refs = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            AbsoluteToken token = tokens.get(i);
            if (!isName(token.kind())) {
                continue;
            }
            String before = neighbor(tokens, i - 1);
            String after = i + 1 < tokens.size() ? tokens.get(i + 1).text() : "";
            if (before.equals(".") || before.equals("new") || after.equals("(")) {
                continue;
            }
            refs.add(new Reference(token.text(), token.offset(), after.equals(".")));
        }
        return refs;
    }

    /**
     * Les champs de la classe porteuse, chacun avec son type lu sur sa
     * déclaration — les champs des types imbriqués n'en font pas partie.
     *
     * @param doc     le document porteur
     * @param analyse l'analyse courante
     * @param owner   l'entrée de la classe porteuse
     * @return les champs nommés et leur type
     */
    static Map<String, String> classFields(Document doc, Analysis analyse, OutlineEntry owner) {
        Map<String, String> fields = new LinkedHashMap<>();
        List<OutlineEntry> entries = TreeOutline.entries(doc, analyse);
        for (OutlineEntry entry : entries) {
            if (isClassField(entry, owner, entries)) {
                String type = fieldType(doc, analyse, entry);
                if (type != null && !type.equals("var")) {
                    fields.putIfAbsent(entry.name(), type);
                }
            }
        }
        return fields;
    }

    /**
     * Les appels d'un nom dans le document, hors la déclaration
     * elle-même. Un récepteur qui n'est pas {@code this} rend le verdict
     * ambigu ; une référence de méthode ({@code ::nom}) l'interdit. Les
     * constructions ne comptent que pour un constructeur ({@code new}).
     *
     * @param doc         le document porteur
     * @param lex         le lexique de la même analyse
     * @param name        le nom appelé
     * @param headerFrom  début de la zone de déclaration à épargner
     * @param headerTo    fin de la zone de déclaration à épargner
     * @param constructor vrai quand le nom déclaré est un constructeur
     * @return les appels et leur verdict
     */
    static CallSites calls(Document doc, LexDocument lex, String name,
            int headerFrom, int headerTo, boolean constructor) {
        CallCollector collector = new CallCollector();
        boolean methodReference = false;
        List<AbsoluteToken> tokens = tokens(doc, lex, 0, doc.length());
        for (int i = 0; i < tokens.size(); i++) {
            AbsoluteToken token = tokens.get(i);
            if (!isName(token.kind()) || !token.text().equals(name)) {
                continue;
            }
            String before = neighbor(tokens, i - 1);
            if (i + 1 < tokens.size() && tokens.get(i + 1).text().equals("(")) {
                classifyCall(tokens, i, withinHeader(token, headerFrom, headerTo),
                        constructor, before, neighbor(tokens, i - 2), collector);
                continue;
            }
            methodReference = methodReference || before.equals("::");
        }
        return new CallSites(List.copyOf(collector.found), collector.ambiguous, methodReference);
    }

    /** Le voisin d'un jeton par son index, chaîne vide aux bornes. */
    private static String neighbor(List<AbsoluteToken> tokens, int i) {
        return i >= 0 && i < tokens.size() ? tokens.get(i).text() : "";
    }

    /** Vrai quand l'occurrence vit dans la zone de déclaration épargnée. */
    private static boolean withinHeader(AbsoluteToken token, int headerFrom, int headerTo) {
        return token.offset() >= headerFrom && token.offset() < headerTo;
    }

    /**
     * Classe l'appel d'une occurrence : l'appel nu et celui de
     * {@code this} se récoltent, le récepteur étranger rend le verdict
     * ambigu, la construction ne compte que pour un constructeur.
     */
    private static void classifyCall(List<AbsoluteToken> tokens, int i, boolean inHeader,
            boolean constructor, String before, String beforeBefore, CallCollector collector) {
        if (inHeader) {
            return;
        }
        Call call = callAt(tokens, i + 1, matchingClose(tokens, i + 1));
        if (call == null) {
            return;
        }
        if (before.equals("new")) {
            if (constructor) {
                collector.found.add(call);
            }
        } else if (before.equals(".")) {
            if (beforeBefore.equals("this")) {
                collector.found.add(call);
            } else {
                collector.ambiguous = true;
            }
        } else if (!before.equals("::")) {
            collector.found.add(call);
        }
    }

    /** Le receuil des appels : les trouvés, le drapeau ambigu. */
    private static final class CallCollector {
        final List<Call> found = new ArrayList<>();
        boolean ambiguous;
    }

    /** L'appel d'une parenthèse ouvrante, ou null si non appariée. */
    private static @Nullable Call callAt(List<AbsoluteToken> tokens, int openIndex,
            int closeIndex) {
        if (closeIndex <= openIndex) {
            return null;
        }
        return new Call(tokens.get(openIndex).offset(), tokens.get(closeIndex).offset());
    }

    /**
     * La parenthèse fermante qui apparie l'ouvrante donnée.
     *
     * @param tokens     les jetons du document ou de la méthode
     * @param openIndex  l'index du jeton ouvrant
     * @return l'index du jeton fermant, ou -1 si non apparié
     */
    static int matchingClose(List<AbsoluteToken> tokens, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < tokens.size(); i++) {
            TokenKind kind = tokens.get(i).kind();
            if (kind == TokenKind.OPEN_BRACKET) {
                depth++;
            } else if (kind == TokenKind.CLOSE_BRACKET) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Vrai quand l'entrée est un champ de la classe porteuse, hors types imbriqués. */
    private static boolean isClassField(OutlineEntry entry, OutlineEntry owner,
            List<OutlineEntry> entries) {
        if (entry.category() != NodeCategory.FIELD
                || entry.startOffset() < owner.startOffset()
                || entry.endOffset() > owner.endOffset()) {
            return false;
        }
        for (OutlineEntry type : entries) {
            if (isNestedType(type, owner)
                    && entry.startOffset() >= type.startOffset()
                    && entry.endOffset() <= type.endOffset()) {
                return false;
            }
        }
        return true;
    }

    /** Vrai quand l'entrée est un type imbriqué de la classe porteuse. */
    private static boolean isNestedType(OutlineEntry entry, OutlineEntry owner) {
        return entry.category() == NodeCategory.TYPE
                && entry.startOffset() >= owner.startOffset()
                && entry.endOffset() <= owner.endOffset()
                && (entry.startOffset() != owner.startOffset()
                        || entry.endOffset() != owner.endOffset());
    }

    /** Le type d'un champ, lu sur sa déclaration. */
    private static @Nullable String fieldType(Document doc, Analysis analyse,
            OutlineEntry entry) {
        List<AbsoluteToken> tokens = tokens(doc, analyse.lex(),
                entry.startOffset(), entry.endOffset());
        for (int i = 0; i < tokens.size(); i++) {
            if (isName(tokens.get(i).kind())
                    && tokens.get(i).text().equals(entry.name())
                    && TypeRun.declaresAfter(tokens, i)) {
                return TypeRun.beforeName(tokens, i);
            }
        }
        return null;
    }

    /** Vrai quand la sorte porte un nom : identifiant, type ou constante. */
    static boolean isName(TokenKind kind) {
        return kind == TokenKind.IDENTIFIER || kind == TokenKind.TYPE
                || kind == TokenKind.CONSTANT;
    }
}
