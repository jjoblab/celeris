package jo.celeris.core.syntax;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Une règle du plan par données (M7.6, H-08 — analyse structurelle
 * niveau 1, ADR-016) : la ligne qui porte le motif déclare une entrée
 * du plan, la catégorie dit sa présentation, le groupe capturant
 * porte son nom. Les règles vivent dans les paquets de langage
 * ({@code outline.json}) — Python, YAML et SQL ont un plan sans une
 * ligne de Java, la donnée parle.
 *
 * <p>Les motifs sont les nôtres (écrits dans les paquets, bornés et
 * ancrés à la ligne) : le moteur de la voie de grammaires et son
 * Oniguruma ne s'y mêlent pas, {@link RuleOutline} applique le regex
 * du JDK sur le texte d'une ligne.</p>
 *
 * @param pattern   le motif compilé, appliqué au texte d'une ligne
 * @param category  la catégorie de l'entrée déclarée
 * @param nameGroup le groupe capturant du nom (1 pour le premier)
 */
public record OutlineRule(Pattern pattern, NodeCategory category, int nameGroup) {

    /** Les catégories que les règles savent déclarer. */
    private static final Set<String> CATEGORIES = Set.of("type", "function",
            "field", "tag");

    /**
     * La règle validée : le motif se compile, la catégorie se connaît,
     * le groupe existe.
     *
     * @param pattern   le motif brut
     * @param category  la catégorie en minuscules (type, function, field, tag)
     * @param nameGroup le groupe capturant du nom, au moins un
     * @return la règle prête à l'application
     * @throws IllegalArgumentException avec le champ en cause sinon
     */
    public static OutlineRule of(String pattern, String category, int nameGroup) {
        Pattern compiled;
        try {
            compiled = Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException(
                    "outline rule pattern rejected: " + pattern, e);
        }
        String normalized = category.toLowerCase(Locale.ROOT);
        if (!CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "outline rule category unknown: " + category);
        }
        if (nameGroup < 1 || nameGroup > compiled.matcher("").groupCount()) {
            throw new IllegalArgumentException(
                    "outline rule name group out of range: " + nameGroup);
        }
        return new OutlineRule(compiled,
                NodeCategory.valueOf(normalized.toUpperCase(Locale.ROOT)),
                nameGroup);
    }
}
