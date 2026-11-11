package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;

/**
 * La complétion contextuelle (C6) : le contexte au curseur décide des
 * candidats. Après « {@code Enum.} » — les constantes de l'énumération
 * déclarée dans le document ; à l'intérieur de « {@code @Anno(} » — les
 * attributs de l'annotation déclarée dans le document. Les déclarations
 * se lisent sur les jetons du lexique (jamais les chaînes ni les
 * commentaires) ; l'absence de contexte rend une liste vide, jamais un
 * contexte deviné.
 *
 * <p>L'algorithme descend de la complétion contextuelle du dépôt de
 * départ (registre de portage, C6 — Spécifier : l'ancien sert
 * d'oracle), réduit au document courant : les énumérations et les
 * annotations hors du document ne se connaissent pas.</p>
 */
public final class ContextualCompletion {

    private ContextualCompletion() {
    }

    /** L'espèce du contexte au curseur. */
    public enum Kind {

        /** Après « {@code Enum.} » : les constantes de l'énumération. */
        ENUM_CONSTANTS,

        /** Dans « {@code @Anno(} » : les attributs de l'annotation. */
        ANNOTATION_ATTRIBUTES,

        /** Aucun contexte reconnu. */
        NONE
    }

    /** Le contexte au curseur : son espèce et ses candidats. */
    public record Context(Kind kind, List<String> candidates) {

        /** Constructeur canonique : liste figée. */
        public Context {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(candidates, "candidates");
            candidates = List.copyOf(candidates);
        }
    }

    /**
     * Le contexte au curseur : les constantes de l'énumération après
     * « {@code Enum.} », les attributs de l'annotation dans
     * « {@code @Anno(} », rien sinon.
     *
     * @param doc     le document, jamais muté
     * @param analyse l'analyse courante du document
     * @param caret   la position du curseur
     * @return le contexte — espèce NONE et candidats vides sans contexte
     * @throws IllegalArgumentException si le curseur est hors bornes
     */
    public static Context context(Document doc, Analysis analyse, int caret) {
        if (caret < 0 || caret > doc.length()) {
            throw new IllegalArgumentException(
                    "caret " + caret + " out of (0, " + doc.length() + ")");
        }
        List<FlowAnalysis.AbsoluteToken> tokens =
                FlowAnalysis.tokens(doc, analyse.lex(), 0, doc.length());
        int at = indexAt(tokens, caret);
        Context enumContext = enumContext(tokens, at);
        if (enumContext != null) {
            return enumContext;
        }
        Context annotationContext = annotationContext(tokens, at);
        if (annotationContext != null) {
            return annotationContext;
        }
        return new Context(Kind.NONE, List.of());
    }

    /**
     * Le contexte d'énumération : le jeton avant le curseur est un
     * point, celui d'avant un identifiant, et le document déclare
     * « {@code enum Identifiant} ».
     */
    private static @Nullable Context enumContext(
            List<FlowAnalysis.AbsoluteToken> tokens, int at) {
        if (at < 2 || !tokens.get(at - 1).text().equals(".")
                || !FlowAnalysis.isName(tokens.get(at - 2).kind())) {
            return null;
        }
        String nom = tokens.get(at - 2).text();
        List<String> constantes = enumConstants(tokens, nom);
        return constantes.isEmpty() ? null
                : new Context(Kind.ENUM_CONSTANTS, constantes);
    }

    /**
     * Le contexte d'annotation : le curseur vit dans la parenthèse
     * d'un usage d'annotation « {@code @Nom(} », et le document
     * déclare « {@code @interface Nom} ».
     */
    private static @Nullable Context annotationContext(
            List<FlowAnalysis.AbsoluteToken> tokens, int at) {
        int open = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).offset() >= caretOf(tokens, at)) {
                break;
            }
            if (tokens.get(i).text().equals("(")) {
                open = i;
            }
            if (tokens.get(i).text().equals(")")) {
                open = -1;
            }
        }
        if (open < 1 || tokens.get(open - 1).kind() != TokenKind.ANNOTATION) {
            return null;
        }
        String texte = tokens.get(open - 1).text();
        String nom = texte.startsWith("@") ? texte.substring(1) : texte;
        List<String> attributs = annotationAttributes(tokens, nom);
        return attributs.isEmpty() ? null
                : new Context(Kind.ANNOTATION_ATTRIBUTES, attributs);
    }

    /**
     * Les constantes de l'énumération nommée, déclarée dans le
     * document : les têtes des segments séparés par virgules de fond,
     * jusqu'à l'accolade fermante.
     */
    private static List<String> enumConstants(
            List<FlowAnalysis.AbsoluteToken> tokens, String nom) {
        List<String> constantes = new ArrayList<>();
        int corps = declarationBody(tokens, "enum", nom);
        if (corps < 0) {
            return constantes;
        }
        collectSegmentHeads(tokens, corps, constantes);
        return constantes;
    }

    /**
     * Les attributs de l'annotation nommée, déclarée dans le document :
     * les identifiants suivis d'une parenthèse ouvrante dans le corps
     * de la déclaration.
     */
    private static List<String> annotationAttributes(
            List<FlowAnalysis.AbsoluteToken> tokens, String nom) {
        List<String> attributs = new ArrayList<>();
        int corps = declarationBody(tokens, "@interface", nom);
        if (corps < 0) {
            return attributs;
        }
        int depth = 1;
        for (int i = corps; i < tokens.size(); i++) {
            String text = tokens.get(i).text();
            if (text.equals("{")) {
                depth++;
                continue;
            }
            if (text.equals("}")) {
                if (--depth == 0) {
                    break;
                }
                continue;
            }
            if (depth == 1 && FlowAnalysis.isName(tokens.get(i).kind())
                    && i + 1 < tokens.size() && tokens.get(i + 1).text().equals("(")) {
                attributs.add(text);
            }
        }
        return attributs;
    }

    /**
     * L'index du jeton qui suit l'accolade ouvrante de la déclaration
     * « {@code mot Nom} », ou -1 : la déclaration introuvable refuse.
     */
    private static int declarationBody(
            List<FlowAnalysis.AbsoluteToken> tokens, String mot, String nom) {
        for (int i = 0; i < tokens.size() - 2; i++) {
            if (tokens.get(i).text().equals(mot)
                    && tokens.get(i + 1).text().equals(nom)
                    && tokens.get(i + 2).text().equals("{")) {
                return i + 3;
            }
        }
        return -1;
    }

    /** Les têtes des segments de niveau un : constantes d'énumération. */
    private static void collectSegmentHeads(
            List<FlowAnalysis.AbsoluteToken> tokens, int from, List<String> heads) {
        int depth = 1;
        boolean tete = true;
        for (int i = from; i < tokens.size(); i++) {
            String text = tokens.get(i).text();
            if (text.equals("{")) {
                depth++;
                tete = false;
                continue;
            }
            if (text.equals("}")) {
                if (--depth == 0) {
                    return;
                }
                tete = false;
                continue;
            }
            if (depth == 1 && text.equals(",")) {
                tete = true;
                continue;
            }
            if (depth == 1 && tete && FlowAnalysis.isName(tokens.get(i).kind())) {
                heads.add(text);
                tete = false;
            }
        }
    }

    /** L'index du premier jeton à l'offset du curseur ou après. */
    private static int indexAt(List<FlowAnalysis.AbsoluteToken> tokens, int caret) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).offset() >= caret) {
                return i;
            }
        }
        return tokens.size();
    }

    /** L'offset du jeton d'index donné, la fin du document aux bornes. */
    private static int caretOf(List<FlowAnalysis.AbsoluteToken> tokens, int at) {
        return at < tokens.size() ? tokens.get(at).offset() : Integer.MAX_VALUE;
    }
}
