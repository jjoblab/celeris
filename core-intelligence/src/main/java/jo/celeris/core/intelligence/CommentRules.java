package jo.celeris.core.intelligence;

import org.jspecify.annotations.Nullable;

/**
 * Les règles de commentaire par langage (M5, A-11) : le préfixe de
 * ligne, les délimiteurs de bloc. Un langage sans commentaire (JSON) ou
 * inconnu masque l'action — jamais de préfixe deviné.
 *
 * @param ligne      le préfixe de commentaire de ligne, ou null
 * @param blocDebut l'ouverture du commentaire de bloc, ou null
 * @param blocFin   la fermeture du commentaire de bloc, ou null
 */
public record CommentRules(@Nullable String ligne, @Nullable String blocDebut,
        @Nullable String blocFin) {

    /**
     * Les règles d'un langage par identifiant. La famille C au double
     * barre (Java, Kotlin, KTS, Groovy), la famille au dièse (Python,
     * Shell, YAML, properties, TOML), le balisage au bloc (XML, HTML) ;
     * JSON n'a pas de commentaire, les identifiants inconnus masquent
     * l'action.
     *
     * @param id l'identifiant du langage
     * @return ses règles, éventuellement sans commentaire
     */
    public static CommentRules parLangage(String id) {
        return switch (id) {
            case "java", "kotlin", "kts", "groovy" ->
                new CommentRules("// ", "/* ", " */");
            case "python", "shell", "sh", "yaml", "properties", "toml" ->
                new CommentRules("# ", null, null);
            case "xml", "html" -> new CommentRules(null, "<!--", "-->");
            default -> new CommentRules(null, null, null);
        };
    }

    /**
     * Vrai si le langage se commente : au moins une forme existe.
     *
     * @return la disponibilité du commentaire
     */
    public boolean commentable() {
        return ligne != null || blocDebut != null;
    }
}
