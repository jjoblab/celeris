package jo.celeris.core.intelligence;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * L'alignement en ligne (B-21) : la déclaration désignée par la
 * sélection disparaît — ses usages reçoivent l'initialisateur, la
 * déclaration se supprime si elle est locale ou privée (le champ
 * visible hors de la classe reste déclaré). Le refus se signale par un
 * plan nul, jamais par un plan faux : l'initialisateur doit être un
 * littéral seul, la déclaration ne doit jamais être réaffectée, aucun
 * nom homonyme ne doit être déclaré ou paramétré dans la portée (une
 * ombre changerait le sens), et le nom pointé par un récepteur
 * étranger refuse le plan. La sûreté et les remplacements vivent dans
 * {@link InlineSafety}, le chemin des champs dans {@link InlineFields}.
 * Les éditions descendent — les offsets originaux restent valides au
 * fil de l'application séquentielle.
 */
public final class Inline {

    private Inline() {
    }

    /**
     * Le plan d'alignement : les éditions du geste, en ordre descendant
     * d'application.
     *
     * @param edits les éditions : remplacement des usages puis
     *              suppression de la déclaration
     */
    public record InlinePlan(List<TextEdit> edits) {

        /** Constructeur canonique : liste figée, jamais vide. */
        public InlinePlan {
            Objects.requireNonNull(edits, "edits");
            if (edits.isEmpty()) {
                throw new IllegalArgumentException("edits must not be empty");
            }
            edits = List.copyOf(edits);
        }
    }

    /**
     * Aligne en ligne la déclaration désignée par la sélection : ses
     * usages reçoivent le littéral de son initialisateur, la
     * déclaration se supprime si elle est locale ou privée.
     *
     * @param doc     le document, jamais muté
     * @param analyse l'analyse courante du document
     * @param start   début de la sélection, dans la déclaration
     * @param end     fin de la sélection (exclusive), dans la déclaration
     * @return le plan, ou null hors déclaration, sans initialisateur
     *         littéral, sur une réaffectation, une ombre homonyme ou un
     *         récepteur étranger
     * @throws IllegalArgumentException si la sélection est inversée ou
     *         hors bornes
     */
    public static @Nullable InlinePlan declaration(Document doc, Analysis analyse,
            int start, int end) {
        if (start < 0 || end > doc.length() || start > end) {
            throw new IllegalArgumentException(
                    "selection [" + start + ", " + end + ") out of (0, " + doc.length() + ")");
        }
        OutlineEntry function = Extraction.enclosing(doc, analyse, start, end,
                NodeCategory.FUNCTION);
        if (function != null) {
            MethodDeclarations.Declaration local = localWithin(doc, analyse, function,
                    start, end);
            if (local != null) {
                return localPlan(doc, analyse, function, local);
            }
        }
        return InlineFields.plan(doc, analyse, start, end);
    }

    /** La déclaration locale de la porteuse qui contient la sélection, ou null. */
    private static MethodDeclarations.@Nullable Declaration localWithin(Document doc,
            Analysis analyse, OutlineEntry function, int start, int end) {
        List<FlowAnalysis.AbsoluteToken> tokens = FlowAnalysis.tokens(doc, analyse.lex(),
                function.startOffset(), function.endOffset());
        for (MethodDeclarations.Declaration declaration
                : MethodStructure.declarations(tokens).values()) {
            if (containsSelection(tokens, declaration, start, end)) {
                return declaration;
            }
        }
        return null;
    }

    /** Vrai quand la sélection vit dans l'instruction de la déclaration. */
    private static boolean containsSelection(List<FlowAnalysis.AbsoluteToken> tokens,
            MethodDeclarations.Declaration declaration, int start, int end) {
        if (declaration.statementStart() < 0 || declaration.statementEnd() < 0) {
            return false;
        }
        int from = tokens.get(declaration.statementStart()).offset();
        int to = tokens.get(declaration.statementEnd()).offset()
                + tokens.get(declaration.statementEnd()).text().length();
        return start >= from && end <= to;
    }

    /** Le plan d'alignement d'une déclaration locale. */
    private static @Nullable InlinePlan localPlan(Document doc, Analysis analyse,
            OutlineEntry function, MethodDeclarations.Declaration declaration) {
        List<FlowAnalysis.AbsoluteToken> tokens = FlowAnalysis.tokens(doc, analyse.lex(),
                function.startOffset(), function.endOffset());
        String literal = InlineSafety.literalOf(tokens, declaration);
        if (literal == null || InlineSafety.reassigned(tokens, declaration.name(),
                tokens.get(declaration.statementStart()).offset(),
                tokens.get(declaration.statementEnd()).offset() + 1)
                || InlineSafety.shadowed(tokens, declaration)) {
            return null;
        }
        int after = tokens.get(declaration.statementEnd()).offset() + 1;
        List<TextEdit> edits = InlineSafety.replacements(doc, tokens, declaration,
                literal, false, after, 0, 0);
        if (edits == null) {
            return null;
        }
        removeStatement(doc, tokens, declaration.statementStart(),
                declaration.statementEnd(), edits);
        return new InlinePlan(descending(edits));
    }

    /**
     * La suppression de la déclaration : la ligne entière quand elle la
     * porte seule, l'instruction seule sinon.
     */
    static void removeStatement(Document doc, List<FlowAnalysis.AbsoluteToken> tokens,
            int from, int to, List<TextEdit> edits) {
        int start = tokens.get(from).offset();
        int end = tokens.get(to).offset() + tokens.get(to).text().length();
        int lineStart = doc.lineStart(doc.lineForOffset(start));
        int lineEnd = lineEnd(doc, end);
        if (doc.subText(lineStart, start).isBlank() && doc.subText(end, lineEnd).isBlank()) {
            int range = lineEnd < doc.length() ? lineEnd + 1 : lineEnd;
            edits.add(new TextEdit(lineStart, doc.subText(lineStart, range), ""));
            return;
        }
        edits.add(new TextEdit(start, doc.subText(start, end), ""));
    }

    /** La fin de la ligne qui contient l'offset (fin de ligne exclue). */
    private static int lineEnd(Document doc, int offset) {
        int end = offset;
        while (end < doc.length() && doc.charAt(end) != '\n') {
            end++;
        }
        return end;
    }

    /** Les éditions en ordre descendant d'application ; à offset égal, le remplacement précède. */
    static List<TextEdit> descending(List<TextEdit> edits) {
        edits.sort((a, b) -> {
            int cmp = Integer.compare(b.start(), a.start());
            if (cmp != 0) {
                return cmp;
            }
            return a.removed().isEmpty() ? 1 : -1;
        });
        return edits;
    }
}
