package jo.celeris.core.intelligence;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.TokenKind;

/**
 * La lecture des types de B-20 (variantes méthode et paramètre) : le type
 * d'une expression se lit sur sa FORME, jamais sur le contexte — littéral,
 * référence seule, promotion numérique des mélanges arithmétiques,
 * concaténation quand une chaîne se joint à des « + » seuls. Ce qui ne se
 * lit pas rend null, jamais un type faux.
 */
final class ExpressionTypes {

    private ExpressionTypes() {
    }

    /**
     * Le type lu d'une expression : sa forme décide.
     *
     * @param text     le texte de l'expression
     * @param tokens   les jetons de la sélection
     * @param refs     ses références libres
     * @param resolved les types résolus des références (nom → type)
     * @param fields   les champs de la classe porteuse (pour this.champ)
     * @return le type lu, ou null quand la forme ne se lit pas
     */
    static @Nullable String read(String text, List<FlowAnalysis.AbsoluteToken> tokens,
            List<FlowAnalysis.Reference> refs, Map<String, String> resolved,
            Map<String, String> fields) {
        String literal = Extraction.literalType(text);
        if (refs.isEmpty() && literal != null) {
            return literal;
        }
        if (refs.size() == 1 && text.strip().equals(refs.get(0).name())) {
            return nonVar(resolved.get(refs.get(0).name()));
        }
        return composite(tokens, refs, resolved, fields);
    }

    /** La promotion d'un mélange arithmétique ou sa concaténation. */
    private static @Nullable String composite(List<FlowAnalysis.AbsoluteToken> tokens,
            List<FlowAnalysis.Reference> refs, Map<String, String> resolved,
            Map<String, String> fields) {
        MixState state = new MixState();
        for (int i = 0; i < tokens.size(); i++) {
            FlowAnalysis.AbsoluteToken token = tokens.get(i);
            String type = operandType(tokens, i, refs, resolved, fields);
            if (type == null && isArithmeticOperator(token)) {
                state.plusOnly = state.plusOnly && token.text().equals("+");
            } else if (type == null) {
                if (!isCarrier(token)) {
                    return null;
                }
            } else {
                state.accumulate(type);
            }
        }
        return state.verdict();
    }

    /** L'état d'un mélange arithmétique : opérandes, promotion, concaténation. */
    private static final class MixState {
        boolean string;
        boolean object;
        boolean plusOnly = true;
        boolean anyOperand;
        String promotion = "int";

        /** Accumule un opérande : chaîne, promotion numérique ou objet. */
        void accumulate(String type) {
            anyOperand = true;
            switch (type) {
                case "String" -> string = true;
                case "double" -> promotion = "double";
                case "float" -> promotion = promotion.equals("double") ? promotion : "float";
                case "long" -> promotion = promotion.equals("int") ? "long" : promotion;
                case "int" -> {
                    // le plancher de la promotion
                }
                default -> object = !isNumeric(type) || object;
            }
        }

        /** Le verdict du mélange : concaténation, promotion ou null. */
        @Nullable String verdict() {
            if (string && plusOnly) {
                return "String";
            }
            if (string || object || !anyOperand) {
                return null;
            }
            return promotion;
        }
    }

    /** Vrai quand le jeton est un opérateur arithmétique du mélange. */
    private static boolean isArithmeticOperator(FlowAnalysis.AbsoluteToken token) {
        return token.kind() == TokenKind.OPERATOR && token.text().length() == 1
                && "+-*/%".contains(token.text());
    }

    /** Vrai quand le jeton porte un opérande sans en être un : this, point, parenthèses. */
    private static boolean isCarrier(FlowAnalysis.AbsoluteToken token) {
        return (token.kind() == TokenKind.KEYWORD && token.text().equals("this"))
                || (token.kind() == TokenKind.PUNCTUATION && token.text().equals("."))
                || (token.kind() == TokenKind.OPEN_BRACKET && token.text().equals("("))
                || (token.kind() == TokenKind.CLOSE_BRACKET && token.text().equals(")"));
    }

    /** Le type d'un opérande, ou null quand le jeton n'en porte pas. */
    private static @Nullable String operandType(List<FlowAnalysis.AbsoluteToken> tokens, int i,
            List<FlowAnalysis.Reference> refs, Map<String, String> resolved,
            Map<String, String> fields) {
        FlowAnalysis.AbsoluteToken token = tokens.get(i);
        if (token.kind() == TokenKind.NUMBER || token.kind() == TokenKind.STRING) {
            return Extraction.literalType(token.text());
        }
        if (!FlowAnalysis.isName(token.kind())) {
            return null;
        }
        for (FlowAnalysis.Reference ref : refs) {
            if (ref.offset() == token.offset()) {
                return nonVar(resolved.get(ref.name()));
            }
        }
        return thisMember(tokens, i, fields);
    }

    /** this.champ : le membre pointé de ce côté se lit comme un champ. */
    private static @Nullable String thisMember(List<FlowAnalysis.AbsoluteToken> tokens, int i,
            Map<String, String> fields) {
        String before = i > 0 ? tokens.get(i - 1).text() : "";
        String beforeBefore = i > 1 ? tokens.get(i - 2).text() : "";
        if (before.equals(".") && beforeBefore.equals("this")) {
            return nonVar(fields.get(tokens.get(i).text()));
        }
        return null;
    }

    /** Le type dépouillé de « var », ou null. */
    private static @Nullable String nonVar(@Nullable String type) {
        return type == null || type.equals("var") ? null : type;
    }

    private static boolean isNumeric(String type) {
        return type.equals("int") || type.equals("long")
                || type.equals("float") || type.equals("double");
    }
}
