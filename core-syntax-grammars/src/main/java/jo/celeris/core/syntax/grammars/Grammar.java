package jo.celeris.core.syntax.grammars;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.joni.Regex;

/**
 * Une grammaire TextMate (ADR-004 révisé, ADR-014) : le nom du langage,
 * sa portée racine, ses règles de premier niveau — les inclusions du
 * référentiel ({@code #clé}, {@code $self}, {@code $base}) restent des
 * références parcourues au fil de la lexicographie — ses injections
 * (H-05) et le rapport de chargement : ce que le moteur a accepté,
 * écarté ou dégradé, jamais en silence. Immuable et sans état : le
 * lexeur la partage entre tous les documents du langage.
 *
 * <p>L'ancre de document (chantier C2, prompt du 2026-11-22) : les
 * expressions qui portent {@code \A} vivent en double — l'expression
 * ordinaire sert la PREMIÈRE ligne de la vie du lexeur (le début du
 * texte coïncide avec le début de document), l'expression IMPOSSIBLE
 * sert les suivantes (le modèle tm4e : allowA sur la ligne 0, caractère
 * introuvable sur le reste). Le lecteur tisse le jeu, le lexeur y
 * traduit — les grammaires sans ancre ne paient rien.</p>
 */
public final class Grammar {

    private final String nom;
    private final String porteeRacine;
    private final List<Rule> patterns;
    private final List<Injection> injections;
    private final GrammarReport report;
    private final List<String> keywords;

    /** Les expressions à ancre de document : l'ordinaire vers
     *  l'impossible (la première ligne garde l'ordinaire). Champ hors
     *  des composantes : l'identité seule compare une expression —
     *  l'égalité de Regex n'existe pas. */
    private final IdentityHashMap<Regex, Regex> ancresDocument;

    /**
     * Une grammaire chargée.
     *
     * @param nom          le nom affichable du langage
     * @param porteeRacine la portée racine (par exemple source.toml)
     * @param patterns     les règles de premier niveau
     * @param injections   les injections déclarées, éventuellement vides
     * @param report       le rapport de chargement
     * @param keywords     les mots réservés moissonnés (complétion de base)
     */
    public Grammar(String nom, String porteeRacine, List<Rule> patterns,
            List<Injection> injections, GrammarReport report,
            List<String> keywords) {
        this(nom, porteeRacine, patterns, injections, report, keywords,
                new IdentityHashMap<>());
    }

    /** La construction complète : composantes et jeu d'ancres. */
    Grammar(String nom, String porteeRacine, List<Rule> patterns,
            List<Injection> injections, GrammarReport report,
            List<String> keywords, IdentityHashMap<Regex, Regex> ancresDocument) {
        this.nom = Objects.requireNonNull(nom);
        this.porteeRacine = Objects.requireNonNull(porteeRacine);
        this.patterns = List.copyOf(patterns);
        this.injections = List.copyOf(injections);
        this.report = Objects.requireNonNull(report);
        this.keywords = List.copyOf(keywords);
        this.ancresDocument = ancresDocument;
    }

    /** Le nom affichable du langage. */
    public String nom() {
        return nom;
    }

    /** La portée racine (par exemple source.toml). */
    public String porteeRacine() {
        return porteeRacine;
    }

    /** Les règles de premier niveau. */
    public List<Rule> patterns() {
        return patterns;
    }

    /** Les injections déclarées, éventuellement vides. */
    public List<Injection> injections() {
        return injections;
    }

    /** Le rapport de chargement. */
    public GrammarReport report() {
        return report;
    }

    /** Les mots réservés moissonnés (complétion de base). */
    public List<String> keywords() {
        return keywords;
    }

    /** Le jeu d'ancres de document tissé par le lecteur (motifs à
     *  {@code \A} : l'ordinaire mène à l'impossible). Vide sans
     *  ancre — la traduction ne coûte rien aux grammaires sans. */
    Map<Regex, Regex> ancres() {
        return ancresDocument;
    }
}
