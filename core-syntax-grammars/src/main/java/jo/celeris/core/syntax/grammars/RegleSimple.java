package jo.celeris.core.syntax.grammars;

import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * Motif simple d'une grammaire (format TextMate) : une expression
 * rationnelle compilée, la portée du jeton couvert et, le cas échéant,
 * les portées des groupes de capture — chaque groupe capturé devient un
 * jeton propre, l'intervalle non couvert porte la portée de la règle.
 *
 * @param motif    l'expression compilée
 * @param portee   la portée du jeton, jamais null
 * @param captures les portées par numéro de groupe, null si aucune
 */
record RegleSimple(Pattern motif, String portee,
        @Nullable Map<Integer, String> captures) implements Regle {
}
