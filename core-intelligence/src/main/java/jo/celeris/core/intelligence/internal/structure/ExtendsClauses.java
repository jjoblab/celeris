package jo.celeris.core.intelligence.internal.structure;

import jo.celeris.core.intelligence.internal.flow.FlowAnalysis;
import java.util.ArrayList;

import java.util.List;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.TokenKind;

/**
 * La lecture des clauses d'héritage d'un en-tête de type (B-12) :
 * {@code extends} et {@code implements}, jusqu'à l'accolade ouvrante
 * du corps. Les chevrons de généricité portent leurs bornes à part —
 * {@code class Enclos<T extends Animal>} ne dit PAS qu'Animal est un
 * parent — et le nom qualifié prend son dernier segment ({@code
 * java.lang.Exception} dit {@code Exception}).
 *
 * <p>La lecture est TOLÉRANTE : un en-tête sans accolade de corps ne
 * dit rien (liste vide), une clause jamais fermée rend ce qu'elle a lu
 * — jamais d'exception.</p>
 */
public final class ExtendsClauses  {

    /** Les chevrons ouvrants des généricités. */
    private static final String OUVRANT = "<";

    /** Les chevrons fermants des généricités. */
    private static final String FERMANT = ">";

    private final List<String> clauses = new ArrayList<>();

    private int chevrons;

    private boolean dansClause;

    private @Nullable String courant;

    private ExtendsClauses() {
    }

    /**
     * Les parents déclarés d'un type : les clauses lues à l'en-tête,
     * dans l'ordre du source, doublon jamais.
     *
     * @param tokens les jetons absolus de la déclaration du type
     * @param body  l'index de l'accolade ouvrante du corps, ou -1
     * @return les noms des parents déclarés, dans l'ordre
     */
    public static List<String> of(List<FlowAnalysis.AbsoluteToken> tokens, int body) {
        if (body < 0) {
            return List.of();
        }
        ExtendsClauses lecture = new ExtendsClauses();
        for (int i = 0; i < body; i++) {
            lecture.token(tokens.get(i));
        }
        lecture.basculer();
        return List.copyOf(lecture.clauses);
    }

    /** Traite un jeton de l'en-tête : chevron, mot de clause, nom, séparateur. */
    private void token(FlowAnalysis.AbsoluteToken token) {
        String text = token.text();
        if (text.equals(OUVRANT)) {
            chevrons++;
        } else if (text.equals(FERMANT)) {
            chevrons = Math.max(0, chevrons - 1);
        } else if (estMotClause(token) && chevrons == 0) {
            basculer();
            dansClause = true;
        } else if (dansClause && chevrons > 0) {
            // les bornes de généricité ne comptent pas de parents
        } else if (dansClause && FlowAnalysis.isName(token.kind())) {
            courant = text;
        } else if (dansClause && text.equals(",")) {
            basculer();
        }
    }

    /** Vrai quand le jeton ouvre une clause, chevrons fermés. */
    private boolean estMotClause(FlowAnalysis.AbsoluteToken token) {
        return token.kind() == TokenKind.KEYWORD
                && (token.text().equals("extends") || token.text().equals("implements"));
    }

    /** Le nom lu bascule dans les clauses, s'il y en a un, doublon jamais. */
    private void basculer() {
        if (courant != null && !clauses.contains(courant)) {
            clauses.add(courant);
        }
        courant = null;
    }
}
