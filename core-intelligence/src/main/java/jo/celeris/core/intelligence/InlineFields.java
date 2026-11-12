package jo.celeris.core.intelligence;

import java.util.List;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.structure.TreeOutline;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * Le chemin des champs de l'alignement (B-21) : le champ désigné par la
 * sélection s'aligne sur toute sa classe — ses usages nus et ses
 * membres de {@code this} reçoivent le littéral de son initialisateur,
 * sa déclaration se supprime s'il est privé, il reste déclaré s'il est
 * visible hors de la classe. Le champ ombré par un nom homonyme
 * (paramètre, local ou autre champ de la classe) refuse le plan, le
 * récepteur étranger refuse aussi — jamais un plan faux.
 */
final class InlineFields {

    private InlineFields() {
    }

    /** Le plan d'alignement du champ désigné par la sélection, ou null hors champ. */
    static Inline.@Nullable InlinePlan plan(Document doc, Analysis analyse,
            int start, int end) {
        OutlineEntry owner = Extraction.enclosing(doc, analyse, start, end, NodeCategory.TYPE);
        if (owner == null) {
            return null;
        }
        for (OutlineEntry entry : TreeOutline.entries(doc, analyse)) {
            if (isDesignated(entry, owner, start, end)) {
                return plan(doc, analyse, owner, entry);
            }
        }
        return null;
    }

    /** Vrai quand l'entrée est le champ de la classe qui porte la sélection. */
    private static boolean isDesignated(OutlineEntry entry, OutlineEntry owner,
            int start, int end) {
        return entry.category() == NodeCategory.FIELD
                && entry.startOffset() >= owner.startOffset()
                && entry.endOffset() <= owner.endOffset()
                && start >= entry.startOffset() && end <= entry.endOffset();
    }

    /** Le plan d'alignement d'un champ. */
    private static Inline.@Nullable InlinePlan plan(Document doc, Analysis analyse,
            OutlineEntry owner, OutlineEntry entry) {
        List<FlowAnalysis.AbsoluteToken> ownTokens = FlowAnalysis.tokens(doc, analyse.lex(),
                entry.startOffset(), entry.endOffset());
        int name = fieldNameIndex(ownTokens, entry.name());
        int semicolon = semicolonIndex(ownTokens);
        if (name < 0 || semicolon < 0) {
            return null;
        }
        MethodDeclarations.Declaration declaration = declaration(ownTokens, name,
                semicolon, entry.name());
        String literal = InlineSafety.literalOf(ownTokens, declaration);
        List<FlowAnalysis.AbsoluteToken> ownerTokens = FlowAnalysis.tokens(doc, analyse.lex(),
                owner.startOffset(), owner.endOffset());
        if (literal == null || InlineSafety.reassigned(ownerTokens, declaration.name(),
                entry.startOffset(), entry.endOffset())
                || shadowed(doc, analyse, owner, entry)) {
            return null;
        }
        List<TextEdit> edits = InlineSafety.replacements(doc, ownerTokens, declaration,
                literal, true, 0, entry.startOffset(), entry.endOffset());
        if (edits == null) {
            return null;
        }
        if (isPrivate(ownTokens, name)) {
            Inline.removeStatement(doc, ownTokens, 0, semicolon, edits);
        }
        return edits.isEmpty() ? null : new Inline.InlinePlan(Inline.descending(edits));
    }

    /** La déclaration d'un champ dans les jetons de sa propre entrée. */
    private static MethodDeclarations.Declaration declaration(
            List<FlowAnalysis.AbsoluteToken> tokens, int name, int semicolon, String entryName) {
        int initializerStart = tokens.get(name + 1).text().equals("=") ? name + 2 : -1;
        int initializerEnd = initializerStart < 0 ? -1 : semicolon - 1;
        return new MethodDeclarations.Declaration(entryName, "", 0, semicolon,
                initializerStart, initializerEnd);
    }

    /** L'index du nom déclaré du champ dans les jetons de son entrée. */
    private static int fieldNameIndex(List<FlowAnalysis.AbsoluteToken> tokens, String name) {
        for (int i = 0; i < tokens.size(); i++) {
            if (FlowAnalysis.isName(tokens.get(i).kind())
                    && tokens.get(i).text().equals(name)
                    && TypeRun.declaresAfter(tokens, i)) {
                return i;
            }
        }
        return -1;
    }

    /** L'index du point-virgule qui clôt l'entrée, ou -1. */
    private static int semicolonIndex(List<FlowAnalysis.AbsoluteToken> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).text().equals(";")) {
                return i;
            }
        }
        return -1;
    }

    /** Vrai quand le champ porte le modifieur « private » avant son nom. */
    private static boolean isPrivate(List<FlowAnalysis.AbsoluteToken> tokens, int name) {
        for (int i = 0; i < name; i++) {
            if (tokens.get(i).text().equals("private")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vrai quand un nom homonyme vit ailleurs dans la classe : un
     * paramètre, un local ou un autre champ — une ombre changerait le
     * sens des usages.
     */
    private static boolean shadowed(Document doc, Analysis analyse, OutlineEntry owner,
            OutlineEntry entry) {
        for (OutlineEntry other : TreeOutline.entries(doc, analyse)) {
            if (outsideOwner(other, owner) || withinEntry(other, entry)) {
                continue;
            }
            if (declaresName(doc, analyse, other, entry.name())) {
                return true;
            }
        }
        return false;
    }

    /** Vrai quand l'entrée vit hors de la classe porteuse. */
    private static boolean outsideOwner(OutlineEntry entry, OutlineEntry owner) {
        return entry.startOffset() < owner.startOffset()
                || entry.endOffset() > owner.endOffset();
    }

    /** Vrai quand l'entrée vit dans le champ désigné (lui-même compris). */
    private static boolean withinEntry(OutlineEntry entry, OutlineEntry designated) {
        return entry.startOffset() >= designated.startOffset()
                && entry.endOffset() <= designated.endOffset();
    }

    /** Vrai quand l'entrée déclare le nom (champ homonyme ou déclaration de méthode). */
    private static boolean declaresName(Document doc, Analysis analyse, OutlineEntry entry,
            String name) {
        if (entry.category() == NodeCategory.FIELD) {
            return entry.name().equals(name);
        }
        if (entry.category() != NodeCategory.FUNCTION) {
            return false;
        }
        List<FlowAnalysis.AbsoluteToken> tokens = FlowAnalysis.tokens(doc, analyse.lex(),
                entry.startOffset(), entry.endOffset());
        return MethodStructure.declarations(tokens).containsKey(name);
    }
}
