package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TreeOutline;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * Le déplacement sûr (B-23) : la méthode désignée par la sélection
 * déménage dans un autre type du même document — annotations comprises —
 * si, et seulement si, rien ne pend. Les usages du nom hors la
 * déclaration refusent (ils perdraient leur cible), les lectures du corps
 * des champs de la classe d'origine refusent (le corps ne saurait les
 * suivre), les appels du corps aux méthodes sœurs refusent, la méthode
 * homonyme du type cible refuse (le conflit précède le déménagement), le
 * constructeur refuse (il appartient à sa classe), la surcharge homonyme
 * refuse. Le refus se signale par un plan nul, jamais par un plan faux.
 * Les éditions descendent — les offsets originaux restent valides au fil
 * de l'application séquentielle.
 */
public final class SafeMove {

    /** L'indentation d'un membre : quatre espaces par niveau. */
    private static final String MEMBER_INDENT = "    ";

    private SafeMove() {
    }

    /**
     * Le plan de déménagement : les éditions du geste, en ordre
     * descendant d'application.
     *
     * @param edits les éditions : insertion dans la cible puis effacement
     *              à l'origine
     */
    public record MovePlan(List<TextEdit> edits) {

        /** Constructeur canonique : liste figée, jamais vide. */
        public MovePlan {
            Objects.requireNonNull(edits, "edits");
            if (edits.isEmpty()) {
                throw new IllegalArgumentException("edits must not be empty");
            }
            edits = List.copyOf(edits);
        }
    }

    /**
     * Déplace la méthode désignée par la sélection dans le type du nom
     * donné — un autre type du même document.
     *
     * @param doc        le document, jamais muté
     * @param analyse    l'analyse courante du document
     * @param start      début de la sélection, dans la méthode
     * @param end        fin de la sélection (exclusive), dans la méthode
     * @param targetName le nom du type cible
     * @return le plan, ou null hors méthode, sur un constructeur, une
     *         surcharge homonyme, une cible inconnue, ambiguë ou
     *         identique, un conflit homonyme dans la cible, un usage du
     *         nom, une lecture de champ d'origine ou un appel sœur
     * @throws IllegalArgumentException si la sélection est inversée ou
     *         hors bornes, ou si le nom cible est vide
     */
    public static @Nullable MovePlan move(Document doc, Analysis analyse,
            int start, int end, String targetName) {
        Objects.requireNonNull(targetName, "targetName");
        if (targetName.isBlank()) {
            throw new IllegalArgumentException("targetName must not be blank");
        }
        requireSelection(doc, start, end);
        OutlineEntry method = Extraction.enclosing(doc, analyse, start, end,
                NodeCategory.FUNCTION);
        OutlineEntry source = Extraction.enclosing(doc, analyse, start, end,
                NodeCategory.TYPE);
        OutlineEntry target = typeNamed(doc, analyse, targetName);
        if (method == null || source == null || target == null
                || carriesSourceName(doc, analyse, method)
                || target.startOffset() == source.startOffset()
                || contains(target, method)) {
            return null;
        }
        List<FlowAnalysis.AbsoluteToken> body = FlowAnalysis.tokens(doc, analyse.lex(),
                method.startOffset(), method.endOffset());
        if (dangles(doc, analyse, source, target, method, body)) {
            return null;
        }
        TextEdit insertion = insertion(doc, analyse, target, method);
        if (insertion == null) {
            return null;
        }
        List<TextEdit> edits = new ArrayList<>();
        edits.add(insertion);
        edits.add(SafeDelete.removal(doc, method));
        return new MovePlan(Inline.descending(edits));
    }

    /** La sélection inversée ou hors bornes refuse d'exception. */
    private static void requireSelection(Document doc, int start, int end) {
        if (start < 0 || end > doc.length() || start > end) {
            throw new IllegalArgumentException(
                    "selection [" + start + ", " + end + ") out of (0, " + doc.length() + ")");
        }
    }

    /** Vrai quand la méthode porte un nom qui l'attache à sa classe : constructeur ou surcharge homonyme. */
    private static boolean carriesSourceName(Document doc, Analysis analyse, OutlineEntry method) {
        return FlowAnalysis.overloaded(doc, analyse, method)
                || SignatureChange.isConstructor(doc, analyse, method);
    }

    /**
     * Vrai quand le déménagement laisse pendre : conflit homonyme dans la
     * cible, usage du nom hors la déclaration, lecture d'un champ
     * d'origine, appel d'une méthode sœur.
     */
    private static boolean dangles(Document doc, Analysis analyse, OutlineEntry source,
            OutlineEntry target, OutlineEntry method,
            List<FlowAnalysis.AbsoluteToken> body) {
        return conflictIn(doc, analyse, target, method.name())
                || usedOutside(doc, analyse, method)
                || readsSourceField(doc, analyse, source, body)
                || callsSibling(doc, analyse, source, method, body);
    }

    /** Le type du nom donné, ou null — le type introuvable ou dédoublé refuse. */
    private static @Nullable OutlineEntry typeNamed(Document doc, Analysis analyse,
            String name) {
        OutlineEntry found = null;
        for (OutlineEntry entry : TreeOutline.entries(doc, analyse)) {
            if (entry.category() == NodeCategory.TYPE && entry.name().equals(name)) {
                if (found != null) {
                    return null;
                }
                found = entry;
            }
        }
        return found;
    }

    /** Vrai quand le type vit dans la méthode (une classe locale ne peut pas l'accueillir). */
    private static boolean contains(OutlineEntry target, OutlineEntry method) {
        return target.startOffset() >= method.startOffset()
                && target.endOffset() <= method.endOffset();
    }

    /** Vrai quand la cible déclare déjà une méthode du nom — le conflit précède. */
    private static boolean conflictIn(Document doc, Analysis analyse,
            OutlineEntry target, String name) {
        for (OutlineEntry entry : TreeOutline.entries(doc, analyse)) {
            if (entry.category() == NodeCategory.FUNCTION && entry.name().equals(name)
                    && entry.startOffset() >= target.startOffset()
                    && entry.endOffset() <= target.endOffset()) {
                return true;
            }
        }
        return false;
    }

    /** Vrai quand le nom de la méthode s'utilise hors sa déclaration — l'usage perdrait sa cible. */
    private static boolean usedOutside(Document doc, Analysis analyse, OutlineEntry method) {
        FlowAnalysis.CallSites sites = FlowAnalysis.calls(doc, analyse.lex(),
                method.name(), method.startOffset(), method.endOffset(), false);
        return !sites.calls().isEmpty() || sites.ambiguous() || sites.methodReference()
                || !sites.suspects().isEmpty();
    }

    /**
     * Vrai quand le corps lit un champ de la classe d'origine — la
     * lecture nue, la lecture pointée ({@code this.champ} ou récepteur
     * étranger) ; l'appel homonyme n'est pas une lecture.
     */
    private static boolean readsSourceField(Document doc, Analysis analyse,
            OutlineEntry source, List<FlowAnalysis.AbsoluteToken> body) {
        Set<String> fields = FlowAnalysis.classFields(doc, analyse, source).keySet();
        for (int i = 0; i < body.size(); i++) {
            FlowAnalysis.AbsoluteToken token = body.get(i);
            if (!FlowAnalysis.isName(token.kind()) || !fields.contains(token.text())) {
                continue;
            }
            String before = textOf(body, i - 1);
            String after = textOf(body, i + 1);
            if (!before.equals("new") && !after.equals("(")) {
                return true;
            }
        }
        return false;
    }

    /** Vrai quand le corps appelle une méthode sœur de la classe d'origine — l'appel ne peut pas suivre. */
    private static boolean callsSibling(Document doc, Analysis analyse,
            OutlineEntry source, OutlineEntry method,
            List<FlowAnalysis.AbsoluteToken> body) {
        Set<String> siblings = new HashSet<>();
        for (OutlineEntry entry : TreeOutline.entries(doc, analyse)) {
            if (entry.category() == NodeCategory.FUNCTION
                    && !entry.name().equals(method.name())
                    && entry.startOffset() >= source.startOffset()
                    && entry.endOffset() <= source.endOffset()) {
                siblings.add(entry.name());
            }
        }
        for (int i = 0; i < body.size(); i++) {
            FlowAnalysis.AbsoluteToken token = body.get(i);
            if (FlowAnalysis.isName(token.kind()) && siblings.contains(token.text())
                    && textOf(body, i + 1).equals("(")
                    && !textOf(body, i - 1).equals("new")) {
                return true;
            }
        }
        return false;
    }

    /**
     * L'insertion de la méthode dans la cible : avant son accolade
     * fermante, indentée de ses membres — ou null si la fermante ne se
     * laisse pas lire (type d'une ligne, fermante partagée).
     */
    private static @Nullable TextEdit insertion(Document doc, Analysis analyse,
            OutlineEntry target, OutlineEntry method) {
        int closeOffset = -1;
        for (FlowAnalysis.AbsoluteToken token : FlowAnalysis.tokens(doc, analyse.lex(),
                target.startOffset(), target.endOffset())) {
            if (token.kind() == TokenKind.CLOSE_BRACKET) {
                closeOffset = token.offset();
            }
        }
        if (closeOffset < 0) {
            return null;
        }
        int closeLine = doc.lineForOffset(closeOffset);
        int insertOffset = doc.lineStart(closeLine);
        if (!doc.subText(insertOffset, closeOffset).isBlank()) {
            return null;
        }
        String memberIndent = Extraction.indentationOf(doc, insertOffset) + MEMBER_INDENT;
        int from = annotatedStart(doc, method);
        int delta = memberIndent.length()
                - Extraction.indentationOf(doc, from).length();
        String moved = doc.subText(from, method.endOffset()).stripTrailing();
        return new TextEdit(insertOffset, "", reindented(moved, delta) + "\n");
    }

    /**
     * Le début de la méthode remonté à ses annotations contiguës de même
     * indentation — elles déménagent avec elle.
     */
    private static int annotatedStart(Document doc, OutlineEntry method) {
        int line = method.startLine();
        String indent = Extraction.indentationOf(doc, doc.lineStart(line));
        while (line > 0 && doc.lineText(line - 1).strip().startsWith("@")
                && Extraction.indentationOf(doc, doc.lineStart(line - 1)).equals(indent)) {
            line--;
        }
        return doc.lineStart(line);
    }

    /** Le texte réindenté : le delta s'applique à chaque ligne portante, les vides restent vides. */
    private static String reindented(String text, int delta) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (String line : text.split("\n", -1)) {
            if (!out.isEmpty()) {
                out.append('\n');
            }
            if (line.isBlank()) {
                continue;
            }
            if (delta >= 0) {
                out.append(" ".repeat(delta)).append(line);
            } else {
                out.append(line.substring(Math.min(-delta, leading(line))));
            }
        }
        return out.toString();
    }

    /** Les espaces de tête d'une ligne. */
    private static int leading(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    /** Le texte du voisin d'un jeton par son index, chaîne vide aux bornes. */
    private static String textOf(List<FlowAnalysis.AbsoluteToken> tokens, int i) {
        return i >= 0 && i < tokens.size() ? tokens.get(i).text() : "";
    }
}
