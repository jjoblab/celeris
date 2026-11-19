package jo.celeris.core.syntax.grammars;

import java.util.List;

/**
 * Une grammaire TextMate (ADR-004 révisé, ADR-014) : le nom du langage,
 * sa portée racine, ses règles de premier niveau — les inclusions du
 * référentiel ({@code #clé}, {@code $self}, {@code $base}) restent des
 * références parcourues au fil de la lexicographie — ses injections
 * (H-05) et le rapport de chargement : ce que le moteur a accepté,
 * écarté ou dégradé, jamais en silence. Immuable et sans état : le
 * lexeur la partage entre tous les documents du langage.
 *
 * @param nom          le nom affichable du langage
 * @param porteeRacine la portée racine (par exemple source.toml)
 * @param patterns       les règles de premier niveau
 * @param injections   les injections déclarées, éventuellement vides
 * @param report      le rapport de chargement
 * @param keywords     les mots réservés moissonnés (complétion de base)
 */
public record Grammar(String nom, String porteeRacine, List<Rule> patterns,
        List<Injection> injections, GrammarReport report,
        List<String> keywords) {

    /** Constructeur canonique : les listes se figent, jamais nulles. */
    public Grammar {
        patterns = List.copyOf(patterns);
        injections = List.copyOf(injections);
        keywords = List.copyOf(keywords);
    }
}
