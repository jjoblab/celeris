package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * Le changement de signature (B-22) : les paramètres de la méthode
 * désignée se réordonnent — la signature et chaque appel du document
 * suivent le nouvel ordre, en une transaction annulable. Le refus se
 * signale par un plan nul, jamais par un plan faux : la surcharge
 * homonyme refuse (l'appel ne se distingue pas par le nom seul), la
 * référence de méthode refuse, l'appel d'arité différente refuse (le
 * réordonnancement n'aurait pas de sens), les varargs refusent (arité
 * variable), et les chevrons non appariés dans une liste d'arguments
 * refusent (comparaison ou type explicite : l'ambiguïté ne se tranche
 * pas ici). Les éditions descendent — les offsets originaux restent
 * valides au fil de l'application séquentielle.
 */
public final class SignatureChange {

    private SignatureChange() {
    }

    /**
     * Le plan de changement : les éditions du geste, en ordre descendant
     * d'application.
     *
     * @param edits les éditions : signature puis appels
     */
    public record SignaturePlan(List<TextEdit> edits) {

        /** Constructeur canonique : liste figée, jamais vide. */
        public SignaturePlan {
            Objects.requireNonNull(edits, "edits");
            if (edits.isEmpty()) {
                throw new IllegalArgumentException("edits must not be empty");
            }
            edits = List.copyOf(edits);
        }
    }

    /**
     * Réordonne les paramètres de la méthode désignée par la sélection :
     * la signature et chaque appel du document suivent le nouvel ordre.
     *
     * @param doc     le document, jamais muté
     * @param analyse l'analyse courante du document
     * @param start   début de la sélection, dans la méthode désignée
     * @param end     fin de la sélection (exclusive)
     * @param order   le nouvel ordre : order[i] est l'index actuel du
     *                paramètre qui prend la i-ème place
     * @return le plan, ou null sur une surcharge homonyme, une référence
     *         de méthode, un appel d'arité différente, des varargs ou
     *         des chevrons non appariés
     * @throws IllegalArgumentException si la sélection est inversée ou
     *         hors bornes, ou si l'ordre n'est pas une permutation des
     *         paramètres
     */
    public static @Nullable SignaturePlan reorder(Document doc, Analysis analyse,
            int start, int end, int[] order) {
        Objects.requireNonNull(order, "order");
        if (start < 0 || end > doc.length() || start > end) {
            throw new IllegalArgumentException(
                    "selection [" + start + ", " + end + ") out of (0, " + doc.length() + ")");
        }
        Designation designation = designate(doc, analyse, start, end);
        if (designation == null) {
            return null;
        }
        if (!permutation(order, designation.parameters().size())) {
            throw new IllegalArgumentException(
                    "order is not a permutation of " + designation.parameters().size()
                            + " parameters");
        }
        if (varargs(doc, designation.parameters())) {
            return null;
        }
        FlowAnalysis.CallSites sites = FlowAnalysis.calls(doc, analyse.lex(),
                designation.enclosing().name(), designation.enclosing().startOffset(),
                designation.body() + 1, designation.constructor());
        if (sites.ambiguous() || sites.methodReference()) {
            return null;
        }
        List<TextEdit> edits = new ArrayList<>();
        edits.add(new TextEdit(designation.openParen() + 1,
                doc.subText(designation.openParen() + 1, designation.closeParen()),
                reordered(doc, designation.parameters(), order)));
        if (!appelsReordonnes(doc, analyse, sites, designation.parameters().size(), order, edits)) {
            return null;
        }
        return new SignaturePlan(Inline.descending(edits));
    }

    /** La méthode désignée : l'entrée, ses parenthèses, son corps, ses paramètres. */
    private record Designation(OutlineEntry enclosing, int openParen, int closeParen,
            int body, boolean constructor, List<int[]> parameters) {
    }

    /** La désignation lue, ou null : méthode introuvable, surchargée, illisible ou sans paramètre. */
    private static @Nullable Designation designate(Document doc, Analysis analyse,
            int start, int end) {
        OutlineEntry enclosing = Extraction.enclosing(doc, analyse, start, end,
                NodeCategory.FUNCTION);
        if (enclosing == null || FlowAnalysis.overloaded(doc, analyse, enclosing)) {
            return null;
        }
        List<FlowAnalysis.AbsoluteToken> tokens = FlowAnalysis.tokens(doc, analyse.lex(),
                enclosing.startOffset(), enclosing.endOffset());
        int[] parens = MethodStructure.signatureParens(tokens);
        int body = MethodStructure.bodyOffset(tokens);
        if (parens == null || body < 0) {
            return null;
        }
        List<int[]> parameters = segments(tokens, parens[0], parens[1]);
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        return new Designation(enclosing, parens[0], parens[1], body,
                isConstructor(doc, analyse, enclosing), parameters);
    }

    /** Vrai quand le nom déclaré est un constructeur (classe porteuse homonyme). */
    static boolean isConstructor(Document doc, Analysis analyse, OutlineEntry enclosing) {
        OutlineEntry owner = null;
        for (OutlineEntry entry : jo.celeris.core.syntax.TreeOutline.entries(doc, analyse)) {
            if (entry.category() == NodeCategory.TYPE
                    && entry.startOffset() <= enclosing.startOffset()
                    && enclosing.endOffset() <= entry.endOffset()
                    && (owner == null || entry.startOffset() >= owner.startOffset())) {
                owner = entry;
            }
        }
        return owner != null && owner.name().equals(enclosing.name());
    }

    /**
     * Les appels réordonnés : chaque appel du nom suit le nouvel ordre —
     * l'appel d'arité différente refuse le plan.
     */
    private static boolean appelsReordonnes(Document doc, Analysis analyse,
            FlowAnalysis.CallSites sites, int expected, int[] order, List<TextEdit> edits) {
        for (FlowAnalysis.Call call : sites.calls()) {
            List<FlowAnalysis.AbsoluteToken> tokens = FlowAnalysis.tokens(doc, analyse.lex(),
                    call.openParen(), call.closeParen() + 1);
            List<int[]> arguments = segments(tokens,
                    tokens.get(0).offset(), tokens.get(tokens.size() - 1).offset());
            if (arguments == null || arguments.size() != expected) {
                return false;
            }
            edits.add(new TextEdit(call.openParen() + 1,
                    doc.subText(call.openParen() + 1, call.closeParen()),
                    reordered(doc, arguments, order)));
        }
        return true;
    }

    /**
     * Les segments d'une liste entre parenthèses : séparés par des
     * virgules de fond (crochets et chevrons appariés compris — un
     * chevron non apparié rend null, l'ambiguïté ne se tranche pas).
     *
     * @param tokens les jetons de la méthode ou de l'appel
     * @param from   l'offset de la parenthèse ouvrante
     * @param to     l'offset de la parenthèse fermante
     * @return les paires { début, fin } d'offsets des segments, ou null
     */
    private static @Nullable List<int[]> segments(List<FlowAnalysis.AbsoluteToken> tokens,
            int from, int to) {
        List<int[]> segments = new ArrayList<>();
        int start = -1;
        int depth = 0;
        int angles = 0;
        for (FlowAnalysis.AbsoluteToken token : tokens) {
            if (token.offset() <= from) {
                start = token.offset() + token.text().length();
                continue;
            }
            if (token.offset() >= to) {
                break;
            }
            if (token.kind() == TokenKind.OPEN_BRACKET) {
                depth++;
            } else if (token.kind() == TokenKind.CLOSE_BRACKET) {
                depth--;
            }
            angles = suivisDeChevrons(token.text(), angles);
            if (angles < 0) {
                return null;
            }
            if (token.text().equals(",") && depth == 0 && angles == 0) {
                segments.add(new int[]{start, token.offset()});
                start = token.offset() + 1;
            }
        }
        if (start < 0) {
            return null;
        }
        if (to > start) {
            segments.add(new int[]{start, to});
        }
        return angles == 0 ? segments : null;
    }

    /** Les chevrons se suivent ; un fermant dépareillé décale sous zéro. */
    private static int suivisDeChevrons(String text, int angles) {
        if (text.equals("<")) {
            return angles + 1;
        }
        return text.equals(">") ? angles - 1 : angles;
    }

    /** Le texte réordonné d'une liste de segments. */
    private static String reordered(Document doc,
            List<int[]> segments, int[] order) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < order.length; i++) {
            if (i > 0) {
                text.append(", ");
            }
            int[] segment = segments.get(order[i]);
            text.append(doc.subText(segment[0], segment[1]).strip());
        }
        return text.toString();
    }

    /** Vrai quand l'ordre est une permutation des count premiers entiers. */
    private static boolean permutation(int[] order, int count) {
        if (order.length != count) {
            return false;
        }
        boolean[] vus = new boolean[count];
        for (int value : order) {
            if (value < 0 || value >= count || vus[value]) {
                return false;
            }
            vus[value] = true;
        }
        return true;
    }

    /** Vrai quand un paramètre porte des varargs (l'arité varie — refus). */
    private static boolean varargs(Document doc, List<int[]> segments) {
        for (int[] segment : segments) {
            if (doc.subText(segment[0], segment[1]).contains("...")) {
                return true;
            }
        }
        return false;
    }
}
