package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.intelligence.Extraction.ExtractionPlan;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.TreeOutline;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * Les plans d'extraction des variantes de flux de B-20 — la méthode et
 * le paramètre — bâtis sur le balayage structurel ({@link FlowAnalysis}),
 * la lecture de la porteuse ({@link MethodStructure}), la course de type
 * ({@link TypeRun}) et la lecture des types ({@link ExpressionTypes}).
 * Les refus se signalent par un plan nul, jamais par un plan faux ; les
 * éditions descendent — les offsets originaux restent valides au fil de
 * l'application séquentielle.
 */
final class FlowExtractions {

    private FlowExtractions() {
    }

    /**
     * Le plan d'extraction en méthode : la sélection se remplace par
     * l'appel et la méthode naît après la méthode porteuse. Les
     * références libres déclarées dans la porteuse deviennent les
     * paramètres de la nouvelle méthode, dans l'ordre de leur première
     * lecture ; les champs de la classe porteuse passent sans paramètre.
     *
     * @param doc       le document, jamais muté
     * @param analyse   l'analyse courante du document
     * @param enclosing la méthode porteuse
     * @param start     début de la sélection
     * @param end       fin de la sélection (exclusive)
     * @param name      le nom de la méthode
     * @return le plan, ou null sur une référence nue non résolue ou sans
     *         type de retour lisible
     */
    static @Nullable ExtractionPlan method(Document doc, Analysis analyse,
            OutlineEntry enclosing, int start, int end, String name) {
        List<FlowAnalysis.AbsoluteToken> methodTokens = FlowAnalysis.tokens(doc, analyse.lex(),
                enclosing.startOffset(), enclosing.endOffset());
        Map<String, String> declared = MethodStructure.declarations(methodTokens);
        OutlineEntry owner = Extraction.enclosing(doc, analyse, start, end, NodeCategory.TYPE);
        Map<String, String> fields = owner == null ? Map.of()
                : FlowAnalysis.classFields(doc, analyse, owner);
        String expression = doc.subText(start, end).strip();
        List<FlowAnalysis.AbsoluteToken> tokens = FlowAnalysis.tokens(doc, analyse.lex(), start, end);
        List<FlowAnalysis.Reference> refs = FlowAnalysis.references(tokens);
        Map<String, String> resolved = new HashMap<>();
        Map<String, String> parameters = freeParameters(refs, declared, fields, resolved);
        if (parameters == null) {
            return null;
        }
        String returnType = ExpressionTypes.read(expression, tokens, refs, resolved, fields);
        if (returnType == null) {
            return null;
        }
        String indent = indentationAt(doc, enclosing.startOffset());
        String declaration = "private " + returnType + " " + name + "("
                + parameterList(parameters) + ") {\n" + indent + "    return "
                + expression + ";\n" + indent + "}";
        List<TextEdit> edits = new ArrayList<>();
        edits.add(new TextEdit(start, doc.subText(start, end),
                name + "(" + String.join(", ", parameters.keySet()) + ")"));
        edits.add(new TextEdit(enclosing.endOffset(), "", "\n\n" + indent + declaration));
        return new ExtractionPlan(descending(edits));
    }

    /**
     * Le plan d'extraction en paramètre : la signature de la porteuse
     * gagne le paramètre typé, la sélection se remplace par son nom et
     * chaque appel du document passe l'expression en argument.
     *
     * @param doc       le document, jamais muté
     * @param analyse   l'analyse courante du document
     * @param enclosing la méthode porteuse
     * @param start     début de la sélection
     * @param end       fin de la sélection (exclusive)
     * @param name      le nom du paramètre
     * @return le plan, ou null sur une expression invivable aux appels,
     *         un appel ambigu ou une surcharge homonyme
     */
    static @Nullable ExtractionPlan parameter(Document doc, Analysis analyse,
            OutlineEntry enclosing, int start, int end, String name) {
        if (overloaded(doc, analyse, enclosing)) {
            return null;
        }
        List<FlowAnalysis.AbsoluteToken> methodTokens = FlowAnalysis.tokens(doc, analyse.lex(),
                enclosing.startOffset(), enclosing.endOffset());
        int body = MethodStructure.bodyOffset(methodTokens);
        int[] parens = MethodStructure.signatureParens(methodTokens);
        if (body < 0 || parens == null || start < body + 1) {
            return null;
        }
        Map<String, String> declared = MethodStructure.declarations(methodTokens);
        OutlineEntry owner = Extraction.enclosing(doc, analyse, start, end, NodeCategory.TYPE);
        Map<String, String> fields = owner == null ? Map.of()
                : FlowAnalysis.classFields(doc, analyse, owner);
        String expression = doc.subText(start, end).strip();
        List<FlowAnalysis.AbsoluteToken> tokens = FlowAnalysis.tokens(doc, analyse.lex(), start, end);
        List<FlowAnalysis.Reference> refs = FlowAnalysis.references(tokens);
        Map<String, String> resolved = fieldReferences(refs, declared, fields);
        if (resolved == null) {
            return null;
        }
        String type = ExpressionTypes.read(expression, tokens, refs, resolved, fields);
        if (type == null) {
            return null;
        }
        boolean constructor = owner != null && owner.name().equals(enclosing.name());
        FlowAnalysis.CallSites sites = FlowAnalysis.calls(doc, analyse.lex(), enclosing.name(),
                enclosing.startOffset(), body + 1, constructor);
        if (sites.ambiguous() || sites.methodReference()) {
            return null;
        }
        return new ExtractionPlan(parameterEdits(doc, start, end, name,
                type, expression, parens, sites));
    }

    /**
     * Les paramètres de la nouvelle méthode : les références libres
     * déclarées dans la porteuse, dans l'ordre de leur première lecture.
     * Rend null sur une référence nue non résolue — jamais un plan faux.
     */
    private static @Nullable Map<String, String> freeParameters(List<FlowAnalysis.Reference> refs,
            Map<String, String> declared, Map<String, String> fields, Map<String, String> resolved) {
        Map<String, String> parameters = new LinkedHashMap<>();
        for (FlowAnalysis.Reference ref : refs) {
            if (resolved.containsKey(ref.name())) {
                continue;
            }
            String local = declared.get(ref.name());
            if (local != null) {
                resolved.put(ref.name(), local);
                parameters.put(ref.name(), local);
                continue;
            }
            String field = fields.get(ref.name());
            if (field != null) {
                resolved.put(ref.name(), field);
                continue;
            }
            if (!ref.receiver()) {
                return null;
            }
        }
        return parameters;
    }

    /**
     * Les références lues aux appels : les champs seuls — ce qui est
     * déclaré dans la porteuse ou inconnu refuse le plan.
     */
    private static @Nullable Map<String, String> fieldReferences(List<FlowAnalysis.Reference> refs,
            Map<String, String> declared, Map<String, String> fields) {
        Map<String, String> resolved = new HashMap<>();
        for (FlowAnalysis.Reference ref : refs) {
            String field = fields.get(ref.name());
            if (field != null) {
                resolved.put(ref.name(), field);
                continue;
            }
            if (declared.containsKey(ref.name()) || !ref.receiver()) {
                return null;
            }
        }
        return resolved;
    }

    /** Les éditions du paramètre : signature, sélection, appels — en descendant. */
    private static List<TextEdit> parameterEdits(Document doc, int start, int end,
            String name, String type, String expression, int[] parens,
            FlowAnalysis.CallSites sites) {
        List<TextEdit> edits = new ArrayList<>();
        boolean emptySignature = doc.subText(parens[0] + 1, parens[1]).isBlank();
        edits.add(new TextEdit(parens[1], "",
                (emptySignature ? "" : ", ") + type + " " + name));
        edits.add(new TextEdit(start, doc.subText(start, end), name));
        for (FlowAnalysis.Call call : sites.calls()) {
            if (call.openParen() < end && call.closeParen() > start) {
                continue;
            }
            boolean empty = doc.subText(call.openParen() + 1, call.closeParen()).isBlank();
            edits.add(new TextEdit(call.closeParen(), "",
                    (empty ? "" : ", ") + expression));
        }
        return descending(edits);
    }

    /** La signature nouvelle : « type nom », virgules comprises. */
    private static String parameterList(Map<String, String> parameters) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(parameter.getValue()).append(' ').append(parameter.getKey());
        }
        return text.toString();
    }

    /** Vrai quand une autre méthode du document porte le même nom. */
    private static boolean overloaded(Document doc, Analysis analyse, OutlineEntry enclosing) {
        for (OutlineEntry entry : TreeOutline.entries(doc, analyse)) {
            if (entry.category() == NodeCategory.FUNCTION
                    && entry.name().equals(enclosing.name())
                    && entry.startOffset() != enclosing.startOffset()) {
                return true;
            }
        }
        return false;
    }

    /** Les éditions en ordre descendant d'application ; à offset égal, le remplacement précède. */
    private static List<TextEdit> descending(List<TextEdit> edits) {
        edits.sort((a, b) -> {
            int cmp = Integer.compare(b.start(), a.start());
            if (cmp != 0) {
                return cmp;
            }
            return a.removed().isEmpty() ? 1 : -1;
        });
        return edits;
    }

    /** Le retrait de la ligne d'un offset. */
    private static String indentationAt(Document doc, int offset) {
        int lineStart = doc.lineStart(doc.lineForOffset(offset));
        return Extraction.indentationOf(doc, lineStart);
    }
}
