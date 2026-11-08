package jo.celeris.core.syntax.grammars;

import java.util.Map;

import org.joni.Regex;
import org.jspecify.annotations.Nullable;

/**
 * Motif simple d'une grammaire TextMate : une expression Oniguruma
 * compilée par joni, la portée du texte couvert et, le cas échéant, les
 * captures par numéro de groupe — chaque groupe capturé devient un
 * jeton propre, l'intervalle non couvert porte la portée de la règle.
 * La portée peut être absente : le texte couvert garde alors la portée
 * du contexte, comme le fait la référence.
 *
 * @param motif    l'expression compilée
 * @param portee   la portée du jeton, null si la règle n'en porte pas
 * @param captures les captures par numéro de groupe, null si aucune
 */
record RegleSimple(Regex motif, @Nullable String portee,
        @Nullable Map<Integer, Capture> captures) implements Regle {
}
