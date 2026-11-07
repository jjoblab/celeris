package jo.celeris.core.syntax.grammars;

import java.util.List;

import jo.celeris.core.language.Language;

/**
 * Le catalogue des langages embarqués de la voie de grammaires
 * (C-15/C-16) : le reste des langages P1 (TOML, YAML, properties,
 * Markdown, SQL, C, C++) et la longue traîne P2 (Python, Shell),
 * sans analyseur dédié — coloration et plis par la grammaire
 * déclarative, portés par le socle incrémental commun. Les
 * {@link Language} nourrissent le registre des langages de l'hôte.
 */
public final class LangagesGrammaires {

    private LangagesGrammaires() {
    }

    /** TOML — les fichiers de configuration des outillages Android. */
    public static GrammaireSyntax toml() {
        return new GrammaireSyntax("toml", Grammaires.ressource("toml"));
    }

    /** YAML — les manifestes et les fichiers d'intégration continue. */
    public static GrammaireSyntax yaml() {
        return new GrammaireSyntax("yaml", Grammaires.ressource("yaml"));
    }

    /** properties — les ressources Java, clés et valeurs. */
    public static GrammaireSyntax proprietes() {
        return new GrammaireSyntax("properties", Grammaires.ressource("properties"));
    }

    /** Markdown — la documentation, titres et blocs de code repliés. */
    public static GrammaireSyntax markdown() {
        return new GrammaireSyntax("markdown", Grammaires.ressource("markdown"));
    }

    /** Python — la longue traîne, scripts d'outillage compris. */
    public static GrammaireSyntax python() {
        return new GrammaireSyntax("python", Grammaires.ressource("python"));
    }

    /** Shell — les scripts de construction et d'installation. */
    public static GrammaireSyntax shell() {
        return new GrammaireSyntax("shell", Grammaires.ressource("shell"));
    }

    /** SQL — les migrations et les états de la base de l'hôte. */
    public static GrammaireSyntax sql() {
        return new GrammaireSyntax("sql", Grammaires.ressource("sql"));
    }

    /** C — le code natif des bibliothèques embarquées. */
    public static GrammaireSyntax c() {
        return new GrammaireSyntax("c", Grammaires.ressource("c"));
    }

    /** C++ — le code natif des moteurs et des bindings. */
    public static GrammaireSyntax cpp() {
        return new GrammaireSyntax("cpp", Grammaires.ressource("cpp"));
    }

    /** R8/ProGuard — les règles de réduction du paquet final. */
    public static GrammaireSyntax proguard() {
        return new GrammaireSyntax("proguard", Grammaires.ressource("proguard"));
    }

    /**
     * Les langages du catalogue pour le registre : identifiants, noms
     * affichables et extensions.
     *
     * @return les langages, dans l'ordre du catalogue
     */
    public static List<Language> langages() {
        return List.of(
                new Language("toml", "TOML", List.of("toml")),
                new Language("yaml", "YAML", List.of("yaml", "yml")),
                new Language("properties", "Properties", List.of("properties")),
                new Language("markdown", "Markdown", List.of("md", "markdown")),
                new Language("python", "Python", List.of("py", "pyw")),
                new Language("shell", "Shell", List.of("sh", "bash", "zsh")),
                new Language("sql", "SQL", List.of("sql")),
                new Language("c", "C", List.of("c", "h")),
                new Language("cpp", "C++", List.of("cpp", "cc", "cxx", "hpp", "hxx")),
                new Language("proguard", "ProGuard", List.of("pro")));
    }
}
