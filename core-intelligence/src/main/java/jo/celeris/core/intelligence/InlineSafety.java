package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * La sûreté de l'alignement (B-21) : ce qui refuse le plan, jamais ce
 * qui le fausse — l'initialisateur doit être un littéral seul (un
 * littéral est atomique, aucune précédence ne se brise à la
 * substitution), la déclaration ne doit jamais être réaffectée
 * (affectation composée, incrément et décrément compris), et aucune
 * ombre homonyme ne doit paraître après la déclaration (redéclaration
 * locale ou paramètre de lambda — une ombre changerait le sens des
 * usages suivants).
 */
final class InlineSafety {

    /** Les textes d'affectation qui réaffectent un nom. */
    private static final Set<String> ASSIGNMENTS = Set.of(
            "=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=");

    private InlineSafety() {
    }

    /** Le littéral de l'initialisateur, ou null s'il n'est pas un littéral seul. */
    static @Nullable String literalOf(List<FlowAnalysis.AbsoluteToken> tokens,
            MethodDeclarations.Declaration declaration) {
        if (declaration.initializerStart() < 0 || declaration.initializerEnd() < 0
                || declaration.initializerEnd() != declaration.initializerStart()) {
            return null;
        }
        String text = tokens.get(declaration.initializerStart()).text();
        return Extraction.literalType(text) == null ? null : text;
    }

    /**
     * Vrai quand le nom se réaffecte hors de sa déclaration — la plage
     * exclue couvre l'instruction déclarée, les affectations composées,
     * l'incrément et le décrément compris.
     */
    static boolean reassigned(List<FlowAnalysis.AbsoluteToken> tokens, String name,
            int excludeFrom, int excludeTo) {
        for (int i = 0; i < tokens.size(); i++) {
            FlowAnalysis.AbsoluteToken token = tokens.get(i);
            if (!FlowAnalysis.isName(token.kind()) || !token.text().equals(name)
                    || (token.offset() >= excludeFrom && token.offset() < excludeTo)) {
                continue;
            }
            String after = i + 1 < tokens.size() ? tokens.get(i + 1).text() : "";
            if (ASSIGNMENTS.contains(after) || after.equals("++") || after.equals("--")) {
                return true;
            }
        }
        return false;
    }

    /** Vrai quand une ombre homonyme paraît après la déclaration. */
    static boolean shadowed(List<FlowAnalysis.AbsoluteToken> tokens,
            MethodDeclarations.Declaration declaration) {
        for (int i = declaration.statementEnd() + 1; i < tokens.size(); i++) {
            FlowAnalysis.AbsoluteToken token = tokens.get(i);
            if (!FlowAnalysis.isName(token.kind()) || !token.text().equals(declaration.name())) {
                continue;
            }
            String before = i > 0 ? tokens.get(i - 1).text() : "";
            String after = i + 1 < tokens.size() ? tokens.get(i + 1).text() : "";
            if (after.equals("->")) {
                return true;
            }
            if (!before.equals(".") && !after.equals("(")
                    && TypeRun.followsType(tokens, i) && TypeRun.declaresAfter(tokens, i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Les remplacements des usages : le littéral prend la place du nom.
     * Pour un champ, les usages nus et les membres de {@code this} se
     * remplacent (toute la classe, la déclaration exclue) — le membre de
     * {@code this} se remplace en entier ({@code this.champ} devient le
     * littéral), le récepteur étranger refuse (cible inconnue). Pour une
     * locale, seuls les usages après la déclaration se remplacent — les
     * membres pointés ne sont jamais les siens.
     *
     * @param doc          le document porteur (le membre de this s'y lit)
     * @param tokens       les jetons de la portée (méthode ou classe)
     * @param declaration  la déclaration alignée
     * @param literal      le littéral substitué
     * @param field        vrai pour un champ, faux pour une locale
     * @param fromOffset   l'offset d'où les usages se comptent
     * @param excludeFrom  le début de la plage exclue (la déclaration)
     * @param excludeTo    la fin de la plage exclue
     * @return les éditions, ou null sur un récepteur étranger
     */
    static @Nullable List<TextEdit> replacements(Document doc,
            List<FlowAnalysis.AbsoluteToken> tokens, MethodDeclarations.Declaration declaration,
            String literal, boolean field, int fromOffset, int excludeFrom, int excludeTo) {
        List<TextEdit> edits = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            FlowAnalysis.AbsoluteToken token = tokens.get(i);
            if (token.offset() < fromOffset || !FlowAnalysis.isName(token.kind())
                    || !token.text().equals(declaration.name())
                    || (token.offset() >= excludeFrom && token.offset() < excludeTo)) {
                continue;
            }
            Usage usage = usageOf(tokens, i, field);
            if (usage == Usage.FOREIGN_MEMBER) {
                return null;
            }
            if (usage == Usage.THIS_MEMBER) {
                edits.add(thisEdit(doc, tokens, i, literal));
            } else if (usage == Usage.READ) {
                edits.add(new TextEdit(token.offset(), token.text(), literal));
            }
        }
        return edits;
    }

    /** La lecture d'une occurrence du nom. */
    private enum Usage {
        /** Le nom se lit : il se remplace par le littéral. */
        READ,

        /** Le membre de « this » se lit : l'expression entière se remplace. */
        THIS_MEMBER,

        /** Le récepteur est étranger : la cible ne se connaît pas — refus. */
        FOREIGN_MEMBER,

        /** L'occurrence n'est pas une usage : membre pointé d'une locale, appel, construction, lambda. */
        OTHER
    }

    /** La lecture d'une occurrence du nom selon ses voisins. */
    private static Usage usageOf(List<FlowAnalysis.AbsoluteToken> tokens, int i, boolean field) {
        String before = i > 0 ? tokens.get(i - 1).text() : "";
        String beforeBefore = i > 1 ? tokens.get(i - 2).text() : "";
        String after = i + 1 < tokens.size() ? tokens.get(i + 1).text() : "";
        if (before.equals(".") && !field) {
            return Usage.OTHER;
        }
        if (before.equals(".") && !beforeBefore.equals("this")) {
            return Usage.FOREIGN_MEMBER;
        }
        if (before.equals(".")) {
            return Usage.THIS_MEMBER;
        }
        if (before.equals("new") || after.equals("(") || after.equals("->")
                || after.equals("::")) {
            return Usage.OTHER;
        }
        return Usage.READ;
    }

    /** L'édition du membre de this : l'expression entière devient le littéral. */
    private static TextEdit thisEdit(Document doc, List<FlowAnalysis.AbsoluteToken> tokens,
            int i, String literal) {
        int start = tokens.get(i - 2).offset();
        int end = tokens.get(i).offset() + tokens.get(i).text().length();
        return new TextEdit(start, doc.subText(start, end), literal);
    }
}
