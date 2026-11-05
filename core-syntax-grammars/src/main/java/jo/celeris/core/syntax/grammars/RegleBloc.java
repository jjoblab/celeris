package jo.celeris.core.syntax.grammars;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * Bloc début/fin d'une grammaire (format TextMate) : l'ouverture et la
 * fermeture peuvent vivre sur des lignes différentes — l'état transporté
 * du lexeur porte le bloc ouvert d'une ligne à la suivante, la
 * fermeture se cherche au fil du contenu. Les portées
 * {@code punctuation.section.*.begin} et {@code *.end} font de
 * l'ouverture et de la fermeture des crochets appariés — la profondeur,
 * les plis et la visée s'en servent (ADR-004).
 *
 * @param debut         l'expression d'ouverture compilée
 * @param fin           l'expression de fermeture compilée
 * @param portee        la portée des jetons d'ouverture et de fermeture
 * @param porteeContenu la portée des écarts de contenu, null pour
 *                      l'identificateur
 * @param capturesDebut la portée de l'ouverture par numéro de groupe,
 *                      la clé 0 portant l'ouverture entière
 * @param capturesFin   la portée de la fermeture par numéro de groupe,
 *                      la clé 0 portant la fermeture entière
 * @param motifs        les règles applicables au contenu du bloc,
 *                      inclusions aplaties
 */
record RegleBloc(Pattern debut, Pattern fin, String portee,
        @Nullable String porteeContenu,
        @Nullable Map<Integer, String> capturesDebut,
        @Nullable Map<Integer, String> capturesFin,
        List<Regle> motifs) implements Regle {

    /** La portée du jeton d'ouverture : la capture 0 prime sur la règle. */
    String porteeDebut() {
        return capture(capturesDebut, 0, portee);
    }

    /** La portée du jeton de fermeture : la capture 0 prime sur la règle. */
    String porteeFin() {
        return capture(capturesFin, 0, portee);
    }

    private static String capture(@Nullable Map<Integer, String> captures,
            int groupe, String defaut) {
        if (captures != null) {
            String portee = captures.get(groupe);
            if (portee != null) {
                return portee;
            }
        }
        return defaut;
    }
}
